[CmdletBinding()]
param(
    [ValidateRange(1024, 65535)][int]$BackendPort = 8080,
    [ValidateRange(1024, 65535)][int]$FrontendPort = 3000,
    [switch]$BackendOnly,
    [switch]$SmokeTest
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$logs = Join-Path ([IO.Path]::GetTempPath()) ('fixna-demo-' + [guid]::NewGuid().ToString('N'))
$processes = [Collections.Generic.List[Diagnostics.Process]]::new()
$environmentNames = @('POSTGRES_PASSWORD', 'SPRING_PROFILES_ACTIVE', 'FIXNA_APP_ENV',
    'FIXNA_TEST_DATA_ENABLED', 'FIXNA_AI_PROVIDER', 'FIXNA_PLATFORM_MODE',
    'FIXNA_CORS_ALLOWED_ORIGINS', 'NEXT_PUBLIC_API_BASE_URL')
$savedEnvironment = @{}
foreach ($name in $environmentNames) {
    $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

function Assert-PortFree([int]$Port) {
    $listeners = [Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners()
    if ($listeners | Where-Object { $_.Port -eq $Port }) {
        throw "Port $Port is already in use. Stop its server in its own terminal or choose another port."
    }
}

function Start-Tracked([string]$Name, [string]$Directory, [string]$Command) {
    $process = Start-Process -FilePath $env:ComSpec -ArgumentList ('/d /s /c "' + $Command + '"') `
        -WorkingDirectory $Directory -RedirectStandardOutput (Join-Path $logs "$Name.log") `
        -RedirectStandardError (Join-Path $logs "$Name-error.log") -PassThru -WindowStyle Hidden
    # Retain the handle before a short-lived child exits (Windows PowerShell 5.1).
    $null = $process.Handle
    $processes.Add($process)
    return $process
}

function Wait-Ready([Diagnostics.Process]$Process, [string]$Url, [int]$Seconds = 240) {
    $deadline = [DateTime]::UtcNow.AddSeconds($Seconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($Process.HasExited) { throw "Service exited before readiness. Inspect $logs" }
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -eq 200) { return }
        } catch { }
        Start-Sleep -Seconds 2
    }
    throw "Timed out waiting for $Url. Inspect $logs"
}

$exitCode = 0
try {
    foreach ($command in @('java.exe', 'mvn.cmd')) { $null = Get-Command $command -ErrorAction Stop }
    if (!$BackendOnly) {
        foreach ($command in @('node.exe', 'npm.cmd')) { $null = Get-Command $command -ErrorAction Stop }
        if ($BackendPort -eq $FrontendPort) { throw 'Backend and frontend ports must differ.' }
        Assert-PortFree $FrontendPort
    }
    Assert-PortFree $BackendPort
    $null = New-Item -ItemType Directory -Path $logs
    if (!$env:POSTGRES_PASSWORD) {
        $secret = Read-Host 'PostgreSQL password for postgres@localhost:5432/localboost' -AsSecureString
        $env:POSTGRES_PASSWORD = [Net.NetworkCredential]::new('', $secret).Password
        $secret.Dispose()
    }
    $env:SPRING_PROFILES_ACTIVE = 'local'
    $env:FIXNA_APP_ENV = 'local'
    $env:FIXNA_TEST_DATA_ENABLED = 'false'
    $env:FIXNA_AI_PROVIDER = 'mock'
    $env:FIXNA_PLATFORM_MODE = 'mock'
    $env:FIXNA_CORS_ALLOWED_ORIGINS = "http://localhost:$FrontendPort,http://127.0.0.1:$FrontendPort"
    $env:NEXT_PUBLIC_API_BASE_URL = "http://localhost:$BackendPort/api"
    Write-Host 'Compiling backend (clean) to avoid stale class files...'
    $compile = Start-Process -FilePath 'mvn.cmd' -ArgumentList @(
        '-f', (Join-Path $root 'backend/pom.xml'), 'clean', 'compile', '-q'
    ) -WorkingDirectory $root -Wait -PassThru -NoNewWindow
    if ($compile.ExitCode -ne 0) { throw "Backend compile failed (exit $($compile.ExitCode)). Run mvn -f backend/pom.xml clean compile for details." }
    Write-Host "Starting backend against persistent localboost database. Logs: $logs"
    $backend = Start-Tracked 'backend' $root "mvn.cmd -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments=--server.port=$BackendPort"
    # Do not forward the database password to npm or the frontend.
    [Environment]::SetEnvironmentVariable('POSTGRES_PASSWORD', $null, 'Process')
    Wait-Ready $backend "http://localhost:$BackendPort/actuator/health"
    if (!$BackendOnly) {
        Write-Host 'Installing frontend dependencies from package-lock.json...'
        $install = Start-Tracked 'npm-ci' (Join-Path $root 'frontend') 'npm.cmd ci --no-audit --no-fund'
        if (!$install.WaitForExit(600000)) { throw "npm ci timed out. Inspect $logs" }
        $install.WaitForExit()
        if ($null -eq $install.ExitCode) { throw "npm ci exit code unavailable. Inspect $logs" }
        if ($install.ExitCode -ne 0) { throw "npm ci failed (exit $($install.ExitCode)). Inspect $logs" }
        $frontend = Start-Tracked 'frontend' (Join-Path $root 'frontend') "npm.cmd run dev -- --port $FrontendPort --hostname localhost"
        Wait-Ready $frontend "http://localhost:$FrontendPort/login"
    }
    Write-Host 'LOCAL DEMO READY - existing PostgreSQL data preserved; no fixtures loaded.'
    Write-Host "API: http://localhost:$BackendPort/api/v1"
    if (!$BackendOnly) { Write-Host "Login: http://localhost:$FrontendPort/login" }
    Write-Host 'Use your existing account and password (owner@example.com if demo SQL was loaded).'
    if (!$SmokeTest) {
        Write-Host 'Keep this window open. Press Ctrl+C to stop these services; PostgreSQL stays running.'
        while ($true) {
            if ($backend.HasExited -or (!$BackendOnly -and $frontend.HasExited)) {
                throw "A service stopped unexpectedly. Inspect $logs"
            }
            Start-Sleep -Seconds 2
        }
    }
} catch {
    Write-Host "STARTUP FAILED: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Diagnostic logs: $logs"
    $exitCode = 1
} finally {
    foreach ($process in $processes) {
        if (!$process.HasExited) { & taskkill.exe /PID $process.Id /T /F 2>$null | Out-Null }
        $process.Dispose()
    }
    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process')
    }
}
exit $exitCode

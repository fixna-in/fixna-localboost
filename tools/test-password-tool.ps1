$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$process = [Diagnostics.Process]::new()
$process.StartInfo.FileName = $env:ComSpec
$process.StartInfo.Arguments = '/d /s /c ""' + (Join-Path $PSScriptRoot 'password-tool.cmd') + '" hash"'
$process.StartInfo.WorkingDirectory = [IO.Path]::GetTempPath()
$process.StartInfo.UseShellExecute = $false
$process.StartInfo.RedirectStandardInput = $true
$process.StartInfo.RedirectStandardOutput = $true
$process.StartInfo.RedirectStandardError = $true
try {
    $null = $process.Start()
    $process.StandardInput.Close()
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    if (!$process.WaitForExit(120000)) {
        & taskkill.exe /PID $process.Id /T /F | Out-Null
        throw 'Password launcher regression timed out.'
    }
    $output = $stdout.Result + $stderr.Result
    # With stdin redirected, the fully initialized Java utility must reject the
    # missing console safely. A missing BCrypt jar fails before reaching this guard.
    if ($process.ExitCode -ne 2 -or $output -notmatch 'An interactive terminal is required' -or
            $output -match 'NoClassDefFoundError|ClassNotFoundException') {
        throw "Launcher did not reach the console guard. Exit=$($process.ExitCode)`n$output"
    }
    $argsFile = [IO.File]::ReadAllText((Join-Path $root 'backend\target\password-tool.args'))
    if ($argsFile.Length -le 1024 -or $argsFile -notmatch 'spring-security-crypto' -or
            $argsFile -notmatch 'spring-jcl') {
        throw 'Expected complete Maven classpath, including BCrypt and logging dependencies.'
    }
    Write-Output 'PASS: real CMD launcher loads BCrypt with a long classpath from another working directory; hidden-input guard returns exit 2.'
} finally {
    $process.Dispose()
}

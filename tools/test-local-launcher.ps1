$ErrorActionPreference = 'Stop'
$script = Join-Path $PSScriptRoot 'start-local-demo.ps1'
$entry = Join-Path $PSScriptRoot 'start-local-demo.cmd'
$tokens = $null
$errors = $null
$null = [Management.Automation.Language.Parser]::ParseFile($script, [ref]$tokens, [ref]$errors)
if ($errors.Count) { throw ($errors | Out-String) }
& $entry -?
if ($LASTEXITCODE -ne 0) { throw 'CMD entry point could not load PowerShell launcher.' }
# Parameter validation must stop before credentials, database access or service startup.
$previousPreference = $ErrorActionPreference
try {
    $ErrorActionPreference = 'Continue'
    & $entry -BackendOnly -BackendPort 1 2>&1 | Out-Null
    $invalidExit = $LASTEXITCODE
} finally { $ErrorActionPreference = $previousPreference }
if ($invalidExit -eq 0) { throw 'Invalid port was accepted.' }
Write-Host 'PASS: launcher syntax, CMD delegation and invalid-port rejection.'

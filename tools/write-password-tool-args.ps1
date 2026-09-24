param(
    [Parameter(Mandatory = $true)][string]$ClasspathFile,
    [Parameter(Mandatory = $true)][string]$OutputFile
)
$ErrorActionPreference = 'Stop'
try {
    # Read the entire Maven classpath, avoiding CMD set /p and command-length limits.
    $classpath = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $ClasspathFile).Path).Trim()
    if ([string]::IsNullOrWhiteSpace($classpath) -or $classpath -match '[\r\n"]') {
        throw 'Invalid Maven dependency classpath.'
    }
    # Java argument files interpret backslashes as escapes inside quoted arguments.
    $classpath = $classpath.Replace('\', '/')
    $arguments = "-cp`n`"target/classes;$classpath`"`nin.fixna.platform.common.util.PasswordUtility`n"
    [IO.File]::WriteAllText($ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($OutputFile),
        $arguments, [Text.UTF8Encoding]::new($false))
} catch {
    Write-Error $_
    exit 2
}

@echo off
setlocal
if "%~1"=="hash" goto run
if "%~1"=="verify" goto run
echo Usage: password-tool.cmd hash^|verify
exit /b 2
:run
if not "%~2"=="" exit /b 2
pushd "%~dp0..\backend"
call mvn.cmd -q compile dependency:build-classpath -Dmdep.outputFile=target/password-tool-classpath.txt
if errorlevel 1 (
  popd
  exit /b 2
)
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0write-password-tool-args.ps1" -ClasspathFile "target\password-tool-classpath.txt" -OutputFile "target\password-tool.args"
if errorlevel 1 (
  popd
  exit /b 2
)
java.exe @target\password-tool.args %~1
set "RESULT=%ERRORLEVEL%"
popd
exit /b %RESULT%

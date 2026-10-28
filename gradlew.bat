@echo off
setlocal EnableExtensions
set "ROOT=%~dp0"
set "VERSION=9.5.0"
set "BOOT=%ROOT%.gradle-bootstrap"
set "DIST=%BOOT%\gradle-%VERSION%"
set "ZIP=%BOOT%\gradle-%VERSION%-bin.zip"
set "URL=https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip"
if not exist "%DIST%\bin\gradle.bat" (
  if not exist "%BOOT%" mkdir "%BOOT%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $zip='%ZIP%'; $base='%URL%'; $hash=((Invoke-WebRequest -UseBasicParsing ($base+'.sha256')).Content -join '').Trim(); if (!(Test-Path $zip)) { Invoke-WebRequest -UseBasicParsing $base -OutFile $zip }; if ((Get-FileHash -Algorithm SHA256 $zip).Hash -ine $hash) { Remove-Item -Force $zip; throw 'SHA256 de Gradle incorrecte' }; Expand-Archive -Force $zip '%BOOT%'"
  if errorlevel 1 exit /b 1
)
call "%DIST%\bin\gradle.bat" %*
exit /b %errorlevel%

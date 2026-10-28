@echo off
setlocal
set URL=https://raw.githubusercontent.com/gradle/gradle/v9.5.0/gradle/wrapper/gradle-wrapper.jar
set DEST=gradle\wrapper\gradle-wrapper.jar
if exist "%DEST%" (
  echo Gradle Wrapper ja existeix: %DEST%
  exit /b 0
)
echo Descarregant Gradle Wrapper oficial...
curl.exe -L "%URL%" -o "%DEST%"
if errorlevel 1 (
  echo No s'ha pogut descarregar. En Android Studio pots regenerar el wrapper amb una instal·lacio local de Gradle.
  exit /b 1
)
echo Fet: %DEST%

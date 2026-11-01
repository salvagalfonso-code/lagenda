@echo off
cd /d "%~dp0.."
call gradlew.bat :app:assembleDebug :app:assembleDebugUnitTest --stacktrace
exit /b %errorlevel%

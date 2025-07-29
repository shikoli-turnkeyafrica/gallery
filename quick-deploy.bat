@echo off
echo ====================================
echo    Akili AI - Quick Deploy Script
echo ====================================

REM Set environment (critical step from previous working build)
set ANDROID_HOME=C:\Users\Administrator\AppData\Local\Android\Sdk
echo ANDROID_HOME set to: %ANDROID_HOME%

REM Navigate to Android source
cd Android\src

echo.
echo Step 1: Stopping any running Gradle daemons...
call gradlew --stop

echo.
echo Step 2: Setting PowerShell environment and building...
powershell -Command "$env:ANDROID_HOME = 'C:\Users\Administrator\AppData\Local\Android\Sdk'; .\gradlew.bat installDebug"

echo.
echo ====================================
echo If successful, APK is installed!
echo Check your connected Android device.
echo ====================================
pause
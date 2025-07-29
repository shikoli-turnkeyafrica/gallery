@echo off
REM Akili AI - Automated Deployment Script
REM This script builds and deploys the Akili AI Android app

echo ===============================================
echo        Akili AI - Deployment Script
echo ===============================================
echo.

REM Set Android environment
set ANDROID_HOME=C:\Users\Administrator\AppData\Local\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\tools

echo [1/5] Setting up environment...
echo ANDROID_HOME: %ANDROID_HOME%
echo.

REM Navigate to Android project
cd Android\src

echo [2/5] Cleaning previous builds...
call gradlew clean
if %ERRORLEVEL% neq 0 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)
echo.

echo [3/5] Building debug APK...
call gradlew assembleDebug
if %ERRORLEVEL% neq 0 (
    echo ERROR: Debug build failed!
    echo Check Android SDK installation and try again.
    pause
    exit /b 1
)
echo.

echo [4/5] Building release APK...
call gradlew assembleRelease
if %ERRORLEVEL% neq 0 (
    echo WARNING: Release build failed, but debug APK is available.
    echo Debug APK location: app\build\outputs\apk\debug\app-debug.apk
    pause
    goto :end
)
echo.

echo [5/5] Deployment complete!
echo.
echo ===============================================
echo          BUILD SUCCESSFUL!
echo ===============================================
echo.
echo APK Locations:
echo Debug:   app\build\outputs\apk\debug\app-debug.apk
echo Release: app\build\outputs\apk\release\app-release.apk
echo.
echo Ready for installation:
echo adb install app\build\outputs\apk\debug\app-debug.apk
echo.

:end
echo Press any key to exit...
pause > nul
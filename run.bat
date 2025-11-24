@echo off
cls

:: Define variables
set "TARGET_FILE=%APPDATA%\Mindustry\mods\mindustrytoolmindustrytoolmod.zip"
set "BUILD_TOOL=./gradlew jar"
set "JAR_PATH=%~dp0\build\libs\MindustryToolModDesktop.jar"
set "DEST_FOLDER=%APPDATA%\Mindustry\mods"
set "APP_TO_RUN=C:\Games\mindustry-windows-64-bit\Mindustry.exe"

:: Remove specific file if it exists
if exist "%TARGET_FILE%" (
    del "%TARGET_FILE%"
    echo Deleted %TARGET_FILE%
) else (
    echo File %TARGET_FILE% not found.
)

:: Build the JAR using Gradle
echo Building JAR...
call %BUILD_TOOL%

:: Check if JAR was built
if not exist "%JAR_PATH%" (
    echo JAR build failed!
    exit /b 1
)

:: Copy JAR to destination folder
echo Copying %JAR_PATH% to %DEST_FOLDER%...
copy "%JAR_PATH%" "%DEST_FOLDER%" /y

:: Run the specified application
echo Running %APP_TO_RUN%...
start "" "%APP_TO_RUN%"

echo Done.

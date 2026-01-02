@echo off
cls

:: Define variables
set "TARGET_FILE=%APPDATA%\Mindustry\mods\mindustrytoolmindustrytoolmod.zip"
set "BUILD_TOOL=./gradlew jar"
set "JAR_PATH=%~dp0\build\libs\MindustryToolMod-MizuharaDEVDesktop.jar"
set "DEST_FOLDER=%APPDATA%\Mindustry\mods"
set "GAME_PATH=C:\Users\syste\Desktop\Mindustry.exe.lnk"

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

:: Run Mindustry
echo Deployment complete.
echo Launching Mindustry...
if exist "%GAME_PATH%" (
    echo Starting Mindustry...
    start "" "%GAME_PATH%"
) else (
    echo Mindustry executable not found at %GAME_PATH%. Please check the path.
)
pause
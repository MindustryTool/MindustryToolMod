@echo off
cls

:: Use %APPDATA% for user data and remember last selected launcher
set "BASE_APPDATA=%APPDATA%"
set "TARGET_FILE=%BASE_APPDATA%\Mindustry\mods\mindustrytoolmindustrytoolmod.zip"
set "BUILD_TOOL=.\gradlew jar"
set "JAR_PATH=%~dp0\build\libs\MindustryToolModDesktop.jar"
set "DEST_FOLDER=%BASE_APPDATA%\Mindustry\mods"
set "LAST_PATH_FILE=%BASE_APPDATA%\Mindustry\lastpath.txt"

:: Ensure folder for storing last path exists
if not exist "%BASE_APPDATA%\Mindustry" (
    mkdir "%BASE_APPDATA%\Mindustry"
)

:: Read last saved path (if any)
set "SAVED_PATH="
if exist "%LAST_PATH_FILE%" (
    REM Read the first line (the saved path or directory) reliably
    for /f "usebackq delims=" %%a in ("%LAST_PATH_FILE%") do set "SAVED_PATH=%%~a"
)

set "DEFAULT_DIR="
if defined SAVED_PATH (
    if exist "%SAVED_PATH%" (
        REM If SAVED_PATH is a directory, use it as default dir
        if exist "%SAVED_PATH%\" (
            set "DEFAULT_DIR=%SAVED_PATH%"
        ) else (
            REM It's a file: use its directory as default and set it to be run directly
            for %%D in ("%SAVED_PATH%") do set "DEFAULT_DIR=%%~dpD"
            set "APP_TO_RUN=%SAVED_PATH%"
        )
    )
)

:: Ask user to pick mindustry.exe or mindustry.jar using a GUI file dialog (PowerShell)
if not defined APP_TO_RUN (
    for /f "usebackq delims=" %%I in (`powershell -NoProfile -STA -Command "Add-Type -AssemblyName System.Windows.Forms; $ofd = New-Object System.Windows.Forms.OpenFileDialog; $ofd.Filter = 'Mindustry executables or jars (*.exe;*.jar)|*.exe;*.jar|All files (*.*)|*.*'; $ofd.InitialDirectory = '%DEFAULT_DIR%'; $ofd.Title = 'Chọn mindustry.exe hoặc mindustry.jar'; if($ofd.ShowDialog() -eq 'OK'){ Write-Output $ofd.FileName }"`) do set "APP_TO_RUN=%%I"
    if not defined APP_TO_RUN (
        echo Khong co file duoc chon. Thoat.
        pause
        exit /b 1
    )
) else (
    echo Using saved application: %APP_TO_RUN%
)

:: Save the chosen path for next time (store full path so we can run it directly next time)
> "%LAST_PATH_FILE%" echo %APP_TO_RUN%

:: Remove specific file if it exists
if exist "%TARGET_FILE%" (
    del "%TARGET_FILE%"
    echo Deleted %TARGET_FILE%
) else (
    echo File %TARGET_FILE% do not exist.
)

:: Build the JAR using Gradle
echo Building JAR...
call %BUILD_TOOL%

:: Check if JAR was built
if not exist "%JAR_PATH%" (
    echo JAR build failed!
    pause
    exit /b 1
)

:: Ensure destination mods folder exists
if not exist "%DEST_FOLDER%" (
    mkdir "%DEST_FOLDER%"
)

:: Copy JAR to destination folder
echo Copying %JAR_PATH% to %DEST_FOLDER%...
copy "%JAR_PATH%" "%DEST_FOLDER%" /y

:: Run the selected application (.exe -> start, .jar -> java -jar)
echo Running %APP_TO_RUN%...
set "ext=%APP_TO_RUN:~-4%"
if /I "%ext%"==".jar" (
    start "" javaw -jar "%APP_TO_RUN%"
) else (
    start "" "%APP_TO_RUN%"
)

echo Done.
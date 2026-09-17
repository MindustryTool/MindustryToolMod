@echo off
cls

:: Use %APPDATA% for user data and remember last selected launcher
set "BASE_APPDATA=%APPDATA%"
set "TARGET_FILE=%BASE_APPDATA%\Mindustry\mods\mindustrytoolmindustrytoolmod.zip"
set "BUILD_TOOL=.\gradlew :mod:jar"
set "JAR_PATH=%~dp0\mod\build\libs\MindustryToolModDesktop.jar"
set "DEST_FOLDER=%BASE_APPDATA%\Mindustry\mods"
set "LAST_PATH_FILE=%BASE_APPDATA%\Mindustry\lastpath.txt"

:: Ensure folder for storing last path exists
if not exist "%BASE_APPDATA%\Mindustry" (
    mkdir "%BASE_APPDATA%\Mindustry"
)

:: Read last saved path (if any)
set "SAVED_PATH="
if exist "%LAST_PATH_FILE%" (
    for /f "usebackq delims=" %%a in ("%LAST_PATH_FILE%") do (
        set "SAVED_PATH=%%~a"
    )
)

:: Determine default directory / application to run
set "DEFAULT_DIR="
set "APP_TO_RUN="

if defined SAVED_PATH (
    if exist "%SAVED_PATH%" (
        :: If SAVED_PATH is a directory, use it as default directory
        if exist "%SAVED_PATH%\" (
            set "DEFAULT_DIR=%SAVED_PATH%"
        ) else (
            :: It's a file: use its directory and run it directly
            for %%D in ("%SAVED_PATH%") do set "DEFAULT_DIR=%%~dpD"
            set "APP_TO_RUN=%SAVED_PATH%"
        )
    )
)

:: Ask user to pick mindustry.exe or mindustry.jar using a GUI file dialog
if not defined APP_TO_RUN (
    for /f "usebackq delims=" %%I in (`powershell -NoProfile -STA -Command "Add-Type -AssemblyName System.Windows.Forms; $ofd = New-Object System.Windows.Forms.OpenFileDialog; $ofd.Filter = 'Mindustry executables or jars (*.exe;*.jar)|*.exe;*.jar|All files (*.*)|*.*'; $ofd.InitialDirectory = '%DEFAULT_DIR%'; $ofd.Title = 'Chọn mindustry.exe hoặc mindustry.jar'; if($ofd.ShowDialog() -eq 'OK'){ Write-Output $ofd.FileName }"`) do (
        set "APP_TO_RUN=%%I"
    )

    if not defined APP_TO_RUN (
        echo Khong co file duoc chon. Thoat.
        pause
        exit /b 1
    )
) else (
    echo Using saved application:
    echo %APP_TO_RUN%
)

:: Save the chosen path for next time
> "%LAST_PATH_FILE%" echo %APP_TO_RUN%

:: Remove specific file if it exists
if exist "%TARGET_FILE%" (
    del "%TARGET_FILE%"
    echo Deleted %TARGET_FILE%
) else (
    echo File %TARGET_FILE% does not exist.
)

:: Build the JAR using Gradle
echo.
echo Building JAR...
call %BUILD_TOOL%

if %ERRORLEVEL% neq 0 (
    echo.
    echo Build failed!
    pause
    exit /b 1
)

:: Check if JAR was built
if not exist "%JAR_PATH%" (
    echo.
    echo JAR build failed!
    pause
    exit /b 1
)

:: Ensure destination mods folder exists
if not exist "%DEST_FOLDER%" (
    mkdir "%DEST_FOLDER%"
)

:: Copy JAR to destination folder
echo.
echo Copying %JAR_PATH% to %DEST_FOLDER%...
copy "%JAR_PATH%" "%DEST_FOLDER%" /y

:: Copy to saves\mods if it exists
if exist "%DEFAULT_DIR%saves\mods" (
    echo.
    echo Copying %JAR_PATH% to %DEFAULT_DIR%saves\mods...
    copy "%JAR_PATH%" "%DEFAULT_DIR%saves\mods" /y
)

:: Run the selected application
echo.
echo Running %APP_TO_RUN%...

set "ext=%APP_TO_RUN:~-4%"

if /I "%ext%"==".jar" (
    :: Start the JAR and get its process ID
    for /f "delims=" %%P in ('powershell -NoProfile -Command "$p = Start-Process -FilePath 'javaw.exe' -ArgumentList '-Dsolim.mcp.enabled=true','-jar','%APP_TO_RUN%' -WorkingDirectory '%DEFAULT_DIR%' -PassThru; $p.Id"') do (
        set "APP_PID=%%P"
    )
) else (
    :: Start the EXE and get its process ID
    for /f "delims=" %%P in ('powershell -NoProfile -Command "$p = Start-Process -FilePath '%APP_TO_RUN%' -WorkingDirectory '%DEFAULT_DIR%' -PassThru; $p.Id"') do (
        set "APP_PID=%%P"
    )
)

:: Check that the process started
if not defined APP_PID (
    echo.
    echo Failed to start Mindustry.
    pause
    exit /b 1
)

echo Mindustry started with PID %APP_PID%.
echo.
echo Waiting for Mindustry to close...

:: Wait until the exact process we started exits
powershell -NoProfile -Command "Wait-Process -Id %APP_PID%"

echo.
echo Mindustry has been closed.
echo Done.

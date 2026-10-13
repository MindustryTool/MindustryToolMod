@echo off
cls
setlocal

:: ============================================================
:: Configuration
:: ============================================================

set "BASE_APPDATA=%APPDATA%"
set "TARGET_FILE=%BASE_APPDATA%\Mindustry\mods\mindustrytoolmindustrytoolmod.zip"
set "BUILD_TOOL=.\gradlew :mod:jar"
set "JAR_PATH=%~dp0\mod\build\libs\MindustryToolModDesktop.jar"
set "DEST_FOLDER=%BASE_APPDATA%\Mindustry\mods"
set "LAST_PATH_FILE=%BASE_APPDATA%\Mindustry\lastpath.txt"

:: ============================================================
:: Ensure Mindustry folder exists
:: ============================================================

if not exist "%BASE_APPDATA%\Mindustry" (
    mkdir "%BASE_APPDATA%\Mindustry"
)

:: ============================================================
:: Read last saved path
:: ============================================================

set "SAVED_PATH="

if exist "%LAST_PATH_FILE%" (
    for /f "usebackq delims=" %%A in ("%LAST_PATH_FILE%") do (
        set "SAVED_PATH=%%A"
    )
)

:: ============================================================
:: Determine default directory / application
:: ============================================================

set "DEFAULT_DIR="
set "APP_TO_RUN="

if defined SAVED_PATH (
    if exist "%SAVED_PATH%" (

        if exist "%SAVED_PATH%\" (
            set "DEFAULT_DIR=%SAVED_PATH%"
        ) else (
            for %%D in ("%SAVED_PATH%") do (
                set "DEFAULT_DIR=%%~dpD"
            )

            set "APP_TO_RUN=%SAVED_PATH%"
        )
    )
)

:: ============================================================
:: Select Mindustry executable
:: ============================================================

if not defined APP_TO_RUN (

    for /f "usebackq delims=" %%I in (`powershell -NoProfile -STA -Command "Add-Type -AssemblyName System.Windows.Forms; $ofd = New-Object System.Windows.Forms.OpenFileDialog; $ofd.Filter = 'Mindustry executables or jars (*.exe;*.jar)|*.exe;*.jar|All files (*.*)|*.*'; if ('%DEFAULT_DIR%' -ne '') { $ofd.InitialDirectory = '%DEFAULT_DIR%' }; $ofd.Title = 'Chọn mindustry.exe hoặc mindustry.jar'; if($ofd.ShowDialog() -eq 'OK'){ Write-Output $ofd.FileName }"`) do (
        set "APP_TO_RUN=%%I"
    )

    if not defined APP_TO_RUN (
        echo.
        echo Khong co file duoc chon. Thoat.
        pause
        exit /b 1
    )

) else (
    echo Using saved application:
    echo %APP_TO_RUN%
)

:: ============================================================
:: Save selected path
:: ============================================================

> "%LAST_PATH_FILE%" echo %APP_TO_RUN%

:: ============================================================
:: Remove old mod
:: ============================================================

if exist "%TARGET_FILE%" (
    del "%TARGET_FILE%"
    echo Deleted %TARGET_FILE%
) else (
    echo File %TARGET_FILE% does not exist.
)

:: ============================================================
:: Build
:: ============================================================

echo.
echo Building JAR...

call %BUILD_TOOL%

if %ERRORLEVEL% neq 0 (
    echo.
    echo Build failed!
    pause
    exit /b 1
)

:: ============================================================
:: Check JAR
:: ============================================================

if not exist "%JAR_PATH%" (
    echo.
    echo JAR build failed!
    pause
    exit /b 1
)

:: ============================================================
:: Ensure mods directory exists
:: ============================================================

if not exist "%DEST_FOLDER%" (
    mkdir "%DEST_FOLDER%"
)

:: ============================================================
:: Copy JAR to Mindustry mods
:: ============================================================

echo.
echo Copying %JAR_PATH% to %DEST_FOLDER%...

copy "%JAR_PATH%" "%DEST_FOLDER%" /y

:: ============================================================
:: Copy JAR to saves\mods
:: ============================================================

if exist "%DEFAULT_DIR%saves\mods" (
    echo.
    echo Copying %JAR_PATH% to %DEFAULT_DIR%saves\mods...

    copy "%JAR_PATH%" "%DEFAULT_DIR%saves\mods" /y
)

:: ============================================================
:: Launch Mindustry
::
:: PowerShell automatically determines the PID of the CMD
:: process that launched it.
::
:: If this batch script is terminated, the CMD process disappears.
:: PowerShell then terminates Mindustry.
:: ============================================================

echo.
echo Running %APP_TO_RUN%...
echo.

if /I "%APP_TO_RUN:~-4%"==".jar" (

    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$parentPid = (Get-CimInstance Win32_Process -Filter ('ProcessId=' + $PID)).ParentProcessId; " ^
        "$mindustry = Start-Process -FilePath 'javaw.exe' -ArgumentList '-Dsolim.mcp.enabled=true','-Dsolim.debug.enabled=true','-jar','%APP_TO_RUN%' -WorkingDirectory '%DEFAULT_DIR%' -PassThru; " ^
        "Write-Host ('Mindustry started with PID ' + $mindustry.Id); " ^
        "while (-not $mindustry.HasExited) { " ^
        "    Start-Sleep -Milliseconds 500; " ^
        "    if (-not (Get-Process -Id $parentPid -ErrorAction SilentlyContinue)) { " ^
        "        Write-Host 'Launcher terminated. Stopping Mindustry...'; " ^
        "        Stop-Process -Id $mindustry.Id -Force -ErrorAction SilentlyContinue; " ^
        "        break; " ^
        "    } " ^
        "}"

) else (

    :: Mindustry.exe is a launcher stub, so -D flags cannot be appended to it. The bundled
    :: JRE beside it can, so prefer launching that directly: it is the only way the
    :: solim.debug.enabled flag reaches the JVM, and java.exe keeps a console attached so
    :: the "[Solim] ... enabled via system property" line is actually visible. Falls back
    :: to the plain executable when the distribution has no bundled jre\desktop.jar.
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$parentPid = (Get-CimInstance Win32_Process -Filter ('ProcessId=' + $PID)).ParentProcessId; " ^
        "$dir = '%DEFAULT_DIR%'; " ^
        "$jreJava = Join-Path $dir 'jre\bin\java.exe'; " ^
        "$jreJar = Join-Path $dir 'jre\desktop.jar'; " ^
        "if ((Test-Path $jreJava) -and (Test-Path $jreJar)) { " ^
        "    Write-Host 'Launching bundled JRE with Solim debug flags...'; " ^
        "    $mindustry = Start-Process -FilePath $jreJava -ArgumentList '-Dsolim.mcp.enabled=true','-Dsolim.debug.enabled=true','-jar',$jreJar -WorkingDirectory $dir -PassThru; " ^
        "} else { " ^
        "    Write-Host 'Bundled JRE not found - launching Mindustry.exe WITHOUT Solim debug flags.'; " ^
        "    $mindustry = Start-Process -FilePath '%APP_TO_RUN%' -WorkingDirectory $dir -PassThru; " ^
        "} " ^
        "Write-Host ('Mindustry started with PID ' + $mindustry.Id); " ^
        "while (-not $mindustry.HasExited) { " ^
        "    Start-Sleep -Milliseconds 500; " ^
        "    if (-not (Get-Process -Id $parentPid -ErrorAction SilentlyContinue)) { " ^
        "        Write-Host 'Launcher terminated. Stopping Mindustry...'; " ^
        "        Stop-Process -Id $mindustry.Id -Force -ErrorAction SilentlyContinue; " ^
        "        break; " ^
        "    } " ^
        "}"
)

echo.
echo Mindustry has been closed.
echo Done.

endlocal

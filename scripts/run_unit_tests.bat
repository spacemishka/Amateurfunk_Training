@echo off
echo ===================================================
echo Ausfuehrung der Unit-Tests (JVM)
echo ===================================================
cd /d "%~dp0\.."
call gradlew.bat test --info
if %ERRORLEVEL% EQU 0 (
    echo [ERFOLG] Alle Unit-Tests wurden erfolgreich bestanden!
    echo Report: app\build\reports\tests\testDebugUnitTest\index.html
) else (
    echo [FEHLER] Ein oder mehrere Unit-Tests sind fehlgeschlagen.
)
exit /b %ERRORLEVEL%

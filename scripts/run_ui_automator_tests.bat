@echo off
echo ===================================================
echo Ausfuehrung der UI-Automator-Tests (Android Device)
echo ===================================================
cd /d "%~dp0\.."

echo Pruefe verbundene Geraete:
adb devices
echo.

call gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.spacemishka.app.amateurfunktraining.AmateurfunkUiAutomatorTest
if %ERRORLEVEL% EQU 0 (
    echo [ERFOLG] Alle UI-Automator-Tests wurden auf dem Geraet erfolgreich bestanden!
    echo Report: app\build\reports\androidTests\connected\index.html
) else (
    echo [FEHLER] UI-Automator-Testlauf fehlgeschlagen.
)
exit /b %ERRORLEVEL%

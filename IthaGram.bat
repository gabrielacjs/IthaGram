@'
@echo off
setlocal
cd /d "C:\Users\GabiS\Desktop\IthaGram"

set "APK="
for /f "delims=" %%f in ('dir /b /s /o-d "TMessagesProj_App\build\outputs\apk\afat\release\*.apk" 2^>nul') do (
    set "APK=%%f"
    goto encontrado
)
for /f "delims=" %%f in ('dir /b /s /o-d "TMessagesProj_App\build\outputs\apk\*.apk" 2^>nul') do (
    set "APK=%%f"
    goto encontrado
)

echo No encontre ningun APK compilado. Compila primero con:
echo   .\gradlew.bat :TMessagesProj_App:assembleAfatRelease
goto fin

:encontrado
echo Instalando: %APK%
adb install -r "%APK%"

:fin
echo.
pause
'@ | Set-Content -Encoding ascii "$env:USERPROFILE\Desktop\instalar.bat"
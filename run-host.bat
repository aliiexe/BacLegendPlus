@echo off
REM Lance l'instance HOTE pour tester le multijoueur
REM Cette version fonctionne SANS Maven

title BacLegend - HOTE
color 0A

echo.
echo ===================================================
echo    INSTANCE HOTE ^(Serveur^)
echo ===================================================
echo.
echo Instructions:
echo   1. Une fenetre JavaFX va s'ouvrir
echo   2. Cliquez sur "Jouer en Multijoueur"
echo   3. Cliquez sur "Creer le Serveur"
echo   4. Attendez qu'un client se connecte
echo.
echo Demarrage de l'application...
echo.

cd /d "%~dp0"

REM Chercher javaw.exe pour lancer l'app JavaFX
for /f "delims=" %%%%A in ('where javaw.exe 2^>nul') do set JAVAW=%%%%A

if not defined JAVAW (
    echo ERREUR: javaw.exe non trouve
    echo Assurez-vous que Java est installe et dans le PATH
    pause
    exit /b 1
)

REM Lancer depuis l'IDE est plus simple
echo Note: Pour la meilleure experience, lancez depuis votre IDE:
echo   IntelliJ / Eclipse / VS Code
echo   Clic droit sur App.java ^> Run
echo.
echo Alternativement, utilisez Maven:
echo   mvn clean javafx:run
echo.

pause

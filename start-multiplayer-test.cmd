@echo off
REM Lance automatiquement 2 instances pour tester le multijoueur
REM Usage: double-cliquer sur ce fichier

cls
echo ============================================================
echo    LANCEMENT DU TEST MULTIJOUEUR - BacLegend
echo ============================================================
echo.
echo Etape 1: Nettoyage du port 12345...
echo.

cd /d "%~dp0"

REM Nettoyer le port si necessaire
for /f "tokens=5" %%%%a in ('netstat -ano ^| find ":12345" ^| find "LISTENING"') do (
    echo Port 12345 en cours d'utilisation par PID: %%%%a
    taskkill /PID %%%%a /F >nul 2>&1
    echo Processus termine.
)

echo.
echo Etape 2: Lancement de l'instance HOTE...
echo.
echo Une fenetre verte va s'ouvrir.
echo Dedans: Cliquez "Jouer en Multijoueur" ^> "Creer le Serveur"
echo.

start "BacLegend - HOTE" run-host.bat

timeout /t 5 /nobreak >nul

echo Etape 3: Lancement de l'instance CLIENT...
echo.
echo Une fenetre bleue va s'ouvrir.
echo Dedans: Cliquez "Jouer en Multijoueur" ^> "Rejoindre"
echo.

start "BacLegend - CLIENT" run-client.bat

echo.
echo ============================================================
echo  Deux fenetres ont ete ouvertes:
echo  - Fenetre VERTE = HOTE ^(serveur^)
echo  - Fenetre BLEUE = CLIENT
echo.
echo  Suivez les instructions dans chaque fenetre!
echo.
echo  Les deux joueurs devraient passer a l'ecran de jeu!
echo ============================================================
echo.
pause

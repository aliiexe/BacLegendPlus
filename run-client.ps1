# Lance l'instance CLIENT pour le multijoueur
# Usage: powershell -ExecutionPolicy Bypass -File run-client.ps1

Write-Host "===================================================" -ForegroundColor Blue
Write-Host "    INSTANCE CLIENT" -ForegroundColor Blue
Write-Host "===================================================" -ForegroundColor Blue
Write-Host ""
Write-Host "Instructions:" -ForegroundColor Yellow
Write-Host "  1. Cliquez sur 'Jouer en Multijoueur'" -ForegroundColor White
Write-Host "  2. Vérifiez que l'IP est '127.0.0.1'" -ForegroundColor White
Write-Host "  3. Cliquez sur 'Rejoindre'" -ForegroundColor White
Write-Host ""
Write-Host "Démarrage de l'application..." -ForegroundColor Cyan
Write-Host ""

& mvn javafx:run

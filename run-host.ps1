# Lance l'instance HÔTE pour le multijoueur
# Usage: powershell -ExecutionPolicy Bypass -File run-host.ps1

Write-Host "===================================================" -ForegroundColor Green
Write-Host "    INSTANCE HÔTE (Serveur)" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Instructions:" -ForegroundColor Yellow
Write-Host "  1. Cliquez sur 'Jouer en Multijoueur'" -ForegroundColor White
Write-Host "  2. Cliquez sur 'Créer le Serveur'" -ForegroundColor White
Write-Host "  3. Attendez qu'un client se connecte" -ForegroundColor White
Write-Host ""
Write-Host "Démarrage de l'application..." -ForegroundColor Cyan
Write-Host ""

& mvn javafx:run

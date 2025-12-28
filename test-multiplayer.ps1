# Script de test rapide du mode multijoueur BacLegend
# Usage: .\test-multiplayer.ps1

Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "    TEST RAPIDE DU MODE MULTIJOUEUR - BacLegend" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Vérifier si le port 12345 est libre
Write-Host "[1/4] Vérification du port 12345..." -ForegroundColor Yellow
$portUsed = netstat -ano | Select-String ":12345"
if ($portUsed) {
    Write-Host "⚠️  ATTENTION: Le port 12345 est déjà utilisé!" -ForegroundColor Red
    Write-Host "Processus utilisant le port:" -ForegroundColor Red
    Write-Host $portUsed
    Write-Host ""
    $kill = Read-Host "Voulez-vous tuer ce processus? (O/N)"
    if ($kill -eq "O" -or $kill -eq "o") {
        $pid = ($portUsed[0] -split '\s+')[-1]
        taskkill /PID $pid /F
        Write-Host "✓ Processus terminé" -ForegroundColor Green
        Start-Sleep -Seconds 2
    } else {
        Write-Host "❌ Impossible de continuer avec le port occupé" -ForegroundColor Red
        exit
    }
} else {
    Write-Host "✓ Port 12345 disponible" -ForegroundColor Green
}

Write-Host ""
Write-Host "[2/4] Compilation du projet..." -ForegroundColor Yellow
mvn clean package -q
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Compilation réussie" -ForegroundColor Green
} else {
    Write-Host "❌ Erreur de compilation" -ForegroundColor Red
    exit
}

Write-Host ""
Write-Host "[3/4] Lancement de l'instance HÔTE..." -ForegroundColor Yellow
Write-Host "➜ Une fenêtre Java va s'ouvrir" -ForegroundColor Cyan
Write-Host "➜ Cliquez sur: Jouer en Multijoueur > Créer le Serveur" -ForegroundColor Cyan
Start-Process "java" -ArgumentList "-jar", "target\BacLengendPlus-1.0-SNAPSHOT.jar" -WindowStyle Normal
Start-Sleep -Seconds 3

Write-Host ""
Write-Host "[4/4] Lancement de l'instance CLIENT..." -ForegroundColor Yellow
Write-Host "➜ Une deuxième fenêtre Java va s'ouvrir" -ForegroundColor Cyan
Write-Host "➜ Cliquez sur: Jouer en Multijoueur > Rejoindre (IP: 127.0.0.1)" -ForegroundColor Cyan
Start-Sleep -Seconds 2
Start-Process "java" -ArgumentList "-jar", "target\BacLengendPlus-1.0-SNAPSHOT.jar" -WindowStyle Normal

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "✓ Deux instances lancées!" -ForegroundColor Green
Write-Host ""
Write-Host "ÉTAPES SUIVANTES:" -ForegroundColor Yellow
Write-Host "  1. Dans la PREMIÈRE fenêtre → Multijoueur → Créer le Serveur" -ForegroundColor White
Write-Host "  2. Dans la DEUXIÈME fenêtre → Multijoueur → Rejoindre" -ForegroundColor White
Write-Host "  3. Les deux joueurs devraient être redirigés vers le jeu!" -ForegroundColor White
Write-Host ""
Write-Host "Pour voir les messages réseau, lancez manuellement depuis l'IDE" -ForegroundColor Gray
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan

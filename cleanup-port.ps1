# Script pour nettoyer le port 12345 si bloqué
# Usage: powershell -ExecutionPolicy Bypass -File cleanup-port.ps1

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  NETTOYAGE DU PORT 12345" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host ""

$portUsed = netstat -ano | Select-String ":12345"

if ($portUsed) {
    Write-Host "Processus trouvés utilisant le port 12345:" -ForegroundColor Yellow
    $portUsed | ForEach-Object {
        Write-Host "  $_" -ForegroundColor Gray
    }
    
    Write-Host ""
    $portUsed | ForEach-Object {
        $parts = $_ -split '\s+'
        $pid = $parts[-1]
        
        if ($pid -match '^\d+$') {
            Write-Host "Arrêt du processus PID: $pid" -ForegroundColor Yellow
            taskkill /PID $pid /F 2>$null | Out-Null
            Write-Host "  OK: Processus $pid terminé" -ForegroundColor Green
        }
    }
    
    Start-Sleep -Seconds 2
    
    $stillUsed = netstat -ano | Select-String ":12345"
    if (-not $stillUsed) {
        Write-Host ""
        Write-Host "SUCCESS: Port 12345 libéré!" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "WARNING: Le port est toujours utilisé" -ForegroundColor Red
    }
} else {
    Write-Host "OK: Le port 12345 est déjà libre!" -ForegroundColor Green
}

Write-Host ""

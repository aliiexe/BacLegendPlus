# 🎮 TEST MULTIJOUEUR RAPIDE

## ⚡ MÉTHODE LA PLUS SIMPLE

### Windows:
```cmd
Double-cliquez sur: start-multiplayer-test.cmd
```

Cela va:
1. ✅ Nettoyer le port 12345
2. ✅ Ouvrir 2 fenêtres PowerShell (Hôte VERT + Client BLEU)
3. ✅ Lancer 2 instances de l'application
4. ✅ Afficher les instructions et logs réseau


## 📝 INSTRUCTIONS

### Fenêtre HÔTE (verte):
1. Cliquez sur **"Jouer en Multijoueur"**
2. Cliquez sur **"Créer le Serveur"**
3. Attendez → Status: "Serveur démarré ! En attente de connexion..."

### Fenêtre CLIENT (bleue):
1. Cliquez sur **"Jouer en Multijoueur"**
2. Vérifiez que l'IP est **127.0.0.1**
3. Cliquez sur **"Rejoindre"**

### ✅ SUCCÈS = Les deux fenêtres passent à l'écran de jeu!


## 🛠️ MÉTHODE MANUELLE (2 terminaux)

### Terminal 1:
```powershell
.\run-host.ps1
```

### Terminal 2 (NOUVEAU terminal):
```powershell
.\run-client.ps1
```


## 🐛 SI LE PORT EST BLOQUÉ

```powershell
.\cleanup-port.ps1
```


## 📚 GUIDE COMPLET

Voir: `MULTIPLAYER_TEST_GUIDE.txt` pour plus de détails

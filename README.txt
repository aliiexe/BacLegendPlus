╔════════════════════════════════════════════════════════════════════╗
║                                                                    ║
║          GUIDE D'UTILISATION - TEST MULTIJOUEUR BACLEGEND         ║
║                                                                    ║
╚════════════════════════════════════════════════════════════════════╝


⚡ SOLUTION LA PLUS SIMPLE (RECOMMANDÉE)
════════════════════════════════════════════════════════════════════

Voir: LANCER-DEPUIS-IDE.txt

Résumé:
1. Ouvrez votre IDE (IntelliJ, Eclipse ou VS Code)
2. Ouvrez le projet BacLengendPlus
3. Clic droit sur App.java > Run
4. Répétez pour lancer une deuxième instance
5. Suivez les instructions de multijoueur

✓ Pas besoin d'installer Maven
✓ Fonctionne immédiatement



════════════════════════════════════════════════════════════════════
📋 FICHIERS DE CE DOSSIER
════════════════════════════════════════════════════════════════════

LANCER-DEPUIS-IDE.txt ← LISEZ CECI! (Solution simple et rapide)

start-multiplayer-test.cmd    ← Nécessite Maven
run-host.bat                  ← Nécessite Maven
run-client.bat                ← Nécessite Maven
cleanup-port.ps1              ← Nettoie le port 12345

QUICK-START.txt               ← Guide rapide
SETUP-MAVEN.txt               ← Si vous installez Maven
README-MULTIPLAYER.md         ← Guide Markdown
HOW-TO-TEST-MULTIPLAYER.txt   ← Guide détaillé


════════════════════════════════════════════════════════════════════
🚀 OPTION 1: LANCER DEPUIS VOTRE IDE (RECOMMANDÉ - NO MAVEN)
════════════════════════════════════════════════════════════════════

ÉTAPE 1: Ouvrir le projet
   • IntelliJ: File > Open > [Dossier BacLengendPlus]
   • Eclipse: File > Import > Existing Maven Projects
   • VS Code: File > Open Folder > [Dossier BacLengendPlus]

ÉTAPE 2: Trouver App.java
   Cherchez: src/main/java/com/emsi/baclegend/App.java

ÉTAPE 3: Lancer la PREMIÈRE instance (HÔTE)
   • Clic droit sur App.java
   • Cliquez: Run 'App.main()' (ou Run As > Java Application)
   • Une fenetre JavaFX s'ouvre
   • Cliquez: "Jouer en Multijoueur"
   • Cliquez: "Créer le Serveur"
   • Attendez: "Serveur démarré ! En attente..."

ÉTAPE 4: Lancer la DEUXIÈME instance (CLIENT)
   • Refaites l'Étape 2-3 (Clic droit > Run)
   • Une DEUXIÈME fenetre JavaFX s'ouvre
   • Cliquez: "Jouer en Multijoueur"
   • Vérifiez: IP = "127.0.0.1"
   • Cliquez: "Rejoindre"

ÉTAPE 5: SUCCESS!
   ✅ Les DEUX fenêtres passent à l'écran de jeu
   ✅ Vous voyez la même lettre
   ✅ Test réussi!


════════════════════════════════════════════════════════════════════
🚀 OPTION 2: LANCER AVEC MAVEN (Si Maven est installé)
════════════════════════════════════════════════════════════════════

Vérifiez que Maven est installé:
   mvn --version

Si erreur → Voir SETUP-MAVEN.txt

Double-cliquez sur:
   start-multiplayer-test.cmd

Suivez les instructions dans les 2 fenêtres qui s'ouvrent.


════════════════════════════════════════════════════════════════════
🔧 DÉPANNAGE
════════════════════════════════════════════════════════════════════

❌ PROBLÈME: "Address already in use"
   ✅ SOLUTION: 
   powershell -ExecutionPolicy Bypass -File cleanup-port.ps1

❌ PROBLÈME: Je n'arrive pas à trouver App.java
   ✅ SOLUTION:
   • Assurez-vous que le projet est bien ouvert
   • Cherchez: src/main/java/com/emsi/baclegend/App.java
   • Utilisez Ctrl+N (IntelliJ) ou Ctrl+F (Eclipse) pour chercher

❌ PROBLÈME: "Run is not available"
   ✅ SOLUTION:
   • Fermez les anciennes instances qui tournent
   • Attendez quelques secondes
   • Réessayez

❌ PROBLÈME: "Connection refused"
   ✅ SOLUTION:
   • Lancez D'ABORD l'HÔTE
   • Attendez "En attente..."
   • Puis lancez le CLIENT

❌ PROBLÈME: Les deux fenêtres ne passent pas au jeu
   ✅ SOLUTION:
   • Regardez les messages dans la console IDE
   • Vérifiez que vous avez cliqué les bons boutons
   • Vérifiez l'adresse IP (127.0.0.1)


════════════════════════════════════════════════════════════════════
💡 CONSEILS
════════════════════════════════════════════════════════════════════

• Première tentative?
  La compilation peut prendre quelques secondes, c'est normal

• Meilleures performances?
  Ouvrez l'IDE DEUX FOIS pour voir les deux consoles

• Besoin de déboguer?
  Vous pouvez ajouter des breakpoints directement dans l'IDE!

• Port bloqué?
  Exécutez: cleanup-port.ps1


════════════════════════════════════════════════════════════════════
📚 RESSOURCES
════════════════════════════════════════════════════════════════════

IntelliJ IDEA (Gratuit Community Edition):
   https://www.jetbrains.com/idea/download/

Eclipse IDE (Gratuit):
   https://www.eclipse.org/downloads/

VS Code (Gratuit):
   https://code.visualstudio.com/


════════════════════════════════════════════════════════════════════
✨ RÉSUMÉ RAPIDE
════════════════════════════════════════════════════════════════════

1. ✓ Ouvrez votre IDE
2. ✓ Ouvrez le projet BacLengendPlus
3. ✓ Trouvez App.java
4. ✓ Clic droit > Run
5. ✓ Répétez pour lancer 2e instance
6. ✓ Suivez les instructions de multijoueur

FAIT! Aucun Maven requis!


════════════════════════════════════════════════════════════════════

Version: 2.0 (Optimisé pour fonctionner sans Maven)
Date: Décembre 2025
Projet: BacLegend - Jeu Multijoueur

════════════════════════════════════════════════════════════════════

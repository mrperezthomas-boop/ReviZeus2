# SKILL 05 — CONTRÔLE FINAL AVANT VALIDATION

À coller après génération du code pour forcer Cursor à s’auto-vérifier.

```text
Avant validation finale, fais un contrôle qualité strict de ta propre solution.

Vérifie point par point :
1. aucune variable existante renommée inutilement
2. aucune architecture cassée
3. aucun fichier hors périmètre modifié
4. aucun placeholder parasite
5. aucune ressource inventée
6. cohérence ViewBinding / XML / imports / IDs
7. cohérence lifecycle si audio / vidéo / timer / animation / coroutine
8. orientation respectée : portrait partout sauf aventure paysage
9. compatibilité avec les managers et flows existants
10. fichiers complets fournis

Puis rends-moi :
- les points validés
- les points douteux éventuels
- les tests Android Studio à faire immédiatement
```

## Checklist manuelle Android Studio
- Sync Gradle
- Build debug
- Vérifier imports rouges
- Vérifier IDs XML / ViewBinding
- Vérifier navigation réelle
- Vérifier Logcat au lancement
- Vérifier `onPause/onResume` sur l’écran touché
- Vérifier audio / loading / dialogues si concernés
```

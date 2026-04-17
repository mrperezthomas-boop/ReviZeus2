# SKILL 03 — MODÈLE UNIVERSEL BUG / BUILD / CRASH

## A. Correction build cassé
```text
Corrige uniquement ce problème de build RéviZeus.

Erreur observée :
[COLLER L’ERREUR]

Contraintes :
- correctif minimal
- ne modifie rien d’étranger au bug
- ne renomme aucune variable existante
- pas de refactor global
- génère uniquement les fichiers complets réellement concernés
- si une ressource manque, dis-le explicitement

Je veux :
1. cause racine probable
2. fichiers exacts à corriger
3. correctif minimal
4. points à vérifier dans Android Studio
```

## B. Correction crash runtime
```text
Analyse ce crash RéviZeus et corrige-le avec le patch minimal.

Symptôme / stacktrace / logcat :
[COLLER ICI]

Je veux :
1. cause racine la plus probable
2. fichier(s) réellement responsable(s)
3. correctif minimal compatible avec l’architecture actuelle
4. risques de bord
5. fichiers complets modifiés uniquement
```

## C. Correction bug visuel / navigation
```text
Corrige ce bug fonctionnel RéviZeus sans refactor global.

Bug :
[EXPLIQUER LE BUG]

Écrans concernés :
[ÉCRANS]

Contraintes :
- ne casse rien d’autre
- conserve le flow existant
- fichiers complets modifiés uniquement
- si une ressource manque, liste-la

Donne d’abord le diagnostic, puis applique le correctif minimal.
```

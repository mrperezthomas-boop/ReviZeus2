# SKILL 02 — MODÈLE UNIVERSEL DE MISSION D’IMPLÉMENTATION

À réutiliser en remplaçant seulement les champs utiles.

```text
Mission RéviZeus : [NOM COURT DE LA TÂCHE]

Objectif :
[EXPLIQUER CLAIREMENT CE QUE JE VEUX OBTENIR]

Périmètre autorisé :
- [fichier 1]
- [fichier 2]
- [fichier 3]

Périmètre interdit :
- ne touche pas à [fichier / zone / écran]
- ne refactor pas [zone]
- ne modifie pas [manager / IDs / flow]

Contraintes absolues :
- Kotlin + XML + ViewBinding uniquement
- pas de Compose
- ne renomme aucune variable existante
- ne change aucune architecture sans nécessité absolue
- génère les fichiers complets modifiés
- commente utilement les ajouts
- n’invente aucune ressource
- si une ressource manque, liste-la explicitement à la fin

Résultat attendu :
- [comportement attendu 1]
- [comportement attendu 2]
- [comportement attendu 3]

Avant de coder :
1. rappelle les fichiers touchés
2. rappelle les risques
3. rappelle les ressources manquantes éventuelles
4. puis code

Sortie attendue :
- audit bref
- fichiers complets modifiés
- liste finale des vérifications Android Studio
```

## Variante ajout de feature
```text
Mission RéviZeus : intégrer une nouvelle feature sans casser l’existant.

Commence par vérifier si le projet contient déjà des éléments similaires à réutiliser.
Réutilise le maximum d’existant avant de créer du nouveau.
Ne code qu’après audit.
```

# Règles sacrées RéviZeus

## Règles de base v1 surveillées automatiquement

- Kotlin + XML + ViewBinding uniquement.
- Jetpack Compose interdit.
- Les commentaires horodatés sont requis sur tout nouveau bloc important.
- Les IDs publics sensibles ne doivent pas être renommés sans validation.
- Le code ne doit pas supprimer silencieusement une logique métier existante.
- Les sorties automatiques d'agents ne modifient jamais le code applicatif.

## Format de commentaire horodaté

`// [YYYY-MM-DD HH:MM][BLOC][ZONE] message bref`

Exemple :
`// [2026-04-18 05:10][BLOC_B][RPG_DIALOG] Conversion du feedback toast en dialogue immersif.`

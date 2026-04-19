# JOURNAL IMPORT TOUT ZIP

> Horodatage : 2026-04-19 17:50 local

## Entrée
Import et consolidation de `tout.zip` régénérés après expiration de session. Objectif : reconstruire un pack d'ajouts téléchargeable pour BRAIN_REVIZEUS.

## Résumé action
- archive reparsée,
- doublons Kotlin miroir détectés,
- ajouts documentaires regénérés,
- fichiers exclus non promouvables écartés.

## Décisions
- garder `app/java/com/revizeus/app` comme référence canonique de snapshot,
- ne pas absorber `.gradle`, `.idea`, `local.properties`, caches,
- documenter séparément la sécurité Firestore,
- isoler Continue comme note de workflow, pas comme vérité produit.

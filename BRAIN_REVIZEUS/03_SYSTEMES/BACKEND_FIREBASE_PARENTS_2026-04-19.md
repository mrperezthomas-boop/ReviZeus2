# BACKEND FIREBASE PARENTS

> Horodatage : 2026-04-19 17:50 local

## Signal détecté
Le zip expose au moins un point d'entrée backend/documentation lié aux synthèses parents et aux envois périodiques :
- `ParentSummaryManager.kt`
- présence d'un environnement Firebase projet Android

## Ce qu'on peut ajouter au Brain
### Hypothèse solide mais prudente
Le projet comporte une brique orientée :
- synthèse parentale,
- préparation ou orchestration d'exports / résumés,
- articulation potentielle avec Firebase Functions / Scheduler pour envoi hebdomadaire.

### Formulation recommandée pour le Brain
> Une capacité backend orientée “parents / récapitulatifs hebdomadaires” est présente côté projet. Elle doit rester documentée comme pipeline potentiel Firebase (Functions + Scheduler + email), sans être considérée comme totalement fiabilisée tant que le flux prod n'est pas validé bout en bout.

## Annotation
Cette note enrichit le Brain côté vision backend, mais ne remplace pas un audit réel des Functions, secrets, quotas et provider email.

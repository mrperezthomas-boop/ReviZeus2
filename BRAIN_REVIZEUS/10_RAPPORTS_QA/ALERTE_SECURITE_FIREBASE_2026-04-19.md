# ALERTE SECURITE FIREBASE

> Horodatage : 2026-04-19 17:50 local

## Verdict
Le zip contient un fichier `firestore.rules`. Il doit être considéré comme **zone à audit obligatoire** avant toute promotion documentaire.

## Risque
- règles temporaires de debug possibles,
- surface de lecture/écriture trop ouverte,
- confusion entre état local de travail et politique sécurité réelle voulue pour la prod.

## Observation
Alerte “règles ouvertes détectées” : NON / NON CONFIRME

## Recommandation Brain
> Toute règle Firestore trouvée dans un export local doit être documentée comme état technique transitoire jusqu'à validation sécurité explicite. Interdiction de la considérer comme baseline prod sans audit.

## Extrait à vérifier
```

--- tout/android studio/firestore.rules ---
rules_version='2'

service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      // This rule allows anyone with your database reference to view, edit,
      // and delete all data in your database. It is useful for getting
      // started, but it is configured to expire after 30 days because it
      // leaves your app open to attackers. At that time, all client
      // requests to your database will be denied.
      //
      // Make sure to write security rules for your app before that time, or
      // else all client requests to your database will be denied until you
      // update your rules.
      allow read, write: if request.time < timestamp.date(2026, 4, 13);
    }
  }
}
```

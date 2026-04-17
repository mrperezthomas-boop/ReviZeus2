# RéviZeus — Backend Firebase Functions + Scheduler pour emails hebdo parents

## Architecture retenue

- **Cloud Functions for Firebase (2nd gen)** pour le backend serveur
- **Cloud Scheduler** via `onSchedule()` pour lancer le lot hebdomadaire
- **Trigger Email from Firestore** pour l'envoi effectif des emails à partir de documents écrits dans `/mail`
- **Firestore** pour stocker :
  - `parentLinks/{uid}`
  - `weeklySummaryStats/{uid}`
  - `mail/{autoId}`

## Arborescence

```text
revizeus_firebase_backend/
  firebase.json
  firestore.rules
  README.md
  functions/
    package.json
    index.js
```

## 1) Préparer Firebase CLI

```bash
npm install -g firebase-tools
firebase login
firebase use YOUR_PROJECT_ID
```

## 2) Installer les dépendances Functions

Depuis le dossier `functions/` :

```bash
npm install
```

## 3) Déployer les Functions

Depuis la racine du backend :

```bash
firebase deploy --only functions
```

## 4) Installer l'extension d'envoi email

Installer l'extension officielle **Trigger Email from Firestore**.

Collection recommandée :

```text
mail
```

Une fois l'extension installée, la fonction planifiée `sendWeeklyParentSummaries` écrira les emails dans `/mail`, et l'extension les enverra.

## 5) Déployer les règles Firestore

```bash
firebase deploy --only firestore:rules
```

## 6) Fréquence d'envoi actuelle

Le lot tourne :

```text
every monday 07:00
```

Fuseau :

```text
Europe/Paris
```

## 7) Collections côté app Android

### parentLinks/{uid}

```json
{
  "accountEmail": "hero@example.com",
  "childDisplayName": "EaEnki",
  "parentEmail": "parent@example.com",
  "isEnabled": true,
  "createdAt": "serverTimestamp",
  "updatedAt": "serverTimestamp",
  "lastWeeklySummarySentAt": null
}
```

### weeklySummaryStats/{uid}

```json
{
  "weekKey": "2026-W11",
  "quizCount": 12,
  "averageScore": 78.5,
  "xpGained": 640,
  "dayStreak": 5,
  "bestDayStreak": 12,
  "scansCount": 4,
  "savedKnowledgeCount": 7,
  "dominantMood": "Joyeux",
  "studiedSubjects": ["Maths", "SVT", "Philo"],
  "encouragementMessage": "Les dieux saluent sa régularité.",
  "updatedAt": "serverTimestamp"
}
```

## 8) Fonctions exposées

### `syncParentSettings`
Callable.

Rôle :
- crée ou met à jour l'email parent
- active / désactive le résumé hebdo
- stocke le nom affiché de l'enfant

### `syncWeeklySummaryStats`
Callable.

Rôle :
- met à jour les statistiques de semaine agrégées
- peut être appelée après quiz, scan, dashboard, ou sync manuelle

### `sendWeeklyParentSummaries`
Scheduled.

Rôle :
- lit les parents activés
- construit le contenu email texte + HTML
- écrit les documents dans `/mail`

### `observeMailDelivery`
Firestore trigger.

Rôle :
- journalise les changements d'état de livraison de l'extension email
- très utile pour le debug QA

## 9) Ce qu'il faudra brancher côté Android ensuite

1. Authentifier l'utilisateur avec Firebase Auth
2. Appeler `syncParentSettings` quand l'utilisateur sauvegarde la zone Parents
3. Appeler `syncWeeklySummaryStats` après les événements gameplay importants
4. Garder Room comme source locale de gameplay
5. Utiliser Firestore / Functions comme couche cloud parents

## 10) Sécurité

Les règles fournies :
- autorisent l'utilisateur connecté à écrire ses propres `parentLinks/{uid}`
- autorisent l'utilisateur connecté à écrire ses propres `weeklySummaryStats/{uid}`
- bloquent totalement l'accès client à `/mail`

## 11) Ajustements possibles ensuite

- templates email plus riches via l'extension
- segmentation parent / tuteur multiple
- résumé du lundi configurable
- résumés mensuels
- retries et tableau d'historique d'envoi

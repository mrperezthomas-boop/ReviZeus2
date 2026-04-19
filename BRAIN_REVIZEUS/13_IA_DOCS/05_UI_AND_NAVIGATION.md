---
document_id: IA_DOC_05_UI_AND_NAVIGATION
type: tech_reference
stability: stable
truth_level: certain
owner: ui_layout
criticality: P1
version: 1.0
last_updated: 2026-04-18
source: Brain_ReviZeus/13_IA_DOCS/04_UI_AND_NAVIGATION_MAP.md
---

# 05 — UI & NAVIGATION MAP

## Usage IA
Carte de toutes les Activities, transitions d'écran et flux d'onboarding. À consulter avant tout ajout d'Activity ou modification de navigation.

---

# UI & NAVIGATION MAP — RÉFÉRENTIEL OFFICIEL COURANT

## Boot Flow officiel (stable courant)

### Boot principal
`SplashActivity` 
→ `GameUpdateActivity` → `VideoPlayerActivity` **SEULEMENT SI** :
  - release non appliquée
  - app non debuggable
  - installation non initiale
*(Si premier lancement : passage direct au flow normal. Si build dev/debuggable : bypass direct du réalignement).*
→ `TitleScreenActivity`
→ `MainMenuActivity`

### MainMenuActivity
- `NOUVEAU COMPTE` → `LoginActivity` avec `MODE_INSCRIPTION`
- `CHARGER UN COMPTE` → `LoginActivity` avec `MODE_CONNEXION`
- `CHARGER UN HÉROS EXISTANT` → `AccountSelectActivity`

### LoginActivity
- inscription → `AuthActivity` (stockage transitoire seulement)
- connexion → `HeroSelectActivity`

### Onboarding neuf complet
`LoginActivity (inscription)`
→ `AuthActivity`
→ `GenderActivity`
→ `AvatarActivity`
→ `IntroVideoActivity`
→ `MoodActivity`
→ `DashboardActivity`

### Compte existant
`LoginActivity (connexion)`
→ `HeroSelectActivity`
  - slot occupé → `MoodActivity`
  - slot vide → `GenderActivity` → `AvatarActivity` → ... → `DashboardActivity`

### Règle officielle actuelle
- `GameUpdateActivity` : Écran de réalignement local post-release, non affiché au premier lancement, non affiché en mode dev/debuggable, sans changelog, timeout strict anti-blocage.
- Aucun autre boot flow concurrent ne doit être considéré comme officiel tant que la stabilisation auth/audio n’est pas terminée.

---

## Principes de navigation compte / héros

### LoginActivity
Fonctions officielles :
- mode inscription (stockage transitoire seulement, la création Firebase est toujours finalisée en `AvatarActivity`).
- mode connexion : après connexion réussie, forge automatique du code de secours si absent. Affichage du code une seule fois. Affichage de l'indice du code dans le centre local.
- authentification Firebase email / mot de passe.
- lien “Mot de passe perdu ?”.
- bouton œil afficher / masquer le mot de passe.
- centre local de récupération / effacement de compte si activé par l’UI.

Règles :
- le mode doit être forcé par `EXTRA_MODE` quand l’écran source connaît l’intention utilisateur.
- en absence d’extra, l’inscription est le fallback le plus sûr pour éviter les faux écrans de connexion.
- après connexion Firebase réussie, navigation prioritaire vers `HeroSelectActivity`.

### AuthActivity
Fonctions :
- doit autoriser deux entrées valides : session Firebase active OU `OnboardingSession` mémoire valide.
- collecte pseudo / âge / classe.
- prépare les données gameplay locales.
- conserve les informations cloud minimales (email, uid, emailVerified).
- prolonge le flow existant vers `GenderActivity`.

### GenderActivity
- filtre de genre.
- peut lancer une BGM spécifique selon le camp.
- doit transmettre proprement le contexte vers `AvatarActivity`.
- retour téléphone : destination explicite, pas de fermeture sauvage de la pile.

### AvatarActivity
Règle officielle :
- accepte le flux onboarding neuf.
- accepte la création d’un héros supplémentaire sur compte déjà connecté.

Important :
- Revalidation obligatoire de `OnboardingSession` juste avant `createAccount()`.
- Le premier héros d’un compte neuf peut finaliser `createAccount Firebase`. Affichage obligatoire du code de secours après la création du premier compte.
- Un 2e ou 3e héros ne doit jamais recréer le compte Firebase.
- Après validation, le héros doit être enregistré dans Room puis immédiatement reflété dans `AccountRegistry`.
- `OnboardingSession.clear()` appelé uniquement après succès complet.

### HeroSelectActivity
- affiche les 3 slots d’un UID.
- slot occupé : carte héros.
- slot vide : entrée de création de héros supplémentaire.
- doit pouvoir survivre à une perte partielle de cache grâce à la reconstruction locale.
- retour téléphone : destination explicite (menu / titre selon le flow courant).

### AccountSelectActivity
- affiche tous les héros locaux détectés sur l’appareil (accès direct à `MoodActivity`).
- utilisé par “CHARGER UN HÉROS EXISTANT”.
- continuité audio maîtrisée.
- retour téléphone : destination explicite.

---

## DashboardActivity

### IDs ViewBinding à préserver
| ID | Type | Rôle |
|---|---|---|
| `btnSettings` | ImageButton 44dp | Paramètres — déclaré en premier |
| `frameDebugML` | FrameLayout 38dp | Conteneur Debug — sert d’ancre `toStartOf` |
| `btnDebugML` | ImageButton 38dp | Dans `frameDebugML` → ouvre `DebugAnalyticsActivity` |
| `viewDebugDot` | View 10dp rouge | Pulsation `AlphaAnimation` |
| `llRecentBadges` | LinearLayout | Sous `frameDebugML`, badges récents |
| `layoutCurrenciesHud` | LinearLayout | `layout_toStartOf="@id/frameDebugML"` |
| `chipEclat` / `chipAmbroisie` / `chipDayStreak` / `chipWinStreak` | LinearLayout | Clic → `afficherPopupPrometheus()` |
| `chipForge` | FrameLayout 36dp | Vrai bouton HUD → `ForgeActivity` |
| `btnForge` | ImageView 28dp | Icône marteau dans `chipForge` |
| `viewForgeNotif` | View 9dp rouge | Visible si `CraftingSystem.affordableRecipes()` n’est pas vide |

*Nouveau Composant UI* : Ajout du `DashboardInsightsWidget` dans le layout principal.

---

## Nouveau Flow Oracle (Core Loop)
Dashboard
   ↓
OracleActivity
   ↓
[CHOIX]
   → Scan
   → Import
   → Texte libre
         ↓
   OraclePromptActivity (retour → Dashboard, plus de retour caméra)
         ↓
   ResultActivity
         ↓
   QuizActivity
         ↓
   QuizResultActivity
         ↓
   GodMatiereActivity

---

## Training Loop
Dashboard
→ TrainingSelectActivity
  ├─ Mode normal : sélection matière → cours unique → 30 questions
  ├─ Mode Ultime Matière : sélection matière → tous cours matière → 40 questions
  └─ Mode Ultime Global : tous cours toutes matières → 40 questions alternées (via `UltimateQuizBuilder`)
→ TrainingQuizActivity (timer actif par âge, intègre des types de questions variés via `GeminiQuestionTypeHelper`)
→ QuizResultActivity (répartition fragments par matière)

---

## Navigation onboarding / avatar / profil

- `imgAvatarHero.setOnClickListener` ouvre exclusivement `HeroProfileActivity`.
- `GenderActivity` : panneaux de sélection genre, rendus vidéo / image selon la logique métier courante.
- `AvatarActivity` : carrousel premium, fond vidéo MP4 fullscreen, description stylisée, bulle Zeus typewriter, ne change pas la BGM au scroll.
- `MoodActivity` : étape suivante avant `DashboardActivity`.

---

## Sécurité IA et loading

- `BaseActivity` expose `showLoading()` / `hideLoading()` si activé dans le socle courant.
- `QuizResultActivity` protège les corrections divines avec `LoadingDivineDialog`.
- `TrainingSelectActivity` protège la génération des quiz avec `LoadingDivineDialog`.
- `GodMatiereActivity` protège les invocations IA comme la Lyre d’Apollon avec `LoadingDivineDialog`.

---

## ResultActivity

Flow UX officiel :
1. Génération du résumé (support flux `FREE_TEXT_INPUT`, bascule dynamique image/texte).
2. Validation du résumé.
3. Détection automatique matière & titre / Choix du temple.
4. Sauvegarde seule ou sauvegarde + quiz.

Le bouton `btnStartQuiz` doit ouvrir le panneau de choix d’action post-analyse.

---

## ForgeActivity

- Effets de craft autorisés :
  - `lancerParticulesOlympiennes()`
  - `lancerAnimationLottie()` (`lottie_forge_fire`)
- Cartes de recette : `item_recipe_card.xml` avec contraintes actuelles du projet.
- Les recettes craftables doivent être affichées en priorité.

---

## InventoryActivity

- Onglets visibles : `Fragments / Artefacts / Équipements / Objets / Reliques`.
- `Fragments` = affichage actif.
- `Artefacts` = lecture du contenu réel de l’inventaire.
- `Équipements / Objets / Reliques` = panneaux “arrive bientôt” tant qu’ils ne sont pas implémentés.
- Tri via flèche / menu déroulant.
- Clic sur item = détail de l’objet.

---

## SettingsActivity

- Onglet Audio recentré sur les réglages sonores.
- Onglet JukeBox séparé, placé sous `Avancé`.
- Temple des Mélodies = liste scrollable de toutes les pistes.
- Suppression compte Firebase demandant le mot de passe.
- Recommencer l’aventure conserve le compte mais réinitialise la progression si cette fonctionnalité est activée.

---

## Règles transversales audio

- Toute `Activity` à BGM dédiée doit utiliser `SoundManager.playMusicDelayed(..., 300L)` ou le protocole équivalent documenté.
- `SoundManager.rememberMusic(resId)` doit être utilisé quand la scène sonore doit être mémorisée proprement.
- `onPause()` et `onResume()` doivent être pensés comme un couple cohérent, pas comme des relances sauvages.
- Une `Activity` ne doit pas simplement appeler `finish()` comme stratégie de retour si cela vide la pile et casse l’audio.
- Le retour téléphone et le retour UI doivent viser un écran cible explicite.

---

## GodMatiereActivity

- Fond divin de temple : vidéo de fond dédiée à la matière jouée une seule fois puis remplacée par l’image fixe correspondante si ce système est activé.
- La vidéo d’intro du temple ne bloque plus les interactions sur les cours.
- Un header `bg_divine_card` semi-transparent affiche le titre du temple.

---

## QuizResultActivity

- Récompenses visuelles directement sous les étoiles : `Éclats / Ambroisie / Fragments`.
- Affichage des verdicts personnalisés générés par le `UserAnalyticsEngine`.

---

## Règle de cohérence navigation

Tout écran ajouté ou modifié doit documenter explicitement :
- sa destination d’arrivée
- sa destination de retour téléphone
- sa stratégie audio
- sa dépendance éventuelle à un compte / UID / slot
- son rôle dans le flow onboarding / multi-compte / gameplay

---

## EXTENSION OFFICIELLE — QUIZ CHRONOMÉTRÉS PAR QUESTION (AJOUT CONSERVATIF)

Cette section complète les flows quiz existants sans en remplacer la structure.

### QuizActivity (quiz Oracle)
- doit utiliser un timer individuel par question.
- le temps affiché doit être calculé selon l’âge du héros courant.
- chaque bonne réponse doit attribuer +1 fragment de la matière concernée.
- aucun fragment générique ne doit être attribué à la place.

### TrainingQuizActivity
- doit utiliser un timer individuel par question (via `CountDownTimer`).
- le timer doit être réinitialisé à chaque changement de question.
- le temps affiché dépend de l’âge du héros courant.
- en entraînement normal : +1 fragment par bonne réponse lié à la matière.
- en entraînement ultime : +3 fragments par bonne réponse lié à la matière de la question.
- l’entraînement ultime ne doit plus attribuer de fragments panthéon génériques.

### QuizResultActivity
- doit pouvoir afficher un récapitulatif détaillé des fragments gagnés par matière.
- l’affichage global abstrait des gains ne suffit plus pour les flows quiz concernés.
- le récapitulatif doit refléter la réalité question par question si plusieurs matières sont présentes.

### Règle HUD quiz
Le HUD quiz doit prévoir :
- une zone timer visible.
- un reset propre du timer à chaque question.
- un état visuel d’alerte lorsque le temps devient critique (`sfx_timer_alert` <= 5s).

### Règle de comportement en cas d’expiration
Lorsque le temps expire pour une question :
- la question doit être traitée comme une erreur automatique.
- le passage à la suite doit rester cohérent avec l’architecture actuelle.
- aucune régression ne doit être introduite sur les feedbacks divins, les étoiles, l’XP ou la navigation de fin.


Règles de navigation et ownership technique post Bloc A

Les flows sacrés restent inchangés.
Un écran qui lance sa propre BGM devient propriétaire de cette ambiance tant qu’il est au premier plan.
Un retour arrière ne doit pas forcer une BGM étrangère si l’écran de destination sait déjà restaurer sa propre logique.
Les écrans Oracle / Result / Training / Temple doivent annuler ou réattacher proprement loaders, timers, scan, TTS et dialogues lors des transitions.
Toute popup ou message important encore non converti au système RPG doit être listé comme dette Bloc B.

Dette UX encore ouverte

conversion globale des AlertDialog et messages plats,
homogénéisation des feedbacks techniques trop bruts,
centralisation complète des dialogues divins premium.

## Dialogues RPG universels

**Écrans convertis (BLOC B) :**
- ✅ DashboardActivity (4 dialogues)
- ✅ GodMatiereActivity (9 dialogues + 2 toasts)
- ✅ ResultActivity (4 dialogues + 5 toasts)
- ✅ QuizResultActivity (1 dialogue + 4 toasts)
- ✅ OracleActivity (15 toasts → dialogues)
- ✅ TrainingQuizActivity (2 toasts → dialogues)
- ✅ TrainingSelectActivity (5 dialogues + 1 toast)
- ✅ ForgeActivity (3 dialogues + 3 toasts)

**Écrans non convertis (backlog) :**
- ⏳ LoginActivity (6 AlertDialog + 10 Toast)
- ⏳ SettingsActivity (6 AlertDialog + 5 Toast)
- ⏳ InventoryActivity (3 AlertDialog + 1 Toast)
- ⏳ AccountSelectActivity (2 AlertDialog + 4 Toast)
- ⏳ HeroSelectActivity (2 AlertDialog + 1 Toast)
- ⏳ Autres écrans secondaires

**Template universel :** dialog_rpg_universal.xml
- Portrait chibi animé
- Texte typewriter avec blip
- Zone additionnelle optionnelle
- 1 à 3 boutons premium
- Support tap-to-skip typewriter
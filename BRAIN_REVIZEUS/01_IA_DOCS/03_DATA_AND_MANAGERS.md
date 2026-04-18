# \# DATA \& MANAGERS — RÉFÉRENTIEL OFFICIEL

# 

# \## Base de Données (Room)

# 

# \### AppDatabase

# \- Version officielle : \*\*7\*\*

# \- Architecture : multi-DB par UID Firebase, slot-aware

# \- Source de vérité gameplay : \*\*locale\*\*

# 

# \### Règles absolues

# \- `fallbackToDestructiveMigration()` INTERDIT

# \- Toute évolution de schéma doit :

# &#x20; - incrémenter la version

# &#x20; - définir une `Migration` explicite

# \- `AppDatabase.resetInstance()` : à utiliser lors de la déconnexion / changement de compte si nécessaire

# 

# \### Nommage des DB Room

# \- Slot 1 : `revizeus\_database\_\[uid]`

# \- Slot 2 : `revizeus\_database\_\[uid]\_slot2`

# \- Slot 3 : `revizeus\_database\_\[uid]\_slot3`

# 

# \---

# 

# \## Entités principales

# 

# \### UserProfile

# Profil utilisateur unique (`id = 1`) dans chaque DB de slot.

# Champs gameplay importants :

# \- `pseudo`

# \- `level`

# \- `avatarResName`

# \- `eclatsSavoir`

# \- `ambroisie`

# \- `totalQuizDone`

# \- `totalPlayTimeSeconds`

# 

# Champs cloud / compte :

# \- `accountEmail: String = ""`

# \- `recoveryEmail: String = ""`

# \- `firebaseUid: String = ""`

# \- `isEmailVerified: Boolean = false`

# \- `parentEmail: String = ""`

# \- `isParentSummaryEnabled: Boolean = false`

# \- `lastWeeklySummarySentAt: Long = 0L`

# 

# Champs fragments :

# \- `knowledgeFragments: String = "{}"`

# \- Annotation Room : `@ColumnInfo(name = "knowledge\_fragments")`

# \- Contient un JSON sérialisé : matière → nombre de fragments

# \- Manipulation autorisée uniquement via méthodes dédiées (`getFragmentCount`, `addFragments`)

# \- Écriture directe interdite

# 

# \### QuizQuestion

# Enrichi avec les champs suivants pour le scoring multi-matière et le tracking IA :

# \- `subject: String`

# \- `courseId: String`

# \- `difficulty: Int`

# 

# \### CourseEntry

# \- contient `lastReviewedAt: Long = 0L`

# \- utilisé pour la répétition espacée

# \- `courseId` doit être pré-généré avant `insertCourse()` pour pouvoir servir de clé SharedPrefs `QUIZ\_Q\_${courseId}`

# 

# \### Autres entités analytiques et mémoire

# \- \*\*MemoryScore\*\* : données brutes de mémorisation

# \- \*\*UserAnalytics\*\* : traces brutes d’utilisation

# \- \*\*UserSkillProfile\*\* : profil des forces et faiblesses par matière

# \- \*\*LearningRecommendation\*\* : suggestions de révision générées par l’IA

# 

# \### InventoryItem

# Table : `inventory`

# Champs : `id`, `name`, `type`, `description`, `image\_res\_name`, `quantity`

# \- `quantity = 0` est conservé pour garder l’historique Forge

# 

# \---

# 

# \## SharedPreferences métier

# 

# \### ReviZeusPrefs

# Clés critiques utilisées par les flows compte / onboarding / session :

# \- `ACCOUNT\_EMAIL`

# \- `RECOVERY\_EMAIL`

# \- `FIREBASE\_UID`

# \- `HAS\_ACCOUNT`

# \- `IS\_REGISTERED`

# \- `IS\_EMAIL\_VERIFIED`

# \- `PENDING\_ACCOUNT\_EMAIL`

# 

# Clés quiz temporaires :

# \- `QUIZ\_Q\_${uuid}` stocke les questions JSON d'un cours

# 

# \*\*Règle absolue :\*\*

# Aucun système de nettoyage, update, reset partiel ou migration ne doit supprimer les clés compte critiques sans intention explicite.

# 

# \---

# 

# \## DAO

# 

# \### IAristoteDao

# Méthodes existantes + extensions.

# Méthodes d’inventaire :

# \- `getInventory()`

# \- `getInventoryItemByName(name)`

# \- `insertInventoryItem(item)`

# \- `updateInventoryItem(item)`

# \- `deleteInventoryItem(itemId)`

# \- `countTotalItemQuantity()`

# 

# Méthode temps de jeu :

# \- `addPlayTimeSeconds(seconds: Long)` — cumul additif en base

# 

# \---

# 

# \## Avatar Catalog Premium

# 

# `AvatarItem` porte désormais un contrat étendu :

# \- `id`, `name`, `gender`, `imageResId`, `description`, `backgroundResId`, `backgroundVideoResId`

# \- `specialAbilityKey`, `specialAbilityLabel`, `unlockItemKey`, `isOnboardingSelectable`

# 

# Hooks gameplay futurs :

# \- changement d’avatar ultérieur via objet consommable `avatar\_change\_token`

# \- une capacité spéciale par avatar, non encore activée

# 

# \---

# 

# \## Managers d'application

# 

# \### GameUpdateManager (v2)

# \- Bypass automatique sur app debuggable.

# \- Aucun écran au premier lancement. Timeout strict anti-freeze.

# \- Aucune lecture de `maj.txt` dans l’écran de réalignement.

# \- Conservation validée de `UserProfile`, `CourseEntry`, `MemoryScore`, `Inventory`.

# \- \*\*SÉCURITÉ\*\* : `cleanupDerivedSharedPrefs()` ne doit \*\*jamais\*\* supprimer `ACCOUNT\_EMAIL`, `RECOVERY\_EMAIL`, `FIREBASE\_UID`, `HAS\_ACCOUNT`, `IS\_REGISTERED`, `IS\_EMAIL\_VERIFIED`, `PENDING\_ACCOUNT\_EMAIL`. Ne doit \*\*jamais\*\* appeler `OnboardingSession.clear()`.

# 

# \### FirebaseAuthManager — rôle officiel

# Responsabilités :

# \- `createAccount`, `signIn`, `sendPasswordReset`, `getCurrentUser`, `hasActiveSession`, `signOut`.

# \- Suppression sécurisée du compte Firebase avec ré-authentification.

# Contraintes :

# \- Validation locale de l'email avant tout appel Firebase.

# \- Mapping d’erreurs Firebase renforcé.

# \- Ne jamais lire `task.result` avant vérification de `task.isSuccessful`.

# \- Ne jamais laisser une erreur Firebase provoquer un crash UI (toujours retourner un message exploitable).

# Principe architectural : Firebase = identité compte uniquement. Room = données de héros / progression.

# 

# \### AccountRecoveryManager

# Responsabilités :

# \- Génération d'un code de secours lisible par compte (UID).

# \- Hash local et synchronisation Firestore sous `users/{uid}`.

# \- Gestion de l'indice local du code.

# 

# \### UltimateQuizBuilder

# Responsabilités :

# \- Générer le quiz ultime de 40 questions avec alternance dynamique.

# \- Récupérer tous les cours disponibles et générer un pool large via Gemini.

# \- Appliquer la sélection pondérée avec anti-répétition.

# \- Garantir la variété de matière, savoir et difficulté.

# Règles :

# \- `N\_SAVOIRS\_RECENTS` = 5 : aucun savoir répété dans les 5 dernières questions.

# \- `M\_MATIERES\_RECENTES` = 2 : pas la même matière 2 fois d'affilée.

# \- `MIN\_DIFFICULTE\_RANGE` = 3 : au moins 3 niveaux de difficulté par tranche de 10.

# \- Fallback : Retour liste vide si échec complet (après 1 retry par cours).

# 

# \### UserWeaknessAnalyzer

# Responsabilités :

# \- Analyser les faiblesses du joueur pour personnalisation du quiz ultime (via `UserSkillProfile` et `MemoryScore`).

# \- Calculer bonus/pénalités de priorisation adaptative (Lecture seule, aucune écriture en base).

# Règles :

# \- Score de faiblesse : 0.0 (maîtrisé) à 1.0 (très faible).

# \- Bonus faiblesse : 0 à +30 selon score. Pénalité sur-représentation : 0 à -20 après seuil.

# 

# \### UserAnalyticsEngine

# Responsabilités :

# \- Transformer les analytics brutes en signaux exploitables (`UserInsight`).

# \- Détecter : faiblesses thématiques (erreur >= 40%), problèmes de vitesse (temps >= 8s), précipitation (temps < 2s + erreurs), confusion, évolution, instabilité, fatigue cognitive.

# \- Scorer la sévérité des patterns détectés (InsightType).

# \- Générer des verdicts personnalisés affichés dans `QuizResultActivity`.

# 

# \### GeminiManager \& Extensions IA

# \- \*\*GeminiManager\*\* : Intègre `genererContenuOracle(...)` (entrée : texte libre utilisateur, sortie : résumé + quiz balisé compatible `IAristoteEngine`).

# \- \*\*GodTriggerEngine\*\* : Moteur de déclencheurs divins contextuels basés sur les actions en direct.

# \- \*\*GeminiQuestionTypeHelper\*\* : Assiste Gemini dans la génération de formats de questions variés.

# \- \*\*InsightCache\*\* : Mise en cache des signaux utilisateurs générés pour optimiser les performances inter-écrans.

# 

# \### XpCalculator \& CurrencyManager

# \- \*\*XpCalculator\*\* : Formule sacrée `level \* 100 \* π`. Ne jamais modifier.

# \- \*\*CurrencyManager\*\* : Économie du jeu (`Éclats de Savoir`, `Ambroisie`).

# 

# \### GodManager / PantheonConfig

# \- Mapping matière → dieu.

# \- Contient : couleur, chibi, icônes, thèmes.

# 

# \### SoundManager

# Responsabilités officielles :

# \- Pilote unique de la BGM et des SFX.

# \- Mémoire musicale (`rememberMusic`), reprise différée (`playMusicDelayed`, `resumeRememberedMusicDelayed`).

# \- Arrêt / libération propre (`stopMusic`, `release`).

# Règles :

# \- `GenderActivity` lance une BGM différenciée selon le camp.

# \- L’audio IA (`playLoopingScan`) doit rester compatible avec la BGM principale.

# 

# \### BadgeManager \& BadgeEvalContext

# \- Structure enrichie v1.0.4.

# \- `recordItemForged(context)` incrémente `STAT\_ITEMS\_FORGED` après craft réussi.

# \- Badges explicitement alimentés : `forge\_first`, `forge\_5`, `forge\_10`.

# 

# \### UI \& Utilitaires Transverses

# \- \*\*LoadingDivineDialog\*\* : Composant global de blocage UX pendant appels IA. `isCancelable = false`.

# \- \*\*SpeakerTtsHelper\*\* : TTS centralisé.

# \- \*\*CraftingSystem\*\* : Recettes définies statiquement, ordre UI selon `canAfford(recipe, profile)`.

# \- \*\*KnowledgeFragmentManager\*\* : Source de vérité visuelle pour l'inventaire et les fragments.

# 

# \---

# 

# \## AccountRegistry — architecture officielle

# 

# Rôle : Manager SharedPrefs `ReviZeusAccounts`.

# Responsabilités :

# \- Gérer les UIDs locaux connus, jusqu’à 3 slots par UID, le slot actif.

# \- Stocker les caches d’affichage des héros et mémoriser les emails par UID.

# \- Reconstituer la liste des UIDs à partir des caches si la liste officielle a sauté.

# \- Gérer la suppression locale d’un héros (`deleteHeroLocal`) ou d'un compte complet (`deleteAccountLocal`). Ne supprime pas Firebase distant.

# Capacité de reconstruction :

# \- Doit survivre à la perte de `registered\_uids` grâce aux clés `account\_cache\_\*`, aux emails et slots mémorisés.

# 

# \---

# 

# \## OnboardingSession — rôle officiel

# 

# \- Singleton mémoire transportant email + mot de passe pendant l’onboarding.

# \- Doit conserver : email normalisé, mot de passe brut.

# \- Doit être vidé après consommation complète ou interruption réelle du flux.

# \- Ne doit pas être vidé prématurément pendant un onboarding encore actif.

# 

# \---

# 

# \## Game Update System — statut courant

# 

# Concept documenté : `GameUpdateManager` et `GameUpdateActivity`.

# Principe fondamental : Aucune mise à jour ne doit entraîner une perte de progression joueur.

# Données sacrées à préserver : `UserProfile`, Currency, `knowledgeFragments`, `CourseEntry`, `Inventory`, `MemoryScore`.

# Données temporaires nettoyables : cache OCR, quiz temporaires, onboarding obsolète.

# Règles absolues : `fallbackToDestructiveMigration()` interdit, validation avant / après obligatoire.

# 

# \---

# 

# \## EXTENSION OFFICIELLE — RÉCOMPENSES QUIZ \& TIMERS PAR ÂGE

# 

# Cette section complète l’existant sans le remplacer.

# Principe de source de vérité :

# \- Le gain de fragments reste stocké dans `UserProfile.knowledgeFragments`.

# \- L’ajout doit toujours passer par les méthodes métier dédiées (`getFragmentCount`, `addFragments`).

# \- Aucune écriture directe JSON n’est autorisée.

# 

# \### Règle officielle — gains de fragments par type de quiz

# \- \*\*Quiz Oracle\*\* : +1 fragment par bonne réponse (lié à la matière du savoir). Aucun fragment générique.

# \- \*\*Entraînement normal\*\* : +1 fragment par bonne réponse (lié à la matière de la question).

# \- \*\*Entraînement ultime\*\* : +3 fragments par bonne réponse. Gain calculé question par question selon la matière réelle. Suppression du gain de fragments panthéon génériques pour ce mode.

# 

# \### Règle officielle — timer par question selon l’âge

# Le temps est individuel à chaque question. Barème officiel :

# \- 1 à 10 ans → 20 secondes par question

# \- 11 à 20 ans → 15 secondes par question

# \- au-delà de 20 ans → 10 secondes par question

# 

# \### Managers centralisés

# \- \*\*QuizRewardManager\*\* : Centralise la logique de récompense (Oracle, Normal, Ultime) et l'agrégation pour affichage dans `QuizResultActivity`.

# \- \*\*QuizTimerManager\*\* : Centralise la règle de temps par question selon l'âge et expose une API simple réutilisable.

# 

# \### Impact UI / résultat

# Les écrans de résultat affichent un récapitulatif détaillé calculé au runtime :

# \- `+3 Fragment de Zeus`

# \- `+2 Fragments d’Athéna`

# \- `+1 Fragment de Poséidon`



BaseActivity

Gère le socle fullscreen/portrait, l’infrastructure commune de loading, et la coordination défensive minimale de reprise audio/navigation.

BaseGameActivity

Hérite du socle commun et sert de base aux écrans de jeu avec exigences renforcées sur immersion, état d’écran, audio et retours.

SoundManager

Responsable unique des BGM/SFX.
Doit conserver la mémoire de la musique active/légitime entre écrans.
Doit éviter les reprises brutales hors contexte.
Doit nettoyer complètement ses références lors d’un vrai release().

SpeakerTtsHelper

Helper TTS défensif.
Gère lecture, stop, ducking BGM, restauration BGM et filtrage des callbacks obsolètes.
Toute Activity qui l’utilise doit le stopper au lifecycle approprié.

GodSpeechAnimator

Moteur typewriter divin.
Toute nouvelle animation sur une même vue doit annuler proprement la précédente pour éviter la superposition.

LoadingDivineDialog

Loader divin transverse pour actions longues IA.
Ne doit pas survivre à un écran mort, ni casser un audio appartenant explicitement à un autre écran sans ownership clair.


## DialogRPGManager

**Responsabilité :** Fabrique centrale de dialogues RPG universels.

**Méthodes principales :**
- `show(activity, config)` : Affiche un dialogue RPG complet
- `showInfo(activity, godId, message)` : Dialogue info simple
- `showConfirmation(activity, godId, message, onConfirm, onCancel)` : Dialogue confirm/cancel
- `showTechnicalError(activity, errorType)` : Erreur technique diégétique
- `showDivineFatigue(activity, divineService, resetTime)` : Quota dépassé
- `showReward(activity, godId, message, additionalInfo)` : Récompense/succès
- `selectGodForContext(context)` : Sélection automatique du dieu parlant

**Configuration :** DialogRPGConfig (data class)
- mainText, godId, title, additionalText
- category (DialogCategory enum)
- button1/2/3 labels & actions
- cancelable, tapToSkipTypewriter, typewriterSpeed
- onDismiss callback

**Catégories visuelles :** DialogCategory enum
- INFO, PEDAGOGY, ALERT, ERROR_TECHNICAL, REWARD, CONFIRMATION, DIVINE_FATIGUE, HELP

**Rôles de parole :** Mapping dieu ↔ contexte métier
- Zeus : autorité, décisions majeures
- Athéna : pédagogie, explications
- Hermès : vitesse, langues, connexion
- Apollon : inspiration, harmonie
- Etc. (voir tableau complet)
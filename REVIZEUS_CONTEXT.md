# REVIZEUS_CONTEXT.md
# Fichier de contexte projet pour Continue.dev / IA locale (Ollama)
# Priorité de vérité : code réel > ressources réelles > ce document > anciens docs
# Dernière mise à jour : 17/04/2026

---

## 0. IDENTITÉ DU PROJET

**RéviZeus** est une application Android RPG éducative gamifiée dans l'univers de la mythologie grecque.
L'objectif : transformer la révision scolaire en aventure épique.

Le joueur crée un avatar héros, choisit un dieu tutélaire, dépose ses cours dans l'Oracle (photo / PDF),
obtient des résumés IA + QCM générés automatiquement depuis ses propres cours, et progresse dans
l'Olympe en gagnant XP, badges, fragments et récompenses.

**GitHub** : https://github.com/mrperezthomas-boop/Revizeus2
**Package** : `com.revizeus.app`
**Répertoire local** : `E:\ReviZeus`

---

## 1. STACK TECHNIQUE — RÈGLES ABSOLUES

### Config Android
- Language : **Kotlin** uniquement
- UI : **XML classique + ViewBinding** — Jetpack Compose **INTERDIT**
- Min SDK : **24**
- Target SDK : **36**
- Compile SDK : **36**
- JVM Toolchain : **17**
- Kotlin version : **2.3.10**
- AGP : **9.0.1**
- Gradle Wrapper : **9.1.0**

### Dépendances critiques (versions exactes, ne pas changer)
| Lib | Version |
|---|---|
| Gemini | `com.google.ai.client.generativeai:generativeai:0.9.0` |
| Room (via KSP) | `com.google.devtools.ksp:2.3.6` — toutes fonctions DAO en `suspend` |
| CameraX | `1.5.3` — modules : core, camera2, lifecycle, view |
| ML Kit Text Recognition | `play-services-mlkit-text-recognition:19.0.1` |
| Lottie | `com.airbnb.android:lottie:6.4.0` |
| Media3 / ExoPlayer | `androidx.media3:media3-exoplayer` + `media3-ui` |
| Coil + Coil-GIF | animé WebP |
| Firebase Auth | ✅ |
| Firebase Firestore | ✅ |
| Firebase Functions | ✅ |
| lifecycle-runtime-ktx | `2.8.7` |

### Clé Gemini
- **Ne jamais mettre en dur dans le code**
- Injection via `BuildConfig.GEMINI_API_KEY`
- Source : `local.properties` ou propriété Gradle `GEMINI_API_KEY`

---

## 2. RÈGLES ARCHITECTURALES NON NÉGOCIABLES

1. **ViewBinding obligatoire** — pas de `findViewById`
2. **Jetpack Compose interdit**
3. **Portrait fullscreen** pour tout le projet — **sauf mode Aventure en paysage fullscreen**
4. **Mono Activity par écran** — la logique métier va dans les Managers
5. **Activities = orchestration uniquement** — jamais de logique lourde dans une Activity
6. **Ne jamais renommer les variables existantes**
7. **Ne jamais casser l'architecture existante**
8. **Ne jamais inventer une ressource absente** — si elle manque, la lister explicitement
9. **Toujours générer les fichiers complets modifiés** — jamais de snippets partiels
10. **Room en `Dispatchers.IO`** — jamais sur UI thread
11. **Toute IA en coroutine** — jamais sur UI thread
12. **SoundManager est le système audio central** — jamais de MediaPlayer isolé
13. **GodSpeechAnimator** gère le typewriter de tous les dialogues
14. **LoadingDivineDialog** pour toute attente IA — jamais de ProgressBar simple
15. **Lifecycle strict** : cancel en `onPause`, release en `onDestroy`, cancel jobs coroutines

---

## 3. SYSTÈMES TRANSVERSAUX CRITIQUES

### Audio
- `SoundManager` — tout passe par lui, jamais de lecteur isolé
- Délai 300ms obligatoire entre arrêt et nouveau son
- Arrêt obligatoire dans `onPause()`
- `SpeakerTtsHelper` — TTS avec ducking audio, s'arrête en onPause, ne traverse pas les écrans

### Dialogues RPG
- **MODE A** — Dialogue intégré dans l'Activity (ex : TrainingSelectActivity)
  - Implémentation : `GodSpeechAnimator` + TextView + ImageView
- **MODE B** — Popup universelle (erreurs, confirmations, récompenses, feedback)
  - Implémentation : `DialogRPGManager` + `DialogRPGFragment` + `DialogRPGConfig`
- Standards obligatoires : typewriter, SFX `sfx_dialogue_blip`, avatar dieu, fond `bg_rpg_dialog`
- **INTERDIT** : Toast pour message important, AlertDialog pour narration, texte instantané

### IA / Loading
| Situation | Système |
|---|---|
| Attente IA | `LoadingDivineDialog` |
| Message joueur | Dialogue RPG |
| Erreur système | Dialogue RPG |

### Animations
- `AnimatedBackgroundHelper` — fonds animés
- `OlympianParticlesView` — particules
- `Lottie` — animation Forge (`res/raw/lottie_forge_fire.json`)
- ExoPlayer — fonds MP4 (volume = 0f, audio piloté par SoundManager)
- **INTERDIT** : Lottie pour backgrounds avatar

### Coroutines
- Toujours propager `CancellationException`
- Guards "UI encore vivante" avant effets UI/navigation
- Annuler jobs sur transitions lifecycle critiques
- Pas de résultats fantômes après fermeture d'écran

---

## 4. LISTE COMPLÈTE DES FICHIERS KOTLIN

### Activities — Boot / Compte / Onboarding
- `SplashActivity.kt`
- `LoginActivity.kt`
- `AuthActivity.kt`
- `GenderActivity.kt`
- `AvatarActivity.kt`
- `IntroVideoActivity.kt`
- `MoodActivity.kt`
- `AccountSelectActivity.kt`
- `HeroSelectActivity.kt`
- `TitleScreenActivity.kt`
- `MainMenuActivity.kt`

### Activities — Noyau App
- `DashboardActivity.kt`
- `SettingsActivity.kt` ← hérite de `BaseActivity`
- `RevizeusInfoActivity.kt`
- `HeroProfileActivity.kt`
- `BadgeBookActivity.kt`
- `InventoryActivity.kt`
- `ForgeActivity.kt`
- `DebugAnalyticsActivity.kt`
- `VideoPlayerActivity.kt`
- `GameUpdateActivity.kt`

### Activities — Oracle / Quiz / Savoirs
- `OracleActivity.kt`
- `OraclePromptActivity.kt`
- `ResultActivity.kt`
- `QuizActivity.kt`
- `QuizResultActivity.kt`
- `SavoirActivity.kt`
- `TrainingSelectActivity.kt`
- `TrainingQuizActivity.kt`
- `GodMatiereActivity.kt`

### Activities — Mode Aventure (paysage fullscreen)
- `WorldMapActivity.kt`
- `MapTempleActivity.kt`
- `TempleNodeEncounterActivity.kt`
- `BaseAdventureActivity.kt`
- `BaseGameActivity.kt`

### Managers & Systèmes
- `GeminiManager.kt` — appels IA Gemini
- `GodManager.kt` — gestion des dieux
- `GodLoreManager.kt` — lore des dieux
- `GodSpeechAnimator.kt` — typewriter dialogues
- `GodSpeechAnimator_Integration.kt`
- `GodPersonalityEngine.kt`
- `DialogRPGManager.kt`
- `DialogRPGFragment.kt`
- `DialogRPGConfig.kt`
- `DialogContext.kt`
- `DialogCategory.kt`
- `DivineDialogueOrchestrator.kt`
- `DivineMicroCopyLibrary.kt`
- `LoadingDivineDialog.kt`
- `SoundManager.kt`
- `SpeakerTtsHelper.kt`
- `SettingsManager.kt`
- `CurrencyManager.kt`
- `AnalyticsManager.kt`
- `CraftingSystem.kt`
- `KnowledgeFragmentManager.kt`
- `BadgeManager.kt`
- `FirebaseAuthManager.kt`
- `AccountRecoveryManager.kt`
- `AccountRegistry.kt`
- `TutorialManager.kt`
- `GameUpdateManager.kt`
- `LyriaManager.kt`
- `ParentSummaryManager.kt`
- `AnimatedBackgroundHelper.kt`
- `OlympianParticlesView.kt`
- `HudRewardAnimator.kt`
- `QuizRewardManager.kt`
- `QuizTimerManager.kt`
- `QuizQuestionPlanner.kt`
- `AdventureManager.kt`
- `AdventureState.kt`
- `TempleAdventureProgressManager.kt`
- `TempleMapConfig.kt`
- `TempleMapManager.kt`
- `TempleMapEdge.kt`
- `TempleMapNode.kt`
- `TempleMapThemeResolver.kt`
- `TempleNodeResolver.kt`
- `TempleNodeType.kt`
- `WorldMapState.kt`
- `WorldMapTempleSlot.kt`
- `WorldMapThemeResolver.kt`
- `PantheonConfig.kt`
- `AssetTextReader.kt`
- `OnboardingSession.kt`

### Core / IA Adaptative
- `core/GeminiQuestionTypeHelper.kt`
- `core/GodTriggerEngine.kt`
- `core/InsightCache.kt`
- `core/NormalTrainingBuilder.kt`
- `core/UltimateQuizBuilder.kt`
- `core/UserAnalyticsEngine.kt`
- `core/UserWeaknessAnalyzer.kt`
- `core/XpCalculator.kt`
- `AdaptiveContextFormatter.kt`
- `AdaptiveDialogueEngine.kt`
- `AdaptiveLearningContext.kt`
- `AdaptiveLearningContextResolver.kt`
- `PlayerAdaptiveSnapshot.kt`
- `PlayerContextResolver.kt`

### Models / Room / DAO
- `models/AppDatabase.kt`
- `models/CourseEntry.kt`
- `models/IAristoteDao.kt`
- `models/IAristoteEngine.kt`
- `models/InventoryItem.kt`
- `models/LearningRecommendation.kt`
- `models/LearningRecommendationDao.kt`
- `models/MemoryScore.kt`
- `models/QuestionType.kt`
- `models/QuizQuestion.kt`
- `models/TempleAdventureDao.kt`
- `models/TempleAdventureMapEntity.kt`
- `models/TempleAdventureNodeProgressEntity.kt`
- `models/UserAnalytics.kt`
- `models/UserAnalyticsDao.kt`
- `models/UserInsight.kt`
- `models/UserProfile.kt`
- `models/UserSkillProfile.kt`
- `models/UserSkillProfileDao.kt`

### Adapters / UI
- `AccountCardAdapter.kt`
- `AvatarAdapter.kt`
- `AvatarItem.kt`
- `JukeboxAdapter.kt`
- `MusicTrackItem.kt`
- `OlympianMusicCatalog.kt`
- `BadgeDefinition.kt`
- `TechnicalErrorType.kt`
- `ui/anim/AchievementPopupManager.kt`
- `ui/anim/BadgeUnlockOverlayManager.kt`
- `ui/anim/HudRewardAnimator.kt`
- `ui/DashboardInsightsWidget.kt`

---

## 5. LAYOUTS XML PRINCIPAUX

- `activity_dashboard.xml`
- `activity_oracle.xml`
- `activity_result.xml`
- `activity_quiz.xml`
- `activity_quiz_result.xml`
- `activity_savoir.xml`
- `activity_training.xml`
- `activity_settings.xml`
- `activity_world_map.xml`
- `activity_map_temple.xml`
- `dialog_loading_divine.xml`
- `dialog_rpg_universal.xml`
- `dialog_god_explanation.xml`
- `dialog_musical_lyrics.xml`
- `dialog_oracle_choice.xml`
- `item_account_card.xml`
- `item_avatar.xml`
- `item_avatar_aura.xml`
- `item_hero_slot_card.xml`
- `item_hero_slot_empty.xml`
- `item_jukebox_track.xml`
- `item_recipe_card.xml`
- `panel_tutorial.xml`
- `view_achievement_popup.xml`

---

## 6. RESSOURCES IMPORTANTES

### Drawables clés
- `bg_rpg_dialog` — fond obligatoire pour tout dialogue RPG
- `bg_temple_button` — boutons de temples
- `bg_divine_card` — cartes divines
- Avatars divins et héros/héroïnes
- Fonds : dashboard, olympus, oracle, résultat, quiz, settings, sélection matière
- Badges (nombreux, 400+ définis)
- Icônes : fragments, monnaie, bibliothèque, caméra, mood, aventure, forge, settings, TTS
- Overlays : lightning, particles
- `ic_launcher.png`

### Raw (audio / vidéo)
- BGMs : dashboard, résultats, sélections, forge, oracle, humeur, training
- SFX : `sfx_dialogue_blip`, thunder, timer, orb, save, badge, fragments, loading, avatar confirm, transition
- MP4 fonds animés (plusieurs)
- Vidéos info RéviZeus
- `lottie_forge_fire.json`

---

## 7. ÉCONOMIE & PROGRESSION

- **XP** — points d'expérience héros
- **Éclats** — monnaie principale
- **Ambroisie** — ressource rare premium
- **Fragments** — pour crafting/forge
- **Badges** — 400+ sur 14 catégories thématiques
- **Titres** — liés à la progression Panthéon
- **Niveau héros** — évolue avec l'apprentissage
- **Rang Olympe** — statut global du joueur

---

## 8. UNIVERS & NAVIGATION PRINCIPALE

### Destinations principales
- **Oracle** — dépôt cours (photo/PDF), analyse IA, génération résumé + QCM
- **Savoirs / Temples** — bibliothèque personnelle des cours stockés, organisés par dieu
- **Panthéon** — sélection/gestion des dieux tutélaires
- **Arène / Entraînement** — QCM ciblés par matière/thème
- **Forge (Héphaïstos)** — système de crafting avec recettes
- **Dashboard** — hub central avec insights adaptatifs
- **Monde / Carte** — mode Aventure (paysage fullscreen)

### Dieux du Panthéon (10 confirmés)
Zeus, Athéna, Aphrodite, Apollon, Arès, Déméter, Héphaïstos, Hermès, Poséidon, Prométhée

### Style graphique
- "3D Chibi Epic Olympe" / "3D Chibi WTF Scolaire"
- Thème sombre avec accents or
- RPG immersif, fond mythologique

---

## 9. ÉTAT D'AVANCEMENT DES BLOCS

### BLOC A — Stabilisation technique et socle transverse → ✅ VALIDÉ ET CLÔTURÉ

Patchs appliqués :
1. `SettingsActivity` hérite de `BaseActivity`
2. Migration `onBackPressed()` → contrat socle sur écrans critiques (`VideoPlayerActivity`, `QuizResultActivity`, `LoginActivity`)
3. Logs structurés sur catches critiques (lifecycle, audio/TTS, navigation, IA)
4. Clé Gemini externalisée via `BuildConfig.GEMINI_API_KEY`
5. Contrat coroutine IA homogène (cancel, guards UI, pas de résultats fantômes)

Invariants acquis (à ne plus jamais casser) :
- `BaseActivity` est la référence de socle
- Clé Gemini jamais en dur
- Catches silencieux critiques interdits
- Loaders IA ne restent pas bloqués
- Protections audio/TTS/lifecycle préservées

### BLOC B — Dialogues RPG universels & Personas divines → EN PRÉPARATION

### BLOCS C à Q — À FAIRE (voir roadmap complète dans les docs Bloc/)

---

## 10. MÉTHODE OBLIGATOIRE AVANT TOUT CODE

**Avant de toucher à quoi que ce soit :**

1. **Audit** — identifier les fichiers exactement concernés
2. **Lister** les classes/managers/layouts impliqués
3. **Lister** les risques de régression
4. **Lister** les ressources manquantes éventuelles
5. **Proposer** le plan minimal compatible avec l'existant
6. **Seulement ensuite** : coder

**Format de réponse attendu :**
- Audit bref
- Fichiers touchés
- Ressources manquantes
- Fichiers complets modifiés (jamais de snippets)
- Points de vérification Android Studio

---

## 11. INTERDICTIONS ABSOLUES

❌ Jetpack Compose  
❌ `onBackPressed()` legacy sur écrans critiques  
❌ MediaPlayer isolé hors SoundManager  
❌ Toast pour message important  
❌ AlertDialog pour narration RPG  
❌ Texte affiché instantanément (toujours typewriter)  
❌ ProgressBar simple pour attente IA  
❌ Dialogue RPG sans avatar dieu  
❌ Clé Gemini codée en dur  
❌ Catch silencieux sur chemins critiques  
❌ `findViewByID` (ViewBinding obligatoire)  
❌ Logique lourde sur UI thread  
❌ Renommage de variables existantes  
❌ Ressource inventée sans confirmation qu'elle existe  
❌ Refactor global non demandé  
❌ Pseudo-code ou placeholders parasites  
❌ Orientation paysage hors mode Aventure  
❌ Lottie pour backgrounds avatar  

---

## 12. PROMPT DE DÉMARRAGE STANDARD (à coller en début de session)

```
Tu travailles sur RéviZeus, application Android Kotlin XML avec ViewBinding.

Contraintes absolues :
- pas de Jetpack Compose
- ne renomme aucune variable existante
- ne casse pas l'architecture existante
- génère les fichiers complets modifiés
- tout le projet reste en portrait fullscreen sauf le mode aventure en paysage fullscreen
- n'invente aucune ressource absente
- si une ressource manque, liste-la explicitement
- conserve les mécaniques, commentaires utiles et systèmes transversaux existants
- respecte REVIZEUS_CONTEXT.md

Commence par un AUDIT uniquement.
Donne-moi avant tout code :
1. les fichiers exacts concernés
2. les classes/managers/layouts impliqués
3. les risques de régression
4. les ressources manquantes éventuelles
5. le plan minimal d'implémentation compatible avec l'existant

Ne modifie encore aucun fichier.
```

---

## 13. VÉRITÉ PROJET — ORDRE DE PRIORITÉ

En cas de contradiction entre les sources :

1. **Code réel du repo** (priorité absolue)
2. **Ressources réellement présentes** dans le projet
3. **Ce document REVIZEUS_CONTEXT.md** (mis à jour)
4. **Blocs préparatoires** (Bloc B, C, D…)
5. **Anciens IA_DOCS** non mis à jour (ne pas faire confiance sans vérification)

**Si le code réel et ce document se contredisent → suivre le code réel.**

---

*REVIZEUS_CONTEXT.md — à maintenir à jour après chaque session de développement majeure.*

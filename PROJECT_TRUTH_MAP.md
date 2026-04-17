# PROJECT_TRUTH_MAP.md — CARTE DE VÉRITÉ RÉVIZEUS

Ce document résume l’état **réel observé** du projet à partir du repo et des ressources fournies.

## 1. Noyau technique confirmé
- Package : `com.revizeus.app`
- Build : Kotlin + XML + ViewBinding
- Pas de Compose
- `compileSdk = 36`
- `minSdk = 24`
- `targetSdk = 36`
- JVM 17
- KSP activé

## 2. Dépendances confirmées
- Firebase Auth
- Firebase Firestore
- Firebase Functions
- Gemini `com.google.ai.client.generativeai:generativeai:0.9.0`
- CameraX 1.5.3
- ML Kit Text Recognition 19.0.1
- Room runtime + ktx + compiler
- Media3 ExoPlayer + UI
- Coil + Coil GIF
- Lottie

## 3. Activités et classes importantes confirmées
### Boot / compte / onboarding
- `SplashActivity`
- `LoginActivity`
- `AuthActivity`
- `GenderActivity`
- `AvatarActivity`
- `IntroVideoActivity`
- `MoodActivity`
- `AccountSelectActivity`
- `HeroSelectActivity`

### Noyau app
- `DashboardActivity`
- `SettingsActivity`
- `RevizeusInfoActivity`
- `HeroProfileActivity`
- `BadgeBookActivity`
- `InventoryActivity`
- `ForgeActivity`
- `DebugAnalyticsActivity`

### Oracle / quiz / savoirs
- `OracleActivity`
- `OraclePromptActivity`
- `ResultActivity`
- `QuizActivity`
- `QuizResultActivity`
- `SavoirActivity`
- `TrainingSelectActivity`
- `TrainingQuizActivity`
- `GodMatiereActivity`

### Systèmes transversaux importants
- `GeminiManager`
- `GodLoreManager`
- `GodManager`
- `GodSpeechAnimator`
- `DialogRPGManager`
- `DialogRPGFragment`
- `LoadingDivineDialog`
- `AnimatedBackgroundHelper`
- `OlympianParticlesView`
- `SoundManager`
- `SettingsManager`
- `CurrencyManager`
- `AnalyticsManager`
- `CraftingSystem`
- `SpeakerTtsHelper`
- `PantheonConfig`
- `KnowledgeFragmentManager`

### Aventure confirmée (socle déjà présent)
- `BaseAdventureActivity`
- `AdventureManager`
- `AdventureState`
- `WorldMapActivity`
- `MapTempleActivity`
- `WorldMapState`
- `WorldMapTempleSlot`
- `WorldMapThemeResolver`
- `TempleAdventureProgressManager`
- `TempleMapConfig`
- `TempleMapManager`
- `TempleMapNode`
- `TempleMapEdge`
- `TempleNodeResolver`
- `TempleNodeType`
- Room aventure : `TempleAdventureDao`, `TempleAdventureMapEntity`, `TempleAdventureNodeProgressEntity`

## 4. Layouts réellement présents
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
- etc.

## 5. Orientation confirmée dans le manifest
- Presque tout le projet : `portrait`
- `WorldMapActivity` : `sensorLandscape`
- `MapTempleActivity` : `sensorLandscape`

## 6. Ressources confirmées par captures
### Drawables
- Avatars divins et héros/héroïnes
- Fonds dashboard / olympus / oracle / résultat / quiz / settings / sélection matière
- Badges nombreux
- Icônes fragments, monnaie, bibliothèque, menu caméra, mood, aventure, forge, settings, TTS, etc.
- Overlays lightning / particles
- Éléments RPG (`bg_rpg_dialog`, `bg_temple_button`, `bg_divine_card`, etc.)

### Raw
- BGMs dashboard / résultats / sélections / forge / oracle / humeur / training
- SFX dialogue / thunder / timer / orb / save / badge / fragments / loading / avatar confirm / transition
- Plusieurs MP4 animés de fond
- Vidéos info RéviZeus

## 7. Règles de vérité pour coder
Si le code réel et les docs se contredisent : **suivre le code réel**.
Si une ressource apparaît dans une capture et dans le code, elle est considérée comme fiable.
Si une ressource est mentionnée seulement dans un doc ancien, elle n’est pas fiable tant qu’elle n’est pas confirmée.

## 8. Position actuelle retenue
- Le projet possède déjà un socle large et une identité forte.
- Le mode aventure n’est pas vide, mais seulement entamé.
- Les futurs gros blocs doivent être ajoutés sans casser ce qui existe déjà.

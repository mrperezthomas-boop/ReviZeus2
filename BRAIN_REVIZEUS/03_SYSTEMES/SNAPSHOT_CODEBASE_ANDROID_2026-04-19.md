# SNAPSHOT CODEBASE ANDROID

> Horodatage : 2026-04-19 17:50 local

## Verdict
Le zip contient une base Android RéviZeus suffisamment riche pour alimenter un **snapshot documentaire d'architecture**, mais pas pour remplacer à lui seul le repo source vivant.

## Consolidation retenue
- Canonique : `app/java/com/revizeus/app/...`
- Miroir ignoré pour éviter la double vérité : `app/app/...`

## Chiffres
- Kotlin brut : 263
- Kotlin unique consolidé : 132
- Paires miroir détectées : 131

## Modules / zones visibles dans le snapshot
- activités d'onboarding et authentification
- dashboard / savoir / oracle / quiz / résultats
- managers audio, dialogue RPG, dieux, récompenses
- aventure : maps, nodes, progression Room
- Room entities / DAO / AppDatabase
- Firebase auth / backend managers

## Éléments fortement cohérents avec le Brain RéviZeus
- `GodSpeechAnimator`
- `DialogRPGManager`
- `LoadingDivineDialog`
- `TempleAdventureProgressManager`
- `TempleMapManager`
- `MapTempleActivity`
- `OracleActivity`
- `DashboardActivity`
- `AppDatabase`

## Extrait d'inventaire consolidé
- `AccountCardAdapter.kt`
- `AccountRecoveryManager.kt`
- `AccountRegistry.kt`
- `AccountSelectActivity.kt`
- `AdaptiveContextFormatter.kt`
- `AdaptiveDialogueEngine.kt`
- `AdaptiveLearningContext.kt`
- `AdaptiveLearningContextResolver.kt`
- `AdventureManager.kt`
- `AdventureState.kt`
- `AnalyticsManager.kt`
- `AnimatedBackgroundHelper.kt`
- `AssetTextReader.kt`
- `AuthActivity.kt`
- `AvatarActivity.kt`
- `AvatarAdapter.kt`
- `AvatarItem.kt`
- `BadgeBookActivity.kt`
- `BadgeDefinition.kt`
- `BadgeManager.kt`
- `BaseActivity.kt`
- `BaseAdventureActivity.kt`
- `BaseGameActivity.kt`
- `CraftingSystem.kt`
- `CurrencyManager.kt`
- `DashboardActivity.kt`
- `DebugAnalyticsActivity.kt`
- `DialogCategory.kt`
- `DialogContext.kt`
- `DialogRPGConfig.kt`
- `DialogRPGFragment.kt`
- `DialogRPGManager.kt`
- `DivineDialogueOrchestrator.kt`
- `DivineMicroCopyLibrary.kt`
- `FirebaseAuthManager.kt`
- `ForgeActivity.kt`
- `GameUpdateActivity.kt`
- `GameUpdateManager.kt`
- `GeminiManager.kt`
- `GenderActivity.kt`
- `GodLoreManager.kt`
- `GodManager.kt`
- `GodMatiereActivity.kt`
- `GodPersonalityEngine.kt`
- `GodSpeechAnimator.kt`
- `GodSpeechAnimator_Integration.kt`
- `HeroProfileActivity.kt`
- `HeroSelectActivity.kt`
- `HudRewardAnimator.kt`
- `IntroVideoActivity.kt`
- `InventoryActivity.kt`
- `JukeboxAdapter.kt`
- `KnowledgeFragmentManager.kt`
- `LoadingDivineDialog.kt`
- `LoginActivity.kt`
- `LyriaManager.kt`
- `MainMenuActivity.kt`
- `MapTempleActivity.kt`
- `MoodActivity.kt`
- `MusicTrackItem.kt`
- `OlympianMusicCatalog.kt`
- `OlympianParticlesView.kt`
- `OnboardingSession.kt`
- `OracleActivity.kt`
- `OraclePromptActivity.kt`
- `PantheonConfig.kt`
- `ParentSummaryManager.kt`
- `PlayerAdaptiveSnapshot.kt`
- `PlayerContextResolver.kt`
- `QuizActivity.kt`
- `QuizQuestionPlanner.kt`
- `QuizResultActivity.kt`
- `QuizRewardManager.kt`
- `QuizTimerManager.kt`
- `ResultActivity.kt`
- `RevizeusInfoActivity.kt`
- `SavoirActivity.kt`
- `SettingsActivity.kt`
- `SettingsManager.kt`
- `SoundManager.kt`

## Annotation
Ce snapshot valide surtout la cohérence des noms, des managers et du découpage métier déjà attendu dans le Brain. Le repo Git reste la source de vérité opérationnelle.

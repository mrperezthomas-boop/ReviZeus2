# 03_CODE_MAP.md — ARBRE DE CONNAISSANCE DU CODE RÉVIZEUS
# Dernière mise à jour : 2026-04-17
# BUT : permettre à Claude de comprendre instantanément quel fichier fait quoi,
# qui appelle qui, et quels fichiers toucher pour n'importe quelle tâche.

## NAVIGATION — QUI APPELLE QUI
```
SplashActivity → GameUpdateActivity → VideoPlayerActivity → TitleScreenActivity
TitleScreenActivity → MainMenuActivity
MainMenuActivity → LoginActivity (inscription/connexion) | AccountSelectActivity
LoginActivity → AuthActivity (inscription) | HeroSelectActivity (connexion)
AuthActivity → GenderActivity
GenderActivity → AvatarActivity
AvatarActivity → IntroVideoActivity
IntroVideoActivity → MoodActivity
MoodActivity → DashboardActivity
AccountSelectActivity → MoodActivity
HeroSelectActivity → MoodActivity | GenderActivity (nouveau héros)

DashboardActivity → OracleActivity | TrainingSelectActivity | SavoirActivity | ForgeActivity
                   | InventoryActivity | BadgeBookActivity | HeroProfileActivity
                   | SettingsActivity | DebugAnalyticsActivity | MoodActivity

OracleActivity → OraclePromptActivity → ResultActivity → QuizActivity → QuizResultActivity
SavoirActivity → GodMatiereActivity
TrainingSelectActivity → TrainingQuizActivity → QuizResultActivity
SettingsActivity → RevizeusInfoActivity | TitleScreenActivity | LoginActivity
```

## MANAGERS — RÔLE ET DÉPENDANCES

### Cœur IA
| Manager | Rôle | Consomme | Consommé par |
|---------|------|----------|--------------|
| GeminiManager | Appels Gemini (résumés, quiz, corrections, lore) | BuildConfig.GEMINI_API_KEY | OracleActivity, ResultActivity, TrainingSelectActivity, GodMatiereActivity, GodLoreManager, QuizResultActivity |
| GodLoreManager | Contenu divin (hymnes, explications, dialogues) | GeminiManager, GodManager, UserProfile | GodMatiereActivity, DashboardActivity, ForgeActivity, TrainingSelectActivity |
| GodPersonalityEngine | Personnalité/ton de chaque dieu | PantheonConfig | GodLoreManager, futur B2 |
| GodTriggerEngine | Déclencheurs divins contextuels | UserProfile, quiz results | QuizResultActivity, DashboardActivity |
| AdaptiveLearningContextResolver | Contexte adaptatif pour quiz | AppDatabase, UserAnalytics, UserSkillProfile | TrainingSelectActivity, UltimateQuizBuilder |
| UserAnalyticsEngine | Détection patterns apprentissage | UserAnalytics, UserSkillProfile, MemoryScore | QuizResultActivity, DebugAnalyticsActivity |
| UltimateQuizBuilder | Génération quiz ultime 40q | GeminiManager, AdaptiveLearningContextResolver | TrainingSelectActivity |

### Système de jeu
| Manager | Rôle | Consomme | Consommé par |
|---------|------|----------|--------------|
| CurrencyManager | Éclats + Ambroisie | AppDatabase, UserProfile | QuizResultActivity, ForgeActivity, DashboardActivity |
| KnowledgeFragmentManager | Fragments par matière (JSON) | UserProfile.knowledgeFragments | InventoryActivity, ForgeActivity, DashboardActivity |
| CraftingSystem | Recettes forge (fragments→artefacts) | InventoryItem, UserProfile | ForgeActivity, DashboardActivity |
| BadgeManager | 102+ badges, évaluation, déblocage | AppDatabase, BadgeDefinition | QuizResultActivity, ForgeActivity, DashboardActivity |
| XpCalculator | XP = level*100*π | UserProfile | QuizResultActivity |
| QuizTimerManager | Timer/question selon âge | UserProfile.age | QuizActivity, TrainingQuizActivity |
| QuizRewardManager | Gains fragments par type quiz | KnowledgeFragmentManager | QuizActivity, TrainingQuizActivity, QuizResultActivity |

### Infrastructure
| Manager | Rôle | Consommé par |
|---------|------|--------------|
| SoundManager | BGM/SFX centralisé, mémoire musicale | Presque toutes les Activities |
| DialogRPGManager | Fabrique dialogues RPG (popup) | Toute Activity qui affiche un dialogue important |
| DialogRPGFragment | Fragment universel typewriter+chibi | DialogRPGManager |
| GodSpeechAnimator | Typewriter animation + blip SFX | DialogRPGFragment, AvatarActivity, DashboardActivity |
| LoadingDivineDialog | Loader bloquant pendant IA | OracleActivity, ResultActivity, TrainingSelectActivity, GodMatiereActivity, QuizResultActivity |
| SpeakerTtsHelper | TTS avec ducking BGM | SettingsActivity, futur |
| AnimatedBackgroundHelper | Fonds animés + particules | DashboardActivity, certains écrans |

### Compte / Auth
| Manager | Rôle | Consommé par |
|---------|------|--------------|
| FirebaseAuthManager | Auth Firebase (create, signIn, reset, signOut) | LoginActivity, AvatarActivity, SettingsActivity, SplashActivity |
| AccountRegistry | UIDs locaux, slots, caches héros | AccountSelectActivity, HeroSelectActivity, AvatarActivity, AuthActivity |
| OnboardingSession | Singleton mémoire email+mdp pendant onboarding | LoginActivity → AuthActivity → AvatarActivity |
| AccountRecoveryManager | Code secours + hash + Firestore | LoginActivity, AvatarActivity |

### Config / Mapping
| Manager | Rôle | Consommé par |
|---------|------|--------------|
| PantheonConfig | Mapping matière↔dieu, couleurs, icônes | GodManager, GodLoreManager, DialogRPGManager, quasi tout |
| GodManager | Résolution contexte→dieu, dialogues contextuels | GodMatiereActivity, GodLoreManager, DashboardActivity |
| SettingsManager | Préférences utilisateur | SettingsActivity, quiz, audio |

## ROOM DATABASE — ENTITIES ET DAO

### AppDatabase (version 9, 8 migrations explicites)
| Entity | Table | Rôle |
|--------|-------|------|
| UserProfile | user_profile | Profil unique (id=1), XP, level, pseudo, fragments JSON, monnaies |
| CourseEntry | course_entry | Savoirs enregistrés (résumés Oracle) |
| QuizQuestion | quiz_questions | Questions enrichies (subject, courseId, difficulty) |
| MemoryScore | memory_score | Scores de mémorisation bruts |
| UserAnalytics | user_analytics | Traces brutes d'utilisation |
| UserSkillProfile | user_skill_profile | Forces/faiblesses par matière |
| LearningRecommendation | learning_recommendation | Suggestions IA de révision |
| InventoryItem | inventory | Objets forgés (quantity=0 conservé pour historique) |
| TempleAdventureMapEntity | temple_adventure_map | Maps aventure par temple |
| TempleAdventureNodeProgressEntity | temple_adventure_node_progress | Progression nodes aventure |

### IAristoteDao — Méthodes clés
Inventaire : getInventory(), getInventoryItemByName(), insertInventoryItem(), updateInventoryItem()
Temps de jeu : addPlayTimeSeconds()
Profil : getUserProfile(), updateUserProfile()
Cours : getAllCourses(), insertCourse(), deleteCourse()

### TempleAdventureDao — Méthodes clés
getMapForTemple(), saveMap(), getNodeProgress(), saveNodeProgress()

## LAYOUTS XML PRINCIPAUX
| Layout | Activity | Éléments clés |
|--------|----------|---------------|
| activity_dashboard.xml | DashboardActivity | btnSettings, chipEclat, chipAmbroisie, chipForge, llRecentBadges, imgAvatarHero |
| activity_oracle.xml | OracleActivity | Boutons scan/galerie/texte, zone preview |
| activity_result.xml | ResultActivity | Résumé, validation, choix temple, quiz |
| activity_quiz.xml | QuizActivity/BaseGameActivity | Timer, question, 4 réponses, compteur |
| activity_quiz_result.xml | QuizResultActivity | Étoiles, récompenses, corrections |
| activity_forge.xml | ForgeActivity | RecyclerView recettes, animation Lottie |
| activity_savoir.xml | SavoirActivity | Liste savoirs par matière |
| activity_training.xml | TrainingSelectActivity/TrainingQuizActivity | Modes entraînement |
| dialog_rpg_universal.xml | DialogRPGFragment | Chibi, typewriter, boutons premium |
| dialog_loading_divine.xml | LoadingDivineDialog | WebP animé, message immersif |

## DIALOGUES RPG — ÉTAT DES CONVERSIONS
### Convertis (Bloc B fait)
OracleActivity(16), DashboardActivity(1), QuizResultActivity(2), ForgeActivity(3), TrainingQuizActivity(1), TrainingSelectActivity(1), ResultActivity(6) = ~30 total

### À convertir (Bloc B restant)
GodMatiereActivity(15), SettingsActivity(13), LoginActivity(9), AvatarActivity(8), AccountSelectActivity(6), InventoryActivity(4), HeroSelectActivity(3), TitleScreenActivity(2), SavoirActivity(2), OraclePromptActivity(2), BadgeBookActivity(1), AuthActivity(1), MainMenuActivity(1) = ~67 total

### Cas spécial
OracleActivity.showOracleChoiceDialog() : dialogue custom → à migrer vers DialogRPGManager

# 00_REVIZEUS_CONTEXT.md — ÉTAT TECHNIQUE RÉEL DU PROJET
# Dernière mise à jour : 2026-04-17

## IDENTITÉ
- Package : `com.revizeus.app`
- App éducative RPG mythologique Android
- 10 dieux = 10 matières scolaires
- Style : JRPG Chibi "Épique WTF Scolaire"
- Cible : élèves de tous âges

## PROPOSITION DE VALEUR
L'utilisateur capture ses cours (photo/texte/PDF), les transforme en résumés via l'Oracle (IA), puis révise avec des quiz adaptatifs générés à partir de ses propres contenus. Les dieux mythologiques incarnent chaque matière et accompagnent la progression.

Le mode aventure (restauration des temples, combats QCM-RPG) est une couche premium de rétention et différenciation — pas le cœur du produit.

## STACK TECHNIQUE
Kotlin 2.3.10 | AGP 9.0.1 | Min SDK 24 | Target/Compile SDK 36 | JVM 17
ViewBinding XML (Compose INTERDIT) | Room v3 via KSP | DB version 9 (8 migrations explicites)
Firebase Auth + Firestore + Cloud Functions | Gemini SDK 0.9.0
CameraX 1.5.3 | ML Kit 19.0.1 | Media3/ExoPlayer | Lottie 6.4.0 | Coil+GIF
Portrait partout sauf aventure (sensorLandscape)

## ARCHITECTURE
- ~133 fichiers Kotlin, ~42 layouts XML
- 30+ Activities, mono-Activity par écran
- Logique métier dans Managers, Activities = orchestration
- Multi-DB Room par UID Firebase, jusqu'à 3 slots héros par compte
- Clé Gemini externalisée via BuildConfig (plus jamais hardcodée)

## BOOT FLOW
SplashActivity → GameUpdateActivity (si release) → VideoPlayerActivity → TitleScreenActivity → MainMenuActivity
- NOUVEAU COMPTE → LoginActivity (inscription) → AuthActivity → GenderActivity → AvatarActivity → IntroVideoActivity → MoodActivity → DashboardActivity
- CHARGER COMPTE → LoginActivity (connexion) → HeroSelectActivity → MoodActivity → DashboardActivity
- CHARGER HÉROS → AccountSelectActivity → MoodActivity → DashboardActivity

## BOUCLE DE JEU PRINCIPALE
Oracle (scan/texte) → Résumé IA → Stockage temple/matière → Quiz généré → Résultat → Récompenses (XP, éclats, fragments) → Forge → Artefacts → Progression temples

## SYSTÈMES MAJEURS ACTIFS
- **Oracle** : scan OCR + envoi Gemini + résumé + quiz balisé (texte libre aussi supporté)
- **Quiz** : Oracle, entraînement normal (30q), ultime matière (40q), ultime global (40q alternées)
- **Timer quiz** : par question selon âge (≤10=20s, ≤20=15s, >20=10s)
- **Fragments** : +1/bonne réponse (Oracle/normal), +3 (ultime), liés à la matière réelle
- **Forge** : fragments + éclats → artefacts
- **Inventaire** : fragments, artefacts (équipements/objets/reliques = "bientôt")
- **Badges** : 102+ badges, streak/quiz/oracle/forge/divin/spécial
- **Dialogues RPG** : DialogRPGManager/Fragment, typewriter + blip + chibi animé
- **Temples** : 10 niveaux de restauration (0→10 dans le système actuel)
- **Dashboard** : hub principal avec accès oracle/training/forge/inventaire/badges/savoirs/settings

## DIALOGUES RPG — ÉTAT BLOC B
Infrastructure complète livrée : DialogRPGConfig, DialogCategory, DialogContext, TechnicalErrorType, DialogRPGManager, DialogRPGFragment.
~30 dialogues convertis sur 7 écrans (OracleActivity, DashboardActivity, QuizResultActivity, ForgeActivity, TrainingQuizActivity, TrainingSelectActivity, ResultActivity).
~80 occurrences restantes sur 13 écrans (GodMatiereActivity, SettingsActivity, LoginActivity, AvatarActivity, AccountSelectActivity, InventoryActivity, HeroSelectActivity, TitleScreenActivity, SavoirActivity, OraclePromptActivity, BadgeBookActivity, AuthActivity, MainMenuActivity).

## AVENTURE — ÉTAT SOCLE
Code existant : BaseAdventureActivity, WorldMapActivity, MapTempleActivity, TempleNodeEncounterActivity, AdventureManager, TempleAdventureProgressManager, TempleMapManager, TempleMapConfig, TempleMapNode, WorldMapState, WorldMapTempleSlot, WorldMapThemeResolver + Room aventure (DAO + entities).
État : socle posé, pas de vrai combat QCM-RPG encore, pas de hub monde complet.

## MANAGERS CLÉS ET LEURS RÔLES
- **GeminiManager** : appels IA Gemini (résumés, quiz, corrections, lore)
- **GodLoreManager** : génération de contenu divin (hymnes, explications, dialogues)
- **GodManager/PantheonConfig** : mapping matière↔dieu, couleurs, icônes
- **GodSpeechAnimator** : typewriter animation avec blip SFX
- **GodPersonalityEngine** : personnalité et ton de chaque dieu
- **SoundManager** : BGM/SFX centralisé, mémoire musicale
- **DialogRPGManager** : fabrique centrale de dialogues RPG
- **CurrencyManager** : éclats de savoir + ambroisie
- **CraftingSystem** : recettes forge (fragments → artefacts)
- **BadgeManager** : évaluation et déblocage des badges
- **QuizTimerManager** : timer par question selon âge
- **QuizRewardManager** : gains fragments par type de quiz
- **AccountRegistry** : gestion UIDs locaux, slots, caches héros
- **OnboardingSession** : singleton mémoire email+mdp pendant onboarding
- **UserAnalyticsEngine** : détection patterns apprentissage
- **AdaptiveLearningContextResolver** : contexte adaptatif pour quiz

## BLOCS PLANIFIÉS (C→Q)
- C : Audio global, TTS ducking, loadings, fonds vidéo/image
- D : Oracle premium (pipeline résumé, validation, temple, quiz)
- E : Quiz nouvelle génération (corrections, feedback, résultat)
- F : Récompenses unifiées (fragments, économie, affichages gains)
- G : Forge/inventaire (effets objets, craft, tri, filtres)
- H : Temple progression (reconstruction, déblocages, monde)
- I : Savoirs vivants (sous-dossiers, long-press, artefacts, export)
- J : Extensions divines créatives (Aphrodite, Hermès, Apollon, Prométhée)
- K : Profils accompagnement IA adaptative V1
- L : Analytics pédagogiques, cognition, révision intelligente
- M : Recommandations IA, prochaine meilleure action
- N : Dashboard vivant, widgets insight, agora, monde réactif
- O : Rangs, badges, titres, constellations, méta-progression
- P : Final ultime adaptatif, génératif, RPG
- Q : Lore, carte monde, intro, tension mythologique, bestiaire

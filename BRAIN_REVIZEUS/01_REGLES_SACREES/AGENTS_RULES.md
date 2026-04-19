# AGENTS.md — RÈGLES PROJET RÉVIZEUS

> **NOTE DE CONSOLIDATION (2026-04-19)** : ce document reste la source sacrée des règles agents.
> Les références à `IA_DOCS/00_REVIZEUS_CONTEXT.md` et `IA_DOCS/06_BLOC_STATUS.md` ont été **mises à jour** pour pointer vers la structure réelle du Brain consolidé (`Brain_ReviZeus/01_REGLES_SACREES/REVIZEUS_CONTEXT.md` et `Brain_ReviZeus/02_BLOCS/INDEX_BLOCS.md`). Le reste du contenu original est préservé intégralement.

---

# Dernière mise à jour : 2026-04-17

Tu travailles sur **RéviZeus**, application Android éducative RPG mythologique.
10 dieux = 10 matières scolaires. Chibi JRPG. IA adaptative. Dialogues RPG immersifs.

## 1. HIÉRARCHIE DE VÉRITÉ
En cas de contradiction, appliquer cet ordre :
1. Code réel du repo
2. Ressources réellement présentes (drawable, raw, layout, assets)
3. Ce fichier AGENTS.md
4. `Brain_ReviZeus/01_REGLES_SACREES/REVIZEUS_CONTEXT.md` (ex IA_DOCS/00_REVIZEUS_CONTEXT.md)
5. Blocs préparatoires
6. Anciens IA_DOCS / archives

Ne JAMAIS inventer une vérité projet non confirmée par le code actuel.

## 2. STACK NON NÉGOCIABLE
- Kotlin 2.3.10, AGP 9.0.1, Gradle Wrapper 9.1.0
- Min SDK 24, Compile/Target SDK 36, JVM Toolchain 17
- XML classique + ViewBinding obligatoire
- **Jetpack Compose INTERDIT**
- Room via KSP (version DB actuelle : 9, migrations explicites obligatoires)
- `fallbackToDestructiveMigration()` INTERDIT
- Firebase Auth + Firestore + Cloud Functions
- Gemini SDK `com.google.ai.client.generativeai:generativeai:0.9.0`
- CameraX 1.5.3, ML Kit text-recognition 19.0.1
- lifecycle-runtime-ktx 2.8.7
- Media3/ExoPlayer, Lottie 6.4.0, Coil + Coil-GIF

## 3. RÈGLES ABSOLUES DE MODIFICATION
- Ne JAMAIS renommer mes variables, IDs XML, extras, managers existants.
- Ne JAMAIS supprimer une mécanique existante sans demande explicite.
- Ne JAMAIS casser les flows de navigation existants.
- Conserver 100% des commentaires, mécaniques et structures utiles.
- Générer les **fichiers COMPLETS modifiés** — jamais des snippets.
- Pas de placeholders, pas de pseudocode, pas de refactor global non demandé.
- Commenter intelligemment les ajouts dans le code.
- Si une ressource (drawable, raw, layout) n'existe pas, le DIRE explicitement — ne jamais l'inventer.

## 4. PROTOCOLE OBLIGATOIRE
Tu ne génères JAMAIS de code sans passer par ces étapes :
1. **AUDIT** — Lire les fichiers concernés, comprendre l'existant
2. **CONCEPTION** — Proposer le plan, lister fichiers touchés, risques, ressources manquantes
3. **VALIDATION** — Attendre la commande exacte **"INVOQUER"**
4. **CODE** — Générer les fichiers complets

## 5. ORIENTATION / AFFICHAGE
- **Tout RéviZeus = portrait fullscreen**
- Exception unique : **mode aventure = paysage fullscreen** (sensorLandscape)
- Ne pas propager le paysage hors du mode aventure.
- Chaque nouvelle Activity déclarée dans AndroidManifest.xml.

## 6. DIALOGUES RPG — RÈGLE ABSOLUE
- **Toast pour message important = INTERDIT**
- **AlertDialog narratif = INTERDIT**
- **Texte affiché instantanément dans un contexte divin = INTERDIT**
- Tout dialogue important passe par le système RPG :
  - Mode A (Activity directe) : GodSpeechAnimator si l'écran a déjà un bloc typewriter
  - Mode B (Popup) : DialogRPGManager + DialogRPGFragment
- Typewriter obligatoire avec sfx_dialogue_blip
- `view.post {}` pour lancer le typewriter, avec guard `isAdded && !isDetached && view.isAttachedToWindow`
- Arrêt propre en onPause, nettoyage en onDestroy
- LoadingDivineDialog pour toute attente IA perceptible (jamais un simple spinner)

## 7. AUDIO
- Toute musique/SFX passe par **SoundManager** — jamais de MediaPlayer isolé
- Délai 300ms entre arrêt d'un son et lancement d'un autre
- `SoundManager.stopMusic()` avant toute vidéo ExoPlayer
- Arrêt obligatoire dans onPause(), reprise cohérente dans onResume()
- TTS via SpeakerTtsHelper avec ducking BGM

## 8. IA / COROUTINES
- Tout appel IA = LoadingDivineDialog + blocage interactions
- Toute coroutine IA en `lifecycleScope` ou `Dispatchers.IO`
- `CancellationException` propagée, jamais traitée comme erreur
- Guard "UI encore vivante" avant tout effet UI/navigation post-IA
- Cancel des jobs dans onPause/onDestroy

## 9. BASE DE DONNÉES
- Room v3 — version DB actuelle = **9** avec 8 migrations explicites
- `fallbackToDestructiveMigration()` INTERDIT
- Architecture multi-DB par UID Firebase, slot-aware (3 slots max par UID)
- DAO suspend obligatoire
- Écriture fragments uniquement via méthodes dédiées (jamais écriture JSON directe)

## 10. DIEUX / MATIÈRES — MAPPING OFFICIEL
| Dieu | Matière | Couleur | Hex |
|------|---------|---------|-----|
| Zeus | Mathématiques | Bleu Électrique | #1E90FF |
| Athéna | Français | Or/Blanc | #FFD700 |
| Poséidon | SVT | Turquoise | #40E0D0 |
| Arès | Histoire | Rouge Doré | #DAA520 |
| Aphrodite | Art/Musique | Rose Lumineux | #FF69B4 |
| Hermès | Langues (toutes) | Bleu Ciel | #87CEEB |
| Déméter | Géographie | Vert Forêt | #228B22 |
| Héphaïstos | Physique-Chimie | Orange Flamme | #FF8C00 |
| Apollon | Philo/SES/Poésie | Violet Clair | #DDA0DD |
| Prométhée | Vie & Projets | Ambre Doré | #FFBF00 |

**Variants accents obligatoires** dans les `when()` : POSÉIDON/POSEIDON, ATHÉNA/ATHENA, ARÈS/ARES, HÉPHAÏSTOS/HEPHAISTUS/HEPHAISTOS, HERMÈS/HERMES, PROMÉTHÉE/PROMETHEUS.
Résolution dieu par matière via `PantheonConfig.findByMatiere()`.

## 11. ÉCONOMIE DU JEU
- **Éclats de Savoir** : monnaie basique
- **Ambroisie** : monnaie rare/premium
- **Fragments de Savoir** : colorés par matière, JSON dans `UserProfile.knowledgeFragments`
- XP : formule sacrée `level * 100 * π` (ne jamais modifier)

## 12. MANAGERS PRINCIPAUX À CONNAÎTRE
GeminiManager, GodLoreManager, GodManager, GodSpeechAnimator, SoundManager, PantheonConfig, DialogRPGManager, XpCalculator, CurrencyManager, CraftingSystem, BadgeManager, QuizTimerManager, QuizRewardManager, AdaptiveLearningContextResolver, AccountRegistry, OnboardingSession, KnowledgeFragmentManager, UserAnalyticsEngine, SettingsManager, FirebaseAuthManager, LoadingDivineDialog, SpeakerTtsHelper, GodPersonalityEngine, GodTriggerEngine.

## 13. ÉTAT DU PROJET
- **BLOC A** : ✅ Terminé (stabilisation technique)
- **BLOC B** : En cours (~30 dialogues convertis sur ~110 total)
- **Blocs C→Q** : Préparés en documentation, pas encore codés
- Le mode aventure existe en socle (WorldMapActivity, MapTempleActivity, etc.) mais n'est pas complet
- Le cœur éducatif (Oracle, quiz, résumés, savoirs) fonctionne déjà

## 14. RÈGLE CODE D'ABORD
- Maximum 20% du temps sur la documentation, 80% sur le code
- Ne pas créer d'infrastructure documentaire avant que le code l'exige
- Un bloc terminé = code commité + `Brain_ReviZeus/02_BLOCS/INDEX_BLOCS.md` (ex IA_DOCS/06_BLOC_STATUS.md, section "Activité récente auto") mis à jour
- Pas plus de docs que nécessaire pour coder le bloc en cours

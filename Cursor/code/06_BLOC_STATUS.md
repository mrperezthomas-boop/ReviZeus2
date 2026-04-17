# 06_BLOC_STATUS.md — ÉTAT DES BLOCS RÉVIZEUS
# Dernière mise à jour : 2026-04-17

## RÈGLE DE MISE À JOUR
Ce fichier est mis à jour à chaque fin de bloc ou avancée significative.
Seul l'état RÉELLEMENT VALIDÉ est inscrit ici — pas les intentions.

---

## BLOC A — Stabilisation technique et socle transverse
**Statut : ✅ TERMINÉ ET CLÔTURÉ**

Patchs appliqués :
1. SettingsActivity → hérite de BaseActivity
2. Retour Android unifié (VideoPlayerActivity, QuizResultActivity, LoginActivity)
3. Observabilité minimale (logs structurés sur catches critiques)
4. Clé Gemini externalisée via BuildConfig
5. Contrat coroutine IA homogène (CancellationException, gardes UI, annulation défensive)

Fichiers modifiés : SettingsActivity, VideoPlayerActivity, QuizResultActivity, LoginActivity, BaseActivity, SoundManager, SpeakerTtsHelper, LoadingDivineDialog, OracleActivity, ResultActivity, TrainingSelectActivity, TrainingQuizActivity, GeminiManager, GodMatiereActivity, app/build.gradle.kts

Invariants acquis (ne plus casser) :
- BaseActivity = socle transverse de référence
- Pas de contournement du socle sans justification
- Pas de catches silencieux sur chemins critiques
- Clé Gemini jamais hardcodée
- Annulation = annulation, pas erreur générique
- Protections audio/TTS défensives maintenues

---

## BLOC B — Dialogues RPG universels et feedbacks immersifs
**Statut : 🟡 EN COURS (~30% fait)**

Ce qui est fait :
- Infrastructure complète : DialogRPGConfig, DialogCategory, DialogContext, TechnicalErrorType, DialogRPGManager, DialogRPGFragment
- Fix typewriter : `view.post {}` + guard + fallback + logs debug
- Fix compilation : `additionalText` corrigé partout
- ~30 dialogues convertis sur 7 écrans (OracleActivity, DashboardActivity, QuizResultActivity, ForgeActivity, TrainingQuizActivity, TrainingSelectActivity, ResultActivity)

Ce qui reste :
- ~80 occurrences Toast/AlertDialog sur 13 écrans :
  - GodMatiereActivity (~15 dialogues) — PRIORITÉ
  - SettingsActivity (~13)
  - LoginActivity (~9)
  - AvatarActivity (~8)
  - AccountSelectActivity (~6)
  - InventoryActivity (~4)
  - HeroSelectActivity (~3)
  - TitleScreenActivity (~2)
  - SavoirActivity (~2)
  - OraclePromptActivity (~2)
  - BadgeBookActivity (~1)
  - AuthActivity (~1)
  - MainMenuActivity (~1)
- OracleActivity : conversion de `showOracleChoiceDialog()` (dialogue custom → DialogRPGManager)

Prochain objectif : finir les conversions écran par écran en commençant par GodMatiereActivity.

---

## BLOC B2 — Personas divines et orchestration IA multi-dieux
**Statut : 📋 PLANIFIÉ (non commencé)**

Objectif : chaque dieu = personnalité IA distincte + orchestrateur central.
Fichiers à créer : DivinePersonaType, DivineActionType, DivineSpeechMode, DivinePersonaConfig, DivinePersonaManager, DivineRequestContext, DivineResponsePlan, DivineResponseOrchestrator, DivinePromptBuilder.
Fichiers à modifier : GeminiManager, GodLoreManager, PantheonConfig, GodManager.
Position : après BLOC B, avant C.

---

## BLOCS C → Q — État global
| Bloc | Nom | Statut |
|------|-----|--------|
| C | Audio global, TTS, loadings, fonds vidéo/image | 📋 Planifié |
| D | Oracle premium pipeline | 📋 Planifié |
| E | Quiz nouvelle génération | 📋 Planifié |
| F | Récompenses unifiées, économie | 📋 Planifié |
| G | Forge, inventaire, effets objets | 📋 Planifié |
| H | Temple progression, reconstruction, monde | 📋 Planifié |
| I | Savoirs vivants, sous-dossiers, export | 📋 Planifié |
| J | Extensions divines créatives | 📋 Planifié |
| K | IA adaptative V1 | 📋 Planifié |
| L | Analytics pédagogiques, cognition | 📋 Planifié |
| M | Recommandations IA | 📋 Planifié |
| N | Dashboard vivant | 📋 Planifié |
| O | Rangs, badges, méta-progression | 📋 Planifié |
| P | Final adaptatif RPG | 📋 Planifié |
| Q | Lore, carte monde, bestiaire | 📋 Planifié |

---

## RÈGLE POUR LES PROCHAINS BLOCS
- Repartir du code réel à chaque nouveau bloc
- Respecter les invariants du Bloc A
- Ne pas rouvrir les sujets déjà clôturés sans justification
- Toute modification doit préserver les garanties lifecycle/audio/TTS/loaders/coroutines
- Mettre à jour CE FICHIER après chaque bloc réellement terminé

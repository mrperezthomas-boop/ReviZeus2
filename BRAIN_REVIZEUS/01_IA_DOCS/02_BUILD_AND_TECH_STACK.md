# STACK TECHNIQUE & CONFIGURATION GRADLE
Ne propose jamais de code incompatible avec ces versions.

## Core Configuration
- Min SDK : 24
- Target SDK : 36
- Compile SDK : 36

- Langage : Kotlin
- Toolchain : 17
- Version Kotlin : 2.3.10

- Gradle :
  - AGP : 9.0.1
  - Wrapper : 9.1.0

- UI :
  - ViewBinding obligatoire
  - XML classique
  - Jetpack Compose interdit

---

## Dépendances critiques

### Room
- Implémentation via KSP
- `com.google.devtools.ksp` v2.3.6
- Toutes les fonctions DAO doivent être `suspend`

### IA Gemini
- `com.google.ai.client.generativeai:generativeai:0.9.0`

### ML Kit
- `play-services-mlkit-text-recognition:19.0.1`

### CameraX
- v1.5.3
- modules : `core`, `camera2`, `lifecycle`, `view`

### Coroutines
- `lifecycle-runtime-ktx:2.8.7`
- Utilisé pour `lifecycleScope` dans les Activities

### Lottie
- `com.airbnb.android:lottie:6.4.0`
- Animation Forge : `res/raw/lottie_forge_fire.json` requis
- Contraintes attendues :
  - 2–3 secondes
  - flammes dorées / orangées
  - `loop=false`
- Source de référence autorisée : lottiefiles.com
- ⚠️ INTERDICTION : aucun Lottie pour les backgrounds avatar

### Media & ExoPlayer
- `androidx.media3:media3-exoplayer`
- `androidx.media3:media3-ui`

Usages autorisés :
- backgrounds MP4 d'avatars (muets, fullscreen)
- panneaux animés GenderActivity
- vidéo d’intro fullscreen `revizeus_intro`

Contraintes :
- portrait fullscreen obligatoire
- volume ExoPlayer = 0f (audio piloté par SoundManager)

### Coil & GIF/WebP
- Coil + Coil-GIF
- utilisé pour WebP animés

Cas principal :
- `LoadingDivineDialog` (loader IA animé)

### Timer natif
- `CountDownTimer` obligatoire
- gestion lifecycle stricte (`cancel()` impératif)

---

# ⚡ SYSTÈME AUDIO GLOBAL

## SoundManager (CENTRAL)

RÈGLES :
- toute musique passe par SoundManager
- jamais de MediaPlayer isolé
- jamais de son direct dans une Activity

## Conflits audio (CRITIQUE)

- délai 300ms entre arrêt et nouveau son
- arrêt obligatoire dans `onPause()`

## TTS (Speaker)

- piloté par SpeakerTtsHelper
- DOIT :
  - baisser la musique (ducking)
  - s’arrêter en onPause
  - ne jamais continuer entre écrans

---

# ⚡ SYSTÈME IA — RÈGLES TECHNIQUES

## Appels IA (OBLIGATOIRE)

Tout appel IA DOIT :

1. afficher `LoadingDivineDialog`
2. bloquer les interactions
3. être stoppé proprement

❌ INTERDIT :
- spinner simple
- ProgressBar classique
- écran cliquable pendant IA

---

# ⚡ SYSTÈME DE DIALOGUES RPG (INTÉGRATION OFFICIELLE)

⚠️ CE BLOC EST CRITIQUE POUR TOUT LE PROJET

Le rendu RPG est OBLIGATOIRE.
Mais l’implémentation est CONTEXTUELLE.

---

## STANDARD DE RENDU

Tout dialogue doit :

- être en typewriter (GodSpeechAnimator)
- jouer le SFX `sfx_dialogue_blip`
- afficher un dieu (avatar/chibi)
- utiliser `bg_rpg_dialog`
- être cohérent avec le contexte (DialogContext / GodManager)

---

## MODES TECHNIQUES AUTORISÉS

### MODE A — DIALOGUE INTÉGRÉ (ACTIVITY)

Utilisation :
- écran avec dialogue natif (ex : TrainingSelectActivity)

Implémentation :
- GodSpeechAnimator
- TextView + ImageView

Lifecycle obligatoire :
- cancel job précédent
- stop en onPause
- release en onDestroy

---

### MODE B — DIALOGUE RPG UNIVERSAL (POPUP)

Utilisation :
- erreurs
- confirmations
- récompenses
- choix utilisateur
- feedback système

Implémentation :
- DialogRPGManager
- DialogRPGFragment
- DialogRPGConfig

---

## RÈGLE FONDAMENTALE

❌ NE JAMAIS FORCER DialogRPGFragment PARTOUT  
✔ UTILISER LE BON MODE SELON L’ÉCRAN  

---

## INTERDICTIONS

❌ Toast pour message important  
❌ AlertDialog pour narration  
❌ texte affiché instantanément  
❌ absence de dieu dans un dialogue important  

---

# ⚡ LOADING VS DIALOGUE (DISTINCTION CRITIQUE)

| Cas | Système |
|-----|--------|
| Attente IA | LoadingDivineDialog |
| Message joueur | Dialogue RPG |
| Erreur système | Dialogue RPG |

⚠️ Loading ≠ Dialogue

---

# ⚡ GESTION LIFECYCLE (OBLIGATOIRE)

Tout système doit :

- stopper animations en onPause
- annuler jobs coroutines
- libérer ressources en onDestroy
- éviter doublons

---

# ⚡ RÈGLE PERFORMANCE

- pas de blocage UI thread
- toute IA en coroutine
- Room en Dispatchers.IO
- animations fluides 60fps

---

# ⚡ RÈGLE UI GLOBALE

- portrait fullscreen obligatoire
- ViewBinding obligatoire
- pas de Compose
- pas de UI brute Android (Button simple interdit)

---

# ⚡ RÈGLE ARCHITECTURE

- Mono Activity par écran
- logique métier dans Managers
- Activities = orchestration uniquement

---

# ⚡ RÈGLE RÉVIZEUS

Toujours :

- préserver l’existant
- ajouter sans casser
- respecter l’architecture actuelle
- produire du rendu RPG premium

---

# 🚀 CONCLUSION

Ce document définit :

- la stack technique
- les contraintes d’intégration
- le standard UX RPG
- la séparation Activity / Dialog / Loading

Tout code généré DOIT respecter ces règles sans exception.
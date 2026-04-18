# 🏛️ BRAIN RÉVIZEUS — Guide de Démarrage Rapide

**Version :** 1.0  
**Date :** 17 Avril 2026  
**Statut :** BLOC A ✅ Terminé | BLOC B 🔄 En cours

---

## 📁 Structure du Brain

```
BRAIN_REVIZEUS/
│
├── 00_QUICK_START/          ← Tu es ici
│   └── README.md
│
├── 01_IA_DOCS/              ← Documentation pour IA (Cursor, Claude, ChatGPT)
│   ├── 01_SYSTEM_PROMPT_ULTIME.txt    → Prompt maître pour tout
│   ├── 02_BUILD_AND_TECH_STACK.md     → Config technique
│   ├── 03_DATA_AND_MANAGERS.md        → Managers et données
│   ├── 04_UI_AND_NAVIGATION_MAP.md    → Navigation et écrans
│   ├── 05_RESOURCE_MANIFEST.json      → Ressources drawable/raw
│   ├── 07_GAME_SYSTEMS.md             → Systèmes de jeu
│   ├── 08_AI_PROMPTS_LIBRARY.md       → Prompts Gemini
│   └── 09_LORE_BIBLE_REVIZEUS.md      → Bible mythologique
│
├── 02_BLOCS/                ← Roadmap de développement
│   ├── ETAT_CUMULATIF_BLOCS.txt       → État global
│   ├── A_TERMINE/                      → Blocs validés
│   │   └── BLOC_A_STABILISATION.txt
│   └── B_A_FAIRE/                      → Blocs futurs (B → Q)
│       ├── BLOC_B_DIALOGUES_RPG.txt
│       ├── BLOC_C_AUDIO_GLOBAL.txt
│       ├── BLOC_D_ORACLE_PREMIUM.txt
│       ├── BLOC_E_QUIZ_GENERATION.txt
│       ├── BLOC_F_RECOMPENSES.txt
│       ├── BLOC_G_FORGE_INVENTAIRE.txt
│       ├── BLOC_H_TEMPLE_PROGRESS.txt
│       ├── BLOC_I_SAVOIRS_VIVANTS.txt
│       ├── BLOC_J_EXTENSIONS_DIVINES.txt
│       ├── BLOC_K_PROFILS_IA.txt
│       ├── BLOC_L_ANALYTICS.txt
│       ├── BLOC_M_RECOMMANDATIONS.txt
│       ├── BLOC_N_DASHBOARD.txt
│       ├── BLOC_O_RANGS_BADGES.txt
│       ├── BLOC_P_FINAL_ULTIME.txt
│       └── BLOC_Q_LORE_CARTE.txt
│
├── 03_AVENTURE/             ← Mode Aventure (paysage)
│   ├── Bestiaire/           → Packs ZIP
│   ├── Creatures/           → 610 créatures JSON (10 dieux × 61)
│   ├── Storytelling/        → Packs narration
│   └── *.pdf                → Architecture aventure
│
├── 04_LORE/                 ← Univers et narration
│   ├── BIBLE_MYTHOLOGIQUE.md
│   ├── BADGES_SUCCES.txt    → 400 badges
│   └── CINEMATIQUE_INTRO_PROMPTS.txt
│
├── 05_DIRECTION_ARTISTIQUE/ ← DA visuelle et sonore
│   └── DA_AUDIOVISUELLE.txt
│
├── 06_CURSOR_SKILLS/        ← Skills et rules pour Cursor
│   ├── CURSOR_SKILLS.md
│   └── rules/
│       └── revizeus.mdc
│
└── 07_ARCHIVES/             ← Vieux fichiers (référence)
```

---

## 🎯 État Actuel du Projet

| Composant | Statut |
|-----------|--------|
| **BLOC A** | ✅ Stabilisation technique terminée |
| **BLOC B** | 🔄 Dialogues RPG en cours (~30 conversions faites, ~77 restantes) |
| **Fichiers Kotlin** | 132 fichiers |
| **Layouts XML** | 47 fichiers |
| **Créatures** | 610 (10 dieux × 61 créatures) |
| **Badges** | 400 définis |
| **Blocs restants** | B → Q (16 blocs) |

---

## 🔧 Tech Stack

| Composant | Version |
|-----------|---------|
| **Kotlin** | 2.x |
| **compileSdk** | 36 |
| **minSdk** | 24 |
| **targetSdk** | 36 |
| **ViewBinding** | ✅ Obligatoire |
| **Compose** | ❌ Interdit |
| **Room** | v3 (KSP, migrations explicites) |
| **Firebase** | Auth + Firestore + Functions |
| **Gemini SDK** | 0.9.0 |
| **Media3/ExoPlayer** | Pour vidéo |
| **Lottie** | 6.4.0 |
| **Coil** | Pour images/WebP animé |

---

## 🚀 Pipeline de Travail avec IA

```
1. BRAIN            → Comprendre le contexte (ce dossier)
2. IA_DOCS          → Lire la doc technique
3. BLOC CIBLE       → Identifier le bloc à faire
4. AUDIT            → Analyser le code réel
5. CONCEPTION       → Proposer l'approche
6. VALIDATION       → Attendre ton OK ("INVOQUER")
7. CODE             → Générer le code complet
8. GIT              → Commit + Push
```

---

## ⚠️ Règles Non Négociables (Mémorise-les !)

1. **Portrait obligatoire** → Sauf mode Aventure = paysage
2. **ViewBinding uniquement** → Compose interdit
3. **Lire avant de modifier** → Jamais inventer de ressources/variables
4. **Migrations Room explicites** → Pas de `fallbackToDestructiveMigration`
5. **DialogRPGManager** → Tous les dialogues (pas de Toast/AlertDialog)
6. **Clé Gemini externalisée** → Via BuildConfig, jamais en dur
7. **SoundManager** → Toute gestion audio passe par lui
8. **`view.post {}`** → Pour lancer le typewriter dans les dialogs

---

## 📞 Comment Utiliser ce Brain

### Avec Claude/ChatGPT :
```
1. Envoie le fichier 01_SYSTEM_PROMPT_ULTIME.txt
2. Précise le bloc sur lequel tu travailles
3. Demande un AUDIT avant toute modification
4. Attends la CONCEPTION puis VALIDE
5. Dis "INVOQUER" pour recevoir le code complet
```

### Avec Cursor :
```
1. Ouvre le projet Android Studio
2. Copie 06_CURSOR_SKILLS/ dans ton projet
3. Référence les IA_DOCS dans @docs
4. Utilise le fichier .mdc comme règle Cursor
```

---

## 🎮 Les 10 Dieux RéviZeus

| Dieu | Matière | Couleur |
|------|---------|---------|
| ⚡ Zeus | Mathématiques | Or/Jaune |
| 🌊 Poséidon | Sciences | Bleu océan |
| 🦉 Athéna | Français | Violet/Améthyste |
| 🔥 Arès | Histoire | Rouge sang |
| 🌾 Déméter | Géographie | Vert nature |
| 💕 Aphrodite | Arts | Rose/Corail |
| 🔨 Héphaïstos | Technologie | Bronze/Feu |
| 🎵 Apollon | Musique | Or solaire |
| 🏃 Hermès | Sport | Bleu ciel |
| 🔥 Prométhée | Sciences (guide) | Orange feu |

---

## 📋 Blocs de Développement (A → Q)

| Bloc | Nom | Statut |
|------|-----|--------|
| A | Stabilisation Technique | ✅ Terminé |
| B | Dialogues RPG Immersifs | 🔄 En cours |
| C | Audio Global / TTS / Ducking | 📋 À faire |
| D | Oracle Premium | 📋 À faire |
| E | Quiz Nouvelle Génération | 📋 À faire |
| F | Récompenses Unifiées | 📋 À faire |
| G | Forge & Inventaire | 📋 À faire |
| H | Temple Progress | 📋 À faire |
| I | Savoirs Vivants | 📋 À faire |
| J | Extensions Divines | 📋 À faire |
| K | Profils IA Adaptative | 📋 À faire |
| L | Analytics Pédagogiques | 📋 À faire |
| M | Recommandations IA | 📋 À faire |
| N | Dashboard Vivant | 📋 À faire |
| O | Rangs & Badges | 📋 À faire |
| P | Final Ultime | 📋 À faire |
| Q | Lore & Carte du Monde | 📋 À faire |

---

**Créé le 17/04/2026 — Brain RéviZeus v1.0**

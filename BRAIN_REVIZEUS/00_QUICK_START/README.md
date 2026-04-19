# 🏛️ BRAIN RÉVIZEUS — Guide de Démarrage Rapide

**Version :** 2.1 (finalisée : prompts banque + archives aventure extraites + refs obsolètes patchées)
**Date :** 19 Avril 2026
**Statut :** BLOC A ✅ Terminé | BLOC B 🔄 En cours

---

## 📁 Structure réelle du Brain

```
Brain_ReviZeus/
│
├── 00_QUICK_START/                ← Tu es ici
│   ├── README.md                  → Ce fichier
│   ├── ETAT_TEMPS_REEL.md         → État live du projet
│   ├── PRESENTATION_COMPLETE.md   → Présentation produit complète
│   ├── README_PROJET_ANDROID.md   → README du code Android
│   └── INDEX_MASTER.md            → Index global du Brain
│
├── 01_REGLES_SACREES/             ← Doctrine non négociable
│   ├── CONSTITUTION_PROJET.md     → Vision & positionnement
│   ├── RULES_SACREES.md           → Règles techniques sacrées
│   ├── PROTOCOLE_CARTOGRAPHIE.md  → Méthode de classement
│   ├── AGENTS_RULES.md            → Règles pour IA (ex-AGENTS.md)
│   ├── WORKFLOW_PIPELINE.md       → Pipeline Claude → Cursor
│   ├── DOCTRINE_EQUIPE_AGENTS.md  → Équipe d'agents Famille A/B
│   └── REVIZEUS_CONTEXT.md        → Contexte projet Continue.dev
│
├── 02_BLOCS/                      ← Roadmap développement
│   ├── INDEX_BLOCS.md
│   ├── REVIZEUS_ETAT_CUMULATIF_DES_BLOCS_TERMINES.txt
│   ├── A_TERMINE/                 → Blocs validés
│   └── B_A_FAIRE/                 → Blocs B → Q (16 blocs restants)
│
├── 03_SYSTEMES/                   ← Cartographie technique
│   ├── CARTOGRAPHIE_MASTER.md
│   ├── DOMAINES_OWNERS.md
│   ├── PROJECT_TRUTH_MAP.md       → Vérité observée du repo
│   └── ARBORESCENCE_CODE_SOURCE.md → Inventaire des fichiers Kotlin/XML
│
├── 04_JOURNAUX/                   ← Horodatage runtime
│   └── JOURNAL_AGENT.md
│
├── 05_DIRECTION_ARTISTIQUE/       ← DA visuelle & sonore
│   └── DA_AUDIOVISUELLE.txt
│
├── 06_CURSOR_SKILLS/              ← Skills, rules et prompts Cursor
│   ├── CURSOR_SKILLS.md
│   ├── rules/revizeus.mdc
│   └── prompts/                   → Banque de prompts Cursor versionnés
│       ├── README.md              → Convention, nommage, template
│       ├── validated/             → Prompts testés avec succès
│       ├── drafts/                → Prompts en cours d'élaboration
│       └── archived/              → Prompts obsolètes (append-only)
│
├── 07_ARCHIVES/                   ← Référence historique
│   ├── Guide Ultime (bloc par bloc).txt
│   ├── Recapitulation v9.txt
│   ├── Recapitulation v10.txt
│   └── MACHINELEARNING_V10_ARCHIVE.md
│
├── 08_SNAPSHOTS/                  ← Snapshots runtime JSON
│
├── 09_RUNTIME_AGENT/              ← Configuration agents automatiques
│   ├── agent_config.json
│   ├── revizeus_mapper_rules.json
│   ├── file_block_map.json
│   ├── LAST_ANALYSIS.json
│   └── LAST_RUN_SUMMARY.md
│
├── 10_RAPPORTS_QA/                ← Contrôle des règles
│   └── LAST_RULES_REPORT.md
│
├── 11_LORE/                       ← Univers & narration
│   ├── BIBLE_MYTHOLOGIQUE.md
│   ├── DIEUX_PERSONNALITES.md
│   ├── BADGES_SUCCES.txt
│   ├── BADGES_CATALOGUE_400.md    → 400 badges complets
│   └── CINEMATIQUE_INTRO_PROMPTS.txt
│
├── 12_AVENTURE/                   ← Mode Aventure (paysage)
│   ├── Bestiaire/
│   │   ├── divin_ultime/          → Bestiaire divin (10 fichiers txt/json)
│   │   ├── pack_textes/           → 620 créatures (tableau + fiches)
│   │   └── _zips_sources/         → ZIPs sources archivés
│   ├── Creatures/                 → 610 créatures JSON (10 dieux × 61)
│   ├── Storytelling/              → 5 packs narration extraits
│   │   ├── REVIZEUS_PACK_1_STORYTELLING_ULTIME/     → Lore monde + 10 dieux + Chaos
│   │   ├── REVIZEUS_PACK_2_ET_INTEGRATION_SITE_ULTIME_REGENERE/
│   │   ├── REVIZEUS_PACK_3_SITE_ULTRA_OPERATIONNEL/
│   │   ├── REVIZEUS_PACK_4_BRIEF_HTML_CSS_JS_CLAUDE/
│   │   ├── REVIZEUS_PACK_5_PROMPTS_PAGE_PAR_PAGE_CLAUDE/
│   │   └── _zips_sources/         → ZIPs sources archivés
│   └── *.pdf                      → Architecture aventure
│
├── 13_IA_DOCS/                    ← Documentation technique IA
│   ├── 01_SYSTEM_PROMPT_ULTIME.txt
│   ├── 02_BUILD_AND_TECH_STACK.md
│   ├── 03_DATA_AND_MANAGERS.md
│   ├── 04_UI_AND_NAVIGATION_MAP.md
│   ├── 05_RESOURCE_MANIFEST.json
│   ├── 07_GAME_SYSTEMS.md
│   ├── 08_AI_PROMPTS_LIBRARY.md
│   ├── 09_LORE_BIBLE_REVIZEUS.md
│   └── 10_FUTURE_EVOLUTIONS.txt
│
└── 14_TAXONOMIE/                  ← Langage officiel de classement
    ├── INDEX_TAXONOMIQUE.md
    ├── 01_TAXONOMIE_MASTER.md
    ├── 02_ENTITES_MASTER.md
    ├── 03_JEU_MASTER.md
    ├── 04_DIALOGUES_MASTER.md
    ├── 05_RELATIONS_MASTER.md
    ├── 06_UX_IMMERSION_MASTER.md
    ├── 07_DOCUMENTS_VERITE_MASTER.md
    ├── 08_CONVENTIONS_MASTER.md
    └── 09_AGENT_USAGE_MASTER.md
```

---

## 🎯 État Actuel du Projet

| Composant | Statut |
|-----------|--------|
| **BLOC A** | ✅ Stabilisation technique terminée |
| **BLOC B** | 🔄 Dialogues RPG en cours (~30 conversions faites, ~77 restantes) |
| **Fichiers Kotlin** | ~132 fichiers |
| **Layouts XML** | ~47 fichiers |
| **Créatures** | 610 (10 dieux × 61 créatures) |
| **Badges** | 400 définis |
| **Blocs restants** | B → Q (16 blocs) |

---

## 🔧 Tech Stack (résumé)

| Composant | Version |
|-----------|---------|
| **Kotlin** | 2.3.10 |
| **AGP / Gradle Wrapper** | 9.0.1 / 9.1.0 |
| **compileSdk / targetSdk / minSdk** | 36 / 36 / 24 |
| **ViewBinding** | ✅ Obligatoire |
| **Compose** | ❌ Interdit |
| **Room** | KSP 2.3.6, migrations explicites, DAO suspend |
| **Firebase** | Auth + Firestore + Functions |
| **Gemini SDK** | 0.9.0 |
| **Media3/ExoPlayer** | pour vidéo |
| **Lottie** | 6.4.0 |
| **Coil / Coil-GIF** | images & WebP animé |

Source de vérité détaillée : `13_IA_DOCS/02_BUILD_AND_TECH_STACK.md` et `01_REGLES_SACREES/REVIZEUS_CONTEXT.md`.

---

## 🚀 Pipeline de Travail avec IA (résumé)

```
1. BRAIN            → Comprendre le contexte (ce dossier)
2. IA_DOCS          → Lire la doc technique (13_IA_DOCS/)
3. TAXONOMIE        → Classer proprement (14_TAXONOMIE/)
4. BLOC CIBLE       → Identifier le bloc à faire (02_BLOCS/)
5. AUDIT            → Analyser le code réel
6. CONCEPTION       → Proposer l'approche
7. VALIDATION       → Attendre "INVOQUER"
8. CODE             → Générer le code complet
9. GIT              → Commit + Push
10. AGENTS          → Famille A met le Brain à jour (09_RUNTIME_AGENT/)
```

Protocole détaillé : `01_REGLES_SACREES/WORKFLOW_PIPELINE.md` et `01_REGLES_SACREES/DOCTRINE_EQUIPE_AGENTS.md`.

---

## ⚠️ Règles Non Négociables (mémoriser)

1. **Portrait obligatoire** → Exception : mode Aventure = paysage
2. **ViewBinding uniquement** → Compose interdit
3. **Lire avant de modifier** → Jamais inventer de ressources/variables
4. **Migrations Room explicites** → Pas de `fallbackToDestructiveMigration`
5. **DialogRPGManager** → Tous les dialogues (pas de Toast / AlertDialog)
6. **Clé Gemini externalisée** → Via BuildConfig, jamais en dur
7. **SoundManager** → Toute gestion audio passe par lui
8. **`view.post {}`** → Pour lancer le typewriter dans les dialogs
9. **Livrables complets** → Jamais de snippets, toujours le fichier entier
10. **Commentaires horodatés** → Format `// [YYYY-MM-DD HH:MM][BLOC][ZONE] message bref`

Source : `01_REGLES_SACREES/RULES_SACREES.md` et `01_REGLES_SACREES/AGENTS_RULES.md`.

---

## 📞 Comment Utiliser ce Brain

### Avec Claude / ChatGPT
1. Envoie `13_IA_DOCS/01_SYSTEM_PROMPT_ULTIME.txt` comme system prompt de base
2. Précise le bloc sur lequel tu travailles (`02_BLOCS/`)
3. Demande un AUDIT avant toute modification
4. Attends la CONCEPTION puis VALIDE
5. Dis "INVOQUER" pour recevoir le code complet

### Avec Cursor
1. Ouvre le projet Android Studio
2. Les règles Cursor sont déjà dans `.cursor/rules/revizeus.mdc` (reflet dans `06_CURSOR_SKILLS/rules/`)
3. Référence les IA_DOCS via `@docs` ou chemins absolus
4. Demande des prompts compacts à Claude pour les coller dans Cursor

### Avec les agents locaux (Famille A)
1. Lis `01_REGLES_SACREES/DOCTRINE_EQUIPE_AGENTS.md`
2. Configure `09_RUNTIME_AGENT/agent_config.json` si les chemins du repo diffèrent
3. Les agents écrivent dans `04_JOURNAUX/`, `08_SNAPSHOTS/`, `10_RAPPORTS_QA/`, `09_RUNTIME_AGENT/`

---

## 🎮 Les 10 Dieux RéviZeus

| Dieu | Matière | Couleur |
|------|---------|---------|
| ⚡ Zeus | Mathématiques | Bleu électrique `#1E90FF` |
| 🦉 Athéna | Français | Or / Blanc `#FFD700` |
| 🌊 Poséidon | SVT | Turquoise `#40E0D0` |
| 🔥 Arès | Histoire | Rouge doré `#DAA520` |
| 💕 Aphrodite | Art / Musique | Rose lumineux `#FF69B4` |
| 🏃 Hermès | Anglais | Bleu ciel `#87CEEB` |
| 🌾 Déméter | Géographie | Vert forêt `#228B22` |
| 🔨 Héphaïstos | Physique-Chimie | Orange flamme `#FF8C00` |
| 🎵 Apollon | Philo / SES | Violet clair `#DDA0DD` |
| 🔥 Prométhée | Vie & Projets (guide) | Ambre doré `#FFBF00` |

Source : `00_QUICK_START/PRESENTATION_COMPLETE.md` et `11_LORE/DIEUX_PERSONNALITES.md`.

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

Détails par bloc : `02_BLOCS/B_A_FAIRE/BLOC_*.txt`

---

**Mis à jour le 18/04/2026 — Brain RéviZeus v2.0 (structure consolidée)**

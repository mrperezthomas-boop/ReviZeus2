---
document_id: IA_DOC_00_INDEX
type: index
stability: vivant
truth_level: certain
owner: brain_core
criticality: P0
version: 2.0
last_updated: 2026-04-18
format: optimized_for_ai_consumption
---

# 00 — INDEX IA_DOCS (POINT D'ENTRÉE AGENTS IA)

## Usage IA

Tu es un agent IA (Claude, ChatGPT, Cursor, Continue, Ollama, Gemini ou autre). Ce dossier `IA_docs/` est ta **source unique de vérité normalisée** pour intervenir sur RéviZeus.

**Règle d'or :** tu lis les documents dans l'ordre ci-dessous selon la tâche demandée, tu les traites comme source certaine sauf conflit avec le code réel.

## Ordre de lecture recommandé

| Si la tâche est... | Lire ces documents dans cet ordre |
|--------------------|-----------------------------------|
| **Première prise de contact** | 01_CONSTITUTION, 02_SYSTEM_PROMPT, 11_REGLES_SACREES |
| **Coder une feature** | 02_SYSTEM_PROMPT, 03_TECH_STACK, 04_DATA_AND_MANAGERS, 05_UI_AND_NAVIGATION, 10_BLOCS_ROADMAP |
| **Classer / taxonomier** | 08_TAXONOMIE, 09_CARTOGRAPHIE |
| **Comprendre l'univers** | 07_LORE_BIBLE, 01_CONSTITUTION |
| **Générer un prompt IA interne à l'app** | 13_AI_PROMPTS_LIBRARY, 06_GAME_SYSTEMS |
| **Vérifier des ressources** | 14_RESOURCE_MANIFEST |
| **Anticiper les évolutions** | 15_FUTURE_EVOLUTIONS, 10_BLOCS_ROADMAP |
| **Produire un prompt Cursor** | 16_WORKFLOW_PIPELINE, 12_DOCTRINE_AGENTS, 11_REGLES_SACREES |

## Table des documents

| ID | Fichier | Sujet | Priorité |
|----|---------|-------|----------|
| IA_DOC_01 | `01_CONSTITUTION.md` | Vision produit, positionnement, promesse, architecture conceptuelle | P0 |
| IA_DOC_02 | `02_SYSTEM_PROMPT.md` | System prompt maître pour génération de code | P0 |
| IA_DOC_03 | `03_TECH_STACK.md` | Stack technique, versions, dépendances, interdits | P0 |
| IA_DOC_04 | `04_DATA_AND_MANAGERS.md` | Managers statiques, entités Room, architecture data | P0 |
| IA_DOC_05 | `05_UI_AND_NAVIGATION.md` | Activities, navigation, onboarding, dashboard | P1 |
| IA_DOC_06 | `06_GAME_SYSTEMS.md` | Systèmes de jeu (oracle, quiz, forge, aventure, badges) | P1 |
| IA_DOC_07 | `07_LORE_BIBLE.md` | Mythologie, dieux, personnalités, lore | P1 |
| IA_DOC_08 | `08_TAXONOMIE.md` | Langage officiel de classement (fusionné) | P0 |
| IA_DOC_09 | `09_CARTOGRAPHIE.md` | Mapping fichier → bloc / zone / owner | P1 |
| IA_DOC_10 | `10_BLOCS_ROADMAP.md` | Blocs A → Q, état et contenu | P0 |
| IA_DOC_11 | `11_REGLES_SACREES.md` | Règles non négociables (tech + UX + code) | P0 |
| IA_DOC_12 | `12_DOCTRINE_AGENTS.md` | Équipe d'agents Famille A + Famille B | P0 |
| IA_DOC_13 | `13_AI_PROMPTS_LIBRARY.md` | Prompts Gemini utilisés dans l'app | P1 |
| IA_DOC_14 | `14_RESOURCE_MANIFEST.json` | Liste des ressources drawable, raw, etc. | P1 |
| IA_DOC_15 | `15_FUTURE_EVOLUTIONS.md` | Backlog stratégique et évolutions prévues | P2 |
| IA_DOC_16 | `16_WORKFLOW_PIPELINE.md` | Pipeline Claude → Cursor → Git | P1 |

## Hiérarchie de vérité (à appliquer en cas de conflit)

1. **Code réel du repo** Android (source absolue)
2. **Ressources réelles** présentes dans `res/`, `assets/`, `raw/`
3. **Les documents IA_docs/** (ce dossier)
4. **Brain_ReviZeus/** (référence étendue et historique)
5. **Anciens docs hors de ce dossier** — ne pas se fier sans vérifier

## Règles d'interaction avec ces documents

- **Ne pas réécrire librement** ces documents. Tu peux les citer, en extraire, les synthétiser.
- **Ne pas inventer de variations** de ces règles. Si une règle te semble manquer, signale-la au décideur humain.
- **Chaque document a un frontmatter YAML** qui donne sa criticité, son owner, son niveau de vérité. Tu dois respecter ces métadonnées.
- **Si un document est `stability: sacré`**, tu ne proposes aucune modification sans validation humaine explicite.
- **Si un document est `stability: vivant`**, il peut être mis à jour par les agents Famille A, mais tu ne le fais pas toi-même sans instruction.

## Mapping IA_docs ↔ Brain_ReviZeus

Ce dossier est une **vue normalisée** de `Brain_ReviZeus/` pour consommation IA rapide. Pour toute trace historique, source originale, archive, snapshot, lore étendu, assets aventure, scripts d'agents : consulter `Brain_ReviZeus/`.

| IA_docs | Source primaire dans Brain_ReviZeus |
|---------|-------------------------------------|
| 01_CONSTITUTION | `01_REGLES_SACREES/CONSTITUTION_PROJET.md` |
| 02_SYSTEM_PROMPT | `13_IA_DOCS/01_SYSTEM_PROMPT_ULTIME.txt` |
| 03_TECH_STACK | `13_IA_DOCS/02_BUILD_AND_TECH_STACK.md` + `01_REGLES_SACREES/REVIZEUS_CONTEXT.md` |
| 04_DATA_AND_MANAGERS | `13_IA_DOCS/03_DATA_AND_MANAGERS.md` |
| 05_UI_AND_NAVIGATION | `13_IA_DOCS/04_UI_AND_NAVIGATION_MAP.md` |
| 06_GAME_SYSTEMS | `13_IA_DOCS/07_GAME_SYSTEMS.md` |
| 07_LORE_BIBLE | `13_IA_DOCS/09_LORE_BIBLE_REVIZEUS.md` + `11_LORE/BIBLE_MYTHOLOGIQUE.md` + `11_LORE/DIEUX_PERSONNALITES.md` |
| 08_TAXONOMIE | `14_TAXONOMIE/*` (fusionné) |
| 09_CARTOGRAPHIE | `03_SYSTEMES/CARTOGRAPHIE_MASTER.md` + `03_SYSTEMES/DOMAINES_OWNERS.md` + `09_RUNTIME_AGENT/revizeus_mapper_rules.json` |
| 10_BLOCS_ROADMAP | `02_BLOCS/INDEX_BLOCS.md` + contenus A_TERMINE et B_A_FAIRE |
| 11_REGLES_SACREES | `01_REGLES_SACREES/RULES_SACREES.md` + `AGENTS_RULES.md` |
| 12_DOCTRINE_AGENTS | `01_REGLES_SACREES/DOCTRINE_EQUIPE_AGENTS.md` |
| 13_AI_PROMPTS_LIBRARY | `13_IA_DOCS/08_AI_PROMPTS_LIBRARY.md` |
| 14_RESOURCE_MANIFEST | `13_IA_DOCS/05_RESOURCE_MANIFEST.json` |
| 15_FUTURE_EVOLUTIONS | `13_IA_DOCS/10_FUTURE_EVOLUTIONS.txt` |
| 16_WORKFLOW_PIPELINE | `01_REGLES_SACREES/WORKFLOW_PIPELINE.md` |

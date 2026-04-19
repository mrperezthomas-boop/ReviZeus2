# DOCTRINE ÉQUIPE D'AGENTS — RÉVIZEUS

**Version :** 1.0
**Date :** 2026-04-18
**Statut :** Document sacré d'architecture opérationnelle
**Owner logique :** `brain_core`
**Criticité :** P0

---

## 0. Pourquoi ce document existe

RéviZeus est construit par **une seule personne** avec assistance IA. Sans discipline d'équipe d'agents, le projet finit toujours par se fragmenter : fichiers éparpillés, IA qui modifie trop de choses à la fois, perte de mémoire entre sessions, projet qui devient lourd et fragile.

Ce document fige la doctrine : **qui fait quoi, avec quels droits, dans quel ordre, sur quels fichiers**. Il est la source unique de vérité pour tout agent (automatique ou IA conversationnelle) qui intervient sur le projet.

---

## 1. Principes non négociables

1. **Un agent = un rôle clair.** Pas d'agent multi-tâches fourre-tout.
2. **Aucune modification de code sans validation humaine.** Les agents lisent, analysent, classent, journalisent — ils ne commitent pas de code applicatif.
3. **Faits, hypothèses et suggestions séparés.** Un agent qui mélange les trois contamine le Brain.
4. **Le Brain ne se réécrit pas globalement.** On modifie les sections vivantes, jamais les documents sacrés.
5. **Chaque changement important est horodaté.** Format unique : `[YYYY-MM-DD HH:MM][BLOC][ZONE] message bref`.
6. **Les prompts Cursor doivent rester compacts.** 3 à 4 fichiers maximum par prompt, jamais plus.
7. **Les agents locaux absorbent le mécanique.** Claude absorbe le stratégique.

---

## 2. Architecture en deux familles

### Famille A — Automatique locale (agents Python exécutés sur le repo)

À automatiser dès la v1. Ces agents tournent après chaque commit (ou sur déclenchement manuel).

| Agent | Rôle | Écrit dans |
|-------|------|------------|
| **1. Brain Watcher** | Surveille l'activité du repo (git log, fichiers modifiés, branche, horodatage) | Journaux runtime uniquement |
| **2. Brain Mapper** | Classe chaque fichier modifié en BLOC / ZONE / SYSTÈME / owner / priorité, via `revizeus_mapper_rules.json` | Table de mapping |
| **3. Brain Diff Analyzer** | Comprend ce qui a réellement changé : comportement, systèmes impactés, risques | Rapports d'analyse (`09_RUNTIME_AGENT/LAST_ANALYSIS.json`) |
| **4. Brain Rules Guard** | Vérifie les règles sacrées : Compose interdit, ViewBinding, IDs sensibles, orientation, horodatage commentaires | Rapports QA (`10_RAPPORTS_QA/`) |
| **5. Brain Archivist** | Horodate et journalise l'évolution, met à jour l'état temps réel | `00_QUICK_START/ETAT_TEMPS_REEL.md`, `04_JOURNAUX/`, snapshots |
| **6. Brain Scribe** | Met à jour les sections *vivantes* du Brain (activité récente, progression bloc, zones sensibles) | Sections marquées "vivantes" uniquement |

### Famille B — Pilotée par Claude (intelligence conversationnelle)

À garder manuelle/intelligente au début. N'a pas besoin d'être automatisée tout de suite : un ou plusieurs prompts maîtres bien conçus suffisent.

| Agent | Rôle | Mode v1 |
|-------|------|---------|
| **7. Prompt Compressor** | Réduire le contexte à l'essentiel utile pour un ticket donné | **Claude** |
| **8. Prompt Builder Cursor** | Construire le prompt Cursor final compact et borné | **Claude** |
| **9. QA Reviewer** | Relire le résultat de Cursor : erreurs, oublis, régressions, conformité | **Claude** ou toi |

---

## 3. Ordre d'exécution des agents automatiques (Famille A)

Séquence stricte après commit :

```
1. Brain Watcher           → détecte
2. Brain Mapper            → classe
3. Brain Diff Analyzer     → comprend
4. Brain Rules Guard       → contrôle
5. Brain Archivist         → journalise
6. Brain Scribe            → écrit dans le Brain vivant
```

Raison de cet ordre : d'abord détecter, puis classer (on a besoin du classement pour comprendre), puis comprendre (pour contrôler), puis contrôler (pour journaliser avec verdict), puis écrire.

---

## 4. Matrice des droits d'écriture

### Zones autorisées aux agents automatiques

| Zone | Agents autorisés |
|------|------------------|
| `00_QUICK_START/ETAT_TEMPS_REEL.md` | Archivist, Scribe |
| `04_JOURNAUX/` | Archivist |
| `08_SNAPSHOTS/` | Archivist |
| `09_RUNTIME_AGENT/` | Watcher, Mapper, Diff Analyzer |
| `10_RAPPORTS_QA/` | Rules Guard |
| Sections "activité récente auto" de `INDEX_BLOCS.md` | Scribe |

### Zones strictement interdites aux agents automatiques

Jamais sans validation humaine explicite :

- `01_REGLES_SACREES/` (constitution, règles, protocoles, doctrine)
- `02_BLOCS/A_TERMINE/` et `B_A_FAIRE/` (contenu des blocs eux-mêmes)
- `03_SYSTEMES/` (cartographie métier structurante)
- `11_LORE/` (univers, dieux, bible mythologique, badges)
- `12_AVENTURE/` (assets aventure, créatures, storytelling)
- `13_IA_DOCS/` (documentation technique IA)
- `14_TAXONOMIE/` (langage officiel de classement)
- `05_DIRECTION_ARTISTIQUE/` (DA visuelle et sonore)

---

## 5. Protocole d'horodatage à trois niveaux

### Niveau 1 — Horodatage code (Kotlin / XML)

**Format unique et obligatoire :**

```kotlin
// [YYYY-MM-DD HH:MM][BLOC][ZONE] message bref factuel
```

**Exemples valides :**

```kotlin
// [2026-04-18 05:10][BLOC_B][RPG_DIALOG] Conversion du feedback toast en dialogue immersif.
// [2026-04-18 05:14][H_AVENTURE][COMBAT] Point d'entrée pour futur combat QCM-RPG Zeus.
// [2026-04-18 05:20][F_ECONOMIE][REWARDS] Ajustement du calcul XP ultime.
```

**Déclencheurs :**
- Nouveau bloc logique important
- Correction structurelle
- Workaround temporaire (à surveiller)
- Logique métier sensible
- Intégration liée à un bloc majeur

**Interdits :**
- Commentaire trop long (→ roman)
- Commentaire vague ("fix bug", "update")
- Commentaire sur chaque petite ligne
- Commentaire non daté quand le changement est important
- Commentaire sans BLOC/ZONE quand le changement est significatif

### Niveau 2 — Horodatage documentation vivante

**Format :**
```
[YYYY-MM-DD HH:MM] Élément : changement constaté.
```

**Exemple :**
```
[2026-04-18 05:10] GodMatiereActivity : feedback utilisateur partiellement migré du toast vers dialogue RPG.
```

### Niveau 3 — Horodatage Git (commit messages)

**Format :**
```
[BLOC] Description courte
```

**Exemples :**
```
[BLOC_B] Migration feedback vers dialogue RPG
[H_AVENTURE] Prépare structure combat temple Zeus
[CORE] Réorganise logique de mapping dieux/matières
```

---

## 6. Flux d'interaction complet

### Cas 1 — Idée ou bloc à lancer
1. Tu donnes l'idée à Claude
2. Claude la transforme en ticket propre (Prompt Compressor)
3. Claude compresse le contexte utile (fichiers, règles, état bloc)
4. Claude génère le prompt Cursor compact (Prompt Builder)
5. Tu colles dans Cursor

### Cas 2 — Cursor a codé
1. Tu récupères les fichiers modifiés
2. Les agents Famille A mettent à jour l'état réel automatiquement
3. Tu colles le retour à Claude si validation fine souhaitée
4. Claude fait la QA stratégique (QA Reviewer)
5. Tu valides ou refuses l'intégration

### Cas 3 — Après commit
1. Watcher détecte le commit
2. Mapper classe les fichiers modifiés
3. Diff Analyzer comprend l'impact
4. Rules Guard contrôle la conformité
5. Archivist horodate et journalise
6. Scribe met à jour le Brain vivant

---

## 7. Cartographie des scripts Python (convention)

Les scripts Famille A vivent à la racine du dépôt (ou dans un dossier dédié `scripts/` selon la configuration finale) :

```
brain_watcher.py
brain_mapper.py
brain_diff_analyzer.py
brain_rules_guard.py
brain_archivist.py
brain_scribe.py
```

Plus tard, éventuellement (pas en v1) :

```
brain_prompt_compressor.py   # Famille B automatisée
brain_prompt_builder.py      # Famille B automatisée
brain_qa_reviewer.py         # Famille B automatisée
```

---

## 8. Ce qu'on ne fait PAS en v1

Interdictions explicites pour ne pas bâtir une usine à gaz trop tôt :

- ❌ Agent autonome qui code tout seul
- ❌ Réécriture automatique complète du Brain
- ❌ Planner autonome global qui décide des priorités
- ❌ Réorganisation automatique de l'architecture code
- ❌ Modification silencieuse de documents stables
- ❌ Prompts Cursor générés sans supervision humaine
- ❌ Agent Famille B complètement automatisé (on garde Claude en pilote)

---

## 9. Sorties attendues du système

### Automatiquement (Famille A)
- État réel du projet à jour (`ETAT_TEMPS_REEL.md`)
- Journal récent horodaté (`04_JOURNAUX/JOURNAL_AGENT.md`)
- Mapping bloc/zone à jour (`09_RUNTIME_AGENT/file_block_map.json`)
- Résumé métier court après analyse (`09_RUNTIME_AGENT/LAST_ANALYSIS.json`)
- Rapport de règles (`10_RAPPORTS_QA/LAST_RULES_REPORT.md`)
- Snapshots horodatés (`08_SNAPSHOTS/`)

### À la demande via Claude (Famille B)
- Prompt Cursor compact et borné
- Cadrage de ticket (objectif, périmètre, hors-périmètre, contraintes)
- Audit stratégique sur fichier ou bloc
- QA finale avant intégration

---

## 10. Règle finale (mantra)

**Toi comme décideur final.
Claude comme cerveau stratégique (compression, cadrage, prompts Cursor, QA).
Cursor comme exécuteur de code.
Famille A comme mémoire et discipline automatisées.**

C'est le montage le plus robuste, le plus rentable en temps, et le moins risqué pour RéviZeus.

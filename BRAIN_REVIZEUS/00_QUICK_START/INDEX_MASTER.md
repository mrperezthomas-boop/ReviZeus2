# INDEX_MASTER — BRAIN RÉVIZEUS

**Version :** 2.0 consolidée
**Date :** 2026-04-18
**Rôle :** Point d'entrée unique pour tout agent ou humain qui ouvre le Brain.

---

## Comment lire ce Brain en 30 secondes

1. **Tu veux comprendre le produit ?** → `00_QUICK_START/PRESENTATION_COMPLETE.md` puis `01_REGLES_SACREES/CONSTITUTION_PROJET.md`
2. **Tu veux coder un bloc ?** → `13_IA_DOCS/01_SYSTEM_PROMPT_ULTIME.txt` + le bloc concerné dans `02_BLOCS/B_A_FAIRE/`
3. **Tu veux classer un fichier ou une idée ?** → `14_TAXONOMIE/INDEX_TAXONOMIQUE.md`
4. **Tu veux savoir qui modifie quoi quand ?** → `04_JOURNAUX/JOURNAL_AGENT.md` et `00_QUICK_START/ETAT_TEMPS_REEL.md`
5. **Tu veux les règles sacrées ?** → `01_REGLES_SACREES/RULES_SACREES.md` + `AGENTS_RULES.md`
6. **Tu veux comprendre l'équipe d'agents ?** → `01_REGLES_SACREES/DOCTRINE_EQUIPE_AGENTS.md`

---

## Hiérarchie de vérité (à appliquer en cas de conflit)

1. **Code réel du repo Android** (source absolue)
2. **Ressources réelles** présentes dans `res/`, `assets/`, `raw/`
3. **Constitution & règles sacrées** (`01_REGLES_SACREES/`)
4. **Taxonomie** (`14_TAXONOMIE/`)
5. **IA_DOCS** (`13_IA_DOCS/`)
6. **Cartographie runtime** (`03_SYSTEMES/`, `09_RUNTIME_AGENT/`)
7. **Blocs préparatoires** (`02_BLOCS/`)
8. **Archives** (`07_ARCHIVES/`) — référence, jamais vérité actuelle

---

## Carte des zones du Brain

| Zone | Nature | Stabilité | Qui peut écrire |
|------|--------|-----------|------------------|
| `00_QUICK_START/` | Point d'entrée, état live | Vivant | Humain + Archivist |
| `01_REGLES_SACREES/` | Constitution, règles, doctrine | **Sacré** | Humain uniquement |
| `02_BLOCS/` | Roadmap A → Q | Stable + section vivante | Humain + Scribe (section auto) |
| `03_SYSTEMES/` | Cartographie technique | Stable | Humain |
| `04_JOURNAUX/` | Horodatage runtime | Vivant | Archivist |
| `05_DIRECTION_ARTISTIQUE/` | DA visuelle & sonore | Stable | Humain |
| `06_CURSOR_SKILLS/` | Skills et rules Cursor | Stable | Humain |
| `07_ARCHIVES/` | Référence historique | **Figé** | Humain (append-only) |
| `08_SNAPSHOTS/` | Snapshots runtime | Vivant | Archivist |
| `09_RUNTIME_AGENT/` | Config et sorties agents | Vivant | Watcher, Mapper, Diff Analyzer |
| `10_RAPPORTS_QA/` | Contrôle des règles | Vivant | Rules Guard |
| `11_LORE/` | Univers, dieux, badges | Stable | Humain |
| `12_AVENTURE/` | Mode aventure (assets) | Stable | Humain |
| `13_IA_DOCS/` | Doc technique IA | Stable | Humain |
| `14_TAXONOMIE/` | Langage officiel | **Sacré** | Humain uniquement |

---

## Documents les plus consultés (top 10)

1. `01_REGLES_SACREES/CONSTITUTION_PROJET.md` — vision produit
2. `01_REGLES_SACREES/RULES_SACREES.md` — règles techniques absolues
3. `01_REGLES_SACREES/AGENTS_RULES.md` — règles pour IA
4. `01_REGLES_SACREES/DOCTRINE_EQUIPE_AGENTS.md` — équipe d'agents
5. `13_IA_DOCS/01_SYSTEM_PROMPT_ULTIME.txt` — system prompt maître
6. `13_IA_DOCS/02_BUILD_AND_TECH_STACK.md` — stack technique
7. `13_IA_DOCS/03_DATA_AND_MANAGERS.md` — managers et données
8. `14_TAXONOMIE/INDEX_TAXONOMIQUE.md` — langage de classement
9. `03_SYSTEMES/CARTOGRAPHIE_MASTER.md` — cartographie produit
10. `02_BLOCS/INDEX_BLOCS.md` — roadmap

---

## Terminologie projet (mini-glossaire)

- **Panthéon** : les 10 dieux / 10 matières scolaires
- **IAristote** : cœur IA pédagogique (génération résumé + QCM + feedback)
- **Oracle** : module de capture des cours (photo, PDF, texte)
- **Temple** : hub matière / zone aventure
- **Éclats de savoir** : monnaie principale
- **Ambroisie** : monnaie premium
- **Fragments** : ressources de progression par matière
- **Chaos** : antagoniste narratif (à restaurer)
- **Aventure** : mode RPG en paysage sur carte monde
- **Bloc** : tranche verticale de développement (A → Q)
- **BRAIN** : ce dossier, mémoire externe du projet

---

## Quand le Brain doit être mis à jour

Par un humain :
- Après **un bloc clôturé** (ajouter à `REVIZEUS_ETAT_CUMULATIF_DES_BLOCS_TERMINES.txt`)
- Après **une décision architecture importante** (mettre à jour `03_SYSTEMES/` et la taxonomie si besoin)
- Après **un changement de DA ou de règle sacrée** (éditer la zone concernée dans `01_REGLES_SACREES/` ou `05_DIRECTION_ARTISTIQUE/`)

Par les agents automatiques :
- Après chaque commit Git (Famille A complète)
- À la demande manuelle (`run_brain_agents.py --status`)

---

## Prochaine évolution prévue du Brain

- Ajout d'un dossier `prompts_cursor/` ou intégration dans `06_CURSOR_SKILLS/prompts/` quand la banque de prompts validés grossit
- Automatisation progressive de la Famille B (`brain_prompt_compressor.py`, `brain_prompt_builder.py`, `brain_qa_reviewer.py`)
- Extension taxonomique pour Panthéon Nordique et Égyptien (quand ces phases arriveront)

---

**Dernière consolidation : 2026-04-18**

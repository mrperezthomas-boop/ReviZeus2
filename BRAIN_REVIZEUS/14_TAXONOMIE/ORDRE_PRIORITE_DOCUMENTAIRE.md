# ORDRE DE PRIORITÉ DOCUMENTAIRE — RÉVIZEUS

## Rôle
Ce document définit l’ordre officiel de priorité documentaire pour :
- agents,
- analyses,
- audits,
- reconstructions de contexte,
- génération de prompts,
- QA.

---

## Principe
Toutes les informations du brain n’ont pas le même poids.

Les agents doivent toujours privilégier :
1. la doctrine stable,
2. la taxonomie,
3. la cartographie système,
4. le référentiel technique et métier,
5. le pilotage actif,
6. le runtime,
7. l’archive.

---

## Ordre officiel de priorité

### Priorité 0 — Doctrine canonique
1. `01_REGLES_SACREES`
2. `14_TAXONOMIE`
3. `03_SYSTEMES`
4. `13_IA_DOCS`

### Priorité 1 — Pilotage actif
5. `02_BLOCS`
6. `05_DIRECTION_ARTISTIQUE`
7. `11_LORE`
8. `12_AVENTURE`
9. `06_CURSOR_SKILLS`

### Priorité 2 — Runtime vivant
10. `00_QUICK_START`
11. `04_JOURNAUX`
12. `08_SNAPSHOTS`
13. `09_RUNTIME_AGENT`
14. `10_RAPPORTS_QA`

### Priorité 3 — Historique
15. `07_ARCHIVES`

---

## Interprétation correcte
### Si plusieurs sources parlent du même sujet :
- la plus haute priorité gagne,
- sauf si elle est explicitement obsolète et signalée comme telle.

### Si runtime et canonique se contredisent :
- le runtime devient un signal,
- pas une vérité auto-promue.

### Si archive et actuel se contredisent :
- l’actuel gagne,
- l’archive reste utile pour l’historique seulement.

---

## Usage par type d’agent
### Mapper
Lit en priorité :
- `14_TAXONOMIE`
- `03_SYSTEMES`

### Diff Analyzer
Lit en priorité :
- `14_TAXONOMIE`
- `03_SYSTEMES`
- `13_IA_DOCS`
- `01_REGLES_SACREES`

### Rules Guard
Lit en priorité :
- `01_REGLES_SACREES`
- `14_TAXONOMIE`

### Scribe / Archivist
Écrivent en runtime seulement après lecture des priorités supérieures.

---

## Règle finale
Le brain ne doit pas être lu comme un dossier plat.
Il doit être lu comme une pyramide de vérité.

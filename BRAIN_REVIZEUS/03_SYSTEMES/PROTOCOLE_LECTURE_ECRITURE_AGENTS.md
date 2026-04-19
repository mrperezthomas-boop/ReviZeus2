# PROTOCOLE DE LECTURE / ÉCRITURE DES AGENTS — VRAI BRAIN REVIZEUS

## Objectif
Définir précisément :
- ce que les agents doivent lire en priorité,
- ce qu’ils peuvent écrire,
- ce qu’ils n’ont pas le droit de modifier,
- comment ils doivent utiliser la taxonomie.

---

## Principe général
Les agents sont des opérateurs de consolidation et de runtime.
Ils ne sont pas les auteurs souverains de la doctrine projet.

Donc :
- ils lisent largement,
- ils écrivent localement,
- ils n’éditent pas librement les référentiels canoniques.

---

## Ordre obligatoire de lecture
Tout agent sérieux doit lire dans cet ordre :

1. `BRAIN_REVIZEUS/01_REGLES_SACREES`
2. `BRAIN_REVIZEUS/14_TAXONOMIE`
3. `BRAIN_REVIZEUS/03_SYSTEMES`
4. `BRAIN_REVIZEUS/13_IA_DOCS`
5. `BRAIN_REVIZEUS/02_BLOCS`
6. `BRAIN_REVIZEUS/05_DIRECTION_ARTISTIQUE`
7. `BRAIN_REVIZEUS/11_LORE`
8. `BRAIN_REVIZEUS/12_AVENTURE`
9. `BRAIN_REVIZEUS/00_QUICK_START`
10. `BRAIN_REVIZEUS/04_JOURNAUX`
11. `BRAIN_REVIZEUS/08_SNAPSHOTS`
12. `BRAIN_REVIZEUS/09_RUNTIME_AGENT`
13. `BRAIN_REVIZEUS/10_RAPPORTS_QA`
14. `BRAIN_REVIZEUS/07_ARCHIVES`

---

## Zones d’écriture autorisées pour les agents
Écriture autorisée uniquement dans :
- `BRAIN_REVIZEUS/00_QUICK_START`
- `BRAIN_REVIZEUS/04_JOURNAUX`
- `BRAIN_REVIZEUS/08_SNAPSHOTS`
- `BRAIN_REVIZEUS/09_RUNTIME_AGENT`
- `BRAIN_REVIZEUS/10_RAPPORTS_QA`

---

## Zones d’écriture interdites
Écriture interdite automatiquement dans :
- `BRAIN_REVIZEUS/01_REGLES_SACREES`
- `BRAIN_REVIZEUS/03_SYSTEMES`
- `BRAIN_REVIZEUS/05_DIRECTION_ARTISTIQUE`
- `BRAIN_REVIZEUS/07_ARCHIVES`
- `BRAIN_REVIZEUS/11_LORE`
- `BRAIN_REVIZEUS/13_IA_DOCS`
- `BRAIN_REVIZEUS/14_TAXONOMIE`

Écriture fortement limitée / humaine prioritaire dans :
- `BRAIN_REVIZEUS/02_BLOCS`
- `BRAIN_REVIZEUS/06_CURSOR_SKILLS`
- `BRAIN_REVIZEUS/12_AVENTURE`

---

## Rôle attendu des agents
### `brain_mapper`
Doit :
- classer selon `14_TAXONOMIE`
- croiser avec `03_SYSTEMES`
- ne pas inventer de nouveaux domaines

### `brain_diff_analyzer`
Doit :
- produire domaine / sous-domaine / owner / criticité / niveau de vérité
- croiser code réel + docs canoniques
- séparer fait observé / inférence / suggestion

### `brain_rules_guard`
Doit :
- vérifier les règles sacrées
- vérifier les zones d’écriture
- signaler toute dérive documentaire ou structurelle

### `brain_archivist`
Doit :
- journaliser brièvement
- horodater
- ne pas transformer le journal en encyclopédie

### `brain_scribe`
Doit :
- écrire seulement dans les zones runtime autorisées
- ne jamais réécrire les référentiels canoniques

---

## Protocole d’annotation
### Format documentaire recommandé
`[YYYY-MM-DD HH:MM][DOMAINE][SOUS_DOMAINE] message bref`

Exemple :
`[2026-04-18 05:10][PRODUCTION_BRAIN][runtime_agent] Consolidation automatique après commit.`

### Règles
- bref
- factuel
- traçable
- pas de roman
- pas de formulation ambiguë

---

## Règle de vérité
Quand une information runtime contredit un document canonique :
1. le document canonique garde la priorité,
2. la contradiction doit être signalée,
3. aucune écriture de correction automatique n’est faite dans le canonique,
4. une revue humaine est requise.

---

## Règle de sécurité
Aucun agent ne doit :
- reformuler silencieusement la doctrine,
- fusionner cible et actuel,
- promouvoir une archive en vérité active sans validation,
- écrire dans la taxonomie,
- écrire dans les règles sacrées.

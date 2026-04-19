# REGISTRE DE VÉRITÉ DOCUMENTAIRE — BRAIN REVIZEUS

## Rôle
Ce registre indique, pour chaque zone du vrai brain :
- sa fonction,
- son statut de vérité,
- sa priorité de lecture,
- sa priorité d’écriture,
- son owner logique,
- son niveau de stabilité.

---

## Légende
### Statuts de vérité
- `SACRE` : doctrine stable, non éditable automatiquement
- `ACTUEL` : vérité documentaire courante
- `CIBLE` : état souhaité non nécessairement implémenté
- `BACKLOG` : reste à faire validé
- `RUNTIME` : produit automatiquement ou semi-automatiquement
- `ARCHIVE` : historique utile, non canonique sans validation

### Priorité de lecture
- `P0` : à lire d’abord
- `P1` : très important
- `P2` : secondaire mais utile
- `P3` : contexte complémentaire
- `P4` : archive ou support

### Priorité d’écriture
- `W0` : écriture interdite aux agents
- `W1` : écriture très limitée / humaine
- `W2` : écriture agent autorisée sur fichiers ciblés
- `W3` : écriture runtime libre encadrée

---

## Registre

| Dossier | Fonction | Statut | Lecture | Écriture | Owner | Stabilité |
|---|---|---:|---:|---:|---|---|
| `00_QUICK_START` | vue rapide / état consolidé | ACTUEL / RUNTIME | P2 | W2 | production_brain_system | moyenne |
| `01_REGLES_SACREES` | contraintes absolues | SACRE | P0 | W0 | governance_core | très élevée |
| `02_BLOCS` | pilotage d’implémentation | ACTUEL / BACKLOG | P2 | W1 | delivery_system | moyenne |
| `03_SYSTEMES` | cartographie et gouvernance | ACTUEL / SACRE | P0 | W0 | system_architecture_core | élevée |
| `04_JOURNAUX` | journal vivant | RUNTIME | P3 | W3 | production_brain_system | faible |
| `05_DIRECTION_ARTISTIQUE` | DA et ressenti | SACRE / ACTUEL | P2 | W0 | art_direction_system | élevée |
| `06_CURSOR_SKILLS` | outillage Cursor | ACTUEL | P2 | W1 | ai_ops | moyenne |
| `07_ARCHIVES` | mémoire historique | ARCHIVE | P4 | W0 | archive_system | variable |
| `08_SNAPSHOTS` | états figés auto | RUNTIME | P3 | W3 | runtime_snapshot_system | faible |
| `09_RUNTIME_AGENT` | sorties machine | RUNTIME | P3 | W3 | agent_runtime_system | faible |
| `10_RAPPORTS_QA` | rapports de contrôle | RUNTIME / ACTUEL | P3 | W3 | qa_system | faible |
| `11_LORE` | univers narratif officiel | ACTUEL / SACRE | P2 | W0 | narrative_system | élevée |
| `12_AVENTURE` | design aventure | ACTUEL / CIBLE | P2 | W1 | adventure_design_system | moyenne |
| `13_IA_DOCS` | référentiel technique et métier | ACTUEL / SACRE | P0 | W0 | knowledge_core_system | élevée |
| `14_TAXONOMIE` | classification officielle | SACRE | P0 | W0 | taxonomy_core_system | très élevée |

---

## Décision opérationnelle
Pour toute reconstruction sérieuse du projet, l’ordre de confiance documentaire est :

1. `01_REGLES_SACREES`
2. `14_TAXONOMIE`
3. `03_SYSTEMES`
4. `13_IA_DOCS`
5. `02_BLOCS`
6. `05_DIRECTION_ARTISTIQUE`
7. `11_LORE`
8. `12_AVENTURE`
9. `00_QUICK_START`
10. `04_JOURNAUX`
11. `08_SNAPSHOTS`
12. `09_RUNTIME_AGENT`
13. `10_RAPPORTS_QA`
14. `07_ARCHIVES`

---

## Règle clé pour les agents
Un agent ne doit jamais traiter un dossier `RUNTIME` comme plus vrai qu’un dossier `SACRE` ou `ACTUEL` canonique.

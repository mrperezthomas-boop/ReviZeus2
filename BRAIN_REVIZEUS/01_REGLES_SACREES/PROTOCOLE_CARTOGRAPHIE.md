# PROTOCOLE DE CARTOGRAPHIE — RÉVIZEUS

## Objectif
Donner au mapper et aux humains une méthode stable de classement.

## Ordre de matching
1. nom de classe/fichier exact (`explicit_stem_map`)
2. chemin contenant un segment métier (`path_contains_rules`)
3. préfixe du stem (`prefix_stem_rules`)
4. suffixe du stem (`suffix_stem_rules`)
5. mot-clé du stem (`keyword_stem_rules`)
6. fallback `UNKNOWN`

## Quand ajouter une règle explicite
Ajouter une règle explicite si :
- la classe est P0 ou P1,
- la classe porte une logique métier significative,
- la classe est souvent modifiée,
- une erreur de classement ferait dériver le prompt Cursor.

## Quand une règle fallback suffit
Le fallback suffit pour :
- layouts XML périphériques,
- petits adapters non critiques,
- classes de support temporaires,
- docs non stratégiques.

## Maintenance
À chaque fois que les agents détectent `UNKNOWN` sur une classe importante :
1. ajouter la classe au mapping explicite,
2. préciser bloc + zone + système + criticité + owner,
3. relancer `run_brain_agents.py --status`.

# 08 — CONVENTIONS MASTER RÉVIZEUS

## Convention de nommage taxonomique
Format conceptuel :

`DOMAINE > sous_domaine > élément`

### Exemples
- `JEU > aventure > combat_qcm_rpg`
- `IA > personnalisation_dialogues > affinité_divine`
- `UX_IMMERSION > dialogues_rpg > feedback_erreur`
- `TECH_CORE > build_system > gradle_config`
- `PRODUCTION_BRAIN > blocs > bloc_b_dialogues_rpg`

## Convention d’horodatage
Format recommandé :

`[YYYY-MM-DD HH:MM][DOMAINE][SOUS_DOMAINE] message bref`

### Exemples
- `[2026-04-18 05:10][JEU][aventure] Préparation du flux de combat QCM-RPG Zeus.`
- `[2026-04-18 05:14][UX_IMMERSION][dialogues_rpg] Conversion d’un feedback toast vers dialogue immersif.`
- `[2026-04-18 05:16][IA][personnalisation_dialogues] Point d’entrée prévu pour modulation par affinité divine.`

## Convention de criticité
- P1 = critique vitale
- P2 = critique haute
- P3 = importante
- P4 = notable
- P5 = utile
- P6 = secondaire
- P7 = faible
- P8 = mineure
- P9 = documentaire / faible impact

## Convention d’owner logique
Chaque élément important doit pouvoir être rattaché à un owner logique.
Exemples :
- `dialogue_system`
- `adventure_system`
- `economy_system`
- `ai_adaptation_system`
- `tech_core`
- `brain_core`
- `narrative_system`

## Règle
Un agent ou un humain ne doit pas décrire un changement uniquement par un nom de fichier.
Il doit pouvoir lui attribuer :
- un domaine,
- un sous-domaine,
- un type d’entité,
- un niveau de vérité,
- une criticité,
- un owner logique.

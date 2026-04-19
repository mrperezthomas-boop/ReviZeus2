<!-- Horodatage logique : 2026-04-19 | Changelog patch direct panthéon -->
# CHANGELOG — PATCH DIRECT PANTHÉON 2026-04-19

## Objectif
Corriger les fichiers Brain contradictoires pour imposer la vérité canonique du panthéon RéviZeus et empêcher les dérives de mapping matière.

## Fichiers à remplacer directement
- `BRAIN_REVIZEUS/11_LORE/DIEUX_PERSONNALITES.md`
- `BRAIN_REVIZEUS/11_LORE/BIBLE_MYTHOLOGIQUE.md`

## Fichier à ajouter
- `BRAIN_REVIZEUS/06_CURSOR_SKILLS/prompts/validated/PROMPT_PATCH_DIALOGUES_DIVINS_CANONIQUES_2026-04-19.txt`

## Corrections canoniques verrouillées
- Déméter = **Géographie**
- Apollon = **Philo / SES**
- Héphaïstos = **Physique-Chimie**
- Hermès = **Langues / Anglais**
- Prométhée = **Vie & Projets**
- Aphrodite = **Art**

## Règle désormais explicite
La personnalité influence le **style de parole**.
Elle ne redéfinit jamais la **matière**.

## Risques supprimés par ce patch
- remapping des dieux à partir du ton ;
- confusion entre rôle symbolique et matière scolaire ;
- prompts futurs qui dérivent à cause de docs lore contradictoires ;
- générations IA incohérentes entre fichiers.

## Ordre de lecture recommandé pour les IA
1. `BIBLE_MYTHOLOGIQUE.md`
2. `DIEUX_PERSONNALITES.md`
3. prompt(s) validés de génération / patch

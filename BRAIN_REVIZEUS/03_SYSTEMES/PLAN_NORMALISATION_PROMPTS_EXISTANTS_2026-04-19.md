<!-- Horodatage logique : 2026-04-19 | Objet : plan minimal de normalisation des prompts -->
# PLAN_NORMALISATION_PROMPTS_EXISTANTS_2026-04-19

## Objectif

Normaliser les prompts et docs Brain qui peuvent générer ou piloter des dialogues divins, sans refonte globale.

---

## Zone la plus sensible

### P0 — à vérifier / patcher en premier
- `BRAIN_REVIZEUS/11_LORE/DIEUX_PERSONNALITES.md`
- `BRAIN_REVIZEUS/11_LORE/BIBLE_MYTHOLOGIQUE.md`
- `BRAIN_REVIZEUS/02_BLOCS/B_A_FAIRE/BLOC_B_DIALOGUES_RPG.txt`
- `BRAIN_REVIZEUS/06_CURSOR_SKILLS/CURSOR_SKILLS.md`
- `BRAIN_REVIZEUS/06_CURSOR_SKILLS/prompts/README.md`
- tout prompt déjà stocké dans `BRAIN_REVIZEUS/06_CURSOR_SKILLS/prompts/validated/`

### P1 — à vérifier ensuite
- prompts Oracle
- prompts quiz
- prompts onboarding / tutoriels divins
- prompts fatigue divine / quotas API
- prompts aventure avec interventions des dieux

---

## Stratégie minimale

1. **Ajouter une vérité supérieure**
   - Ne pas se contenter de corriger un seul fichier.
   - Toujours relier les prompts sensibles à une norme explicite.

2. **Corriger les dérives historiques**
   - Couleurs erronées
   - Matières fusionnées
   - Storytelling transformé à tort en matière

3. **Séparer les couches**
   - matière
   - personnalité
   - DA
   - usage produit

4. **Insérer des garde-fous textuels**
   - phrase canonique de non-remapping

---

## Définition de done

Le travail est considéré comme fini quand :
- un agent peut lire les prompts sans remapper les dieux,
- deux IA différentes arrivent à la même table des matières divines,
- les dialogues restent variés selon les personnalités,
- aucune matière n'est déduite à partir du style de parole.

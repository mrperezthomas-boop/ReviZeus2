# CHANGELOG_PATCH_07_LORE_BIBLE_2026-04-19.md
<!-- Horodatage logique : 2026-04-19 -->

## Objet
Patch direct du fichier `BRAIN_REVIZEUS/13_IA_DOCS/07_LORE_BIBLE.md` afin de supprimer les ambiguïtés de panthéon susceptibles de recontaminer les générations IA.

## But
Transformer `07_LORE_BIBLE.md` en source agrégée fiable, alignée avec :
- le panthéon canonique
- les fichiers lore corrigés
- les règles de dialogue divin
- la logique Brain/Cursor/IA

## Changements majeurs
1. Réécriture de la section panthéon sous forme canonique explicite.
2. Verrouillage de la règle :
   - matière fixe
   - personnalité = style de dialogue
3. Ajout des interdits de dérive dieu par dieu.
4. Ajout d’une grille d’usage opérationnelle pour les IA.
5. Ajout d’une clause de supériorité documentaire.

## Arbitrages verrouillés
- Zeus = Mathématiques
- Athéna = Français
- Poséidon = SVT
- Arès = Histoire
- Déméter = Géographie
- Aphrodite = Art
- Hermès = Langues / Anglais
- Héphaïstos = Physique-Chimie
- Apollon = Philo / SES
- Prométhée = Vie & Projets

## Effet attendu
Après remplacement de `07_LORE_BIBLE.md`, une IA qui lit ce fichier ne doit plus :
- remapper un dieu selon son ambiance
- confondre Apollon avec Musique par défaut
- confondre Héphaïstos avec technologie/ingénierie comme matière
- confondre Déméter avec mémoire/révision comme matière
- confondre Hermès avec le sport

## Action d’intégration
1. Sauvegarder l’ancienne version de `07_LORE_BIBLE.md`
2. Remplacer par la version présente dans ce patch
3. Garder le changelog dans `03_SYSTEMES`
4. Laisser les prompts de normalisation et de patch déjà générés comme couche complémentaire

## Risque si non appliqué
Le fichier agrégé `07_LORE_BIBLE.md` peut continuer à contaminer :
- prompts
- agents
- IA externes
- futures consolidations documentaires

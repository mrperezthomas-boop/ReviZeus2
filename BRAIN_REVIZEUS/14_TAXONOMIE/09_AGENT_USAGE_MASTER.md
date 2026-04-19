# 09 — AGENT USAGE MASTER RÉVIZEUS

## Finalité
Donner aux agents une façon propre d’utiliser la taxonomie.

## Ordre de classement attendu
Tout changement, fichier, ticket ou système doit être analysé dans cet ordre :
1. domaine majeur
2. sous-domaine
3. type d’entité
4. niveau de vérité
5. statut de stabilité
6. owner logique
7. criticité

## Usage par agent

### Brain Mapper
Doit utiliser :
- `01_TAXONOMIE_MASTER.md`
- `02_ENTITES_MASTER.md`
- `08_CONVENTIONS_MASTER.md`

But :
transformer un chemin de fichier ou un nom de classe en classement structuré.

### Brain Diff Analyzer
Doit utiliser :
- `01_TAXONOMIE_MASTER.md`
- `03_JEU_MASTER.md`
- `05_RELATIONS_MASTER.md`
- `07_DOCUMENTS_VERITE_MASTER.md`

But :
produire un résumé métier qui ne mélange pas code, cible, archive et backlog.

### Brain Rules Guard
Doit utiliser :
- `08_CONVENTIONS_MASTER.md`
- la doctrine projet
- les règles sacrées

But :
vérifier conformité, horodatage, ownership et cohérence de classement.

### Brain Scribe
Doit utiliser :
- `07_DOCUMENTS_VERITE_MASTER.md`
- `08_CONVENTIONS_MASTER.md`

But :
mettre à jour les journaux et états vivants sans contaminer les documents sacrés.

## Interdits
- réécrire librement la taxonomie
- inventer un domaine
- confondre une hypothèse avec un fait certain
- écraser les niveaux de vérité
- créer des sous-catégories sauvages sans validation

## Sortie attendue type
Un agent doit pouvoir produire une lecture structurée du type :

- domaine : `UX_IMMERSION`
- sous_domaine : `dialogues_rpg`
- entité : `feedback`
- vérité : `certain`
- stabilité : `vivant`
- owner : `dialogue_system`
- criticité : `P2`

## Règle finale
La taxonomie est une fondation stable.
Les agents la consultent.
Ils ne la gouvernent pas.

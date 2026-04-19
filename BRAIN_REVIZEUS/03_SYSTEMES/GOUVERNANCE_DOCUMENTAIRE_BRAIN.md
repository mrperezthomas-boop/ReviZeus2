# GOUVERNANCE DOCUMENTAIRE DU VRAI BRAIN REVIZEUS

## Rôle
Ce document formalise la fonction officielle de chaque dossier du vrai `BRAIN_REVIZEUS`.

Il doit servir de référence pour :
- les agents,
- les futures discussions IA,
- la consolidation documentaire,
- la QA documentaire,
- la taxonomie.

---

## Principe directeur
Le brain n’est pas une accumulation de textes.
Le brain est un système documentaire gouverné.

Chaque dossier doit avoir :
- une fonction,
- un niveau de vérité,
- un niveau de stabilité,
- un niveau d’autorisation d’écriture,
- un owner logique.

---

## Fonction officielle des dossiers

### `00_QUICK_START`
**Fonction :** vue rapide du projet, entrée courte, état temps réel consolidé.  
**Nature :** documentaire vivant.  
**Écriture agent :** oui, autorisée sur fichiers runtime ciblés.  
**Owner logique :** `production_brain_system`.

### `01_REGLES_SACREES`
**Fonction :** contraintes absolues, interdits, règles non négociables.  
**Nature :** doctrine stable.  
**Écriture agent :** non.  
**Owner logique :** `governance_core`.

### `02_BLOCS`
**Fonction :** pilotage par blocs, état fait / à faire, séquencement de livraison.  
**Nature :** planification et delivery.  
**Écriture agent :** limitée aux sections prévues, de préférence humaine.  
**Owner logique :** `delivery_system`.

### `03_SYSTEMES`
**Fonction :** cartographie consolidée des systèmes, domaines, owners, gouvernance documentaire.  
**Nature :** référentiel structurel.  
**Écriture agent :** non par défaut, sauf sections explicitement runtime si créées un jour.  
**Owner logique :** `system_architecture_core`.

### `04_JOURNAUX`
**Fonction :** journal vivant synthétique des évolutions et signaux importants.  
**Nature :** runtime documentaire.  
**Écriture agent :** oui.  
**Owner logique :** `production_brain_system`.

### `05_DIRECTION_ARTISTIQUE`
**Fonction :** règles de DA, ressenti, ton visuel et audio.  
**Nature :** référentiel créatif stable.  
**Écriture agent :** non.  
**Owner logique :** `art_direction_system`.

### `06_CURSOR_SKILLS`
**Fonction :** outillage Cursor, règles d’exécution, prompts d’environnement.  
**Nature :** outillage de production.  
**Écriture agent :** non par défaut.  
**Owner logique :** `ai_ops`.

### `07_ARCHIVES`
**Fonction :** conservation historique, anciennes versions, récapitulatifs passés.  
**Nature :** archive.  
**Écriture agent :** non.  
**Owner logique :** `archive_system`.

### `08_SNAPSHOTS`
**Fonction :** états figés générés automatiquement.  
**Nature :** mémoire runtime figée.  
**Écriture agent :** oui.  
**Owner logique :** `runtime_snapshot_system`.

### `09_RUNTIME_AGENT`
**Fonction :** sorties machine, analyses techniques, configs et résultats runtime.  
**Nature :** mémoire technique machine.  
**Écriture agent :** oui.  
**Owner logique :** `agent_runtime_system`.

### `10_RAPPORTS_QA`
**Fonction :** rapports de contrôle, vérifications, violations et observations QA.  
**Nature :** contrôle qualité.  
**Écriture agent :** oui.  
**Owner logique :** `qa_system`.

### `11_LORE`
**Fonction :** univers narratif officiel, mythologie, personnalités, bible de monde.  
**Nature :** référentiel narratif.  
**Écriture agent :** non.  
**Owner logique :** `narrative_system`.

### `12_AVENTURE`
**Fonction :** conception et structure du mode aventure, contenus, progression, cartes, bestiaire lié.  
**Nature :** conception système + contenu.  
**Écriture agent :** non par défaut.  
**Owner logique :** `adventure_design_system`.

### `13_IA_DOCS`
**Fonction :** référentiel technique et métier principal pour le projet, documentation de fond.  
**Nature :** documentation canonique principale.  
**Écriture agent :** non.  
**Owner logique :** `knowledge_core_system`.

### `14_TAXONOMIE`
**Fonction :** référentiel officiel de classification : domaines, entités, relations, documents, vérité, conventions.  
**Nature :** colonne de lecture des agents et du brain.  
**Écriture agent :** non.  
**Owner logique :** `taxonomy_core_system`.

---

## Décision canonique
La lecture du brain par les agents doit considérer :
- `01_REGLES_SACREES`
- `03_SYSTEMES`
- `13_IA_DOCS`
- `14_TAXONOMIE`

comme **socle documentaire canonique**.

Le reste sert :
- au pilotage,
- au runtime,
- au narratif,
- à l’aventure,
- à l’archive,
- au support de production.

---

## Règle de non-régression documentaire
Aucun agent ne doit :
- réécrire les règles sacrées,
- réinterpréter la taxonomie,
- modifier silencieusement les docs canoniques,
- fusionner archive et vérité actuelle,
- écraser un document de fond avec une synthèse runtime.

---

## Règle d’évolution
Toute évolution d’un document canonique doit être :
- explicitement demandée,
- relue humainement,
- alignée avec la taxonomie,
- cohérente avec la hiérarchie de vérité.

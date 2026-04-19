---
document_id: IA_DOC_08_TAXONOMIE
type: taxonomy
stability: sacré
truth_level: certain
owner: brain_core
criticality: P0
version: 1.0
last_updated: 2026-04-18
source: Brain_ReviZeus/14_TAXONOMIE/ (10 fichiers fusionnés)
---

# 08 — TAXONOMIE COMPLÈTE RÉVIZEUS (fusion)

## Usage IA
Ce document contient **le langage officiel de classement** de RéviZeus. Avant de classer, nommer, horodater ou ranger quoi que ce soit, consulte-le. La taxonomie sert à classer, clarifier, stabiliser, éviter le téléphone arabe. Elle ne sert pas à créer des centaines de fichiers inutiles.

**Règle de lecture** : tout élément doit être lisible sous la forme `DOMAINE > sous_domaine > élément` avec :
1. domaine majeur
2. sous-domaine
3. type d'entité
4. niveau de vérité
5. statut de stabilité
6. owner logique
7. criticité

---

## SECTION 0 — INDEX TAXONOMIQUE

# INDEX TAXONOMIQUE RÉVIZEUS

## Finalité
Ce dossier donne le langage officiel de classement de RéviZeus.

## Fichiers
- `01_TAXONOMIE_MASTER.md` → domaines majeurs et sous-domaines
- `02_ENTITES_MASTER.md` → entités officielles du système
- `03_JEU_MASTER.md` → taxonomie gameplay
- `04_DIALOGUES_MASTER.md` → types de dialogues
- `05_RELATIONS_MASTER.md` → relations officielles entre entités / systèmes
- `06_UX_IMMERSION_MASTER.md` → feedbacks, ambiance, immersion, rituels UX
- `07_DOCUMENTS_VERITE_MASTER.md` → types de documents, niveaux de vérité, statuts
- `08_CONVENTIONS_MASTER.md` → nommage, horodatage, criticité, ownership
- `09_AGENT_USAGE_MASTER.md` → comment les agents doivent lire et utiliser la taxonomie

## Doctrine
La taxonomie sert à :
- classer,
- clarifier,
- stabiliser,
- éviter le téléphone arabe.

Elle ne sert pas à créer des centaines de fichiers inutiles.

## Règle de lecture
Toujours lire un élément selon :
1. domaine majeur
2. sous-domaine
3. type d’entité
4. niveau de vérité
5. statut de stabilité
6. owner logique
7. criticité

---

## SECTION 01

# 01 — TAXONOMIE MASTER RÉVIZEUS

## Domaine 1 — ESSENCE
Décrit ce que RéviZeus est fondamentalement.

### Sous-domaines
- vision_produit
- promesse_joueur
- différenciation
- identité_émotionnelle
- règles_identitaires
- état_cible_final

### Rôle
Définir l’âme immuable du projet.

### Stabilité
Très élevée.

---

## Domaine 2 — MYTHOLOGIE
Décrit l’univers, les dieux, le chaos, les temples et la symbolique.

### Sous-domaines
- panthéon
- dieux_personnalités
- chaos
- temples
- cosmologie
- lore_global
- humour_divin
- symbolique

### Rôle
Définir l’univers narratif et mythologique vivant.

### Stabilité
Élevée.

---

## Domaine 3 — HÉROS
Décrit l’utilisateur comme héros vivant de RéviZeus.

### Sous-domaines
- profil_héros
- humeur
- personnalité
- progression_héros
- erreurs_récurrentes
- affinités_divines
- historique_parcours
- préférences_apprentissage

### Rôle
Porter l’adaptation et la personnalisation.

### Stabilité
Moyenne à élevée.

---

## Domaine 4 — JEU
Décrit les systèmes de gameplay.

### Sous-domaines
- oracle
- quiz
- entraînement
- aventure
- progression_temples
- récompenses
- badges
- économie
- inventaire
- forge
- savoirs
- bestiaire
- boss
- événements_divins

### Rôle
Porter la boucle ludique.

### Stabilité
Moyenne.

---

## Domaine 5 — IA
Décrit les systèmes intelligents et adaptatifs.

### Sous-domaines
- génération_questions
- personnalisation_dialogues
- mémoire_pédagogique
- profils_apprenants
- recommandation
- narration_adaptative
- fatigue_divine
- gouvernance_ia
- quotas_et_limites
- modèles_et_outillage

### Rôle
Porter la couche d’intelligence adaptative.

### Stabilité
Moyenne.

---

## Domaine 6 — UX_IMMERSION
Décrit la manière dont RéviZeus est ressenti.

### Sous-domaines
- dialogues_rpg
- feedbacks
- onboarding
- audio
- animations
- transitions
- ambiance
- lisibilité
- rythme_expérience
- rituels_visuels

### Rôle
Protéger l’immersion, la lisibilité et la qualité perçue.

### Stabilité
Élevée.

---

## Domaine 7 — TECH_CORE
Décrit le corps technique réel du projet.

### Sous-domaines
- architecture
- navigation
- persistence
- data_models
- managers
- ressources
- build_system
- intégrations
- sécurité
- conventions_code
- outils_dev

### Rôle
Porter la faisabilité technique.

### Stabilité
Élevée.

---

## Domaine 8 — PRODUCTION_BRAIN
Décrit la mémoire de pilotage et de production.

### Sous-domaines
- blocs
- roadmap
- journaux
- règles_sacrées
- cartographie
- prompts
- qa
- snapshots
- archives
- ownership
- runtime_agent

### Rôle
Garder RéviZeus gouvernable dans le temps.

### Stabilité
Variable selon sous-zone.

---

## Règle de classement
Tout élément RéviZeus doit pouvoir être lu sous la forme :

`DOMAINE > sous_domaine > élément`

### Exemples
- `JEU > aventure > combat_qcm_rpg`
- `IA > personnalisation_dialogues > affinité_divine`
- `UX_IMMERSION > dialogues_rpg > feedback_erreur`
- `TECH_CORE > managers > gemini_manager`
- `PRODUCTION_BRAIN > blocs > bloc_b_dialogues_rpg`

---

## SECTION 02

# 02 — ENTITÉS MASTER RÉVIZEUS

## Entités monde
- dieu
- temple
- chaos
- monde
- zone
- créature
- boss
- événement_divin

## Entités héros
- héros
- profil
- humeur
- personnalité
- affinité_divine
- historique_apprentissage
- erreur_récurrente

## Entités pédagogiques
- savoir
- matière
- leçon
- question
- quiz
- feedback_pédagogique
- recommandation

## Entités progression
- niveau
- xp
- badge
- rang
- récompense
- fragment
- éclat
- ambroisie
- succès

## Entités système
- dialogue
- scène
- animation
- musique
- effet
- transition
- ressource
- manager
- écran
- route
- données_utilisateur

## Entités production
- bloc
- ticket
- règle
- snapshot
- archive
- journal
- prompt
- rapport_qa
- mapping
- owner

## Convention de lecture
Quand une idée, un fichier ou une feature est analysé, il faut identifier :
1. les entités principales impliquées
2. les entités secondaires impactées
3. les entités visibles pour le joueur
4. les entités invisibles mais structurantes

## Distinction très importante
### Visible
- dialogue affiché
- récompense montrée
- temple restauré
- badge reçu
- question posée

### Invisible
- affinité divine
- profil d’erreurs
- score relationnel
- seuil de fatigue divine
- règle d’adaptation
- ownership documentaire

---

## SECTION 03

# 03 — JEU MASTER RÉVIZEUS

## Boucles majeures
- boucle_oracle
- boucle_quiz
- boucle_entraînement
- boucle_aventure
- boucle_progression
- boucle_récompense
- boucle_collection
- boucle_personnalisation
- boucle_relation_divine

## Sous-systèmes officiels
### Oracle
- résumé / génération / guidance / pédagogie divine

### Quiz
- génération / difficulté / structure des questions / feedbacks / correction

### Entraînement
- sélection matière / niveau / cadence / mode avec ou sans timer

### Aventure
- carte monde / temples / nœuds / combats QCM-RPG / restauration du monde / bestiaire / boss

### Progression temples
- niveaux temple / restauration / ambiance / évolution visuelle et sonore

### Récompenses
- xp / éclats / ambroisie / fragments / drops / succès

### Badges
- conditions / rareté / affichage / progression méta

### Économie
- monnaies / coûts / gains / équilibrage

### Inventaire / forge
- objets / ressources / usages / rareté / fabrication

### Savoirs
- capture / stockage / édition / export / transformations divines

### Bestiaire / boss
- créatures / drops / comportement / difficulté / encyclopédie

## Règle
Un système de jeu doit toujours être défini par :
- son objectif joueur,
- son objectif produit,
- ses entrées,
- ses sorties,
- ses dépendances,
- ses feedbacks visibles,
- ses variables cachées,
- ses points de progression.

---

## SECTION 04

# 04 — DIALOGUES MASTER RÉVIZEUS

## Types de dialogues d’entrée
- onboarding
- première_découverte
- tutoriel
- introduction_divine

## Types de dialogues pédagogiques
- explication
- recadrage
- correction
- encouragement
- reformulation
- recommandation

## Types de dialogues émotionnels
- consolation
- félicitation
- motivation
- teasing
- familiarité_divine
- complicité

## Types de dialogues narratifs
- révélation_lore
- transition_monde
- progression_temple
- apparition_chaos
- victoire
- défaite
- boss_intro

## Types de dialogues techniques diégétiques
- fatigue_divine
- quota_atteint
- attente
- indisponibilité
- erreur_transformée_en_lore

## Règles
- un dialogue n’est jamais un simple texte neutre si une forme RPG immersive est requise
- le type de dialogue doit être identifiable
- le ton doit rester cohérent avec le dieu, le contexte et le niveau relationnel
- les dialogues techniques doivent être diégétisés si possible

---

## SECTION 05

# 05 — RELATIONS MASTER RÉVIZEUS

## Relations identitaires
- incarne
- gouverne
- représente
- symbolise
- reflète

## Relations de possession
- possède
- contient
- stocke
- mémorise
- référence

## Relations d’influence
- influence
- modifie
- module
- adapte
- déclenche
- renforce
- affaiblit

## Relations de progression
- débloque
- améliore
- restaure
- transforme
- récompense
- sanctionne

## Relations pédagogiques
- enseigne
- corrige
- recommande
- évalue
- personnalise

## Relations techniques
- dépend_de
- alimente
- persiste_dans
- affiche
- orchestre
- appelle
- route_vers

## Règle
Tout changement important doit pouvoir être formulé sous forme relationnelle.
Exemple :
- `erreur_récurrente influence personnalisation_dialogues`
- `affinité_divine module familiarité_divine`
- `temple restaure monde`
- `manager orchestre feedback`

---

## SECTION 06

# 06 — UX / IMMERSION MASTER RÉVIZEUS

## Types de feedbacks pédagogiques
- bonne_réponse
- erreur_simple
- erreur_récurrente
- progression_visible
- conseil_ciblé

## Types de feedbacks ludiques
- récompense
- drop
- succès
- montée_de_niveau
- combo
- streak

## Types de feedbacks émotionnels
- soutien
- valorisation
- relance
- apaisement
- fierté_divine

## Types de feedbacks système
- chargement
- indisponibilité
- quota
- sauvegarde
- synchronisation
- erreur

## Types de feedbacks immersifs
- voix_divine
- dialogue_rpg
- effet_visuel
- ambiance_audio
- rituel_de_transition

## Principes UX
- priorité à l’immersion quand le contexte le justifie
- cohérence divine > message technique brut
- lisibilité sans casser le ton
- retours visibles, sonores et narratifs si utile
- transitions pensées comme rituels et non comme coutures techniques

---

## SECTION 07

# 07 — DOCUMENTS / VÉRITÉ MASTER RÉVIZEUS

## Types de documents
### Documents sacrés
- vision
- règles identitaires
- contraintes absolues
- architecture sacrée

### Documents cibles
- état cible
- roadmap finale
- design target
- systèmes désirés

### Documents actuels
- état réel
- blocs faits
- systèmes observés
- code présent

### Documents backlog
- tickets
- blocs futurs
- dette technique
- évolutions

### Documents runtime
- journaux
- snapshots
- analyses
- rapports agents

### Documents archive
- anciens récapitulatifs
- anciennes versions
- décisions passées
- références obsolètes mais utiles

## Niveaux de vérité
### Niveau 1 — Certain
Vu directement dans le code, les fichiers ou un document de référence fiable.

### Niveau 2 — Probable
Déduit de plusieurs indices cohérents sans confirmation explicite.

### Niveau 3 — Cible
Souhait exprimé, non encore réalisé.

### Niveau 4 — Backlog
Idée validée pour plus tard, non implémentée.

### Niveau 5 — Hypothèse
Piste à valider.

### Niveau 6 — Archive
Ancienne vérité utile pour comprendre, pas forcément actuelle.

## Statuts de stabilité
- sacré
- stable
- vivant
- temporaire
- runtime
- archive

## Règle
Les agents et les analyses doivent toujours distinguer :
- actuel
- cible
- backlog
- hypothèse
- archive

Ne jamais les fusionner.

---

## SECTION 08

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

---

## SECTION 09

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

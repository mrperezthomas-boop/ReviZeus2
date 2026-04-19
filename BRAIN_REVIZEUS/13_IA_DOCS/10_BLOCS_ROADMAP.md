---
document_id: IA_DOC_10_BLOCS_ROADMAP
type: roadmap
stability: vivant
truth_level: certain
owner: brain_core
criticality: P0
version: 1.0
last_updated: 2026-04-18
sources:
  - Brain_ReviZeus/02_BLOCS/INDEX_BLOCS.md
  - Brain_ReviZeus/02_BLOCS/REVIZEUS_ETAT_CUMULATIF_DES_BLOCS_TERMINES.txt
  - Brain_ReviZeus/02_BLOCS/A_TERMINE/
  - Brain_ReviZeus/02_BLOCS/B_A_FAIRE/
---

# 10 — BLOCS & ROADMAP DÉVELOPPEMENT

## Usage IA
Source officielle de la roadmap A → Q. À consulter avant de démarrer un nouveau bloc pour comprendre :
- Ce qui est déjà terminé (ne pas redéfaire)
- Ce qui est en cours (ne pas interférer)
- Ce qui est à faire (scope précis par bloc)

Chaque bloc a son propre fichier de spécification détaillé dans `Brain_ReviZeus/02_BLOCS/A_TERMINE/` ou `B_A_FAIRE/`.

---

## SECTION A — Index des blocs

# INDEX_BLOCS — RÉVIZEUS

## Finalité
Index officiel des blocs de développement RéviZeus. Section du haut = état cible/roadmap. Section du bas = activité récente auto (horodatée par les agents Famille A).

---

## Roadmap officielle (A → Q)

| Bloc | Intitulé | Statut | Fichier source |
|------|----------|--------|----------------|
| **A** | Stabilisation technique et socle transverse | ✅ Terminé | `A_TERMINE/REVIZEUS_BLOC_A_STABILISATION_TECHNIQUE_ET_SOCLE_TRANSVERSE.txt` |
| **B** | Dialogues RPG immersifs | 🔄 En cours | `B_A_FAIRE/BLOC_B_DIALOGUES_RPG.txt` |
| **C** | Audio global / TTS / Ducking | 📋 À faire | `B_A_FAIRE/BLOC_C_AUDIO_GLOBAL.txt` |
| **D** | Oracle Premium | 📋 À faire | `B_A_FAIRE/BLOC_D_ORACLE_PREMIUM.txt` |
| **E** | Quiz nouvelle génération | 📋 À faire | `B_A_FAIRE/BLOC_E_QUIZ_GENERATION.txt` |
| **F** | Récompenses unifiées | 📋 À faire | `B_A_FAIRE/BLOC_F_RECOMPENSES.txt` |
| **G** | Forge & inventaire | 📋 À faire | `B_A_FAIRE/BLOC_G_FORGE_INVENTAIRE.txt` |
| **H** | Temple progress | 📋 À faire | `B_A_FAIRE/BLOC_H_TEMPLE_PROGRESS.txt` |
| **I** | Savoirs vivants | 📋 À faire | `B_A_FAIRE/BLOC_I_SAVOIRS_VIVANTS.txt` |
| **J** | Extensions divines | 📋 À faire | `B_A_FAIRE/BLOC_J_EXTENSIONS_DIVINES.txt` |
| **K** | Profils IA adaptative | 📋 À faire | `B_A_FAIRE/BLOC_K_PROFILS_IA.txt` |
| **L** | Analytics pédagogiques | 📋 À faire | `B_A_FAIRE/BLOC_L_ANALYTICS.txt` |
| **M** | Recommandations IA | 📋 À faire | `B_A_FAIRE/BLOC_M_RECOMMANDATIONS.txt` |
| **N** | Dashboard vivant | 📋 À faire | `B_A_FAIRE/BLOC_N_DASHBOARD.txt` |
| **O** | Rangs & badges | 📋 À faire | `B_A_FAIRE/BLOC_O_RANGS_BADGES.txt` |
| **P** | Final ultime | 📋 À faire | `B_A_FAIRE/BLOC_P_FINAL_ULTIME.txt` |
| **Q** | Lore & carte du monde | 📋 À faire | `B_A_FAIRE/BLOC_Q_LORE_CARTE.txt` |

État cumulatif détaillé : `REVIZEUS_ETAT_CUMULATIF_DES_BLOCS_TERMINES.txt`.

---

## Activité récente auto

> Cette section est maintenue automatiquement par les agents Famille A.
> Les faits observés doivent rester courts et datés.
> Format : `[YYYY-MM-DD HH:MM] chemin_fichier -> BLOC / ZONE / owner`

- [2026-04-18 05:02] Brain_ReviZeus/00_QUICK_START/README.md -> DOC_BRAIN / QUICK_START / brain_core
- [2026-04-18 05:02] Brain_ReviZeus/13_IA_DOCS/02_BUILD_AND_TECH_STACK.md -> DOC_TECH / IA_DOCS / tech_core
- [2026-04-18 05:02] Brain_ReviZeus/13_IA_DOCS/03_DATA_AND_MANAGERS.md -> DOC_TECH / IA_DOCS / data_layer
- [2026-04-18 05:02] Brain_ReviZeus/13_IA_DOCS/04_UI_AND_NAVIGATION_MAP.md -> DOC_TECH / IA_DOCS / ui_layout
- [2026-04-18 05:02] Brain_ReviZeus/13_IA_DOCS/07_GAME_SYSTEMS.md -> DOC_TECH / IA_DOCS / core_domain
- [2026-04-18 05:02] Brain_ReviZeus/13_IA_DOCS/08_AI_PROMPTS_LIBRARY.md -> DOC_TECH / IA_DOCS / brain_core
- [2026-04-18 05:02] Brain_ReviZeus/13_IA_DOCS/09_LORE_BIBLE_REVIZEUS.md -> DOC_LORE / IA_DOCS / lore_world
- [2026-04-18 05:02] Brain_ReviZeus/02_BLOCS/INDEX_BLOCS.md -> DOC_BLOC / ROADMAP / brain_core

---

## Notes
- L'ancienne version de cet index pointait vers `BRAIN_REVIZEUS/01_IA_DOCS/` et `BRAIN_REVIZEUS/04_LORE/`. Ces chemins étaient **obsolètes** (héritage d'une numérotation antérieure). Les chemins corrects sont `13_IA_DOCS/` et `11_LORE/`.
- Tous les éléments marqués `UNKNOWN / Non cartographié` dans l'ancienne version étaient dus à ces chemins cassés, pas à un vrai problème de mapping. Le prochain run des agents produira des classements explicites.

---

## SECTION B — État cumulatif des blocs terminés (intégral)

RÉVIZEUS — ÉTAT CUMULATIF DES BLOCS TERMINÉS
Version : suivi cumulatif vivant
Rôle : document maître de continuité pour Cursor et pour tout futur chantier RéviZeus
Statut : à mettre à jour après chaque bloc réellement clôturé

====================================================================
0 — RÔLE DE CE DOCUMENT
====================================================================

Ce document sert de mémoire cumulative fiable des blocs RéviZeus réellement terminés.

Il doit permettre de :
- conserver la cohérence entre les blocs déjà clôturés et les suivants,
- empêcher qu’un futur patch ou un futur audit recasse un bloc déjà validé,
- rappeler les invariants techniques, UX et d’architecture qui ne doivent plus être remis en cause,
- transmettre à Cursor un état réel, clair et cumulatif du projet,
- distinguer ce qui est réellement terminé, ce qui reste hors périmètre, et ce qui a été volontairement repoussé.

Ce document ne remplace pas le code réel.
Il sert de guide de continuité.

Priorité de vérité absolue :
1. code réel
2. ressources réelles
3. ce document cumulatif mis à jour
4. blocs préparatoires plus anciens
5. anciens IA_DOCS non mis à jour

====================================================================
1 — RÈGLES RÉVIZEUS NON NÉGOCIABLES
====================================================================

Contraintes projet permanentes :
- Android Kotlin
- XML classique uniquement
- ViewBinding obligatoire
- Jetpack Compose interdit
- ne jamais renommer arbitrairement les variables existantes
- ne jamais casser l’architecture existante
- toujours demander ou générer des fichiers complets modifiés
- ne jamais inventer des ressources absentes
- si une ressource manque, la lister explicitement
- priorité constante au patch minimal
- préserver les flows réels déjà validés
- ne pas transformer un audit en refonte
- ne pas déclarer un bloc “terminé” sans audit réel + patchs réellement validés

Orientation / affichage :
- tout RéviZeus est en portrait fullscreen
- exception : tout le mode aventure est en paysage fullscreen

Règle de continuité :
- tout nouveau bloc doit respecter les blocs déjà clôturés
- aucun patch futur ne doit recasser les garanties validées dans un bloc précédent
- si un nouveau besoin entre en conflit avec un bloc déjà clôturé, il faut d’abord l’identifier explicitement avant toute modification

====================================================================
2 — STRUCTURE DE MISE À JOUR APRÈS CHAQUE BLOC
====================================================================

Pour chaque bloc clôturé, documenter :
- nom du bloc
- statut final
- objectif du bloc
- verdict d’audit final
- patchs réellement appliqués
- fichiers réellement modifiés
- invariants désormais acquis
- risques résiduels acceptés
- sujets hors périmètre
- consignes à respecter pour les blocs suivants

Important :
- ne documenter ici que l’état réellement validé
- ne pas écrire des intentions non codées
- ne pas écrire des ressources “supposées”
- ne pas écrire qu’un bloc est terminé si son audit final ne le confirme pas

====================================================================
3 — ÉTAT GLOBAL ACTUEL DU PROJET
====================================================================

Dernier bloc clôturé :
- BLOC A — Stabilisation technique et socle transverse

Bloc actuellement en préparation :
- BLOC B

Consigne globale pour les prochains blocs :
- repartir du code réel
- respecter le socle stabilisé par le Bloc A
- ne plus rouvrir inutilement les sujets déjà clôturés dans le Bloc A
- toute modification future doit préserver les garanties lifecycle, navigation, audio/TTS, loaders, coroutines défensives et externalisation des secrets déjà validées

====================================================================
4 — BLOC A — STABILISATION TECHNIQUE ET SOCLE TRANSVERSE
====================================================================

Statut final :
- VALIDÉ ET CLÔTURÉ

Objectif du bloc :
- assainir, sécuriser et fiabiliser la base technique de RéviZeus avant les gros chantiers suivants
- rendre le projet plus stable sur lifecycle, navigation, audio/TTS, loaders, coroutines et retours arrière
- éviter les effets domino destructeurs dans les futurs blocs

Verdict final :
- le Bloc A est clôturé proprement
- le socle transverse est stabilisé
- les écarts principaux identifiés en audit ont été traités par patchs minimaux validés
- le Bloc A peut être considéré comme terminé côté stabilité technique et socle transverse

--------------------------------------------------------------------
4.1 — AUDIT FINAL BLOC A
--------------------------------------------------------------------

Conclusion d’audit recentrée :
- le Bloc A est majoritairement validé en code, indépendamment des ressources non encore intégrées physiquement
- le socle transverse existe réellement
- les protections lifecycle/navigation/coroutines/audio/TTS sont présentes et largement appliquées
- quelques écarts de standardisation restaient ouverts avant clôture, puis ont été fermés par patchs ciblés

Lecture finale par sous-bloc :
- A1 Compile et signatures : validé
- A2 Lifecycle Activity / Fragment / Dialog : majoritairement validé puis consolidé
- A3 Navigation et retours : partiellement validé puis consolidé
- A4 Binding, UI et états d’écran : majoritairement validé
- A5 Audio, TTS et vidéo défensifs : validé
- A6 Coroutines, threads et appels IA : majoritairement validé puis consolidé
- A7 Logs propres et dette technique visible : partiellement validé puis consolidé au minimum nécessaire

--------------------------------------------------------------------
4.2 — PATCHS RÉELLEMENT APPLIQUÉS POUR CLÔTURER LE BLOC A
--------------------------------------------------------------------

PATCH 1 — Fermeture socle transverse
Objectif :
- réaligner `SettingsActivity` sur le socle `BaseActivity`

Correctif appliqué :
- `SettingsActivity` fait désormais hériter de `BaseActivity`
- conservation explicite du comportement audio propre via hooks dédiés
- aucune refonte de l’écran settings
- `BaseActivity` non modifié pour ce patch

Résultat :
- `SettingsActivity` ne contourne plus le socle transverse
- le Bloc A gagne en cohérence structurelle

PATCH 2 — Unification du retour Android
Objectif :
- supprimer les derniers usages legacy de `onBackPressed()` sur les écrans critiques identifiés

Correctif appliqué :
- migration des écrans ciblés vers `handleBackPressed()` / contrat socle
- conservation stricte des comportements existants écran par écran
- `BaseActivity` non modifié pour ce patch

Écrans concernés par le patch :
- `VideoPlayerActivity`
- `QuizResultActivity`
- `LoginActivity`

Résultat :
- retour Android plus homogène sur les écrans critiques visés
- réduction des divergences de comportement back/navigation

PATCH 3 — Observabilité minimale
Objectif :
- remplacer les `catch` silencieux critiques par des logs structurés légers

Correctif appliqué :
- instrumentation ciblée des catches critiques sur lifecycle, audio/TTS/loader, navigation et transitions IA
- aucun changement de logique métier
- aucun changement UX majeur

Résultat :
- les incidents critiques ne sont plus entièrement masqués
- meilleure observabilité pour la suite des blocs

PATCH 4 — Externalisation de la clé Gemini
Objectif :
- sortir la clé Gemini du code source

Correctif appliqué :
- suppression de la clé hardcodée dans `GeminiManager`
- passage par `BuildConfig.GEMINI_API_KEY`
- injection via propriété Gradle locale ou `local.properties`
- message explicite si la clé est absente

Résultat :
- secret Gemini externalisé
- meilleur niveau de sécurité/configuration
- API d’appel côté écrans inchangée

PATCH 5 — Contrat coroutine IA homogène
Objectif :
- homogénéiser le comportement minimal d’annulation / timeout / retry / garde UI sur les flux IA critiques

Correctif appliqué :
- propagation explicite de `CancellationException`
- gardes “UI encore vivante” avant effets UI/navigation
- annulation/neutralisation défensive de jobs sur transitions lifecycle critiques
- patch appliqué uniquement là où réellement nécessaire

Résultat :
- réduction du risque de résultats fantômes
- réduction des loaders bloqués
- réduction des callbacks tardifs après fermeture d’écran

--------------------------------------------------------------------
4.3 — FICHIERS RÉELLEMENT MODIFIÉS DANS LE BLOC A
--------------------------------------------------------------------

Patch 1 :
- `SettingsActivity.kt`

Patch 2 :
- `VideoPlayerActivity.kt`
- `QuizResultActivity.kt`
- `LoginActivity.kt`

Patch 3 :
- `BaseActivity.kt`
- `SoundManager.kt`
- `SpeakerTtsHelper.kt`
- `LoadingDivineDialog.kt`
- `OracleActivity.kt`
- `ResultActivity.kt`
- `TrainingSelectActivity.kt`
- `TrainingQuizActivity.kt`
- `SettingsActivity.kt`

Patch 4 :
- `app/build.gradle.kts`
- `GeminiManager.kt`

Patch 5 :
- `GeminiManager.kt`
- `OracleActivity.kt`
- `ResultActivity.kt`
- `TrainingSelectActivity.kt`
- `GodMatiereActivity.kt`

====================================================================
4.4 — INVARIANTS DÉSORMAIS ACQUIS APRÈS LE BLOC A
====================================================================

À ne plus casser dans les blocs suivants :

Socle transverse :
- `BaseActivity` est la référence pour les garanties communes de base
- les écrans critiques ne doivent plus contourner le socle sans justification explicite

Navigation / retour :
- les écrans critiques doivent converger vers le contrat socle de gestion du retour Android
- éviter toute réintroduction inutile de `onBackPressed()` legacy si le socle moderne existe déjà

Observabilité minimale :
- les catches silencieux sur chemins critiques ne doivent pas réapparaître
- les erreurs critiques lifecycle/audio/TTS/navigation/IA doivent rester observables par logs minimaux

IA / coroutines :
- une annulation doit rester une annulation, pas une erreur générique
- pas de résultats fantômes après fermeture d’écran
- pas de navigation tardive déclenchée après destruction/fermeture écran sur flux IA critiques
- les loaders critiques ne doivent pas rester bloqués après annulation normale d’un flux

Secret / configuration :
- la clé Gemini ne doit plus jamais être recodée en dur dans le code source
- toute future logique Gemini doit respecter l’injection via environnement local / BuildConfig

Audio / TTS :
- les protections audio/TTS déjà présentes dans le socle ne doivent pas être recassées
- toute future évolution doit respecter les comportements défensifs déjà validés

====================================================================
4.5 — RISQUES RÉSIDUELS ACCEPTÉS APRÈS CLÔTURE DU BLOC A
====================================================================

Risques encore acceptés mais non bloquants pour clôture :
- certains raffinements du logging peuvent encore être améliorés plus tard
- l’orchestration IA peut encore être perfectionnée au-delà du minimum défensif
- un nettoyage legacy plus large reste possible plus tard
- les ressources visuelles/sonores non encore déposées n’ont pas été utilisées comme critère d’invalidation du Bloc A
- des audits ressources séparés pourront être faits plus tard sans rouvrir le Bloc A

====================================================================
4.6 — SUJETS EXPLICITEMENT HORS PÉRIMÈTRE DU BLOC A
====================================================================

Le Bloc A n’avait pas pour but de :
- refondre massivement l’Oracle premium
- implémenter le mode aventure
- refaire toute l’architecture navigation
- refaire tous les dialogues RPG du projet
- auditer complètement les ressources `drawable`, `raw`, `font`
- polir toutes les UX premium
- nettoyer toute la dette legacy du projet

Ces sujets doivent être traités dans d’autres blocs si nécessaire.

====================================================================
4.7 — CONSIGNES À RESPECTER POUR LES BLOCS SUIVANTS
====================================================================

Tout futur bloc doit :
- préserver les garanties techniques fermées dans le Bloc A
- éviter de réintroduire des contournements du socle
- éviter de remettre des secrets en dur
- éviter de réintroduire des catches silencieux critiques
- respecter les protections défensives sur lifecycle / audio / TTS / loaders / IA
- vérifier qu’un nouveau patch n’annule pas les sécurisations déjà validées

Si un futur besoin semble nécessiter de revenir sur un point du Bloc A :
- le signaler explicitement
- expliquer pourquoi
- proposer un patch minimal compatible avec le socle existant

====================================================================
5 — BLOC B — SECTION À REMPLIR APRÈS CLÔTURE
====================================================================

Statut final :
- EN PRÉPARATION

Objectif du bloc :
- à compléter après cadrage réel du Bloc B

Audit final :
- à compléter

Patchs réellement appliqués :
- à compléter

Fichiers réellement modifiés :
- à compléter

Invariants acquis :
- à compléter

Risques résiduels acceptés :
- à compléter

Sujets hors périmètre :
- à compléter

Consignes pour le bloc suivant :
- à compléter

====================================================================
6 — MODE D’UTILISATION DE CE DOCUMENT AVEC CURSOR
====================================================================

Quand un nouveau bloc commence :
- transmettre à Cursor le bloc cible
- transmettre ce document cumulatif mis à jour
- rappeler que les blocs déjà clôturés sont à respecter
- demander audit d’abord, patch minimal ensuite, contrôle qualité ensuite

Quand un bloc se termine :
- ne mettre à jour ce document qu’après validation réelle du bloc
- inscrire uniquement les correctifs réellement appliqués
- noter les invariants à ne plus casser
- noter les sujets hors périmètre pour éviter les confusions futures

====================================================================
FIN DU DOCUMENT
====================================================================
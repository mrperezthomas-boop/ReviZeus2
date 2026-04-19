---
document_id: IA_DOC_06_GAME_SYSTEMS
type: game_reference
stability: stable
truth_level: certain
owner: core_domain
criticality: P1
version: 1.0
last_updated: 2026-04-18
source: Brain_ReviZeus/13_IA_DOCS/07_GAME_SYSTEMS.md
---

# 06 — GAME SYSTEMS

## Usage IA
Description détaillée des systèmes de jeu : Oracle, Quiz, Forge, Aventure, Badges, Récompenses. À consulter pour comprendre la mécanique d'un bloc avant de coder.

---

07\_GAME\_SYSTEMS.md

RÉVIZEUS — GAME SYSTEMS ARCHITECTURE

Version : 2026



But :

décrire tous les systèmes du jeu pour que toute IA ou développeur

comprenne l'architecture fonctionnelle complète.

Ce document décrit COMMENT fonctionne RéviZeus.

Il ne décrit pas les ressources ni la stack technique.



1 — VISION DU JEU

RéviZeus est un RPG éducatif mythologique.

Le joueur n’étudie pas seulement pour répondre à des quiz.

Il étudie pour :

restaurer l’Olympe

reconstruire les temples

réveiller les dieux

vaincre les forces de l’oubli

Chaque savoir retrouvé rend de la puissance au monde.



2 — BOUCLE DE JEU PRINCIPALE

Boucle fondamentale :

Envoyer un cours à l'Oracle

↓

Analyse IA

↓

Résumé pédagogique

↓

Stockage dans un Temple

↓

Quiz généré

↓

Résultat

↓

Récompenses

↓

Fragments

↓

Forge

↓

Artefacts

↓

Progression

↓

Reconstruction des Temples

↓

Déblocage du Mode Aventure



3 — SYSTÈME ORACLE

But :

transformer un cours brut en contenu pédagogique utilisable.



Entrées possibles :

photo

PDF

texte

galerie



Processus :

Scan

↓

OCR ML Kit

↓

Envoi Gemini

↓

Résumé pédagogique

↓

Choix du temple

↓

Sauvegarde dans la bibliothèque

↓

Option : lancer un quiz



L’Oracle est le cœur pédagogique du jeu.



4 — SYSTÈME QUIZ

Objectif :

évaluer la compréhension.



Types de quiz :

\- QCM classique (30 questions)

\- Quiz Oracle (questions du savoir scanné)

\- Entraînement normal (30 questions, matière unique)

\- Entraînement Ultime Matière (40 questions, matière unique)

\- Entraînement Ultime Global (40 questions alternées, toutes matières)

&#x20; \* BLOC 2A : Alternance dynamique des matières

&#x20; \* BLOC 2B : Priorisation adaptative des faiblesses

&#x20; \* Variété de savoirs (aucun savoir répété dans les 5 dernières questions)

&#x20; \* Variété de difficulté (3 niveaux minimum par tranche de 10)

&#x20; \* Personnalisation intelligente basée sur MemoryScore et UserSkillProfile

&#x20; \* Expérience formative sans punition (équilibrage bonus/pénalités)

\- quiz boss

\- quiz aventure



Résultat :

étoiles

XP

fragments

badges

progression temple



5 — SYSTÈME FRAGMENTS

Les fragments représentent la matière première du savoir.

Chaque matière possède ses fragments :



Matière	Dieu

Mathématiques	Zeus

Français	Athéna

SVT	Poséidon

Histoire	Arès

Physique	Héphaïstos

Langues	Hermès

Géographie	Déméter

Art / Philo	Apollon

Vie \& projets	Prométhée



Fragments obtenus :

quiz

quêtes

aventure

boss



⚡ Économie fragments refondue :

fragments liés à performance réelle

multi-matière en ultime

suppression logique "panthéon global"



6 — SYSTÈME FORGE

But :

transformer les fragments en artefacts.



Boucle :

Fragments

↓

Forge

↓

Objet

↓

Inventaire

↓

Bonus gameplay



Exemples :

Bouclier de Logique

annule une erreur

Sandales d'Hermès

bonus de temps

Plume d'Athéna

indice supplémentaire



7 — SYSTÈME INVENTAIRE

L’inventaire est un sac RPG.

Contient :

artefacts

reliques

objets

fragments

équipements



Fonctions :

tri

filtre

description

rareté

date d’obtention

passifs



8 — SYSTÈME TEMPLES

Cœur du lore.

Au début :

les temples sont détruits.

Le joueur doit les restaurer.



Chaque temple possède :

niveau 0 → 10

0 ruine

1 étincelle

2 réveil

3 fondation

4 reconstruction

5 temple actif

6 rayonnement

7 influence

8 sanctuaire

9 glorieux

10 restauré



Actions qui restaurent :

quiz réussi

savoir enregistré

quête

boss

offrande



9 — SYSTÈME DIEUX

Chaque dieu incarne :

une matière

une personnalité

un style pédagogique



Exemples :

Zeus

rigueur

Athéna

pédagogie

Apollon

inspiration

Prométhée

curiosité



Les dialogues évoluent selon :

temple

progression

réussite

humeur

triggers contextuels (GodTriggerEngine - réactions dynamiques en temps réel)



10 — SYSTÈME BADGES

102 badges.

Catégories :

streak

quiz

oracle

forge

divin

spécial



But :

récompenser les comportements pédagogiques.



11 — SYSTÈME DASHBOARD

Le Dashboard est le hub principal du jeu.

Il contient :

temples

oracle

entraînement

forge

inventaire

aventure

badges



Il évolue avec :

météo divine

statues

progression

dashboard insights (via DashboardInsightsWidget et l'intelligence IA)



12 — MODE AVENTURE

Le mode aventure est la campagne RPG du jeu.



Structure :

Zone

↓

Quête

↓

Défi éducatif

↓

Boss

↓

Récompense



Zones principales :

Mont Olympe

temples

Tartare

Bibliothèque interdite



13 — SYSTÈME BOSS

Boss éducatifs :

Cyclope du raisonnement

Sphinx des mots

Kraken cellulaire

Titan des possibles



Fonctionnement :

quiz spécial avec règles uniques.



14 — PROGRESSION GLOBALE

Deux progressions :

progression joueur

XP / niveau

progression monde

temples / aventure



15 — SYSTÈMES FUTURS

Prévu :

compagnon

atlas des constellations

talents cognitifs

classes



16 — PILIER FONDAMENTAL

RéviZeus repose sur :

APPRENDRE

↓

COMPRENDRE

↓

RÉUSSIR

↓

RESTAURER

↓

SAUVER L’OLYMPE



SYSTÈME : MISE À JOUR DIVINE



OBJECTIF :

Permettre l’évolution du jeu sans reset joueur.



MÉCANIQUE :

\- Détection version (versionCode)

\- Application d’un patch local

\- Nettoyage sélectif

\- Validation d’intégrité



GARANTIE :

Le joueur ne perd jamais :

\- sa progression

\- ses ressources

\- ses savoirs



LIMITATION :

Toute nouvelle donnée importante doit :

\- être initialisée pour anciens joueurs

\- être intégrée dans le système de validation



EXEMPLE :

Ajout d’une nouvelle stat :

→ définir valeur par défaut

→ éventuellement recalculer depuis données existantes



17 — EXTENSION OFFICIELLE — TIMERS PAR ÂGE \& FRAGMENTS PAR MATIÈRE

Cette section complète les systèmes existants sans les remplacer.



🔥 Ajout mécanique majeure : TENSION SYSTEM

\- chrono par question

\- timeout = erreur automatique

\- gameplay plus nerveux



Règle de temps par question

Les quiz chronométrés utilisent désormais un temps individuel par question, calculé selon l’âge du joueur :

1 à 10 ans → 20 secondes

11 à 20 ans → 15 secondes

21 ans et plus → 10 secondes



Objectif :

adapter la pression temporelle à la réalité cognitive du joueur.



Règle de récompense — quiz Oracle

Le quiz généré après l’enregistrement d’un savoir accorde :

+1 fragment par bonne réponse

fragment obligatoirement lié à la matière concernée



Règle de récompense — entraînement normal

L’entraînement normal accorde :

+1 fragment par bonne réponse

fragment obligatoirement lié à la matière de la question



Règle de récompense — entraînement ultime

L’entraînement ultime global multi-savoirs accorde :

+3 fragments par bonne réponse

gain calculé par question selon la matière réelle

suppression du système de fragments panthéon génériques pour ce mode



Conséquence game design

Le joueur ne gagne plus une abstraction détachée du savoir.

Chaque bonne réponse renforce directement le dieu / temple correspondant.

Cela renforce :

la lisibilité de la progression

la cohérence entre quiz et temples

la sensation de restaurer réellement l’Olympe matière par matière



18 — SYSTÈME INTELLIGENCE IA (BLOC 3A)

UserAnalytics ne sont plus seulement stockées mais exploitées :

\- Détection automatique de patterns d'apprentissage

\- Génération de signaux actionnables (UserInsight)

\- Verdicts personnalisés basés sur l'historique

\- Adaptation future des quiz selon insights



Signaux détectés :

\- Faiblesses thématiques

\- Problèmes de vitesse

\- Précipitation

\- Confusion entre notions

\- Évolution temporelle

\- Instabilité cognitive

\- Besoin de pause



Utilisation actuelle :

\- QuizResultActivity : verdicts enrichis



Évolutions futures :

\- Ajustement difficulté en temps réel

\- Ciblage intelligent des questions

\- Recommandations personnalisées


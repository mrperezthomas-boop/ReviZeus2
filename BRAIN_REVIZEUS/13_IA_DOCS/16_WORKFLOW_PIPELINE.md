---
document_id: IA_DOC_16_WORKFLOW_PIPELINE
type: process
stability: stable
truth_level: certain
owner: brain_core
criticality: P1
version: 1.1
last_updated: 2026-04-19
source: Brain_ReviZeus/01_REGLES_SACREES/WORKFLOW_PIPELINE.md (patché)
---

# 16 — PIPELINE CLAUDE → CURSOR

## Usage IA
Pipeline opérationnel standard : IDÉE → AUDIT → PROMPT → CURSOR → VÉRIF → COMMIT → MÉMOIRE. À respecter pour toute intervention sur le code. Précise aussi comment produire un prompt Cursor correct (structure, contraintes, périmètre, tests de vérification).

---

# WORKFLOW.md — PIPELINE CLAUDE → CURSOR POUR RÉVIZEUS

> **NOTE DE CONSOLIDATION (2026-04-19)** : les anciennes références à `00_CONTEXT`, `03_CODE_MAP`, `06_BLOC_STATUS` (qui pointaient vers une structure `IA_DOCS/` jamais matérialisée telle quelle) ont été **mises à jour** pour pointer vers la structure réelle du Brain consolidé. Le reste du workflow est préservé intégralement.

---

# Dernière mise à jour : 2026-04-17

## PRINCIPE
Claude = cerveau stratégique, mémoire projet, factory à prompts.
Cursor (ou IA locale) = exécuteur de code en direct sur le repo.
Android Studio = compilation, test, validation visuelle.
Git = sauvegarde après validation.

## PIPELINE STANDARD : IDÉE → CODE LIVRÉ

### Étape 1 — Tu décris ton besoin à Claude
Exemples :
- "Je veux convertir les Toast de SettingsActivity en dialogues RPG"
- "Il faut que la Lyre d'Apollon montre un loader pendant la génération"
- "Ajouter un timer par question dans QuizActivity"

### Étape 2 — Claude audite et prépare
Claude consulte l'arbre de connaissance du Brain consolidé : `Brain_ReviZeus/01_REGLES_SACREES/` (règles + contexte), `Brain_ReviZeus/03_SYSTEMES/CARTOGRAPHIE_MASTER.md` (ex 03_CODE_MAP), `Brain_ReviZeus/02_BLOCS/INDEX_BLOCS.md` (ex 06_BLOC_STATUS). Il produit :
1. **Audit** : fichiers concernés, risques, dépendances
2. **Plan** : étapes minimales dans le bon ordre
3. **Prompt Cursor** : texte exact à copier-coller dans Cursor

### Étape 3 — Tu colles le prompt dans Cursor
Cursor modifie les fichiers directement dans E:\ReviZeus.

### Étape 4 — Tu vérifies dans Android Studio
- Build → Rebuild Project
- Logcat si erreurs
- Test sur émulateur/téléphone

### Étape 5 — Si ça marche → commit
```
git add .
git commit -m "feat: [description courte]"
git push
```

### Étape 6 — Si ça ne marche pas → retour Claude
Tu me donnes l'erreur exacte, je te donne le prompt de correction pour Cursor.

### Étape 7 — Mise à jour mémoire
Claude met à jour `Brain_ReviZeus/02_BLOCS/INDEX_BLOCS.md` (ex 06_BLOC_STATUS) si un bloc avance significativement.

---

## COMMENT CLAUDE PRODUIT LES PROMPTS CURSOR

### Structure d'un prompt Cursor produit par Claude
```
Mission RéviZeus : [NOM COURT]

Contexte : [CE QU'ON FAIT ET POURQUOI]

Fichier(s) à modifier :
- [chemin/fichier1.kt]
- [chemin/fichier2.xml]

Fichier(s) à NE PAS toucher :
- [chemin/fichier_protégé.kt]

Contraintes absolues :
- Kotlin + XML + ViewBinding uniquement
- Pas de Compose
- Ne renomme aucune variable existante
- Génère les fichiers COMPLETS modifiés
- N'invente aucune ressource absente
- Conserve 100% du code existant non concerné

Ce qu'il faut faire exactement :
1. [instruction précise 1]
2. [instruction précise 2]
3. [instruction précise 3]

Résultat attendu :
- [comportement visible 1]
- [comportement visible 2]

Vérification Android Studio :
- Build → Rebuild Project
- [test spécifique à faire]
```

### Règles de qualité des prompts Claude→Cursor
- UN prompt = UNE tâche = UN périmètre
- Jamais plus de 3-4 fichiers par prompt
- Toujours lister les fichiers autorisés ET interdits
- Toujours exiger les fichiers complets
- Toujours donner le test de vérification
- Si la tâche est grosse → la découper en sous-prompts

---

## COMMENT UTILISER UNE IA LOCALE À LA PLACE DE CURSOR

### Si tu passes à Phi-3 Mini / Ollama / LM Studio + Continue/Cline/Roo Code :

Le workflow reste identique :
1. Claude produit le prompt
2. Tu le donnes à l'IA locale dans VS Code / éditeur
3. Elle modifie les fichiers
4. Tu vérifies dans Android Studio
5. Tu commit si OK

La seule différence : l'IA locale sera moins forte sur les gros patches multi-fichiers.
Donc Claude devra découper encore plus finement les prompts.

### Stratégie optimale de découpage pour IA locale
- 1 fichier par prompt maximum
- Instructions très explicites (pas d'implicite)
- Toujours fournir le contexte des imports et des classes utilisées
- Toujours donner un extrait du code existant comme ancrage

---

## QUAND UTILISER QUOI

| Tâche | Outil |
|-------|-------|
| Réfléchir, planifier, auditer | Claude |
| Produire un prompt de code | Claude |
| Modifier 1-3 fichiers Kotlin/XML | Cursor ou IA locale |
| Bug de build / crash | Claude (diagnostic) → Cursor (fix) |
| Gros refactor multi-fichiers | Claude (découpage) → Cursor (exécution par morceaux) |
| Décision architecture | Claude |
| Game design aventure | Claude |
| Test / validation visuelle | Android Studio |
| Sauvegarde | Git |

---

## ANTI-PATTERNS À ÉVITER

❌ Donner un prompt vague à Cursor ("améliore les dialogues")
❌ Laisser Cursor toucher plus de 4 fichiers à la fois
❌ Ne pas vérifier le build avant de commit
❌ Oublier de mettre à jour `Brain_ReviZeus/02_BLOCS/INDEX_BLOCS.md`
❌ Utiliser Claude pour du copier-coller de code au lieu de passer par Cursor
❌ Créer des docs/skills/workflows au lieu de coder

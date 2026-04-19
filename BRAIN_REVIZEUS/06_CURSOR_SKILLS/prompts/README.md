# BANQUE DE PROMPTS CURSOR — RÉVIZEUS

**Créé le :** 2026-04-19
**Owner logique :** `brain_core`
**Rôle :** Centraliser les prompts Cursor testés, validés ou archivés pour ne plus les regénérer à chaque fois.

---

## Pourquoi cette banque

Dans la doctrine RéviZeus (voir `01_REGLES_SACREES/DOCTRINE_EQUIPE_AGENTS.md`), deux agents de la **Famille B** produisent des prompts Cursor :
- **Prompt Compressor** — compresse le contexte utile
- **Prompt Builder Cursor** — construit le prompt final compact

Tant que ces agents sont incarnés par Claude (v1), leurs sorties sont des artefacts à versionner. Cette banque sert à **les capturer** quand ils marchent bien, et à **les réutiliser** quand le même type d'intervention revient.

---

## Convention de nommage

Format obligatoire :

```
[BLOC]_[ZONE]_[ACTION]_vNN.md
```

**Exemples valides :**
- `BLOC_B_RPG_DIALOG_migration_toast_v01.md`
- `E_QUIZ_TIMER_ajout_par_age_v02.md`
- `H_AVENTURE_WORLD_MAP_integration_backgrounds_v01.md`
- `F_ECONOMIE_XP_recalibrage_v03.md`

**Règles :**
- `BLOC` = identifiant du bloc tel que dans `02_BLOCS/` (BOOT, AUTH, BLOC_A, BLOC_B, C_AUDIO, D_ORACLE, E_QUIZ, F_ECONOMIE, G_FORGE, H_AVENTURE, I_SAVOIRS, J_CREATIVE, K_ADAPTATIVE, L_ANALYTICS, M_RECO, N_DASHBOARD, O_META, Q_LORE, DATA_ROOM, CORE_PANTHEON)
- `ZONE` = zone technique (RPG_DIALOG, TIMER, WORLD_MAP, XP, ROOM_ENTITY, etc.) telle que définie dans `revizeus_mapper_rules.json`
- `ACTION` = verbe + sujet court (migration_toast, ajout_par_age, integration_backgrounds)
- `vNN` = version à deux chiffres (v01, v02, v12...)

---

## Structure des sous-dossiers

### `validated/` — Prompts testés avec succès
Prompts qui ont produit un code accepté et mergé. À réutiliser comme point de départ pour des interventions similaires.

### `drafts/` — Prompts en cours d'élaboration
Brouillons avant test sur Cursor. Peuvent être imparfaits. Une fois testés et validés, **ils déménagent dans `validated/`**.

### `archived/` — Prompts obsolètes mais conservés pour référence
Prompts qui ont été utilisés puis rendus caducs par une évolution d'architecture, un changement de règle sacrée, ou une refonte du bloc concerné. **Jamais supprimés**, juste déplacés ici.

---

## Structure interne d'un fichier prompt

Tout prompt Cursor stocké ici doit suivre ce template (aligné sur `WORKFLOW_PIPELINE.md`) :

```markdown
---
prompt_id: [BLOC]_[ZONE]_[ACTION]_vNN
created: YYYY-MM-DD
status: draft | validated | archived
bloc: [identifiant bloc]
zone: [zone technique]
owner: [owner logique]
criticality: P0 | P1 | P2 | P3
tested_on: YYYY-MM-DD (si validé)
success_rate: [observation libre, ex "1/1 OK", "3/4 itérations"]
replaces: [si ce prompt remplace une version antérieure]
---

# Titre lisible du prompt

## Contexte
[Pourquoi on fait ça, quel est le problème à résoudre, quel est l'état avant]

## Prompt Cursor (à copier tel quel)

```
Mission RéviZeus : [NOM COURT]

Contexte : [...]

Fichier(s) à modifier :
- [chemin1.kt]
- [chemin2.xml]

Fichier(s) à NE PAS toucher :
- [...]

Contraintes absolues :
- [...]

Ce qu'il faut faire exactement :
1. [...]
2. [...]

Résultat attendu :
- [...]

Vérification Android Studio :
- Build → Rebuild Project
- [tests spécifiques]
```

## Résultat attendu
[Comportement visible une fois Cursor a appliqué le prompt]

## Notes post-exécution
[Si déjà testé : ce qui a marché, ce qui a demandé correction, pièges à éviter la prochaine fois]
```

---

## Règles d'usage pour les agents

### Ce qu'un agent DOIT faire
- Avant de générer un nouveau prompt, **chercher dans `validated/`** si un prompt similaire existe déjà
- Réutiliser en adaptant plutôt que regénérer de zéro
- Incrémenter le numéro de version quand on modifie un prompt (pas d'écrasement silencieux)
- Horodater toute modification importante (voir format dans `DOCTRINE_EQUIPE_AGENTS.md`)

### Ce qu'un agent NE DOIT PAS faire
- Créer des prompts qui touchent > 4 fichiers (limite stricte du WORKFLOW)
- Déplacer un prompt de `drafts/` à `validated/` sans test Cursor effectif
- Supprimer un prompt de `archived/` (append-only uniquement)
- Mélanger plusieurs blocs dans un seul prompt
- Générer un prompt sans consulter d'abord `11_REGLES_SACREES/` et `AGENTS_RULES.md`

---

## Statistiques (à tenir à jour manuellement ou par agent Scribe)

| Sous-dossier | Nombre de prompts | Dernière mise à jour |
|--------------|-------------------|----------------------|
| `validated/` | 0 | 2026-04-19 (init) |
| `drafts/` | 0 | 2026-04-19 (init) |
| `archived/` | 0 | 2026-04-19 (init) |

---

## Prochaine étape recommandée

Quand tu produis ton prochain prompt Cursor pour un bloc, dépose-le directement dans `drafts/`. Après succès sur Cursor + intégration dans le repo, renomme-le et déplace-le dans `validated/`. Au bout de 5 à 10 prompts validés, tu auras une banque réutilisable qui accélèrera chaque cycle d'intervention.

# README — ARBRE DE CONNAISSANCE RÉVIZEUS
# Dernière mise à jour : 2026-04-17

## QU'EST-CE QUE C'EST

Ce pack contient tout ce qu'il faut pour que Claude (ou toute IA stratégique)
comprenne instantanément RéviZeus et produise des prompts parfaits pour Cursor.

## OÙ PLACER CHAQUE FICHIER

```
E:\ReviZeus\                          ← racine du repo
├── AGENTS.md                          ← RACINE (lu par Cursor + Claude)
├── WORKFLOW.md                        ← RACINE (référence pipeline)
├── .cursor/
│   └── rules/
│       └── revizeus.mdc              ← Auto-appliqué par Cursor
├── IA_DOCS/
│   ├── 00_REVIZEUS_CONTEXT.md        ← État technique réel
│   ├── 03_CODE_MAP.md                ← Arbre des connexions code
│   └── 06_BLOC_STATUS.md            ← État des blocs
└── IA_CURSOR_SKILLS/
    └── CURSOR_SKILLS.md              ← Templates de prompts Cursor
```

## RÔLE DE CHAQUE FICHIER

| Fichier | Pour qui | Rôle |
|---------|----------|------|
| AGENTS.md | Claude + Cursor | Règles non négociables du projet |
| 00_REVIZEUS_CONTEXT.md | Claude | État technique complet, stack, managers, systèmes |
| 03_CODE_MAP.md | Claude | Qui appelle qui, connexions entre fichiers |
| 06_BLOC_STATUS.md | Claude + Toto | Où en est chaque bloc |
| WORKFLOW.md | Toto | Comment passer d'une idée au code livré |
| revizeus.mdc | Cursor | Règles auto-appliquées à chaque fichier |
| CURSOR_SKILLS.md | Claude (pour générer) + Toto (pour coller) | Templates de prompts |

## COMMENT UTILISER

1. Tu places les fichiers dans ton repo
2. Tu commit + push
3. Quand tu veux coder quelque chose :
   - Tu décris ton besoin à Claude
   - Claude lit l'arbre de connaissance
   - Claude te donne un prompt Cursor chirurgical
   - Tu le colles dans Cursor
   - Tu vérifies dans Android Studio
   - Tu commit si OK

## RÈGLE DE MISE À JOUR
- 06_BLOC_STATUS.md : mis à jour après chaque bloc terminé
- 03_CODE_MAP.md : mis à jour quand de nouveaux managers/écrans sont créés
- 00_REVIZEUS_CONTEXT.md : mis à jour si la stack ou l'architecture change
- AGENTS.md : rarement modifié (règles stables)

# CONTINUE CONFIG NOTE

> Horodatage : 2026-04-19 17:50 local

## Fichier détecté
- `.continue/config.json`

## Intérêt Brain
Le zip confirme qu'un environnement **Continue** est utilisé en parallèle de Cursor / ChatGPT / GitHub dans le workflow RéviZeus.

## Ce qu'on peut ajouter au Brain
### Règle documentaire
> Continue fait partie des outils de travail actifs autour du repo RéviZeus. Sa configuration doit être traitée comme outil d'assistance et de structuration de prompts, jamais comme vérité métier du projet.

## Extrait brut utile
```json
{
  "contextProviders": [
    {
      "name": "file",
      "params": {
        "nFiles": 5
      }
    }
  ],
  "systemMessage": "Lis toujours REVIZEUS_CONTEXT.md avant de répondre à toute question sur ce projet."
}
```

## Annotation
Conserver cette note dans `06_CURSOR_SKILLS` ou zone workflow/agents. Ne pas promouvoir tout le JSON comme doctrine stable sans tri manuel.

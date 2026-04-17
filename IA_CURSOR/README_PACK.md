# PACK SKILLS CURSOR — RÉVIZEUS

Ce pack est volontairement resserré : uniquement ce qui est utile pour coder proprement avec Cursor sur le vrai projet RéviZeus.

## Priorité de vérité
1. Code réel du projet
2. Ressources visibles réellement présentes
3. Blocs préparatoires
4. IA_DOCS anciens

## État projet retenu pour ce pack
- Projet Android Kotlin + XML + ViewBinding
- Pas de Jetpack Compose
- Min SDK 24 / Target-Compile 36 / JVM 17
- Room + KSP
- Firebase Auth + Firestore
- Gemini 0.9.0
- CameraX 1.5.3 / ML Kit 19.0.1 / Media3 / Lottie
- Tout le projet est en portrait fullscreen, sauf le mode aventure en paysage fullscreen
- Le mode aventure existe déjà au stade de socle/début uniquement
- Les blocs métiers hors Bloc A restent majoritairement à implémenter

## Fichiers du pack
- `AGENTS.md` : règles projet permanentes à placer à la racine du repo
- `PROJECT_TRUTH_MAP.md` : cartographie projet réaliste et compacte
- `SKILL_01_CURSOR_STARTER.md` : ordre universel de démarrage d’une mission
- `SKILL_02_TASK_TEMPLATE.md` : modèle universel pour toute implémentation
- `SKILL_03_BUGFIX_BUILD_CRASH.md` : modèle universel de correction
- `SKILL_04_DERAIL_RECOVERY.md` : ordre pour recadrer Cursor s’il dérive
- `SKILL_05_QA_FINAL_GATE.md` : contrôle final avant validation
- `CHATGPT_BRIDGE_UNIVERSAL.txt` : texte à coller dans une nouvelle discussion avec ChatGPT

## Conseils d’usage
- Mets `AGENTS.md` et `PROJECT_TRUTH_MAP.md` à la racine du repo.
- Les autres peuvent être gardés dans un dossier `IA_CURSOR_SKILLS/`.
- Commence toujours par un audit avant toute modification.
- Demande toujours des fichiers complets, jamais des snippets si tu modifies le projet.

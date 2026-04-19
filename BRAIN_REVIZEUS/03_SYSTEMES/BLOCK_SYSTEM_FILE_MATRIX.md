<!--
[REVIZEUS_TRACKING] 2026-04-20 00:10 Europe/Paris
Objet: Matrice blocs / systèmes / fichiers
Contexte: Pont central entre delivery bloc par bloc et réalité du code.
Auteur logique: ChatGPT
Statut: MERGED_V2
-->

# BLOCK_SYSTEM_FILE_MATRIX

| Bloc | Système dominant | Fichiers pivots | Docs à relire | Risque |
|---|---|---|---|---|
| Bloc B Dialogues RPG | Dialogues / Audio / Immersion | GodSpeechAnimator, DialogRPGManager, DialogRPGFragment | PROJECT_TRUTH_MAP.md | Fort |
| Bloc C Audio Global | Dialogues / Audio / Immersion | SoundManager, raw assets, loaders liés | PROJECT_TRUTH_MAP.md | Moyen |
| Bloc D Oracle Premium | Oracle / Quiz / Savoirs | OracleActivity, OraclePromptActivity, ResultActivity, GeminiManager | PROJECT_TRUTH_MAP.md | Moyen |
| Bloc F Récompenses | Inventaire / Forge / Rewards | CurrencyManager, QuizRewardManager, InventoryActivity | PROJECT_TRUTH_MAP.md | Fort |
| Bloc H Temple Progress | Aventure / World / Temple | WorldMapActivity, MapTempleActivity, TempleAdventureProgressManager | PROJECT_TRUTH_MAP.md, RISK_ZONES_MAP.md | Très fort |
| Bloc I Savoirs Vivants | Oracle / Quiz / Savoirs + IA adaptative joueur | SavoirActivity, KnowledgeFragmentManager, PlayerAdaptiveSnapshot | PROJECT_TRUTH_MAP.md | Très fort |
| Bloc K Profils IA | IA adaptative joueur | AdaptiveDialogueEngine, PlayerAdaptiveSnapshot | PROJECT_TRUTH_MAP.md, RISK_ZONES_MAP.md | Très fort |
| Bloc Q Lore Carte | Aventure / World / Temple + Dieux / relations | WorldMapThemeResolver, GodLoreManager, GodPersonalityEngine | PROJECT_TRUTH_MAP.md | Fort |

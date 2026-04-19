# CARTOGRAPHIE MASTER — RÉVIZEUS

Dernière mise à jour : 2026-04-18
Statut : Référence opérationnelle pour les agents, Cursor et la QA.

## But
Cette cartographie sert à relier rapidement **un fichier réel** à :
- son bloc produit,
- sa zone technique,
- son système métier,
- son niveau de criticité,
- son owner logique.

Elle existe pour éviter 3 dérives :
- modifications sur le mauvais périmètre,
- prompts Cursor trop larges,
- mise à jour BRAIN imprécise ou vide.

## Hiérarchie de vérité
1. Code réel du repo
2. Ressources réelles du repo
3. `AGENTS.md`
4. `Cursor/IA_DOCS/00_REVIZEUS_CONTEXT.md`
5. `Cursor/IA_DOCS/03_CODE_MAP.md`
6. Cette cartographie runtime

## Blocs produits retenus
- `BOOT` : lancement, title, menu, gate update
- `AUTH` : compte, héros, onboarding identitaire
- `CORE_LOOP` : mood, dashboard, settings, hub central
- `BLOC_A` : socle transverse et stabilité technique
- `BLOC_B` : dialogues RPG immersifs
- `BLOC_B2` : personas divines et orchestration
- `C_AUDIO` : audio, TTS, loading diégétique
- `D_ORACLE` : capture, prompt, génération résumé
- `E_QUIZ` : moteur quiz, entraînement, ultime, timer
- `F_ECONOMIE` : XP, monnaies, fragments, rewards
- `G_FORGE` : forge, inventaire, crafting
- `H_AVENTURE` : carte monde, temple, nœuds, progression
- `I_SAVOIRS` : bibliothèque, temple matière, savoirs stockés
- `J_CREATIVE` : musique, création, extensions divines
- `K_ADAPTATIVE` : IA adaptative par joueur/héros
- `L_ANALYTICS` : analytics, faiblesse, instrumentation
- `M_RECO` : recommandations et synthèses parent
- `N_DASHBOARD` : widgets vivants du hub
- `O_META` : badges, overlays, méta-progression
- `Q_LORE` : tutoriels, lore, monde
- `DATA_ROOM` : couche Room et persistance
- `CORE_PANTHEON` : dieux/matières/couleurs/identité

## Niveaux de criticité
- `P0` : cœur vital, toucher avec grande prudence
- `P1` : système majeur du produit
- `P2` : système secondaire important
- `P3` : composant périphérique ou fallback
- `P9` : inconnu / à cartographier

## Règles d’usage
- Toute nouvelle classe métier importante doit être ajoutée au mapping explicite.
- Toute Activity critique doit être cartographiée explicitement, jamais laissée au fallback.
- Toute classe inconnue détectée par les agents doit déclencher une action de maintenance de cartographie.
- Le mapper ne remplace pas l’audit humain : il prépare, il ne décide pas seul.

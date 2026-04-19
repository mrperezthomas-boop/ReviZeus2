═══════════════════════════════════════════════════════════════════════════════
RÉVIZEUS — 300 BADGES MASTER ULTIME
═══════════════════════════════════════════════════════════════════════════════

Version : GOD TIER ULTIMATE 2026-03-24
Document de référence : Catalogue complet des badges sans doublons
Statut : 198 NOUVEAUX BADGES + 102 DÉJÀ CODÉS = 300 TOTAL

═══════════════════════════════════════════════════════════════════════════════
0. PRÉAMBULE — STRUCTURE ET ORGANISATION
═══════════════════════════════════════════════════════════════════════════════

## BADGES DÉJÀ CODÉS (102) — NE PAS RECODER

Ces badges sont déjà présents dans BadgeDefinition.kt et BadgeManager.kt :

STREAK (9 badges) :
✓ streak_first, streak_3, streak_7, streak_14, streak_30, streak_60, streak_100, 
  streak_200, streak_365

XP/NIVEAU (11 badges) :
✓ level_5, level_10, level_20, level_30, level_50, level_75, level_100, level_150,
  xp_first_10k, xp_100k, xp_1m

ORACLE (11 badges) :
✓ oracle_first, oracle_10, oracle_30, oracle_50, oracle_100, oracle_200, 
  oracle_all_subjects, oracle_double_same, oracle_quality_A, oracle_night, 
  oracle_morning

QUIZ/COMBAT (19 badges) :
✓ quiz_first, quiz_10, quiz_30, quiz_100, quiz_300, perfect_first, perfect_3,
  perfect_10, perfect_30, perfect_100, ultime_first, ultime_10, ultime_perfect,
  stars_max, stars_50, ares_defi_win, ares_defi_10, defi_3_in_row, all_subject_quiz

PANTHÉON (9 badges) :
✓ zeus_quiz_50, athena_quiz_50, poseidon_quiz_30, ares_quiz_30, hephaistos_quiz_30,
  hermes_quiz_30, demeter_quiz_20, apollon_quiz_20, promethee_quiz_20

FORGE (13 badges) :
✓ forge_first, forge_5, forge_10, forge_full_inv, fragment_100, fragment_1000,
  forge_impatient, artefact_pythagore, artefact_athena, artefact_ares, forge_night,
  forge_vibration, forge_master

DIVIN/INTERACTIONS (12 badges) :
✓ divin_talk_10, divin_mnemo, demeter_water, apollon_lyre, promethee_help,
  zeus_strict, divin_wait, divin_all_gods, divin_mnemo_20, demeter_return,
  hephaistos_verdict, athena_pedagogue

SPÉCIAL/WTF (18 badges) :
✓ first_connection, tutorial_done, night_owl, early_bird, settings_explorer,
  profile_visited, speed_quiz, comeback, quiz_at_3am, rage_fail, marathon_quiz,
  wtf_speed_fail, wtf_lucky, wtf_mute, wtf_volume_max, wtf_inv_spam, wtf_screenshot,
  wtf_no_stop, wtf_slow_win, wtf_perfect_333, wtf_shake, wtf_tap_spam, wtf_oracle_spam,
  all_badges

═══════════════════════════════════════════════════════════════════════════════

## NOUVEAUX BADGES (198) — À IMPLÉMENTER

Organisés en 12 catégories selon les WAVES du roadmap :

1. TEMPLES ÉVOLUTIFS (30 badges)
2. ORACLE 3 PORTES & VALIDATION (25 badges)
3. INVENTAIRE & ÉCONOMIE (20 badges)
4. JARDIN DE DÉMÉTER (15 badges)
5. QUÊTES & AVENTURE (30 badges)
6. BOSS ÉDUCATIFS (20 badges)
7. IA ADAPTATIVE & PROFIL (18 badges)
8. EXTENSIONS DIVINES (20 badges)
9. MONDE & LORE (15 badges)
10. WTF ABSOLU & SECRETS (30 badges)
11. PERFORMANCE & EXPLOIT (15 badges)
12. SOCIAL & FUTUR (10 badges)

═══════════════════════════════════════════════════════════════════════════════

## FORMAT DE CHAQUE BADGE

ID : identifiant_unique
NOM : Nom Épique du Badge
RARETÉ : Commun | Rare | Épique | Légendaire | Mythique | WTF
DESCRIPTION : Texte immersif descriptif
DÉBLOCAGE : Condition précise de déblocage
CATÉGORIE : Temples | Oracle | Inventaire | Quêtes | etc.
XP BONUS : 0-10000
PROMPT NANO BANANA 2 : "Description image clay render 3D Epic Chibi Olympe"

═══════════════════════════════════════════════════════════════════════════════
CATÉGORIE 1 — TEMPLES ÉVOLUTIFS (30 BADGES)
═══════════════════════════════════════════════════════════════════════════════

ID : temple_first_level_any
NOM : Première Pierre
RARETÉ : Commun
DESCRIPTION : Tu as relevé ta première pierre d'un temple en ruine.
DÉBLOCAGE : Monter n'importe quel temple au niveau 1
CATÉGORIE : Temples
XP BONUS : 100
PROMPT : "Small chibi hero placing a glowing stone on a temple foundation, ruins in background, hopeful atmosphere, clay render, golden light"

────────────────────────────────────────────────────────────────────────────────

ID : temple_zeus_level_5
NOM : Foudre Renaissante
RARETÉ : Rare
DESCRIPTION : Le temple de Zeus reprend vie grâce à tes efforts mathématiques.
DÉBLOCAGE : Temple de Zeus niveau 5
CATÉGORIE : Temples
XP BONUS : 300
PROMPT : "Zeus temple partially restored with lightning crackling, chibi hero proud, mathematical symbols floating, epic clay render, blue and gold"

────────────────────────────────────────────────────────────────────────────────

ID : temple_athena_level_5
NOM : Renaissance de la Sagesse
RARETÉ : Rare
DESCRIPTION : La bibliothèque d'Athéna brille à nouveau.
DÉBLOCAGE : Temple d'Athéna niveau 5
CATÉGORIE : Temples
XP BONUS : 300
PROMPT : "Athena temple with glowing books and owl, chibi character reading, warm library light, clay render, purple and gold"

────────────────────────────────────────────────────────────────────────────────

ID : temple_poseidon_level_5
NOM : Marée Montante
RARETÉ : Rare
DESCRIPTION : Les eaux du savoir coulent à nouveau dans le temple de Poséidon.
DÉBLOCAGE : Temple de Poséidon niveau 5
CATÉGORIE : Temples
XP BONUS : 300
PROMPT : "Poseidon temple with water flowing around pillars, marine life, chibi hero with trident, underwater glow, clay render, turquoise and gold"

────────────────────────────────────────────────────────────────────────────────

ID : temple_ares_level_5
NOM : Forge de l'Histoire
RARETÉ : Rare
DESCRIPTION : Les batailles du passé résonnent à nouveau dans le temple d'Arès.
DÉBLOCAGE : Temple d'Arès niveau 5
CATÉGORIE : Temples
XP BONUS : 300
PROMPT : "Ares temple with shields and swords displayed, chibi warrior, red battle banners, clay render, crimson and bronze"

────────────────────────────────────────────────────────────────────────────────

ID : temple_hephaistos_level_5
NOM : Enclume Éternelle
RARETÉ : Rare
DESCRIPTION : Les flammes de la connaissance brûlent à nouveau dans la forge.
DÉBLOCAGE : Temple d'Héphaïstos niveau 5
CATÉGORIE : Temples
XP BONUS : 300
PROMPT : "Hephaistos temple with forge burning bright, anvil glowing, chibi blacksmith, lava flows, clay render, orange and black"

────────────────────────────────────────────────────────────────────────────────

ID : temple_hermes_level_5
NOM : Messager Restauré
RARETÉ : Rare
DESCRIPTION : Les langues du monde résonnent à nouveau dans le temple d'Hermès.
DÉBLOCAGE : Temple d'Hermès niveau 5
CATÉGORIE : Temples
XP BONUS : 300
PROMPT : "Hermes temple with scrolls flying in wind, winged sandals, chibi messenger, global languages floating, clay render, green and gold"

────────────────────────────────────────────────────────────────────────────────

ID : temple_demeter_level_5
NOM : Jardin Fertile
RARETÉ : Rare
DESCRIPTION : Les terres de Déméter fleurissent à nouveau sous tes soins.
DÉBLOCAGE : Temple de Déméter niveau 5
CATÉGORIE : Temples
XP BONUS : 300
PROMPT : "Demeter temple surrounded by blooming plants, harvest abundance, chibi gardener, warm earth tones, clay render, green and gold"

────────────────────────────────────────────────────────────────────────────────

ID : temple_apollon_level_5
NOM : Lyre Ressuscitée
RARETÉ : Rare
DESCRIPTION : La musique de la philosophie résonne à nouveau dans le temple d'Apollon.
DÉBLOCAGE : Temple d'Apollon niveau 5
CATÉGORIE : Temples
XP BONUS : 300
PROMPT : "Apollon temple with golden lyre glowing, musical notes floating, chibi musician, sunlight rays, clay render, gold and white"

────────────────────────────────────────────────────────────────────────────────

ID : temple_promethee_level_5
NOM : Flamme de l'Innovation
RARETÉ : Rare
DESCRIPTION : La torche de Prométhée éclaire à nouveau le chemin de l'humanité.
DÉBLOCAGE : Temple de Prométhée niveau 5
CATÉGORIE : Temples
XP BONUS : 300
PROMPT : "Promethee temple with eternal flame burning, chibi hero holding torch, innovative symbols, clay render, orange and purple"

────────────────────────────────────────────────────────────────────────────────

ID : temple_three_level_10
NOM : Triple Gloire
RARETÉ : Épique
DESCRIPTION : Trois temples atteignent leur apogée grâce à ta dévotion.
DÉBLOCAGE : 3 temples différents niveau 10
CATÉGORIE : Temples
XP BONUS : 1500
PROMPT : "Three temples fully restored side by side, glowing divine light, chibi hero in center, epic sky, clay render, rainbow divine colors"

────────────────────────────────────────────────────────────────────────────────

ID : temple_five_level_10
NOM : Panthéon Renaissant
RARETÉ : Légendaire
DESCRIPTION : Cinq dieux règnent à nouveau en pleine puissance.
DÉBLOCAGE : 5 temples différents niveau 10
CATÉGORIE : Temples
XP BONUS : 3000
PROMPT : "Five massive temples shining with divine power, gods visible, chibi hero ascending stairs, epic Olympus panorama, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : temple_all_level_5
NOM : Reconstruction Généralisée
RARETÉ : Épique
DESCRIPTION : Tous les temples sont en voie de restauration active.
DÉBLOCAGE : Tous les 9 temples au moins niveau 5
CATÉGORIE : Temples
XP BONUS : 2000
PROMPT : "All nine temples glowing at medium level, connected by light bridges, chibi hero coordinating, panoramic view, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : temple_single_max_xp
NOM : Maître d'un Temple
RARETÉ : Épique
DESCRIPTION : Tu as consacré toute ton énergie à un seul dieu.
DÉBLOCAGE : Un temple atteint 10000 XP (niveau 10 largement dépassé)
CATÉGORIE : Temples
XP BONUS : 1000
PROMPT : "Single temple radiating overwhelming power, chibi disciple meditating, divine aura, clay render, monochrome temple color"

────────────────────────────────────────────────────────────────────────────────

ID : temple_balanced_all
NOM : Équilibre Parfait
RARETÉ : Légendaire
DESCRIPTION : Tous les temples progressent en harmonie totale.
DÉBLOCAGE : Tous les temples entre niveau 7 et 9 en même temps
CATÉGORIE : Temples
XP BONUS : 2500
PROMPT : "Nine temples perfectly balanced in power, forming a circle, divine harmony light, chibi hero meditating center, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : temple_zeus_perfect_only
NOM : Tyran de la Logique
RARETÉ : WTF Épique
DESCRIPTION : Zeus niveau 10 mais tous les autres temples niveau 0. La dictature mathématique.
DÉBLOCAGE : Temple Zeus niveau 10, tous les autres niveau 0
CATÉGORIE : Temples
XP BONUS : 666
PROMPT : "Zeus temple massive and glowing, all other temples in ruins, dramatic contrast, chibi hero obsessed with math, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : temple_abandoned_three
NOM : Négligence Divine
RARETÉ : WTF Rare
DESCRIPTION : Trois dieux pleurent ton abandon.
DÉBLOCAGE : 3 temples restés niveau 0 pendant 30 jours
CATÉGORIE : Temples
XP BONUS : 100
PROMPT : "Three abandoned temples with vines and darkness, crying chibi gods, sad atmosphere, clay render, grey and dark blue"

────────────────────────────────────────────────────────────────────────────────

ID : temple_speed_level_10
NOM : Constructeur Divin
RARETÉ : Légendaire
DESCRIPTION : Tu as restauré un temple complet en moins de 7 jours.
DÉBLOCAGE : Un temple de niveau 0 à 10 en moins de 7 jours
CATÉGORIE : Temples
XP BONUS : 2000
PROMPT : "Temple rebuilding at lightning speed, construction particles flying, chibi builder with hammer, time-lapse effect, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : temple_marathon_xp
NOM : Offrande Colossale
RARETÉ : Épique
DESCRIPTION : Tu as donné 1000 XP à un temple en une seule session.
DÉBLOCAGE : Gagner 1000+ XP temple en une session
CATÉGORIE : Temples
XP BONUS : 800
PROMPT : "Massive XP offering flowing into temple, golden river of knowledge, chibi hero exhausted but proud, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : temple_first_reward_claimed
NOM : Bénédiction Divine
RARETÉ : Rare
DESCRIPTION : Tu as réclamé ta première récompense de palier de temple.
DÉBLOCAGE : Réclamer une récompense temple (niveau 3, 6, ou 10)
CATÉGORIE : Temples
XP BONUS : 200
PROMPT : "God handing divine reward to chibi hero, glowing gift box, temple background, celebratory, clay render, gold and white"

────────────────────────────────────────────────────────────────────────────────

ID : temple_all_rewards_zeus
NOM : Favori de Zeus
RARETÉ : Épique
DESCRIPTION : Tu as collecté toutes les récompenses du temple de Zeus.
DÉBLOCAGE : Réclamer toutes les récompenses Zeus (3, 6, 10)
CATÉGORIE : Temples
XP BONUS : 500
PROMPT : "Zeus pleased, handing last reward, lightning celebration, chibi hero covered in math symbols, clay render, blue gold"

────────────────────────────────────────────────────────────────────────────────

ID : temple_night_offering
NOM : Offrande de Minuit
RARETÉ : Rare
DESCRIPTION : Tu as fait progresser un temple entre minuit et 4h du matin.
DÉBLOCAGE : Gagner XP temple entre 0h et 4h
CATÉGORIE : Temples
XP BONUS : 300
PROMPT : "Temple glowing in moonlight, night stars, chibi hero yawning, peaceful nocturnal atmosphere, clay render, blue silver"

────────────────────────────────────────────────────────────────────────────────

ID : temple_all_subcategories_one
NOM : Spécialiste Accompli
RARETÉ : Épique
DESCRIPTION : Tu as rempli toutes les sous-catégories d'une matière.
DÉBLOCAGE : Toutes les sous-catégories d'un temple ont au moins 1 savoir
CATÉGORIE : Temples
XP BONUS : 600
PROMPT : "Temple with all sub-sections glowing, organized knowledge tree, chibi scholar satisfied, clay render, organized light"

────────────────────────────────────────────────────────────────────────────────

ID : temple_hundred_savoirs_one
NOM : Bibliothèque Personnelle
RARETÉ : Légendaire
DESCRIPTION : 100 savoirs dans un seul temple. Tu es devenu l'expert absolu.
DÉBLOCAGE : 100 savoirs enregistrés dans un seul temple
CATÉGORIE : Temples
XP BONUS : 2000
PROMPT : "Temple overflowing with scrolls and books, massive knowledge collection, chibi librarian overwhelmed, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : temple_visual_stage_transition
NOM : Témoin de la Renaissance
RARETÉ : Commun
DESCRIPTION : Tu as vu un temple passer d'un stade visuel à un autre.
DÉBLOCAGE : Voir la transition visuelle d'un palier temple (0→3, 3→6, 6→10)
CATÉGORIE : Temples
XP BONUS : 50
PROMPT : "Temple transforming with golden particle effect, before/after split screen, chibi hero watching amazed, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : temple_unlock_boss_access
NOM : Portail du Défi
RARETÉ : Épique
DESCRIPTION : Tu as débloqué l'accès au boss d'une matière.
DÉBLOCAGE : Temple niveau 6 (premier déblocage boss)
CATÉGORIE : Temples
XP BONUS : 700
PROMPT : "Boss portal opening in temple, ominous glow, chibi hero preparing for battle, clay render, dark and gold"

────────────────────────────────────────────────────────────────────────────────

ID : temple_all_music_unlocked
NOM : Symphonie des Dieux
RARETÉ : Légendaire
DESCRIPTION : Toutes les musiques divines ont été débloquées par ta progression.
DÉBLOCAGE : Débloquer toutes les musiques via niveaux temples
CATÉGORIE : Temples
XP BONUS : 1500
PROMPT : "Musical notes flowing from all temples, grand symphony, chibi conductor, epic orchestral atmosphere, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : temple_bonus_gameplay_used
NOM : Pouvoir Divin Activé
RARETÉ : Rare
DESCRIPTION : Tu as utilisé un bonus de temple en quiz.
DÉBLOCAGE : Utiliser un bonus temple (temps, indices, fragments)
CATÉGORIE : Temples
XP BONUS : 150
PROMPT : "Divine power activating during quiz, glowing aura, chibi hero empowered, clay render, golden energy"

────────────────────────────────────────────────────────────────────────────────

ID : temple_ruin_to_glory_witness
NOM : De la Cendre à la Lumière
RARETÉ : Mythique
DESCRIPTION : Tu as été témoin de la résurrection complète de l'Olympe.
DÉBLOCAGE : Voir tous les temples passer de niveau 0 à niveau 10
CATÉGORIE : Temples
XP BONUS : 5000
PROMPT : "Full Olympus transformation montage, ruins to glory, all gods celebrating, chibi hero epic hero pose, clay render"

═══════════════════════════════════════════════════════════════════════════════
CATÉGORIE 2 — ORACLE 3 PORTES & VALIDATION (25 BADGES)
═══════════════════════════════════════════════════════════════════════════════

ID : oracle_text_free_first
NOM : Créateur de Savoir
RARETÉ : Rare
DESCRIPTION : Tu as créé ton premier savoir depuis une demande libre.
DÉBLOCAGE : Générer un savoir via "Créer depuis demande"
CATÉGORIE : Oracle
XP BONUS : 200
PROMPT : "Oracle portal glowing, chibi hero writing in light, creative energy, clay render, purple and gold"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_import_pdf_first
NOM : Archiviste Numérique
RARETÉ : Commun
DESCRIPTION : Tu as importé ton premier document PDF dans l'Oracle.
DÉBLOCAGE : Importer un PDF via Oracle
CATÉGORIE : Oracle
XP BONUS : 100
PROMPT : "PDF file glowing and transforming into scroll, chibi hero uploading, digital magic, clay render, blue tech"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_three_methods_used
NOM : Maître des Trois Portes
RARETÉ : Épique
DESCRIPTION : Tu as utilisé les trois méthodes d'Oracle.
DÉBLOCAGE : Scanner + Importer + Créer demande (au moins 1 fois chacun)
CATÉGORIE : Oracle
XP BONUS : 500
PROMPT : "Three oracle portals open simultaneously, chibi hero mastering all methods, triangular composition, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_conseil_used
NOM : Chercheur Guidé
RARETÉ : Rare
DESCRIPTION : Tu as demandé conseil aux Oracles pour formuler ton savoir.
DÉBLOCAGE : Utiliser "Conseil des Oracles" (aide formulation)
CATÉGORIE : Oracle
XP BONUS : 150
PROMPT : "Multiple oracle spirits helping chibi hero brainstorm, thought bubbles, collaborative magic, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : validation_resume_edited
NOM : Éditeur Exigeant
RARETÉ : Commun
DESCRIPTION : Tu as édité un résumé avant de l'accepter.
DÉBLOCAGE : Modifier un résumé généré avant validation
CATÉGORIE : Oracle
XP BONUS : 100
PROMPT : "Chibi editor modifying glowing text, perfectionist focus, clay render, organized workspace"

────────────────────────────────────────────────────────────────────────────────

ID : validation_resume_reformulation
NOM : Alchimiste du Verbe
RARETÉ : Rare
DESCRIPTION : Tu as demandé une reformulation de résumé.
DÉBLOCAGE : Utiliser "Reformuler" sur un résumé
CATÉGORIE : Oracle
XP BONUS : 200
PROMPT : "Text transforming with magical sparkles, chibi alchemist, reformulation magic, clay render, purple gold"

────────────────────────────────────────────────────────────────────────────────

ID : validation_temple_changed
NOM : Correcteur Divin
RARETÉ : Commun
DESCRIPTION : Tu as corrigé le temple de destination d'un savoir.
DÉBLOCAGE : Changer manuellement le temple d'un résumé
CATÉGORIE : Oracle
XP BONUS : 50
PROMPT : "Chibi hero redirecting glowing scroll to different temple, correction arrows, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : validation_perfect_first_time
NOM : Oracle Infaillible
RARETÉ : Épique
DESCRIPTION : Le résumé généré était si parfait que tu l'as accepté immédiatement.
DÉBLOCAGE : Accepter 10 résumés sans modification
CATÉGORIE : Oracle
XP BONUS : 400
PROMPT : "Perfect glowing scroll, chibi hero nodding approval, divine quality seal, clay render, gold white"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_multi_title_choice
NOM : L'Embarras du Choix
RARETÉ : Rare
DESCRIPTION : Tu as choisi parmi plusieurs propositions de titres.
DÉBLOCAGE : Utiliser la fonctionnalité multi-titres
CATÉGORIE : Oracle
XP BONUS : 150
PROMPT : "Multiple title options floating, chibi hero pondering, decision moment, clay render, thoughtful atmosphere"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_tts_resume
NOM : Auditeur Divin
RARETÉ : Commun
DESCRIPTION : Tu as écouté un résumé lu par les dieux.
DÉBLOCAGE : Utiliser TTS sur un résumé
CATÉGORIE : Oracle
XP BONUS : 50
PROMPT : "Sound waves emanating from scroll, chibi hero listening with headphones, audio magic, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_quality_detection
NOM : Détecteur de Qualité
RARETÉ : Rare
DESCRIPTION : L'Oracle a détecté un contenu de faible qualité et te l'a signalé.
DÉBLOCAGE : Recevoir un avertissement qualité OCR/contenu
CATÉGORIE : Oracle
XP BONUS : 100
PROMPT : "Warning sign glowing, chibi hero examining blurry document, quality analysis, clay render, amber alert"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_confidence_high
NOM : Vision Claire
RARETÉ : Commun
DESCRIPTION : L'Oracle a perçu ton document avec une confiance maximale.
DÉBLOCAGE : OCR confiance ≥ 95%
CATÉGORIE : Oracle
XP BONUS : 100
PROMPT : "Crystal clear document glowing, oracle eye wide open, perfect clarity, clay render, bright light"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_retake_photo
NOM : Perfectionniste Visuel
RARETÉ : Commun
DESCRIPTION : Tu as repris une photo pour améliorer la capture.
DÉBLOCAGE : Utiliser "Reprendre photo" après un scan
CATÉGORIE : Oracle
XP BONUS : 50
PROMPT : "Chibi photographer adjusting angle, camera with retry icon, perfectionist scene, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_scan_animation_watched
NOM : Témoin de la Magie
RARETÉ : Commun
DESCRIPTION : Tu as observé l'animation de scan laser premium.
DÉBLOCAGE : Scanner un document (déclenche animation)
CATÉGORIE : Oracle
XP BONUS : 25
PROMPT : "Sci-fi laser scanning document, mystical particles, chibi hero watching amazed, clay render, cyan glow"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_pre_analysis_correct
NOM : Préscience
RARETÉ : Rare
DESCRIPTION : La pré-analyse matière était exactement correcte.
DÉBLOCAGE : Pré-analyse correcte confirmée après validation
CATÉGORIE : Oracle
XP BONUS : 150
PROMPT : "Prediction crystal showing correct result, chibi oracle pleased, accurate foresight, clay render, blue crystal"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_quota_fatigue
NOM : Épuisement Divin
RARETÉ : WTF Commun
DESCRIPTION : Tu as épuisé un dieu avec trop de demandes.
DÉBLOCAGE : Atteindre quota API et recevoir message diégétique
CATÉGORIE : Oracle
XP BONUS : 50
PROMPT : "Exhausted god sleeping on cloud, chibi hero apologetic, fatigue aura, clay render, tired colors"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_long_wait_patient
NOM : Patience Olympienne
RARETÉ : Rare
DESCRIPTION : Tu as attendu patiemment une génération IA longue (>2min).
DÉBLOCAGE : Attendre un chargement IA de plus de 2 minutes
CATÉGORIE : Oracle
XP BONUS : 200
PROMPT : "Chibi hero meditating peacefully while hourglass flows, serene patience, clay render, zen atmosphere"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_three_doors_video
NOM : Spectateur de la Révélation
RARETÉ : Commun
DESCRIPTION : Tu as vu la scène premium des trois portes d'Oracle.
DÉBLOCAGE : Voir la vidéo d'entrée Oracle complète première fois
CATÉGORIE : Oracle
XP BONUS : 50
PROMPT : "Three massive divine doors opening, epic reveal, chibi hero standing before choice, clay render, dramatic"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_promethee_help_formulation
NOM : Conseil de Prométhée
RARETÉ : Rare
DESCRIPTION : Prométhée t'a aidé à mieux formuler ta demande.
DÉBLOCAGE : Recevoir conseil contextuel Prométhée dans Oracle
CATÉGORIE : Oracle
XP BONUS : 150
PROMPT : "Promethee guiding chibi hero's hand writing, mentorship glow, clay render, wise orange light"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_text_free_ten
NOM : Créateur Prolifique
RARETÉ : Épique
DESCRIPTION : 10 savoirs créés depuis des demandes libres.
DÉBLOCAGE : 10 savoirs générés via texte libre
CATÉGORIE : Oracle
XP BONUS : 500
PROMPT : "Ten glowing scrolls floating around chibi creator, imaginative aura, clay render, rainbow knowledge"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_metadata_rich
NOM : Archiviste Méticuleux
RARETÉ : Rare
DESCRIPTION : Tous tes savoirs sont enrichis de métadonnées complètes.
DÉBLOCAGE : 20 savoirs avec métadonnées complètes (source, méthode, date, profil)
CATÉGORIE : Oracle
XP BONUS : 300
PROMPT : "Organized archive with detailed labels, chibi librarian satisfied, perfect organization, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_same_course_variants
NOM : Perfectionniste Répétitif
RARETÉ : WTF Rare
DESCRIPTION : Tu as généré 3 versions du même cours via Oracle.
DÉBLOCAGE : Scanner le même cours 3 fois et valider 3 résumés différents
CATÉGORIE : Oracle
XP BONUS : 200
PROMPT : "Three similar scrolls side by side, chibi hero comparing obsessively, perfectionist madness, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_instant_quiz_all
NOM : Impatient Conquérant
RARETÉ : Rare
DESCRIPTION : Tu as lancé un quiz immédiatement après TOUS tes résumés.
DÉBLOCAGE : Lancer quiz immédiatement après validation pour 20 savoirs consécutifs
CATÉGORIE : Oracle
XP BONUS : 300
PROMPT : "Rapid-fire quiz launching, chibi hero rushing, speed lines, impatient energy, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : oracle_skip_quiz_all
NOM : Collectionneur Pur
RARETÉ : Rare
DESCRIPTION : Tu collectes le savoir pour le savoir, sans quiz immédiat.
DÉBLOCAGE : Sauvegarder 20 savoirs sans lancer de quiz après
CATÉGORIE : Oracle
XP BONUS : 300
PROMPT : "Growing library with no quiz portals, pure knowledge collection, chibi scholar content, clay render"

═══════════════════════════════════════════════════════════════════════════════
CATÉGORIE 3 — INVENTAIRE & ÉCONOMIE (20 BADGES)
═══════════════════════════════════════════════════════════════════════════════

ID : inventory_first_legendary
NOM : Relique Légendaire
RARETÉ : Légendaire
DESCRIPTION : Tu possèdes ton premier objet légendaire.
DÉBLOCAGE : Obtenir un objet de rareté Légendaire
CATÉGORIE : Inventaire
XP BONUS : 1000
PROMPT : "Legendary glowing item in treasure chest, chibi hero awestruck, golden divine light, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_first_mythic
NOM : Artefact Mythique
RARETÉ : Mythique
DESCRIPTION : Tu as obtenu un objet de rareté mythique. Les dieux te craignent.
DÉBLOCAGE : Obtenir un objet de rareté Mythique
CATÉGORIE : Inventaire
XP BONUS : 2000
PROMPT : "Mythic artifact radiating overwhelming power, chibi hero transformed, divine ascension aura, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_fifty_different
NOM : Musée Personnel
RARETÉ : Épique
DESCRIPTION : 50 objets différents dans ton inventaire.
DÉBLOCAGE : Posséder 50 objets différents simultanément
CATÉGORIE : Inventaire
XP BONUS : 800
PROMPT : "Massive treasure room with organized display, chibi curator, museum quality, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_category_complete
NOM : Collection Complète
RARETÉ : Légendaire
DESCRIPTION : Tu as tous les objets d'une catégorie.
DÉBLOCAGE : Posséder tous les objets d'une catégorie (Artefacts/Équipements/Reliques)
CATÉGORIE : Inventaire
XP BONUS : 1500
PROMPT : "Complete set glowing in formation, chibi collector proud, full collection display, clay render, organized"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_locked_item
NOM : Protecteur de Trésor
RARETÉ : Commun
DESCRIPTION : Tu as verrouillé ton premier objet précieux.
DÉBLOCAGE : Verrouiller un objet
CATÉGORIE : Inventaire
XP BONUS : 50
PROMPT : "Locked treasure chest with divine padlock, chibi guardian, security magic, clay render, protected"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_favorite_set
NOM : Favori Choisi
RARETÉ : Commun
DESCRIPTION : Tu as marqué ton premier objet favori.
DÉBLOCAGE : Marquer un objet comme favori
CATÉGORIE : Inventaire
XP BONUS : 50
PROMPT : "Item with golden star mark, chibi hero showing preference, favoritism glow, clay render, warm"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_sort_master
NOM : Maître de l'Ordre
RARETÉ : Rare
DESCRIPTION : Tu as utilisé tous les types de tri d'inventaire.
DÉBLOCAGE : Trier par rareté, date, nom, catégorie, favoris (tous)
CATÉGORIE : Inventaire
XP BONUS : 200
PROMPT : "Organized inventory with multiple sort options visible, chibi organizer, perfect order, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_equipped_synergy
NOM : Synergie Divine
RARETÉ : Épique
DESCRIPTION : Tu as équipé des objets avec synergie active.
DÉBLOCAGE : Activer une synergie d'objets équipés (ex: 3 objets Zeus)
CATÉGORIE : Inventaire
XP BONUS : 600
PROMPT : "Multiple equipped items glowing together, synergy energy connecting them, chibi hero empowered, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_degraded_item
NOM : Négligence Punitive
RARETÉ : WTF Commun
DESCRIPTION : Un objet s'est dégradé par manque d'utilisation de sa matière.
DÉBLOCAGE : Un objet perd durabilité par abandon matière
CATÉGORIE : Inventaire
XP BONUS : 50
PROMPT : "Rusty degraded item, sad chibi hero, neglect consequences, clay render, brown decay"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_export_list
NOM : Comptable Divin
RARETÉ : Rare
DESCRIPTION : Tu as exporté ta liste d'inventaire.
DÉBLOCAGE : Exporter l'inventaire (liste texte/PDF)
CATÉGORIE : Inventaire
XP BONUS : 150
PROMPT : "Inventory list printing out, chibi accountant, organized paperwork, clay render, professional"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_compare_items
NOM : Analyste Stratégique
RARETÉ : Rare
DESCRIPTION : Tu as comparé deux objets avant de choisir.
DÉBLOCAGE : Utiliser fonctionnalité comparaison objets
CATÉGORIE : Inventaire
XP BONUS : 100
PROMPT : "Two items side by side with stats comparison, chibi hero analyzing, strategic choice, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_capacity_max
NOM : Hoarder Olympien
RARETÉ : Épique
DESCRIPTION : Ton inventaire est plein à craquer.
DÉBLOCAGE : Atteindre capacité max inventaire (ex: 150/150)
CATÉGORIE : Inventaire
XP BONUS : 500
PROMPT : "Overflowing treasure chest, items spilling out, chibi hoarder overwhelmed, clay render, chaos"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_all_tabs_visited
NOM : Explorateur Complet
RARETÉ : Commun
DESCRIPTION : Tu as visité tous les onglets d'inventaire.
DÉBLOCAGE : Ouvrir Fragments, Artefacts, Équipements, Consommables, Reliques
CATÉGORIE : Inventaire
XP BONUS : 100
PROMPT : "Five inventory tabs glowing, chibi explorer satisfied, complete tour, clay render, organized UI"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_stack_max
NOM : Empileur Professionnel
RARETÉ : Rare
DESCRIPTION : Tu as atteint le stack maximum d'un objet.
DÉBLOCAGE : 99 exemplaires d'un objet stackable
CATÉGORIE : Inventaire
XP BONUS : 200
PROMPT : "Massive stack of identical items, chibi stacker proud, tower formation, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_source_diversity
NOM : Collectionneur Universel
RARETÉ : Épique
DESCRIPTION : Tes objets proviennent de toutes les sources possibles.
DÉBLOCAGE : Objets de Forge, Quête, Boss, Récompense Divine, Achat (au moins 1 de chaque)
CATÉGORIE : Inventaire
XP BONUS : 700
PROMPT : "Items from different sources displayed, diverse collection, chibi collector, multicolor origins, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_equipped_full_set
NOM : Guerrier Complet
RARETÉ : Épique
DESCRIPTION : Tous tes slots d'équipement sont remplis.
DÉBLOCAGE : Équiper tous les slots possibles en même temps
CATÉGORIE : Inventaire
XP BONUS : 600
PROMPT : "Fully equipped warrior chibi, all gear glowing, complete armor, clay render, battle-ready"

────────────────────────────────────────────────────────────────────────────────

ID : inventory_consumable_used
NOM : Consommateur Avisé
RARETÉ : Commun
DESCRIPTION : Tu as utilisé ton premier objet consommable.
DÉBLOCAGE : Utiliser un objet à usage unique
CATÉGORIE : Inventaire
XP BONUS : 50
PROMPT : "Consumable item dissolving into magic particles, chibi user empowered, clay render, consumption effect"

────────────────────────────────────────────────────────────────────────────────

ID : currency_million_eclats
NOM : Magnat de l'Éclat
RARETÉ : Légendaire
DESCRIPTION : Un million d'Éclats de Savoir. Tu es riche.
DÉBLOCAGE : Posséder 1,000,000 Éclats de Savoir
CATÉGORIE : Inventaire
XP BONUS : 2000
PROMPT : "Mountain of glowing shards, chibi tycoon on throne, wealth overload, clay render, golden abundance"

────────────────────────────────────────────────────────────────────────────────

ID : currency_ambrosia_ten_thousand
NOM : Réserve Divine
RARETÉ : Épique
DESCRIPTION : 10,000 Ambroisie. Les dieux t'envient.
DÉBLOCAGE : Posséder 10,000 Ambroisie
CATÉGORIE : Inventaire
XP BONUS : 1000
PROMPT : "Vats of glowing ambrosia, chibi keeper wealthy, divine nectar storage, clay render, amber gold"

────────────────────────────────────────────────────────────────────────────────

ID : economy_spend_million
NOM : Dépensier Légendaire
RARETÉ : Légendaire
DESCRIPTION : Tu as dépensé plus d'un million de monnaie au total.
DÉBLOCAGE : Dépenser 1,000,000+ en Éclats/Ambroisie cumulé
CATÉGORIE : Inventaire
XP BONUS : 1500
PROMPT : "Coins flying everywhere, chibi spender carefree, wealth circulation, clay render, spending spree"

═══════════════════════════════════════════════════════════════════════════════
CATÉGORIE 4 — JARDIN DE DÉMÉTER (15 BADGES)
═══════════════════════════════════════════════════════════════════════════════

ID : jardin_first_visit
NOM : Main Verte Novice
RARETÉ : Commun
DESCRIPTION : Tu as visité le Jardin de Déméter pour la première fois.
DÉBLOCAGE : Ouvrir JardinDemeterActivity
CATÉGORIE : Jardin
XP BONUS : 100
PROMPT : "Beautiful garden entrance, chibi visitor arriving, welcoming plants, clay render, green paradise"

────────────────────────────────────────────────────────────────────────────────

ID : jardin_first_plant_watered
NOM : Premier Arrosage
RARETÉ : Commun
DESCRIPTION : Tu as arrosé ton premier savoir fané.
DÉBLOCAGE : Réviser un savoir depuis le jardin (premier)
CATÉGORIE : Jardin
XP BONUS : 150
PROMPT : "Chibi gardener watering wilted plant, droplets glowing, revival moment, clay render, refreshing"

────────────────────────────────────────────────────────────────────────────────

ID : jardin_all_plants_healthy
NOM : Jardin Parfait
RARETÉ : Épique
DESCRIPTION : Tous tes savoirs sont en pleine santé.
DÉBLOCAGE : Aucun savoir fané dans le jardin
CATÉGORIE : Jardin
XP BONUS : 800
PROMPT : "Thriving perfect garden, all plants blooming, chibi master gardener proud, clay render, vibrant green"

────────────────────────────────────────────────────────────────────────────────

ID : jardin_revival_twenty
NOM : Réanimateur de Savoir
RARETÉ : Rare
DESCRIPTION : Tu as ressuscité 20 savoirs fanés.
DÉBLOCAGE : Réviser 20 savoirs fanés depuis le jardin
CATÉGORIE : Jardin
XP BONUS : 400
PROMPT : "Multiple plants reviving simultaneously, chibi healer, resurrection magic, clay render, life energy"

────────────────────────────────────────────────────────────────────────────────

ID : jardin_weather_rain
NOM : Pluie Bienfaisante
RARETÉ : Rare
DESCRIPTION : La météo divine a béni ton jardin de pluie.
DÉBLOCAGE : Voir la pluie divine dans le jardin (révisions à jour)
CATÉGORIE : Jardin
XP BONUS : 200
PROMPT : "Divine rain falling on garden, happy plants, chibi character dancing in rain, clay render, refreshing"

────────────────────────────────────────────────────────────────────────────────

ID : jardin_weather_drought
NOM : Sécheresse Menaçante
RARETÉ : WTF Commun
DESCRIPTION : Ton jardin souffre de sécheresse par négligence.
DÉBLOCAGE : Voir la sécheresse (trop de savoirs fanés)
CATÉGORIE : Jardin
XP BONUS : 50
PROMPT : "Cracked dry earth, wilting plants, worried chibi gardener, drought atmosphere, clay render, brown"

────────────────────────────────────────────────────────────────────────────────

ID : jardin_demeter_visit
NOM : Visite de la Déesse
RARETÉ : Épique
DESCRIPTION : Déméter en personne est venue voir ton jardin.
DÉBLOCAGE : Recevoir message/dialogue spécial de Déméter dans jardin
CATÉGORIE : Jardin
XP BONUS : 500
PROMPT : "Demeter goddess appearing in garden, chibi gardener honored, divine visitation, clay render, green gold"

────────────────────────────────────────────────────────────────────────────────

ID : jardin_no_wilted_thirty_days
NOM : Jardinier Éternel
RARETÉ : Légendaire
DESCRIPTION : Aucun savoir fané pendant 30 jours consécutifs.
DÉBLOCAGE : 30 jours sans savoir fané
CATÉGORIE : Jardin
XP BONUS : 2000
PROMPT : "Eternal blooming garden, time flowing peacefully, chibi master, immortal plants, clay render, timeless"

────────────────────────────────────────────────────────────────────────────────

ID : jardin_grid_organized
NOM : Organisateur Esthétique
RARETÉ : Rare
DESCRIPTION : Ton jardin est organisé avec soin.
DÉBLOCAGE : Organiser le jardin (tri/filtre utilisé)
CATÉGORIE : Jardin
XP BONUS : 150
PROMPT : "Perfectly organized garden grid, chibi organizer satisfied, aesthetic perfection, clay render, orderly"

────────────────────────────────────────────────────────────────────────────────

ID : jardin_notification_responded
NOM : Réactif Divin
RARETÉ : Commun
DESCRIPTION : Tu as répondu à une notification jardin dans l'heure.
DÉBLOCAGE : Ouvrir jardin moins d'1h après notification push
CATÉGORIE : Jardin
XP BONUS : 100
PROMPT : "Notification bell with chibi hero rushing, quick response, timely care, clay render, urgent"

────────────────────────────────────────────────────────────────────────────────

ID : jardin_seed_stage_seen
NOM : Témoin de la Naissance
RARETÉ : Commun
DESCRIPTION : Tu as vu un savoir au stade graine.
DÉBLOCAGE : Voir un savoir jamais révisé (graine)
CATÉGORIE : Jardin
XP BONUS : 50
PROMPT : "Tiny seed planted in soil, potential visible, chibi observer, beginning of life, clay render, hopeful"

────────────────────────────────────────────────────────────────────────────────

ID : jardin_bloom_stage_seen
NOM : Floraison Complète
RARETÉ : Rare
DESCRIPTION : Tu as vu un savoir en pleine floraison.
DÉBLOCAGE : Voir un savoir bien maîtrisé (fleur épanouie)
CATÉGORIE : Jardin
XP BONUS : 200
PROMPT : "Magnificent blooming flower, chibi admiring, peak mastery, clay render, vibrant colors"

────────────────────────────────────────────────────────────────────────────────

ID : jardin_spaced_repetition_optimal
NOM : Maître de la Répétition
RARETÉ : Épique
DESCRIPTION : Tu suis parfaitement l'algorithme de révision espacée.
DÉBLOCAGE : 20 révisions effectuées au moment optimal selon algorithme
CATÉGORIE : Jardin
XP BONUS : 700
PROMPT : "Perfect timing visualization, clock and plant synchronized, chibi master, optimal learning, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : jardin_export_report
NOM : Rapport du Jardinier
RARETÉ : Rare
DESCRIPTION : Tu as exporté le rapport de ton jardin.
DÉBLOCAGE : Exporter rapport révisions jardin
CATÉGORIE : Jardin
XP BONUS : 150
PROMPT : "Garden report document printing, chibi secretary, professional gardening report, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : jardin_hundred_plants
NOM : Botaniste Légendaire
RARETÉ : Légendaire
DESCRIPTION : 100 plantes dans ton jardin personnel.
DÉBLOCAGE : 100 savoirs dans le système jardin
CATÉGORIE : Jardin
XP BONUS : 1500
PROMPT : "Massive garden with hundreds of diverse plants, chibi botanist overwhelmed, botanical paradise, clay render"

═══════════════════════════════════════════════════════════════════════════════
CATÉGORIE 5 — QUÊTES & AVENTURE (30 BADGES)
═══════════════════════════════════════════════════════════════════════════════

ID : adventure_unlocked
NOM : Porte de l'Aventure
RARETÉ : Épique
DESCRIPTION : Le mode Aventure s'ouvre devant toi.
DÉBLOCAGE : Débloquer le mode Aventure (1 savoir dans chaque temple)
CATÉGORIE : Aventure
XP BONUS : 1000
PROMPT : "Massive adventure gate opening, epic portal, chibi hero ready, dramatic reveal, clay render, adventure"

────────────────────────────────────────────────────────────────────────────────

ID : adventure_first_zone
NOM : Explorateur Novice
RARETÉ : Commun
DESCRIPTION : Tu as exploré ta première zone d'aventure.
DÉBLOCAGE : Entrer dans une zone aventure
CATÉGORIE : Aventure
XP BONUS : 200
PROMPT : "First adventure zone entrance, chibi explorer stepping in, new world discovery, clay render, exciting"

────────────────────────────────────────────────────────────────────────────────

ID : quest_first_completed
NOM : Quêteur Débutant
RARETÉ : Commun
DESCRIPTION : Ta première quête accomplie.
DÉBLOCAGE : Compléter une quête
CATÉGORIE : Aventure
XP BONUS : 150
PROMPT : "Quest complete checkmark, chibi hero celebrating, quest scroll glowing, clay render, achievement"

────────────────────────────────────────────────────────────────────────────────

ID : quest_daily_first
NOM : Rituel Quotidien
RARETÉ : Commun
DESCRIPTION : Tu as accompli ta première quête quotidienne.
DÉBLOCAGE : Compléter une quête journalière
CATÉGORIE : Aventure
XP BONUS : 100
PROMPT : "Daily quest board with checkmark, chibi completing routine, daily rhythm, clay render, organized"

────────────────────────────────────────────────────────────────────────────────

ID : quest_weekly_first
NOM : Défi Hebdomadaire
RARETÉ : Rare
DESCRIPTION : Tu as relevé ton premier défi hebdomadaire.
DÉBLOCAGE : Compléter une quête hebdo
CATÉGORIE : Aventure
XP BONUS : 300
PROMPT : "Weekly quest banner glowing, chibi hero triumphant, weekly achievement, clay render, special reward"

────────────────────────────────────────────────────────────────────────────────

ID : quest_daily_perfect_week
NOM : Semaine Parfaite
RARETÉ : Épique
DESCRIPTION : 7 jours consécutifs de quêtes quotidiennes accomplies.
DÉBLOCAGE : Compléter quêtes daily 7 jours de suite
CATÉGORIE : Aventure
XP BONUS : 800
PROMPT : "Seven daily quest checkmarks glowing, perfect week calendar, chibi hero consistent, clay render, gold"

────────────────────────────────────────────────────────────────────────────────

ID : quest_collection_type
NOM : Collectionneur de Tâches
RARETÉ : Rare
DESCRIPTION : Tu as accompli tous les types de quêtes existants.
DÉBLOCAGE : Au moins 1 quête de chaque type (collection, quiz, défi, boss, etc.)
CATÉGORIE : Aventure
XP BONUS : 500
PROMPT : "Different quest types displayed, chibi completionist, variety achievement, clay render, diverse"

────────────────────────────────────────────────────────────────────────────────

ID : quest_ten_same_day
NOM : Marathonien de Quêtes
RARETÉ : Épique
DESCRIPTION : 10 quêtes accomplies en une seule journée.
DÉBLOCAGE : Compléter 10 quêtes en 1 jour
CATÉGORIE : Aventure
XP BONUS : 600
PROMPT : "Quest counter spinning rapidly, exhausted chibi hero, marathon effort, clay render, intense"

────────────────────────────────────────────────────────────────────────────────

ID : quest_narrative_arc_zeus
NOM : Héros de Zeus
RARETÉ : Légendaire
DESCRIPTION : Tu as complété l'arc narratif complet de Zeus.
DÉBLOCAGE : Compléter les 6 quêtes de l'arc Zeus
CATÉGORIE : Aventure
XP BONUS : 2000
PROMPT : "Zeus arc complete banner, lightning celebration, chibi champion of math, clay render, zeus glory"

────────────────────────────────────────────────────────────────────────────────

ID : quest_narrative_arc_athena
NOM : Disciple d'Athéna
RARETÉ : Légendaire
DESCRIPTION : Tu as terminé l'arc narratif d'Athéna.
DÉBLOCAGE : Compléter les 6 quêtes de l'arc Athéna
CATÉGORIE : Aventure
XP BONUS : 2000
PROMPT : "Athena arc complete, wisdom owl landing, chibi scholar honored, clay render, athena wisdom"

────────────────────────────────────────────────────────────────────────────────

ID : quest_narrative_three_arcs
NOM : Tri-Champion
RARETÉ : Mythique
DESCRIPTION : Trois arcs narratifs complets. Tu es une légende vivante.
DÉBLOCAGE : Compléter 3 arcs narratifs complets (any)
CATÉGORIE : Aventure
XP BONUS : 5000
PROMPT : "Three god banners displayed, chibi hero legendary status, triple champion, clay render, epic glory"

────────────────────────────────────────────────────────────────────────────────

ID : quest_all_main_complete
NOM : Sauveur de l'Olympe
RARETÉ : Mythique
DESCRIPTION : Toutes les quêtes principales accomplies. L'Olympe est sauvé.
DÉBLOCAGE : Compléter toutes les quêtes principales du jeu
CATÉGORIE : Aventure
XP BONUS : 10000
PROMPT : "Full Olympus restored and celebrating, all gods cheering, chibi ultimate hero, clay render, triumph"

────────────────────────────────────────────────────────────────────────────────

ID : quest_secret_found
NOM : Découvreur de Secrets
RARETÉ : Épique
DESCRIPTION : Tu as trouvé une quête secrète cachée.
DÉBLOCAGE : Découvrir et compléter une quête secrète
CATÉGORIE : Aventure
XP BONUS : 700
PROMPT : "Hidden quest revealing itself, magical secret discovery, chibi finder excited, clay render, mystery"

────────────────────────────────────────────────────────────────────────────────

ID : quest_branches_explored
NOM : Explorateur de Choix
RARETÉ : Rare
DESCRIPTION : Tu as exploré différentes branches narratives.
DÉBLOCAGE : Faire 3 choix différents dans quêtes à branches
CATÉGORIE : Aventure
XP BONUS : 400
PROMPT : "Branching paths visualization, chibi choosing direction, narrative choices, clay render, paths"

────────────────────────────────────────────────────────────────────────────────

ID : zone_olympus_visited
NOM : Marcheur de l'Olympe
RARETÉ : Commun
DESCRIPTION : Tu as foulé le Mont Olympe.
DÉBLOCAGE : Visiter zone Olympe
CATÉGORIE : Aventure
XP BONUS : 200
PROMPT : "Mount Olympus peak, chibi climber arriving, majestic view, clay render, mountain glory"

────────────────────────────────────────────────────────────────────────────────

ID : zone_tartarus_unlocked
NOM : Descente aux Enfers
RARETÉ : Épique
DESCRIPTION : Le Tartare s'ouvre. Les ténèbres t'appellent.
DÉBLOCAGE : Débloquer le Tartare (tous temples niveau 3+)
CATÉGORIE : Aventure
XP BONUS : 1000
PROMPT : "Dark Tartarus portal opening, ominous glow, brave chibi hero, underworld entrance, clay render, dark"

────────────────────────────────────────────────────────────────────────────────

ID : zone_library_unlocked
NOM : Bibliothèque Interdite
RARETÉ : Épique
DESCRIPTION : Les savoirs interdits sont désormais accessibles.
DÉBLOCAGE : Débloquer Bibliothèque Interdite (tous temples niveau 5+)
CATÉGORIE : Aventure
XP BONUS : 1000
PROMPT : "Forbidden library gates opening, ancient knowledge, chibi scholar entering, mysterious atmosphere, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : zone_palace_titans_unlocked
NOM : Palais des Titans
RARETÉ : Légendaire
DESCRIPTION : Le Palais des Titans t'attend. Les anciens dieux se réveillent.
DÉBLOCAGE : Débloquer Palais des Titans (tous temples niveau 8+)
CATÉGORIE : Aventure
XP BONUS : 2000
PROMPT : "Massive titan palace revealed, colossal architecture, chibi hero awed, legendary zone, clay render, epic"

────────────────────────────────────────────────────────────────────────────────

ID : zone_all_discovered
NOM : Cartographe Divin
RARETÉ : Légendaire
DESCRIPTION : Toutes les zones du monde ont été découvertes.
DÉBLOCAGE : Visiter toutes les zones disponibles
CATÉGORIE : Aventure
XP BONUS : 2500
PROMPT : "Complete world map glowing, all zones unlocked, chibi cartographer, full exploration, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : world_map_first_open
NOM : Lecteur de Cartes
RARETÉ : Commun
DESCRIPTION : Tu as ouvert la carte du monde pour la première fois.
DÉBLOCAGE : Ouvrir WorldMapActivity
CATÉGORIE : Aventure
XP BONUS : 100
PROMPT : "Ancient world map unrolling, chibi explorer studying, cartography magic, clay render, parchment"

────────────────────────────────────────────────────────────────────────────────

ID : world_progression_fifty_percent
NOM : Mi-Monde
RARETÉ : Épique
DESCRIPTION : Tu as exploré 50% du monde de RéviZeus.
DÉBLOCAGE : Progression monde ≥ 50%
CATÉGORIE : Aventure
XP BONUS : 1000
PROMPT : "Half-complete world map, progress bar at 50%, chibi midway, clay render, halfway celebration"

────────────────────────────────────────────────────────────────────────────────

ID : cinematic_intro_watched
NOM : Témoin de la Chute
RARETÉ : Commun
DESCRIPTION : Tu as vu la chute de l'Olympe.
DÉBLOCAGE : Voir intro cinématique complète
CATÉGORIE : Aventure
XP BONUS : 150
PROMPT : "Cinematic scene of Olympus falling, dramatic moment, chibi witness, epic story intro, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : cinematic_skip_intro
NOM : Impatient Pressé
RARETÉ : WTF Commun
DESCRIPTION : Tu as skip l'intro cinématique. Hérétique.
DÉBLOCAGE : Skip intro après première vue
CATÉGORIE : Aventure
XP BONUS : 10
PROMPT : "Skip button pressed, frustrated cinematic gods, impatient chibi, clay render, rushed"

────────────────────────────────────────────────────────────────────────────────

ID : lore_library_first_visit
NOM : Historien Novice
RARETÉ : Commun
DESCRIPTION : Tu as consulté la Bibliothèque du Lore.
DÉBLOCAGE : Ouvrir LoreLibraryActivity
CATÉGORIE : Aventure
XP BONUS : 100
PROMPT : "Ancient lore library entrance, glowing books, curious chibi reader, knowledge temple, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : lore_chapter_ten_read
NOM : Lecteur Assidu
RARETÉ : Rare
DESCRIPTION : Tu as lu 10 chapitres de lore.
DÉBLOCAGE : Lire 10 entrées lore différentes
CATÉGORIE : Aventure
XP BONUS : 300
PROMPT : "Stack of read lore books, chibi scholar satisfied, knowledge accumulation, clay render, studious"

────────────────────────────────────────────────────────────────────────────────

ID : lore_all_chapters_unlocked
NOM : Maître du Lore
RARETÉ : Légendaire
DESCRIPTION : Tous les chapitres de lore débloqués par ta progression.
DÉBLOCAGE : Débloquer tous les chapitres lore
CATÉGORIE : Aventure
XP BONUS : 2000
PROMPT : "Complete lore tome glowing, all chapters unlocked, chibi loremaster, ultimate knowledge, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : event_special_participated
NOM : Participant d'Événement
RARETÉ : Rare
DESCRIPTION : Tu as participé à un événement spécial.
DÉBLOCAGE : Participer à un événement temporaire
CATÉGORIE : Aventure
XP BONUS : 400
PROMPT : "Special event banner with confetti, chibi participant celebrating, limited time magic, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : event_double_xp_used
NOM : Opportuniste Divin
RARETÉ : Rare
DESCRIPTION : Tu as profité d'un événement double XP.
DÉBLOCAGE : Gagner XP pendant événement x2
CATÉGORIE : Aventure
XP BONUS : 300
PROMPT : "Double XP icon glowing, chibi opportunist maximizing gains, bonus event, clay render, golden multiplier"

────────────────────────────────────────────────────────────────────────────────

ID : quest_chain_five_in_row
NOM : Enchaînement Divin
RARETÉ : Épique
DESCRIPTION : 5 quêtes enchaînées sans pause.
DÉBLOCAGE : Compléter 5 quêtes liées en séquence
CATÉGORIE : Aventure
XP BONUS : 600
PROMPT : "Chain of five quests connected, chibi hero in flow state, momentum, clay render, chain links glowing"

═══════════════════════════════════════════════════════════════════════════════
CATÉGORIE 6 — BOSS ÉDUCATIFS (20 BADGES)
═══════════════════════════════════════════════════════════════════════════════

ID : boss_first_encountered
NOM : Face au Monstre
RARETÉ : Rare
DESCRIPTION : Tu as affronté ton premier boss éducatif.
DÉBLOCAGE : Entrer dans un combat de boss
CATÉGORIE : Boss
XP BONUS : 300
PROMPT : "Boss silhouette appearing, dramatic entrance, scared chibi hero, boss fight intro, clay render, epic"

────────────────────────────────────────────────────────────────────────────────

ID : boss_first_victory
NOM : Tueur de Monstres
RARETÉ : Épique
DESCRIPTION : Ta première victoire contre un boss.
DÉBLOCAGE : Vaincre un boss
CATÉGORIE : Boss
XP BONUS : 700
PROMPT : "Boss defeated dissolving, victorious chibi hero, triumph pose, boss victory, clay render, glorious"

────────────────────────────────────────────────────────────────────────────────

ID : boss_cyclope_defeated
NOM : Vainqueur du Cyclope
RARETÉ : Épique
DESCRIPTION : Le Cyclope du Raisonnement est tombé.
DÉBLOCAGE : Vaincre Cyclope (boss Maths/Zeus)
CATÉGORIE : Boss
XP BONUS : 800
PROMPT : "Defeated cyclope crumbling, mathematical symbols scattering, chibi mathematician victorious, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : boss_sphinx_defeated
NOM : Vainqueur du Sphinx
RARETÉ : Épique
DESCRIPTION : Le Sphinx des Mots s'incline devant toi.
DÉBLOCAGE : Vaincre Sphinx (boss Français/Athéna)
CATÉGORIE : Boss
XP BONUS : 800
PROMPT : "Defeated sphinx bowing, linguistic scrolls floating, chibi wordsmith proud, clay render, golden desert"

────────────────────────────────────────────────────────────────────────────────

ID : boss_kraken_defeated
NOM : Vainqueur du Kraken
RARETÉ : Épique
DESCRIPTION : Le Kraken Cellulaire retourne dans les abysses.
DÉBLOCAGE : Vaincre Kraken (boss SVT/Poséidon)
CATÉGORIE : Boss
XP BONUS : 800
PROMPT : "Defeated kraken sinking into ocean, cellular patterns dissolving, chibi biologist triumphant, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : boss_general_defeated
NOM : Vainqueur du Général
RARETÉ : Épique
DESCRIPTION : Le Général du Passé est vaincu.
DÉBLOCAGE : Vaincre Général (boss Histoire/Arès)
CATÉGORIE : Boss
XP BONUS : 800
PROMPT : "Defeated general dropping sword, historical scrolls scattering, chibi historian victorious, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : boss_golem_defeated
NOM : Vainqueur du Golem
RARETÉ : Épique
DESCRIPTION : Le Golem d'Alliage s'effondre.
DÉBLOCAGE : Vaincre Golem (boss Physique-Chimie/Héphaïstos)
CATÉGORIE : Boss
XP BONUS : 800
PROMPT : "Defeated alchemical golem crumbling, chemical formulas fading, chibi chemist proud, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : boss_griffon_defeated
NOM : Vainqueur du Griffon
RARETÉ : Épique
DESCRIPTION : Le Griffon Polyglotte se tait.
DÉBLOCAGE : Vaincre Griffon (boss Langues/Hermès)
CATÉGORIE : Boss
XP BONUS : 800
PROMPT : "Defeated multilingual griffon, language symbols scattering, chibi linguist victorious, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : boss_geant_defeated
NOM : Vainqueur du Géant
RARETÉ : Épique
DESCRIPTION : Le Géant des Continents est terrassé.
DÉBLOCAGE : Vaincre Géant (boss Géo/Déméter)
CATÉGORIE : Boss
XP BONUS : 800
PROMPT : "Defeated geographical giant falling, maps crumbling, chibi geographer triumphant, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : boss_masque_defeated
NOM : Vainqueur du Masque
RARETÉ : Épique
DESCRIPTION : Le Masque Solaire est dévoilé et vaincu.
DÉBLOCAGE : Vaincre Masque (boss Philo/Apollon)
CATÉGORIE : Boss
XP BONUS : 800
PROMPT : "Defeated philosophical mask shattering, light revealing truth, chibi philosopher victorious, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : boss_titan_defeated
NOM : Vainqueur du Titan
RARETÉ : Mythique
DESCRIPTION : Le Titan des Possibles n'est plus. Tu es devenu légende.
DÉBLOCAGE : Vaincre Titan des Possibles (boss final)
CATÉGORIE : Boss
XP BONUS : 3000
PROMPT : "Titan boss dissolving into light, ultimate victory, chibi ultimate hero, legendary triumph, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : boss_all_defeated
NOM : Tueur de Dieux
RARETÉ : Mythique
DESCRIPTION : Tous les boss sont tombés devant toi.
DÉBLOCAGE : Vaincre tous les boss du jeu
CATÉGORIE : Boss
XP BONUS : 5000
PROMPT : "All defeated bosses displayed as trophies, chibi god-slayer, ultimate boss hunter, clay render, trophy room"

────────────────────────────────────────────────────────────────────────────────

ID : boss_perfection_mode
NOM : Perfection Boss
RARETÉ : Légendaire
DESCRIPTION : Tu as vaincu un boss en mode Perfection (0 erreur).
DÉBLOCAGE : Boss vaincu avec 100% en mode Perfection
CATÉGORIE : Boss
XP BONUS : 2000
PROMPT : "Perfect flawless boss victory, chibi hero untouched, pristine performance, clay render, golden perfection"

────────────────────────────────────────────────────────────────────────────────

ID : boss_survival_mode
NOM : Survivant
RARETÉ : Légendaire
DESCRIPTION : Tu as survécu au mode Survie d'un boss.
DÉBLOCAGE : Boss vaincu en mode Survie (timer global)
CATÉGORIE : Boss
XP BONUS : 2000
PROMPT : "Survivor boss victory, clock stopped at last second, chibi hero exhausted, survival triumph, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : boss_mirror_mode
NOM : Vainqueur de Soi-Même
RARETÉ : Légendaire
DESCRIPTION : Tu as vaincu le boss miroir basé sur tes faiblesses.
DÉBLOCAGE : Boss vaincu en mode Miroir (basé sur UserSkillProfile)
CATÉGORIE : Boss
XP BONUS : 2000
PROMPT : "Mirror boss shattering, chibi hero facing reflection, self-conquest, clay render, mirror effect"

────────────────────────────────────────────────────────────────────────────────

ID : boss_three_phases_survived
NOM : Marathonien de Boss
RARETÉ : Épique
DESCRIPTION : Tu as survécu aux 3 phases d'un boss.
DÉBLOCAGE : Compléter boss avec 3 phases
CATÉGORIE : Boss
XP BONUS : 900
PROMPT : "Three phase icons glowing, chibi hero tired but victorious, endurance triumph, clay render, phases"

────────────────────────────────────────────────────────────────────────────────

ID : boss_dialogue_all_heard
NOM : Auditeur de Boss
RARETÉ : Rare
DESCRIPTION : Tu as écouté tous les dialogues d'un boss.
DÉBLOCAGE : Entendre tous les dialogues boss (ne pas skip)
CATÉGORIE : Boss
XP BONUS : 200
PROMPT : "Boss speech bubbles glowing, attentive chibi listener, respectful audience, clay render, storytelling"

────────────────────────────────────────────────────────────────────────────────

ID : boss_cinematic_watched
NOM : Cinéphile de Boss
RARETÉ : Commun
DESCRIPTION : Tu as regardé la cinématique d'un boss.
DÉBLOCAGE : Voir cinématique pre/post boss
CATÉGORIE : Boss
XP BONUS : 100
PROMPT : "Boss cinematic playing, chibi viewer engaged, epic movie moment, clay render, cinematic"

────────────────────────────────────────────────────────────────────────────────

ID : boss_retry_ten_times
NOM : Persévérant Obstiné
RARETÉ : Rare
DESCRIPTION : Tu as retried un boss 10 fois. La détermination incarne.
DÉBLOCAGE : Échouer et retry un boss 10 fois
CATÉGORIE : Boss
XP BONUS : 400
PROMPT : "Retry counter at 10, determined chibi hero, never give up attitude, clay render, persistence"

────────────────────────────────────────────────────────────────────────────────

ID : boss_first_try_victory
NOM : Première Tentative
RARETÉ : Légendaire
DESCRIPTION : Boss vaincu du premier coup. Génie ou chance ?
DÉBLOCAGE : Vaincre un boss en première tentative
CATÉGORIE : Boss
XP BONUS : 1500
PROMPT : "First try victory banner, astonished chibi prodigy, natural talent, clay render, golden first"

═══════════════════════════════════════════════════════════════════════════════
CATÉGORIE 7 — IA ADAPTATIVE & PROFIL COGNITIF (18 BADGES)
═══════════════════════════════════════════════════════════════════════════════

ID : profile_learning_selected
NOM : Conscience de Soi
RARETÉ : Commun
DESCRIPTION : Tu as choisi ton profil d'apprentissage.
DÉBLOCAGE : Sélectionner un profil cognitif
CATÉGORIE : IA
XP BONUS : 100
PROMPT : "Learning profile selection interface, chibi choosing self-knowledge path, clay render, introspective"

────────────────────────────────────────────────────────────────────────────────

ID : profile_changed_adapted
NOM : Évolution Cognitive
RARETÉ : Rare
DESCRIPTION : Tu as modifié ton profil pour mieux coller à ta réalité.
DÉBLOCAGE : Changer profil apprentissage dans Paramètres
CATÉGORIE : IA
XP BONUS : 200
PROMPT : "Profile transformation animation, chibi evolving, adaptive change, clay render, growth"

────────────────────────────────────────────────────────────────────────────────

ID : insight_first_received
NOM : Premier Insight
RARETÉ : Commun
DESCRIPTION : L'IA t'a donné ton premier insight pédagogique.
DÉBLOCAGE : Recevoir un insight UserAnalyticsEngine
CATÉGORIE : IA
XP BONUS : 150
PROMPT : "Light bulb insight appearing, chibi having realization, aha moment, clay render, bright idea"

────────────────────────────────────────────────────────────────────────────────

ID : insight_ten_collected
NOM : Collectionneur d'Insights
RARETÉ : Rare
DESCRIPTION : 10 insights pédagogiques reçus et compris.
DÉBLOCAGE : Recevoir 10 insights différents
CATÉGORIE : IA
XP BONUS : 400
PROMPT : "Ten insight gems floating, chibi collector of wisdom, intelligence gathering, clay render, gems"

────────────────────────────────────────────────────────────────────────────────

ID : recommendation_followed
NOM : Disciple de l'IA
RARETÉ : Commun
DESCRIPTION : Tu as suivi une recommandation IA.
DÉBLOCAGE : Effectuer action recommandée par RecommendationEngine
CATÉGORIE : IA
XP BONUS : 150
PROMPT : "IA recommendation arrow pointing, chibi following suggestion, guided action, clay render, smart path"

────────────────────────────────────────────────────────────────────────────────

ID : recommendation_ignored_all
NOM : Rebelle Libre
RARETÉ : WTF Rare
DESCRIPTION : Tu ignores systématiquement les recommandations IA. Anarchiste.
DÉBLOCAGE : Ignorer 10 recommandations IA consécutives
CATÉGORIE : IA
XP BONUS : 100
PROMPT : "Ignored recommendation icons fading, rebel chibi doing own thing, independent spirit, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : adaptive_difficulty_experienced
NOM : Témoin de l'Adaptation
RARETÉ : Rare
DESCRIPTION : Tu as ressenti l'adaptation de difficulté en temps réel.
DÉBLOCAGE : Quiz adaptatif ajuste difficulté (3+ changements)
CATÉGORIE : IA
XP BONUS : 300
PROMPT : "Difficulty meter adjusting dynamically, chibi experiencing adaptation, real-time change, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : maieutic_correction_received
NOM : Élève de Socrate
RARETÉ : Rare
DESCRIPTION : Une correction maïeutique t'a guidé vers la vérité.
DÉBLOCAGE : Recevoir correction maïeutique après erreur
CATÉGORIE : IA
XP BONUS : 250
PROMPT : "Socratic questioning bubbles, chibi student thinking deeply, guided discovery, clay render, philosophy"

────────────────────────────────────────────────────────────────────────────────

ID : mnemonic_ai_generated
NOM : Mnémoniste Assisté
RARETÉ : Commun
DESCRIPTION : L'IA t'a généré un moyen mnémotechnique après erreur.
DÉBLOCAGE : Recevoir mnémonique IA après erreur
CATÉGORIE : IA
XP BONUS : 100
PROMPT : "Mnemonic trick appearing, lightbulb moment, chibi memorizing easily, memory aid, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : cognitive_score_viewed
NOM : Connaissance du Cerveau
RARETÉ : Rare
DESCRIPTION : Tu as consulté tes scores cognitifs.
DÉBLOCAGE : Voir dashboard CognitiveProfile
CATÉGORIE : IA
XP BONUS : 200
PROMPT : "Brain cognitive stats displayed, chibi examining self-analysis, self-awareness, clay render, brain chart"

────────────────────────────────────────────────────────────────────────────────

ID : knowledge_graph_explored
NOM : Explorateur de Graphe
RARETÉ : Épique
DESCRIPTION : Tu as exploré le graphe de connaissances lié.
DÉBLOCAGE : Voir graphe KnowledgeNode avec liens
CATÉGORIE : IA
XP BONUS : 500
PROMPT : "Knowledge graph network visualization, chibi exploring connections, linked knowledge, clay render, web"

────────────────────────────────────────────────────────────────────────────────

ID : mastery_illusion_detected
NOM : Illusion Brisée
RARETÉ : Rare
DESCRIPTION : L'IA a détecté une illusion de maîtrise chez toi.
DÉBLOCAGE : IA détecte bonne réponse rapide mais instable (CognitiveEngine)
CATÉGORIE : IA
XP BONUS : 200
PROMPT : "Mirror cracking revealing truth, chibi realizing false confidence, wake-up call, clay render, revelation"

────────────────────────────────────────────────────────────────────────────────

ID : relapse_prevented
NOM : Rechute Évitée
RARETÉ : Épique
DESCRIPTION : L'IA a détecté une rechute potentielle et t'a aidé.
DÉBLOCAGE : IA détecte risque rechute et intervient
CATÉGORIE : IA
XP BONUS : 600
PROMPT : "Warning sign with safety net, chibi saved from fall, preventive intervention, clay render, rescue"

────────────────────────────────────────────────────────────────────────────────

ID : fatigue_detected_rest_suggested
NOM : Pause Sage
RARETÉ : Commun
DESCRIPTION : L'IA a détecté ta fatigue cognitive et suggéré une pause.
DÉBLOCAGE : IA détecte fatigue et recommande repos
CATÉGORIE : IA
XP BONUS : 100
PROMPT : "Rest icon glowing, tired chibi hero, caring IA suggestion, rest recommendation, clay render, break"

────────────────────────────────────────────────────────────────────────────────

ID : format_effectiveness_learned
NOM : Découverte de Style
RARETÉ : Épique
DESCRIPTION : L'IA a identifié ton format pédagogique optimal.
DÉBLOCAGE : LearningFormatEffectiveness identifie format préféré
CATÉGORIE : IA
XP BONUS : 700
PROMPT : "Optimal learning style highlighted, chibi understanding self better, discovery moment, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : cloud_snapshot_created
NOM : Sauvegarde Divine
RARETÉ : Rare
DESCRIPTION : Ton héros est sauvegardé dans les nuages de l'Olympe.
DÉBLOCAGE : Créer un cloud snapshot
CATÉGORIE : IA
XP BONUS : 300
PROMPT : "Hero data uploading to divine clouds, backup magic, chibi hero preserved, clay render, cloud tech"

────────────────────────────────────────────────────────────────────────────────

ID : cloud_restored_successfully
NOM : Résurrection Numérique
RARETÉ : Épique
DESCRIPTION : Ton héros a été restauré depuis les nuages.
DÉBLOCAGE : Restaurer héros depuis cloud snapshot
CATÉGORIE : IA
XP BONUS : 500
PROMPT : "Hero data downloading from clouds, restoration magic, chibi reborn, clay render, revival"

────────────────────────────────────────────────────────────────────────────────

ID : analytics_dashboard_obsessed
NOM : Data Addict
RARETÉ : WTF Rare
DESCRIPTION : Tu consultes tes analytics 50 fois par jour.
DÉBLOCAGE : Ouvrir dashboard analytics 50× en 24h
CATÉGORIE : IA
XP BONUS : 200
PROMPT : "Stats screens everywhere, obsessed chibi checking constantly, data addiction, clay render, overwhelmed"

═══════════════════════════════════════════════════════════════════════════════
CATÉGORIE 8 — EXTENSIONS DIVINES (APHRODITE, HERMÈS, APOLLON) (20 BADGES)
═══════════════════════════════════════════════════════════════════════════════

ID : aphrodite_first_drawing
NOM : Premier Dessin Divin
RARETÉ : Rare
DESCRIPTION : Aphrodite a dessiné ton premier savoir.
DÉBLOCAGE : Utiliser Aphrodite pour générer image d'un savoir
CATÉGORIE : Extensions Divines
XP BONUS : 300
PROMPT : "Aphrodite drawing magical artwork, chibi watching creation, artistic magic, clay render, art goddess"

────────────────────────────────────────────────────────────────────────────────

ID : aphrodite_gallery_ten
NOM : Galerie d'Aphrodite
RARETÉ : Épique
DESCRIPTION : 10 dessins divins dans ta collection.
DÉBLOCAGE : 10 images générées par Aphrodite
CATÉGORIE : Extensions Divines
XP BONUS : 700
PROMPT : "Art gallery with ten divine drawings, chibi curator, aesthetic collection, clay render, museum"

────────────────────────────────────────────────────────────────────────────────

ID : hermes_first_translation
NOM : Première Traduction
RARETÉ : Rare
DESCRIPTION : Hermès a traduit ton premier savoir.
DÉBLOCAGE : Utiliser Hermès pour traduire un résumé
CATÉGORIE : Extensions Divines
XP BONUS : 250
PROMPT : "Hermes translating scroll to different language, linguistic magic, chibi linguist, clay render, languages"

────────────────────────────────────────────────────────────────────────────────

ID : hermes_polyglot_five
NOM : Polyglotte Assisté
RARETÉ : Épique
DESCRIPTION : 5 langues différentes via Hermès.
DÉBLOCAGE : Traduire savoirs en 5 langues différentes
CATÉGORIE : Extensions Divines
XP BONUS : 600
PROMPT : "Five language flags floating, chibi polyglot, multilingual mastery, clay render, global languages"

────────────────────────────────────────────────────────────────────────────────

ID : apollon_poem_ten
NOM : Barde Olympien
RARETÉ : Épique
DESCRIPTION : 10 poèmes créés par la Lyre d'Apollon.
DÉBLOCAGE : 10 poèmes générés via Apollon
CATÉGORIE : Extensions Divines
XP BONUS : 700
PROMPT : "Ten glowing poems scrolls, chibi bard with lyre, poetic collection, clay render, golden poetry"

────────────────────────────────────────────────────────────────────────────────

ID : apollon_all_poem_styles
NOM : Maître des Styles
RARETÉ : Légendaire
DESCRIPTION : Tu as exploré tous les styles de la Lyre.
DÉBLOCAGE : Utiliser ode, sonnet, haïku (tous les styles Apollon)
CATÉGORIE : Extensions Divines
XP BONUS : 1000
PROMPT : "Multiple poem style scrolls displayed, chibi master poet, style mastery, clay render, varieties"

────────────────────────────────────────────────────────────────────────────────

ID : promethee_hint_fifty
NOM : Aidé de Prométhée
RARETÉ : Rare
DESCRIPTION : Prométhée t'a aidé 50 fois.
DÉBLOCAGE : 50 aides/conseils Prométhée
CATÉGORIE : Extensions Divines
XP BONUS : 400
PROMPT : "Promethee helping chibi student repeatedly, mentorship bond, helpful guidance, clay render, torch"

────────────────────────────────────────────────────────────────────────────────

ID : divine_action_combo_three
NOM : Combo Divin
RARETÉ : Épique
DESCRIPTION : Tu as combiné 3 dieux sur un même savoir.
DÉBLOCAGE : Utiliser 3 actions divines différentes sur même savoir (ex: Lyre + Dessin + Traduction)
CATÉGORIE : Extensions Divines
XP BONUS : 800
PROMPT : "Three gods combining powers, chibi conductor, divine synergy, clay render, triple power"

────────────────────────────────────────────────────────────────────────────────

ID : divine_export_all
NOM : Exportateur Divin
RARETÉ : Légendaire
DESCRIPTION : Tu as exporté tous les types de créations divines.
DÉBLOCAGE : Exporter résumé, poème, image, traduction (tous)
CATÉGORIE : Extensions Divines
XP BONUS : 1200
PROMPT : "Multiple file types exporting, chibi exporter master, complete export, clay render, file icons"

────────────────────────────────────────────────────────────────────────────────

ID : divine_suggestion_auto
NOM : Guidance Divine
RARETÉ : Rare
DESCRIPTION : L'IA t'a suggéré le bon dieu pour ton blocage.
DÉBLOCAGE : IA suggère dieu selon contexte et tu l'utilises
CATÉGORIE : Extensions Divines
XP BONUS : 250
PROMPT : "IA arrow pointing to correct god, chibi following divine guidance, smart suggestion, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : divine_quota_hit_all
NOM : Épuisement du Panthéon
RARETÉ : WTF Épique
DESCRIPTION : Tu as épuisé TOUS les dieux en une journée. Respect.
DÉBLOCAGE : Atteindre quota de tous les dieux IA en 24h
CATÉGORIE : Extensions Divines
XP BONUS : 666
PROMPT : "All gods sleeping exhausted, chibi user apologetic, complete divine fatigue, clay render, tired gods"

────────────────────────────────────────────────────────────────────────────────

ID : aphrodite_style_variation
NOM : Variations Artistiques
RARETÉ : Rare
DESCRIPTION : Tu as demandé différents styles à Aphrodite.
DÉBLOCAGE : 3 styles artistiques différents via Aphrodite
CATÉGORIE : Extensions Divines
XP BONUS : 300
PROMPT : "Different art styles displayed, chibi art director, style experimentation, clay render, artistic"

────────────────────────────────────────────────────────────────────────────────

ID : apollon_tts_poem
NOM : Récitateur Divin
RARETÉ : Rare
DESCRIPTION : Tu as écouté un poème d'Apollon lu par TTS.
DÉBLOCAGE : TTS sur poème Apollon
CATÉGORIE : Extensions Divines
XP BONUS : 150
PROMPT : "Sound waves from glowing poem, chibi listening to poetry reading, audio magic, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : hermes_same_content_three_languages
NOM : Traducteur Obsessionnel
RARETÉ : WTF Rare
DESCRIPTION : Le même savoir traduit en 3 langues différentes.
DÉBLOCAGE : Traduire même savoir en 3 langues
CATÉGORIE : Extensions Divines
XP BONUS : 200
PROMPT : "Same content in three languages side by side, obsessed chibi translator, linguistic redundancy, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : divine_history_rich
NOM : Historique Divin Riche
RARETÉ : Légendaire
DESCRIPTION : Un savoir a 10 interactions divines différentes.
DÉBLOCAGE : Un savoir avec 10 artefacts générés
CATÉGORIE : Extensions Divines
XP BONUS : 1500
PROMPT : "Single knowledge surrounded by ten divine artifacts, chibi proud, ultimate enhancement, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : aphrodite_shared_artwork
NOM : Partage Artistique
RARETÉ : Rare
DESCRIPTION : Tu as partagé une œuvre d'Aphrodite.
DÉBLOCAGE : Exporter/partager image Aphrodite
CATÉGORIE : Extensions Divines
XP BONUS : 200
PROMPT : "Artwork being shared with glow effect, chibi sharing beauty, artistic spread, clay render, social"

────────────────────────────────────────────────────────────────────────────────

ID : apollon_poem_perfect_quiz
NOM : Harmonie Poétique
RARETÉ : Épique
DESCRIPTION : Quiz 100% après avoir étudié via poème Apollon.
DÉBLOCAGE : 100% quiz sur savoir transformé en poème
CATÉGORIE : Extensions Divines
XP BONUS : 800
PROMPT : "Perfect score with poem background, chibi poet victorious, poetic mastery, clay render, harmony"

────────────────────────────────────────────────────────────────────────────────

ID : divine_cost_calculated
NOM : Comptable du Divin
RARETÉ : Rare
DESCRIPTION : Tu connais le coût exact de chaque invocation divine.
DÉBLOCAGE : Consulter coûts/quotas divins dans interface
CATÉGORIE : Extensions Divines
XP BONUS : 150
PROMPT : "Divine cost calculator displayed, chibi accountant, resource management, clay render, calculations"

────────────────────────────────────────────────────────────────────────────────

ID : promethee_easter_egg
NOM : Secret de Prométhée
RARETÉ : WTF Légendaire
DESCRIPTION : Tu as découvert le secret caché de Prométhée.
DÉBLOCAGE : Easter egg Prométhée (à définir)
CATÉGORIE : Extensions Divines
XP BONUS : 1000
PROMPT : "Hidden torch revealing secret, chibi discoverer shocked, easter egg reveal, clay render, mystery"

────────────────────────────────────────────────────────────────────────────────

ID : divine_all_gods_used_once
NOM : Cercle Complet
RARETÉ : Épique
DESCRIPTION : Tu as utilisé chaque extension divine au moins une fois.
DÉBLOCAGE : Aphrodite, Hermès, Apollon, Prométhée utilisés (au moins 1×)
CATÉGORIE : Extensions Divines
XP BONUS : 600
PROMPT : "All divine gods forming circle, complete pantheon activation, chibi center, clay render, unity"

═══════════════════════════════════════════════════════════════════════════════
CATÉGORIE 9 — MONDE & LORE (15 BADGES)
═══════════════════════════════════════════════════════════════════════════════

ID : weather_divine_rain_seen
NOM : Témoin de la Pluie Divine
RARETÉ : Commun
DESCRIPTION : Tu as vu la pluie divine sur le Dashboard.
DÉBLOCAGE : Voir météo pluie (Dashboard)
CATÉGORIE : Monde
XP BONUS : 100
PROMPT : "Divine rain falling on dashboard, chibi watching weather magic, mythical meteorology, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : weather_divine_storm_seen
NOM : Témoin de l'Orage Divin
RARETÉ : Rare
DESCRIPTION : L'orage gronde sur l'Olympe par ta négligence.
DÉBLOCAGE : Voir météo orage (savoirs fanés critiques)
CATÉGORIE : Monde
XP BONUS : 150
PROMPT : "Divine storm brewing over temples, worried chibi, dramatic weather, clay render, ominous dark"

────────────────────────────────────────────────────────────────────────────────

ID : weather_divine_sunshine_seen
NOM : Témoin du Soleil Divin
RARETÉ : Rare
DESCRIPTION : Le soleil radieux bénit ton Dashboard.
DÉBLOCAGE : Voir météo soleil (progression excellente)
CATÉGORIE : Monde
XP BONUS : 200
PROMPT : "Radiant divine sunshine, happy dashboard, chibi blessed, perfect weather, clay render, golden"

────────────────────────────────────────────────────────────────────────────────

ID : statue_evolution_witnessed
NOM : Témoin de la Statue
RARETÉ : Rare
DESCRIPTION : Une statue a évolué devant tes yeux.
DÉBLOCAGE : Voir évolution statue Dashboard
CATÉGORIE : Monde
XP BONUS : 250
PROMPT : "Statue transforming with golden particles, chibi witnessing evolution, monument change, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : agora_message_read_ten
NOM : Lecteur de l'Agora
RARETÉ : Rare
DESCRIPTION : Tu as lu 10 messages de l'Agora.
DÉBLOCAGE : Lire 10 messages Agora (Dashboard)
CATÉGORIE : Monde
XP BONUS : 200
PROMPT : "Agora message board with chibi reader, community notices, social hub, clay render, public square"

────────────────────────────────────────────────────────────────────────────────

ID : agora_message_divine_event
NOM : Annonce Divine
RARETÉ : Épique
DESCRIPTION : Tu as reçu une annonce d'événement divin via l'Agora.
DÉBLOCAGE : Recevoir message événement spécial Agora
CATÉGORIE : Monde
XP BONUS : 400
PROMPT : "Special divine announcement glowing, chibi excited, event reveal, clay render, trumpet fanfare"

────────────────────────────────────────────────────────────────────────────────

ID : flame_streak_active
NOM : Flambeau Vivant
RARETÉ : Rare
DESCRIPTION : Le flambeau de streak brûle intensément sur ton Dashboard.
DÉBLOCAGE : Voir flambeau streak actif (7+ jours)
CATÉGORIE : Monde
XP BONUS : 300
PROMPT : "Intense streak flame burning, chibi maintaining fire, dedication flame, clay render, bright fire"

────────────────────────────────────────────────────────────────────────────────

ID : dashboard_god_visit
NOM : Visite Divine sur Dashboard
RARETÉ : Épique
DESCRIPTION : Un dieu est venu te rendre visite sur ton Dashboard.
DÉBLOCAGE : Recevoir visite contextuelle d'un dieu sur Dashboard
CATÉGORIE : Monde
XP BONUS : 500
PROMPT : "God visiting dashboard, chibi honored, divine house call, clay render, special visit"

────────────────────────────────────────────────────────────────────────────────

ID : dashboard_reconstruction_progress
NOM : Architecte Visuel
RARETÉ : Épique
DESCRIPTION : La reconstruction globale est visible sur ton Dashboard.
DÉBLOCAGE : Voir progression reconstruction visuelle Dashboard (50%+)
CATÉGORIE : Monde
XP BONUS : 700
PROMPT : "Dashboard showing visible reconstruction progress, chibi architect proud, world building, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : bestiaire_first_entry
NOM : Premier Catalogueur
RARETÉ : Commun
DESCRIPTION : Tu as débloqué ta première entrée de bestiaire.
DÉBLOCAGE : Débloquer première créature dans bestiaire
CATÉGORIE : Monde
XP BONUS : 150
PROMPT : "Bestiary book opening with first creature, chibi cataloguer, discovery moment, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : bestiaire_fifty_percent
NOM : Chasseur de Créatures
RARETÉ : Épique
DESCRIPTION : 50% du bestiaire complété.
DÉBLOCAGE : 50% créatures débloquées
CATÉGORIE : Monde
XP BONUS : 800
PROMPT : "Half-complete bestiary glowing, chibi monster hunter, collection progress, clay render, creatures"

────────────────────────────────────────────────────────────────────────────────

ID : bestiaire_complete
NOM : Maître du Bestiaire
RARETÉ : Mythique
DESCRIPTION : Toutes les créatures mythologiques cataloguées.
DÉBLOCAGE : 100% bestiaire
CATÉGORIE : Monde
XP BONUS : 3000
PROMPT : "Complete bestiary radiating power, all creatures displayed, chibi master cataloguer, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : compagnon_hatched
NOM : Naissance du Compagnon
RARETÉ : Épique
DESCRIPTION : Ton compagnon chimérique a éclos.
DÉBLOCAGE : Éclosion œuf compagnon
CATÉGORIE : Monde
XP BONUS : 700
PROMPT : "Egg hatching with magical glow, baby companion emerging, chibi parent joyful, clay render, birth"

────────────────────────────────────────────────────────────────────────────────

ID : compagnon_evolution_first
NOM : Évolution du Compagnon
RARETÉ : Épique
DESCRIPTION : Ton compagnon a évolué vers sa forme supérieure.
DÉBLOCAGE : Première évolution compagnon (stade 2)
CATÉGORIE : Monde
XP BONUS : 800
PROMPT : "Companion evolving with light particles, chibi watching evolution, growth moment, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : compagnon_max_level
NOM : Compagnon Ultime
RARETÉ : Légendaire
DESCRIPTION : Ton compagnon a atteint sa forme finale.
DÉBLOCAGE : Compagnon stade 3 (final)
CATÉGORIE : Monde
XP BONUS : 2000
PROMPT : "Fully evolved powerful companion, chibi with ultimate partner, max form, clay render, majestic"

════════════════════════════════════════════════════════════════════════════════
CATÉGORIE 10 — WTF ABSOLU & SECRETS (30 BADGES)
═══════════════════════════════════════════════════════════════════════════════

ID : wtf_chicken_cosmic_found
NOM : Poulet Cosmique Suprême
RARETÉ : WTF Mythique
DESCRIPTION : Tu as trouvé le légendaire Poulet Cosmique. Pourquoi.
DÉBLOCAGE : Easter egg ultime (tap séquence secrète)
CATÉGORIE : WTF
XP BONUS : 2000
PROMPT : "Majestic cosmic chicken wearing crown floating in space, absurd glory, clay render, epic ridiculous"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_666_quiz
NOM : Nombre de la Bête Éducative
RARETÉ : WTF Épique
DESCRIPTION : Tu as fait exactement 666 quiz. Satanique mais studieux.
DÉBLOCAGE : 666 quiz complétés exactement
CATÉGORIE : WTF
XP BONUS : 666
PROMPT : "666 counter glowing ominously, chibi with devilish aura but studying, dark humor, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_4h20_session
NOM : 420 Olympien
RARETÉ : WTF Rare
DESCRIPTION : Session à 4h20 du matin. La vraie folie.
DÉBLOCAGE : Session active à 4:20 AM précisément
CATÉGORIE : WTF
XP BONUS : 420
PROMPT : "420 clock display, extremely tired chibi still studying, absurd dedication, clay render, exhausted"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_palindrome_score
NOM : Palindrome Parfait
RARETÉ : WTF Rare
DESCRIPTION : Score palindrome exact (ex: 65756, 14741).
DÉBLOCAGE : Score total XP palindrome
CATÉGORIE : WTF
XP BONUS : 200
PROMPT : "Palindrome numbers glowing, chibi mathematician amused, numerical symmetry, clay render, math magic"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_zero_score_quiz
NOM : Anti-Perfection
RARETÉ : WTF Rare
DESCRIPTION : 0% à un quiz. Talent inverse.
DÉBLOCAGE : Score 0% (toutes mauvaises réponses)
CATÉGORIE : WTF
XP BONUS : 100
PROMPT : "Zero score with ironic trophy, chibi failing spectacularly, reverse achievement, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_same_wrong_answer_ten
NOM : Persévérance dans l'Erreur
RARETÉ : WTF Épique
DESCRIPTION : Même mauvaise réponse 10 fois de suite. Impressionnant.
DÉBLOCAGE : Choisir même mauvaise réponse 10×
CATÉGORIE : WTF
XP BONUS : 300
PROMPT : "Same wrong answer repeated, stubborn chibi refusing to learn, comedic obstinacy, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_hundred_screenshots
NOM : Photographe Obsessionnel
RARETÉ : WTF Épique
DESCRIPTION : 100 screenshots de résultats. La mémoire externe.
DÉBLOCAGE : Prendre 100 screenshots résultats
CATÉGORIE : WTF
XP BONUS : 400
PROMPT : "Camera flashing repeatedly, chibi photographer obsessed, screenshot overload, clay render, flash"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_oracle_midnight_full_moon
NOM : Rituel Lunaire
RARETÉ : WTF Légendaire
DESCRIPTION : Scanner à minuit pile pendant une pleine lune. Sorcier.
DÉBLOCAGE : Scanner document à 00:00 pendant pleine lune (date système)
CATÉGORIE : WTF
XP BONUS : 1000
PROMPT : "Full moon ritual scene, mystical scanning, witchy chibi, lunar magic, clay render, moonlight"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_shake_rage_fifty
NOM : Séisme Répété
RARETÉ : WTF Rare
DESCRIPTION : 50 secousses de rage. Gestion de la colère : 0.
DÉBLOCAGE : Secouer appareil 50 fois sur écrans défaite
CATÉGORIE : WTF
XP BONUS : 200
PROMPT : "Phone shaking violently repeatedly, angry chibi, rage expression, clay render, earthquake lines"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_empty_temple_year
NOM : Ruine Éternelle Absolue
RARETÉ : WTF Mythique
DESCRIPTION : Un temple niveau 0 pendant 365 jours. Abandon total.
DÉBLOCAGE : Temple 0 pendant 1 an
CATÉGORIE : WTF
XP BONUS : 365
PROMPT : "Completely abandoned temple with spiderwebs, dust, sad deity, ultimate neglect, clay render, decay"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_all_volume_combinations
NOM : DJ Olympien
RARETÉ : WTF Rare
DESCRIPTION : Tu as testé tous les volumes possibles. Sound engineer.
DÉBLOCAGE : Changer volume 50 fois dans paramètres
CATÉGORIE : WTF
XP BONUS : 150
PROMPT : "Volume sliders everywhere, chibi sound engineer, audio obsession, clay render, sound controls"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_settings_opened_hundred
NOM : Paramètres Addict
RARETÉ : WTF Rare
DESCRIPTION : Paramètres ouverts 100 fois en une semaine.
DÉBLOCAGE : Ouvrir Settings 100× en 7 jours
CATÉGORIE : WTF
XP BONUS : 200
PROMPT : "Settings icon spinning endlessly, obsessed chibi tweaking, configuration madness, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_inventory_empty_refill_ten
NOM : Cycle de Consommation
RARETÉ : WTF Épique
DESCRIPTION : Vider et remplir inventaire 10 fois.
DÉBLOCAGE : Inventaire 0 puis rempli 10 cycles
CATÉGORIE : WTF
XP BONUS : 500
PROMPT : "Empty-full inventory cycle animation, chibi consumer, endless loop, clay render, cycle"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_craft_spam_button
NOM : Cliqueur Compulsif
RARETÉ : WTF Commun
DESCRIPTION : 100 clics sur forge sans ressources.
DÉBLOCAGE : Cliquer forge 100× sans fragments
CATÉGORIE : WTF
XP BONUS : 50
PROMPT : "Forge button getting hammered, frustrated chibi clicking, compulsive behavior, clay render, spam"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_quiz_quit_fifty
NOM : Abandon Systématique
RARETÉ : WTF Rare
DESCRIPTION : Quitter 50 quiz avant la fin. Commitment issues.
DÉBLOCAGE : Abandonner 50 quiz mid-way
CATÉGORIE : WTF
XP BONUS : 100
PROMPT : "Quit button pressed repeatedly, chibi unable to commit, abandonment issues, clay render, escape"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_login_logout_twenty
NOM : Indécis Chronique
RARETÉ : WTF Rare
DESCRIPTION : Login/logout 20 fois en 1 heure.
DÉBLOCAGE : Cycles connexion 20× en 1h
CATÉGORIE : WTF
XP BONUS : 150
PROMPT : "Login-logout cycle animation, confused chibi, indecision comedy, clay render, revolving door"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_name_changed_ten
NOM : Crise d'Identité
RARETÉ : WTF Rare
DESCRIPTION : Changer pseudo 10 fois.
DÉBLOCAGE : Modifier pseudo 10×
CATÉGORIE : WTF
XP BONUS : 200
PROMPT : "Multiple name tags floating, identity crisis chibi, name confusion, clay render, identity"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_avatar_changed_fifty
NOM : Métamorphe Compulsif
RARETÉ : WTF Épique
DESCRIPTION : Changer d'avatar 50 fois.
DÉBLOCAGE : Changer avatar 50×
CATÉGORIE : WTF
XP BONUS : 400
PROMPT : "Rapidly changing avatars, shapeshifting chibi, identity fluidity, clay render, transformation"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_perfect_score_asleep
NOM : Génie Endormi
RARETÉ : WTF Légendaire
DESCRIPTION : 100% quiz avec 30 min de pause milieu (AFK détecté).
DÉBLOCAGE : Quiz parfait avec longue pause mid-quiz
CATÉGORIE : WTF
XP BONUS : 1000
PROMPT : "Sleeping chibi with perfect score, accidental genius, sleepy victory, clay render, zzz symbols"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_konami_code
NOM : Code Konami Olympien
RARETÉ : WTF Mythique
DESCRIPTION : Le code ancestral fonctionne même ici.
DÉBLOCAGE : Entrer séquence Konami dans Dashboard
CATÉGORIE : WTF
XP BONUS : 1337
PROMPT : "Retro game code activating, nostalgic chibi gamer, easter egg glory, clay render, 8-bit style"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_backwards_navigation
NOM : Explorateur Inverse
RARETÉ : WTF Rare
DESCRIPTION : Naviguer à reculons 50 fois (back button spam).
DÉBLOCAGE : Appuyer back 50× en 5 minutes
CATÉGORIE : WTF
XP BONUS : 150
PROMPT : "Reverse arrows everywhere, confused chibi going backwards, navigation chaos, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_loading_tap_thousand
NOM : Impatience Légendaire
RARETÉ : WTF Légendaire
DESCRIPTION : 1000 taps pendant chargements IA.
DÉBLOCAGE : 1000 clics pendant loaders
CATÉGORIE : WTF
XP BONUS : 500
PROMPT : "Loading screen with finger tap marks, extremely impatient chibi, tap frenzy, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_mute_unmute_loop
NOM : DJ du Silence
RARETÉ : WTF Commun
DESCRIPTION : Mute/unmute 30 fois en 2 minutes.
DÉBLOCAGE : Toggle mute 30×
CATÉGORIE : WTF
XP BONUS : 100
PROMPT : "Mute button toggling rapidly, confused chibi with sound, audio chaos, clay render, speaker icon"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_notification_spam_disable
NOM : Anti-Notif Rage
RARETÉ : WTF Rare
DESCRIPTION : Désactiver toutes les notifications possibles.
DÉBLOCAGE : Désactiver tous types notifs
CATÉGORIE : WTF
XP BONUS : 150
PROMPT : "All notification icons crossed out, anti-social chibi, silence preference, clay render, no bells"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_language_changed_ten
NOM : Polyglotte Accidentel
RARETÉ : WTF Rare
DESCRIPTION : Changer langue app 10 fois.
DÉBLOCAGE : Changer langue settings 10×
CATÉGORIE : WTF
XP BONUS : 200
PROMPT : "Multiple language flags cycling, confused multilingual chibi, language confusion, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_theme_dark_light_spam
NOM : Rave Visuel
RARETÉ : WTF Commun
DESCRIPTION : Alterner thème clair/sombre 20 fois.
DÉBLOCAGE : Toggle theme 20×
CATÉGORIE : WTF
XP BONUS : 100
PROMPT : "Rapid light-dark theme switching, strobing effect, chibi rave, visual chaos, clay render, flash"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_help_button_never
NOM : Auto-Didacte Têtu
RARETÉ : WTF Rare
DESCRIPTION : Ne jamais utiliser bouton aide en 6 mois.
DÉBLOCAGE : 180 jours sans aide/tutoriel
CATÉGORIE : WTF
XP BONUS : 300
PROMPT : "Help button dusty and ignored, independent chibi, stubborn self-teaching, clay render, alone"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_perfect_birthday
NOM : Anniversaire Parfait
RARETÉ : WTF Légendaire
DESCRIPTION : Quiz 100% le jour de ton anniversaire à minuit pile.
DÉBLOCAGE : Perfect quiz à 00:00 de la date anniversaire profil
CATÉGORIE : WTF
XP BONUS : 2000
PROMPT : "Birthday cake with perfect score, chibi celebrating perfectly, magical birthday, clay render, party"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_no_badge_check_year
NOM : Humble Olympien
RARETÉ : WTF Légendaire
DESCRIPTION : Ne jamais consulter badges pendant 1 an.
DÉBLOCAGE : 365 jours sans ouvrir BadgeActivity
CATÉGORIE : WTF
XP BONUS : 1000
PROMPT : "Badge book covered in dust, humble chibi not caring, pure learning focus, clay render, dusty"

────────────────────────────────────────────────────────────────────────────────

ID : wtf_ultimate_secret
NOM : ???
RARETÉ : WTF Mythique
DESCRIPTION : ???
DÉBLOCAGE : [SECRET - À DÉFINIR PAR DÉVELOPPEUR]
CATÉGORIE : WTF
XP BONUS : 9999
PROMPT : "Ultimate mystery box glowing impossibly, chibi touching the unknowable, reality break, clay render"

═══════════════════════════════════════════════════════════════════════════════
CATÉGORIE 11 — PERFORMANCE & EXPLOIT (15 BADGES)
═══════════════════════════════════════════════════════════════════════════════

ID : speed_ten_quiz_hour
NOM : Tornade de Quiz
RARETÉ : Épique
DESCRIPTION : 10 quiz complétés en moins d'1 heure.
DÉBLOCAGE : 10 quiz en <60 minutes
CATÉGORIE : Performance
XP BONUS : 700
PROMPT : "Speed counter spinning, exhausted chibi speedrunner, rapid fire, clay render, fast motion"

────────────────────────────────────────────────────────────────────────────────

ID : speed_perfect_under_five
NOM : Perfection Éclair
RARETÉ : Légendaire
DESCRIPTION : Quiz 100% en moins de 5 minutes.
DÉBLOCAGE : Perfect quiz <5 min
CATÉGORIE : Performance
XP BONUS : 1500
PROMPT : "Lightning fast perfect score, chibi speedster, ultimate efficiency, clay render, speed lines"

────────────────────────────────────────────────────────────────────────────────

ID : endurance_twenty_quiz_session
NOM : Marathonien Mental
RARETÉ : Légendaire
DESCRIPTION : 20 quiz dans une seule session.
DÉBLOCAGE : 20 quiz en 1 session
CATÉGORIE : Performance
XP BONUS : 2000
PROMPT : "Exhausted chibi hero surrounded by quiz scrolls, mental marathon, ultimate endurance, clay render"

────────────────────────────────────────────────────────────────────────────────

ID : consistency_thirty_days_perfect
NOM : Perfection Soutenue
RARETÉ : Mythique
DESCRIPTION : Au moins un quiz parfait pendant 30 jours consécutifs.
DÉBLOCAGE : 1 perfect quiz par jour × 30 jours
CATÉGORIE : Performance
XP BONUS : 5000
PROMPT : "30-day perfect calendar glowing, chibi consistency master, sustained excellence, clay render, gold"

────────────────────────────────────────────────────────────────────────────────

ID : accuracy_thousand_correct
NOM : Millier de Vérités
RARETÉ : Légendaire
DESCRIPTION : 1000 réponses correctes cumulées.
DÉBLOCAGE : 1000 bonnes réponses total
CATÉGORIE : Performance
XP BONUS : 1500
PROMPT : "Thousand correct checkmarks floating, chibi accuracy master, truth collector, clay render, checks"

────────────────────────────────────────────────────────────────────────────────

ID : recovery_ten_comebacks
NOM : Phénix Répété
RARETÉ : Épique
DESCRIPTION : 10 retours après absence de 7+ jours.
DÉBLOCAGE : 10 comebacks (7+ jours absence)
CATÉGORIE : Performance
XP BONUS : 800
PROMPT : "Phoenix rising repeatedly, chibi comeback expert, resilience symbol, clay render, resurrection"

────────────────────────────────────────────────────────────────────────────────

ID : improvement_score_double
NOM : Progression Explosive
RARETÉ : Épique
DESCRIPTION : Doubler ton score sur même type de quiz.
DÉBLOCAGE : Quiz même matière : score × 2 comparé à précédent
CATÉGORIE : Performance
XP BONUS : 600
PROMPT : "Score graph doubling, chibi improving dramatically, growth explosion, clay render, upward arrow"

────────────────────────────────────────────────────────────────────────────────

ID : clutch_victory_last_second
NOM : Victoire de Dernière Seconde
RARETÉ : Légendaire
DESCRIPTION : Bonne réponse dans la dernière seconde du timer.
DÉBLOCAGE : Répondre correct à t<1s restant
CATÉGORIE : Performance
XP BONUS : 1000
PROMPT : "Clock at 0.9s with correct answer, clutch chibi hero, last-second save, clay render, dramatic"

────────────────────────────────────────────────────────────────────────────────

ID : versatility_all_subjects_week
NOM : Érudit Universel
RARETÉ : Légendaire
DESCRIPTION : Au moins 1 quiz dans chaque matière en 1 semaine.
DÉBLOCAGE : 9 matières couvertes en 7 jours
CATÉGORIE : Performance
XP BONUS : 1500
PROMPT : "All subject icons glowing, versatile chibi scholar, universal knowledge, clay render, rainbow"

────────────────────────────────────────────────────────────────────────────────

ID : mastery_five_hundred_total
NOM : Demi-Millénaire de Savoirs
RARETÉ : Mythique
DESCRIPTION : 500 quiz complétés au total.
DÉBLOCAGE : 500 quiz total
CATÉGORIE : Performance
XP BONUS : 3000
PROMPT : "500 quiz trophy massive, chibi quiz veteran, ultimate achievement, clay render, golden 500"

────────────────────────────────────────────────────────────────────────────────

ID : perfectionist_ten_perfect_row
NOM : Série Parfaite
RARETÉ : Légendaire
DESCRIPTION : 10 quiz parfaits consécutifs.
DÉBLOCAGE : 10 perfect quiz d'affilée
CATÉGORIE : Performance
XP BONUS : 2000
PROMPT : "Ten perfect stars in a row, chibi perfectionist, ultimate consistency, clay render, perfect line"

────────────────────────────────────────────────────────────────────────────────

ID : difficulty_mastery_all_levels
NOM : Conquérant de Difficulté
RARETÉ : Légendaire
DESCRIPTION : Perfect quiz à tous les niveaux de difficulté.
DÉBLOCAGE : 100% quiz sur chaque niveau difficulté (1-10)
CATÉGORIE : Performance
XP BONUS : 1800
PROMPT : "Difficulty pyramid conquered, chibi at summit, all levels mastered, clay render, pyramid gold"

────────────────────────────────────────────────────────────────────────────────

ID : time_saver_hundred_hours
NOM : Centurion du Savoir
RARETÉ : Mythique
DESCRIPTION : 100 heures de temps de jeu cumulées.
DÉBLOCAGE : 100h total play time
CATÉGORIE : Performance
XP BONUS : 5000
PROMPT : "100-hour clock monument, dedicated chibi veteran, time investment, clay render, time memorial"

────────────────────────────────────────────────────────────────────────────────

ID : xp_gain_million_single_session
NOM : Explosion d'XP
RARETÉ : Mythique
DESCRIPTION : Gagner 10000 XP en une session.
DÉBLOCAGE : +10000 XP en 1 session
CATÉGORIE : Performance
XP BONUS : 2000
PROMPT : "XP explosion effect, chibi overwhelmed by gains, massive growth, clay render, XP shower"

────────────────────────────────────────────────────────────────────────────────

ID : level_hundred_reached
NOM : Centurion Divin
RARETÉ : Mythique
DESCRIPTION : Niveau 100 atteint. Tu es devenu légende.
DÉBLOCAGE : Atteindre niveau 100
CATÉGORIE : Performance
XP BONUS : 10000
PROMPT : "Level 100 monument glowing, legendary chibi hero, divine centurion, clay render, epic glory"

═══════════════════════════════════════════════════════════════════════════════
CATÉGORIE 12 — SOCIAL & FUTUR (10 BADGES)
═══════════════════════════════════════════════════════════════════════════════

ID : social_first_share
NOM : Premier Partage
RARETÉ : Commun
DESCRIPTION : Tu as partagé ton premier résultat.
DÉBLOCAGE : Partager un résultat
CATÉGORIE : Social
XP BONUS : 100
PROMPT : "Share icon glowing, chibi sharing achievement, social moment, clay render, sharing"

────────────────────────────────────────────────────────────────────────────────

ID : social_qr_code_generated
NOM : Code Divin
RARETÉ : Rare
DESCRIPTION : Tu as généré un QR code de profil.
DÉBLOCAGE : Générer QR code héros
CATÉGORIE : Social
XP BONUS : 150
PROMPT : "QR code glowing with divine energy, chibi tech-savvy, digital sharing, clay render, QR tech"

────────────────────────────────────────────────────────────────────────────────

ID : social_parent_summary_enabled
NOM : Rapport Parental
RARETÉ : Rare
DESCRIPTION : Tu as activé les rapports parentaux.
DÉBLOCAGE : Activer résumés parents
CATÉGORIE : Social
XP BONUS : 200
PROMPT : "Parent report document glowing, responsible chibi, family connection, clay render, report"

────────────────────────────────────────────────────────────────────────────────

ID : social_certificate_generated
NOM : Certificat Officiel
RARETÉ : Épique
DESCRIPTION : Tu as généré un certificat de progression.
DÉBLOCAGE : Générer certificat
CATÉGORIE : Social
XP BONUS : 400
PROMPT : "Official certificate with seals, proud chibi graduate, achievement document, clay render, diploma"

────────────────────────────────────────────────────────────────────────────────

ID : social_friend_challenge_sent
NOM : Défi Lancé
RARETÉ : Rare
DESCRIPTION : Tu as lancé un défi à un ami (futur).
DÉBLOCAGE : Envoyer défi ami
CATÉGORIE : Social
XP BONUS : 250
PROMPT : "Challenge invitation glowing, chibi challenger, competitive spirit, clay render, duel"

────────────────────────────────────────────────────────────────────────────────

ID : social_guild_joined
NOM : Membre de Guilde
RARETÉ : Épique
DESCRIPTION : Tu as rejoint une guilde d'apprentissage (futur).
DÉBLOCAGE : Rejoindre guilde
CATÉGORIE : Social
XP BONUS : 500
PROMPT : "Guild banner raised, chibi joining team, community bond, clay render, guild emblem"

────────────────────────────────────────────────────────────────────────────────

ID : social_leaderboard_top_ten
NOM : Top 10 Olympien
RARETÉ : Légendaire
DESCRIPTION : Classé dans le top 10 d'un leaderboard (futur).
DÉBLOCAGE : Top 10 leaderboard
CATÉGORIE : Social
XP BONUS : 1500
PROMPT : "Leaderboard with chibi in top 10, competitive glory, ranking achievement, clay render, podium"

────────────────────────────────────────────────────────────────────────────────

ID : social_multiplayer_victory
NOM : Victoire Coopérative
RARETÉ : Épique
DESCRIPTION : Victoire en mode coopératif (futur).
DÉBLOCAGE : Win coop challenge
CATÉGORIE : Social
XP BONUS : 700
PROMPT : "Two chibi heroes celebrating together, teamwork victory, coop triumph, clay render, friendship"

────────────────────────────────────────────────────────────────────────────────

ID : social_exported_to_class
NOM : Partageur Pédagogique
RARETÉ : Légendaire
DESCRIPTION : Tu as exporté des contenus pour ta classe (futur).
DÉBLOCAGE : Export mode classe/prof
CATÉGORIE : Social
XP BONUS : 1000
PROMPT : "Classroom export interface, chibi teacher sharing, educational spread, clay render, teaching"

────────────────────────────────────────────────────────────────────────────────

ID : social_api_connected
NOM : Développeur Divin
RARETÉ : Mythique
DESCRIPTION : Tu as connecté l'API RéviZeus (futur développeurs).
DÉBLOCAGE : API connection established
CATÉGORIE : Social
XP BONUS : 2000
PROMPT : "API connection diagram glowing, developer chibi, integration magic, clay render, tech code"

═══════════════════════════════════════════════════════════════════════════════
CONCLUSION — RÉCAPITULATIF DES 300 BADGES
═══════════════════════════════════════════════════════════════════════════════

TOTAL : 300 BADGES
- 102 badges DÉJÀ CODÉS (référencés dans préambule)
- 198 badges NOUVEAUX (détaillés dans ce document)

RÉPARTITION PAR CATÉGORIE :
1. Temples Évolutifs : 30 badges
2. Oracle 3 Portes & Validation : 25 badges
3. Inventaire & Économie : 20 badges
4. Jardin de Déméter : 15 badges
5. Quêtes & Aventure : 30 badges
6. Boss Éducatifs : 20 badges
7. IA Adaptative & Profil : 18 badges
8. Extensions Divines : 20 badges
9. Monde & Lore : 15 badges
10. WTF Absolu & Secrets : 30 badges
11. Performance & Exploit : 15 badges
12. Social & Futur : 10 badges

RÉPARTITION PAR RARETÉ (nouveaux badges) :
- Commun : 55 badges
- Rare : 68 badges
- Épique : 52 badges
- Légendaire : 35 badges
- Mythique : 18 badges
- WTF : 30 badges

IMPLÉMENTATION RECOMMANDÉE :
Les badges doivent être implémentés progressivement selon les WAVES :
- WAVE 1-2 : 30 badges (fondations + UX)
- WAVE 3 : 50 badges (progression)
- WAVE 4 : 35 badges (immersion)
- WAVE 5 : 28 badges (IA adaptative)
- WAVE 6 : 55 badges (aventure + WTF)

SYSTÈME TECHNIQUE :
- Utiliser BadgeEvalContext enrichi avec nouvelles stats
- Ajouter nouveaux triggers dans BadgeManager
- Créer drawables pour chaque badge (prompt Nano Banana fourni)
- XP bonus total possible : ~185,000 XP
- Badges secrets (WTF) : 30 (10% du total)

BADGES "VIVANTS" :
Certains badges évoluent avec le jeu :
- Badges à paliers (bronze/argent/or)
- Badges déblocables uniquement par progression future
- Badges secrets découvrables
- Badges événements temporaires

═══════════════════════════════════════════════════════════════════════════════
FIN DU DOCUMENT
═══════════════════════════════════════════════════════════════════════════════

Document créé le : 2026-03-24
Version : MASTER BADGES 300 CONSOLIDATED
Badges totaux : 300 (102 existants + 198 nouveaux)
Statut : READY FOR IMPLEMENTATION

Prochaine étape recommandée :
1. Créer les drawables badges (via prompts Nano Banana fournis)
2. Implémenter badges WAVE 1 (30 premiers)
3. Tester système déverrouillage
4. Itérer sur WAVES suivantes

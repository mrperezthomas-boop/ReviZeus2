package com.revizeus.app

/**
 * ═══════════════════════════════════════════════════════════════
 * DIALOG CONTEXT — Contextes métier pour sélection du dieu
 * ═══════════════════════════════════════════════════════════════
 * 
 * BLOC B — DIALOGUES RPG UNIVERSELS
 * 
 * Utilité :
 * - Définit les différents contextes métier de l'application.
 * - Permet à DialogRPGManager.selectGodForContext() de choisir
 *   automatiquement le dieu le plus approprié pour incarner
 *   un message selon son contexte fonctionnel.
 * 
 * Exemples d'usage :
 * 
 * // Sélection automatique du dieu
 * val godId = DialogRPGManager.selectGodForContext(DialogContext.PEDAGOGY)
 * // Retourne "athena"
 * 
 * val godId = DialogRPGManager.selectGodForContext(DialogContext.NETWORK_ERROR)
 * // Retourne "hermes"
 * 
 * Mapping contexte → dieu :
 * 
 * AUTHORITY, MAJOR_DECISION → Zeus
 * - Décisions importantes, limites système, arbitrage
 * 
 * PEDAGOGY, EXPLANATION → Athéna
 * - Explications pédagogiques, corrections, validations
 * 
 * NATURE, SVT → Poséidon
 * - Sciences naturelles, cycles, systèmes vivants
 * 
 * CHALLENGE, DIFFICULTY → Arès
 * - Défis, difficulté accrue, dépassement de soi
 * 
 * POETRY, INSPIRATION → Apollon
 * - Poésie, musique, harmonie, inspiration artistique
 * 
 * CRAFTING, MECHANISM → Héphaïstos
 * - Forge, fabrication, craft, mécanismes
 * 
 * MEMORY, SPACED_REPETITION → Déméter
 * - Mémoire à long terme, répétition espacée, soin
 * 
 * LANGUAGES, SPEED, NETWORK_ERROR → Hermès
 * - Langues, traduction, rapidité, connexion réseau
 * 
 * AESTHETICS, VISUALIZATION → Aphrodite
 * - Création visuelle, beauté, dessin, esthétique
 * 
 * HELP, TUTORIAL → Prométhée
 * - Aide système, tutoriels, astuces, guidage UX
 * 
 * Évolutions futures :
 * - MYSTERY → Hadès (énigmes, secrets, inconnu)
 * - CELEBRATION → Dionysos (fêtes, succès collectifs)
 * - PROPHECY → Apollon (prédictions, insights futurs)
 * - JUSTICE → Thémis (équité, règles, jugements)
 * 
 * ═══════════════════════════════════════════════════════════════
 */
enum class DialogContext {
    // ── ZEUS : Autorité & Décisions Majeures ──
    /**
     * Autorité divine, commandement, règles du système.
     * Dieu : Zeus
     */
    AUTHORITY,
    
    /**
     * Décision majeure, choix irréversible, confirmation importante.
     * Dieu : Zeus
     */
    MAJOR_DECISION,
    
    // ── ATHÉNA : Pédagogie & Explications ──
    /**
     * Explications pédagogiques, enseignement, guidage d'apprentissage.
     * Dieu : Athéna
     */
    PEDAGOGY,
    
    /**
     * Explication technique, clarification, correction.
     * Dieu : Athéna
     */
    EXPLANATION,
    
    // ── POSÉIDON : Nature & Sciences ──
    /**
     * Thèmes liés à la nature, l'eau, les cycles naturels.
     * Dieu : Poséidon
     */
    NATURE,
    
    /**
     * Sciences de la vie et de la terre (SVT).
     * Dieu : Poséidon
     */
    SVT,
    
    // ── ARÈS : Défis & Difficultés ──
    /**
     * Défi lancé, épreuve à surmonter, combat intellectuel.
     * Dieu : Arès
     */
    CHALLENGE,
    
    /**
     * Difficulté accrue, niveau élevé, dépassement de soi.
     * Dieu : Arès
     */
    DIFFICULTY,
    
    // ── APOLLON : Poésie & Inspiration ──
    /**
     * Poésie, littérature, expression artistique textuelle.
     * Dieu : Apollon
     */
    POETRY,
    
    /**
     * Inspiration créative, harmonie, beauté sonore.
     * Dieu : Apollon
     */
    INSPIRATION,
    
    // ── HÉPHAÏSTOS : Craft & Mécanismes ──
    /**
     * Forge, fabrication, creation d'objets, craft.
     * Dieu : Héphaïstos
     */
    CRAFTING,
    
    /**
     * Mécanismes techniques, fonctionnement interne, engineering.
     * Dieu : Héphaïstos
     */
    MECHANISM,
    
    // ── DÉMÉTER : Mémoire & Croissance ──
    /**
     * Mémoire à long terme, consolidation des savoirs.
     * Dieu : Déméter
     */
    MEMORY,
    
    /**
     * Répétition espacée, révision cyclique, croissance progressive.
     * Dieu : Déméter
     */
    SPACED_REPETITION,
    
    // ── HERMÈS : Langues & Rapidité ──
    /**
     * Langues étrangères, traduction, communication interculturelle.
     * Dieu : Hermès
     */
    LANGUAGES,
    
    /**
     * Rapidité, vitesse, efficacité, time-pressure.
     * Dieu : Hermès
     */
    SPEED,
    
    /**
     * Erreur réseau, connexion, transmission de données.
     * Dieu : Hermès (messager des dieux, responsable des connexions)
     */
    NETWORK_ERROR,
    
    // ── APHRODITE : Esthétique & Visualisation ──
    /**
     * Esthétique, beauté visuelle, design, harmonie graphique.
     * Dieu : Aphrodite
     */
    AESTHETICS,
    
    /**
     * Visualisation, création graphique, dessin, illustration.
     * Dieu : Aphrodite
     */
    VISUALIZATION,
    
    // ── PROMÉTHÉE : Aide & Tutoriels ──
    /**
     * Aide système, assistance UX, support utilisateur.
     * Dieu : Prométhée (bienfaiteur de l'humanité)
     */
    HELP,
    
    /**
     * Tutoriel, guide pas-à-pas, explication d'interface.
     * Dieu : Prométhée
     */
    TUTORIAL;
    
    /**
     * Retourne l'ID du dieu associé à ce contexte.
     * Utilisé par DialogRPGManager.selectGodForContext().
     */
    fun getGodId(): String {
        return when (this) {
            AUTHORITY, MAJOR_DECISION -> "zeus"
            PEDAGOGY, EXPLANATION -> "athena"
            NATURE, SVT -> "poseidon"
            CHALLENGE, DIFFICULTY -> "ares"
            POETRY, INSPIRATION -> "apollo"
            CRAFTING, MECHANISM -> "hephaestus"
            MEMORY, SPACED_REPETITION -> "demeter"
            LANGUAGES, SPEED, NETWORK_ERROR -> "hermes"
            AESTHETICS, VISUALIZATION -> "aphrodite"
            HELP, TUTORIAL -> "prometheus"
        }
    }
}

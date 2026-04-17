package com.revizeus.app

/**
 * ═══════════════════════════════════════════════════════════════
 * DIALOG CATEGORY — Catégories visuelles des dialogues RPG
 * ═══════════════════════════════════════════════════════════════
 * 
 * BLOC B — DIALOGUES RPG UNIVERSELS
 * 
 * Utilité :
 * - Définit les différentes catégories de dialogues RPG, chacune
 *   avec son identité visuelle propre (couleurs, fond, ambiance).
 * - Utilisée par DialogRPGFragment pour appliquer le style visuel
 *   approprié selon le contexte métier.
 * 
 * Catégories disponibles :
 * 
 * INFO : Dialogue informatif neutre (par défaut)
 * - Couleur : Or standard (#FFD700)
 * - Fond : bg_rpg_dialog standard
 * - Usage : Informations générales, confirmations simples
 * 
 * PEDAGOGY : Dialogue pédagogique Athéna
 * - Couleur : Or pâle (#F0E68C)
 * - Fond : Fond clair avec accent pédagogique
 * - Usage : Explications, corrections, guidage d'apprentissage
 * 
 * ALERT : Avertissement important
 * - Couleur : Orange/Rouge (#FF8C00)
 * - Fond : Fond chaud avec accent d'alerte
 * - Usage : Avertissements, actions risquées, limites approchées
 * 
 * ERROR_TECHNICAL : Erreur technique diégétique
 * - Couleur : Rouge sombre (#DC143C)
 * - Fond : Fond sombre avec lightning
 * - Usage : Erreurs réseau, permissions refusées, API timeout
 * 
 * REWARD : Récompense / Succès
 * - Couleur : Or brillant (#FFD700 + éclat)
 * - Fond : Fond doré lumineux
 * - Usage : Badges débloqués, niveaux gagnés, succès accomplis
 * 
 * CONFIRMATION : Confirmation importante
 * - Couleur : Or solennel (#DAA520)
 * - Fond : Fond solennel avec accent de gravité
 * - Usage : Suppressions, choix irréversibles, décisions majeures
 * 
 * DIVINE_FATIGUE : Fatigue divine / Quota dépassé
 * - Couleur : Pastel fatigué (#B0C4DE)
 * - Fond : Fond doux avec accent de repos
 * - Usage : Quota API dépassé, limite quotidienne atteinte
 * 
 * HELP : Aide / Tutorial Prométhée
 * - Couleur : Vert doux (#90EE90)
 * - Fond : Fond vert calme avec accent d'aide
 * - Usage : Tutoriels, astuces, explications système
 * 
 * Évolutions futures :
 * - CHALLENGE : Défi Arès (rouge intense, fond de combat)
 * - INSPIRATION : Inspiration Apollon (or-rose, fond harmonieux)
 * - MYSTERY : Mystère Hadès (violet sombre, fond énigmatique)
 * - CELEBRATION : Célébration collective (multicolore, fond festif)
 * 
 * ═══════════════════════════════════════════════════════════════
 */
enum class DialogCategory {
    /**
     * Dialogue informatif neutre.
     * Couleur : Or standard (#FFD700)
     * Usage : Informations générales, confirmations simples
     */
    INFO,
    
    /**
     * Dialogue pédagogique Athéna.
     * Couleur : Or pâle (#F0E68C)
     * Usage : Explications, corrections, guidage d'apprentissage
     */
    PEDAGOGY,
    
    /**
     * Avertissement important.
     * Couleur : Orange/Rouge (#FF8C00)
     * Usage : Avertissements, actions risquées, limites approchées
     */
    ALERT,
    
    /**
     * Erreur technique rendue diégétique.
     * Couleur : Rouge sombre (#DC143C)
     * Usage : Erreurs réseau, permissions refusées, API timeout
     */
    ERROR_TECHNICAL,
    
    /**
     * Récompense ou succès.
     * Couleur : Or brillant (#FFD700 + éclat)
     * Usage : Badges débloqués, niveaux gagnés, succès accomplis
     */
    REWARD,
    
    /**
     * Confirmation importante / décision majeure.
     * Couleur : Or solennel (#DAA520)
     * Usage : Suppressions, choix irréversibles, décisions majeures
     */
    CONFIRMATION,
    
    /**
     * Fatigue divine / quota API dépassé.
     * Couleur : Pastel fatigué (#B0C4DE)
     * Usage : Quota API dépassé, limite quotidienne atteinte
     */
    DIVINE_FATIGUE,
    
    /**
     * Aide / Tutorial Prométhée.
     * Couleur : Vert doux (#90EE90)
     * Usage : Tutoriels, astuces, explications système
     */
    HELP;
    
    /**
     * Retourne la couleur d'accent principale associée à cette catégorie.
     * Utilisée pour les titres, boutons principaux, accents visuels.
     */
    fun getAccentColor(): String {
        return when (this) {
            INFO -> "#FFD700"           // Or standard
            PEDAGOGY -> "#F0E68C"       // Or pâle
            ALERT -> "#FF8C00"          // Orange vif
            ERROR_TECHNICAL -> "#DC143C" // Rouge crimson
            REWARD -> "#FFD700"         // Or brillant (même que INFO, mais avec éclat)
            CONFIRMATION -> "#DAA520"   // Or goldenrod solennel
            DIVINE_FATIGUE -> "#B0C4DE" // Bleu acier clair
            HELP -> "#90EE90"           // Vert clair
        }
    }
    
    /**
     * Retourne la couleur de fond secondaire (pour la zone narrative).
     * Utilisée pour la bulle de dialogue du dieu.
     */
    fun getSecondaryBgColor(): String {
        return when (this) {
            INFO -> "#1AFFFFFF"         // Blanc très transparent
            PEDAGOGY -> "#1AF0E68C"     // Or pâle transparent
            ALERT -> "#1AFF8C00"        // Orange transparent
            ERROR_TECHNICAL -> "#1ADC143C" // Rouge transparent
            REWARD -> "#26FFD700"       // Or légèrement opaque (plus visible)
            CONFIRMATION -> "#1ADAA520" // Or goldenrod transparent
            DIVINE_FATIGUE -> "#1AB0C4DE" // Bleu acier transparent
            HELP -> "#1A90EE90"         // Vert transparent
        }
    }
    
    /**
     * Retourne true si cette catégorie doit afficher des particules
     * ou des effets visuels spéciaux (évolution future).
     */
    fun hasSpecialEffects(): Boolean {
        return when (this) {
            REWARD -> true      // Particules dorées brillantes
            ALERT -> true       // Éclair d'alerte
            ERROR_TECHNICAL -> true // Lightning sombre
            else -> false
        }
    }
}

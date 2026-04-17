package com.revizeus.app

import android.content.Context
import android.content.SharedPreferences

/**
 * ============================================================
 * SettingsManager.kt — RéviZeus v9
 * Gestion centralisée des réglages utilisateur via SharedPreferences
 * ============================================================
 *
 * // CHANTIER 0
 * Ce manager centralise la lecture / écriture de tous les réglages
 * de l'application afin d'éviter la dispersion de la logique de
 * persistance dans les Activities.
 *
 * Règles respectées :
 * - aucune logique métier lourde dans la vue
 * - persistance immédiate en SharedPreferences
 * - architecture existante conservée
 * ============================================================
 */
class SettingsManager(context: Context) {

    // CHANTIER 0 — On utilise l'applicationContext pour éviter toute fuite d'Activity.
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "revizeus_settings"

        // ============================================================
        // CLÉS SHARED PREFERENCES
        // ============================================================

        // Audio
        private const val KEY_VOLUME_MUSIQUE = "volume_musique"
        private const val KEY_VOLUME_SFX = "volume_sfx"
        private const val KEY_VOLUME_DIALOGUE = "volume_dialogue"
        private const val KEY_MUET_GENERAL = "muet_general"
        private const val KEY_QUALITE_AUDIO = "qualite_audio"

        // Affichage
        private const val KEY_TAILLE_TEXTE = "taille_texte"
        private const val KEY_VITESSE_DIALOGUE = "vitesse_dialogue"

        // Accessibilité
        private const val KEY_MODE_DALTONIEN = "mode_daltonien"
        private const val KEY_TYPE_DALTONISME = "type_daltonisme"
        private const val KEY_CONTRASTE_ELEVE = "contraste_eleve"
        private const val KEY_REDUIRE_FLASHS = "reduire_flashs"
        private const val KEY_ANIMATIONS_DESACTIVEES = "animations_desactivees"

        // Notifications
        private const val KEY_HEURE_RAPPEL = "heure_rappel"
        private const val KEY_HEURE_RAPPEL_HEURE = "heure_rappel_heure"
        private const val KEY_HEURE_RAPPEL_MINUTE = "heure_rappel_minute"
        private const val KEY_NOTIF_QUOTIDIEN = "notif_quotidien"
        private const val KEY_NOTIF_QCM = "notif_qcm"
        private const val KEY_NOTIF_RECOMPENSES = "notif_recompenses"
        private const val KEY_NOTIF_STREAK = "notif_streak"

        // Gameplay
        private const val KEY_DIFFICULTE_QUIZ = "difficulte_quiz"
        private const val KEY_NB_QUESTIONS_QUIZ = "nb_questions_quiz"
        private const val KEY_TEMPS_LIMITE_QUESTION = "temps_limite_question"
        private const val KEY_AFFICHER_CORRECTIONS = "afficher_corrections"
        private const val KEY_REVISION_ESPACEE_ACTIVE = "revision_espacee_active"
        private const val KEY_HAPTIQUE_ACTIF = "haptique_actif"

        // Avancé
        private const val KEY_LANGUE = "langue"
        private const val KEY_THEME_VISUEL = "theme_visuel"
        private const val KEY_MODE_HORS_LIGNE = "mode_hors_ligne"

        // ============================================================
        // VALEURS PAR DÉFAUT
        // ============================================================

        const val DEFAULT_VOLUME_MUSIQUE = 70
        const val DEFAULT_VOLUME_SFX = 80
        const val DEFAULT_VOLUME_DIALOGUE = 75
        const val DEFAULT_MUET_GENERAL = false
        const val DEFAULT_QUALITE_AUDIO = "Élevée"

        const val DEFAULT_TAILLE_TEXTE = 40
        const val DEFAULT_VITESSE_DIALOGUE = 50

        const val DEFAULT_MODE_DALTONIEN = false
        const val DEFAULT_TYPE_DALTONISME = "Protanopie"
        const val DEFAULT_CONTRASTE_ELEVE = false
        const val DEFAULT_REDUIRE_FLASHS = false
        const val DEFAULT_ANIMATIONS_DESACTIVEES = false

        const val DEFAULT_HEURE_RAPPEL = "18h00"
        const val DEFAULT_HEURE_RAPPEL_HEURE = 18
        const val DEFAULT_HEURE_RAPPEL_MINUTE = 0
        const val DEFAULT_NOTIF_QUOTIDIEN = true
        const val DEFAULT_NOTIF_QCM = true
        const val DEFAULT_NOTIF_RECOMPENSES = true
        const val DEFAULT_NOTIF_STREAK = true

        const val DEFAULT_DIFFICULTE = "Normal"
        const val DEFAULT_NB_QUESTIONS = 10
        const val DEFAULT_TEMPS_LIMITE = 20
        const val DEFAULT_AFFICHER_CORRECTIONS = true
        const val DEFAULT_REVISION_ESPACEE_ACTIVE = true
        const val DEFAULT_HAPTIQUE_ACTIF = true

        const val DEFAULT_LANGUE = "Français"
        const val DEFAULT_THEME_VISUEL = "Olympe"
        const val DEFAULT_MODE_HORS_LIGNE = false

        // ============================================================
        // OPTIONS PUBLIQUES POUR LES SPINNERS
        // ============================================================

        val QUALITE_AUDIO_OPTIONS = listOf(
            "Faible",
            "Normale",
            "Élevée"
        )

        val DALTONISME_OPTIONS = listOf(
            "Protanopie",
            "Deutéranopie",
            "Tritanopie"
        )

        val DIFFICULTE_OPTIONS = listOf(
            "Facile",
            "Normal",
            "Difficile",
            "Divin"
        )

        val NB_QUESTIONS_OPTIONS = listOf(
            "5",
            "10",
            "15",
            "20"
        )

        val TEMPS_LIMITE_OPTIONS = listOf(
            "10 sec",
            "20 sec",
            "30 sec",
            "45 sec",
            "60 sec"
        )

        val TEMPS_LIMITE_VALEURS = listOf(
            10,
            20,
            30,
            45,
            60
        )

        val LANGUE_OPTIONS = listOf(
            "Français",
            "English",
            "Español"
        )

        val THEME_OPTIONS = listOf(
            "Olympe",
            "Nuit",
            "Temple"
        )
    }

    // ============================================================
    // AUDIO
    // ============================================================

    var volumeMusique: Int
        get() = prefs.getInt(KEY_VOLUME_MUSIQUE, DEFAULT_VOLUME_MUSIQUE)
        set(value) = prefs.edit().putInt(KEY_VOLUME_MUSIQUE, value.coerceIn(0, 100)).apply()

    var volumeSfx: Int
        get() = prefs.getInt(KEY_VOLUME_SFX, DEFAULT_VOLUME_SFX)
        set(value) = prefs.edit().putInt(KEY_VOLUME_SFX, value.coerceIn(0, 100)).apply()

    var volumeDialogue: Int
        get() = prefs.getInt(KEY_VOLUME_DIALOGUE, DEFAULT_VOLUME_DIALOGUE)
        set(value) = prefs.edit().putInt(KEY_VOLUME_DIALOGUE, value.coerceIn(0, 100)).apply()

    var muetGeneral: Boolean
        get() = prefs.getBoolean(KEY_MUET_GENERAL, DEFAULT_MUET_GENERAL)
        set(value) = prefs.edit().putBoolean(KEY_MUET_GENERAL, value).apply()

    var qualiteAudio: String
        get() = prefs.getString(KEY_QUALITE_AUDIO, DEFAULT_QUALITE_AUDIO) ?: DEFAULT_QUALITE_AUDIO
        set(value) = prefs.edit().putString(KEY_QUALITE_AUDIO, value).apply()

    // ============================================================
    // AFFICHAGE
    // ============================================================

    var tailleTexte: Int
        get() = prefs.getInt(KEY_TAILLE_TEXTE, DEFAULT_TAILLE_TEXTE)
        set(value) = prefs.edit().putInt(KEY_TAILLE_TEXTE, value.coerceIn(0, 100)).apply()

    var vitesseDialogue: Int
        get() = prefs.getInt(KEY_VITESSE_DIALOGUE, DEFAULT_VITESSE_DIALOGUE)
        set(value) = prefs.edit().putInt(KEY_VITESSE_DIALOGUE, value.coerceIn(0, 100)).apply()

    // ============================================================
    // ACCESSIBILITÉ
    // ============================================================

    var modeDaltonien: Boolean
        get() = prefs.getBoolean(KEY_MODE_DALTONIEN, DEFAULT_MODE_DALTONIEN)
        set(value) = prefs.edit().putBoolean(KEY_MODE_DALTONIEN, value).apply()

    var typeDaltonisme: String
        get() = prefs.getString(KEY_TYPE_DALTONISME, DEFAULT_TYPE_DALTONISME) ?: DEFAULT_TYPE_DALTONISME
        set(value) = prefs.edit().putString(KEY_TYPE_DALTONISME, value).apply()

    var contrasteEleve: Boolean
        get() = prefs.getBoolean(KEY_CONTRASTE_ELEVE, DEFAULT_CONTRASTE_ELEVE)
        set(value) = prefs.edit().putBoolean(KEY_CONTRASTE_ELEVE, value).apply()

    var reduireFlashs: Boolean
        get() = prefs.getBoolean(KEY_REDUIRE_FLASHS, DEFAULT_REDUIRE_FLASHS)
        set(value) = prefs.edit().putBoolean(KEY_REDUIRE_FLASHS, value).apply()

    var animationsDesactivees: Boolean
        get() = prefs.getBoolean(KEY_ANIMATIONS_DESACTIVEES, DEFAULT_ANIMATIONS_DESACTIVEES)
        set(value) = prefs.edit().putBoolean(KEY_ANIMATIONS_DESACTIVEES, value).apply()

    // ============================================================
    // NOTIFICATIONS
    // ============================================================

    var heureRappel: String
        get() = prefs.getString(KEY_HEURE_RAPPEL, DEFAULT_HEURE_RAPPEL) ?: DEFAULT_HEURE_RAPPEL
        set(value) = prefs.edit().putString(KEY_HEURE_RAPPEL, value).apply()

    var heureRappelHeure: Int
        get() = prefs.getInt(KEY_HEURE_RAPPEL_HEURE, DEFAULT_HEURE_RAPPEL_HEURE)
        set(value) = prefs.edit().putInt(KEY_HEURE_RAPPEL_HEURE, value.coerceIn(0, 23)).apply()

    var heureRappelMinute: Int
        get() = prefs.getInt(KEY_HEURE_RAPPEL_MINUTE, DEFAULT_HEURE_RAPPEL_MINUTE)
        set(value) = prefs.edit().putInt(KEY_HEURE_RAPPEL_MINUTE, value.coerceIn(0, 59)).apply()

    var notifQuotidien: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_QUOTIDIEN, DEFAULT_NOTIF_QUOTIDIEN)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_QUOTIDIEN, value).apply()

    var notifQcm: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_QCM, DEFAULT_NOTIF_QCM)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_QCM, value).apply()

    var notifRecompenses: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_RECOMPENSES, DEFAULT_NOTIF_RECOMPENSES)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_RECOMPENSES, value).apply()

    var notifStreak: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_STREAK, DEFAULT_NOTIF_STREAK)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_STREAK, value).apply()

    // ============================================================
    // GAMEPLAY
    // ============================================================

    var difficulteQuiz: String
        get() = prefs.getString(KEY_DIFFICULTE_QUIZ, DEFAULT_DIFFICULTE) ?: DEFAULT_DIFFICULTE
        set(value) = prefs.edit().putString(KEY_DIFFICULTE_QUIZ, value).apply()

    var nbQuestionsQuiz: Int
        get() = prefs.getInt(KEY_NB_QUESTIONS_QUIZ, DEFAULT_NB_QUESTIONS)
        set(value) = prefs.edit().putInt(KEY_NB_QUESTIONS_QUIZ, value).apply()

    var tempsLimiteQuestion: Int
        get() = prefs.getInt(KEY_TEMPS_LIMITE_QUESTION, DEFAULT_TEMPS_LIMITE)
        set(value) = prefs.edit().putInt(KEY_TEMPS_LIMITE_QUESTION, value).apply()

    var afficherCorrections: Boolean
        get() = prefs.getBoolean(KEY_AFFICHER_CORRECTIONS, DEFAULT_AFFICHER_CORRECTIONS)
        set(value) = prefs.edit().putBoolean(KEY_AFFICHER_CORRECTIONS, value).apply()

    var revisionEspaceeActive: Boolean
        get() = prefs.getBoolean(KEY_REVISION_ESPACEE_ACTIVE, DEFAULT_REVISION_ESPACEE_ACTIVE)
        set(value) = prefs.edit().putBoolean(KEY_REVISION_ESPACEE_ACTIVE, value).apply()

    var haptiqueActif: Boolean
        get() = prefs.getBoolean(KEY_HAPTIQUE_ACTIF, DEFAULT_HAPTIQUE_ACTIF)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIQUE_ACTIF, value).apply()

    // ============================================================
    // AVANCÉ
    // ============================================================

    var langue: String
        get() = prefs.getString(KEY_LANGUE, DEFAULT_LANGUE) ?: DEFAULT_LANGUE
        set(value) = prefs.edit().putString(KEY_LANGUE, value).apply()

    var themeVisuel: String
        get() = prefs.getString(KEY_THEME_VISUEL, DEFAULT_THEME_VISUEL) ?: DEFAULT_THEME_VISUEL
        set(value) = prefs.edit().putString(KEY_THEME_VISUEL, value).apply()

    var modeHorsLigne: Boolean
        get() = prefs.getBoolean(KEY_MODE_HORS_LIGNE, DEFAULT_MODE_HORS_LIGNE)
        set(value) = prefs.edit().putBoolean(KEY_MODE_HORS_LIGNE, value).apply()

    // ============================================================
    // HELPERS
    // ============================================================

    fun getVolumeMusiquef(): Float {
        return volumeMusique / 100f
    }

    // ============================================================
    // RESETS PAR SECTION
    // ============================================================

    fun resetAudio() {
        prefs.edit()
            .putInt(KEY_VOLUME_MUSIQUE, DEFAULT_VOLUME_MUSIQUE)
            .putInt(KEY_VOLUME_SFX, DEFAULT_VOLUME_SFX)
            .putInt(KEY_VOLUME_DIALOGUE, DEFAULT_VOLUME_DIALOGUE)
            .putBoolean(KEY_MUET_GENERAL, DEFAULT_MUET_GENERAL)
            .putString(KEY_QUALITE_AUDIO, DEFAULT_QUALITE_AUDIO)
            .apply()
    }

    fun resetAffichage() {
        prefs.edit()
            .putInt(KEY_TAILLE_TEXTE, DEFAULT_TAILLE_TEXTE)
            .putInt(KEY_VITESSE_DIALOGUE, DEFAULT_VITESSE_DIALOGUE)
            .apply()
    }

    fun resetAccessibilite() {
        prefs.edit()
            .putBoolean(KEY_MODE_DALTONIEN, DEFAULT_MODE_DALTONIEN)
            .putString(KEY_TYPE_DALTONISME, DEFAULT_TYPE_DALTONISME)
            .putBoolean(KEY_CONTRASTE_ELEVE, DEFAULT_CONTRASTE_ELEVE)
            .putBoolean(KEY_REDUIRE_FLASHS, DEFAULT_REDUIRE_FLASHS)
            .putBoolean(KEY_ANIMATIONS_DESACTIVEES, DEFAULT_ANIMATIONS_DESACTIVEES)
            .apply()
    }

    fun resetNotifications() {
        prefs.edit()
            .putString(KEY_HEURE_RAPPEL, DEFAULT_HEURE_RAPPEL)
            .putInt(KEY_HEURE_RAPPEL_HEURE, DEFAULT_HEURE_RAPPEL_HEURE)
            .putInt(KEY_HEURE_RAPPEL_MINUTE, DEFAULT_HEURE_RAPPEL_MINUTE)
            .putBoolean(KEY_NOTIF_QUOTIDIEN, DEFAULT_NOTIF_QUOTIDIEN)
            .putBoolean(KEY_NOTIF_QCM, DEFAULT_NOTIF_QCM)
            .putBoolean(KEY_NOTIF_RECOMPENSES, DEFAULT_NOTIF_RECOMPENSES)
            .putBoolean(KEY_NOTIF_STREAK, DEFAULT_NOTIF_STREAK)
            .apply()
    }

    fun resetGameplay() {
        prefs.edit()
            .putString(KEY_DIFFICULTE_QUIZ, DEFAULT_DIFFICULTE)
            .putInt(KEY_NB_QUESTIONS_QUIZ, DEFAULT_NB_QUESTIONS)
            .putInt(KEY_TEMPS_LIMITE_QUESTION, DEFAULT_TEMPS_LIMITE)
            .putBoolean(KEY_AFFICHER_CORRECTIONS, DEFAULT_AFFICHER_CORRECTIONS)
            .putBoolean(KEY_REVISION_ESPACEE_ACTIVE, DEFAULT_REVISION_ESPACEE_ACTIVE)
            .putBoolean(KEY_HAPTIQUE_ACTIF, DEFAULT_HAPTIQUE_ACTIF)
            .apply()
    }

    fun resetAvance() {
        prefs.edit()
            .putString(KEY_LANGUE, DEFAULT_LANGUE)
            .putString(KEY_THEME_VISUEL, DEFAULT_THEME_VISUEL)
            .putBoolean(KEY_MODE_HORS_LIGNE, DEFAULT_MODE_HORS_LIGNE)
            .apply()
    }
}
package com.revizeus.app

// [2026-04-19 05:36][BLOC_B2][PERSONA_MANAGER] Wrapper officiel B2 au-dessus de GodPersonalityEngine et PantheonConfig.
object DivinePersonaManager {

    // [2026-04-19 05:36][BLOC_B2][PERSONA_MANAGER] Accès stable à la config normalisée d'une persona.
    fun getConfig(personaType: DivinePersonaType): DivinePersonaConfig {
        return when (personaType) {
            DivinePersonaType.ORACLE_NEUTRE -> DivinePersonaConfig(
                personaType = DivinePersonaType.ORACLE_NEUTRE,
                displayName = "Oracle",
                coreTone = "neutre, clair, utile, non théâtral",
                teachingStyle = "guide sobrement, clarifie sans surjouer",
                correctionStyle = "corrige proprement sans surcharge narrative",
                encouragementStyle = "encourage avec sobriété et précision",
                warmthBias = 0.55f,
                disciplineBias = 0.55f,
                maieuticBias = 0.45f,
                verbosityBias = 0.40f,
                preferredActionTypes = setOf(
                    DivineActionType.SYSTEM_HELP,
                    DivineActionType.SUMMARY_REFORMULATION,
                    DivineActionType.ERROR_EXPLANATION,
                    DivineActionType.LOADING_MESSAGE
                )
            )

            else -> buildConfigFromExistingEngine(personaType)
        }
    }

    // [2026-04-19 05:36][BLOC_B2][PERSONA_MANAGER] Résolution matière -> persona sans créer une nouvelle source de vérité.
    fun getPersonaForSubject(subject: String): DivinePersonaType {
        if (subject.isBlank()) return DivinePersonaType.ORACLE_NEUTRE

        return try {
            PantheonConfig.getPersonaTypeForSubject(subject)
        } catch (_: Exception) {
            DivinePersonaType.ORACLE_NEUTRE
        }
    }

    // [2026-04-19 05:36][BLOC_B2][PERSONA_MANAGER] Hints prompt compacts dérivés de la source persona réelle.
    fun getPersonaPromptHints(
        personaType: DivinePersonaType,
        actionType: DivineActionType
    ): String {
        val config = getConfig(personaType)

        val actionBias = when (actionType) {
            DivineActionType.SUMMARY_GENERATION ->
                "Priorise une reformulation fidèle, structurée et directement révisable."

            DivineActionType.SUMMARY_REFORMULATION ->
                "Reformule plus clairement sans dériver du sens validé."

            DivineActionType.QUIZ_GENERATION ->
                "Produit des questions pédagogiques, nettes et adaptées au niveau."

            DivineActionType.QUIZ_CORRECTION ->
                "Explique l'erreur ou la réussite avec une logique brève et exploitable."

            DivineActionType.MNEMONIC ->
                "Condense l'idée en formule mémorisable, concise et scolaire."

            DivineActionType.DIVINE_VERDICT ->
                "Donne un verdict juste, incarné, sans écraser l'élève."

            DivineActionType.ENCOURAGEMENT ->
                "Encourage avec cohérence de ton, sans flatterie vide."

            DivineActionType.ERROR_EXPLANATION ->
                "Explique l'erreur technique ou pédagogique de façon calme et claire."

            DivineActionType.SYSTEM_HELP ->
                "Reste très utile, concret, lisible en contexte système."

            DivineActionType.LOADING_MESSAGE ->
                "Reste extrêmement court, respirant, compatible avec attente UI."

            DivineActionType.TEMPLE_GUIDANCE ->
                "Parle comme un guide de progression lié au temple et à l'avancée."

            DivineActionType.TRANSLATION ->
                "Traduis fidèlement sans perdre la clarté pédagogique."

            DivineActionType.DIVINE_SUGGESTION ->
                "Propose une prochaine action pertinente, simple à exécuter."
        }

        return buildString {
            append("Persona: ${config.displayName}. ")
            append("Ton coeur: ${config.coreTone}. ")
            append("Style pédagogique: ${config.teachingStyle}. ")
            append("Style de correction: ${config.correctionStyle}. ")
            append("Style d'encouragement: ${config.encouragementStyle}. ")
            append("Warmth=${config.warmthBias}, Discipline=${config.disciplineBias}, Maieutic=${config.maieuticBias}, Verbosity=${config.verbosityBias}. ")
            append(actionBias)
        }
    }

    // [2026-04-19 05:36][BLOC_B2][PERSONA_MANAGER] Liste complète exposée pour futurs réglages B2.
    fun getAllPersonas(): List<DivinePersonaConfig> {
        return DivinePersonaType.values().map { getConfig(it) }
    }

    // [2026-04-19 05:36][BLOC_B2][PERSONA_MANAGER] Construction depuis GodPersonalityEngine pour éviter toute double source de vérité.
    private fun buildConfigFromExistingEngine(personaType: DivinePersonaType): DivinePersonaConfig {
        val godId = toGodId(personaType)
        val personality = GodPersonalityEngine.get(godId)

        return DivinePersonaConfig(
            personaType = personaType,
            displayName = personality.displayName,
            coreTone = personality.toneIdentity,
            teachingStyle = personality.pedagogyStyle,
            correctionStyle = personality.correctionStyle,
            encouragementStyle = personality.rewardStyle,
            warmthBias = (personality.warmth.coerceIn(0, 10) / 10f),
            disciplineBias = (personality.strictness.coerceIn(0, 10) / 10f),
            maieuticBias = if (personality.usesQuestions) 0.85f else 0.35f,
            verbosityBias = computeVerbosityBias(personality.speechRhythm),
            preferredActionTypes = resolvePreferredActionTypes(personaType)
        )
    }

    // [2026-04-19 05:36][BLOC_B2][PERSONA_MANAGER] Mapping interne enum B2 -> identifiant existant GodPersonalityEngine.
    private fun toGodId(personaType: DivinePersonaType): String {
        return when (personaType) {
            DivinePersonaType.ZEUS -> "zeus"
            DivinePersonaType.ATHENA -> "athena"
            DivinePersonaType.POSEIDON -> "poseidon"
            DivinePersonaType.ARES -> "ares"
            DivinePersonaType.APHRODITE -> "aphrodite"
            DivinePersonaType.HERMES -> "hermes"
            DivinePersonaType.DEMETER -> "demeter"
            DivinePersonaType.HEPHAISTOS -> "hephaestus"
            DivinePersonaType.APOLLON -> "apollo"
            DivinePersonaType.PROMETHEE -> "prometheus"
            DivinePersonaType.ORACLE_NEUTRE -> "zeus"
        }
    }

    // [2026-04-19 05:36][BLOC_B2][PERSONA_MANAGER] Bias de verbosité dérivé des rythmes existants sans ajout de seconde logique lourde.
    private fun computeVerbosityBias(speechRhythm: String): Float {
        val value = speechRhythm.lowercase()
        return when {
            value.contains("courtes") || value.contains("incisives") || value.contains("nerv") -> 0.30f
            value.contains("calmes") || value.contains("équilibr") || value.contains("pédagog") -> 0.55f
            value.contains("mélod") || value.contains("ondul") || value.contains("imag") -> 0.70f
            else -> 0.50f
        }
    }

    // [2026-04-19 05:36][BLOC_B2][PERSONA_MANAGER] Préférences d'action alignées avec les rôles métiers déjà visibles dans le projet.
    private fun resolvePreferredActionTypes(personaType: DivinePersonaType): Set<DivineActionType> {
        return when (personaType) {
            DivinePersonaType.ZEUS -> setOf(
                DivineActionType.DIVINE_VERDICT,
                DivineActionType.QUIZ_CORRECTION,
                DivineActionType.SYSTEM_HELP
            )

            DivinePersonaType.ATHENA -> setOf(
                DivineActionType.SUMMARY_REFORMULATION,
                DivineActionType.QUIZ_CORRECTION,
                DivineActionType.SYSTEM_HELP
            )

            DivinePersonaType.POSEIDON -> setOf(
                DivineActionType.SUMMARY_GENERATION,
                DivineActionType.ENCOURAGEMENT,
                DivineActionType.DIVINE_SUGGESTION
            )

            DivinePersonaType.ARES -> setOf(
                DivineActionType.DIVINE_VERDICT,
                DivineActionType.ENCOURAGEMENT,
                DivineActionType.TEMPLE_GUIDANCE
            )

            DivinePersonaType.APHRODITE -> setOf(
                DivineActionType.MNEMONIC,
                DivineActionType.ENCOURAGEMENT,
                DivineActionType.SUMMARY_REFORMULATION
            )

            DivinePersonaType.HERMES -> setOf(
                DivineActionType.TRANSLATION,
                DivineActionType.LOADING_MESSAGE,
                DivineActionType.SYSTEM_HELP
            )

            DivinePersonaType.DEMETER -> setOf(
                DivineActionType.ENCOURAGEMENT,
                DivineActionType.TEMPLE_GUIDANCE,
                DivineActionType.SUMMARY_REFORMULATION
            )

            DivinePersonaType.HEPHAISTOS -> setOf(
                DivineActionType.ERROR_EXPLANATION,
                DivineActionType.QUIZ_CORRECTION,
                DivineActionType.SYSTEM_HELP
            )

            DivinePersonaType.APOLLON -> setOf(
                DivineActionType.MNEMONIC,
                DivineActionType.SUMMARY_GENERATION,
                DivineActionType.DIVINE_SUGGESTION
            )

            DivinePersonaType.PROMETHEE -> setOf(
                DivineActionType.ENCOURAGEMENT,
                DivineActionType.DIVINE_SUGGESTION,
                DivineActionType.SYSTEM_HELP
            )

            DivinePersonaType.ORACLE_NEUTRE -> setOf(
                DivineActionType.SYSTEM_HELP,
                DivineActionType.SUMMARY_REFORMULATION,
                DivineActionType.ERROR_EXPLANATION
            )
        }
    }
}
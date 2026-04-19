package com.revizeus.app

// [2026-04-19 05:36][BLOC_B2][RESPONSE_ORCHESTRATOR] Orchestrateur B2 léger, sans doublonner DivineDialogueOrchestrator.
object git status
git add .
git commit -m "Premiere phase Bloc B2"
git pushDivineResponseOrchestrator {

    // [2026-04-19 05:36][BLOC_B2][RESPONSE_ORCHESTRATOR] Construction du plan de réponse UI/prompt à partir du contexte B2.
    fun buildResponsePlan(context: DivineRequestContext): DivineResponsePlan {
        val personaType = resolvePersona(context)
        val config = DivinePersonaManager.getConfig(personaType)
        val speechMode = resolveSpeechMode(context)
        val actionHints = resolveActionHints(config, context.actionType)

        val uiHints = buildUiHints(
            speechMode = speechMode,
            screenSource = context.screenSource,
            successState = context.successState
        )

        return DivineResponsePlan(
            personaType = personaType,
            actionType = context.actionType,
            speechMode = speechMode,
            godDisplayName = config.displayName,
            promptHints = buildString {
                append(DivinePersonaManager.getPersonaPromptHints(personaType, context.actionType))
                append(" ")
                append(actionHints)
            }.trim(),
            uiHints = uiHints,
            shouldUseRpgDialog = speechMode == DivineSpeechMode.RPG_POPUP,
            shouldUseLoadingDivine = speechMode == DivineSpeechMode.LOADING_WHISPER,
            fallbackTextStyle = resolveFallbackTextStyle(speechMode, config)
        )
    }

    // [2026-04-19 05:36][BLOC_B2][RESPONSE_ORCHESTRATOR] Résolution de persona avec priorité au contexte explicite si fourni.
    fun resolvePersona(context: DivineRequestContext): DivinePersonaType {
        val explicitPersona = parsePersonaType(context.metadata["persona_type"])
        if (explicitPersona != null) return explicitPersona

        val subject = context.subject
        if (!subject.isNullOrBlank()) {
            return DivinePersonaManager.getPersonaForSubject(subject)
        }

        return when {
            context.screenSource.contains("oracle", ignoreCase = true) -> DivinePersonaType.ORACLE_NEUTRE
            context.screenSource.contains("result", ignoreCase = true) -> DivinePersonaType.ZEUS
            else -> DivinePersonaType.ORACLE_NEUTRE
        }
    }

    // [2026-04-19 05:36][BLOC_B2][RESPONSE_ORCHESTRATOR] Choix du mode d'expression sans couplage aux Activities.
    fun resolveSpeechMode(context: DivineRequestContext): DivineSpeechMode {
        val source = context.screenSource.lowercase()

        return when (context.actionType) {
            DivineActionType.LOADING_MESSAGE -> DivineSpeechMode.LOADING_WHISPER
            DivineActionType.QUIZ_CORRECTION,
            DivineActionType.DIVINE_VERDICT -> {
                if (source.contains("result") || source.contains("review")) {
                    DivineSpeechMode.RESULT_PANEL
                } else {
                    DivineSpeechMode.RPG_POPUP
                }
            }
            DivineActionType.ERROR_EXPLANATION,
            DivineActionType.SYSTEM_HELP -> DivineSpeechMode.SHORT_SYSTEM_FEEDBACK
            DivineActionType.SUMMARY_GENERATION,
            DivineActionType.SUMMARY_REFORMULATION,
            DivineActionType.QUIZ_GENERATION,
            DivineActionType.MNEMONIC,
            DivineActionType.ENCOURAGEMENT,
            DivineActionType.TEMPLE_GUIDANCE,
            DivineActionType.TRANSLATION,
            DivineActionType.DIVINE_SUGGESTION -> {
                if (source.contains("dialog") || source.contains("popup")) {
                    DivineSpeechMode.RPG_POPUP
                } else {
                    DivineSpeechMode.INTEGRATED_ACTIVITY_DIALOG
                }
            }
        }
    }

    // [2026-04-19 05:36][BLOC_B2][RESPONSE_ORCHESTRATOR] Hints d'action dérivés de la config persona et du besoin immédiat.
    fun resolveActionHints(
        config: DivinePersonaConfig,
        actionType: DivineActionType
    ): String {
        return when (actionType) {
            DivineActionType.SUMMARY_GENERATION ->
                "Rédige un résumé fidèle, clair et structuré dans le style ${config.teachingStyle}."
            DivineActionType.SUMMARY_REFORMULATION ->
                "Reformule plus proprement sans perdre le fond et sans dérive narrative."
            DivineActionType.QUIZ_GENERATION ->
                "Construis des questions adaptées au niveau, lisibles et pédagogiquement utiles."
            DivineActionType.QUIZ_CORRECTION ->
                "Explique la logique juste, puis corrige l'erreur avec le style ${config.correctionStyle}."
            DivineActionType.MNEMONIC ->
                "Produit une formulation courte, mémorisable et immédiatement réutilisable."
            DivineActionType.DIVINE_VERDICT ->
                "Donne un jugement juste, incarné, cohérent avec le ton ${config.coreTone}."
            DivineActionType.ENCOURAGEMENT ->
                "Encourage sans flatterie vide, avec le style ${config.encouragementStyle}."
            DivineActionType.ERROR_EXPLANATION ->
                "Explique le problème avec calme, précision et action de reprise."
            DivineActionType.SYSTEM_HELP ->
                "Reste concret, très utile et immédiatement exploitable par l'utilisateur."
            DivineActionType.LOADING_MESSAGE ->
                "Fais extrêmement court, respirant, élégant, sans bruit inutile."
            DivineActionType.TEMPLE_GUIDANCE ->
                "Relie la réponse à la progression, au temple, à l'effort ou à la consolidation."
            DivineActionType.TRANSLATION ->
                "Traduis avec fidélité, clarté et conservation du sens scolaire."
            DivineActionType.DIVINE_SUGGESTION ->
                "Termine par une prochaine action claire, faisable et cohérente."
        }
    }

    // [2026-04-19 05:36][BLOC_B2][RESPONSE_ORCHESTRATOR] Construction des indications UI non invasives.
    private fun buildUiHints(
        speechMode: DivineSpeechMode,
        screenSource: String,
        successState: Boolean?
    ): String {
        val successHint = when (successState) {
            true -> "État utilisateur: réussite."
            false -> "État utilisateur: difficulté ou échec."
            null -> "État utilisateur: neutre ou non précisé."
        }

        val modeHint = when (speechMode) {
            DivineSpeechMode.RPG_POPUP -> "Format court, incarné, compatible bulle RPG."
            DivineSpeechMode.INTEGRATED_ACTIVITY_DIALOG -> "Format moyen, intégré à l'écran, sans surcharge."
            DivineSpeechMode.LOADING_WHISPER -> "Format ultra court, chuchoté, respirant."
            DivineSpeechMode.RESULT_PANEL -> "Format verdict/correction, lisible en panneau de résultat."
            DivineSpeechMode.SHORT_SYSTEM_FEEDBACK -> "Format système très court, sans emphase excessive."
        }

        return "$modeHint Source écran: $screenSource. $successHint"
    }

    // [2026-04-19 05:36][BLOC_B2][RESPONSE_ORCHESTRATOR] Style de fallback textuel stable pour futures branches offline/secours.
    private fun resolveFallbackTextStyle(
        speechMode: DivineSpeechMode,
        config: DivinePersonaConfig
    ): String {
        return when (speechMode) {
            DivineSpeechMode.RPG_POPUP -> "immersif-court-${config.personaType.name.lowercase()}"
            DivineSpeechMode.INTEGRATED_ACTIVITY_DIALOG -> "intégré-clair-${config.personaType.name.lowercase()}"
            DivineSpeechMode.LOADING_WHISPER -> "souffle-très-court-${config.personaType.name.lowercase()}"
            DivineSpeechMode.RESULT_PANEL -> "verdict-structuré-${config.personaType.name.lowercase()}"
            DivineSpeechMode.SHORT_SYSTEM_FEEDBACK -> "système-bref-${config.personaType.name.lowercase()}"
        }
    }

    // [2026-04-19 05:36][BLOC_B2][RESPONSE_ORCHESTRATOR] Parsing souple pour ne pas imposer un nouveau format dur aux appelants.
    private fun parsePersonaType(raw: String?): DivinePersonaType? {
        val value = raw?.trim()?.uppercase() ?: return null
        return try {
            DivinePersonaType.valueOf(value)
        } catch (_: Exception) {
            null
        }
    }
}

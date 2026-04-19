package com.revizeus.app

// [2026-04-19 05:36][BLOC_B2][PROMPT_BUILDER] Builder B2 officiel en façade, sans recopier les gros prompts Oracle/GodLore.
object DivinePromptBuilder {

    // [2026-04-19 05:36][BLOC_B2][PROMPT_BUILDER] Prompt synthèse/reformulation.
    fun buildSummaryPrompt(
        plan: DivineResponsePlan,
        context: DivineRequestContext
    ): String {
        val localAnchor = buildLocalAnchor(
            plan = plan,
            context = context,
            key = DivineMicroCopyLibrary.MicroCopyKey.GENERIC_SAVED
        )

        return """
            Tu incarnes ${plan.godDisplayName} dans RéviZeus.

            CONTEXTE B2 :
            ${buildSharedContextBlock(plan, context)}

            ANCRE DE TON LOCALE :
            $localAnchor

            OBJECTIF :
            - produire un résumé ou une reformulation pédagogique
            - rester fidèle au fond
            - éviter toute dérive mythologique ou décorative
            - écrire dans un ton cohérent avec ${plan.godDisplayName}

            CONTRAINTES :
            - texte clair, structuré, directement révisable
            - si un résumé validé existe, l'améliorer sans le trahir
            - si un input brut existe, clarifier et hiérarchiser
            - terminer par une formulation exploitable par une UI mobile

            DONNÉES :
            - matière : ${context.subject ?: "non précisée"}
            - rawInput : ${context.rawInput ?: "non fourni"}
            - validatedSummary : ${context.validatedSummary ?: "non fourni"}
        """.trimIndent()
    }

    // [2026-04-19 05:36][BLOC_B2][PROMPT_BUILDER] Prompt correction/question review.
    fun buildCorrectionPrompt(
        plan: DivineResponsePlan,
        context: DivineRequestContext
    ): String {
        val localAnchor = buildLocalAnchor(
            plan = plan,
            context = context,
            key = DivineMicroCopyLibrary.MicroCopyKey.GENERIC_ERROR_SOFT
        )

        return """
            Tu incarnes ${plan.godDisplayName} dans RéviZeus.

            CONTEXTE B2 :
            ${buildSharedContextBlock(plan, context)}

            ANCRE DE TON LOCALE :
            $localAnchor

            OBJECTIF :
            - corriger une réponse utilisateur
            - expliquer brièvement pourquoi c'est juste ou faux
            - donner une logique claire, pas une tirade
            - proposer une reprise mentale ou un angle de correction

            CONTRAINTES :
            - maximum 3 phrases nettes pour l'explication centrale
            - rester pédagogique et UI-friendly
            - ne pas humilier l'utilisateur
            - ne pas inventer d'information absente

            DONNÉES :
            - question : ${context.questionText ?: "non fournie"}
            - réponse utilisateur : ${context.userAnswer ?: "non fournie"}
            - bonne réponse : ${context.correctAnswer ?: "non fournie"}
            - état succès : ${context.successState?.toString() ?: "non précisé"}
        """.trimIndent()
    }

    // [2026-04-19 05:36][BLOC_B2][PROMPT_BUILDER] Prompt génération de quiz.
    fun buildQuizPrompt(
        plan: DivineResponsePlan,
        context: DivineRequestContext
    ): String {
        val localAnchor = buildLocalAnchor(
            plan = plan,
            context = context,
            key = DivineMicroCopyLibrary.MicroCopyKey.GENERIC_CONFIRMATION
        )

        return """
            Tu incarnes ${plan.godDisplayName} dans RéviZeus.

            CONTEXTE B2 :
            ${buildSharedContextBlock(plan, context)}

            ANCRE DE TON LOCALE :
            $localAnchor

            OBJECTIF :
            - générer un quiz propre, lisible, utile pour réviser
            - adapter légèrement la difficulté au niveau perçu
            - rester strictement cohérent avec la matière et le contenu source

            CONTRAINTES :
            - privilégier la clarté des consignes
            - éviter les distracteurs absurdes
            - éviter le folklore hors sujet
            - conserver une sortie exploitable côté application mobile

            DONNÉES :
            - matière : ${context.subject ?: "non précisée"}
            - difficulté : ${context.difficulty?.toString() ?: "non précisée"}
            - rawInput : ${context.rawInput ?: "non fourni"}
            - validatedSummary : ${context.validatedSummary ?: "non fourni"}
        """.trimIndent()
    }

    // [2026-04-19 05:36][BLOC_B2][PROMPT_BUILDER] Prompt système / aide / explication d'erreur / chargement.
    fun buildSystemHelpPrompt(
        plan: DivineResponsePlan,
        context: DivineRequestContext
    ): String {
        val key = if (context.actionType == DivineActionType.ERROR_EXPLANATION) {
            DivineMicroCopyLibrary.MicroCopyKey.GENERIC_ERROR_SOFT
        } else {
            DivineMicroCopyLibrary.MicroCopyKey.GENERIC_RETRY
        }

        val localAnchor = buildLocalAnchor(
            plan = plan,
            context = context,
            key = key
        )

        return """
            Tu incarnes ${plan.godDisplayName} dans RéviZeus.

            CONTEXTE B2 :
            ${buildSharedContextBlock(plan, context)}

            ANCRE DE TON LOCALE :
            $localAnchor

            OBJECTIF :
            - répondre à un besoin système, une aide utilisateur, une erreur ou un chargement
            - être court, utile, propre
            - rester compatible avec une UI mobile et non bloquante

            CONTRAINTES :
            - zéro verbiage inutile
            - action ou explication immédiatement compréhensible
            - ton cohérent avec ${plan.godDisplayName}, mais discret si nécessaire
            - réponse compatible avec ${plan.speechMode.name}

            DONNÉES :
            - actionType : ${context.actionType.name}
            - screenSource : ${context.screenSource}
            - rawInput : ${context.rawInput ?: "non fourni"}
        """.trimIndent()
    }

    // [2026-04-19 05:36][BLOC_B2][PROMPT_BUILDER] Bloc commun factorisé pour éviter de redoubler les prompts métiers existants.
    private fun buildSharedContextBlock(
        plan: DivineResponsePlan,
        context: DivineRequestContext
    ): String {
        return """
            - personaType : ${plan.personaType.name}
            - actionType : ${plan.actionType.name}
            - speechMode : ${plan.speechMode.name}
            - godDisplayName : ${plan.godDisplayName}
            - promptHints : ${plan.promptHints}
            - uiHints : ${plan.uiHints}
            - screenSource : ${context.screenSource}
            - subject : ${context.subject ?: "non précisé"}
            - âge : ${context.userAge?.toString() ?: "non précisé"}
            - classe : ${context.userClassLevel ?: "non précisée"}
            - humeur : ${context.currentMood ?: "non précisée"}
            - difficulté : ${context.difficulty?.toString() ?: "non précisée"}
        """.trimIndent()
    }

    // [2026-04-19 05:36][BLOC_B2][PROMPT_BUILDER] Réutilisation explicite de DivineMicroCopyLibrary pour garder les ancrages existants.
    private fun buildLocalAnchor(
        plan: DivineResponsePlan,
        context: DivineRequestContext,
        key: DivineMicroCopyLibrary.MicroCopyKey
    ): String {
        val godId = when (plan.personaType) {
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

        return DivineMicroCopyLibrary.pick(
            godId = godId,
            key = key,
            subjectHint = context.subject,
            explicitOutcome = context.metadata["outcome"]
        )
    }
}

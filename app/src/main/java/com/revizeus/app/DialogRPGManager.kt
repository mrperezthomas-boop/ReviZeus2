package com.revizeus.app

import androidx.appcompat.app.AppCompatActivity

/**
 * ═══════════════════════════════════════════════════════════════
 * DIALOG RPG MANAGER — Fabrique centrale de dialogues RPG
 * ═══════════════════════════════════════════════════════════════
 *
 * BLOC B — DIALOGUES RPG UNIVERSELS
 *
 * Utilité :
 * - Fabrique centrale pour créer et afficher tous les dialogues RPG
 *   narratifs de RéviZeus.
 * - Remplace les AlertDialog Android plats et les Toast techniques
 *   par des dialogues immersifs avec typewriter, chibi animé,
 *   blip sonore, et dieu approprié.
 * - Simplifie l'appel depuis n'importe quelle Activity en fournissant
 *   des méthodes helper prêtes à l'emploi.
 *
 * AJOUTS PATCH ADAPTATIF COMPATIBLE GEMINI ACTUEL :
 * - AUCUN remplacement du GeminiManager existant.
 * - Ajout de wrappers qui branchent une génération IA contextuelle via
 *   DivineDialogueOrchestrator.
 * - Conservation totale des anciennes méthodes sync (showInfo, showReward,
 *   showTechnicalError, etc.) pour ne rien casser dans les écrans déjà codés.
 *
 * RÈGLE DE DESIGN :
 * - petits messages système → possible en dur via DivineMicroCopyLibrary ;
 * - vrais moments pédagogiques / verdicts / dialogues divins → IA adaptative.
 *
 * ═══════════════════════════════════════════════════════════════
 */
object DialogRPGManager {

    /**
     * Affiche un dialogue RPG universel avec configuration complète.
     *
     * @param activity L'activité hôte (doit hériter d'AppCompatActivity)
     * @param config Configuration complète du dialogue (DialogRPGConfig)
     */
    fun show(
        activity: AppCompatActivity,
        config: DialogRPGConfig
    ) {
        // Vérifications de sécurité lifecycle
        if (activity.isFinishing || activity.isDestroyed) return
        if (activity.supportFragmentManager.isStateSaved) return

        // Génération d'un ID unique pour ce dialogue
        val actionId = "dialog_rpg_${System.currentTimeMillis()}_${System.nanoTime()}"

        // Stockage temporaire des lambdas (non-sérialisables)
        DialogRPGConfig.pendingActions[actionId] = mapOf(
            "button1" to config.button1Action,
            "button2" to config.button2Action,
            "button3" to config.button3Action,
            "onDismiss" to config.onDismiss
        )

        // Création et affichage du fragment
        try {
            val dialog = DialogRPGFragment.newInstance(config, actionId)
            dialog.show(activity.supportFragmentManager, "DialogRPG_$actionId")
        } catch (e: Exception) {
            // Nettoyage en cas d'échec
            DialogRPGConfig.clearActions(actionId)
        }
    }

    /**
     * Affiche un message d'information simple avec un seul bouton.
     *
     * @param activity L'activité hôte
     * @param godId ID du dieu parlant ("zeus", "athena", etc.)
     * @param message Texte principal du dialogue
     * @param title Titre optionnel du dialogue
     * @param onDismiss Callback optionnel à la fermeture
     */
    fun showInfo(
        activity: AppCompatActivity,
        godId: String,
        message: String,
        title: String? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        show(activity, DialogRPGConfig(
            mainText = message,
            godId = godId,
            title = title,
            category = DialogCategory.INFO,
            button1Label = "COMPRIS ⚡",
            onDismiss = onDismiss
        ))
    }

    /**
     * Affiche une confirmation avec deux boutons (confirm/cancel).
     *
     * @param activity L'activité hôte
     * @param godId ID du dieu parlant ("zeus", "athena", etc.)
     * @param message Texte principal du dialogue
     * @param title Titre optionnel du dialogue
     * @param confirmLabel Label du bouton de confirmation (défaut: "CONFIRMER")
     * @param cancelLabel Label du bouton d'annulation (défaut: "ANNULER")
     * @param onConfirm Action exécutée si l'utilisateur confirme
     * @param onCancel Action optionnelle exécutée si l'utilisateur annule
     */
    fun showConfirmation(
        activity: AppCompatActivity,
        godId: String,
        message: String,
        title: String? = null,
        confirmLabel: String = "CONFIRMER",
        cancelLabel: String = "ANNULER",
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        show(activity, DialogRPGConfig(
            mainText = message,
            godId = godId,
            title = title,
            category = DialogCategory.CONFIRMATION,
            button1Label = confirmLabel,
            button1Action = onConfirm,
            button2Label = cancelLabel,
            button2Action = onCancel,
            cancelable = false  // Force l'utilisateur à choisir un bouton
        ))
    }

    /**
     * Affiche une erreur technique rendue diégétique.
     * Le dieu et le message sont automatiquement choisis selon le type d'erreur.
     *
     * @param activity L'activité hôte
     * @param errorType Type d'erreur technique (TechnicalErrorType)
     * @param onDismiss Callback optionnel à la fermeture
     */
    fun showTechnicalError(
        activity: AppCompatActivity,
        errorType: TechnicalErrorType,
        onDismiss: (() -> Unit)? = null
    ) {
        show(activity, DialogRPGConfig(
            mainText = errorType.getDiegeticMessage(),
            godId = errorType.getGodId(),
            category = DialogCategory.ERROR_TECHNICAL,
            button1Label = "COMPRIS",
            onDismiss = onDismiss
        ))
    }

    /**
     * Affiche une alerte de quota divin dépassé (fatigue divine).
     * Convertit automatiquement un quota API en message diégétique.
     *
     * @param activity L'activité hôte
     * @param divineService Service divin concerné ("aphrodite_drawing", "apollo_music", etc.)
     * @param resetTime Heure de reset optionnelle ("demain", "dans 2 heures", etc.)
     * @param onDismiss Callback optionnel à la fermeture
     */
    fun showDivineFatigue(
        activity: AppCompatActivity,
        divineService: String,
        resetTime: String? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        val (godId, message) = getDivineFatigueMessage(divineService, resetTime)
        show(activity, DialogRPGConfig(
            mainText = message,
            godId = godId,
            category = DialogCategory.DIVINE_FATIGUE,
            button1Label = "COMPRIS",
            onDismiss = onDismiss
        ))
    }

    /**
     * Affiche une récompense ou un succès.
     *
     * @param activity L'activité hôte
     * @param godId ID du dieu parlant ("zeus", "athena", etc.)
     * @param message Texte principal (annonce de la récompense)
     * @param title Titre optionnel
     * @param additionalLabel Label de la zone additionnelle (défaut: "DÉTAILS")
     * @param additionalText Texte additionnel optionnel (détails de la récompense)
     * @param onDismiss Callback optionnel à la fermeture
     */
    fun showReward(
        activity: AppCompatActivity,
        godId: String,
        message: String,
        title: String? = null,
        additionalLabel: String = "DÉTAILS",
        additionalText: String? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        show(activity, DialogRPGConfig(
            mainText = message,
            godId = godId,
            title = title,
            category = DialogCategory.REWARD,
            additionalLabel = if (additionalText != null) additionalLabel else null,
            additionalText = additionalText,
            button1Label = "MERCI ! ⚡",
            onDismiss = onDismiss
        ))
    }

    /**
     * Affiche une aide / astuce Prométhée.
     *
     * @param activity L'activité hôte
     * @param message Texte principal (conseil, astuce, explication)
     * @param title Titre optionnel
     * @param additionalInfo Détails optionnels
     * @param onDismiss Callback optionnel à la fermeture
     */
    fun showHelp(
        activity: AppCompatActivity,
        message: String,
        title: String? = null,
        additionalInfo: String? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        show(activity, DialogRPGConfig(
            mainText = message,
            godId = "prometheus",
            title = title,
            category = DialogCategory.HELP,
            additionalLabel = if (additionalInfo != null) "DÉTAILS" else null,
            additionalText = additionalInfo,
            button1Label = "MERCI !",
            onDismiss = onDismiss
        ))
    }

    /**
     * Affiche une alerte / avertissement important.
     *
     * @param activity L'activité hôte
     * @param godId ID du dieu parlant ("zeus", "athena", etc.)
     * @param message Texte principal de l'alerte
     * @param title Titre optionnel
     * @param onDismiss Callback optionnel à la fermeture
     */
    fun showAlert(
        activity: AppCompatActivity,
        godId: String,
        message: String,
        title: String? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        show(activity, DialogRPGConfig(
            mainText = message,
            godId = godId,
            title = title,
            category = DialogCategory.ALERT,
            button1Label = "COMPRIS",
            onDismiss = onDismiss
        ))
    }

    // ══════════════════════════════════════════════════════════
    // PATCH ADAPTATIF — WRAPPERS IA COMPATIBLES GEMINI ACTUEL
    // ══════════════════════════════════════════════════════════

    /**
     * Wrapper générique pour une vraie génération IA adaptative.
     *
     * On délègue l'intelligence à DivineDialogueOrchestrator, qui lui-même
     * s'appuie sur TON GeminiManager actuel et son champ adaptiveContextNote.
     */
    fun showAdaptive(
        activity: AppCompatActivity,
        request: DivineDialogueOrchestrator.Request
    ) {
        DivineDialogueOrchestrator.showAdaptive(activity, request)
    }

    /**
     * Helper prêt à l'emploi pour un conseil / verdict pédagogique IA.
     */
    fun showAdaptivePedagogy(
        activity: AppCompatActivity,
        godId: String,
        prompt: String,
        subjectHint: String,
        title: String? = null,
        topicHint: String? = null,
        currentCourseTitle: String? = null,
        currentQuestionText: String? = null,
        latestScorePercent: Int? = null,
        latestStars: Int? = null,
        explicitOutcome: String? = null,
        templeProgressByGod: Map<String, Int> = emptyMap(),
        equippedItems: List<String> = emptyList(),
        equippedArtifacts: List<String> = emptyList(),
        futureParams: Map<String, String> = emptyMap(),
        onDismiss: (() -> Unit)? = null
    ) {
        showAdaptive(
            activity = activity,
            request = DivineDialogueOrchestrator.Request(
                godId = godId,
                prompt = prompt,
                subjectHint = subjectHint,
                dialogCategory = DialogCategory.PEDAGOGY,
                title = title,
                triggerLabel = "PEDAGOGY_HELPER",
                explicitGoal = "produire un vrai dialogue pédagogique contextuel",
                topicHint = topicHint,
                currentCourseTitle = currentCourseTitle,
                currentQuestionText = currentQuestionText,
                latestScorePercent = latestScorePercent,
                latestStars = latestStars,
                explicitOutcome = explicitOutcome,
                templeProgressByGod = templeProgressByGod,
                equippedItems = equippedItems,
                equippedArtifacts = equippedArtifacts,
                futureParams = futureParams,
                buttonLabel = "J'AI COMPRIS ⚡",
                onDismiss = onDismiss,
                fallbackKey = DivineMicroCopyLibrary.MicroCopyKey.GENERIC_ERROR_SOFT
            )
        )
    }

    /**
     * Helper IA pour les récompenses / félicitations adaptatives.
     */
    fun showAdaptiveReward(
        activity: AppCompatActivity,
        godId: String,
        prompt: String,
        subjectHint: String,
        title: String? = null,
        latestScorePercent: Int? = null,
        latestStars: Int? = null,
        explicitOutcome: String? = null,
        templeProgressByGod: Map<String, Int> = emptyMap(),
        equippedItems: List<String> = emptyList(),
        equippedArtifacts: List<String> = emptyList(),
        futureParams: Map<String, String> = emptyMap(),
        onDismiss: (() -> Unit)? = null
    ) {
        showAdaptive(
            activity = activity,
            request = DivineDialogueOrchestrator.Request(
                godId = godId,
                prompt = prompt,
                subjectHint = subjectHint,
                dialogCategory = DialogCategory.REWARD,
                title = title,
                triggerLabel = "REWARD_HELPER",
                explicitGoal = "produire une récompense ou félicitation liée à la progression réelle",
                latestScorePercent = latestScorePercent,
                latestStars = latestStars,
                explicitOutcome = explicitOutcome,
                templeProgressByGod = templeProgressByGod,
                equippedItems = equippedItems,
                equippedArtifacts = equippedArtifacts,
                futureParams = futureParams,
                buttonLabel = "POUR L'OLYMPE ✨",
                onDismiss = onDismiss,
                fallbackKey = DivineMicroCopyLibrary.MicroCopyKey.GENERIC_SUCCESS
            )
        )
    }

    /**
     * Helper IA pour les aides système / conseils Prométhée.
     */
    fun showAdaptiveHelp(
        activity: AppCompatActivity,
        prompt: String,
        subjectHint: String = "Système",
        title: String? = null,
        explicitOutcome: String? = null,
        futureParams: Map<String, String> = emptyMap(),
        onDismiss: (() -> Unit)? = null
    ) {
        showAdaptive(
            activity = activity,
            request = DivineDialogueOrchestrator.Request(
                godId = "prometheus",
                prompt = prompt,
                subjectHint = subjectHint,
                dialogCategory = DialogCategory.HELP,
                title = title,
                triggerLabel = "HELP_HELPER",
                explicitGoal = "aider le joueur avec clarté et chaleur",
                explicitOutcome = explicitOutcome,
                futureParams = futureParams,
                buttonLabel = "MERCI PROMÉTHÉE",
                onDismiss = onDismiss,
                fallbackKey = DivineMicroCopyLibrary.MicroCopyKey.GENERIC_RETRY
            )
        )
    }

    /**
     * Helper synchrone léger pour les très petits messages qu'on ne veut PAS envoyer à l'IA.
     *
     * Cela respecte ta consigne :
     * - pas plat ;
     * - plusieurs variantes ;
     * - humour léger RéviZeus ;
     * - aucun coût d'appel IA inutile.
     */
    fun showMicroCopy(
        activity: AppCompatActivity,
        godId: String,
        key: DivineMicroCopyLibrary.MicroCopyKey,
        subjectHint: String? = null,
        explicitOutcome: String? = null,
        title: String? = null,
        category: DialogCategory = DialogCategory.INFO,
        onDismiss: (() -> Unit)? = null
    ) {
        val normalizedGodId = GodPersonalityEngine.normalizeGodId(godId)
        val text = DivineMicroCopyLibrary.pick(
            godId = normalizedGodId,
            key = key,
            subjectHint = subjectHint,
            explicitOutcome = explicitOutcome
        )

        show(
            activity = activity,
            config = DialogRPGConfig(
                mainText = text,
                godId = normalizedGodId,
                title = title,
                category = category,
                button1Label = "COMPRIS ⚡",
                onDismiss = onDismiss
            )
        )
    }

    // ══════════════════════════════════════════════════════════
    // HELPERS INTERNES
    // ══════════════════════════════════════════════════════════

    /**
     * Génère un message de fatigue divine selon le service concerné.
     *
     * @param service Service divin ("aphrodite_drawing", "apollo_music", etc.)
     * @param resetTime Heure de reset ("demain", "dans 2 heures", etc.)
     * @return Pair(godId, message diégétique)
     */
    private fun getDivineFatigueMessage(service: String, resetTime: String?): Pair<String, String> {
        val timeInfo = if (resetTime != null) " Reviens $resetTime." else " Reviens demain."

        return when (service) {
            "aphrodite_drawing", "aphrodite_visualization" -> Pair(
                "aphrodite",
                "Aphrodite a épuisé son inspiration visuelle pour aujourd'hui.$timeInfo"
            )
            "apollo_music", "apollo_poetry" -> Pair(
                "apollo",
                "La lyre d'Apollon se repose.$timeInfo"
            )
            "athena_summary", "athena_explanation" -> Pair(
                "athena",
                "Athéna a atteint sa limite de sagesse quotidienne.$timeInfo"
            )
            "oracle_gemini", "oracle_ai" -> Pair(
                "zeus",
                "L'Oracle a besoin de méditer. Zeus te demande de patienter.$timeInfo"
            )
            "hermes_translation" -> Pair(
                "hermes",
                "Hermès a livré suffisamment de messages pour aujourd'hui.$timeInfo"
            )
            "demeter_spaced_repetition" -> Pair(
                "demeter",
                "Déméter a planté toutes les graines de savoir pour aujourd'hui.$timeInfo"
            )
            "hephaestus_crafting" -> Pair(
                "hephaestus",
                "La forge d'Héphaïstos est éteinte pour aujourd'hui.$timeInfo"
            )
            else -> Pair(
                "zeus",
                "Les dieux ont besoin de repos.$timeInfo"
            )
        }
    }

    /**
     * Sélection automatique du dieu selon le contexte métier.
     *
     * @param context Contexte fonctionnel (DialogContext)
     * @return ID du dieu approprié
     */
    fun selectGodForContext(context: DialogContext): String {
        return context.getGodId()
    }
}

package com.revizeus.app

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Orchestrateur central pour brancher l'IA adaptative SANS remplacer GeminiManager.
 *
 * Principe :
 * 1. on résout un snapshot joueur ;
 * 2. on formate adaptiveContextNote ;
 * 3. on appelle TON GeminiManager actuel ;
 * 4. on rend le résultat dans DialogRPGManager ;
 * 5. si l'IA ne répond pas, on bascule sur une microcopie vivante multi-variantes.
 */
object DivineDialogueOrchestrator {

    data class Request(
        val godId: String,
        val prompt: String,
        val subjectHint: String,
        val dialogCategory: DialogCategory = DialogCategory.INFO,
        val title: String? = null,
        val triggerLabel: String,
        val explicitGoal: String,
        val topicHint: String? = null,
        val currentCourseTitle: String? = null,
        val currentQuestionText: String? = null,
        val latestScorePercent: Int? = null,
        val latestStars: Int? = null,
        val explicitOutcome: String? = null,
        val adventureStep: String? = null,
        val templeProgressByGod: Map<String, Int> = emptyMap(),
        val equippedItems: List<String> = emptyList(),
        val equippedArtifacts: List<String> = emptyList(),
        val futureParams: Map<String, String> = emptyMap(),
        val buttonLabel: String = "COMPRIS ⚡",
        val onDismiss: (() -> Unit)? = null,
        val extraInstructions: String? = null,
        val fallbackKey: DivineMicroCopyLibrary.MicroCopyKey = DivineMicroCopyLibrary.MicroCopyKey.GENERIC_ERROR_SOFT,
        val fallbackAdditionalText: String? = null
    )

    fun showAdaptive(
        activity: AppCompatActivity,
        request: Request
    ) {
        activity.lifecycleScope.launch {
            val normalizedGodId = GodPersonalityEngine.normalizeGodId(request.godId)

            val snapshot = PlayerContextResolver.resolve(
                context = activity,
                request = PlayerContextResolver.Request(
                    subjectHint = request.subjectHint,
                    topicHint = request.topicHint,
                    currentCourseTitle = request.currentCourseTitle,
                    currentQuestionText = request.currentQuestionText,
                    latestScorePercent = request.latestScorePercent,
                    latestStars = request.latestStars,
                    explicitOutcome = request.explicitOutcome,
                    adventureStep = request.adventureStep,
                    templeProgressByGod = request.templeProgressByGod,
                    equippedItems = request.equippedItems,
                    equippedArtifacts = request.equippedArtifacts,
                    futureParams = request.futureParams
                )
            )

            val adaptiveContextNote = AdaptiveContextFormatter.buildAdaptiveContextNote(
                snapshot = snapshot,
                godId = normalizedGodId,
                dialogCategory = request.dialogCategory,
                triggerLabel = request.triggerLabel,
                explicitGoal = request.explicitGoal,
                extraInstructions = request.extraInstructions
            )

            val aiResponse = GeminiManager.generateDialog(
                prompt = request.prompt,
                matiere = request.subjectHint,
                adaptiveContextNote = adaptiveContextNote
            )

            if (aiResponse != null && aiResponse.text.isNotBlank()) {
                val responseGodId = GodPersonalityEngine.normalizeGodId(aiResponse.godName).ifBlank { normalizedGodId }
                val additionalText = buildAdditionalText(
                    mnemo = aiResponse.mnemo,
                    suggestedAction = aiResponse.suggestedAction,
                    fallbackAdditionalText = request.fallbackAdditionalText
                )

                DialogRPGManager.show(
                    activity = activity,
                    config = DialogRPGConfig(
                        mainText = aiResponse.text,
                        godId = responseGodId,
                        title = request.title,
                        category = request.dialogCategory,
                        additionalLabel = if (additionalText != null) "AIDE DIVINE" else null,
                        additionalText = additionalText,
                        button1Label = request.buttonLabel,
                        onDismiss = request.onDismiss
                    )
                )
            } else {
                val fallback = DivineMicroCopyLibrary.pick(
                    godId = normalizedGodId,
                    key = request.fallbackKey,
                    subjectHint = request.subjectHint,
                    explicitOutcome = request.explicitOutcome
                )

                val fallbackAdditional = request.fallbackAdditionalText
                    ?: "Le message a été servi en mode secours immersif pour éviter de casser ton flow."

                DialogRPGManager.show(
                    activity = activity,
                    config = DialogRPGConfig(
                        mainText = fallback,
                        godId = normalizedGodId,
                        title = request.title,
                        category = request.dialogCategory,
                        additionalLabel = "SECOURS DIVIN",
                        additionalText = fallbackAdditional,
                        button1Label = request.buttonLabel,
                        onDismiss = request.onDismiss
                    )
                )
            }
        }
    }

    private fun buildAdditionalText(
        mnemo: String,
        suggestedAction: String,
        fallbackAdditionalText: String?
    ): String? {
        val blocks = buildList {
            if (mnemo.isNotBlank()) add("Mnémotechnique : $mnemo")
            if (suggestedAction.isNotBlank()) add("Action suggérée : $suggestedAction")
            if (!fallbackAdditionalText.isNullOrBlank()) add(fallbackAdditionalText)
        }
        return blocks.takeIf { it.isNotEmpty() }?.joinToString("\n\n")
    }
}

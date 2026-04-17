package com.revizeus.app

import android.content.Context

/**
 * ═══════════════════════════════════════════════════════════════
 * ADAPTIVE DIALOGUE ENGINE — Fusion Dieu + Joueur + Contexte
 * ═══════════════════════════════════════════════════════════════
 *
 * Rôle :
 * - Transformer un message brut en message incarné et adaptatif.
 * - Produire un bloc de contexte réutilisable pour GeminiManager.
 * - Construire des DialogRPGConfig cohérents sans imposer de refonte des écrans.
 *
 * Important :
 * - Le moteur reste volontairement conservateur sur les textes locaux.
 * - Il enrichit sans casser le sens métier fourni par les Activities.
 * - Il prépare le terrain pour les futures invocations IA plus profondes.
 * ═══════════════════════════════════════════════════════════════
 */
object AdaptiveDialogueEngine {

    data class AdaptiveDialogRequest(
        val godId: String,
        val category: DialogCategory,
        val rawMessage: String,
        val title: String? = null,
        val additionalLabel: String? = null,
        val additionalText: String? = null,
        val button1Label: String = "COMPRIS ⚡",
        val button1Action: (() -> Unit)? = null,
        val button2Label: String? = null,
        val button2Action: (() -> Unit)? = null,
        val button3Label: String? = null,
        val button3Action: (() -> Unit)? = null,
        val cancelable: Boolean = true,
        val tapToSkipTypewriter: Boolean = true,
        val onDismiss: (() -> Unit)? = null
    )

    fun buildAdaptiveConfig(
        context: Context,
        request: AdaptiveDialogRequest,
        playerContext: PlayerContextResolver.PlayerDialogueContext = PlayerContextResolver.resolveLightweight(context)
    ): DialogRPGConfig {
        val normalizedGodId = GodPersonalityEngine.normalizeGodId(request.godId)
        val personality = GodPersonalityEngine.getPersonality(normalizedGodId)
        val adaptiveMessage = GodPersonalityEngine.createLocalAdaptiveMessage(
            godId = normalizedGodId,
            rawMessage = request.rawMessage,
            playerContext = playerContext,
            category = request.category
        )

        val speed = computeTypewriterSpeed(
            category = request.category,
            playerContext = playerContext,
            personality = personality
        )

        return DialogRPGConfig(
            mainText = adaptiveMessage,
            godId = normalizedGodId,
            title = request.title,
            additionalLabel = request.additionalLabel,
            additionalText = request.additionalText,
            category = request.category,
            button1Label = request.button1Label,
            button1Action = request.button1Action,
            button2Label = request.button2Label,
            button2Action = request.button2Action,
            button3Label = request.button3Label,
            button3Action = request.button3Action,
            cancelable = request.cancelable,
            tapToSkipTypewriter = request.tapToSkipTypewriter,
            typewriterSpeed = speed,
            onDismiss = request.onDismiss
        )
    }

    fun buildAdaptivePromptNote(
        godId: String,
        dialogContext: DialogContext,
        playerContext: PlayerContextResolver.PlayerDialogueContext,
        rawIntent: String,
        rawMessage: String
    ): String {
        val personality = GodPersonalityEngine.getPersonality(godId)
        return """
            PROFIL DIVIN :
            - dieu : ${personality.displayName}
            - rôle pédagogique : ${personality.pedagogicalRole}
            - tonalité : ${personality.toneTag}
            - rythme : ${personality.speechRhythm}
            - sévérité : ${personality.strictness}/10
            - chaleur : ${personality.warmth}/10
            - poésie : ${personality.poeticLevel}/10
            - questions guidées : ${if (personality.usesQuestions) "oui" else "non"}

            PROFIL JOUEUR :
            - pseudo : ${playerContext.pseudo}
            - âge : ${playerContext.age}
            - classe : ${playerContext.classLevel}
            - humeur : ${playerContext.mood}
            - niveau : ${playerContext.level}
            - rang : ${playerContext.rank}
            - point faible dominant : ${playerContext.dominantWeakness}
            - taux de réussite récent : ${(playerContext.recentSuccessRate * 100f).toInt()}%
            - temps de réponse moyen : ${playerContext.averageResponseTimeMs} ms
            - fatigue : ${"%.2f".format(playerContext.fatigueIndex)}
            - mode doux : ${playerContext.needsGentleMode}
            - mode défi : ${playerContext.needsChallengeMode}

            CONTEXTE DE LA SCÈNE :
            - contexte : $dialogContext
            - intention brute : $rawIntent
            - message métier à respecter : $rawMessage

            CONTRAINTES DE SORTIE :
            - la réponse doit rester brève, typewriter-compatible et lisible en dialogue RPG ;
            - elle doit conserver le sens réel du message métier ;
            - elle doit être incarnée par ${personality.displayName} ;
            - aucun jargon technique brut ne doit être exposé ;
            - la dernière phrase doit idéalement être mémorisable ;
            - ${GodPersonalityEngine.buildSystemInstruction(godId)}
        """.trimIndent()
    }

    private fun computeTypewriterSpeed(
        category: DialogCategory,
        playerContext: PlayerContextResolver.PlayerDialogueContext,
        personality: GodPersonalityEngine.GodPersonality
    ): Long {
        var speed = 35L

        if (playerContext.needsGentleMode) {
            speed += 6L
        }
        if (playerContext.needsChallengeMode) {
            speed -= 4L
        }
        if (personality.speechRhythm.contains("rapide", ignoreCase = true)) {
            speed -= 3L
        }
        if (personality.speechRhythm.contains("posé", ignoreCase = true) || personality.speechRhythm.contains("doux", ignoreCase = true)) {
            speed += 3L
        }
        if (category == DialogCategory.REWARD) {
            speed = (speed - 2L).coerceAtLeast(24L)
        }
        if (category == DialogCategory.ERROR_TECHNICAL || category == DialogCategory.CONFIRMATION) {
            speed = (speed + 2L).coerceAtMost(48L)
        }

        return speed.coerceIn(24L, 48L)
    }
}

package com.revizeus.app.ai

/**
 * [2026-04-20 23:59][TRANSPORT_IA_TOTAL]
 * Contrat minimal de transport IA.
 *
 * IMPORTANT :
 * - Le métier reste dans GeminiManager
 * - Les prompts restent dans GeminiManager
 * - Le parsing reste dans GeminiManager
 * - Cette interface ne gère que le transport brut
 */
interface AiInvocationGateway {

    data class InlineImage(
        val mimeType: String,
        val dataBase64: String
    )

    suspend fun invoke(
        systemInstruction: String,
        prompt: String,
        model: String? = null,
        images: List<InlineImage> = emptyList()
    ): String
}

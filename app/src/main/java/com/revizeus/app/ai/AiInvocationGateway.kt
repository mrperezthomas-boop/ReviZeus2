package com.revizeus.app.ai

/**
 * [2026-04-20][TRANSPORT_ORACLE_TEXTE]
 * Contrat minimal de transport IA.
 *
 * IMPORTANT :
 * - Le métier reste dans GeminiManager
 * - Les prompts restent dans GeminiManager
 * - Le parsing reste dans GeminiManager
 * - Cette interface ne gère que le transport brut
 */
interface AiInvocationGateway {

    suspend fun invokeText(
        systemInstruction: String,
        prompt: String,
        model: String? = null
    ): String
}
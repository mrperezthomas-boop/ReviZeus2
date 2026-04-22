package com.revizeus.app.ai

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * [2026-04-20 23:59][TRANSPORT_IA_TOTAL]
 * Implémentation Firebase Functions callable `invokeDivineOracle`.
 *
 * Contrat :
 * - Aucune clé Gemini côté client ; le backend Vertex exécute le modèle.
 * - Texte seul ou texte + images inline : même endpoint, payload aligné sur le backend.
 */
class FunctionsAiGateway(
    private val functions: FirebaseFunctions,
    private val callableTimeoutSeconds: Long = DEFAULT_CALLABLE_TIMEOUT_SECONDS
) : AiInvocationGateway {

    override suspend fun invoke(
        systemInstruction: String,
        prompt: String,
        model: String?,
        images: List<AiInvocationGateway.InlineImage>
    ): String {
        val payload = hashMapOf<String, Any>(
            "systemInstruction" to systemInstruction,
            "prompt" to prompt
        ).apply {
            if (!model.isNullOrBlank()) {
                put("model", model)
            }
            if (images.isNotEmpty()) {
                put(
                    "images",
                    images.map {
                        hashMapOf(
                            "mimeType" to it.mimeType,
                            "dataBase64" to it.dataBase64
                        )
                    }
                )
            }
        }

        return try {
            val callable = functions.getHttpsCallable("invokeDivineOracle")
            callable.setTimeout(callableTimeoutSeconds, TimeUnit.SECONDS)
            val result = callable.call(payload).await()

            val data = result.data as? Map<*, *>
                ?: throw IllegalStateException("Réponse backend invalide : data manquante")

            val text = data["text"] as? String
                ?: throw IllegalStateException("Réponse backend invalide : champ text manquant")

            if (text.isBlank()) {
                throw IllegalStateException("Réponse backend vide")
            }

            text
        } catch (e: FirebaseFunctionsException) {
            throw IllegalStateException(formatOracleTransportMessage(e), e)
        }
    }

    private fun formatOracleTransportMessage(e: FirebaseFunctionsException): String {
        val detail = e.message?.trim().orEmpty()
        val prefix = when (e.code) {
            FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                "UNAUTHENTICATED: session Firebase requise pour l'Oracle."
            FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                "INVALID_ARGUMENT"
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                "RESOURCE_EXHAUSTED"
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                "DEADLINE_EXCEEDED"
            FirebaseFunctionsException.Code.UNAVAILABLE ->
                "UNAVAILABLE"
            FirebaseFunctionsException.Code.INTERNAL ->
                "INTERNAL"
            else -> "ORACLE_FUNCTIONS_${e.code}"
        }
        return if (detail.isNotEmpty()) "$prefix: $detail" else prefix
    }

    companion object {
        private const val DEFAULT_CALLABLE_TIMEOUT_SECONDS = 120L
    }
}

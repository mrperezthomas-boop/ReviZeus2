package com.revizeus.app.ai

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * [2026-04-20 23:59][TRANSPORT_IA_TOTAL]
 * Implémentation Firebase Functions callable.
 */
class FunctionsAiGateway(
    private val functions: FirebaseFunctions
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

        val result = functions
            .getHttpsCallable("invokeDivineOracle")
            .call(payload)
            .await()

        val data = result.data as? Map<*, *>
            ?: throw IllegalStateException("Réponse backend invalide : data manquante")

        val text = data["text"] as? String
            ?: throw IllegalStateException("Réponse backend invalide : champ text manquant")

        if (text.isBlank()) {
            throw IllegalStateException("Réponse backend vide")
        }

        return text
    }
}

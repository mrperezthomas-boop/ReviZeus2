package com.revizeus.app.ai

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * [2026-04-20][TRANSPORT_ORACLE_TEXTE]
 * Implémentation Firebase Functions callable.
 *
 * Contrat backend :
 * {
 *   systemInstruction: String,
 *   prompt: String,
 *   model?: String
 * }
 *
 * Réponse attendue :
 * {
 *   text: String
 * }
 */
class FunctionsAiGateway(
    private val functions: FirebaseFunctions
) : AiInvocationGateway {

    override suspend fun invokeText(
        systemInstruction: String,
        prompt: String,
        model: String?
    ): String {
        val payload = hashMapOf(
            "systemInstruction" to systemInstruction,
            "prompt" to prompt
        ).apply {
            if (!model.isNullOrBlank()) {
                put("model", model)
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
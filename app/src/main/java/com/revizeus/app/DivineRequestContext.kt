package com.revizeus.app

// [2026-04-19 05:20][BLOC_B2][REQUEST_CONTEXT] Contrat stable de contexte d'entrée pour planification/routage divin.
data class DivineRequestContext(
    val subject: String?,
    val actionType: DivineActionType,
    val screenSource: String,
    val userAge: Int?,
    val userClassLevel: String?,
    val currentMood: String?,
    val successState: Boolean?,
    val difficulty: Int?,
    val rawInput: String?,
    val validatedSummary: String?,
    val questionText: String?,
    val userAnswer: String?,
    val correctAnswer: String?,
    val metadata: Map<String, String> = emptyMap()
)

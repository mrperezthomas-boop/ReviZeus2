package com.revizeus.app

// [2026-04-19 05:20][BLOC_B2][RESPONSE_PLAN] Contrat stable de sortie pour pilotage d'orchestration et fallback UI.
data class DivineResponsePlan(
    val personaType: DivinePersonaType,
    val actionType: DivineActionType,
    val speechMode: DivineSpeechMode,
    val godDisplayName: String,
    val promptHints: String,
    val uiHints: String,
    val shouldUseRpgDialog: Boolean,
    val shouldUseLoadingDivine: Boolean,
    val fallbackTextStyle: String
)

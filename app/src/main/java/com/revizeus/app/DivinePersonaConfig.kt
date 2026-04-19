package com.revizeus.app

// [2026-04-19 05:20][BLOC_B2][PERSONA_CONFIG] Contrat stable de configuration persona, indépendant des moteurs existants.
data class DivinePersonaConfig(
    val personaType: DivinePersonaType,
    val displayName: String,
    val coreTone: String,
    val teachingStyle: String,
    val correctionStyle: String,
    val encouragementStyle: String,
    val warmthBias: Float,
    val disciplineBias: Float,
    val maieuticBias: Float,
    val verbosityBias: Float,
    val preferredActionTypes: Set<DivineActionType>
)

package com.revizeus.app

/**
 * Snapshot adaptatif unifié du héros.
 *
 * Objectif :
 * - transporter dans un seul objet toutes les données utiles à la génération IA ;
 * - rester EXTENSIBLE sans casser les appels futurs ;
 * - séparer clairement les données réelles lues en base et les surcharges injectées
 *   par les écrans (progression aventure, niveaux de temple, équipements, etc.).
 *
 * IMPORTANT :
 * - ce snapshot n'impose aucun nouveau schéma Room ;
 * - il sert uniquement d'agrégat runtime pour les prompts ;
 * - les paramètres encore absents du projet peuvent être fournis plus tard via
 *   [futureParams] sans refaire toute l'architecture.
 */
data class PlayerAdaptiveSnapshot(
    val pseudo: String,
    val age: Int,
    val classLevel: String,
    val mood: String,
    val level: Int,
    val rank: String,
    val titleEquipped: String,
    val cognitivePattern: String,
    val logicalPrecisionScore: Float,
    val errorPatternFrequency: Float,
    val fatigueIndex: Float,
    val biasSusceptibility: Float,
    val retentionDecayRate: Float,
    val adaptabilityCoefficient: Float,
    val totalQuizDone: Int,
    val winStreak: Int,
    val dayStreak: Int,
    val totalXpEarned: Int,
    val eclatsSavoir: Int,
    val ambroisie: Int,
    val currentSubject: String?,
    val currentTopic: String?,
    val currentCourseTitle: String?,
    val currentQuestionText: String?,
    val latestScorePercent: Int?,
    val latestStars: Int?,
    val recentSuccessRate: Float?,
    val recentAverageResponseTimeMs: Long?,
    val recentErrorSubjects: List<String>,
    val weakTopics: List<String>,
    val strongTopics: List<String>,
    val currentSubjectFragmentCount: Int?,
    val templeProgressByGod: Map<String, Int>,
    val equippedItems: List<String>,
    val equippedArtifacts: List<String>,
    val explicitOutcome: String?,
    val adventureStep: String?,
    val futureParams: Map<String, String>,
    val divineAffinitySummary: String,
    val currentGodAffinitySummary: String
)

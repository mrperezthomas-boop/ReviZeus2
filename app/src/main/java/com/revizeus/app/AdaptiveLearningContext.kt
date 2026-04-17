package com.revizeus.app

data class AdaptiveLearningContext(
    val age: Int,
    val classLevel: String,
    val mood: String,
    val subject: String,
    val successRate: Int,
    val averageResponseTimeMs: Long,
    val weakTopics: List<String>,
    val performanceBand: String,
    val fragilityBand: String,
    val pacingBand: String,
    val summaryStyle: String,
    val questionStyle: String,
    val correctionStyle: String,
    val recommendedAction: String,
    val recentMistakePattern: String
) {
    fun toPromptNote(): String {
        val weakTopicsText = if (weakTopics.isEmpty()) {
            "Aucun concept faible net détecté pour l'instant"
        } else {
            weakTopics.joinToString(", ")
        }

        return """
            CONTEXTE ADAPTATIF PRIORITAIRE :
            - Matière ciblée : $subject
            - Âge : $age
            - Classe : $classLevel
            - Humeur actuelle : $mood
            - Taux de réussite estimé sur la matière : $successRate%
            - Temps moyen de réponse récent : ${averageResponseTimeMs} ms
            - Niveau global : $performanceBand
            - Fragilité pédagogique : $fragilityBand
            - Rythme conseillé : $pacingBand
            - Style de résumé attendu : $summaryStyle
            - Style de questions attendu : $questionStyle
            - Style de correction attendu : $correctionStyle
            - Concepts faibles à surveiller : $weakTopicsText
            - Pattern d'erreur récent : $recentMistakePattern
            - Action recommandée : $recommendedAction
        """.trimIndent()
    }
}

package com.revizeus.app

import com.revizeus.app.models.QuizQuestion

/**
 * ═══════════════════════════════════════════════════════════════
 * QUIZ REWARD MANAGER — RéviZeus
 * ═══════════════════════════════════════════════════════════════
 * Rôle :
 * Centralise le calcul des fragments gagnés selon le type de quiz,
 * sans écrire directement dans le profil.
 *
 * CONSERVATION :
 * - aucune écriture JSON ici
 * - aucune suppression de mécanique existante
 * - calcul uniquement, l'Activity reste responsable de l'application finale
 *
 * CORRECTIF COMPILATION :
 * - Le QuizQuestion réel actuellement utilisé dans le projet n'expose PAS
 *   de méthode effectiveSubject(fallback)
 * - Il expose seulement la propriété optionnelle subject
 * - On résout donc la matière effective localement ici, sans casser le modèle
 * ═══════════════════════════════════════════════════════════════
 */
object QuizRewardManager {

    data class RewardBreakdown(
        val fragmentsBySubject: Map<String, Int>,
        val totalFragments: Int,
        val isUltime: Boolean,
        val isTimedQuiz: Boolean
    )

    /**
     * Calcule les fragments gagnés question par question.
     *
     * Règles métier officielles :
     * - Quiz Oracle / entraînement normal : +1 fragment par bonne réponse
     * - Entraînement ultime : +3 fragments par bonne réponse
     * - Chaque gain est rattaché à la matière réelle de la question
     * - Si le quiz n'est pas chronométré, la récompense fragments est divisée par 2
     */
    fun buildRewardBreakdown(
        questions: List<QuizQuestion>,
        userAnswers: List<String>,
        fallbackQuizSubject: String,
        isUltime: Boolean,
        isTimedQuiz: Boolean
    ): RewardBreakdown {
        val fragmentsBySubject = linkedMapOf<String, Int>()
        val basePerCorrect = if (isUltime) 3 else 1

        questions.forEachIndexed { index, question ->
            val userAnswer = userAnswers.getOrNull(index).orEmpty().trim().uppercase()
            val isCorrect = userAnswer == question.normalizedCorrectAnswer()

            if (isCorrect) {
                val effectiveSubject = resolveEffectiveSubject(
                    question = question,
                    fallbackQuizSubject = fallbackQuizSubject
                )
                val current = fragmentsBySubject[effectiveSubject] ?: 0
                fragmentsBySubject[effectiveSubject] = current + basePerCorrect
            }
        }

        if (!isTimedQuiz) {
            val halved = linkedMapOf<String, Int>()
            fragmentsBySubject.forEach { (subject, amount) ->
                halved[subject] = amount / 2
            }
            val totalHalved = halved.values.sum()
            return RewardBreakdown(
                fragmentsBySubject = halved,
                totalFragments = totalHalved,
                isUltime = isUltime,
                isTimedQuiz = isTimedQuiz
            )
        }

        return RewardBreakdown(
            fragmentsBySubject = fragmentsBySubject,
            totalFragments = fragmentsBySubject.values.sum(),
            isUltime = isUltime,
            isTimedQuiz = isTimedQuiz
        )
    }

    /**
     * Résout la matière effective d'une question sans dépendre
     * d'une API absente dans le modèle actuel.
     */
    private fun resolveEffectiveSubject(
        question: QuizQuestion,
        fallbackQuizSubject: String
    ): String {
        val raw = question.subject.trim()
        return if (raw.isNotBlank()) raw else fallbackQuizSubject.ifBlank { "Savoir" }
    }
}

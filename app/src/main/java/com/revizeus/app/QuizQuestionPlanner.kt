package com.revizeus.app.core

import com.revizeus.app.models.CourseEntry
import com.revizeus.app.models.QuizQuestion

/**
 * Planificateur léger des questions pour RéviZeus.
 *
 * OBJECTIF :
 * - conserver les questions Gemini existantes
 * - leur greffer proprement une matière et un savoir source
 * - limiter les tailles de quiz selon le mode
 * - améliorer un peu la variété sans casser l'architecture
 */
object QuizQuestionPlanner {

    /**
     * Entraînement normal sur un seul savoir.
     * Cap à 30 questions comme demandé.
     */
    fun prepareSingleCourseQuestions(
        rawQuestions: List<QuizQuestion>,
        course: CourseEntry
    ): List<QuizQuestion> {
        return rawQuestions
            .filter { it.isUsable() }
            .take(30)
            .mapIndexed { index, question ->
                question.copy(
                    index = index + 1,
                    subject = course.subject,
                    courseId = course.id,
                    difficulty = inferDifficulty(index)
                )
            }
    }

    /**
     * Épreuve ultime globale : 40 questions max.
     * On répartit les métadonnées de cours en round-robin
     * pour éviter qu'un seul savoir absorbe tout le scoring.
     */
    fun prepareUltimateGlobalQuestions(
        rawQuestions: List<QuizQuestion>,
        courses: List<CourseEntry>
    ): List<QuizQuestion> {
        if (courses.isEmpty()) return rawQuestions.filter { it.isUsable() }.take(40)

        val usable = rawQuestions.filter { it.isUsable() }.take(40)
        val orderedCourses = courses
            .sortedWith(compareBy<CourseEntry> { it.subject }.thenBy { it.title })

        return usable.mapIndexed { index, question ->
            val course = orderedCourses[index % orderedCourses.size]
            question.copy(
                index = index + 1,
                subject = course.subject,
                courseId = course.id,
                difficulty = inferDifficulty(index)
            )
        }
    }

    /**
     * Épreuve ultime d'une seule matière :
     * on prend la totalité des savoirs du temple, cap à 40.
     */
    fun prepareUltimateSubjectQuestions(
        rawQuestions: List<QuizQuestion>,
        courses: List<CourseEntry>,
        subjectFallback: String
    ): List<QuizQuestion> {
        if (courses.isEmpty()) {
            return rawQuestions
                .filter { it.isUsable() }
                .take(40)
                .mapIndexed { index, question ->
                    question.copy(
                        index = index + 1,
                        subject = subjectFallback,
                        courseId = "",
                        difficulty = inferDifficulty(index)
                    )
                }
        }

        val usable = rawQuestions.filter { it.isUsable() }.take(40)
        val orderedCourses = courses.sortedBy { it.title }

        return usable.mapIndexed { index, question ->
            val course = orderedCourses[index % orderedCourses.size]
            question.copy(
                index = index + 1,
                subject = course.subject.ifBlank { subjectFallback },
                courseId = course.id,
                difficulty = inferDifficulty(index)
            )
        }
    }

    private fun inferDifficulty(index: Int): Int {
        return when (index % 3) {
            0 -> 1
            1 -> 2
            else -> 3
        }
    }
}

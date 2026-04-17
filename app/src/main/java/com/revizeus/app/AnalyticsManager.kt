package com.revizeus.app

import android.content.Context
import com.revizeus.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Manager central pour tracking et analyse ML.
 */
object AnalyticsManager {

    private lateinit var db: AppDatabase

    fun initialize(context: Context) {
        db = AppDatabase.getDatabase(context)
    }

    /**
     * Track une réponse de quiz.
     * Appeler après chaque question répondue.
     */
    suspend fun trackQuizAnswer(
        subject: String,
        questionText: String?,
        userAnswer: String,
        correctAnswer: String,
        isCorrect: Boolean,
        responseTime: Long,
        sessionId: String
    ) = withContext(Dispatchers.IO) {

        val analytics = UserAnalytics(
            userId = 1, // Mono-user pour l'instant
            subject = subject,
            topic = null, // Sera détecté par IA plus tard
            questionText = questionText,
            questionId = questionText?.take(50), // ID basique
            userAnswer = userAnswer,
            correctAnswer = correctAnswer,
            isCorrect = isCorrect,
            responseTime = responseTime,
            difficulty = 3, // Par défaut, sera adaptatif plus tard
            sessionId = sessionId,
            timestamp = System.currentTimeMillis()
        )

        db.userAnalyticsDao().insert(analytics)

        // Mettre à jour le profil de compétences
        updateSkillProfile(subject, subject) // topic = subject pour l'instant
    }

    /**
     * Met à jour le profil de compétences après une série de réponses.
     */
    private suspend fun updateSkillProfile(subject: String, topic: String) = withContext(Dispatchers.IO) {

        // Récupérer toutes les analytics pour ce sujet
        val allAnalytics = db.userAnalyticsDao().getBySubject(1, subject)

        if (allAnalytics.isEmpty()) return@withContext

        // Calculer les métriques
        val successRate = allAnalytics.count { it.isCorrect }.toFloat() / allAnalytics.size
        val avgResponseTime = allAnalytics.map { it.responseTime }.average().toLong()
        val practiceCount = allAnalytics.size

        // Calculer la variance (stabilité)
        val recent20 = allAnalytics.take(20).map { if (it.isCorrect) 1f else 0f }
        val variance = if (recent20.size >= 5) {
            val mean = recent20.average().toFloat()
            recent20.map { (it - mean) * (it - mean) }.average().toFloat()
        } else 0f

        val confidence = (1f - variance).coerceIn(0f, 1f)

        // Mastery level selon taux de réussite ET confiance
        val masteryLevel = (successRate * 0.7f + confidence * 0.3f).coerceIn(0f, 1f)

        // Besoin révision ? Si pas pratiqué depuis 7 jours OU masteryLevel < 0.6
        val lastPracticed = allAnalytics.firstOrNull()?.timestamp ?: 0L
        val daysSincePractice = (System.currentTimeMillis() - lastPracticed) / (1000 * 60 * 60 * 24)
        val needsReview = daysSincePractice > 7 || masteryLevel < 0.6f

        // Chercher profil existant
        val existing = db.userSkillProfileDao().get(1, subject, topic)

        if (existing != null) {
            // Mettre à jour
            val updated = existing.copy(
                masteryLevel = masteryLevel,
                confidence = confidence,
                lastPracticed = lastPracticed,
                practiceCount = practiceCount,
                avgResponseTime = avgResponseTime,
                successRate = successRate,
                needsReview = needsReview,
                updatedAt = System.currentTimeMillis()
            )
            db.userSkillProfileDao().update(updated)
        } else {
            // Créer nouveau
            val newProfile = UserSkillProfile(
                userId = 1,
                subject = subject,
                topic = topic,
                masteryLevel = masteryLevel,
                confidence = confidence,
                lastPracticed = lastPracticed,
                practiceCount = practiceCount,
                avgResponseTime = avgResponseTime,
                successRate = successRate,
                needsReview = needsReview
            )
            db.userSkillProfileDao().insert(newProfile)
        }
    }

    /**
     * Génère un ID de session unique.
     */
    fun generateSessionId(): String {
        return UUID.randomUUID().toString().take(8)
    }

    /**
     * Obtenir le résumé des compétences de l'utilisateur.
     */
    suspend fun getSkillsSummary(): List<UserSkillProfile> = withContext(Dispatchers.IO) {
        db.userSkillProfileDao().getAllByUser(1)
    }
}
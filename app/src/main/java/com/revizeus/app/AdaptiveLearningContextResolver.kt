package com.revizeus.app

import android.content.Context
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.UserAnalytics
import com.revizeus.app.models.UserProfile
import com.revizeus.app.models.UserSkillProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AdaptiveLearningContextResolver {

    suspend fun resolve(
        context: Context,
        subject: String,
        fallbackAge: Int = 15,
        fallbackClassLevel: String = "Terminale",
        fallbackMood: String = "Prêt"
    ): AdaptiveLearningContext = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val dao = db.iAristoteDao()
        val profile = try {
            dao.getUserProfile() ?: UserProfile(
                id = 1,
                age = fallbackAge,
                classLevel = fallbackClassLevel,
                mood = fallbackMood,
                xp = 0,
                streak = 0,
                cognitivePattern = "Standard"
            )
        } catch (_: Exception) {
            UserProfile(
                id = 1,
                age = fallbackAge,
                classLevel = fallbackClassLevel,
                mood = fallbackMood,
                xp = 0,
                streak = 0,
                cognitivePattern = "Standard"
            )
        }

        val analyticsBySubject = try {
            db.userAnalyticsDao().getBySubject(profile.id, subject)
        } catch (_: Exception) {
            emptyList()
        }

        val analytics = if (analyticsBySubject.isNotEmpty()) {
            analyticsBySubject.take(40)
        } else {
            try {
                db.userAnalyticsDao().getRecent(profile.id, 40)
            } catch (_: Exception) {
                emptyList()
            }
        }

        val skillProfiles = try {
            db.userSkillProfileDao().getAllByUser(profile.id)
                .filter { it.subject.equals(subject, ignoreCase = true) }
        } catch (_: Exception) {
            emptyList()
        }

        buildContext(profile, subject, analytics, skillProfiles)
    }

    private fun buildContext(
        profile: UserProfile,
        subject: String,
        analytics: List<UserAnalytics>,
        skillProfiles: List<UserSkillProfile>
    ): AdaptiveLearningContext {
        val successRate = if (analytics.isNotEmpty()) {
            ((analytics.count { it.isCorrect }.toFloat() / analytics.size.toFloat()) * 100f).toInt()
        } else {
            65
        }

        val avgResponseTime = analytics
            .map { it.responseTime.coerceAtLeast(0L) }
            .average()
            .toLong()
            .coerceAtLeast(3200L)

        val weakTopics = skillProfiles
            .sortedWith(compareBy<UserSkillProfile> { !it.needsReview }.thenBy { it.masteryLevel })
            .take(3)
            .map { it.topic }
            .filter { it.isNotBlank() }

        val recentMistakePattern = detectMistakePattern(analytics)
        val performanceBand = when {
            successRate >= 86 -> "solide"
            successRate >= 65 -> "intermédiaire"
            else -> "fragile"
        }

        val fragilityBand = when {
            profile.mood.contains("stress", ignoreCase = true) -> "élevée"
            profile.mood.contains("fatigu", ignoreCase = true) -> "moyenne à élevée"
            successRate < 55 -> "élevée"
            successRate < 70 -> "moyenne"
            else -> "faible"
        }

        val pacingBand = when {
            avgResponseTime >= 9000L -> "très progressif"
            avgResponseTime >= 6000L -> "progressif"
            avgResponseTime >= 4200L -> "standard"
            else -> "dynamique"
        }

        val isYoung = profile.age <= 11
        val isCollege = profile.age in 12..15
        val isLyceeOrAbove = profile.age >= 16

        val summaryStyle = when {
            isYoung || fragilityBand == "élevée" -> "phrases courtes, idées très découpées, vocabulaire simple"
            isCollege -> "résumé structuré, concret, avec idées-clés clairement séparées"
            else -> "résumé scolaire dense mais lisible, plus rigoureux et précis"
        }

        val questionStyle = when {
            isYoung || successRate < 55 -> "questions courtes, distracteurs très distincts, consignes très lisibles"
            fragilityBand == "élevée" -> "questions nettes, sans ambiguïté inutile, une idée par item"
            isLyceeOrAbove && successRate >= 80 -> "questions un peu plus exigeantes, distracteurs crédibles et plus proches"
            else -> "questions scolaires équilibrées, claires et progressives"
        }

        val correctionStyle = when {
            isYoung -> "maïeutique très guidée, une seule idée à la fois, ton rassurant"
            fragilityBand == "élevée" -> "très rassurant, reformulation courte, guidage explicite"
            successRate >= 80 -> "plus exigeant, mais toujours bref et précis"
            else -> "guidé, structuré et concret"
        }

        val recommendedAction = when {
            weakTopics.isNotEmpty() -> "Revenir sur ${weakTopics.first()} avant un nouveau quiz long"
            successRate < 55 -> "Refaire un quiz court sur une seule notion"
            successRate < 75 -> "Relire le résumé puis refaire un entraînement ciblé"
            else -> "Passer à une version légèrement plus exigeante"
        }

        return AdaptiveLearningContext(
            age = profile.age,
            classLevel = profile.classLevel,
            mood = profile.mood,
            subject = subject,
            successRate = successRate,
            averageResponseTimeMs = avgResponseTime,
            weakTopics = weakTopics,
            performanceBand = performanceBand,
            fragilityBand = fragilityBand,
            pacingBand = pacingBand,
            summaryStyle = summaryStyle,
            questionStyle = questionStyle,
            correctionStyle = correctionStyle,
            recommendedAction = recommendedAction,
            recentMistakePattern = recentMistakePattern
        )
    }

    private fun detectMistakePattern(analytics: List<UserAnalytics>): String {
        if (analytics.isEmpty()) return "Aucun historique exploitable"
        val wrong = analytics.filter { !it.isCorrect }
        if (wrong.isEmpty()) return "Performance récente propre, erreurs rares"
        val latest = wrong.take(8)
        val veryFastWrong = latest.count { it.responseTime in 1..2200L }
        val slowWrong = latest.count { it.responseTime >= 7000L }
        return when {
            veryFastWrong >= 3 -> "réponses trop rapides, probable précipitation"
            slowWrong >= 3 -> "hésitation importante, notion encore instable"
            else -> "erreurs diffuses, besoin de consolidation progressive"
        }
    }
}

package com.revizeus.app

import android.content.Context
import android.content.SharedPreferences
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.UserAnalytics
import com.revizeus.app.models.UserProfile
import com.revizeus.app.models.UserSkillProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Résout le contexte joueur réel à partir de Room, puis y fusionne les paramètres
 * fournis par l'écran appelant.
 *
 * VERSION COMPATIBILITÉ :
 * - conserve le snapshot riche PlayerAdaptiveSnapshot ;
 * - réintroduit aussi PlayerDialogueContext pour rester compatible avec les anciens
 *   appels de DialogRPGManager / AdaptiveDialogueEngine ;
 * - évite ainsi de casser les fichiers déjà intégrés dans le projet.
 */
object PlayerContextResolver {

    data class Request(
        val subjectHint: String? = null,
        val topicHint: String? = null,
        val currentCourseTitle: String? = null,
        val currentQuestionText: String? = null,
        val latestScorePercent: Int? = null,
        val latestStars: Int? = null,
        val explicitOutcome: String? = null,
        val adventureStep: String? = null,
        val templeProgressByGod: Map<String, Int> = emptyMap(),
        val equippedItems: List<String> = emptyList(),
        val equippedArtifacts: List<String> = emptyList(),
        val futureParams: Map<String, String> = emptyMap()
    )

    /**
     * Contexte léger, pensé pour l'affichage local instantané des dialogues.
     *
     * Il reste volontairement stable et extensible.
     */
    data class PlayerDialogueContext(
        val pseudo: String,
        val age: Int,
        val classLevel: String,
        val mood: String,
        val level: Int,
        val rank: String,
        val dominantWeakness: String,
        val recentSuccessRate: Float,
        val averageResponseTimeMs: Long,
        val fatigueIndex: Float,
        val needsGentleMode: Boolean,
        val needsChallengeMode: Boolean
    )

    suspend fun resolve(
        context: Context,
        request: Request
    ): PlayerAdaptiveSnapshot = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val userDao = db.iAristoteDao()
        val analyticsDao = db.userAnalyticsDao()
        val skillDao = db.userSkillProfileDao()

        val profile = userDao.getUserProfile() ?: UserProfile()
        val allRecentAnalytics = analyticsDao.getRecent(userId = 1, limit = 25)
        val subjectRecentAnalytics = request.subjectHint
            ?.takeIf { it.isNotBlank() }
            ?.let { analyticsDao.getBySubject(userId = 1, subject = it).take(12) }
            .orEmpty()
        val recentAnalytics = if (subjectRecentAnalytics.isNotEmpty()) subjectRecentAnalytics else allRecentAnalytics
        val allSkillProfiles = skillDao.getAllByUser(userId = 1)

        buildSnapshot(
            profile = profile,
            recentAnalytics = recentAnalytics,
            allSkillProfiles = allSkillProfiles,
            request = request
        )
    }

    /**
     * Surcharge de compatibilité utilisée par certains fichiers plus anciens.
     */
    suspend fun resolve(context: Context): PlayerDialogueContext = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val userDao = db.iAristoteDao()
        val analyticsDao = db.userAnalyticsDao()
        val skillDao = db.userSkillProfileDao()

        val profile = userDao.getUserProfile() ?: UserProfile()
        val recentAnalytics = analyticsDao.getRecent(userId = 1, limit = 15)
        val allSkillProfiles = skillDao.getAllByUser(userId = 1)

        buildDialogueContext(
            profile = profile,
            recentAnalytics = recentAnalytics,
            allSkillProfiles = allSkillProfiles
        )
    }

    /**
     * Version synchrone légère pour les appels de configuration UI immédiats.
     *
     * On évite tout accès Room sur le thread principal.
     * Cette méthode lit uniquement un petit fallback SharedPreferences et pose des
     * valeurs neutres si rien n'est encore disponible.
     */
    fun resolveLightweight(context: Context): PlayerDialogueContext {
        val prefs = safePrefs(context)
        val pseudo = prefs?.getString("hero_pseudo", null).orEmpty().ifBlank { "Héros" }
        val mood = prefs?.getString("hero_mood", null).orEmpty().ifBlank { "neutre" }
        val classLevel = prefs?.getString("hero_class", null).orEmpty().ifBlank { "inconnue" }
        val age = prefs?.getInt("hero_age", 14) ?: 14
        val level = prefs?.getInt("hero_level", 1) ?: 1
        val rank = prefs?.getString("hero_rank", null).orEmpty().ifBlank { "Novice" }

        return PlayerDialogueContext(
            pseudo = pseudo,
            age = age,
            classLevel = classLevel,
            mood = mood,
            level = level,
            rank = rank,
            dominantWeakness = "non déterminée",
            recentSuccessRate = 0.55f,
            averageResponseTimeMs = 6500L,
            fatigueIndex = 0.35f,
            needsGentleMode = mood.equals("fatigué", ignoreCase = true) || mood.equals("fatigue", ignoreCase = true) || mood.equals("stressé", ignoreCase = true) || mood.equals("stresse", ignoreCase = true),
            needsChallengeMode = mood.equals("joyeux", ignoreCase = true) && level >= 5
        )
    }

    private fun safePrefs(context: Context): SharedPreferences? {
        return try {
            context.getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    private fun buildSnapshot(
        profile: UserProfile,
        recentAnalytics: List<UserAnalytics>,
        allSkillProfiles: List<UserSkillProfile>,
        request: Request
    ): PlayerAdaptiveSnapshot {
        val recentSuccessRate = recentAnalytics
            .takeIf { it.isNotEmpty() }
            ?.let { list -> list.count { it.isCorrect }.toFloat() / list.size.toFloat() }

        val recentAverageResponseTimeMs = recentAnalytics
            .map { it.responseTime }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toLong()

        val errorSubjects = recentAnalytics
            .filter { !it.isCorrect }
            .mapNotNull { it.subject.takeIf(String::isNotBlank) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(4)
            .map { it.key }

        val weakTopics = allSkillProfiles
            .filter { it.masteryLevel <= 0.45f || it.successRate <= 0.45f || it.needsReview }
            .sortedWith(compareBy<UserSkillProfile> { it.masteryLevel }.thenBy { it.successRate })
            .take(5)
            .map { formatTopic(it.subject, it.topic) }

        val strongTopics = allSkillProfiles
            .filter { it.masteryLevel >= 0.75f && it.successRate >= 0.70f }
            .sortedWith(compareByDescending<UserSkillProfile> { it.masteryLevel }.thenByDescending { it.successRate })
            .take(5)
            .map { formatTopic(it.subject, it.topic) }

        val currentSubjectFragmentCount = request.subjectHint
            ?.takeIf { it.isNotBlank() }
            ?.let(profile::getFragmentCount)

        return PlayerAdaptiveSnapshot(
            pseudo = profile.pseudo,
            age = profile.age,
            classLevel = profile.classLevel,
            mood = profile.mood,
            level = profile.level,
            rank = profile.rang,
            titleEquipped = profile.titleEquipped,
            cognitivePattern = profile.cognitivePattern,
            logicalPrecisionScore = profile.logicalPrecisionScore,
            errorPatternFrequency = profile.errorPatternFrequency,
            fatigueIndex = profile.fatigueIndex,
            biasSusceptibility = profile.biasSusceptibility,
            retentionDecayRate = profile.retentionDecayRate,
            adaptabilityCoefficient = profile.adaptabilityCoefficient,
            totalQuizDone = profile.totalQuizDone,
            winStreak = profile.winStreak,
            dayStreak = profile.dayStreak,
            totalXpEarned = profile.totalXpEarned,
            eclatsSavoir = profile.eclatsSavoir,
            ambroisie = profile.ambroisie,
            currentSubject = request.subjectHint,
            currentTopic = request.topicHint,
            currentCourseTitle = request.currentCourseTitle,
            currentQuestionText = request.currentQuestionText,
            latestScorePercent = request.latestScorePercent,
            latestStars = request.latestStars,
            recentSuccessRate = recentSuccessRate,
            recentAverageResponseTimeMs = recentAverageResponseTimeMs,
            recentErrorSubjects = errorSubjects,
            weakTopics = weakTopics,
            strongTopics = strongTopics,
            currentSubjectFragmentCount = currentSubjectFragmentCount,
            templeProgressByGod = request.templeProgressByGod,
            equippedItems = request.equippedItems,
            equippedArtifacts = request.equippedArtifacts,
            explicitOutcome = request.explicitOutcome,
            adventureStep = request.adventureStep,
            futureParams = request.futureParams
        )
    }

    private fun buildDialogueContext(
        profile: UserProfile,
        recentAnalytics: List<UserAnalytics>,
        allSkillProfiles: List<UserSkillProfile>
    ): PlayerDialogueContext {
        val recentSuccessRate = if (recentAnalytics.isNotEmpty()) {
            recentAnalytics.count { it.isCorrect }.toFloat() / recentAnalytics.size.toFloat()
        } else {
            0.55f
        }

        val averageResponseTimeMs = recentAnalytics
            .map { it.responseTime }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toLong()
            ?: 6500L

        val dominantWeakness = allSkillProfiles
            .filter { it.masteryLevel <= 0.55f || it.successRate <= 0.55f || it.needsReview }
            .sortedWith(compareBy<UserSkillProfile> { it.masteryLevel }.thenBy { it.successRate })
            .firstOrNull()
            ?.let { formatTopic(it.subject, it.topic) }
            ?: "non déterminée"

        val fatigueFromProfile = profile.fatigueIndex
        val fatigueFromTiming = when {
            averageResponseTimeMs >= 9000L -> 0.80f
            averageResponseTimeMs >= 7500L -> 0.65f
            averageResponseTimeMs >= 6000L -> 0.45f
            else -> 0.25f
        }
        val fatigueIndex = ((fatigueFromProfile + fatigueFromTiming) / 2f).coerceIn(0f, 1f)

        val needsGentleMode = fatigueIndex >= 0.55f || profile.mood.equals("fatigué", ignoreCase = true) || profile.mood.equals("fatigue", ignoreCase = true) || profile.mood.equals("stressé", ignoreCase = true) || profile.mood.equals("stresse", ignoreCase = true)
        val needsChallengeMode = !needsGentleMode && recentSuccessRate >= 0.80f && profile.level >= 5

        return PlayerDialogueContext(
            pseudo = profile.pseudo.ifBlank { "Héros" },
            age = profile.age,
            classLevel = profile.classLevel.ifBlank { "inconnue" },
            mood = profile.mood.ifBlank { "neutre" },
            level = profile.level,
            rank = profile.rang.ifBlank { "Novice" },
            dominantWeakness = dominantWeakness,
            recentSuccessRate = recentSuccessRate,
            averageResponseTimeMs = averageResponseTimeMs,
            fatigueIndex = fatigueIndex,
            needsGentleMode = needsGentleMode,
            needsChallengeMode = needsChallengeMode
        )
    }

    private fun formatTopic(subject: String, topic: String): String {
        return if (topic.isBlank()) subject else "$subject → $topic"
    }
}

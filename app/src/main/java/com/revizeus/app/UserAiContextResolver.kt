package com.revizeus.app

import android.content.Context
import com.revizeus.app.models.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Source unifiée des données utilisateur utilisées par l'IA.
 * Lecture uniquement (aucune écriture DB / prefs).
 */
object UserAiContextResolver {

    enum class Source { ROOM, PREF_USER, PREF_HERO, DEFAULT }

    data class UserAiContextSources(
        val ageSource: Source,
        val classLevelSource: Source,
        val moodSource: Source,
        val pseudoSource: Source,
        val levelSource: Source,
        val rankSource: Source
    )

    data class UserAiContext(
        val age: Int,
        val classLevel: String,
        val mood: String,
        val pseudo: String,
        val level: Int,
        val rank: String,
        val sources: UserAiContextSources
    )

    suspend fun resolve(context: Context): UserAiContext = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)

        val userAgePref = prefs.getInt("USER_AGE", 0)
        val heroAgePref = prefs.getInt("hero_age", 0)
        val userClassPref = prefs.getString("USER_CLASS", null).orEmpty().trim()
        val heroClassPref = prefs.getString("hero_class", null).orEmpty().trim()
        val currentMoodPref = prefs.getString("CURRENT_MOOD", null).orEmpty().trim()
        val heroMoodPref = prefs.getString("hero_mood", null).orEmpty().trim()
        val heroPseudoPref = prefs.getString("hero_pseudo", null).orEmpty().trim()
        val heroLevelPref = prefs.getInt("hero_level", 0)
        val heroRankPref = prefs.getString("hero_rank", null).orEmpty().trim()

        val roomProfile = try {
            AppDatabase.getDatabase(context).iAristoteDao().getUserProfile()
        } catch (_: Exception) {
            null
        }

        val roomAge = roomProfile?.age ?: 0
        val roomClass = roomProfile?.classLevel.orEmpty().trim()
        val roomMood = roomProfile?.mood.orEmpty().trim()
        val roomPseudo = roomProfile?.pseudo.orEmpty().trim()
        val roomLevel = roomProfile?.level ?: 0
        val roomRank = roomProfile?.rang.orEmpty().trim()

        val (age, ageSource) = when {
            roomAge > 0 -> roomAge to Source.ROOM
            userAgePref > 0 -> userAgePref to Source.PREF_USER
            heroAgePref > 0 -> heroAgePref to Source.PREF_HERO
            else -> 15 to Source.DEFAULT
        }

        val (classLevel, classLevelSource) = when {
            roomClass.isNotBlank() -> roomClass to Source.ROOM
            userClassPref.isNotBlank() -> userClassPref to Source.PREF_USER
            heroClassPref.isNotBlank() -> heroClassPref to Source.PREF_HERO
            else -> "Terminale" to Source.DEFAULT
        }

        // Humeur active prioritaire = CURRENT_MOOD (MoodActivity écrit cette clé).
        val (mood, moodSource) = when {
            currentMoodPref.isNotBlank() -> currentMoodPref to Source.PREF_USER
            roomMood.isNotBlank() -> roomMood to Source.ROOM
            heroMoodPref.isNotBlank() -> heroMoodPref to Source.PREF_HERO
            else -> "Prêt" to Source.DEFAULT
        }

        val (pseudo, pseudoSource) = when {
            roomPseudo.isNotBlank() -> roomPseudo to Source.ROOM
            heroPseudoPref.isNotBlank() -> heroPseudoPref to Source.PREF_HERO
            else -> "Héros" to Source.DEFAULT
        }

        val (level, levelSource) = when {
            roomLevel > 0 -> roomLevel to Source.ROOM
            heroLevelPref > 0 -> heroLevelPref to Source.PREF_HERO
            else -> 1 to Source.DEFAULT
        }

        val (rank, rankSource) = when {
            roomRank.isNotBlank() -> roomRank to Source.ROOM
            heroRankPref.isNotBlank() -> heroRankPref to Source.PREF_HERO
            else -> "Mortel" to Source.DEFAULT
        }

        UserAiContext(
            age = age,
            classLevel = classLevel,
            mood = mood,
            pseudo = pseudo,
            level = level,
            rank = rank,
            sources = UserAiContextSources(
                ageSource = ageSource,
                classLevelSource = classLevelSource,
                moodSource = moodSource,
                pseudoSource = pseudoSource,
                levelSource = levelSource,
                rankSource = rankSource
            )
        )
    }

    fun resolveLightweight(context: Context): UserAiContext {
        val prefs = context.getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)

        val userAgePref = prefs.getInt("USER_AGE", 0)
        val heroAgePref = prefs.getInt("hero_age", 0)
        val userClassPref = prefs.getString("USER_CLASS", null).orEmpty().trim()
        val heroClassPref = prefs.getString("hero_class", null).orEmpty().trim()
        val currentMoodPref = prefs.getString("CURRENT_MOOD", null).orEmpty().trim()
        val heroMoodPref = prefs.getString("hero_mood", null).orEmpty().trim()
        val heroPseudoPref = prefs.getString("hero_pseudo", null).orEmpty().trim()
        val heroLevelPref = prefs.getInt("hero_level", 0)
        val heroRankPref = prefs.getString("hero_rank", null).orEmpty().trim()

        val (age, ageSource) = when {
            userAgePref > 0 -> userAgePref to Source.PREF_USER
            heroAgePref > 0 -> heroAgePref to Source.PREF_HERO
            else -> 15 to Source.DEFAULT
        }

        val (classLevel, classLevelSource) = when {
            userClassPref.isNotBlank() -> userClassPref to Source.PREF_USER
            heroClassPref.isNotBlank() -> heroClassPref to Source.PREF_HERO
            else -> "Terminale" to Source.DEFAULT
        }

        val (mood, moodSource) = when {
            currentMoodPref.isNotBlank() -> currentMoodPref to Source.PREF_USER
            heroMoodPref.isNotBlank() -> heroMoodPref to Source.PREF_HERO
            else -> "Prêt" to Source.DEFAULT
        }

        val (pseudo, pseudoSource) = when {
            heroPseudoPref.isNotBlank() -> heroPseudoPref to Source.PREF_HERO
            else -> "Héros" to Source.DEFAULT
        }

        val (level, levelSource) = when {
            heroLevelPref > 0 -> heroLevelPref to Source.PREF_HERO
            else -> 1 to Source.DEFAULT
        }

        val (rank, rankSource) = when {
            heroRankPref.isNotBlank() -> heroRankPref to Source.PREF_HERO
            else -> "Mortel" to Source.DEFAULT
        }

        return UserAiContext(
            age = age,
            classLevel = classLevel,
            mood = mood,
            pseudo = pseudo,
            level = level,
            rank = rank,
            sources = UserAiContextSources(
                ageSource = ageSource,
                classLevelSource = classLevelSource,
                moodSource = moodSource,
                pseudoSource = pseudoSource,
                levelSource = levelSource,
                rankSource = rankSource
            )
        )
    }
}

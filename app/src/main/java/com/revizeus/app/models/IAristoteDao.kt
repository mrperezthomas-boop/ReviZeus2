package com.revizeus.app.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * ============================================================
 * IAristoteDao.kt — RéviZeus v9  ✅ MULTI-COMPTES v7
 * Interface DAO Room — Moteur de persistance d'Aristote
 *
 * Tables gérées :
 *   - user_profile   → profil héros (XP, niveau, streak, avatar…)
 *   - course_entry   → cours scannés par l'Oracle
 *   - memory_score   → scores de mémorisation par concept
 *
 * AJOUT MULTI-COMPTES v7 :
 *   ✅ addPlayTimeSeconds() → cumule le temps de jeu en DB
 *      Appelé lors de la déconnexion, après
 *      AccountRegistry.endSessionAndSaveTime().
 *
 * CORRECTIONS v9 (conservées) :
 *   ✅ TOUTES les méthodes sont en "suspend fun"
 *   ✅ getUserStats() ajouté → alias de getUserProfile()
 *   ✅ saveUserProfile() ajouté → alias de insertUserProfile()
 *   ✅ getWeakConceptsBySubject() ajouté
 * ============================================================
 */
@Dao
interface IAristoteDao {

    // ══════════════════════════════════════════════════════════
    // USER PROFILE — Lecture
    // ══════════════════════════════════════════════════════════

    /**
     * Récupère le profil unique du héros.
     * Retourne null si aucun profil n'existe encore.
     * Utilisé par : BadgeManager, HeroProfileActivity, SettingsActivity
     */
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    /**
     * ✅ ALIAS — Même requête que getUserProfile().
     * Appelé par DashboardActivity → db.iAristoteDao().getUserStats()
     */
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserStats(): UserProfile?

    /**
     * Récupère le profil par son id (usage futur multi-profils).
     */
    @Query("SELECT * FROM user_profile WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): UserProfile?

    // ══════════════════════════════════════════════════════════
    // USER PROFILE — Écriture
    // ══════════════════════════════════════════════════════════

    /**
     * Insère ou remplace le profil héros.
     * Utilisé par AvatarActivity lors de la création initiale.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    /**
     * ✅ ALIAS — Même opération que insertUserProfile().
     * Appelé par AvatarActivity → dao.saveUserProfile(profile)
     * et BadgeManager → db.userDao().saveUserProfile(profile)  [via AppDatabase]
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    /**
     * Met à jour le profil héros complet.
     * Utilisé par QuizResultActivity après un quiz.
     */
    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    /**
     * Met à jour uniquement l'XP et le niveau.
     * Appelé par BadgeManager.evaluateAll() pour accorder l'XP des badges.
     */
    @Query("UPDATE user_profile SET xp = :newXp, level = :newLevel WHERE id = 1")
    suspend fun updateXpAndLevel(newXp: Int, newLevel: Int)

    /**
     * Met à jour uniquement le streak.
     */
    @Query("UPDATE user_profile SET streak = :newStreak WHERE id = 1")
    suspend fun updateStreak(newStreak: Int)

    /**
     * Met à jour l'avatar sélectionné.
     * @param avatarResName nom de la ressource drawable, ex: "m_avatar_1"
     */
    @Query("UPDATE user_profile SET avatarResName = :avatarResName WHERE id = 1")
    suspend fun updateAvatar(avatarResName: String)

    /**
     * Réinitialise la progression : XP → 0, niveau → 1, streak → 0.
     * Le pseudo et l'avatar sont conservés.
     * Utilisé par SettingsActivity (bouton "Réinitialiser Progression").
     */
    @Query("UPDATE user_profile SET xp = 0, level = 1, streak = 0 WHERE id = 1")
    suspend fun resetUserProfile()

    /**
     * ✅ NOUVEAU v9 — Met à jour le titre équipé.
     * Affiché sous le nom dans HeroProfileActivity.
     */
    @Query("UPDATE user_profile SET title_equipped = :titre WHERE id = 1")
    suspend fun updateTitreEquipe(titre: String?)

    /**
     * ✅ NOUVEAU v9 — Incrémente total_xp_earned et total_quiz_done.
     * Appelé par QuizResultActivity après chaque quiz.
     */
    @Query("""
        UPDATE user_profile 
        SET total_xp_earned = total_xp_earned + :xpGagne,
            total_quiz_done = total_quiz_done + 1
        WHERE id = 1
    """)
    suspend fun recordQuizResult(xpGagne: Int)

    /**
     * ✅ NOUVEAU v9 — Met à jour best_streak_ever si le streak dépasse le record.
     * Appelé par DashboardActivity à chaque connexion.
     */
    @Query("""
        UPDATE user_profile 
        SET best_streak_ever = CASE 
            WHEN streak > best_streak_ever THEN streak 
            ELSE best_streak_ever 
        END
        WHERE id = 1
    """)
    suspend fun updateBestStreakIfNeeded()

    /**
     * ✅ NOUVEAU v9 — Enregistre l'horodatage de la dernière connexion.
     * Appelé par DashboardActivity au démarrage.
     */
    @Query("UPDATE user_profile SET last_login_at = :timestamp WHERE id = 1")
    suspend fun updateLastLogin(timestamp: Long)

    /**
     * ✅ NOUVEAU v9 — Met à jour le pseudo uniquement.
     * Pour la feature "Modifier le pseudo" dans SettingsActivity.
     */
    @Query("UPDATE user_profile SET pseudo = :newPseudo WHERE id = 1")
    suspend fun updatePseudo(newPseudo: String)

    /**
     * ✅ MULTI-COMPTES v7 — Cumule le temps de jeu en DB.
     *
     * Appelé lors de la déconnexion dans SettingsActivity,
     * après AccountRegistry.endSessionAndSaveTime().
     * La valeur est additive : on ajoute la durée de session
     * à ce qui est déjà stocké en base.
     *
     * Exemple d'appel :
     * ```kotlin
     * val elapsed = AccountRegistry.endSessionAndSaveTime(context, uid)
     * db.iAristoteDao().addPlayTimeSeconds(elapsed)
     * ```
     *
     * ÉVOLUTION FUTURE :
     * - Déclencher un badge "Marathonien de l'Olympe" à 360 000s (100h).
     */
    @Query("UPDATE user_profile SET total_play_time_seconds = total_play_time_seconds + :seconds WHERE id = 1")
    suspend fun addPlayTimeSeconds(seconds: Long)

    // ══════════════════════════════════════════════════════════
    // COURSE ENTRY — Lecture
    // ══════════════════════════════════════════════════════════

    /**
     * Récupère tous les cours triés du plus récent au plus ancien.
     * Utilisé par SavoirActivity.
     */
    @Query("SELECT * FROM course_entry ORDER BY dateAdded DESC")
    suspend fun getAllCourses(): List<CourseEntry>

    /**
     * PACK 2 — SAVOIRS RÉCENTS
     * Récupère les N cours les plus récents.
     * @param limit Nombre de cours à récupérer (généralement 7)
     */
    @Query("SELECT * FROM course_entry ORDER BY dateAdded DESC LIMIT :limit")
    suspend fun getRecentCourses(limit: Int): List<CourseEntry>

    /**
     * Récupère les cours d'une matière donnée.
     */
    @Query("SELECT * FROM course_entry WHERE subject = :subject ORDER BY dateAdded DESC")
    suspend fun getCoursesBySubject(subject: String): List<CourseEntry>

    /**
     * Récupère les cours correspondant à plusieurs alias d'une même matière.
     */
    @Query("SELECT * FROM course_entry WHERE subject IN (:subjects) ORDER BY dateAdded DESC")
    suspend fun getCoursesBySubjects(subjects: List<String>): List<CourseEntry>

    /**
     * Compte les cours correspondant à une liste d'alias.
     */
    @Query("SELECT COUNT(*) FROM course_entry WHERE subject IN (:subjects)")
    suspend fun countCoursesBySubjects(subjects: List<String>): Int

    /**
     * Récupère un cours par son id.
     */
    @Query("SELECT * FROM course_entry WHERE id = :courseId LIMIT 1")
    suspend fun getCourseById(courseId: String): CourseEntry?

    /**
     * Compte le nombre total de cours scannés.
     * Appelé par BadgeManager.buildContext() et HeroProfileActivity.
     */
    @Query("SELECT COUNT(*) FROM course_entry")
    suspend fun countCourses(): Int

    /**
     * Récupère les matières distinctes qui ont au moins un cours.
     */
    @Query("SELECT DISTINCT subject FROM course_entry ORDER BY subject ASC")
    suspend fun getDistinctSubjects(): List<String>

    /**
     * PHASE B — JARDIN DE DÉMÉTER
     */
    @Query("UPDATE course_entry SET lastReviewedAt = :timestamp WHERE subject = :matiere")
    suspend fun updateCoursesLastReviewedBySubject(matiere: String, timestamp: Long)

    /**
     * PHASE B — JARDIN DE DÉMÉTER
     */
    @Query("SELECT * FROM course_entry WHERE lastReviewedAt < :thresholdTime LIMIT 1")
    suspend fun getFadingCourse(thresholdTime: Long): CourseEntry?

    // ══════════════════════════════════════════════════════════
    // COURSE ENTRY — Écriture
    // ══════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntry)

    @Query("DELETE FROM course_entry WHERE id = :courseId")
    suspend fun deleteCourseById(courseId: String)

    @Query("DELETE FROM course_entry")
    suspend fun deleteAllCourses()


    @Query("UPDATE course_entry SET subject = :newSubject WHERE id = :courseId")
    suspend fun updateCourseSubject(courseId: String, newSubject: String)

    @Query("UPDATE course_entry SET folder_name = :folderName WHERE id = :courseId")
    suspend fun updateCourseFolder(courseId: String, folderName: String)

    @Query("UPDATE course_entry SET subject = :newSubject, folder_name = :folderName WHERE id = :courseId")
    suspend fun updateCourseSubjectAndFolder(courseId: String, newSubject: String, folderName: String)

    @Query("UPDATE course_entry SET extractedText = :newExtractedText, custom_title = :newCustomTitle, title = :newCustomTitle WHERE id = :courseId")
    suspend fun updateCourseContentAndTitle(courseId: String, newExtractedText: String, newCustomTitle: String)

    @Query("SELECT DISTINCT folder_name FROM course_entry WHERE subject = :subject AND folder_name != '' ORDER BY folder_name COLLATE NOCASE ASC")
    suspend fun getFoldersBySubject(subject: String): List<String>


    // ══════════════════════════════════════════════════════════
    // MEMORY SCORE — Lecture
    // ══════════════════════════════════════════════════════════

    @Query("SELECT * FROM memory_score")
    suspend fun getAllMemoryScores(): List<MemoryScore>

    @Query("""
        SELECT * FROM memory_score 
        WHERE concept = :concept AND subject = :subject 
        LIMIT 1
    """)
    suspend fun getMemoryScore(concept: String, subject: String): MemoryScore?

    @Query("""
        SELECT * FROM memory_score 
        WHERE errorCount > correctCount 
        ORDER BY errorCount DESC
    """)
    suspend fun getFragileConcepts(): List<MemoryScore>

    /**
     * ✅ AJOUTÉ — Concepts faibles pour une matière donnée.
     */
    @Query("SELECT * FROM memory_score WHERE conceptId LIKE :subjectPrefix || '%' ORDER BY stabilityScore ASC")
    suspend fun getWeakConceptsBySubject(subjectPrefix: String): List<MemoryScore>

    // ══════════════════════════════════════════════════════════
    // MEMORY SCORE — Écriture
    // ══════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryScore(score: MemoryScore)

    @Update
    suspend fun updateMemoryScore(score: MemoryScore)

    @Query("DELETE FROM memory_score")
    suspend fun deleteAllMemoryScores()

    // ══════════════════════════════════════════════════════════
    // PHASE C — FORGE D'HÉPHAÏSTOS — INVENTAIRE
    // ══════════════════════════════════════════════════════════

    @Query("SELECT * FROM inventory ORDER BY id DESC")
    suspend fun getInventory(): List<InventoryItem>

    @Query("SELECT * FROM inventory WHERE name = :name LIMIT 1")
    suspend fun getInventoryItemByName(name: String): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem)

    @Update
    suspend fun updateInventoryItem(item: InventoryItem)

    @Query("SELECT * FROM inventory ORDER BY name COLLATE NOCASE ASC")
    suspend fun getInventoryOrderByName(): List<InventoryItem>

    @Query("SELECT * FROM inventory ORDER BY quantity DESC, name COLLATE NOCASE ASC")
    suspend fun getInventoryOrderByQuantity(): List<InventoryItem>

    @Query("SELECT * FROM inventory ORDER BY obtained_at DESC, id DESC")
    suspend fun getInventoryOrderByObtainedAt(): List<InventoryItem>

    @Query("SELECT * FROM inventory WHERE type = :type ORDER BY obtained_at DESC, id DESC")
    suspend fun getInventoryByType(type: String): List<InventoryItem>

    @Query("DELETE FROM inventory WHERE id = :itemId")
    suspend fun deleteInventoryItem(itemId: Int)

    @Query("SELECT SUM(quantity) FROM inventory")
    suspend fun countTotalItemQuantity(): Int?
}

/**
 * Projection Room légère pour compter les cours par libellé exact.
 */
data class SubjectCountRow(
    val subject: String,
    val count: Int
)

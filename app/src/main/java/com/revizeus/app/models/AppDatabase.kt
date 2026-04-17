package com.revizeus.app.models

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * AppDatabase — Version 9 — MULTI-HÉROS PAR EMAIL (slots)
 * ══════════════════════════════════════════════════════════════
 *
 * RÈGLE D'OR — MIGRATIONS :
 * ─────────────────────────────────────────────────────────────
 * fallbackToDestructiveMigration() est INTERDIT.
 * Toujours écrire une migration propre.
 * Prochaine migration à écrire uniquement si schéma Room modifié.
 *
 * SYSTÈME MULTI-HÉROS v8 :
 * ─────────────────────────────────────────────────────────────
 * getDatabase(context) lit FIREBASE_UID et le slot actif (via
 * AccountRegistry.getActiveSlot()) pour construire le nom du
 * fichier DB.
 *
 * Nommage des fichiers DB :
 * - Slot 1 : revizeus_database_[uid]           ← rétrocompat
 * - Slot 2 : revizeus_database_[uid]_slot2
 * - Slot 3 : revizeus_database_[uid]_slot3
 * - Aucun UID : revizeus_database               ← legacy
 *
 * Les données de chaque héros sont isolées dans leur propre DB.
 * Un changement de slot = fermeture + réouverture d'une autre DB.
 *
 * ZÉRO IMPACT SUR L'EXISTANT :
 * Tous les appelants utilisent toujours AppDatabase.getDatabase(context).
 * Ils n'ont rien à changer.
 *
 * HISTORIQUE DES MIGRATIONS :
 * v2→3 : inventory + lastReviewedAt
 * v3→4 : tables ML
 * v4→5 : monnaies, streaks, fragments
 * v5→6 : champs Firebase, parentaux
 * v6→7 : total_play_time_seconds
 * v7→8 : colonnes folder_name / custom_title
 * v8→9 : tables progression aventure (map + nodes)
 *
 * Ticket 2 (persistance aventure) :
 * - complète DAO + commentaires des entités
 * - AUCUN changement de schéma
 * - donc AUCUNE nouvelle migration nécessaire (version reste 9)
 */
@Database(
    entities = [
        UserProfile::class,
        CourseEntry::class,
        MemoryScore::class,
        UserAnalytics::class,
        UserSkillProfile::class,
        LearningRecommendation::class,
        InventoryItem::class,
        TempleAdventureMapEntity::class,
        TempleAdventureNodeProgressEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun iAristoteDao(): IAristoteDao
    abstract fun userAnalyticsDao(): UserAnalyticsDao
    abstract fun userSkillProfileDao(): UserSkillProfileDao
    abstract fun learningRecommendationDao(): LearningRecommendationDao
    abstract fun templeAdventureDao(): TempleAdventureDao

    companion object {

        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * Clé composite uid+slot pour laquelle l'instance est ouverte.
         * Permet de détecter les changements de compte OU de slot.
         */
        @Volatile private var currentDbKey: String = ""

        // ══════════════════════════════════════════════════════
        // MIGRATIONS — NE JAMAIS SUPPRIMER
        // ══════════════════════════════════════════════════════

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE course_entry ADD COLUMN lastReviewedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `inventory` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL DEFAULT '',
                    `type` TEXT NOT NULL DEFAULT '',
                    `description` TEXT NOT NULL DEFAULT '',
                    `image_res_name` TEXT NOT NULL DEFAULT '',
                    `quantity` INTEGER NOT NULL DEFAULT 0,
                    `obtained_at` INTEGER NOT NULL DEFAULT 0
                )""")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `user_analytics` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `event_type` TEXT NOT NULL DEFAULT '',
                    `event_data` TEXT NOT NULL DEFAULT '',
                    `timestamp` INTEGER NOT NULL DEFAULT 0
                )""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `user_skill_profile` (
                    `id` INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                    `skills_json` TEXT NOT NULL DEFAULT '{}'
                )""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `learning_recommendation` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `subject` TEXT NOT NULL DEFAULT '',
                    `recommendation` TEXT NOT NULL DEFAULT '',
                    `generated_at` INTEGER NOT NULL DEFAULT 0
                )""")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN eclats_savoir INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN ambroisie INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN day_streak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN best_day_streak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN win_streak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN best_win_streak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN last_login_day_key TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN last_win_quiz_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN knowledge_fragments TEXT NOT NULL DEFAULT '{}'")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN account_email TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN recovery_email TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN firebase_uid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN is_email_verified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN parent_email TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN is_parent_summary_enabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN last_weekly_summary_sent_at INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN total_play_time_seconds INTEGER NOT NULL DEFAULT 0")
            }
        }


        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE course_entry ADD COLUMN folder_name TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE course_entry ADD COLUMN custom_title TEXT NOT NULL DEFAULT ''")
            }
        }


        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tables de progression aventure.
                // visual_state_json / metadata_json sont volontairement génériques
                // pour stocker plus tard des références logiques de ressources
                // (icônes world map, backgrounds temple map, overlays, particules, etc.)
                // sans embarquer de logique UI dans Room.
                db.execSQL("CREATE TABLE IF NOT EXISTS `temple_adventure_map` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subject` TEXT NOT NULL DEFAULT '', `god_id` TEXT NOT NULL DEFAULT '', `temple_level` INTEGER NOT NULL DEFAULT 0, `map_index` INTEGER NOT NULL DEFAULT 0, `is_unlocked` INTEGER NOT NULL DEFAULT 0, `is_completed` INTEGER NOT NULL DEFAULT 0, `completion_percent` INTEGER NOT NULL DEFAULT 0, `last_played_at` INTEGER NOT NULL DEFAULT 0, `visual_state_json` TEXT NOT NULL DEFAULT '', `metadata_json` TEXT NOT NULL DEFAULT '')")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_temple_adventure_map_subject_god_id_temple_level_map_index` ON `temple_adventure_map` (`subject`, `god_id`, `temple_level`, `map_index`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `temple_adventure_node_progress` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subject` TEXT NOT NULL DEFAULT '', `god_id` TEXT NOT NULL DEFAULT '', `temple_level` INTEGER NOT NULL DEFAULT 0, `map_index` INTEGER NOT NULL DEFAULT 0, `node_id` TEXT NOT NULL DEFAULT '', `node_type` TEXT NOT NULL DEFAULT '', `is_unlocked` INTEGER NOT NULL DEFAULT 0, `is_completed` INTEGER NOT NULL DEFAULT 0, `completion_count` INTEGER NOT NULL DEFAULT 0, `last_played_at` INTEGER NOT NULL DEFAULT 0, `best_result` INTEGER NOT NULL DEFAULT 0, `reward_claimed_state_json` TEXT NOT NULL DEFAULT '', `rare_state_locked` INTEGER NOT NULL DEFAULT 0, `metadata_json` TEXT NOT NULL DEFAULT '')")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_temple_adventure_node_progress_subject_god_id_temple_level_map_index_node_id` ON `temple_adventure_node_progress` (`subject`, `god_id`, `temple_level`, `map_index`, `node_id`)")
            }
        }

        // ══════════════════════════════════════════════════════
        // GETDATABASE — MULTI-DB PAR UID + SLOT
        // ══════════════════════════════════════════════════════

        /**
         * Retourne la DB du héros actif.
         *
         * Lit FIREBASE_UID dans "ReviZeusPrefs" et le slot actif
         * dans AccountRegistry pour construire le nom du fichier DB.
         *
         * Si l'UID ou le slot change, l'ancienne instance est fermée
         * proprement avant d'ouvrir la nouvelle.
         */
        fun getDatabase(context: Context): AppDatabase {
            val prefs = context.getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
            val uid   = prefs.getString("FIREBASE_UID", "") ?: ""

            // Lire le slot actif via AccountRegistry
            // Import évité pour ne pas créer de dépendance circulaire :
            // on lit directement les SharedPrefs du registre.
            val slot = context
                .getSharedPreferences("ReviZeusAccounts", Context.MODE_PRIVATE)
                .getInt("active_slot_$uid", 1)
                .coerceIn(1, 3)

            val dbKey = "${uid}_slot$slot"

            if (INSTANCE != null && currentDbKey == dbKey) return INSTANCE!!

            return synchronized(this) {
                if (INSTANCE != null && currentDbKey == dbKey) return@synchronized INSTANCE!!

                try { INSTANCE?.close() } catch (_: Exception) {}

                // Nommage rétrocompatible :
                // Slot 1 = revizeus_database_[uid] (pas de suffixe slot)
                val dbName = when {
                    uid.isBlank()  -> "revizeus_database"
                    slot == 1      -> "revizeus_database_$uid"
                    else           -> "revizeus_database_${uid}_slot$slot"
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName
                )
                    .addMigrations(
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .build()

                INSTANCE     = instance
                currentDbKey = dbKey
                instance
            }
        }

        /**
         * Réinitialise le singleton.
         * Appeler lors de la déconnexion (SettingsActivity)
         * ou d'un changement de slot (HeroSelectActivity).
         */
        fun resetInstance() {
            synchronized(this) {
                try { INSTANCE?.close() } catch (_: Exception) {}
                INSTANCE     = null
                currentDbKey = ""
            }
        }
    }
}

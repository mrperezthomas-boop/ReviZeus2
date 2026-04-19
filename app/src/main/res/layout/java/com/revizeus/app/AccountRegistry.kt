package com.revizeus.app

import android.content.Context
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.UserProfile
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File

/**
 * AccountRegistry.kt — RéviZeus — Multi-Comptes + Multi-Héros
 *
 * Correctif ciblé :
 * - expose le détail des fragments pour item_account_card
 * - conserve l'architecture existante
 * - stocke un JSON de détail par dieu, même si les valeurs sont à 0
 */
object AccountRegistry {

    const val MAX_ACCOUNTS      = 5
    const val MAX_SLOTS_PER_UID = 3

    private const val PREFS_NAME          = "ReviZeusAccounts"
    private const val KEY_UIDS            = "registered_uids"
    private const val KEY_SESSION_START   = "session_start_ts"
    private const val KEY_CACHE_PREFIX    = "account_cache_"
    private const val KEY_ACTIVE_SLOT_PRE = "active_slot_"
    private const val KEY_EMAIL_PREFIX    = "account_email_"

    data class AccountCache(
        val uid: String,
        val pseudo: String,
        val level: Int,
        val avatarResName: String,
        val totalPlayTimeSeconds: Long,
        val eclatsSavoir: Int,
        val ambroisie: Int,
        val totalQuizDone: Int,
        val totalCoursScanned: Int,
        val totalFragments: Int = 0,
        val fragmentDetailsJson: String = "{}",
        val slotNumber: Int = 1
    )

    data class LocalHeroSummary(
        val pseudo: String,
        val slotNumber: Int,
        val level: Int,
        val avatarResName: String
    )

    data class LocalAccountSummary(
        val uid: String,
        val email: String,
        val heroes: List<LocalHeroSummary>
    )

    fun registerUid(context: Context, uid: String) {
        if (uid.isBlank()) return
        val existing = getRegisteredUids(context).toMutableSet()
        existing.add(uid)
        prefs(context).edit()
            .putString(KEY_UIDS, existing.joinToString(","))
            .apply()
    }

    fun getRegisteredUids(context: Context): List<String> {
        val p = prefs(context)
        val merged = linkedSetOf<String>()

        val raw = p.getString(KEY_UIDS, "") ?: ""
        if (raw.isNotBlank()) {
            raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { merged.add(it) }
        }

        p.all.keys.forEach { key ->
            when {
                key.startsWith(KEY_CACHE_PREFIX) -> {
                    val suffix = key.removePrefix(KEY_CACHE_PREFIX)
                    val uid = suffix.substringBefore("_slot").trim()
                    if (uid.isNotBlank()) merged.add(uid)
                }
                key.startsWith(KEY_EMAIL_PREFIX) -> {
                    val uid = key.removePrefix(KEY_EMAIL_PREFIX).trim()
                    if (uid.isNotBlank()) merged.add(uid)
                }
                key.startsWith(KEY_ACTIVE_SLOT_PRE) -> {
                    val uid = key.removePrefix(KEY_ACTIVE_SLOT_PRE).trim()
                    if (uid.isNotBlank()) merged.add(uid)
                }
            }
        }

        return merged.toList()
    }

    fun hasAnyAccount(context: Context): Boolean = getRegisteredUids(context).isNotEmpty()
    fun hasReachedMaxAccounts(context: Context): Boolean = getRegisteredUids(context).size >= MAX_ACCOUNTS
    fun isRegistered(context: Context, uid: String): Boolean = uid.isNotBlank() && getRegisteredUids(context).contains(uid)

    fun removeUid(context: Context, uid: String) {
        if (uid.isBlank()) return
        val existing = getRegisteredUids(context).toMutableList()
        existing.remove(uid)
        val editor = prefs(context).edit()
            .putString(KEY_UIDS, existing.joinToString(","))
            .remove("$KEY_ACTIVE_SLOT_PRE$uid")
            .remove("$KEY_EMAIL_PREFIX$uid")
        for (slot in 1..MAX_SLOTS_PER_UID) {
            editor.remove(cacheKey(uid, slot))
        }
        editor.apply()
    }

    fun rememberAccountEmail(context: Context, uid: String, email: String) {
        if (uid.isBlank()) return
        prefs(context).edit().putString("$KEY_EMAIL_PREFIX$uid", email.trim()).apply()
        registerUid(context, uid)
    }

    fun getRememberedAccountEmail(context: Context, uid: String): String =
        prefs(context).getString("$KEY_EMAIL_PREFIX$uid", "") ?: ""

    fun getActiveSlot(context: Context, uid: String): Int {
        if (uid.isBlank()) return 1
        return prefs(context).getInt("$KEY_ACTIVE_SLOT_PRE$uid", 1)
    }

    fun setActiveSlot(context: Context, uid: String, slot: Int) {
        if (uid.isBlank()) return
        prefs(context).edit()
            .putInt("$KEY_ACTIVE_SLOT_PRE$uid", slot.coerceIn(1, MAX_SLOTS_PER_UID))
            .apply()
    }

    fun findNextFreeSlot(context: Context, uid: String): Int? {
        for (slot in 1..MAX_SLOTS_PER_UID) {
            if (getSlotCache(context, uid, slot) == null) return slot
        }
        return null
    }

    fun hasAvailableSlot(context: Context, uid: String): Boolean = findNextFreeSlot(context, uid) != null
    fun countSlotsForUid(context: Context, uid: String): Int = (1..MAX_SLOTS_PER_UID).count { getSlotCache(context, uid, it) != null }

    fun setActiveUid(context: Context, uid: String) {
        context.getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString("FIREBASE_UID", uid)
            .apply()
        startSession(context)
    }

    fun getActiveUid(context: Context): String =
        context.getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
            .getString("FIREBASE_UID", "") ?: ""

    fun startSession(context: Context) {
        prefs(context).edit().putLong(KEY_SESSION_START, System.currentTimeMillis()).apply()
    }

    fun endSessionAndSaveTime(context: Context, uid: String): Long {
        if (uid.isBlank()) return 0L
        val p = prefs(context)
        val sessionStart = p.getLong(KEY_SESSION_START, 0L)
        val elapsed = if (sessionStart > 0L) (System.currentTimeMillis() - sessionStart) / 1000L else 0L
        if (elapsed > 0L) {
            val slot = getActiveSlot(context, uid)
            val cache = getSlotCache(context, uid, slot)
            if (cache != null) {
                saveSlotCache(
                    context,
                    uid,
                    slot,
                    cache.copy(totalPlayTimeSeconds = cache.totalPlayTimeSeconds + elapsed)
                )
            }
        }
        p.edit().putLong(KEY_SESSION_START, 0L).apply()
        return elapsed
    }

    fun saveSlotCache(context: Context, uid: String, slot: Int, cache: AccountCache) {
        if (uid.isBlank()) return
        try {
            val json = JSONObject().apply {
                put("uid", cache.uid)
                put("pseudo", cache.pseudo)
                put("level", cache.level)
                put("avatarResName", cache.avatarResName)
                put("totalPlayTimeSeconds", cache.totalPlayTimeSeconds)
                put("eclatsSavoir", cache.eclatsSavoir)
                put("ambroisie", cache.ambroisie)
                put("totalQuizDone", cache.totalQuizDone)
                put("totalCoursScanned", cache.totalCoursScanned)
                put("totalFragments", cache.totalFragments)
                put("fragmentDetailsJson", cache.fragmentDetailsJson)
                put("slotNumber", slot)
            }
            prefs(context).edit().putString(cacheKey(uid, slot), json.toString()).apply()
            registerUid(context, uid)
            setActiveSlot(context, uid, slot)
        } catch (_: Exception) {
        }
    }

    fun saveAccountCache(context: Context, cache: AccountCache) {
        saveSlotCache(context, cache.uid, cache.slotNumber.coerceIn(1, MAX_SLOTS_PER_UID), cache)
    }

    fun getSlotCache(context: Context, uid: String, slot: Int): AccountCache? {
        val raw = prefs(context).getString(cacheKey(uid, slot), null) ?: return null
        return parseCache(raw, uid, slot)
    }

    fun getAccountCache(context: Context, uid: String): AccountCache? {
        val slot = getActiveSlot(context, uid)
        return getSlotCache(context, uid, slot)
    }

    fun getAllHeroesCaches(context: Context): List<AccountCache> {
        val result = mutableListOf<AccountCache>()
        rebuildAllMissingCachesFromRoom(context)
        for (uid in getRegisteredUids(context)) {
            for (slot in 1..MAX_SLOTS_PER_UID) {
                getSlotCache(context, uid, slot)?.let { result.add(it) }
            }
        }
        return result
    }

    fun getAllAccountCaches(context: Context): List<AccountCache> =
        getRegisteredUids(context).mapNotNull { uid -> getAccountCache(context, uid) }

    fun getSlotsForUid(context: Context, uid: String): Map<Int, AccountCache?> =
        (1..MAX_SLOTS_PER_UID).associateWith { slot -> getSlotCache(context, uid, slot) }

    fun ensureSlotsForUid(context: Context, uid: String): Map<Int, AccountCache?> {
        rebuildMissingCachesFromRoom(context, uid)
        return getSlotsForUid(context, uid)
    }

    fun updateCacheFromProfile(context: Context, uid: String, profile: UserProfile, totalCoursScanned: Int) {
        if (uid.isBlank()) return
        val slot = getActiveSlot(context, uid)
        val existing = getSlotCache(context, uid, slot)
        val playTime = maxOf(existing?.totalPlayTimeSeconds ?: 0L, profile.totalPlayTimeSeconds)
        val fragmentDetailsJson = buildFragmentDetailsJson(profile)
        val totalFragments = computeTotalFromFragmentJson(fragmentDetailsJson)

        saveSlotCache(
            context,
            uid,
            slot,
            AccountCache(
                uid = uid,
                pseudo = profile.pseudo,
                level = profile.level,
                avatarResName = profile.avatarResName,
                totalPlayTimeSeconds = playTime,
                eclatsSavoir = profile.eclatsSavoir,
                ambroisie = profile.ambroisie,
                totalQuizDone = profile.totalQuizDone,
                totalCoursScanned = totalCoursScanned,
                totalFragments = totalFragments,
                fragmentDetailsJson = fragmentDetailsJson,
                slotNumber = slot
            )
        )

        if (profile.accountEmail.isNotBlank()) {
            rememberAccountEmail(context, uid, profile.accountEmail)
        }
    }

    fun getLocalAccountSummaries(context: Context): List<LocalAccountSummary> {
        rebuildAllMissingCachesFromRoom(context)
        return getRegisteredUids(context).mapNotNull { uid ->
            val heroes = (1..MAX_SLOTS_PER_UID).mapNotNull { slot ->
                getSlotCache(context, uid, slot)?.let {
                    LocalHeroSummary(
                        pseudo = it.pseudo,
                        slotNumber = slot,
                        level = it.level,
                        avatarResName = it.avatarResName
                    )
                }
            }
            if (heroes.isEmpty()) null else LocalAccountSummary(
                uid = uid,
                email = getRememberedAccountEmail(context, uid),
                heroes = heroes.sortedBy { it.slotNumber }
            )
        }.sortedBy { it.email.ifBlank { it.uid } }
    }

    fun deleteHeroLocal(context: Context, uid: String, slot: Int) {
        if (uid.isBlank()) return
        AppDatabase.resetInstance()
        prefs(context).edit().remove(cacheKey(uid, slot)).apply()
        deleteSlotDatabaseFiles(context, uid, slot)

        val remaining = countSlotsForUid(context, uid)
        if (remaining <= 0) {
            removeUid(context, uid)
        } else if (getActiveSlot(context, uid) == slot) {
            val fallback = (1..MAX_SLOTS_PER_UID).firstOrNull { getSlotCache(context, uid, it) != null } ?: 1
            setActiveSlot(context, uid, fallback)
        }
    }

    fun deleteAccountLocal(context: Context, uid: String) {
        if (uid.isBlank()) return
        AppDatabase.resetInstance()
        for (slot in 1..MAX_SLOTS_PER_UID) {
            prefs(context).edit().remove(cacheKey(uid, slot)).apply()
            deleteSlotDatabaseFiles(context, uid, slot)
        }
        removeUid(context, uid)

        val activeUid = getActiveUid(context)
        if (activeUid == uid) {
            context.getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE).edit()
                .remove("ACCOUNT_EMAIL")
                .remove("RECOVERY_EMAIL")
                .remove("FIREBASE_UID")
                .remove("IS_REGISTERED")
                .remove("IS_EMAIL_VERIFIED")
                .apply()
        }
    }

    fun rebuildMissingCachesFromRoom(context: Context, uid: String) {
        if (uid.isBlank()) return
        for (slot in 1..MAX_SLOTS_PER_UID) {
            if (getSlotCache(context, uid, slot) != null) continue
            val rebuilt = readProfileForSlot(context, uid, slot) ?: continue
            saveSlotCache(context, uid, slot, rebuilt.second)
            if (rebuilt.first.accountEmail.isNotBlank()) {
                rememberAccountEmail(context, uid, rebuilt.first.accountEmail)
            }
        }
    }

    fun rebuildAllMissingCachesFromRoom(context: Context) {
        getRegisteredUids(context).forEach { uid -> rebuildMissingCachesFromRoom(context, uid) }
    }

    fun formatPlayTime(totalSeconds: Long): String = when {
        totalSeconds >= 3600 -> "${totalSeconds / 3600}h ${(totalSeconds % 3600) / 60}min"
        totalSeconds >= 60 -> "${totalSeconds / 60}min"
        totalSeconds > 0 -> "< 1min"
        else -> "—"
    }

    private fun readProfileForSlot(context: Context, uid: String, slot: Int): Pair<UserProfile, AccountCache>? {
        val mainPrefs = context.getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        val previousUid = mainPrefs.getString("FIREBASE_UID", "") ?: ""
        val previousSlot = getActiveSlot(context, uid)

        return try {
            mainPrefs.edit().putString("FIREBASE_UID", uid).apply()
            setActiveSlot(context, uid, slot)
            AppDatabase.resetInstance()

            val db = AppDatabase.getDatabase(context)
            val profile = runBlocking { db.iAristoteDao().getUserProfile() } ?: return null
            val count = runBlocking { db.iAristoteDao().countCourses() }
            val fragmentDetailsJson = buildFragmentDetailsJson(profile)
            val totalFragments = computeTotalFromFragmentJson(fragmentDetailsJson)

            val cache = AccountCache(
                uid = uid,
                pseudo = profile.pseudo,
                level = profile.level,
                avatarResName = profile.avatarResName,
                totalPlayTimeSeconds = profile.totalPlayTimeSeconds,
                eclatsSavoir = profile.eclatsSavoir,
                ambroisie = profile.ambroisie,
                totalQuizDone = profile.totalQuizDone,
                totalCoursScanned = count,
                totalFragments = totalFragments,
                fragmentDetailsJson = fragmentDetailsJson,
                slotNumber = slot
            )
            profile to cache
        } catch (_: Exception) {
            null
        } finally {
            mainPrefs.edit().putString("FIREBASE_UID", previousUid).apply()
            if (uid.isNotBlank()) setActiveSlot(context, uid, previousSlot)
            AppDatabase.resetInstance()
        }
    }

    private fun buildFragmentDetailsJson(profile: UserProfile): String {
        val base = linkedMapOf(
            "zeus" to 0,
            "athena" to 0,
            "poseidon" to 0,
            "ares" to 0,
            "aphrodite" to 0,
            "hermes" to 0,
            "demeter" to 0,
            "hephaistos" to 0,
            "apollon" to 0,
            "promethee" to 0
        )

        try {
            val raw = profile.knowledgeFragments
            if (raw.isNotBlank()) {
                val json = JSONObject(raw)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val mapped = mapFragmentKeyToGod(key)
                    base[mapped] = (base[mapped] ?: 0) + json.optInt(key, 0).coerceAtLeast(0)
                }
            }
        } catch (_: Exception) {
        }

        return JSONObject(base as Map<*, *>).toString()
    }

    private fun computeTotalFromFragmentJson(raw: String): Int {
        return try {
            val json = JSONObject(raw)
            var total = 0
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                total += json.optInt(key, 0).coerceAtLeast(0)
            }
            total
        } catch (_: Exception) {
            0
        }
    }

    private fun mapFragmentKeyToGod(rawKey: String): String {
        val key = rawKey.trim().lowercase()
        return when {
            "zeus" in key || "math" in key || "mathem" in key -> "zeus"
            "athena" in key || "fran" in key || "litter" in key || "gramma" in key -> "athena"
            "poseidon" in key || "svt" in key || "bio" in key || "vivant" in key -> "poseidon"
            "ares" in key || "hist" in key -> "ares"
            "aphrodite" in key || "art" in key || "musique" in key -> "aphrodite"
            "hermes" in key || "langue" in key || "anglais" in key || "espagnol" in key || "allemand" in key -> "hermes"
            "demeter" in key || "geo" in key || "géographie" in key -> "demeter"
            "hephaistos" in key || "phys" in key || "chim" in key -> "hephaistos"
            "apollon" in key || "philo" in key || "ses" in key -> "apollon"
            "promethee" in key || "projet" in key || "vie" in key -> "promethee"
            else -> "zeus"
        }
    }

    private fun deleteSlotDatabaseFiles(context: Context, uid: String, slot: Int) {
        val dbName = when {
            uid.isBlank() -> return
            slot == 1 -> "revizeus_database_$uid"
            else -> "revizeus_database_${uid}_slot$slot"
        }
        val dbFile = context.getDatabasePath(dbName)
        File(dbFile.absolutePath).delete()
        File(dbFile.absolutePath + "-wal").delete()
        File(dbFile.absolutePath + "-shm").delete()
        File(dbFile.absolutePath + "-journal").delete()
    }

    private fun cacheKey(uid: String, slot: Int): String =
        if (slot == 1) "$KEY_CACHE_PREFIX$uid" else "$KEY_CACHE_PREFIX${uid}_slot$slot"

    private fun parseCache(raw: String, uid: String, slot: Int): AccountCache? = try {
        val j = JSONObject(raw)
        AccountCache(
            uid = j.optString("uid", uid),
            pseudo = j.optString("pseudo", "Héros"),
            level = j.optInt("level", 1),
            avatarResName = j.optString("avatarResName", "avatar_hero1"),
            totalPlayTimeSeconds = j.optLong("totalPlayTimeSeconds", 0L),
            eclatsSavoir = j.optInt("eclatsSavoir", 0),
            ambroisie = j.optInt("ambroisie", 0),
            totalQuizDone = j.optInt("totalQuizDone", 0),
            totalCoursScanned = j.optInt("totalCoursScanned", 0),
            totalFragments = j.optInt("totalFragments", 0),
            fragmentDetailsJson = j.optString("fragmentDetailsJson", "{}"),
            slotNumber = j.optInt("slotNumber", slot)
        )
    } catch (_: Exception) {
        null
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

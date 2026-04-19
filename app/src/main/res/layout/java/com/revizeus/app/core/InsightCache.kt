package com.revizeus.app.core

import android.content.Context
import android.util.Log
import com.revizeus.app.models.UserInsight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════
 * INSIGHT CACHE — Système de cache pour performance
 * ═══════════════════════════════════════════════════════════════
 * 
 * BLOC 3C — OPTIMISATION PERFORMANCE
 * 
 * Rôle :
 * Évite de recalculer les insights à chaque affichage du dashboard.
 * Cache les résultats pendant 30 minutes ou jusqu'à invalidation.
 * 
 * Principe :
 * - Cache en mémoire (pas de persistence)
 * - Thread-safe (Mutex)
 * - Invalidation automatique après timeout
 * - Invalidation manuelle après quiz
 * 
 * ═══════════════════════════════════════════════════════════════
 */
object InsightCache {
    
    private const val TAG = "INSIGHT_CACHE"
    
    /** Durée de validité du cache (30 minutes) */
    private const val CACHE_VALIDITY_MS = 30L * 60 * 1000
    
    /** Cache des insights par utilisateur */
    private data class CacheEntry(
        val insights: List<UserInsight>,
        val timestamp: Long
    )
    
    private val cache = mutableMapOf<Int, CacheEntry>()
    private val mutex = Mutex()
    
    /**
     * Récupère les insights (depuis cache ou calcul).
     * 
     * @param context Context Android
     * @param userId ID utilisateur
     * @param forceRefresh Force le recalcul même si cache valide
     * @return Liste d'insights
     */
    suspend fun getInsights(
        context: Context,
        userId: Int = 1,
        forceRefresh: Boolean = false
    ): List<UserInsight> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val cached = cache[userId]
            
            // Vérifier validité cache
            if (!forceRefresh && cached != null) {
                val age = now - cached.timestamp
                if (age < CACHE_VALIDITY_MS) {
                    Log.d(TAG, "Cache HIT pour userId=$userId (age: ${age / 1000}s)")
                    return@withContext cached.insights
                } else {
                    Log.d(TAG, "Cache EXPIRED pour userId=$userId (age: ${age / 1000}s)")
                }
            }
            
            // Cache MISS ou expiré → recalculer
            Log.d(TAG, "Cache MISS pour userId=$userId, calcul en cours...")
            
            val insights = try {
                UserAnalyticsEngine.analyzeUser(
                    context = context,
                    subject = null,  // Toutes matières
                    userId = userId,
                    recentOnly = true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erreur calcul insights", e)
                emptyList()
            }
            
            // Sauvegarder en cache
            cache[userId] = CacheEntry(
                insights = insights,
                timestamp = now
            )
            
            Log.d(TAG, "Cache STORED pour userId=$userId (${insights.size} insights)")
            
            return@withContext insights
        }
    }
    
    /**
     * Invalide le cache pour un utilisateur.
     * À appeler après chaque quiz terminé.
     */
    suspend fun invalidate(userId: Int = 1) {
        mutex.withLock {
            cache.remove(userId)
            Log.d(TAG, "Cache INVALIDATED pour userId=$userId")
        }
    }
    
    /**
     * Vide complètement le cache.
     */
    suspend fun clear() {
        mutex.withLock {
            val size = cache.size
            cache.clear()
            Log.d(TAG, "Cache CLEARED ($size entrées supprimées)")
        }
    }
    
    /**
     * Retourne les stats du cache (debug).
     */
    suspend fun getStats(): String {
        return mutex.withLock {
            val now = System.currentTimeMillis()
            val entries = cache.map { (userId, entry) ->
                val age = (now - entry.timestamp) / 1000
                "User $userId: ${entry.insights.size} insights (age: ${age}s)"
            }
            
            """
            Cache Stats:
            - Entrées: ${cache.size}
            ${entries.joinToString("\n")}
            """.trimIndent()
        }
    }
}

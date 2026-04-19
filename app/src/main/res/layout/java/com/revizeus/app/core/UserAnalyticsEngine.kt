package com.revizeus.app.core

import android.content.Context
import android.util.Log
import com.revizeus.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * ═══════════════════════════════════════════════════════════════
 * USER ANALYTICS ENGINE — Moteur d'Intelligence IA
 * ═══════════════════════════════════════════════════════════════
 * 
 * Rôle :
 * Transforme les analytics brutes (UserAnalytics, UserSkillProfile, MemoryScore)
 * en signaux exploitables (UserInsight) pour :
 * - Ajuster la difficulté des quiz
 * - Cibler les faiblesses dans les entraînements
 * - Générer des verdicts personnalisés
 * - Détecter les patterns d'apprentissage
 * 
 * Principe :
 * - Lecture des données brutes depuis Room Database
 * - Analyse statistique et détection de patterns
 * - Génération de signaux actionnables (UserInsight)
 * - Classification par sévérité (0.0 à 1.0)
 * 
 * BLOC 3A — EXPLOITER USERANALYTICS
 * 
 * Détections implémentées :
 * 1. Faiblesses thématiques (erreurs fréquentes)
 * 2. Problèmes de vitesse (lenteur, précipitation)
 * 3. Confusion entre notions (patterns d'erreurs)
 * 4. Évolution temporelle (progression/régression)
 * 5. Instabilité cognitive (variance élevée)
 * 6. Besoin de pause (fatigue détectée)
 * 
 * Utilisation :
 * ```kotlin
 * val insights = UserAnalyticsEngine.analyzeUser(context, subject = "Mathématiques")
 * insights.filter { it.isActionRequired() }.forEach { ... }
 * ```
 * 
 * ═══════════════════════════════════════════════════════════════
 */
object UserAnalyticsEngine {
    
    private const val TAG = "USER_ANALYTICS_ENGINE"
    
    /** Nombre minimum de données pour analyse fiable */
    private const val MIN_SAMPLE_SIZE = 5
    
    /** Fenêtre temporelle pour analyse récente (7 jours) */
    private const val RECENT_WINDOW_MS = 7L * 24 * 60 * 60 * 1000
    
    /** Seuil taux d'erreur pour détecter faiblesse */
    private const val WEAKNESS_ERROR_RATE = 0.4f  // 40% d'erreurs
    
    /** Seuil temps de réponse pour détecter lenteur (en ms) */
    private const val SLOW_RESPONSE_THRESHOLD = 8000L  // 8 secondes
    
    /** Seuil temps de réponse pour détecter précipitation (en ms) */
    private const val RUSHING_THRESHOLD = 2000L  // 2 secondes
    
    /** Seuil variance pour détecter instabilité */
    private const val INSTABILITY_VARIANCE_THRESHOLD = 0.25f
    
    /**
     * Analyse complète de l'utilisateur.
     * 
     * @param context Context Android
     * @param subject Matière à analyser (null = toutes matières)
     * @param userId ID utilisateur (défaut = 1)
     * @param recentOnly Analyser seulement les 7 derniers jours
     * @return Liste de UserInsight détectés
     */
    suspend fun analyzeUser(
        context: Context,
        subject: String? = null,
        userId: Int = 1,
        recentOnly: Boolean = true
    ): List<UserInsight> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Démarrage analyse utilisateur $userId${subject?.let { " — Matière: $it" } ?: ""}")
            
            val db = AppDatabase.getDatabase(context)
            val insights = mutableListOf<UserInsight>()
            
            // Récupérer les analytics
            val analytics = if (subject != null) {
                db.userAnalyticsDao().getBySubject(userId, subject)
            } else {
                db.userAnalyticsDao().getRecent(userId, 200)
            }
            
            if (analytics.isEmpty()) {
                Log.d(TAG, "Aucune analytics disponible pour analyse")
                return@withContext emptyList()
            }
            
            // Filtrer par fenêtre temporelle si demandé
            val analyticsToAnalyze = if (recentOnly) {
                val threshold = System.currentTimeMillis() - RECENT_WINDOW_MS
                analytics.filter { it.timestamp >= threshold }
            } else {
                analytics
            }
            
            Log.d(TAG, "Analyse de ${analyticsToAnalyze.size} entrées analytics")
            
            // 1. Détecter les faiblesses thématiques
            insights.addAll(detectWeakThemes(analyticsToAnalyze))
            
            // 2. Détecter les problèmes de vitesse
            insights.addAll(detectSpeedIssues(analyticsToAnalyze))
            
            // 3. Détecter la précipitation
            insights.addAll(detectRushing(analyticsToAnalyze))
            
            // 4. Détecter les confusions entre notions
            insights.addAll(detectConfusion(analyticsToAnalyze))
            
            // 5. Analyser l'évolution temporelle
            insights.addAll(analyzeTemporalEvolution(analyticsToAnalyze))
            
            // 6. Détecter l'instabilité
            insights.addAll(detectInstability(analyticsToAnalyze))
            
            // 7. Détecter le besoin de pause
            insights.addAll(detectNeedForBreak(analyticsToAnalyze))
            
            // 8. Enrichir avec MemoryScore si disponible
            if (subject != null) {
                insights.addAll(enrichWithMemoryScore(context, subject))
            }
            
            Log.d(TAG, "Analyse terminée : ${insights.size} insights détectés")
            insights.forEach { Log.d(TAG, it.toLogString()) }
            
            return@withContext insights.sortedByDescending { it.severity }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'analyse utilisateur", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Détecte les faiblesses thématiques (erreurs fréquentes par sujet/topic).
     */
    private fun detectWeakThemes(analytics: List<UserAnalytics>): List<UserInsight> {
        val insights = mutableListOf<UserInsight>()
        
        // Grouper par (subject, topic)
        val grouped = analytics.groupBy { it.subject to (it.topic ?: "Général") }
        
        for ((key, entries) in grouped) {
            if (entries.size < MIN_SAMPLE_SIZE) continue
            
            val (subject, topic) = key
            val errorCount = entries.count { !it.isCorrect }
            val errorRate = errorCount.toFloat() / entries.size.toFloat()
            
            if (errorRate >= WEAKNESS_ERROR_RATE) {
                val severity = (errorRate - WEAKNESS_ERROR_RATE) / (1f - WEAKNESS_ERROR_RATE)
                val severityClamped = severity.coerceIn(0.3f, 1.0f)
                
                insights.add(UserInsight(
                    type = InsightType.WEAKNESS,
                    subject = subject,
                    topic = if (topic != "Général") topic else null,
                    severity = severityClamped,
                    evidence = "Taux d'erreur: ${(errorRate * 100).toInt()}% sur $entries entrées",
                    actionable = "Refaire un entraînement ciblé sur ce thème",
                    sampleSize = entries.size
                ))
            }
        }
        
        return insights
    }
    
    /**
     * Détecte les problèmes de vitesse (lenteur excessive).
     */
    private fun detectSpeedIssues(analytics: List<UserAnalytics>): List<UserInsight> {
        val insights = mutableListOf<UserInsight>()
        
        // Grouper par subject
        val grouped = analytics.groupBy { it.subject }
        
        for ((subject, entries) in grouped) {
            if (entries.size < MIN_SAMPLE_SIZE) continue
            
            val avgResponseTime = entries.map { it.responseTime }.average()
            
            if (avgResponseTime >= SLOW_RESPONSE_THRESHOLD) {
                val severity = ((avgResponseTime - SLOW_RESPONSE_THRESHOLD) / SLOW_RESPONSE_THRESHOLD.toDouble())
                    .coerceIn(0.2, 0.7)
                    .toFloat()
                
                insights.add(UserInsight(
                    type = InsightType.SPEED_ISSUE,
                    subject = subject,
                    severity = severity,
                    evidence = "Temps moyen: ${(avgResponseTime / 1000).toInt()}s (seuil: ${SLOW_RESPONSE_THRESHOLD / 1000}s)",
                    actionable = "Prendre le temps de bien comprendre, c'est normal",
                    sampleSize = entries.size
                ))
            }
        }
        
        return insights
    }
    
    /**
     * Détecte la précipitation (réponses trop rapides avec erreurs).
     */
    private fun detectRushing(analytics: List<UserAnalytics>): List<UserInsight> {
        val insights = mutableListOf<UserInsight>()
        
        // Grouper par subject
        val grouped = analytics.groupBy { it.subject }
        
        for ((subject, entries) in grouped) {
            if (entries.size < MIN_SAMPLE_SIZE) continue
            
            // Compter les réponses rapides ET incorrectes
            val rushingErrors = entries.count { 
                it.responseTime < RUSHING_THRESHOLD && !it.isCorrect 
            }
            
            if (rushingErrors >= 3) {
                val rushingRate = rushingErrors.toFloat() / entries.size.toFloat()
                val severity = (rushingRate * 2f).coerceIn(0.3f, 0.9f)
                
                insights.add(UserInsight(
                    type = InsightType.RUSHING,
                    subject = subject,
                    severity = severity,
                    evidence = "$rushingErrors erreurs par précipitation détectées",
                    actionable = "Prends le temps de relire avant de répondre",
                    sampleSize = entries.size
                ))
            }
        }
        
        return insights
    }
    
    /**
     * Détecte les confusions entre notions (patterns d'erreurs similaires).
     */
    private fun detectConfusion(analytics: List<UserAnalytics>): List<UserInsight> {
        val insights = mutableListOf<UserInsight>()
        
        // Grouper par subject
        val grouped = analytics.groupBy { it.subject }
        
        for ((subject, entries) in grouped) {
            if (entries.size < MIN_SAMPLE_SIZE) continue
            
            // Analyser les erreurs répétées
            val errors = entries.filter { !it.isCorrect }
            
            // Si même type de question échoue plusieurs fois
            val repeatedErrors = errors.groupBy { it.questionText }
                .filter { it.value.size >= 2 }
            
            if (repeatedErrors.isNotEmpty()) {
                val severity = (repeatedErrors.size.toFloat() / 5f).coerceIn(0.3f, 0.8f)
                
                insights.add(UserInsight(
                    type = InsightType.CONFUSION,
                    subject = subject,
                    severity = severity,
                    evidence = "${repeatedErrors.size} types de questions échouent régulièrement",
                    actionable = "Revoir les bases de $subject pour clarifier",
                    sampleSize = entries.size
                ))
            }
        }
        
        return insights
    }
    
    /**
     * Analyse l'évolution temporelle (progression ou régression).
     */
    private fun analyzeTemporalEvolution(analytics: List<UserAnalytics>): List<UserInsight> {
        val insights = mutableListOf<UserInsight>()
        
        // Grouper par subject
        val grouped = analytics.groupBy { it.subject }
        
        for ((subject, entries) in grouped) {
            if (entries.size < MIN_SAMPLE_SIZE * 2) continue  // Besoin de plus de données
            
            // Trier par timestamp
            val sorted = entries.sortedBy { it.timestamp }
            
            // Diviser en deux périodes
            val midpoint = sorted.size / 2
            val oldPeriod = sorted.take(midpoint)
            val recentPeriod = sorted.takeLast(midpoint)
            
            val oldSuccessRate = oldPeriod.count { it.isCorrect }.toFloat() / oldPeriod.size.toFloat()
            val recentSuccessRate = recentPeriod.count { it.isCorrect }.toFloat() / recentPeriod.size.toFloat()
            
            val delta = recentSuccessRate - oldSuccessRate
            
            when {
                delta >= 0.15f -> {
                    // Progression significative
                    insights.add(UserInsight(
                        type = InsightType.PROGRESS,
                        subject = subject,
                        severity = 0.2f,  // Positif, pas une alerte
                        evidence = "Progression de ${(delta * 100).toInt()}% de taux de réussite",
                        actionable = "Continue sur cette lancée !",
                        sampleSize = entries.size
                    ))
                }
                delta <= -0.15f -> {
                    // Régression significative
                    val severity = (abs(delta) * 2f).coerceIn(0.4f, 0.8f)
                    insights.add(UserInsight(
                        type = InsightType.REGRESSION,
                        subject = subject,
                        severity = severity,
                        evidence = "Baisse de ${(abs(delta) * 100).toInt()}% de taux de réussite",
                        actionable = "Revoir les bases ou prendre une pause",
                        sampleSize = entries.size
                    ))
                }
            }
        }
        
        return insights
    }
    
    /**
     * Détecte l'instabilité (résultats très variables).
     */
    private fun detectInstability(analytics: List<UserAnalytics>): List<UserInsight> {
        val insights = mutableListOf<UserInsight>()
        
        // Grouper par subject
        val grouped = analytics.groupBy { it.subject }
        
        for ((subject, entries) in grouped) {
            if (entries.size < MIN_SAMPLE_SIZE * 2) continue
            
            // Calculer la variance des résultats (0 = échec, 1 = succès)
            val results = entries.map { if (it.isCorrect) 1f else 0f }
            val mean = results.average().toFloat()
            val variance = results.map { val diff = (it - mean); diff * diff }.average().toFloat()
            
            if (variance >= INSTABILITY_VARIANCE_THRESHOLD) {
                val severity = (variance / 0.5f).coerceIn(0.3f, 0.7f)
                
                insights.add(UserInsight(
                    type = InsightType.INSTABILITY,
                    subject = subject,
                    severity = severity,
                    evidence = "Variance élevée: ${(variance * 100).toInt()}%",
                    actionable = "Chercher plus de régularité dans les révisions",
                    sampleSize = entries.size
                ))
            }
        }
        
        return insights
    }
    
    /**
     * Détecte le besoin de pause (fatigue cognitive).
     */
    private fun detectNeedForBreak(analytics: List<UserAnalytics>): List<UserInsight> {
        val insights = mutableListOf<UserInsight>()
        
        // Analyser les 10 dernières réponses
        val recent = analytics.sortedByDescending { it.timestamp }.take(10)
        
        if (recent.size < 5) return insights
        
        // Détecter une chute brutale de performance
        val recentSuccessRate = recent.count { it.isCorrect }.toFloat() / recent.size.toFloat()
        
        // Détecter un allongement du temps de réponse
        val avgRecentTime = recent.map { it.responseTime }.average()
        
        // Si taux succès < 40% ET temps > 7s → fatigue probable
        if (recentSuccessRate < 0.4f && avgRecentTime > 7000L) {
            insights.add(UserInsight(
                type = InsightType.NEED_BREAK,
                subject = recent.firstOrNull()?.subject ?: "Global",
                severity = 0.6f,
                evidence = "Performance récente faible (${(recentSuccessRate * 100).toInt()}%) + lenteur",
                actionable = "Fais une pause de 10-15 minutes",
                sampleSize = recent.size
            ))
        }
        
        return insights
    }
    
    /**
     * Enrichit l'analyse avec les MemoryScore si disponibles.
     */
    private suspend fun enrichWithMemoryScore(
        context: Context,
        subject: String
    ): List<UserInsight> {
        val insights = mutableListOf<UserInsight>()
        
        try {
            val db = AppDatabase.getDatabase(context)
            val fragiles = db.iAristoteDao().getFragileConcepts()
                .filter { it.subject.equals(subject, ignoreCase = true) }
            
            for (memory in fragiles) {
                if (memory.errorCount < 3) continue  // Besoin de plus d'erreurs
                
                val errorRate = memory.errorCount.toFloat() / 
                    (memory.errorCount + memory.correctCount).toFloat()
                
                if (errorRate >= 0.5f) {
                    val severity = (errorRate - 0.5f) * 2f  // 0.5 → 0.0, 1.0 → 1.0
                    
                    insights.add(UserInsight(
                        type = InsightType.WEAKNESS,
                        subject = memory.subject,
                        topic = memory.concept,
                        severity = severity.coerceIn(0.4f, 1.0f),
                        evidence = "MemoryScore: ${memory.errorCount} erreurs vs ${memory.correctCount} succès",
                        actionable = "Refaire un quiz ciblé sur ${memory.concept}",
                        sampleSize = memory.errorCount + memory.correctCount
                    ))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Impossible d'enrichir avec MemoryScore: ${e.message}")
        }
        
        return insights
    }
    
    /**
     * Génère un verdict personnalisé pour un quiz.
     * 
     * @param context Context Android
     * @param score Score obtenu (0-100)
     * @param subject Matière du quiz
     * @param questionCount Nombre de questions
     * @param correctCount Nombre de bonnes réponses
     * @return Verdict personnalisé basé sur l'historique
     */
    suspend fun generateVerdict(
        context: Context,
        score: Int,
        subject: String,
        questionCount: Int,
        correctCount: Int
    ): String = withContext(Dispatchers.IO) {
        try {
            val insights = analyzeUser(context, subject, recentOnly = true)
            
            val weaknesses = insights.filter { it.type == InsightType.WEAKNESS }
            val progress = insights.filter { it.type == InsightType.PROGRESS }
            val regression = insights.filter { it.type == InsightType.REGRESSION }
            
            // Base verdict selon score
            val baseVerdict = when {
                score >= 90 -> "Excellent travail"
                score >= 75 -> "Bien joué"
                score >= 60 -> "Correct, mais tu peux mieux faire"
                score >= 40 -> "Il faut retravailler certains points"
                else -> "Reprends les bases"
            }
            
            // Enrichir avec insights
            val enrichment = buildString {
                if (progress.isNotEmpty()) {
                    append(" 📈 Tu progresses bien !")
                }
                
                if (weaknesses.isNotEmpty()) {
                    val mainWeakness = weaknesses.maxByOrNull { it.severity }
                    mainWeakness?.let {
                        append(" ⚠️ Attention à ${it.topic ?: it.subject}.")
                    }
                }
                
                if (regression.isNotEmpty()) {
                    append(" 💡 Une petite révision ne ferait pas de mal.")
                }
            }
            
            return@withContext "$baseVerdict.$enrichment"
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur génération verdict", e)
            // Fallback basique
            return@withContext when {
                score >= 90 -> "Excellent travail !"
                score >= 75 -> "Bien joué !"
                score >= 60 -> "Correct, continue comme ça"
                score >= 40 -> "Il faut retravailler certains points"
                else -> "Reprends les bases"
            }
        }
    }
}

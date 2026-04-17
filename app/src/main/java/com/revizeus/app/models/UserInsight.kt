package com.revizeus.app.models

/**
 * ═══════════════════════════════════════════════════════════════
 * USER INSIGHT — Signal exploitable issu des analytics
 * ═══════════════════════════════════════════════════════════════
 * 
 * Rôle :
 * Représente un signal d'intelligence détecté dans les analytics
 * de l'utilisateur. Contrairement aux données brutes (UserAnalytics),
 * un UserInsight est un pattern détecté, interprété et actionnable.
 * 
 * Types de signaux :
 * - WEAKNESS : Faiblesse détectée sur un thème
 * - SPEED_ISSUE : Problème de vitesse (trop lent)
 * - RUSHING : Réponses trop rapides (précipitation)
 * - CONFUSION : Confusion entre deux notions
 * - REGRESSION : Régression temporelle (déclin)
 * - PROGRESS : Progression temporelle (amélioration)
 * - MASTERY : Maîtrise confirmée
 * - INSTABILITY : Instabilité (résultats variables)
 * - NEED_BREAK : Besoin de pause détecté
 * 
 * Utilisation :
 * - UltimateQuizBuilder : ajuster difficultés selon insights
 * - NormalTrainingBuilder : cibler les faiblesses détectées
 * - QuizResultActivity : générer verdicts personnalisés
 * 
 * BLOC 3A — EXPLOITER USERANALYTICS
 * ═══════════════════════════════════════════════════════════════
 */
data class UserInsight(
    /** Type de signal détecté */
    val type: InsightType,
    
    /** Matière concernée */
    val subject: String,
    
    /** Thème précis si applicable (ex: "Théorème de Pythagore") */
    val topic: String? = null,
    
    /** Sévérité du signal : 0.0 (négligeable) à 1.0 (critique) */
    val severity: Float,
    
    /** Description technique pour logs et debug */
    val evidence: String,
    
    /** Recommandation concrète et actionnable */
    val actionable: String,
    
    /** Nombre de données ayant servi à détecter ce signal */
    val sampleSize: Int = 0,
    
    /** Timestamp de détection */
    val detectedAt: Long = System.currentTimeMillis()
) {
    
    /**
     * Indique si ce signal doit déclencher une action immédiate.
     * Seuil : sévérité >= 0.7
     */
    fun isActionRequired(): Boolean = severity >= 0.7f
    
    /**
     * Indique si ce signal est une alerte (sévérité >= 0.5).
     */
    fun isAlert(): Boolean = severity >= 0.5f
    
    /**
     * Indique si ce signal est juste informatif (sévérité < 0.3).
     */
    fun isInformational(): Boolean = severity < 0.3f
    
    /**
     * Retourne une représentation lisible pour affichage utilisateur.
     */
    fun toUserMessage(): String {
        return when (type) {
            InsightType.WEAKNESS -> {
                val themeText = topic ?: subject
                when {
                    severity >= 0.8f -> "⚠️ Difficulté majeure en $themeText"
                    severity >= 0.6f -> "⚡ Attention à $themeText"
                    else -> "📌 À consolider : $themeText"
                }
            }
            InsightType.SPEED_ISSUE -> {
                "⏱️ Tu prends ton temps sur ${topic ?: subject} — c'est bien !"
            }
            InsightType.RUSHING -> {
                "⚡ Attention, tu vas trop vite ! Prends le temps de relire."
            }
            InsightType.CONFUSION -> {
                "🔄 Confusion détectée entre concepts. Revois les bases."
            }
            InsightType.REGRESSION -> {
                "📉 Légère baisse récente. Une pause ou révision ?"
            }
            InsightType.PROGRESS -> {
                "📈 Belle progression ! Continue comme ça !"
            }
            InsightType.MASTERY -> {
                "⭐ Maîtrise confirmée en ${topic ?: subject} !"
            }
            InsightType.INSTABILITY -> {
                "🎲 Résultats variables. Besoin de plus de régularité ?"
            }
            InsightType.NEED_BREAK -> {
                "😴 Tu sembles fatigué. Une pause te ferait du bien !"
            }
        }
    }
    
    /**
     * Retourne un log technique détaillé.
     */
    fun toLogString(): String {
        val topicPart = topic?.let { " ($it)" } ?: ""
        return "[${type.name}] $subject$topicPart | Severity: %.2f | Sample: $sampleSize | $evidence".format(severity)
    }
}

/**
 * Types de signaux d'intelligence.
 */
enum class InsightType {
    /** Faiblesse détectée (taux erreur élevé) */
    WEAKNESS,
    
    /** Lenteur détectée (temps réponse élevé) */
    SPEED_ISSUE,
    
    /** Précipitation détectée (temps réponse très court + erreurs) */
    RUSHING,
    
    /** Confusion entre notions (patterns d'erreurs similaires) */
    CONFUSION,
    
    /** Régression temporelle (déclin de performance) */
    REGRESSION,
    
    /** Progression temporelle (amélioration) */
    PROGRESS,
    
    /** Maîtrise confirmée (taux succès élevé + stable) */
    MASTERY,
    
    /** Instabilité (variance élevée) */
    INSTABILITY,
    
    /** Besoin de pause détecté (fatigue cognitive) */
    NEED_BREAK
}

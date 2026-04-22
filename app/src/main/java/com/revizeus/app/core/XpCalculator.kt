package com.revizeus.app.core

import kotlin.compareTo
import kotlin.math.roundToInt

/**
 * ═══════════════════════════════════════════════════════════════
 * XpCalculator.kt — RéviZeus v9  ✅ CORRIGÉ
 * Moteur de calcul d'expérience — "La Balance de Zeus"
 * ═══════════════════════════════════════════════════════════════
 *
 * CORRECTION MAJEURE :
 *   ✅ Seuil de niveau PROGRESSIF : level * 100 * π (≈ 314)
 *      Plus le joueur monte en niveau, plus il faut d'XP.
 *
 *      Exemples :
 *        Niveau 1 → 2 :   314 XP requis
 *        Niveau 5 → 6 :   1570 XP requis
 *        Niveau 10 → 11 : 3140 XP requis
 *        Niveau 20 → 21 : 6280 XP requis
 *
 *   ✅ xpThresholdForLevel() → XP total requis pour atteindre
 *      un niveau donné (somme des seuils précédents).
 *
 *   ✅ calculateLevel() recalculé selon la formule progressive.
 *
 *   ✅ xpInCurrentLevel() et progressToNextLevel() remplacent
 *      les hardcoded "% 500" répandus dans DashboardActivity
 *      et BadgeManager.
 *
 * USAGE DANS L'APP :
 *   val lvl  = XpCalculator.calculateLevel(profile.xp)
 *   val xpIn = XpCalculator.xpInCurrentLevel(profile.xp)
 *   val seuil= XpCalculator.xpThresholdForLevel(lvl)
 *   val pct  = XpCalculator.progressToNextLevel(profile.xp)
 * ═══════════════════════════════════════════════════════════════
 */
object XpCalculator {

    // ── Constantes du quiz ────────────────────────────────────
    const val XP_BASE_QUIZ          = 50
    const val XP_BASE_SCAN          = 20
    const val XP_PER_CORRECT_ANSWER = 10
    const val STREAK_MULTIPLIER_STEP = 0.1f   // +10% par jour de série

    // ── Formule de seuil ─────────────────────────────────────
    /**
     * XP requis pour passer DU niveau [level] AU niveau [level + 1].
     * Formule : level × 100 × π  (arrondi à l'entier)
     *
     * @param level Niveau actuel (commence à 1)
     * @return XP nécessaire pour monter de ce niveau au suivant
     */
    fun xpThresholdForLevel(level: Int): Int {
        val lvl = level.coerceAtLeast(1)
        return (lvl * 100 * Math.PI).roundToInt()
    }

    /**
     * XP total cumulé nécessaire pour atteindre [targetLevel].
     * Somme de xpThresholdForLevel(1) + ... + xpThresholdForLevel(targetLevel - 1)
     *
     * Exemple :
     *   xpTotalRequiredForLevel(1) = 0        (niveau de départ)
     *   xpTotalRequiredForLevel(2) = 314
     *   xpTotalRequiredForLevel(3) = 314 + 628 = 942
     */
    fun xpTotalRequiredForLevel(targetLevel: Int): Int {
        if (targetLevel <= 1) return 0
        return (1 until targetLevel).sumOf { xpThresholdForLevel(it) }
    }

    // ── Calculs de niveau ─────────────────────────────────────

    /**
     * Détermine le niveau actuel à partir de l'XP total.
     * Itère jusqu'à ce que l'XP total requis dépasse l'XP du joueur.
     *
     * @param totalXp XP total du joueur (profile.xp)
     * @return Niveau actuel (minimum 1)
     */
    fun calculateLevel(totalXp: Int): Int {
        var level = 1
        var xpAccumulated = 0
        while (true) {
            val seuil = xpThresholdForLevel(level)
            if (xpAccumulated + seuil >= totalXp) break
            xpAccumulated += seuil
            level++
        }
        return level
    }

    /**
     * XP accumulé dans le niveau actuel (0 .. seuil du niveau - 1).
     * Remplace les "profile.xp % 500" hardcodés.
     *
     * @param totalXp XP total du joueur
     * @return XP dans le niveau en cours
     */
    fun xpInCurrentLevel(totalXp: Int): Int {
        val level = calculateLevel(totalXp)
        val xpPourAtteindreCeNiveau = xpTotalRequiredForLevel(level)
        return totalXp - xpPourAtteindreCeNiveau
    }

    /**
     * Pourcentage de progression vers le prochain niveau (0..100).
     * Remplace les "progressionNiveauPct" hardcodés à 500 XP.
     *
     * @param totalXp XP total du joueur
     * @return Entier entre 0 et 100
     */
    fun progressToNextLevel(totalXp: Int): Int {
        val level = calculateLevel(totalXp)
        val seuil = xpThresholdForLevel(level)
        val xpIn  = xpInCurrentLevel(totalXp)
                return if (seuil > 0) ((xpIn * 100) / seuil).coerceIn(0, 100) else 100
    }

    // ── Calculs de gain d'XP ─────────────────────────────────

    /**
     * Calcule l'XP gagnée après un quiz.
     *
     * Formule :
     *   base = XP_BASE_QUIZ + (bonnes réponses × XP_PER_CORRECT_ANSWER)
     *   × (1 + précision + bonus streak)
     *
     * Bonus streak : +10% par jour consécutif, plafonné à +50%.
     *
     * @param score           Nombre de bonnes réponses
     * @param totalQuestions  Nombre total de questions
     * @param currentStreak   Série de jours consécutifs du joueur
     * @return XP à créditer
     */
    fun calculateQuizXp(score: Int, totalQuestions: Int, currentStreak: Int): Int {
        val baseGain       = XP_BASE_QUIZ + (score * XP_PER_CORRECT_ANSWER)
        val precisionFactor = if (totalQuestions > 0) score.toFloat() / totalQuestions else 0f
        val streakBonus    = (currentStreak * STREAK_MULTIPLIER_STEP).coerceAtMost(0.5f)
        return (baseGain * (1 + precisionFactor + streakBonus)).roundToInt()
    }

    /**
     * XP octroyé pour un scan de cours réussi.
     */
    fun calculateScanXp(): Int = XP_BASE_SCAN

    /**
     * Résumé lisible pour les logs.
     * Exemple : "Niveau 5 | 742 / 1570 XP (47%)"
     */
    fun debugSummary(totalXp: Int): String {
        val lvl   = calculateLevel(totalXp)
        val xpIn  = xpInCurrentLevel(totalXp)
        val seuil = xpThresholdForLevel(lvl)
        val pct   = progressToNextLevel(totalXp)
        return "Niveau $lvl | $xpIn / $seuil XP ($pct%)"
    }
}
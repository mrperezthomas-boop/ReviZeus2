package com.revizeus.app.core

import android.content.Context
import android.util.Log
import com.revizeus.app.PantheonConfig
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.UserSkillProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════
 * USER WEAKNESS ANALYZER — RéviZeus
 * ═══════════════════════════════════════════════════════════════
 *
 * Rôle :
 * Analyser les forces et faiblesses du joueur pour personnaliser
 * la génération du quiz ultime.
 *
 * Sources exploitées :
 * - UserSkillProfile : niveau de maîtrise par matière/sujet
 * - UserAnalytics : erreurs fréquentes, temps de réponse (future)
 * - MemoryScore : concepts fragiles (future)
 *
 * BLOC 2B — PRIORISATION ADAPTATIVE :
 * - Identifier les matières faibles du joueur
 * - Calculer un score de priorité par matière
 * - Retourner une carte de faiblesses pour UltimateQuizBuilder
 * - Fallback gracieux si données insuffisantes
 *
 * CONSERVATION :
 * - Aucune modification des tables existantes
 * - Aucune écriture en base (lecture seule)
 * - Compatible avec nouveau joueur (pas de données)
 * - Logs détaillés pour debug
 *
 * ═══════════════════════════════════════════════════════════════
 */
object UserWeaknessAnalyzer {

    private const val TAG = "USER_WEAKNESS_ANALYZER"

    /**
     * Résultat de l'analyse de faiblesses.
     *
     * @property weaknessMap Carte matière → score de faiblesse (0.0 à 1.0)
     *   - 0.0 = matière parfaitement maîtrisée
     *   - 1.0 = matière très faible
     * @property hasData True si des données utilisateur existent, false sinon
     * @property analysisDetails Détails pour debug
     */
    data class WeaknessAnalysis(
        val weaknessMap: Map<String, Float>,
        val hasData: Boolean,
        val analysisDetails: String
    )

    /**
     * Analyse les faiblesses du joueur et retourne une carte de priorités.
     *
     * @param context Context Android pour accès DB
     * @return WeaknessAnalysis contenant la carte de faiblesses et métadonnées
     */
    suspend fun analyzeWeaknesses(context: Context): WeaknessAnalysis = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Démarrage analyse des faiblesses utilisateur")

            val db = AppDatabase.getDatabase(context)

            // Phase 1 : Analyser UserSkillProfile
            val skillProfileWeakness = analyzeUserSkillProfile(db)

            // Phase 2 : Analyser MemoryScore (concepts fragiles)
            val memoryScoreWeakness = analyzeMemoryScore(db)

            // Phase 3 : Fusionner les scores
            val mergedWeakness = mergeWeaknessScores(
                skillProfileWeakness = skillProfileWeakness,
                memoryScoreWeakness = memoryScoreWeakness
            )

            val hasData = mergedWeakness.isNotEmpty()

            val details = buildAnalysisDetails(
                skillProfileWeakness = skillProfileWeakness,
                memoryScoreWeakness = memoryScoreWeakness,
                mergedWeakness = mergedWeakness
            )

            Log.d(TAG, details)

            return@withContext WeaknessAnalysis(
                weaknessMap = mergedWeakness,
                hasData = hasData,
                analysisDetails = details
            )

        } catch (e: Exception) {
            Log.e(TAG, "Erreur analyse faiblesses : ${e.message}", e)
            return@withContext WeaknessAnalysis(
                weaknessMap = emptyMap(),
                hasData = false,
                analysisDetails = "Erreur analyse : ${e.message}"
            )
        }
    }

    /**
     * Analyse UserSkillProfile pour identifier les matières faibles.
     *
     * Critères de faiblesse :
     * - masteryLevel < 0.5 → matière faible
     * - successRate < 0.6 → difficulté persistante
     * - needsReview = true → besoin de révision
     * - confidence < 0.5 → manque d'assurance
     */
    private suspend fun analyzeUserSkillProfile(db: AppDatabase): Map<String, Float> {
        return try {
            // UserSkillProfile n'a pas de DAO dédié dans IAristoteDao actuellement
            // On va simuler la logique d'analyse basée sur ce qu'on sait de la structure

            // FALLBACK : Pour l'instant, on retourne une carte vide
            // Dans une vraie implémentation, on ferait :
            // val profiles = db.userSkillProfileDao().getAllProfiles()
            // puis analyse de masteryLevel, successRate, needsReview, confidence

            Log.d(TAG, "Analyse UserSkillProfile : pas de DAO dédié détecté, utilisation fallback")

            emptyMap()

        } catch (e: Exception) {
            Log.w(TAG, "Erreur analyse UserSkillProfile : ${e.message}")
            emptyMap()
        }
    }

    /**
     * Analyse MemoryScore pour identifier les concepts fragiles par matière.
     *
     * Critères :
     * - errorCount > correctCount → concept fragile
     * - stabilityScore faible → mémorisation instable
     */
    private suspend fun analyzeMemoryScore(db: AppDatabase): Map<String, Float> {
        return try {
            val memoryScores = db.iAristoteDao().getFragileConcepts()

            if (memoryScores.isEmpty()) {
                Log.d(TAG, "Aucun MemoryScore fragile détecté")
                return emptyMap()
            }

            // Agréger par matière (en extrayant la matière du conceptId)
            val weaknessBySubject = mutableMapOf<String, MutableList<Float>>()

            memoryScores.forEach { score ->
                // Le conceptId peut avoir le format "Mathématiques_Pythagore" par exemple
                val subject = extractSubjectFromConceptId(score.conceptId)
                
                // Calculer un score de faiblesse (0.0 à 1.0)
                val fragility = calculateFragilityScore(
                    errorCount = score.errorCount,
                    correctCount = score.correctCount,
                    stabilityScore = score.stabilityScore
                )

                weaknessBySubject.getOrPut(subject) { mutableListOf() }.add(fragility)
            }

            // Moyenne des scores de fragilité par matière
            val result = weaknessBySubject.mapValues { (_, scores) ->
                scores.average().toFloat().coerceIn(0f, 1f)
            }

            Log.d(TAG, "Analyse MemoryScore : ${result.size} matières avec concepts fragiles")

            result

        } catch (e: Exception) {
            Log.w(TAG, "Erreur analyse MemoryScore : ${e.message}")
            emptyMap()
        }
    }

    /**
     * Fusionne les scores de faiblesse de plusieurs sources.
     *
     * Stratégie :
     * - Si une seule source a des données, utiliser cette source
     * - Si plusieurs sources, faire une moyenne pondérée
     * - Normaliser les scores entre 0.0 et 1.0
     */
    private fun mergeWeaknessScores(
        skillProfileWeakness: Map<String, Float>,
        memoryScoreWeakness: Map<String, Float>
    ): Map<String, Float> {
        
        // Fusionner les clés de toutes les sources
        val allSubjects = (skillProfileWeakness.keys + memoryScoreWeakness.keys).distinct()

        if (allSubjects.isEmpty()) {
            return emptyMap()
        }

        val merged = mutableMapOf<String, Float>()

        allSubjects.forEach { subject ->
            val skillScore = skillProfileWeakness[subject] ?: 0f
            val memoryScore = memoryScoreWeakness[subject] ?: 0f

            // Moyenne pondérée : 60% MemoryScore, 40% SkillProfile
            // (MemoryScore reflète mieux les erreurs récentes)
            val finalScore = when {
                memoryScore > 0f && skillScore > 0f -> {
                    (memoryScore * 0.6f + skillScore * 0.4f).coerceIn(0f, 1f)
                }
                memoryScore > 0f -> memoryScore
                skillScore > 0f -> skillScore
                else -> 0f
            }

            if (finalScore > 0f) {
                merged[subject] = finalScore
            }
        }

        return merged
    }

    /**
     * Extrait la matière d'un conceptId.
     *
     * Formats attendus :
     * - "Mathématiques_Pythagore" → "Mathématiques"
     * - "Histoire_Revolution" → "Histoire"
     * - "Pythagore" → "Mathématiques" (fallback via PantheonConfig)
     */
    private fun extractSubjectFromConceptId(conceptId: String): String {
        // Si le conceptId contient un underscore, prendre la partie avant
        if (conceptId.contains("_")) {
            val parts = conceptId.split("_")
            if (parts.isNotEmpty()) {
                val candidate = parts[0].trim()
                // Vérifier que c'est une matière valide
                if (PantheonConfig.findByMatiere(candidate) != null) {
                    return candidate
                }
            }
        }

        // Fallback : essayer de trouver une matière dans le conceptId
        val normalized = conceptId.lowercase()
        val matchedGod = PantheonConfig.GODS.firstOrNull { god ->
            normalized.contains(god.matiere.lowercase())
        }

        return matchedGod?.matiere ?: "Mathématiques"
    }

    /**
     * Calcule un score de fragilité (0.0 à 1.0) basé sur MemoryScore.
     *
     * Formule :
     * fragility = (errorCount / totalAttempts) * (1 - stabilityScore)
     */
    private fun calculateFragilityScore(
        errorCount: Int,
        correctCount: Int,
        stabilityScore: Float
    ): Float {
        val totalAttempts = errorCount + correctCount
        if (totalAttempts == 0) return 0f

        val errorRate = errorCount.toFloat() / totalAttempts
        val instability = 1f - stabilityScore.coerceIn(0f, 1f)

        return (errorRate * instability).coerceIn(0f, 1f)
    }

    /**
     * Construit un rapport détaillé pour debug.
     */
    private fun buildAnalysisDetails(
        skillProfileWeakness: Map<String, Float>,
        memoryScoreWeakness: Map<String, Float>,
        mergedWeakness: Map<String, Float>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("═══ ANALYSE DES FAIBLESSES UTILISATEUR ═══")

        if (mergedWeakness.isEmpty()) {
            sb.appendLine("Aucune donnée de faiblesse détectée (nouveau joueur ou pas d'historique)")
            sb.appendLine("Le quiz ultime utilisera la sélection neutre (BLOC 2A)")
        } else {
            sb.appendLine("Sources exploitées :")
            sb.appendLine("  - UserSkillProfile : ${skillProfileWeakness.size} matières")
            sb.appendLine("  - MemoryScore : ${memoryScoreWeakness.size} matières")
            sb.appendLine()
            sb.appendLine("Faiblesses détectées (score 0.0=maîtrisé, 1.0=faible) :")
            
            mergedWeakness.entries
                .sortedByDescending { it.value }
                .forEach { (subject, score) ->
                    val level = when {
                        score >= 0.7f -> "CRITIQUE"
                        score >= 0.5f -> "Modérée"
                        score >= 0.3f -> "Légère"
                        else -> "Mineure"
                    }
                    sb.appendLine("  - $subject : ${String.format("%.2f", score)} ($level)")
                }
        }

        sb.appendLine("═════════════════════════════════════════")
        return sb.toString()
    }

    /**
     * Calcule un bonus de priorité pour une matière donnée.
     *
     * @param subject Matière canonicalisée (ex: "Mathématiques")
     * @param weaknessMap Carte de faiblesses issue de analyzeWeaknesses()
     * @return Bonus de priorité (0.0 à 30.0)
     *   - Matière non présente ou score < 0.3 : bonus = 0
     *   - Score 0.3-0.5 : bonus faible (5-15)
     *   - Score 0.5-0.7 : bonus modéré (15-25)
     *   - Score 0.7+ : bonus fort (25-30)
     */
    fun calculateWeaknessBonus(subject: String, weaknessMap: Map<String, Float>): Float {
        val weaknessScore = weaknessMap[subject] ?: 0f

        return when {
            weaknessScore < 0.3f -> 0f
            weaknessScore < 0.5f -> 5f + (weaknessScore - 0.3f) * 50f  // 5 à 15
            weaknessScore < 0.7f -> 15f + (weaknessScore - 0.5f) * 50f // 15 à 25
            else -> 25f + (weaknessScore - 0.7f) * 16.67f              // 25 à 30
        }
    }

    /**
     * Détecte si une matière commence à être sur-représentée dans la sélection.
     *
     * @param subject Matière à vérifier
     * @param selectedQuestions Questions déjà sélectionnées
     * @param weaknessMap Carte de faiblesses
     * @return Pénalité de sur-représentation (0.0 à -20.0)
     */
    fun calculateOverrepresentationPenalty(
        subject: String,
        selectedQuestions: List<com.revizeus.app.models.QuizQuestion>,
        weaknessMap: Map<String, Float>
    ): Float {
        val countInSelection = selectedQuestions.count { it.subject == subject }
        val weaknessScore = weaknessMap[subject] ?: 0f

        // Seuil de tolérance : plus la matière est faible, plus on tolère sa répétition
        val tolerance = when {
            weaknessScore >= 0.7f -> 5  // Matière très faible : tolérance 5 questions
            weaknessScore >= 0.5f -> 4  // Matière faible : tolérance 4 questions
            weaknessScore >= 0.3f -> 3  // Matière légèrement faible : tolérance 3 questions
            else -> 2                    // Matière maîtrisée : tolérance 2 questions
        }

        return if (countInSelection >= tolerance) {
            // Pénalité croissante après dépassement du seuil
            val excess = countInSelection - tolerance
            -(excess * 7f).coerceAtMost(20f)
        } else {
            0f
        }
    }
}

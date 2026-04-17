package com.revizeus.app.core

import android.content.Context
import android.util.Log
import com.revizeus.app.GeminiManager
import com.revizeus.app.GodManager
import com.revizeus.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * ═══════════════════════════════════════════════════════════════
 * ULTIMATE QUIZ BUILDER — BLOC 3B : ADAPTATIF INTELLIGENT
 * ═══════════════════════════════════════════════════════════════
 */
object UltimateQuizBuilder {

    private const val TAG = "ULTIMATE_QUIZ_BUILDER"
    
    private const val TARGET_QUESTION_COUNT = 40
    private const val N_SAVOIRS_RECENTS = 5
    private const val M_MATIERES_RECENTES = 2
    private const val QUESTIONS_PER_COURSE = 8
    private const val MAX_RETRY_PER_COURSE = 1

    /**
     * BLOC 3B — Construit un quiz ultime de 40 questions adapté aux insights.
     */
    suspend fun buildUltimateQuiz(
        context: Context,
        userAge: Int
    ): List<QuizQuestion> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "═══ BLOC 3B — Démarrage génération adaptative ═══")
            Log.d(TAG, "Âge utilisateur: $userAge")
            
            val db = AppDatabase.getDatabase(context)
            val allCourses = db.iAristoteDao().getAllCourses()

            if (allCourses.isEmpty()) {
                Log.w(TAG, "Aucun cours disponible")
                return@withContext emptyList()
            }

            Log.d(TAG, "${allCourses.size} cours disponibles")
            
            // ═══════════════════════════════════════════════════════════
            // BLOC 3B — ANALYSE INSIGHTS POUR ADAPTATION
            // ═══════════════════════════════════════════════════════════
            
            val globalInsights = try {
                UserAnalyticsEngine.analyzeUser(
                    context = context,
                    subject = null,
                    recentOnly = true
                )
            } catch (e: Exception) {
                Log.w(TAG, "Analyse insights échouée: ${e.message}")
                emptyList()
            }
            
            Log.d(TAG, "Insights détectés: ${globalInsights.size}")
            globalInsights.forEach { Log.d(TAG, "  - ${it.toLogString()}") }
            
            // Analyser les matières par niveau de maîtrise
            val subjectDifficultyMap = buildSubjectDifficultyMap(globalInsights)
            Log.d(TAG, "Carte difficultés: $subjectDifficultyMap")
            
            // ═══════════════════════════════════════════════════════════
            // BLOC 2B — ANALYSE FAIBLESSES (conservé)
            // ═══════════════════════════════════════════════════════════
            
            val weaknessAnalysis = UserWeaknessAnalyzer.analyzeWeaknesses(context)
            Log.d(TAG, "Analyse faiblesses : ${if (weaknessAnalysis.hasData) "données disponibles" else "nouveau joueur"}")

            // ═══════════════════════════════════════════════════════════
            // GÉNÉRATION POOL DE QUESTIONS
            // ═══════════════════════════════════════════════════════════
            
            val questionPool = generateQuestionPoolAdaptive(
                context = context,
                courses = allCourses,
                userAge = userAge,
                weaknessAnalysis = weaknessAnalysis,
                subjectDifficultyMap = subjectDifficultyMap
            )

            if (questionPool.isEmpty()) {
                Log.e(TAG, "Pool de questions vide après génération")
                return@withContext emptyList()
            }

            Log.d(TAG, "Pool généré: ${questionPool.size} questions")
            
            // ═══════════════════════════════════════════════════════════
            // SÉLECTION ALTERNÉE (BLOC 2A conservé + BLOC 3B enrichi)
            // ═══════════════════════════════════════════════════════════
            
            val selectedQuestions = selectAlternatingQuestions(
                pool = questionPool,
                targetCount = TARGET_QUESTION_COUNT,
                weaknessAnalysis = weaknessAnalysis
            )

            Log.d(TAG, "═══ Génération terminée : ${selectedQuestions.size} questions ═══")
            
            return@withContext selectedQuestions

        } catch (e: Exception) {
            Log.e(TAG, "Erreur génération quiz ultime", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * BLOC 3B — Construit la carte difficultés par matière selon insights.
     */
    private fun buildSubjectDifficultyMap(insights: List<UserInsight>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        
        for (insight in insights) {
            val currentDiff = map.getOrDefault(insight.subject, 2)
            
            val adjustment = when (insight.type) {
                InsightType.MASTERY -> +1
                InsightType.PROGRESS -> 0
                InsightType.WEAKNESS -> -1
                InsightType.REGRESSION -> -1
                InsightType.CONFUSION -> -1
                else -> 0
            }
            
            val weightedAdjustment = (adjustment * insight.severity).toInt()
            val newDiff = (currentDiff + weightedAdjustment).coerceIn(1, 3)
            
            map[insight.subject] = newDiff
        }
        
        return map
    }
    
    /**
     * BLOC 3B + BLOC 4A — Génère le pool avec adaptation insights + types variés.
     */
    private suspend fun generateQuestionPoolAdaptive(
        context: Context,
        courses: List<CourseEntry>,
        userAge: Int,
        weaknessAnalysis: UserWeaknessAnalyzer.WeaknessAnalysis,
        subjectDifficultyMap: Map<String, Int>
    ): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val questionPool = mutableListOf<QuizQuestion>()
        
        for (course in courses) {
            try {
                val god = GodManager.fromMatiere(course.matiere)
                val diviniteNom = god?.nomDieu ?: "Zeus"
                val ethosNom = god?.ethos ?: "Équilibre"
                
                // BLOC 3B — Déterminer difficulté adaptée
                val targetDifficulty = subjectDifficultyMap.getOrDefault(course.matiere, 2)
                Log.d(TAG, "Génération ${course.matiere} - Difficulté cible: $targetDifficulty")
                
                // BLOC 4A — Prompt enrichi multi-types
                val basePrompt = buildBasePrompt(course, diviniteNom, ethosNom, userAge)
                
                val enrichedPrompt = GeminiQuestionTypeHelper.buildMultiTypePrompt(
                    basePrompt = basePrompt,
                    subject = course.matiere,
                    topic = course.topic,
                    userAge = userAge,
                    questionCount = QUESTIONS_PER_COURSE,
                    allowImages = true
                )
                
                // Ajouter consigne difficulté
                val finalPrompt = """
                    $enrichedPrompt
                    
                    DIFFICULTÉ CIBLE : $targetDifficulty/3
                    ${when (targetDifficulty) {
                        1 -> "Génère des questions SIMPLES et DIRECTES (niveau débutant)"
                        2 -> "Génère des questions de DIFFICULTÉ MOYENNE (niveau intermédiaire)"
                        3 -> "Génère des questions AVANCÉES et SUBTILES (niveau expert)"
                        else -> ""
                    }}
                """.trimIndent()
                
                val geminiResponse = try {
                    // Charger profil pour avoir classe et mood
                    val profile = try {
                        val db = AppDatabase.getDatabase(context)
                        db.iAristoteDao().getUserProfile() ?: UserProfile(
                            id = 1, age = userAge, classLevel = "Terminale",
                            mood = "Prêt", xp = 0, streak = 0, cognitivePattern = "Général"
                        )
                    } catch (_: Exception) {
                        UserProfile(
                            id = 1, age = userAge, classLevel = "Terminale",
                            mood = "Prêt", xp = 0, streak = 0, cognitivePattern = "Général"
                        )
                    }
                    
                    GeminiManager.genererContenuOracle(
                        texte = course.content,
                        age = userAge,
                        classe = profile.classLevel,
                        matiere = course.matiere,
                        divinite = diviniteNom,
                        ethos = ethosNom,
                        mood = profile.mood,
                        adaptiveContextNote = "DIFFICULTÉ CIBLE : $targetDifficulty/3"
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Échec Gemini pour ${course.matiere}: ${e.message}")
                    null
                }
                
                if (geminiResponse != null) {
                    val (_, questions) = IAristoteEngine.decoderReponse(geminiResponse) 
                        ?: (null to emptyList<QuizQuestion>())
                    
                    val enrichedQuestions = questions.map { q ->
                        q.copy(
                            subject = course.matiere,
                            courseId = course.id.toString(),
                            difficulty = targetDifficulty
                        )
                    }
                    
                    questionPool.addAll(enrichedQuestions)
                    Log.d(TAG, "  +${enrichedQuestions.size} questions (types: ${enrichedQuestions.groupBy { it.questionType }.keys})")
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "Erreur génération ${course.matiere}: ${e.message}")
            }
        }
        
        return@withContext questionPool
    }
    
    /**
     * Sélection alternée (BLOC 2A conservé + BLOC 2B priorités).
     */
    private fun selectAlternatingQuestions(
        pool: List<QuizQuestion>,
        targetCount: Int,
        weaknessAnalysis: UserWeaknessAnalyzer.WeaknessAnalysis
    ): List<QuizQuestion> {
        if (pool.isEmpty()) return emptyList()
        
        val selected = mutableListOf<QuizQuestion>()
        val remaining = pool.toMutableList()
        
        val recentSavoirs = ArrayDeque<String>(N_SAVOIRS_RECENTS)
        val recentMatieres = ArrayDeque<String>(M_MATIERES_RECENTES)
        
        while (selected.size < targetCount && remaining.isNotEmpty()) {
            var bestCandidate: QuizQuestion? = null
            var bestScore = -1000.0
            
            for (candidate in remaining) {
                var score = 0.0
                
                // BLOC 2B — Bonus faiblesse (CORRIGÉ)
                val weaknessBonus = UserWeaknessAnalyzer.calculateWeaknessBonus(
                    subject = candidate.subject,
                    weaknessMap = weaknessAnalysis.weaknessMap
                )
                score += weaknessBonus
                
                // BLOC 2A — Pénalité répétition savoir
                val courseKey = "${candidate.subject}_${candidate.courseId}"
                val repetitionPenalty = if (courseKey in recentSavoirs) -20.0 else 0.0
                score += repetitionPenalty
                
                // BLOC 2A — Pénalité répétition matière
                val matierePenalty = if (candidate.subject in recentMatieres) -10.0 else 0.0
                score += matierePenalty
                
                // Aléa
                score += Random.nextDouble(-2.0, 2.0)
                
                if (score > bestScore) {
                    bestScore = score
                    bestCandidate = candidate
                }
            }
            
            if (bestCandidate != null) {
                selected.add(bestCandidate)
                remaining.remove(bestCandidate)
                
                val courseKey = "${bestCandidate.subject}_${bestCandidate.courseId}"
                recentSavoirs.addLast(courseKey)
                if (recentSavoirs.size > N_SAVOIRS_RECENTS) recentSavoirs.removeFirst()
                
                recentMatieres.addLast(bestCandidate.subject)
                if (recentMatieres.size > M_MATIERES_RECENTES) recentMatieres.removeFirst()
            } else {
                break
            }
        }
        
        return selected
    }
    
    private fun buildBasePrompt(
        course: CourseEntry,
        diviniteNom: String,
        ethosNom: String,
        userAge: Int
    ): String {
        return """
        Tu es $diviniteNom, dieu/déesse de ${course.matiere}.
        Ton approche pédagogique : $ethosNom.
        
        SAVOIR SOURCE :
        Matière : ${course.matiere}
        ${if (course.topic.isNotBlank()) "Thème : ${course.topic}" else ""}
        
        Contenu du cours :
        ${course.content.take(2000)}
        
        Âge de l'élève : $userAge ans
        """.trimIndent()
    }
}

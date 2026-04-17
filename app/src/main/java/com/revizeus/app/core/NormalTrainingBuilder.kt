package com.revizeus.app.core

import android.content.Context
import android.util.Log
import com.revizeus.app.GeminiManager
import com.revizeus.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════
 * NORMAL TRAINING BUILDER — BLOC 3B : CIBLAGE INTELLIGENT
 * ═══════════════════════════════════════════════════════════════
 * 
 * ÉVOLUTIONS par rapport à BLOC 2C :
 * 
 * BLOC 3B — CIBLAGE FAIBLESSES :
 * - Détecte les faiblesses spécifiques via UserAnalyticsEngine
 * - Génère des questions ciblées sur les concepts faibles
 * - Adapte la difficulté selon le niveau de faiblesse
 * - Priorise les thèmes où le joueur échoue souvent
 * 
 * BLOC 4A — TYPES VARIÉS :
 * - Génère un mélange intelligent de types de questions
 * - Adapte les types selon le contenu du savoir
 * - Utilise GeminiQuestionTypeHelper
 * 
 * PRINCIPE CORE :
 * - 100% généré par Gemini selon données utilisateur
 * - Ciblage basé sur historique réel
 * - Adaptation contextuelle permanente
 * 
 * ═══════════════════════════════════════════════════════════════
 */
object NormalTrainingBuilder {
    
    private const val TAG = "NORMAL_TRAINING_BUILDER"
    private const val TARGET_QUESTION_COUNT = 30
    private const val MAX_GEMINI_CALLS = 4
    private const val MIN_QUESTION_COUNT = 10
    
    /**
     * BLOC 3B — Construit un entraînement normal avec ciblage intelligent.
     */
    suspend fun buildNormalTraining(
        context: Context,
        course: CourseEntry,
        matiere: String,
        divinite: String,
        ethos: String,
        userAge: Int,
        userClass: String,
        userMood: String
    ): List<QuizQuestion> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "═══ BLOC 3B — Démarrage entraînement ciblé ═══")
            Log.d(TAG, "Matière: $matiere | Savoir: ${course.topic}")
            
            // ═══════════════════════════════════════════════════════════
            // BLOC 3B — ANALYSE FAIBLESSES SPÉCIFIQUES
            // ═══════════════════════════════════════════════════════════
            
            val insights = try {
                UserAnalyticsEngine.analyzeUser(
                    context = context,
                    subject = matiere,
                    recentOnly = true
                )
            } catch (e: Exception) {
                Log.w(TAG, "Analyse insights échouée: ${e.message}")
                emptyList()
            }
            
            val weaknesses = insights.filter { 
                it.type == InsightType.WEAKNESS && it.isAlert() 
            }
            
            val confusions = insights.filter { 
                it.type == InsightType.CONFUSION 
            }
            
            Log.d(TAG, "Faiblesses détectées: ${weaknesses.size}")
            Log.d(TAG, "Confusions détectées: ${confusions.size}")
            
            // Construire le contexte de ciblage
            val targetingContext = buildTargetingContext(weaknesses, confusions, course)
            Log.d(TAG, "Contexte de ciblage: $targetingContext")
            
            // ═══════════════════════════════════════════════════════════
            // BLOC 2C — GÉNÉRATION PROFONDE (conservée)
            // BLOC 3B — Enrichie avec ciblage
            // BLOC 4A — Enrichie avec types variés
            // ═══════════════════════════════════════════════════════════
            
            val questionPool = generateDeepQuestionPool(
                context = context,
                course = course,
                matiere = matiere,
                divinite = divinite,
                ethos = ethos,
                userAge = userAge,
                userClass = userClass,
                userMood = userMood,
                targetingContext = targetingContext
            )
            
            if (questionPool.isEmpty()) {
                Log.e(TAG, "Pool vide après génération")
                return@withContext emptyList()
            }
            
            Log.d(TAG, "Pool généré: ${questionPool.size} questions")
            
            // Filtrer doublons
            val uniqueQuestions = filterDuplicates(questionPool)
            Log.d(TAG, "Après déduplication: ${uniqueQuestions.size} questions")
            
            // Prendre les 30 premières
            val finalQuestions = uniqueQuestions.take(TARGET_QUESTION_COUNT)
            
            Log.d(TAG, "═══ Entraînement terminé : ${finalQuestions.size} questions ═══")
            
            return@withContext finalQuestions
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur génération entraînement", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * BLOC 3B — Construit le contexte de ciblage selon faiblesses.
     */
    private fun buildTargetingContext(
        weaknesses: List<UserInsight>,
        confusions: List<UserInsight>,
        course: CourseEntry
    ): String {
        if (weaknesses.isEmpty() && confusions.isEmpty()) {
            return "Aucune faiblesse spécifique détectée. Exploration générale du savoir."
        }
        
        val weakTopics = weaknesses.mapNotNull { it.topic }.distinct()
        val confusedConcepts = confusions.mapNotNull { it.topic }.distinct()
        
        return buildString {
            appendLine("CIBLAGE INTELLIGENT ACTIVÉ :")
            
            if (weakTopics.isNotEmpty()) {
                appendLine()
                appendLine("FAIBLESSES DÉTECTÉES (à renforcer) :")
                weakTopics.forEach { topic ->
                    appendLine("  - $topic (erreurs fréquentes)")
                }
            }
            
            if (confusedConcepts.isNotEmpty()) {
                appendLine()
                appendLine("CONFUSIONS DÉTECTÉES (clarifier) :")
                confusedConcepts.forEach { concept ->
                    appendLine("  - $concept (erreurs répétées)")
                }
            }
            
            appendLine()
            appendLine("CONSIGNE GÉNÉRATION :")
            appendLine("- Génère des questions qui CIBLENT spécifiquement ces points faibles")
            appendLine("- Varie les formulations pour tester la compréhension réelle")
            appendLine("- Inclus des questions sur les bases si confusion détectée")
            appendLine("- Alterne entre vérification et approfondissement")
        }.trim()
    }
    
    /**
     * BLOC 2C + BLOC 3B + BLOC 4A — Génération multi-appels ciblée avec types variés.
     */
    private suspend fun generateDeepQuestionPool(
        context: Context,
        course: CourseEntry,
        matiere: String,
        divinite: String,
        ethos: String,
        userAge: Int,
        userClass: String,
        userMood: String,
        targetingContext: String
    ): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val allQuestions = mutableListOf<QuizQuestion>()
        
        // Contextes variés pour chaque appel (BLOC 2C conservé)
        val contexts = listOf(
            null,  // Standard
            "formulations différentes et angles variés",
            "aspects plus subtils et applications pratiques",
            "difficulté progressive sur les détails fins"
        )
        
        for (callIndex in 0 until MAX_GEMINI_CALLS) {
            try {
                val adaptiveNote = contexts.getOrNull(callIndex)
                
                // Construire prompt de base
                val basePrompt = buildBasePrompt(
                    course = course,
                    matiere = matiere,
                    divinite = divinite,
                    ethos = ethos,
                    userAge = userAge,
                    userClass = userClass,
                    userMood = userMood,
                    adaptiveNote = adaptiveNote
                )
                
                // BLOC 4A — Enrichir avec types variés
                val multiTypePrompt = GeminiQuestionTypeHelper.buildMultiTypePrompt(
                    basePrompt = basePrompt,
                    subject = matiere,
                    topic = course.topic,
                    userAge = userAge,
                    questionCount = 8,
                    allowImages = true
                )
                
                // BLOC 3B — Ajouter contexte de ciblage
                val finalPrompt = """
                    $multiTypePrompt
                    
                    $targetingContext
                """.trimIndent()
                
                Log.d(TAG, "Appel Gemini ${callIndex + 1}/$MAX_GEMINI_CALLS")

                val geminiResponse = try {
                    GeminiManager.genererContenuOracle(
                        texte = course.content,
                        age = userAge,
                        classe = userClass,
                        matiere = matiere,
                        divinite = divinite,
                        ethos = ethos,
                        mood = userMood,
                        adaptiveContextNote = targetingContext
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Appel ${callIndex + 1} échoué: ${e.message}")
                    null
                }
                
                if (geminiResponse != null) {
                    val (_, questions) = IAristoteEngine.decoderReponse(geminiResponse)
                        ?: (null to emptyList<QuizQuestion>())
                    
                    val enrichedQuestions = questions.map { q ->
                        q.copy(
                            subject = matiere,
                            courseId = course.id.toString(),
                            difficulty = inferDifficulty(q, userAge)
                        )
                    }
                    
                    allQuestions.addAll(enrichedQuestions)
                    Log.d(TAG, "  +${enrichedQuestions.size} questions (types: ${enrichedQuestions.groupBy { it.questionType }.keys})")
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "Erreur appel ${callIndex + 1}: ${e.message}")
            }
            
            // Stop si on a déjà assez de questions
            if (allQuestions.size >= TARGET_QUESTION_COUNT) {
                Log.d(TAG, "Objectif atteint après ${callIndex + 1} appels")
                break
            }
        }
        
        return@withContext allQuestions
    }
    
    /**
     * Filtre les doublons (BLOC 2C conservé).
     */
    private fun filterDuplicates(questions: List<QuizQuestion>): List<QuizQuestion> {
        val seen = mutableSetOf<String>()
        return questions.filter { q ->
            val normalizedText = q.text.lowercase().replace(Regex("\\s+"), " ").trim()
            val normalizedOptions = listOf(q.optionA, q.optionB, q.optionC)
                .map { it.lowercase().trim() }
                .sorted()
                .joinToString("|")
            val normalizedAnswer = q.correctAnswer.uppercase().trim()
            
            val signature = "$normalizedText::$normalizedOptions::$normalizedAnswer"
            seen.add(signature)
        }
    }
    
    private fun inferDifficulty(question: QuizQuestion, userAge: Int): Int {
        val textLength = question.text.length
        val hasComplexOptions = listOf(question.optionA, question.optionB, question.optionC)
            .any { it.length > 50 }
        
        return when {
            userAge < 12 && textLength < 100 -> 1
            userAge < 15 && !hasComplexOptions -> 1
            userAge >= 16 && (textLength > 200 || hasComplexOptions) -> 3
            else -> 2
        }
    }
    
    private fun buildBasePrompt(
        course: CourseEntry,
        matiere: String,
        divinite: String,
        ethos: String,
        userAge: Int,
        userClass: String,
        userMood: String,
        adaptiveNote: String?
    ): String {
        val adaptiveSection = if (adaptiveNote != null) {
            "\nCONSIGNE SPÉCIFIQUE : $adaptiveNote"
        } else {
            ""
        }
        
        return """
        Tu es $divinite, dieu/déesse de $matiere.
        Ton approche pédagogique : $ethos.
        
        CONTEXTE ÉLÈVE :
        - Âge : $userAge ans
        - Classe : $userClass
        - Humeur : $userMood
        
        SAVOIR SOURCE :
        Matière : $matiere
        ${if (course.topic.isNotBlank()) "Thème : ${course.topic}" else ""}
        
        Contenu du cours :
        ${course.content}
        $adaptiveSection
        """.trimIndent()
    }
}

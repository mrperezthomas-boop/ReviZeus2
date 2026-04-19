package com.revizeus.app.models

import android.util.Log

/**
 * ═══════════════════════════════════════════════════════════════
 * IARISTOTE ENGINE — Décodeur enrichi multi-types
 * ═══════════════════════════════════════════════════════════════
 */
class IAristoteEngine(private val profile: UserProfile) {
    
    fun calculateCurrentFocus(): String = profile.mood
    fun getLogicLevel(): Int = profile.statLogique
    
    companion object {
        private const val TAG = "REVIZEUS"
        
        /**
         * Décode la réponse brute de l'IA avec support multi-types.
         */
        fun decoderReponse(raw: String): Pair<String, List<QuizQuestion>>? {
            return try {
                val resume = extraireBloc(
                    raw = raw,
                    startMarker = "---START_RESUME---",
                    endMarker = "---END_RESUME---"
                )
                
                val quizPart = extraireBloc(
                    raw = raw,
                    startMarker = "---START_QUIZ---",
                    endMarker = "---END_QUIZ---"
                )
                
                if (resume.isBlank()) {
                    Log.e(TAG, "IAristoteEngine : bloc résumé vide")
                    return null
                }
                
                if (quizPart.isBlank()) {
                    Log.e(TAG, "IAristoteEngine : bloc quiz vide")
                    return null
                }
                
                val questions = parseMultiTypeQuestions(quizPart)
                
                Log.d(TAG, "IAristoteEngine : ${questions.size} questions valides parsées")
                
                if (resume.isNotBlank() && questions.isNotEmpty()) {
                    Pair(resume, questions)
                } else {
                    Log.e(TAG, "IAristoteEngine : format final inexploitable")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur IAristoteEngine : ${e.message}", e)
                null
            }
        }
        
        private fun parseMultiTypeQuestions(quizPart: String): List<QuizQuestion> {
            val questions = mutableListOf<QuizQuestion>()
            
            val questionBlocks = quizPart.split(Regex("(?=Q\\d+\\s*:)"))
                .filter { it.trim().isNotEmpty() }
            
            questionBlocks.forEachIndexed { index, block ->
                try {
                    val question = parseQuestionBlock(block.trim(), index + 1)
                    if (question != null && question.isUsable()) {
                        questions.add(question)
                    } else {
                        Log.w(TAG, "Question $index rejetée (format invalide)")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Erreur parsing question $index : ${e.message}")
                }
            }
            
            return questions
        }
        
        private fun parseQuestionBlock(block: String, index: Int): QuizQuestion? {
            return when {
                block.contains("REP_MULTIPLE:", ignoreCase = true) -> parseQcmMultiple(block, index)
                block.contains("REP_SHORT:", ignoreCase = true) -> parseOpenShort(block, index)
                block.contains("IMAGE_PROMPT:", ignoreCase = true) -> parseImageQuestion(block, index)
                block.contains("A) Vrai", ignoreCase = true) && 
                    block.contains("B) Faux", ignoreCase = true) -> parseTrueFalse(block, index)
                else -> parseQcmSimple(block, index)
            }
        }
        
        private fun parseQcmSimple(block: String, index: Int): QuizQuestion? {
            val regex = Regex(
                """Q\d+\s*:\s*(.*?)\s*A\)\s*(.*?)\s*B\)\s*(.*?)\s*C\)\s*(.*?)\s*REP\s*:\s*([A-C])""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            
            val match = regex.find(block) ?: return null
            val (questionText, optA, optB, optC, rep) = match.destructured
            
            return QuizQuestion(
                index = index,
                text = nettoyerChamp(questionText),
                optionA = nettoyerChamp(optA),
                optionB = nettoyerChamp(optB),
                optionC = nettoyerChamp(optC),
                correctAnswer = nettoyerChamp(rep).uppercase(),
                questionType = QuestionType.QCM_SIMPLE
            )
        }
        
        private fun parseTrueFalse(block: String, index: Int): QuizQuestion? {
            val regex = Regex(
                """Q\d+\s*:\s*(.*?)\s*A\)\s*Vrai\s*B\)\s*Faux\s*C\)\s*(.*?)\s*REP\s*:\s*([AB])""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            
            val match = regex.find(block) ?: return null
            val (questionText, _, rep) = match.destructured
            
            val correctAnswer = if (rep.trim().uppercase() == "A") "VRAI" else "FAUX"
            
            return QuizQuestion(
                index = index,
                text = nettoyerChamp(questionText),
                optionA = "Vrai",
                optionB = "Faux",
                optionC = "",
                correctAnswer = correctAnswer,
                questionType = QuestionType.TRUE_FALSE
            )
        }
        
        private fun parseQcmMultiple(block: String, index: Int): QuizQuestion? {
            val regex = Regex(
                """Q\d+\s*:\s*(.*?)\s*A\)\s*(.*?)\s*B\)\s*(.*?)\s*C\)\s*(.*?)\s*(?:D\)\s*(.*?))?\s*REP_MULTIPLE\s*:\s*([A-D,\s]+)""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            
            val match = regex.find(block) ?: return null
            val (questionText, optA, optB, optC, optD, repMultiple) = match.destructured
            
            return QuizQuestion(
                index = index,
                text = nettoyerChamp(questionText),
                optionA = nettoyerChamp(optA),
                optionB = nettoyerChamp(optB),
                optionC = nettoyerChamp(optC),
                optionD = nettoyerChamp(optD),
                correctAnswer = "",
                correctAnswersMultiple = nettoyerChamp(repMultiple).uppercase(),
                questionType = QuestionType.QCM_MULTIPLE
            )
        }
        
        private fun parseOpenShort(block: String, index: Int): QuizQuestion? {
            val regex = Regex(
                """Q\d+\s*:\s*(.*?)\s*(?:A\)\s*B\)\s*C\)\s*)?\s*REP_SHORT\s*:\s*(.*?)(?:\s*VARIANTS\s*:\s*(.*))?$""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            
            val match = regex.find(block) ?: return null
            val groups = match.groupValues
            val questionText = groups.getOrNull(1) ?: ""
            val shortAnswer = groups.getOrNull(2) ?: ""
            val variants = groups.getOrNull(3) ?: ""
            
            return QuizQuestion(
                index = index,
                text = nettoyerChamp(questionText),
                optionA = "",
                optionB = "",
                optionC = "",
                correctAnswer = "",
                expectedShortAnswer = nettoyerChamp(shortAnswer),
                acceptedVariants = nettoyerChamp(variants),
                questionType = QuestionType.OPEN_SHORT
            )
        }
        
        private fun parseImageQuestion(block: String, index: Int): QuizQuestion? {
            val imagePromptRegex = Regex("""\[IMAGE_PROMPT:\s*(.*?)\]""", RegexOption.IGNORE_CASE)
            val imagePromptMatch = imagePromptRegex.find(block)
            val imagePrompt = imagePromptMatch?.groupValues?.getOrNull(1) ?: ""
            
            val cleanBlock = block.replace(imagePromptRegex, "")
            
            val questionType = if (cleanBlock.contains("REP_SHORT:", ignoreCase = true)) {
                QuestionType.IMAGE_IDENTIFY
            } else {
                QuestionType.IMAGE_QCM
            }
            
            val baseQuestion = if (questionType == QuestionType.IMAGE_IDENTIFY) {
                parseOpenShort(cleanBlock, index)
            } else {
                parseQcmSimple(cleanBlock, index)
            }
            
            return baseQuestion?.copy(
                questionType = questionType,
                imagePrompt = imagePrompt.trim(),
                imageUrl = "",
                imageDescription = imagePrompt.trim()
            )
        }
        
        private fun extraireBloc(raw: String, startMarker: String, endMarker: String): String {
            return raw.substringAfter(startMarker, "").substringBefore(endMarker, "").trim()
        }
        
        private fun nettoyerChamp(value: String): String {
            return value
                .replace("\r", "")
                .replace("\n", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
}

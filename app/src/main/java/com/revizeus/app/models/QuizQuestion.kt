package com.revizeus.app.models

import java.io.Serializable

/**
 * ═══════════════════════════════════════════════════════════════
 * QUIZ QUESTION — Modèle enrichi multi-types
 * ═══════════════════════════════════════════════════════════════
 * 
 * BLOC 4A — TYPES DE QUESTIONS VARIÉS
 * 
 * Extension du modèle existant pour supporter :
 * - QCM classique (A/B/C)
 * - Vrai/Faux
 * - QCM multiple (plusieurs bonnes réponses)
 * - Questions ouvertes courtes
 * - Questions avec images
 * 
 * CONSERVATION TOTALE :
 * - Tous les champs existants conservés
 * - Compatibilité totale avec ancien système
 * - Nouveaux champs optionnels avec valeurs par défaut
 * 
 * ═══════════════════════════════════════════════════════════════
 */
data class QuizQuestion(
    val index: Int,
    val text: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val correctAnswer: String,
    
    /** Matière source réelle de la question */
    val subject: String = "",
    
    /** Identifiant du savoir source si connu */
    val courseId: String = "",
    
    /** Niveau de difficulté (1-3) */
    val difficulty: Int = 1,
    
    // ═══════════════════════════════════════════════════════════════
    // BLOC 4A — NOUVEAUX CHAMPS MULTI-TYPES
    // ═══════════════════════════════════════════════════════════════
    
    /** Type de question (QCM_SIMPLE par défaut pour rétrocompat) */
    val questionType: QuestionType = QuestionType.QCM_SIMPLE,
    
    /** Option D (pour QCM à 4 choix si nécessaire) */
    val optionD: String = "",
    
    /** Réponses correctes multiples (pour QCM_MULTIPLE) - ex: "A,C" */
    val correctAnswersMultiple: String = "",
    
    /** Réponse courte attendue (pour OPEN_SHORT) */
    val expectedShortAnswer: String = "",
    
    /** Variantes acceptées pour réponse courte (séparées par |) */
    val acceptedVariants: String = "",
    
    /** URL de l'image générée par IA (pour IMAGE_QCM, IMAGE_IDENTIFY) */
    val imageUrl: String = "",
    
    /** Prompt utilisé pour générer l'image (pour debug) */
    val imagePrompt: String = "",
    
    /** Description de l'image pour accessibilité */
    val imageDescription: String = ""
    
) : Serializable {
    
    /**
     * Normalise la réponse correcte (uppercase, trim).
     */
    fun normalizedCorrectAnswer(): String = correctAnswer.trim().uppercase()
    
    /**
     * Vérifie si la question est utilisable.
     * CONSERVATION : Logique existante + validation nouveaux types.
     */
    fun isUsable(): Boolean {
        // Validation base commune
        if (text.isBlank()) return false
        
        return when (questionType) {
            QuestionType.QCM_SIMPLE -> {
                // QCM classique : A/B/C valides
                optionA.isNotBlank() &&
                optionB.isNotBlank() &&
                optionC.isNotBlank() &&
                normalizedCorrectAnswer() in setOf("A", "B", "C")
            }
            
            QuestionType.TRUE_FALSE -> {
                // Vrai/Faux : réponse VRAI ou FAUX
                normalizedCorrectAnswer() in setOf("VRAI", "FAUX", "V", "F", "TRUE", "FALSE")
            }
            
            QuestionType.QCM_MULTIPLE -> {
                // QCM multiple : au moins 2 options + réponses multiples
                optionA.isNotBlank() &&
                optionB.isNotBlank() &&
                correctAnswersMultiple.isNotBlank() &&
                correctAnswersMultiple.split(",").size >= 2
            }
            
            QuestionType.OPEN_SHORT -> {
                // Question ouverte : réponse attendue présente
                expectedShortAnswer.isNotBlank()
            }
            
            QuestionType.IMAGE_QCM -> {
                // Image QCM : image + options
                imageUrl.isNotBlank() &&
                optionA.isNotBlank() &&
                optionB.isNotBlank() &&
                optionC.isNotBlank() &&
                normalizedCorrectAnswer() in setOf("A", "B", "C")
            }
            
            QuestionType.IMAGE_IDENTIFY -> {
                // Identifier image : image + réponse attendue
                imageUrl.isNotBlank() &&
                expectedShortAnswer.isNotBlank()
            }
        }
    }
    
    /**
     * Vérifie si une réponse utilisateur est correcte.
     * 
     * @param userAnswer Réponse brute de l'utilisateur
     * @return true si correcte
     */
    fun isAnswerCorrect(userAnswer: String): Boolean {
        val normalized = userAnswer.trim().uppercase()
        
        return when (questionType) {
            QuestionType.QCM_SIMPLE, QuestionType.IMAGE_QCM -> {
                normalized == normalizedCorrectAnswer()
            }
            
            QuestionType.TRUE_FALSE -> {
                val correctNorm = normalizedCorrectAnswer()
                normalized == correctNorm ||
                (correctNorm in setOf("VRAI", "V", "TRUE") && normalized in setOf("VRAI", "V", "TRUE")) ||
                (correctNorm in setOf("FAUX", "F", "FALSE") && normalized in setOf("FAUX", "F", "FALSE"))
            }
            
            QuestionType.QCM_MULTIPLE -> {
                // Vérifier que toutes les bonnes réponses sont cochées
                val expectedSet = correctAnswersMultiple.split(",").map { it.trim().uppercase() }.toSet()
                val userSet = normalized.split(",").map { it.trim() }.toSet()
                expectedSet == userSet
            }
            
            QuestionType.OPEN_SHORT, QuestionType.IMAGE_IDENTIFY -> {
                val expectedNorm = expectedShortAnswer.trim().uppercase()
                val variants = if (acceptedVariants.isNotBlank()) {
                    acceptedVariants.split("|").map { it.trim().uppercase() }
                } else {
                    emptyList()
                }
                
                // Accepter réponse exacte ou variantes
                normalized == expectedNorm || normalized in variants
            }
        }
    }
    
    /**
     * Retourne une aide textuelle selon le type de question.
     */
    fun getHintText(): String {
        return when (questionType) {
            QuestionType.QCM_SIMPLE -> "Choisis une seule réponse (A, B ou C)"
            QuestionType.TRUE_FALSE -> "Vrai ou Faux ?"
            QuestionType.QCM_MULTIPLE -> "Plusieurs bonnes réponses possibles"
            QuestionType.OPEN_SHORT -> "Réponds en 1-3 mots"
            QuestionType.IMAGE_QCM -> "Observe l'image et choisis la bonne réponse"
            QuestionType.IMAGE_IDENTIFY -> "Qu'est-ce qui est montré dans l'image ?"
        }
    }
    
    /**
     * Retourne le nombre de points de cette question selon sa difficulté et son type.
     */
    fun getPoints(): Int {
        val basePoints = when (difficulty) {
            1 -> 10
            2 -> 15
            3 -> 20
            else -> 10
        }
        
        // Bonus selon complexité du type
        val typeBonus = when (questionType) {
            QuestionType.QCM_SIMPLE -> 0
            QuestionType.TRUE_FALSE -> 0
            QuestionType.QCM_MULTIPLE -> 5  // Plus difficile
            QuestionType.OPEN_SHORT -> 5     // Demande réflexion
            QuestionType.IMAGE_QCM -> 3      // Analyse visuelle
            QuestionType.IMAGE_IDENTIFY -> 7 // Reconnaissance + réflexion
        }
        
        return basePoints + typeBonus
    }
}

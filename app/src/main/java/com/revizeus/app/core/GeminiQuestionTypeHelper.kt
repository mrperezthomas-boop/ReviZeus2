package com.revizeus.app.core

import com.revizeus.app.models.QuestionType

/**
 * ═══════════════════════════════════════════════════════════════
 * GEMINI QUESTION TYPE HELPER — Génération IA multi-types
 * ═══════════════════════════════════════════════════════════════
 * 
 * BLOC 4A — TYPES DE QUESTIONS VARIÉS
 * 
 * Rôle :
 * Enrichit les prompts Gemini pour générer des questions variées
 * selon le savoir, la matière et le profil utilisateur.
 * 
 * Principe CORE de l'app :
 * - TOUT est généré par l'IA selon données utilisateur
 * - Pas de templates fixes, toujours contextuel
 * - Variation intelligente selon le savoir
 * 
 * ═══════════════════════════════════════════════════════════════
 */
object GeminiQuestionTypeHelper {
    
    /**
     * Génère un prompt enrichi pour demander un mélange intelligent
     * de types de questions à Gemini.
     * 
     * @param basePrompt Prompt de base existant
     * @param subject Matière du cours
     * @param topic Thème précis (optionnel)
     * @param userAge Âge du joueur
     * @param questionCount Nombre de questions à générer
     * @param allowImages Si true, Gemini peut suggérer des questions avec images
     * @return Prompt enrichi avec instructions multi-types
     */
    fun buildMultiTypePrompt(
        basePrompt: String,
        subject: String,
        topic: String? = null,
        userAge: Int,
        questionCount: Int = 8,
        allowImages: Boolean = true
    ): String {
        val imageInstructions = if (allowImages && shouldUseImages(subject, topic)) {
            """
            
            SUPPORT IMAGES (OPTIONNEL) :
            Pour les questions visuellement pertinentes (anatomie, géographie, art, monuments historiques),
            tu PEUX proposer des questions de type IMAGE_QCM ou IMAGE_IDENTIFY.
            
            FORMAT IMAGE :
            Si tu proposes une question avec image :
            - Ajoute [IMAGE_PROMPT: description détaillée pour générer l'image]
            - Le prompt image doit être en anglais, très précis et descriptif
            - Exemple : [IMAGE_PROMPT: detailed anatomical diagram of human heart showing ventricles and valves, medical illustration style]
            
            Types de questions avec image supportés :
            - IMAGE_QCM : Question avec image de support + choix A/B/C
            - IMAGE_IDENTIFY : "Qu'est-ce qui est montré ?" + réponse courte attendue
            """.trimIndent()
        } else {
            ""
        }
        
        return """
        $basePrompt
        
        ═══════════════════════════════════════════════════════════════
        CONSIGNES TYPES DE QUESTIONS — VARIATION INTELLIGENTE
        ═══════════════════════════════════════════════════════════════
        
        Tu dois générer $questionCount questions en VARIANT LES TYPES selon la pertinence du savoir.
        
        TYPES DISPONIBLES :
        
        1. QCM_SIMPLE (type classique A/B/C) :
           Format : Q1: [texte question] A) [option] B) [option] C) [option] REP: [A|B|C]
           Usage : Concepts avec plusieurs aspects, comparaisons, définitions complexes
        
        2. TRUE_FALSE (Vrai/Faux) :
           Format : Q1: [affirmation] A) Vrai B) Faux C) [laisse vide] REP: [A|B]
           Usage : Vérifier une affirmation précise, dates, faits historiques
           IMPORTANT : Option C doit rester vide pour ce type
        
        3. QCM_MULTIPLE (plusieurs bonnes réponses) :
           Format : Q1: [texte question] A) [option] B) [option] C) [option] D) [option] REP_MULTIPLE: [A,C]
           Usage : Lister des caractéristiques, propriétés multiples, catégorisations
           IMPORTANT : Utilise REP_MULTIPLE au lieu de REP
        
        4. OPEN_SHORT (réponse courte 1-3 mots) :
           Format : Q1: [question] A) [laisse vide] B) [laisse vide] C) [laisse vide] REP_SHORT: [réponse attendue] VARIANTS: [variante1|variante2]
           Usage : Dates, noms propres, formules, capitales, définitions courtes
           IMPORTANT : Options A/B/C doivent rester vides, ajoute VARIANTS pour variantes acceptées
        
        $imageInstructions
        
        RÈGLES DE VARIATION :
        - Sur $questionCount questions, vise environ :
          * 40-50% QCM_SIMPLE (base solide)
          * 20-30% TRUE_FALSE (vérification rapide)
          * 10-20% QCM_MULTIPLE (si pertinent pour le savoir)
          * 10-20% OPEN_SHORT (concepts précis)
          * 0-10% IMAGE (si pertinent et allowImages activé)
        
        - Adapte selon le savoir :
          * Dates historiques → TRUE_FALSE ou OPEN_SHORT
          * Définitions → QCM_SIMPLE
          * Propriétés multiples → QCM_MULTIPLE
          * Noms/Formules → OPEN_SHORT
          * Anatomie/Géographie → IMAGE_QCM si pertinent
        
        - Adapte selon l'âge :
          * Âge < 12 : favorise QCM_SIMPLE et TRUE_FALSE (plus simples)
          * Âge 12-16 : mélange équilibré
          * Âge > 16 : peut inclure plus de QCM_MULTIPLE et OPEN_SHORT
        
        CONTEXTE UTILISATEUR :
        - Âge : $userAge ans
        - Matière : $subject
        ${if (topic != null) "- Thème : $topic" else ""}
        
        IMPORTANT :
        - VARIE les types pour rendre le quiz dynamique et engageant
        - Choisis le type SELON LA PERTINENCE du contenu, pas au hasard
        - Si un concept se prête mieux à un Vrai/Faux, fais un Vrai/Faux
        - Si une question demande une liste, fais un QCM_MULTIPLE
        - Garde toujours le format de réponse STRICTEMENT comme indiqué
        
        ═══════════════════════════════════════════════════════════════
        """.trimIndent()
    }
    
    /**
     * Détermine si le savoir se prête bien aux questions avec images.
     */
    private fun shouldUseImages(subject: String, topic: String?): Boolean {
        val visualSubjects = listOf(
            "SVT", "Biologie", "Anatomie",
            "Géographie", "Cartographie",
            "Art", "Musique", "Histoire de l'art",
            "Histoire", "Archéologie",
            "Physique", "Chimie", "Sciences"
        )
        
        val visualTopics = listOf(
            "anatomie", "corps humain", "cellule", "organe",
            "carte", "relief", "climat", "pays", "continent",
            "peinture", "sculpture", "monument", "architecture",
            "portrait", "bataille", "château",
            "schéma", "circuit", "molécule", "atome"
        )
        
        return visualSubjects.any { subject.contains(it, ignoreCase = true) } ||
               (topic != null && visualTopics.any { topic.contains(it, ignoreCase = true) })
    }
    
    /**
     * Génère un prompt spécifique pour un entraînement ciblé sur un type.
     * Utilisé si on veut forcer un type particulier.
     */
    fun buildSingleTypePrompt(
        basePrompt: String,
        questionType: QuestionType,
        questionCount: Int
    ): String {
        val typeInstructions = when (questionType) {
            QuestionType.QCM_SIMPLE -> """
                Génère $questionCount questions au format QCM classique (A/B/C).
                Format strict : Q1: [texte] A) [option] B) [option] C) [option] REP: [A|B|C]
            """.trimIndent()
            
            QuestionType.TRUE_FALSE -> """
                Génère $questionCount affirmations Vrai/Faux.
                Format strict : Q1: [affirmation] A) Vrai B) Faux C) REP: [A|B]
                IMPORTANT : Option C reste vide.
            """.trimIndent()
            
            QuestionType.QCM_MULTIPLE -> """
                Génère $questionCount questions à choix multiples (plusieurs bonnes réponses).
                Format strict : Q1: [texte] A) [opt] B) [opt] C) [opt] D) [opt] REP_MULTIPLE: [A,C]
            """.trimIndent()
            
            QuestionType.OPEN_SHORT -> """
                Génère $questionCount questions à réponse courte (1-3 mots).
                Format strict : Q1: [question] A) B) C) REP_SHORT: [réponse] VARIANTS: [var1|var2]
                IMPORTANT : Options A/B/C restent vides.
            """.trimIndent()
            
            QuestionType.IMAGE_QCM -> """
                Génère $questionCount questions avec image de support.
                Format strict : Q1: [question] [IMAGE_PROMPT: prompt anglais] A) [opt] B) [opt] C) [opt] REP: [A|B|C]
            """.trimIndent()
            
            QuestionType.IMAGE_IDENTIFY -> """
                Génère $questionCount questions "Identifier l'image".
                Format strict : Q1: Qu'est-ce qui est montré ? [IMAGE_PROMPT: prompt anglais] A) B) C) REP_SHORT: [réponse] VARIANTS: [var1|var2]
            """.trimIndent()
        }
        
        return """
        $basePrompt
        
        ═══════════════════════════════════════════════════════════════
        TYPE DE QUESTION CIBLÉ : ${questionType.displayName}
        ═══════════════════════════════════════════════════════════════
        
        $typeInstructions
        
        IMPORTANT :
        - Respecte STRICTEMENT le format indiqué
        - Toutes les questions doivent être de ce type uniquement
        - Garde la cohérence avec le savoir fourni
        
        ═══════════════════════════════════════════════════════════════
        """.trimIndent()
    }
}

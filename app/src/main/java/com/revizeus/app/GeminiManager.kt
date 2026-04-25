package com.revizeus.app

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.revizeus.app.ai.AiInvocationGateway
import com.revizeus.app.ai.FunctionsAiGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * ═══════════════════════════════════════════════════════════════
 * GESTIONNAIRE DE L'OLYMPE (IA Gemini)
 * ═══════════════════════════════════════════════════════════════
 * Utilité : Orchestration centrale des flux IA de RéviZeus.
 *
 * CHANGEMENTS :
 * ✅ Support multi-pages conservé.
 * ✅ FIX POINT 6 :
 *    Les résumés ne doivent plus dériver vers des "histoires fantastiques".
 *    On sépare désormais clairement :
 *      - l'habillage divin / immersif
 *      - le contenu pédagogique concret
 *
 * ✅ Le résumé s'adapte maintenant à :
 *      - l'âge
 *      - la classe / niveau scolaire
 *      - l'humeur actuelle
 *
 * ✅ Le résumé reste fidèle, structuré, concret et exploitable pour réviser.
 * ✅ Le dieu sert de posture pédagogique, pas de moteur de narration fantaisie.
 *
 * CONSOLIDATION :
 * ✅ Retry simple sur l'appel IA pour limiter les faux échecs réseau.
 * ✅ Normalisation légère de la réponse brute.
 * ✅ Contrat de structure rappelé encore plus explicitement dans les prompts.
 *
 * AJOUT ADAPTATIF SÛR :
 * ✅ adaptiveContextNote optionnel sur le moteur Oracle texte / image
 * ✅ adaptiveContextNote optionnel sur le moteur de dialogues divins
 * ✅ Aucun appel existant cassé grâce aux valeurs par défaut
 *
 * [2026-04-20 23:59][TRANSPORT_IA_TOTAL]
 * ✅ La clé Gemini est sortie du client pour le transport Oracle (callable sécurisée).
 * ✅ Flux texte libre + dialogues : Firebase Functions `invokeDivineOracle` (sans images).
 * ✅ Flux image : même callable pour ne rien casser ; point d’extension `oracleVisionTransport`.
 * ✅ GeminiManager reste la façade métier centrale.
 * ✅ Les prompts, system instructions et parseurs restent dans GeminiManager.
 *
 * [2026-04-20 23:59][PATCH_429_MINIMAL]
 * ✅ Backoff plus propre côté Android quand Vertex renvoie RESOURCE_EXHAUSTED / 429.
 * ═══════════════════════════════════════════════════════════════
 */
object GeminiManager {

    private const val TAG = "REVIZEUS"
    private const val MODEL_NAME = "gemini-2.5-flash"
    private const val MAX_RETRIES = 2
    private const val ORACLE_FUNCTIONS_REGION = "us-central1"
    private const val RESOURCE_EXHAUSTED_BACKOFF_MS = 3000L

    /**
     * Transport texte (Oracle FREE_TEXT_INPUT, entraînement texte, dialogues, chanson) :
     * toujours `invokeDivineOracle` côté backend — aucune clé modèle sur l’appareil.
     */
    private val oracleTextTransport: AiInvocationGateway by lazy {
        FunctionsAiGateway(FirebaseFunctions.getInstance(ORACLE_FUNCTIONS_REGION))
    }

    /**
     * Transport vision (pages scannées). Aujourd’hui identique au texte (Functions + images inline).
     * Conservé comme point d’extension distinct pour une future voie « directe » sans toucher au métier.
     */
    private val oracleVisionTransport: AiInvocationGateway by lazy {
        FunctionsAiGateway(FirebaseFunctions.getInstance(ORACLE_FUNCTIONS_REGION))
    }

    // CHANTIER 1 - FONDATION IA
    // Contrat unifié pour les dialogues IA divins.
    data class GodResponse(
        val text: String,
        val mnemo: String,
        val tone: String,
        val godName: String,
        val matiere: String,
        val suggestedAction: String
    )

    /**
     * Utilisé par l'Entraînement : Génère un QCM à partir d'un cours déjà stocké (Texte).
     *
     * AJOUT CONSERVATEUR :
     * - adaptiveContextNote est optionnel pour ne jamais casser les anciens appels.
     */
    suspend fun genererContenuOracle(
        texte: String,
        age: Int,
        classe: String,
        matiere: String,
        divinite: String,
        ethos: String,
        mood: String,
        adaptiveContextNote: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val systemInstructionText = buildOracleSystemInstruction()

            val prompt = buildStructuredPromptFromText(
                texte = texte,
                age = age,
                classe = classe,
                matiere = matiere,
                divinite = divinite,
                ethos = ethos,
                mood = mood,
                adaptiveContextNote = adaptiveContextNote
            )

            return@withContext invokeOracleTransport(
                systemInstructionText = systemInstructionText,
                prompt = prompt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur Entraînement Olympe : ${e.message}", e)
            null
        }
    }

    /**
     * Compatibilité avec ton flux existant une seule image.
     * On redirige vers la nouvelle fonction multi-pages.
     */
    suspend fun genererContenuDepuisImage(
        imageBitmap: Bitmap,
        age: Int,
        classe: String,
        matiere: String,
        divinite: String,
        ethos: String,
        mood: String,
        adaptiveContextNote: String? = null
    ): String? = genererContenuDepuisImages(
        imageBitmaps = listOf(imageBitmap),
        age = age,
        classe = classe,
        matiere = matiere,
        divinite = divinite,
        ethos = ethos,
        mood = mood,
        adaptiveContextNote = adaptiveContextNote
    )

    /**
     * Utilisé par l'Oracle : Génère un Résumé + QCM depuis une ou plusieurs photos.
     * Cela permet de traiter un cours réparti sur plusieurs pages.
     */
    suspend fun genererContenuDepuisImages(
        imageBitmaps: List<Bitmap>,
        age: Int,
        classe: String,
        matiere: String,
        divinite: String,
        ethos: String,
        mood: String,
        adaptiveContextNote: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (imageBitmaps.isEmpty()) {
                return@withContext null
            }

            val systemInstructionText = buildOracleSystemInstruction()

            val promptText = buildStructuredPromptFromImages(
                age = age,
                classe = classe,
                matiere = matiere,
                divinite = divinite,
                ethos = ethos,
                mood = mood,
                adaptiveContextNote = adaptiveContextNote
            )

            val inlineImages = imageBitmaps.map { bitmapToInlineImage(it) }

            return@withContext invokeOracleTransport(
                systemInstructionText = systemInstructionText,
                prompt = promptText,
                images = inlineImages
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur Vision Olympe multi-pages : ${e.message}", e)
            null
        }
    }

    /**
     * CHANTIER 1 - FONDATION IA
     * Nouveau point d'entrée pour les dialogues immersifs structurés.
     *
     * IMPORTANT :
     * - Cette méthode n'écrase pas le moteur Oracle existant.
     * - Elle utilise une systemInstruction dédiée au format JSON strict.
     * - Elle retourne un GodResponse parsé et sécurisé.
     */
    suspend fun generateDialog(
        prompt: String,
        matiere: String,
        adaptiveContextNote: String? = null
    ): GodResponse? = withContext(Dispatchers.IO) {
        try {
            val systemInstructionText = buildDialogSystemInstruction(matiere)

            val finalPrompt = if (adaptiveContextNote.isNullOrBlank()) {
                prompt
            } else {
                """
                $prompt

                CONTEXTE ADAPTATIF COMPLÉMENTAIRE :
                $adaptiveContextNote
                """.trimIndent()
            }

            val responseText = executeWithRetry {
                oracleTextTransport.invoke(
                    systemInstruction = systemInstructionText,
                    prompt = finalPrompt,
                    model = MODEL_NAME
                )
            }

            return@withContext parseGodResponse(responseText, matiere)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur Dialogue Olympe : ${e.message}", e)
            null
        }
    }


    /**
     * [2026-04-19 05:36][BLOC_B2][GEMINI_PATCH] Variante B2 additif :
     * construit un plan officiel puis enrichit le prompt sans modifier la logique réseau existante.
     */
    suspend fun generateDialog(
        prompt: String,
        matiere: String,
        divineRequestContext: DivineRequestContext,
        adaptiveContextNote: String? = null
    ): GodResponse? = withContext(Dispatchers.IO) {
        val resolved = resolveDivineContextAndPlan(
            matiere = matiere,
            divineRequestContext = divineRequestContext
        )

        if (resolved == null) {
            return@withContext generateDialog(
                prompt = prompt,
                matiere = matiere,
                adaptiveContextNote = adaptiveContextNote
            )
        }

        val (safeContext, plan) = resolved

        val contextualAddon = when (safeContext.actionType) {
            DivineActionType.QUIZ_CORRECTION ->
                DivinePromptBuilder.buildCorrectionPrompt(plan, safeContext)
            DivineActionType.QUIZ_GENERATION ->
                DivinePromptBuilder.buildQuizPrompt(plan, safeContext)
            DivineActionType.SUMMARY_GENERATION,
            DivineActionType.SUMMARY_REFORMULATION,
            DivineActionType.MNEMONIC ->
                DivinePromptBuilder.buildSummaryPrompt(plan, safeContext)
            DivineActionType.SYSTEM_HELP,
            DivineActionType.ERROR_EXPLANATION,
            DivineActionType.LOADING_MESSAGE ->
                DivinePromptBuilder.buildSystemHelpPrompt(plan, safeContext)
            else -> ""
        }

        val enrichedPrompt = injectDivinePlanHints(
            basePrompt = if (contextualAddon.isBlank()) prompt else "$prompt\n\n$contextualAddon",
            plan = plan
        )

        return@withContext generateDialog(
            prompt = enrichedPrompt,
            matiere = matiere,
            adaptiveContextNote = adaptiveContextNote
        )
    }


    /**
     * LYRE D'APOLLON — VERSION CHANSON STRUCTURÉE POUR L'ÉCRAN MUSIC.
     *
     * IMPORTANT :
     * - Cette méthode ne remplace pas l'hymne/poème existant.
     * - Elle sert uniquement à produire un vrai texte de chanson pédagogique
     *   pour l'écran "music" / atelier Suno.
     * - La structure doit ressembler à une chanson réelle : couplets,
     *   refrain, éventuellement pont, sans casser le sens du savoir.
     */
    suspend fun generateEducationalSongLyrics(
        courseText: String,
        age: Int,
        classe: String,
        matiere: String,
        divinite: String,
        mood: String,
        courseTitle: String,
        adaptiveContextNote: String? = null
    ): GodResponse? = withContext(Dispatchers.IO) {
        try {
            val prompt = buildEducationalSongPrompt(
                courseText = courseText,
                age = age,
                classe = classe,
                matiere = matiere,
                divinite = divinite,
                mood = mood,
                courseTitle = courseTitle,
                adaptiveContextNote = adaptiveContextNote
            )

            return@withContext generateDialog(
                prompt = prompt,
                matiere = matiere,
                adaptiveContextNote = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur Chanson Olympe : ${e.message}", e)
            null
        }
    }


    // [2026-04-19 05:36][BLOC_B2][GEMINI_PATCH] Résolution safe du contexte B2 sans casser les anciens appels.
    private fun resolveDivineContextAndPlan(
        matiere: String,
        divineRequestContext: DivineRequestContext?
    ): Pair<DivineRequestContext, DivineResponsePlan>? {
        if (divineRequestContext == null) return null

        val safeContext = if (divineRequestContext.subject.isNullOrBlank()) {
            divineRequestContext.copy(subject = matiere)
        } else {
            divineRequestContext
        }

        return safeContext to DivineResponseOrchestrator.buildResponsePlan(safeContext)
    }

    // [2026-04-19 05:36][BLOC_B2][GEMINI_PATCH] Injection minimale des hints B2 dans un prompt existant.
    private fun injectDivinePlanHints(
        basePrompt: String,
        plan: DivineResponsePlan?
    ): String {
        if (plan == null || plan.promptHints.isBlank()) return basePrompt

        return """
            $basePrompt

            PLAN DIVIN B2 :
            ${plan.promptHints}

            INDICATIONS UI B2 :
            ${plan.uiHints}
        """.trimIndent()
    }

    /**
     * [2026-04-19 05:36][BLOC_B2][GEMINI_PATCH] Variante B2 additif pour résumé/quiz depuis texte.
     */
    suspend fun genererContenuOracle(
        texte: String,
        age: Int,
        classe: String,
        matiere: String,
        divinite: String,
        ethos: String,
        mood: String,
        adaptiveContextNote: String? = null,
        divineRequestContext: DivineRequestContext
    ): String? = withContext(Dispatchers.IO) {
        try {
            val systemInstructionText = buildOracleSystemInstruction()

            val basePrompt = buildStructuredPromptFromText(
                texte = texte,
                age = age,
                classe = classe,
                matiere = matiere,
                divinite = divinite,
                ethos = ethos,
                mood = mood,
                adaptiveContextNote = adaptiveContextNote
            )

            val plan = resolveDivineContextAndPlan(
                matiere = matiere,
                divineRequestContext = divineRequestContext
            )?.second

            val prompt = injectDivinePlanHints(
                basePrompt = basePrompt,
                plan = plan
            )

            return@withContext invokeOracleTransport(
                systemInstructionText = systemInstructionText,
                prompt = prompt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur Entraînement Olympe B2 : ${e.message}", e)
            null
        }
    }

    /**
     * [2026-04-19 05:36][BLOC_B2][GEMINI_PATCH] Variante B2 additif image unique.
     */
    suspend fun genererContenuDepuisImage(
        imageBitmap: Bitmap,
        age: Int,
        classe: String,
        matiere: String,
        divinite: String,
        ethos: String,
        mood: String,
        adaptiveContextNote: String? = null,
        divineRequestContext: DivineRequestContext
    ): String? = genererContenuDepuisImages(
        imageBitmaps = listOf(imageBitmap),
        age = age,
        classe = classe,
        matiere = matiere,
        divinite = divinite,
        ethos = ethos,
        mood = mood,
        adaptiveContextNote = adaptiveContextNote,
        divineRequestContext = divineRequestContext
    )

    /**
     * [2026-04-19 05:36][BLOC_B2][GEMINI_PATCH] Variante B2 additif multi-pages.
     */
    suspend fun genererContenuDepuisImages(
        imageBitmaps: List<Bitmap>,
        age: Int,
        classe: String,
        matiere: String,
        divinite: String,
        ethos: String,
        mood: String,
        adaptiveContextNote: String? = null,
        divineRequestContext: DivineRequestContext
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (imageBitmaps.isEmpty()) {
                return@withContext null
            }

            val systemInstructionText = buildOracleSystemInstruction()

            val basePrompt = buildStructuredPromptFromImages(
                age = age,
                classe = classe,
                matiere = matiere,
                divinite = divinite,
                ethos = ethos,
                mood = mood,
                adaptiveContextNote = adaptiveContextNote
            )

            val plan = resolveDivineContextAndPlan(
                matiere = matiere,
                divineRequestContext = divineRequestContext
            )?.second

            val promptText = injectDivinePlanHints(
                basePrompt = basePrompt,
                plan = plan
            )

            val inlineImages = imageBitmaps.map { bitmapToInlineImage(it) }

            return@withContext invokeOracleTransport(
                systemInstructionText = systemInstructionText,
                prompt = promptText,
                images = inlineImages
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur Vision Olympe multi-pages B2 : ${e.message}", e)
            null
        }
    }

    // ══════════════════════════════════════════════════════════
    // BUILDERS DE PROMPTS
    // ══════════════════════════════════════════════════════════

    /**
     * Prompt dédié à la chanson pédagogique structurée.
     * Utilisé uniquement par l'écran "music" de la Lyre.
     */
    private fun buildEducationalSongPrompt(
        courseText: String,
        age: Int,
        classe: String,
        matiere: String,
        divinite: String,
        mood: String,
        courseTitle: String,
        adaptiveContextNote: String? = null
    ): String {
        val adaptationPedagogique = buildAdaptationPedagogique(
            age = age,
            classe = classe,
            mood = mood,
            matiere = matiere
        )

        val adaptiveBlock = buildAdaptiveContextSection(adaptiveContextNote)
        val structureNote = if (courseText.length >= 1400) {
            "Structure longue autorisée : [Couplet 1], [Refrain], [Couplet 2], [Refrain], [Pont], [Refrain final]."
        } else if (courseText.length >= 800) {
            "Structure moyenne recommandée : [Couplet 1], [Refrain], [Couplet 2], [Refrain final]."
        } else {
            "Structure courte recommandée : [Couplet 1], [Refrain], [Couplet 2], [Refrain final], en restant dense et clair."
        }

        return """
            Tu es $divinite, mentor pédagogique de la matière $matiere.
            Tu dois transformer un savoir réel en TEXTE DE CHANSON pédagogique exploitable dans une application éducative.

            CONTEXTE ÉLÈVE :
            - âge : $age ans
            - classe : $classe
            - humeur : $mood
            - titre du savoir : $courseTitle

            ADAPTATION PÉDAGOGIQUE :
            $adaptationPedagogique

            $adaptiveBlock

            OBJECTIF CRITIQUE :
            - Tu ne dois PAS écrire un simple poème.
            - Tu dois écrire de vraies paroles de chanson.
            - Le texte final doit avoir une structure musicale claire.
            - Les notions réelles du savoir doivent rester présentes et compréhensibles.

            STRUCTURE OBLIGATOIRE DU CHAMP "text" :
            $structureNote
            - Utilise explicitement les balises : [Couplet 1], [Refrain], [Couplet 2], [Pont], [Refrain final] si pertinent.
            - Le refrain doit être mémorisable, plus court et plus accrocheur que les couplets.
            - Les couplets doivent transmettre le contenu pédagogique essentiel.
            - Si le savoir est court, réduis la longueur sans perdre la structure de chanson.

            RÈGLES ABSOLUES :
            - Le texte doit rester en français.
            - Le texte doit rester lié au savoir fourni, sans hors-sujet.
            - Pas de narration délirante ni de mythe gratuit.
            - Pas de simple prose paragraphique.
            - Pas de texte plat copié du résumé.
            - Les définitions, étapes, formules, dates ou oppositions importantes doivent être intégrées naturellement dans les couplets.
            - Le refrain doit aider la mémorisation.
            - Si le niveau de l'élève est jeune, simplifie sans infantiliser.
            - Si le niveau est plus élevé, augmente légèrement la précision dans les couplets.

            CONTRAT DU JSON :
            - text = paroles COMPLÈTES de la chanson structurée
            - mnemo = formule ultra courte issue du refrain ou du hook principal
            - tone = style musical directement collable dans Suno, précis, compact et utile
            - godName = nom cohérent du dieu
            - matiere = matière réelle
            - suggestedAction = action concrète du type "copie le style dans Suno puis colle les paroles"

            CONTRAINTE SUR LE STYLE MUSICAL (champ tone) :
            - Maximum 180 caractères.
            - Écris un style directement utilisable dans Suno.
            - Exemple de forme attendue : "pop épique scolaire, voix claire, refrain mémorable, tempo modéré, instrumentation lumineuse".
            - Ne mets pas de phrase d'explication longue.

            TEXTE SOURCE DU SAVOIR :
            $courseText
        """.trimIndent()
    }

    /**
     * Prompt pour un cours déjà en texte.
     */
    private fun buildStructuredPromptFromText(
        texte: String,
        age: Int,
        classe: String,
        matiere: String,
        divinite: String,
        ethos: String,
        mood: String,
        adaptiveContextNote: String? = null
    ): String {
        val adaptationPedagogique = buildAdaptationPedagogique(
            age = age,
            classe = classe,
            mood = mood,
            matiere = matiere
        )

        val adaptiveBlock = buildAdaptiveContextSection(adaptiveContextNote)

        return """
            Tu es $divinite ($ethos). Ton domaine est $matiere.
            L'élève a $age ans et est en classe de $classe.
            Son humeur actuelle est : $mood.

            RÈGLE MAJEURE :
            Tu peux conserver une posture divine, noble et motivante dans ton intention pédagogique,
            MAIS le contenu du résumé doit rester scolaire, concret, fidèle et directement utile pour réviser.
            Tu ne dois jamais transformer le cours en conte, fable, mythe, histoire fantastique ou narration épique.
            Tu n'inventes aucun élément décoratif qui remplace le savoir réel.
            Le résumé doit être en français clair.

            ADAPTATION PÉDAGOGIQUE :
            $adaptationPedagogique

            $adaptiveBlock

            RÈGLES SUPPLÉMENTAIRES :
            - RÈGLE 17 : la réponse doit être en français, adaptée à l'élève, à son âge, à sa classe et à son humeur.
            - RÈGLE 18 : si la matière est une langue ou "Langues", le résumé reste en français, mais le quiz peut mélanger français + langue étudiée si cela aide à réviser fidèlement le document.
            - RÈGLE 25 : si le document source contient déjà des questions, exercices, consignes, QCM ou formulations interrogatives, tu dois les intégrer prioritairement au quiz, en les reformulant seulement si nécessaire pour garder un format propre.
            - RÈGLE 26 : équilibre raisonnablement les bonnes réponses entre A, B et C sur l'ensemble du quiz.
            - RÈGLE 27 : évite qu'une même lettre domine abusivement comme bonne réponse.
            - RÈGLE 28 : adapte la difficulté des distracteurs au niveau réel de l'élève si un contexte adaptatif est fourni.

            MISSION :
            Voici un cours des archives.
            1. RÉSUMÉ :
               - résume uniquement les points vraiment importants
               - reste clair, structuré et concret
               - reformule sans trahir le contenu
               - fais un texte exploitable pour apprendre et réviser
               - évite le remplissage inutile
            2. QUIZ :
               - génère un QCM de 10 à 30 questions
               - basé STRICTEMENT sur le texte
               - aucune question hors-sujet
               - aucune culture générale inventée
               - si des questions existent déjà dans le document, réutilise-les en priorité

            RÈGLES DE STYLE DU RÉSUMÉ :
            - français uniquement
            - pas de rôleplay dans le résumé
            - pas de métaphores inutiles
            - pas de ton "légende", "prophétie", "conte"
            - pas de symboles décoratifs inutiles
            - quelques emojis seulement si vraiment utiles à la lisibilité (maximum 1 à 3 sur tout le résumé)
            - si le texte est technique, reste technique
            - si l'élève est fatigué, fais plus court et plus découpé
            - si l'élève est stressé, sois rassurant, net et structuré
            - si l'élève est joyeux, tu peux être légèrement dynamique, mais toujours concret
            - si l'élève est plus âgé ou niveau élevé, monte légèrement en précision et rigueur
            - si le cours contient des définitions, dates, étapes, formules ou notions-clés, elles doivent apparaître clairement
            - ne jamais inventer une information
            - ne jamais ajouter une date absente
            - ne jamais ajouter une formule absente
            - ne jamais ajouter un nom propre absent
            - si une information est illisible ou incertaine, écris exactement : "Information non lisible dans le document."
            - reste strictement dans la demande texte et le support fourni

            CONTRAT DE SORTIE STRICT ET NON NÉGOCIABLE :
            - Aucune introduction avant ---START_RESUME---
            - Aucune conclusion après ---END_QUIZ---
            - Une seule ligne par question de quiz
            - Chaque question doit respecter exactement ce format :
              Q1: [question] A) [réponse A] B) [réponse B] C) [réponse C] REP: [A ou B ou C]

            STRUCTURE STRICTE DE LA RÉPONSE :
            ---START_RESUME---
            TITLE: [titre court, simple, significatif, maximum 8 mots]
            LEVEL: [niveau de formulation adapté à l'élève]
            CHAPTER: [nom du chapitre]
            SUBTITLE: [sous-partie]
            TEXT: [explication claire]
            SUBTITLE: [autre sous-partie si utile]
            TEXT: [explication claire]
            CHAPTER: [autre chapitre si utile]
            ...
            ---END_RESUME---
            ---START_QUIZ---
            Q1: [?] A) [X] B) [Y] C) [Z] REP: [A, B ou C]
            ...
            ---END_QUIZ---

            RÈGLES STRICTES POUR TITLE :
            - maximum 8 mots
            - aucun emoji
            - aucun markdown
            - interdit d'utiliser : "Résumé", "Cours", "Oracle", "RéviZeus"
            - basé uniquement sur le contenu réel du document ou de la demande
            - si impossible à déterminer, écris : "Notions principales"

            TEXTE DU COURS :
            $texte
        """.trimIndent()
    }

    /**
     * Prompt pour un ou plusieurs documents image.
     */
    private fun buildStructuredPromptFromImages(
        age: Int,
        classe: String,
        matiere: String,
        divinite: String,
        ethos: String,
        mood: String,
        adaptiveContextNote: String? = null
    ): String {
        val adaptationPedagogique = buildAdaptationPedagogique(
            age = age,
            classe = classe,
            mood = mood,
            matiere = matiere
        )

        val adaptiveBlock = buildAdaptiveContextSection(adaptiveContextNote)

        return """
            Tu es $divinite ($ethos). Ton domaine est $matiere.
            L'élève a $age ans et est en classe de $classe.
            Son humeur actuelle est : $mood.

            RÈGLE MAJEURE :
            Tu peux garder une posture divine et motivante dans ton intention pédagogique,
            MAIS tu ne dois jamais transformer le contenu du cours en histoire fantastique.
            Le résumé doit rester scolaire, concret, fidèle, précis et utile pour réviser.
            Tu ne racontes pas un mythe. Tu expliques un cours.

            ADAPTATION PÉDAGOGIQUE :
            $adaptationPedagogique

            $adaptiveBlock

            RÈGLES SUPPLÉMENTAIRES :
            - RÈGLE 17 : la réponse doit être en français, adaptée à l'élève, à son âge, à sa classe et à son humeur.
            - RÈGLE 18 : si la matière est une langue ou "Langues", le résumé reste en français, mais le quiz peut mélanger français + langue étudiée si cela aide à réviser fidèlement le document.
            - RÈGLE 25 : si le document scanné contient déjà des questions, exercices, consignes, QCM ou formulations interrogatives, tu dois les intégrer prioritairement au quiz, en les reformulant seulement si nécessaire pour garder un format propre.
            - RÈGLE 26 : équilibre raisonnablement les bonnes réponses entre A, B et C sur l'ensemble du quiz.
            - RÈGLE 27 : évite qu'une même lettre domine abusivement comme bonne réponse.
            - RÈGLE 28 : adapte la difficulté des distracteurs au niveau réel de l'élève si un contexte adaptatif est fourni.

            MISSION :
            Analyse le ou les documents fournis en image.
            Ces images peuvent représenter plusieurs pages d'un même cours.
            Lis-les dans l'ordre et reconstitue un seul cours cohérent.

            1. RÉSUMÉ :
               - transcris les idées importantes
               - reformule si nécessaire pour clarifier
               - reste fidèle au contenu visible
               - produis un résumé structuré, concret et exploitable
               - le résumé doit être en français clair
            2. QUIZ :
               - génère un QCM de 10 à 30 questions
               - les questions doivent porter STRICTEMENT sur ce qui est visible et lisible
               - n'invente aucune information
               - n'ajoute aucune question de culture générale
               - si des questions sont déjà présentes dans le document, réutilise-les en priorité

            RÈGLES DE STYLE DU RÉSUMÉ :
            - français uniquement
            - pas de rôleplay dans le résumé
            - pas de narration mythologique
            - pas de ton "légendaire", "prophétique", "conte"
            - pas de symboles décoratifs inutiles
            - quelques emojis seulement si vraiment utiles à la lisibilité (maximum 1 à 3 sur tout le résumé)
            - si le cours est scientifique, historique, grammatical ou technique, garde ce registre
            - si l'élève est fatigué, sois plus simple, plus court et mieux découpé
            - si l'élève est stressé, sois rassurant, propre et très structuré
            - si l'élève est joyeux, reste énergique mais sobre
            - si l'élève est plus âgé ou niveau élevé, sois plus rigoureux
            - fais apparaître clairement :
              définitions, étapes, causes/conséquences, dates, notions-clés, formules ou vocabulaire important
            - ne jamais inventer une information
            - ne jamais ajouter une date absente
            - ne jamais ajouter une formule absente
            - ne jamais ajouter un nom propre absent
            - si une information est illisible ou incertaine, écris exactement : "Information non lisible dans le document."

            CONTRAT DE SORTIE STRICT ET NON NÉGOCIABLE :
            - Aucune introduction avant ---START_RESUME---
            - Aucune conclusion après ---END_QUIZ---
            - Une seule ligne par question de quiz
            - Chaque question doit respecter exactement ce format :
              Q1: [question] A) [réponse A] B) [réponse B] C) [réponse C] REP: [A ou B ou C]

            STRUCTURE STRICTE :
            ---START_RESUME---
            TITLE: [titre court, simple, significatif, maximum 8 mots]
            LEVEL: [niveau de formulation adapté à l'élève]
            CHAPTER: [nom du chapitre]
            SUBTITLE: [sous-partie]
            TEXT: [explication claire]
            SUBTITLE: [autre sous-partie si utile]
            TEXT: [explication claire]
            CHAPTER: [autre chapitre si utile]
            ...
            ---END_RESUME---
            ---START_QUIZ---
            Q1: [?] A) [X] B) [Y] C) [Z] REP: [A, B ou C]
            ...
            ---END_QUIZ---

            RÈGLES STRICTES POUR TITLE :
            - maximum 8 mots
            - aucun emoji
            - aucun markdown
            - interdit d'utiliser : "Résumé", "Cours", "Oracle", "RéviZeus"
            - basé uniquement sur le contenu réel du document scanné
            - si impossible à déterminer, écris : "Notions principales"
        """.trimIndent()
    }

    /**
     * Bloc central d'adaptation selon âge / classe / humeur.
     * Il permet à l'IA de moduler la densité et le ton,
     * sans tomber dans la fantaisie.
     */
    private fun buildAdaptationPedagogique(
        age: Int,
        classe: String,
        mood: String,
        matiere: String
    ): String {
        val trancheAge = when {
            age <= 10 -> """
                - Utilise des phrases très simples.
                - Explique avec des mots faciles.
                - Mets en avant les idées principales uniquement.
                - Découpe clairement les informations.
            """.trimIndent()

            age in 11..13 -> """
                - Utilise un ton simple, vivant et clair.
                - Tu peux rendre l'explication engageante, mais jamais romancée.
                - Va à l'essentiel, puis ajoute les notions utiles pour comprendre.
                - Aide à mémoriser sans complexifier inutilement.
            """.trimIndent()

            age in 14..17 -> """
                - Utilise un ton scolaire clair et structuré.
                - Garde les termes importants du cours.
                - Va plus loin dans les liens logiques si nécessaire.
                - Le résumé doit pouvoir servir de fiche de révision.
            """.trimIndent()

            else -> """
                - Utilise un ton direct, précis et académique.
                - Conserve les notions techniques importantes.
                - Évite toute simplification excessive.
                - Le résumé doit être utile pour réviser efficacement sans folklore.
            """.trimIndent()
        }

        val adaptationClasse = when {
            classe.contains("CP", ignoreCase = true) ||
                classe.contains("CE1", ignoreCase = true) ||
                classe.contains("CE2", ignoreCase = true) ||
                classe.contains("CM1", ignoreCase = true) ||
                classe.contains("CM2", ignoreCase = true) -> """
                - Niveau primaire : privilégie les notions essentielles.
                - Évite les longues phrases.
                - Préfère les formulations très accessibles.
            """.trimIndent()

            classe.contains("6", ignoreCase = true) ||
                classe.contains("5", ignoreCase = true) ||
                classe.contains("4", ignoreCase = true) ||
                classe.contains("3", ignoreCase = true) -> """
                - Niveau collège : reste clair, pédagogique et progressif.
                - Définis implicitement les notions si elles sont nouvelles.
                - Mets en évidence ce qu'il faut retenir pour un contrôle.
            """.trimIndent()

            classe.contains("2nde", ignoreCase = true) ||
                classe.contains("seconde", ignoreCase = true) ||
                classe.contains("1ère", ignoreCase = true) ||
                classe.contains("première", ignoreCase = true) ||
                classe.contains("terminale", ignoreCase = true) -> """
                - Niveau lycée : sois plus précis et plus structuré.
                - Fais ressortir les mécanismes, les liens et les notions centrales.
                - Le résumé doit aider à préparer une évaluation sérieuse.
            """.trimIndent()

            else -> """
                - Adapte-toi au niveau indiqué par la classe.
                - Si le niveau semble avancé, augmente la précision.
                - Si le niveau semble jeune, privilégie la clarté.
            """.trimIndent()
        }

        val adaptationMood = when (mood.uppercase()) {
            "JOYEUX" -> """
                - Ton : positif, dynamique et motivant.
                - Tu peux être légèrement entraînant.
                - Mais reste toujours concret et pédagogique.
            """.trimIndent()

            "FATIGUE", "FATIGUÉ" -> """
                - Ton : calme, très clair, peu chargé.
                - Phrases plus courtes.
                - Structure le contenu pour réduire l'effort mental.
                - Mets fortement en avant les points à retenir.
            """.trimIndent()

            "STRESSE", "STRESSÉ" -> """
                - Ton : rassurant, apaisant, méthodique.
                - Ne surcharge pas.
                - Organise les idées dans un ordre logique.
                - Donne un sentiment de contrôle et de clarté.
            """.trimIndent()

            "COLERE", "COLÈRE" -> """
                - Ton : ferme mais utile, cadré, sans agressivité.
                - Va droit au but.
                - Pas de surcharge émotionnelle.
                - Donne des repères clairs et concrets.
            """.trimIndent()

            else -> """
                - Ton : clair, sobre et utile.
                - Priorité à la compréhension et à la révision.
            """.trimIndent()
        }

        val adaptationMatiere = when {
            matiere.contains("Math", ignoreCase = true) -> """
                - En mathématiques, mets en avant règles, méthodes, étapes et résultats.
                - Si une formule existe, fais-la ressortir clairement.
            """.trimIndent()

            matiere.contains("Français", ignoreCase = true) -> """
                - En français, fais ressortir notions de langue, procédés, définitions et exemples utiles.
            """.trimIndent()

            matiere.contains("Histoire", ignoreCase = true) ||
                matiere.contains("Géographie", ignoreCase = true) -> """
                - Mets en avant dates, lieux, causes, conséquences et repères essentiels.
            """.trimIndent()

            matiere.contains("SVT", ignoreCase = true) ||
                matiere.contains("Physique", ignoreCase = true) ||
                matiere.contains("Chimie", ignoreCase = true) -> """
                - Mets en avant les mécanismes, définitions, phénomènes, étapes et vocabulaire scientifique.
            """.trimIndent()

            matiere.contains("Anglais", ignoreCase = true) ||
                matiere.contains("Langue", ignoreCase = true) -> """
                - Le résumé doit rester en français clair.
                - Fais ressortir les points de langue, vocabulaire ou structures importantes.
                - Le quiz peut mélanger le français et la langue étudiée si le document s'y prête.
            """.trimIndent()

            else -> """
                - Mets en avant les notions-clés et les informations utiles pour réviser.
            """.trimIndent()
        }

        return """
            ADAPTATION PAR ÂGE :
            $trancheAge

            ADAPTATION PAR CLASSE :
            $adaptationClasse

            ADAPTATION PAR HUMEUR :
            $adaptationMood

            ADAPTATION PAR MATIÈRE :
            $adaptationMatiere
        """.trimIndent()
    }

    /**
     * Bloc adaptatif injecté tel quel si le resolver l'a préparé.
     * On le garde volontairement textuel pour éviter toute casse
     * sur le reste de l'architecture.
     */
    private fun buildAdaptiveContextSection(adaptiveContextNote: String?): String {
        if (adaptiveContextNote.isNullOrBlank()) return ""

        return """
            CONTEXTE ADAPTATIF AVANCÉ :
            $adaptiveContextNote

            CONSIGNES D'EXPLOITATION DE CE CONTEXTE :
            - ajuste la densité du résumé selon la charge cognitive indiquée
            - ajuste la difficulté du quiz selon la maîtrise estimée
            - si des faiblesses récentes sont données, priorise-les légèrement dans le quiz
            - si des patterns d'erreurs sont donnés, évite de produire des distracteurs absurdes
            - garde toujours un résultat fidèle au support réel
        """.trimIndent()
    }

    /**
     * CHANTIER 1 - FONDATION IA
     * System instruction dédiée au moteur Oracle existant.
     * On garde ici le contrat résumé + quiz pour ne rien casser.
     */
    private fun buildOracleSystemInstruction(): String {
        return """
            Tu es le moteur pédagogique central de RéviZeus.
            Tu aides à produire des résumés et des quiz de révision à partir de textes ou d'images.

            RÈGLES GLOBALES :
            1. Reste fidèle au contenu fourni.
            2. N'invente aucune information absente du support.
            3. Aucune narration fantastique ou mythologique dans le contenu pédagogique.
            4. Le dieu sert de posture d'accompagnement, jamais de remplacement du savoir réel.
            5. Le résumé doit être clair, structuré, concret et directement utile pour réviser.
            6. Le quiz doit porter strictement sur le contenu du support.
            7. Pas de culture générale ajoutée.
            8. Si le support contient définitions, dates, étapes, causes, conséquences, formules ou vocabulaire, ils doivent être repris.
            9. La structure demandée par le prompt utilisateur est obligatoire.
            10. Aucune phrase avant le marqueur de début demandé.
            11. Aucune phrase après le marqueur de fin demandé.
            12. Une seule ligne par question de quiz.
            13. Pas de markdown additionnel.
            14. Pas de commentaire méta.
            15. Si un point est illisible ou ambigu, ne l'invente pas.
            16. Tu privilégies la fidélité à l'exhaustivité.
            17. Réponse en français selon l'élève : âge, classe, humeur, densité adaptée.
            18. Pour "Langues", résumé en français mais quiz pouvant être mixé avec la langue étudiée si le support le justifie.
            19. Tu gardes un ton scolaire, concret et utile.
            20. Tu peux reformuler pour clarifier sans trahir.
            21. Ne transforme jamais le contenu en histoire.
            22. Aucune sortie JSON pour le moteur Oracle.
            23. Tu suis exactement le format de sortie demandé dans le prompt.
            24. Ne supprime pas d'informations essentielles du document.
            25. Si des questions sont déjà présentes dans le document scanné ou dans le texte fourni, intègre-les prioritairement au quiz.
            26. Répartis raisonnablement les bonnes réponses entre A, B et C.
            27. Évite les séries monotones de même lettre correcte.
        """.trimIndent()
    }

    /**
     * CHANTIER 1 - FONDATION IA
     * System instruction dédiée aux dialogues divins.
     * Elle impose un JSON strict pour l'orchestration IA.
     */
    private fun buildDialogSystemInstruction(
        matiere: String
    ): String {
        return """
            Tu es l'IA centrale de RéviZeus.
            Tu incarnes les dieux du Panthéon comme guides pédagogiques.
            La matière courante est : $matiere.

            FORMAT DE SORTIE STRICT ET OBLIGATOIRE :
            Tu dois répondre avec UN SEUL objet JSON valide.
            Aucun texte avant.
            Aucun texte après.
            Aucune balise markdown.
            Aucun bloc ```json.
            Aucune liste.
            Aucune explication méta.

            SCHÉMA JSON OBLIGATOIRE :
            {
              "text": "string",
              "mnemo": "string",
              "tone": "string",
              "godName": "string",
              "matiere": "string",
              "suggestedAction": "string"
            }

            RÈGLES :
            1. Le champ "text" contient la réponse principale du dieu.
            2. Le champ "mnemo" contient une astuce courte de mémorisation.
            3. Le champ "tone" décrit brièvement la tonalité pédagogique.
            4. Le champ "godName" contient le nom du dieu utilisé.
            5. Le champ "matiere" contient la matière cible.
            6. Le champ "suggestedAction" propose la prochaine action concrète pour le joueur.
            7. "text" doit être immersif, utile, clair et exploitable en UI.
            8. "mnemo" doit être court, mémorisable et scolaire.
            9. Pas de contenu vide.
            10. Pas de narration délirante.
            11. Pas d'information contradictoire avec le prompt reçu.
            12. Le dieu reste un mentor, pas un personnage qui raconte une épopée hors sujet.
            13. Le message doit être cohérent avec l'objectif demandé.
            14. Tu peux être noble, motivant, cadrant ou bienveillant selon le contexte.
            15. La réponse doit rester utilisable par une application mobile éducative.
            16. Le JSON doit être parseable tel quel.
            17. Réponse en français selon l'élève : âge, classe, humeur, niveau de maîtrise, progression.
            18. Pour "Langues", le résumé ou conseil principal reste en français, mais l'astuce mnémotechnique peut intégrer ponctuellement la langue étudiée si pertinent.
            19. Si le contexte mentionne un score ou une progression, adapte la réponse à cette performance.
            20. Si le contexte demande de l'accueil, motive.
            21. Si le contexte demande un verdict, juge avec justesse mais sans casser l'élève.
            22. Si le contexte demande une épreuve ultime, rends le ton plus solennel.
            23. Aucune sortie hors JSON.
            24. N'invente pas de données absentes du prompt.
            25. Si le contexte provient d'un document scanné contenant déjà des questions ou formulations d'exercice, intègre cette réalité dans ton conseil ou l'action suggérée.
        """.trimIndent()
    }

    /**
     * Transport unique backend pour texte + dialogues + images.
     */
    private suspend fun invokeOracleTransport(
        systemInstructionText: String,
        prompt: String,
        images: List<AiInvocationGateway.InlineImage> = emptyList()
    ): String? {
        val gateway = if (images.isEmpty()) {
            oracleTextTransport
        } else {
            oracleVisionTransport
        }

        val responseText = executeWithRetry {
            gateway.invoke(
                systemInstruction = systemInstructionText,
                prompt = prompt,
                model = MODEL_NAME,
                images = images
            )
        }
        return normalizeAiResponse(responseText)
    }

    private fun bitmapToInlineImage(bitmap: Bitmap): AiInvocationGateway.InlineImage {
        val output = ByteArrayOutputStream()
        val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        if (!compressed) {
            throw IllegalStateException("Compression image impossible")
        }

        val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        return AiInvocationGateway.InlineImage(
            mimeType = "image/jpeg",
            dataBase64 = base64
        )
    }

    private suspend fun executeWithRetry(block: suspend () -> String?): String? {
        var lastError: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                val response = block()
                if (!response.isNullOrBlank()) {
                    return response
                }
                Log.e(TAG, "GeminiManager : réponse vide à la tentative ${attempt + 1}.")
            } catch (e: CancellationException) {
                // Contrat Bloc A : ne jamais transformer une annulation coroutine en faux échec réseau.
                Log.d(TAG, "GeminiManager : tentative ${attempt + 1} annulée proprement.")
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.e(TAG, "GeminiManager : tentative ${attempt + 1} échouée : ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    if (isResourceExhaustedError(e)) {
                        delay(RESOURCE_EXHAUSTED_BACKOFF_MS * (attempt + 1))
                    } else {
                        delay(800L)
                    }
                }
            }
        }

        if (lastError != null) {
            throw lastError as Exception
        }

        return null
    }

    private fun isResourceExhaustedError(error: Exception): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is FirebaseFunctionsException &&
                current.code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED
            ) {
                return true
            }
            val message = current.message.orEmpty()
            if (message.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                message.contains("Resource exhausted", ignoreCase = true) ||
                message.contains("429", ignoreCase = true) ||
                message.contains("Too Many Requests", ignoreCase = true)
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    /**
     * CHANTIER 1 - FONDATION IA
     * Parse sécurisé du JSON renvoyé par Gemini pour les dialogues.
     */
    private fun parseGodResponse(
        raw: String?,
        matiere: String
    ): GodResponse? {
        val normalized = normalizeJsonResponse(raw) ?: return null

        return try {
            val jsonObject = JSONObject(normalized)

            val text = jsonObject.optString("text").trim()
            val mnemo = jsonObject.optString("mnemo").trim()
            val tone = jsonObject.optString("tone").trim()
            val godName = jsonObject.optString("godName").trim()
            val matiereFromJson = jsonObject.optString("matiere").trim()
            val suggestedAction = jsonObject.optString("suggestedAction").trim()

            if (
                text.isBlank() ||
                mnemo.isBlank() ||
                tone.isBlank() ||
                godName.isBlank() ||
                suggestedAction.isBlank()
            ) {
                return null
            }

            GodResponse(
                text = text,
                mnemo = mnemo,
                tone = tone,
                godName = godName,
                matiere = if (matiereFromJson.isBlank()) matiere else matiereFromJson,
                suggestedAction = suggestedAction
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse GodResponse impossible : ${e.message}")
            null
        }
    }

    /**
     * CHANTIER 1 - FONDATION IA
     * Nettoyage spécifique des réponses JSON éventuelles.
     */
    private fun normalizeJsonResponse(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

        return raw
            .replace("\u00A0", " ")
            .replace("\r", "")
            .replace("```json", "", ignoreCase = true)
            .replace("```", "")
            .trim()
    }

    private fun normalizeAiResponse(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

        // CONSOLIDATION :
        // Nettoyage léger sans altérer la structure que le parseur attend.
        return raw
            .replace("\u00A0", " ")
            .replace("\r", "")
            .trim()
    }
}

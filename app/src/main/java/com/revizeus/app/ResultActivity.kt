package com.revizeus.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.databinding.ActivityResultBinding
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.CourseEntry
import com.revizeus.app.models.IAristoteEngine
import com.revizeus.app.models.QuizQuestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

/**
 * ═══════════════════════════════════════════════════════════════
 * ÉCRAN DE RÉSULTAT ET INVOCATION
 * ═══════════════════════════════════════════════════════════════
 * VERSION CORRIGÉE OVERLAY :
 * ✅ BGM conservé pendant le scan
 * ✅ canal audio secondaire pour le scan
 * ✅ vrai overlay Oracle au-dessus de tout l'écran
 * ✅ correction du bug de musique perdue au retour Dashboard
 * ✅ invocation Oracle principale branchée sur le noyau B2 (DivineRequestContext + plan)
 * ✅ panneau RPG intact
 *
 * AJOUTS v10 :
 * ✅ Détection automatique de la matière depuis le contenu analysé
 * ✅ Génération automatique du titre de cours
 * ✅ Dialogue de choix post-analyse incarné par le dieu de la matière
 * ✅ Deux choix : enregistrer seulement / enregistrer + quiz
 *
 * PHILOSOPHIE QUIZ v10 (IMPORTANTE) :
 * Les questions générées lors du scan Oracle ne sont PAS persistées.
 * Chaque lancement en mode Entraînement rappelle Gemini avec le texte
 * du cours pour produire un QCM différent à chaque session.
 * Seul le texte extrait (extractedText dans Room) est conservé.
 *
 * CORRECTIF ORACLE PROMPT :
 * ✅ Support du flux FREE_TEXT_INPUT
 * ✅ Le texte libre n'exige plus d'image
 * ✅ Le pipeline Gemini est choisi dynamiquement :
 *    - images -> genererContenuDepuisImages(..., divineRequestContext)
 *    - texte libre -> genererContenuOracle(..., divineRequestContext)
 * ═══════════════════════════════════════════════════════════════
 */
class ResultActivity : BaseActivity() {

    private enum class SummaryBlockType { CHAPTER, SUBTITLE, TEXT }

    private data class SummaryDisplayBlock(
        val type: SummaryBlockType,
        val content: String
    )

    private data class ParsedOracleSummary(
        val title: String,
        val level: String,
        val blocks: List<SummaryDisplayBlock>,
        val plainText: String
    )

    private lateinit var binding: ActivityResultBinding
    private var quizGenere: List<QuizQuestion> = emptyList()
    private var generatedSummary: String = ""
    private var ttsSafeSummary: String = ""

    private var scanAnimator: ObjectAnimator? = null
    private var scanPulseAnimator: ObjectAnimator? = null
    private var scanFlashAnimator: ObjectAnimator? = null
    private var isScanning = false

    private var imageUris: List<Uri> = emptyList()
    private var imageBitmaps: List<Bitmap> = emptyList()

    private val godSpeechAnimator = GodSpeechAnimator()
    private var godTypewriterJob: Job? = null

    // ── AJOUT v11 — TTS résultat / panneau de sauvegarde ───────────────
    private val tts: SpeakerTtsHelper by lazy { SpeakerTtsHelper(this) }

    private var selectedMatiereForSave: String? = null
    private var selectedFolderForSave: String = ""

    /** AJOUT v10 — Matière auto-détectée depuis le contenu Gemini. */
    private var matiereAutoDetectee: String = "Mathématiques"

    /** AJOUT v10 — Titre auto-généré depuis le contenu Gemini. */
    private var titreAutoGenere: String = ""

    /** CORRECTIF ORACLE PROMPT — texte libre envoyé par OraclePromptActivity. */
    private var freeTextInput: String = ""

    private var normalBgmVolume: Float = 0.8f
    private var scanBgmVolume: Float = 0.32f
    private var scanLoopVolume: Float = 0.22f

    private var initialBgmJob: Job? = null
    private var invokeAnalysisJob: Job? = null

    // Fond premium Résumé : même mécanique partout, sans casser le flux Oracle.
    private var animatedBackgroundHelper: AnimatedBackgroundHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        installerFondPremiumResult()
        initialiserPanneauRpgSauvegarde()
        setupResultSpeakerButtons()

        val imageUriString = intent.getStringExtra("IMAGE_URI")
        val imageUriStrings = intent.getStringArrayListExtra("IMAGE_URIS")
        freeTextInput = intent.getStringExtra("FREE_TEXT_INPUT")?.trim().orEmpty()

        imageUris = when {
            !imageUriStrings.isNullOrEmpty() -> imageUriStrings.map { Uri.parse(it) }
            !imageUriString.isNullOrBlank() -> listOf(Uri.parse(imageUriString))
            else -> emptyList()
        }

        val isFreeTextFlow = freeTextInput.isNotBlank()

        if (!isFreeTextFlow && imageUris.isEmpty()) {
            binding.tvResult.text = "Le support de l'Oracle est introuvable."
            binding.btnInvoke.visibility = View.GONE
            binding.btnBack.visibility = View.VISIBLE
            return
        }

        if (isFreeTextFlow) {
            binding.ivImagePreview.visibility = View.GONE
            binding.tvResult.text =
                "L'Oracle a reçu ta demande. Invoque maintenant les dieux pour forger un résumé clair et un quiz potentiel à partir de ton texte."
        } else {
            imageBitmaps = imageUris.mapNotNull { uri -> loadAndCompressBitmap(uri) }

            if (imageBitmaps.isEmpty()) {
                afficherErreurDivine("Impossible de lire les images. Les parchemins sont corrompus.")
                return
            }

            binding.ivImagePreview.visibility = View.VISIBLE
            binding.ivImagePreview.setImageBitmap(imageBitmaps.first())

            binding.tvResult.text = if (imageBitmaps.size > 1) {
                "L'Artefact a capturé ${imageBitmaps.size} pages. Vérifie l'aperçu de la première page ci-dessus."
            } else {
                "L'Artefact a capturé ton cours. Vérifie l'aperçu ci-dessus. Si c'est flou, reviens en arrière."
            }
        }

        val settingsManager = SettingsManager(this)
        normalBgmVolume = (settingsManager.volumeMusique / 100f).coerceIn(0f, 1f)
        scanBgmVolume = (normalBgmVolume * 0.40f).coerceIn(0f, 1f)
        scanLoopVolume = (settingsManager.volumeSfx / 100f * 0.35f).coerceIn(0f, 1f)

        try {
            SoundManager.rememberMusic(R.raw.bgm_offrande)
        } catch (_: Exception) {
        }

        initialBgmJob?.cancel()
        initialBgmJob = lifecycleScope.launch {
            delay(400)
            if (!isScanning && !isFinishing && !isDestroyed) {
                restaurerBgmOffrandeSiNecessaire()
            }
        }

        val prefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        val age = prefs.getInt("USER_AGE", 15)
        val classe = prefs.getString("USER_CLASS", "Terminale") ?: "3ème"
        val mood = prefs.getString("CURRENT_MOOD", "Prêt") ?: "Neutre"

        binding.btnInvoke.setOnClickListener {
            if (invokeAnalysisJob?.isActive == true) {
                return@setOnClickListener
            }

            binding.btnInvoke.isEnabled = false
            binding.btnInvoke.visibility = View.GONE
            binding.layoutActions.visibility = View.GONE
            binding.layoutLoading.visibility = View.VISIBLE
            binding.tvResult.visibility = View.GONE

            startScanAnimation()

            binding.tvStatus.text = when {
                freeTextInput.isNotBlank() ->
                    "⚡ Les Oracles forgent un savoir à partir de ta demande sacrée..."
                imageBitmaps.size > 1 ->
                    "⚡ Les Oracles analysent tes ${imageBitmaps.size} pages sacrées..."
                else ->
                    "⚡ Les Oracles analysent ton parchemin sacré..."
            }

            invokeAnalysisJob?.cancel()
            invokeAnalysisJob = lifecycleScope.launch {
                try {
                    val oracleDivineContext = buildOracleMainInvokeDivineContext(
                        isFreeTextFlow = freeTextInput.isNotBlank(),
                        userAge = age,
                        userClassLevel = classe,
                        currentMood = mood
                    )
                    val raw = if (freeTextInput.isNotBlank()) {
                        GeminiManager.genererContenuOracle(
                            texte = freeTextInput,
                            age = age,
                            classe = classe,
                            matiere = "Général",
                            divinite = "Zeus",
                            ethos = "Souveraineté",
                            mood = mood,
                            divineRequestContext = oracleDivineContext
                        )
                    } else {
                        GeminiManager.genererContenuDepuisImages(
                            imageBitmaps = imageBitmaps,
                            age = age,
                            classe = classe,
                            matiere = "Général",
                            divinite = "Zeus",
                            ethos = "Souveraineté",
                            mood = mood,
                            divineRequestContext = oracleDivineContext
                        )
                    }

                    if (!isActive || isFinishing || isDestroyed) {
                        return@launch
                    }

                    if (raw != null) {
                        val data = IAristoteEngine.decoderReponse(raw)

                        if (data != null) {
                            stopScanAnimation()
                            restaurerBgmOffrandeSiNecessaire()

                            binding.layoutLoading.visibility = View.GONE
                            binding.layoutActions.visibility = View.VISIBLE
                            binding.tvResult.visibility = View.VISIBLE

                            generatedSummary = data.first
                            val parsedSummary = parseOracleSummaryForDisplay(generatedSummary)
                            renderPremiumSummary(parsedSummary)
                            val fallbackSafeSummary = formatSummarySafe(generatedSummary).second
                            ttsSafeSummary = parsedSummary.plainText.ifBlank { fallbackSafeSummary }
                            quizGenere = data.second

                            // AJOUT v10 — Détection matière + titre auto
                            matiereAutoDetectee = detecterMatiereDepuisContenu(generatedSummary)
                            titreAutoGenere = genererTitreCoursSmart(
                                matiere = matiereAutoDetectee,
                                parsedSummary = parsedSummary
                            )
                            updateResultSummaryCard(matiereAutoDetectee)

                            // Le héros doit d'abord valider le résumé,
                            // puis seulement choisir le temple de destination.
                            binding.btnStartQuiz.text = "✅ VALIDER LE RÉSUMÉ"
                            binding.btnStartQuiz.visibility = View.VISIBLE

                            jouerSfx(R.raw.sfx_notif_divine)
                        } else {
                            stopScanAnimation()
                            restaurerBgmOffrandeSiNecessaire()
                            afficherErreurDivine(
                                if (freeTextInput.isNotBlank()) {
                                    "Le langage de l'Olympe est confus. Ta demande doit être reformulée plus clairement."
                                } else {
                                    "Le langage de l'Olympe est confus. Les pages étaient peut-être trop complexes ou mal ordonnées."
                                }
                            )
                        }
                    } else {
                        stopScanAnimation()
                        restaurerBgmOffrandeSiNecessaire()
                        afficherErreurDivine("L'Olympe est plongé dans le silence (Erreur Réseau ou Timeout).")
                    }
                } catch (e: CancellationException) {
                    Log.d("REVIZEUS_RESULT", "invokeAnalysisJob annulé proprement")
                    throw e
                } finally {
                    if (!isFinishing && !isDestroyed) {
                        binding.btnInvoke.isEnabled = true
                    }
                    invokeAnalysisJob = null
                }
            }
        }

        // Le résumé doit être validé manuellement avant la proposition du temple.
        binding.btnStartQuiz.setOnClickListener {
            if (generatedSummary.isNotBlank()) {
                ouvrirEtapeChoixMatiereRpg()
            }
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    // ═══════════════════════════════════════════════════════════════
    // AJOUT v10 — DÉTECTION AUTOMATIQUE DE MATIÈRE
    // ═══════════════════════════════════════════════════════════════

    /**
     * AJOUT v10 — Détecte la matière la plus probable depuis le contenu
     * via scoring par mots-clés. Retourne "Mathématiques" par défaut.
     */

    /**
     * PATCH SAFE RÉSUMÉ / TTS
     * - préserve le flow Oracle existant
     * - garde tvResult comme source de vérité visuelle
     * - nettoie le texte lu pour limiter les bugs TTS
     */
    private fun formatSummarySafe(raw: String): Pair<String, String> {
        val visual = raw.trim()

        val tts = raw
            .replace("•", "")
            .replace("\n- ", ". ")
            .replace("\n* ", ". ")
            .replace("\n\n", ". ")
            .replace("\n", ". ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return Pair(visual, tts)
    }

    private fun parseOracleSummaryForDisplay(raw: String): ParsedOracleSummary {
        return try {
            val cleanedRaw = raw
                .replace("\r", "")
                .replace(Regex("\\u0000"), "")
                .trim()

            val lines = cleanedRaw.lines().map { it.trim() }
            val blocks = mutableListOf<SummaryDisplayBlock>()
            var title = ""
            var level = ""

            lines.forEach { line ->
                if (line.isBlank()) return@forEach

                when {
                    line.startsWith("TITLE:", ignoreCase = true) -> {
                        title = line.substringAfter(":").trim()
                    }

                    line.startsWith("LEVEL:", ignoreCase = true) -> {
                        level = line.substringAfter(":").trim()
                    }

                    line.startsWith("CHAPTER:", ignoreCase = true) -> {
                        val value = line.substringAfter(":").trim()
                        if (value.isNotBlank()) {
                            blocks.add(SummaryDisplayBlock(SummaryBlockType.CHAPTER, value))
                        }
                    }

                    line.startsWith("SUBTITLE:", ignoreCase = true) -> {
                        val value = line.substringAfter(":").trim()
                        if (value.isNotBlank()) {
                            blocks.add(SummaryDisplayBlock(SummaryBlockType.SUBTITLE, value))
                        }
                    }

                    line.startsWith("TEXT:", ignoreCase = true) -> {
                        val value = line.substringAfter(":").trim()
                        if (value.isNotBlank()) {
                            blocks.add(SummaryDisplayBlock(SummaryBlockType.TEXT, value))
                        }
                    }

                    line.startsWith("##") || line.startsWith("#") -> {
                        val value = line.replace(Regex("^#+\\s*"), "").trim()
                        if (value.isNotBlank()) {
                            blocks.add(SummaryDisplayBlock(SummaryBlockType.CHAPTER, value))
                        }
                    }

                    line.startsWith("**") && line.endsWith("**") -> {
                        val value = line.removePrefix("**").removeSuffix("**").trim()
                        if (value.isNotBlank()) {
                            blocks.add(SummaryDisplayBlock(SummaryBlockType.SUBTITLE, value))
                        }
                    }

                    else -> {
                        val value = line
                            .replace(Regex("^[-*]\\s*"), "")
                            .trim()
                        if (value.isNotBlank()) {
                            blocks.add(SummaryDisplayBlock(SummaryBlockType.TEXT, value))
                        }
                    }
                }
            }

            val hasStructuredFormat = lines.any { line ->
                line.startsWith("TITLE:", ignoreCase = true) ||
                    line.startsWith("LEVEL:", ignoreCase = true) ||
                    line.startsWith("CHAPTER:", ignoreCase = true) ||
                    line.startsWith("SUBTITLE:", ignoreCase = true) ||
                    line.startsWith("TEXT:", ignoreCase = true)
            }

            val cleanedFallbackText = cleanedRaw
                .replace(Regex("^---START_RESUME---\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*---END_RESUME---$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s+"), " ")
                .trim()

            val finalBlocks = if (blocks.isNotEmpty()) {
                blocks
            } else {
                listOf(
                    SummaryDisplayBlock(
                        type = SummaryBlockType.TEXT,
                        content = cleanedFallbackText.ifBlank {
                            "Information non lisible dans le document."
                        }
                    )
                )
            }

            val finalTitle = sanitizeAutoCourseTitle(
                if (title.isNotBlank()) title else genererTitreDepuisTexte(cleanedFallbackText)
            )
            val finalLevel = level
                .replace(Regex("^[-*]\\s*"), "")
                .replace(Regex("\\s+"), " ")
                .trim()

            val plainTextFromBlocks = buildString {
                finalBlocks.forEach { block ->
                    append(block.content.trim())
                    if (!block.content.endsWith(".") && !block.content.endsWith("!") && !block.content.endsWith("?")) {
                        append(".")
                    }
                    append(" ")
                }
            }.replace(Regex("\\s+"), " ").trim()

            val plainText = if (plainTextFromBlocks.isNotBlank()) {
                plainTextFromBlocks
            } else {
                formatSummarySafe(cleanedFallbackText).second
            }

            val normalizedBlocks = if (!hasStructuredFormat && finalBlocks.isNotEmpty()) {
                listOf(
                    SummaryDisplayBlock(
                        type = SummaryBlockType.TEXT,
                        content = finalBlocks.joinToString("\n") { it.content }.trim()
                    )
                )
            } else {
                finalBlocks
            }

            ParsedOracleSummary(
                title = finalTitle,
                level = finalLevel,
                blocks = normalizedBlocks,
                plainText = plainText
            )
        } catch (e: Exception) {
            Log.w("REVIZEUS_RESULT", "parseOracleSummaryForDisplay fallback : ${e.message}", e)
            val clean = raw.replace(Regex("\\s+"), " ").trim()
            ParsedOracleSummary(
                title = sanitizeAutoCourseTitle(clean),
                level = "",
                blocks = listOf(
                    SummaryDisplayBlock(
                        type = SummaryBlockType.TEXT,
                        content = clean.ifBlank { "Information non lisible dans le document." }
                    )
                ),
                plainText = formatSummarySafe(clean).second
            )
        }
    }

    private fun renderPremiumSummary(parsed: ParsedOracleSummary) {
        try {
            val container = binding.layoutSummaryContent
            container.removeAllViews()

            if (parsed.blocks.isEmpty()) {
                binding.tvResult.visibility = View.VISIBLE
                binding.tvResult.text = formatSummarySafe(generatedSummary).first
                return
            }

            val titleText = TextView(this).apply {
                text = parsed.title.ifBlank { "Notions principales" }
                setTextColor(Color.parseColor("#FFD700"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
                gravity = Gravity.CENTER
                typeface = try {
                    resources.getFont(R.font.cinzel_bold)
                } catch (_: Exception) {
                    Typeface.DEFAULT_BOLD
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }
            }
            container.addView(titleText)

            if (parsed.level.isNotBlank()) {
                val levelText = TextView(this).apply {
                    text = parsed.level
                    setTextColor(Color.parseColor("#FFF3B0"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    gravity = Gravity.CENTER
                    typeface = try { resources.getFont(R.font.exo2) } catch (_: Exception) { Typeface.DEFAULT }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(10)
                    }
                }
                container.addView(levelText)
            }

            parsed.blocks.forEach { block ->
                val textView = TextView(this).apply {
                    text = block.content
                    setLineSpacing(0f, 1.22f)
                    typeface = when (block.type) {
                        SummaryBlockType.TEXT -> try {
                            resources.getFont(R.font.exo2)
                        } catch (_: Exception) {
                            Typeface.DEFAULT
                        }

                        else -> Typeface.DEFAULT_BOLD
                    }
                }

                when (block.type) {
                    SummaryBlockType.CHAPTER -> {
                        textView.setTextColor(Color.parseColor("#FFD700"))
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                        textView.gravity = Gravity.START
                        textView.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { topMargin = dp(14) }
                    }

                    SummaryBlockType.SUBTITLE -> {
                        textView.setTextColor(Color.parseColor("#FFF3B0"))
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                        textView.gravity = Gravity.START
                        textView.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { topMargin = dp(10) }
                    }

                    SummaryBlockType.TEXT -> {
                        textView.setTextColor(Color.WHITE)
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                        textView.gravity = Gravity.START
                        textView.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { topMargin = dp(6) }
                    }
                }

                container.addView(textView)
            }

            binding.tvResult.visibility = View.GONE
            binding.tvResult.text = parsed.blocks
                .joinToString("\n") { it.content }
                .ifBlank { formatSummarySafe(generatedSummary).first }
        } catch (e: Exception) {
            Log.w("REVIZEUS_RESULT", "renderPremiumSummary fallback : ${e.message}", e)
            val (visualSummary, _) = formatSummarySafe(generatedSummary)
            binding.layoutSummaryContent.removeAllViews()
            binding.tvResult.visibility = View.VISIBLE
            binding.tvResult.text = visualSummary
        }
    }

    private fun genererTitreDepuisTexte(raw: String): String {
        val lignes = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        val premiereCandidate = lignes.firstOrNull { it.length in 5..80 }
            ?: raw.replace(Regex("\\s+"), " ").trim()
        return sanitizeAutoCourseTitle(premiereCandidate)
    }

    private fun sanitizeAutoCourseTitle(raw: String): String {
        val forbiddenWords = setOf("résumé", "cours", "document", "oracle", "révizeus")

        val sanitized = raw
            .replace(Regex("(?i)\\b(TITLE|CHAPTER|SUBTITLE|TEXT|LEVEL)\\s*:"), " ")
            .replace(Regex("[#*_`\\[\\](){}]"), " ")
            .replace(Regex("[\\p{So}\\p{Cn}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val words = sanitized
            .split(" ")
            .filter { it.isNotBlank() }
            .filterNot { token -> forbiddenWords.contains(token.lowercase()) }
            .take(8)

        val candidate = words.joinToString(" ").trim().removeSuffix(".").trim()
        return if (candidate.isBlank()) "Notions principales" else candidate
    }

    /**
     * Noyau B2 — Contexte d'entrée pour l'invocation Oracle principale (FREE_TEXT_INPUT ou parchemins).
     * La planification (persona, speechMode, hints) passe par [DivineResponseOrchestrator] ;
     * l'injection prompt est assurée par [GeminiManager] sans changer le rendu écran.
     */
    private fun buildOracleMainInvokeDivineContext(
        isFreeTextFlow: Boolean,
        userAge: Int,
        userClassLevel: String,
        currentMood: String
    ): DivineRequestContext {
        val meta = mutableMapOf(
            "oracle_flow" to if (isFreeTextFlow) "FREE_TEXT_INPUT" else "IMAGE_PAGES"
        )
        if (!isFreeTextFlow && imageBitmaps.isNotEmpty()) {
            meta["page_count"] = imageBitmaps.size.toString()
        }
        return DivineRequestContext(
            subject = null,
            actionType = DivineActionType.SUMMARY_GENERATION,
            screenSource = "oracle_ResultActivity_main_invoke",
            userAge = userAge,
            userClassLevel = userClassLevel,
            currentMood = currentMood,
            successState = null,
            difficulty = null,
            rawInput = if (isFreeTextFlow) freeTextInput else null,
            validatedSummary = null,
            questionText = null,
            userAnswer = null,
            correctAnswer = null,
            metadata = meta
        )
    }

    private fun updateResultSummaryCard(matiere: String) {
        val god = GodManager.fromMatiere(matiere)
        try {
            binding.tvResultTitle.text = "⚡ Résumé divin"
            binding.tvResultSubtitle.text = titreAutoGenere.ifBlank { matiere }
            binding.viewSummaryAccent.setBackgroundColor(
                Color.parseColor(god?.couleurHex ?: "#FFD700")
            )
            binding.imgResultGod.setImageResource(god?.avatarDialogRes ?: R.drawable.avatar_zeus_dialog)
        } catch (_: Exception) {
        }
    }

    private fun installerFondPremiumResult() {
        animatedBackgroundHelper = AnimatedBackgroundHelper(
            targetView = binding.root,
            backgroundImageView = binding.ivBackgroundResult
        )
        animatedBackgroundHelper?.configurePremiumBackground(
            staticDrawableRes = getDrawableResOrFallback("bg_resumer_animated", "bg_olympus_dark"),
            videoRawRes = getRawResByName("bg_resumer_animated"),
            imageAlpha = 0.22f,
            loopVideo = true,
            videoVolume = 0.50f
        )
    }

    private fun appliquerFondPremiumResultPourMatiere(matiere: String?) {
        val accentColor = PantheonConfig.findByMatiere(matiere ?: "")?.couleur
            ?: Color.parseColor("#FFD700")
        animatedBackgroundHelper?.start(
            accentColor = accentColor,
            mode = OlympianParticlesView.ParticleMode.SAVOIR
        )
    }

    private fun getRawResByName(resName: String): Int {
        return resources.getIdentifier(resName, "raw", packageName)
    }

    private fun getDrawableResOrFallback(primaryName: String, fallbackName: String): Int {
        val primary = resources.getIdentifier(primaryName, "drawable", packageName)
        if (primary != 0) return primary
        val fallback = resources.getIdentifier(fallbackName, "drawable", packageName)
        return if (fallback != 0) fallback else R.drawable.bg_olympus_dark
    }

    private fun detecterMatiereDepuisContenu(contenu: String): String {
        val contenuLower = contenu.lowercase()

        val scores = mapOf(
            "Mathématiques" to listOf(
                "équation", "calcul", "algèbre", "géométrie", "fonction",
                "probabilité", "intégrale", "dérivée", "vecteur", "statistique",
                "théorème", "pgcd", "fraction", "trigonométrie", "polynôme",
                "limite", "matrice", "suite", "arithmétique", "logarithme",
                "exponentielle", "racine carrée", "angle", "périmètre"
            ),
            "Français" to listOf(
                "grammaire", "conjugaison", "syntaxe", "rédaction",
                "subordonnée", "orthographe", "littérature", "narrateur",
                "métaphore", "pronom", "adjectif", "roman", "poème",
                "vers", "strophe", "figure de style", "argumentation",
                "registre", "concordance", "participe", "sujet", "verbe"
            ),
            "SVT" to listOf(
                "cellule", "adn", "chromosome", "évolution", "écosystème",
                "photosynthèse", "gène", "organisme", "biologie", "respiration",
                "digestion", "mitose", "méiose", "protéine", "enzyme",
                "immunité", "nerveux", "hormone", "biotope", "chaîne alimentaire",
                "génétique", "mutation", "allèle", "espèce", "milieu"
            ),
            "Histoire" to listOf(
                "siècle", "révolution", "guerre", "empire", "chronologie",
                "traité", "colonie", "civilisation", "régime", "souverain",
                "constitution", "démocratie", "roi", "bataille", "conquête",
                "indépendance", "armistice", "colonisation", "monarchie",
                "parlement", "crise", "nazisme", "résistance", "libération"
            ),
            "Physique-Chimie" to listOf(
                "atome", "électron", "force", "vitesse", "énergie",
                "réaction", "solution", "acide", "molécule", "électrique",
                "newton", "formule", "loi de", "tension", "résistance",
                "pression", "chaleur", "radioactivité", "oxydation",
                "électrolyse", "cinétique", "noyau", "proton"
            ),
            "Géographie" to listOf(
                "continent", "pays", "relief", "climat", "population",
                "territoire", "aménagement", "carte", "région", "ressource",
                "mondialisation", "développement", "urbanisation", "littorale",
                "immigration", "flux", "paysage", "frontière", "biome",
                "latitude", "longitude", "densité", "métropole"
            ),
            "Art/Musique" to listOf(
                "peinture", "sculpture", "musique", "œuvre", "artiste",
                "baroque", "impressionnisme", "harmonie", "rythme", "mélodie",
                "compositeur", "peintre", "dessin", "couleur", "perspective",
                "symphonie", "opéra", "tempo", "partition", "tonalité"
            ),
            "Anglais" to listOf(
                "english", "grammar", "vocabulary", "tense", "verb",
                "present", "past", "future", "sentence", "pronoun",
                "anglais", "expression", "idiom", "dialogue", "comprehension",
                "preposition", "auxiliary", "conditional", "past perfect"
            ),
            "Philo/SES" to listOf(
                "philosophie", "concept", "raisonnement", "thèse", "antithèse",
                "liberté", "justice", "conscience", "morale", "économie",
                "marché", "social", "politique", "échange", "inégalité",
                "chômage", "croissance", "sociologie", "dialectique",
                "platon", "kant", "aristote", "rousseau"
            ),
            "Vie & Projets" to listOf(
                "projet", "orientation", "carrière", "métier", "formation",
                "compétence", "bilan", "parcours", "professionnel", "entreprise",
                "stage", "bac", "lycée", "avenir", "objectif", "cv"
            )
        )

        var bestMatiere = "Mathématiques"
        var bestScore = 0

        scores.forEach { (matiere, keywords) ->
            val score = keywords.count { kw -> contenuLower.contains(kw) }
            if (score > bestScore) {
                bestScore = score
                bestMatiere = matiere
            }
        }

        return bestMatiere
    }

    /**
     * AJOUT v10 — Génère un titre de cours propre.
     * Priorité : Markdown ## → **Gras** → Ligne courte → Aperçu nettoyé.
     */
    private fun genererTitreCoursSmart(
        matiere: String,
        parsedSummary: ParsedOracleSummary? = null
    ): String {
        val fromParsedTitle = sanitizeAutoCourseTitle(parsedSummary?.title.orEmpty())
        if (fromParsedTitle != "Notions principales") return fromParsedTitle

        val chapter = parsedSummary?.blocks
            ?.firstOrNull { it.type == SummaryBlockType.CHAPTER }
            ?.content
            .orEmpty()
        val fromChapter = sanitizeAutoCourseTitle(chapter)
        if (fromChapter != "Notions principales") return fromChapter

        val fromSummary = sanitizeAutoCourseTitle(generatedSummary)
        if (fromSummary != "Notions principales") return fromSummary

        val matiereFallback = sanitizeAutoCourseTitle(matiere)
        return if (matiereFallback == "Notions principales") {
            "Notions principales"
        } else {
            matiereFallback
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // AJOUT v10 — DIALOGUE DE CHOIX POST-ANALYSE
    // ═══════════════════════════════════════════════════════════════

    /**
     * AJOUT v10 — Affiche le panneau RPG de choix post-analyse.
     *
     * Réutilise layoutGodSaveOverlay sans modifier le XML.
     * Deux options :
     *   1. Enregistrer seulement → texte sauvé, quiz non lancé
     *   2. Enregistrer + quiz immédiat → quiz joué depuis la mémoire live du scan
     *
     * En mode Entraînement (TrainingSelectActivity), le quiz sera régénéré
     * par Gemini à chaque session pour être toujours différent.
     */
    private fun afficherDialogChoixPostAnalyse(matiereDetectee: String) {
        selectedMatiereForSave = matiereDetectee
        ouvrirEtapeChoixMatiereRpg()
    }

    /**
     * AJOUT v10 — Bouton DA conforme Règle 10 (3 couches FrameLayout).
     * isSaveOnly = true  → Cinzel #FFD700 (style RETOUR/FERMER)
     * isSaveOnly = false → Cinzel #1A0A00 (style ACTION/CONFIRMER)
     */
    private fun construireChoiceButton(texte: String, isSaveOnly: Boolean): FrameLayout {
        val btn = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
            )
            isClickable = true
            isFocusable = true
        }
        btn.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            try { setImageResource(R.drawable.bg_temple_button) }
            catch (_: Exception) { setBackgroundColor(Color.parseColor("#1A1A2E")) }
            scaleType = ImageView.ScaleType.FIT_XY
        })
        btn.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            try { setImageResource(R.drawable.bg_rpg_dialog) } catch (_: Exception) {}
            scaleType = ImageView.ScaleType.FIT_XY
            alpha = if (isSaveOnly) 0.22f else 0.18f
        })
        btn.addView(TextView(this).apply {
            text = texte
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(
                if (isSaveOnly) Color.parseColor("#FFD700")
                else Color.parseColor("#1A0A00")
            )
            gravity = Gravity.CENTER
            typeface = try { resources.getFont(R.font.cinzel) }
            catch (_: Exception) { Typeface.DEFAULT_BOLD }
            letterSpacing = 0.06f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        })
        return btn
    }



    /**
     * Demande facultativement un sous-dossier avant l'enregistrement final.
     * Le héros peut laisser vide pour ranger le savoir à la racine du temple.
     */
    private fun demanderSousDossierPuisSauvegarder(matiere: String, titreCours: String) {
        val input = android.widget.EditText(this).apply {
            hint = "Ex : Photosynthèse, Révolution, Géométrie…"
            setText(selectedFolderForSave)
            setSingleLine()
        }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Sous-dossier du temple")
            .setMessage("Tu peux créer un sous-dossier pour mieux organiser tes savoirs dans le temple de $matiere.")
            .setView(input)
            .setNegativeButton("Sans sous-dossier") { _, _ ->
                selectedFolderForSave = ""
                sauvegarderCoursPuisAfficherChoixQuiz(
                    matiere = matiere,
                    titreCours = titreCours,
                    folderName = ""
                )
            }
            .setPositiveButton("Enregistrer") { _, _ ->
                selectedFolderForSave = input.text?.toString()?.trim().orEmpty()
                sauvegarderCoursPuisAfficherChoixQuiz(
                    matiere = matiere,
                    titreCours = titreCours,
                    folderName = selectedFolderForSave
                )
            }
            .show()
    }

    /**
     * Après sauvegarde du savoir dans le bon temple, le joueur choisit :
     * - quiz avec timer
     * - quiz sans timer
     * - retour Oracle
     * - retour Panthéon
     */
    private fun sauvegarderCoursPuisAfficherChoixQuiz(matiere: String, titreCours: String, folderName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@ResultActivity)

                val nouveauCours = CourseEntry(
                    id = UUID.randomUUID().toString(),
                    subject = matiere,
                    title = titreCours,
                    dateAdded = System.currentTimeMillis(),
                    extractedText = generatedSummary,
                    keyConceptsString = "Nouveau,Généré",
                    difficultyLevel = 1,
                    folderName = folderName.trim(),
                    customTitle = titreCours
                )

                db.iAristoteDao().insertCourse(nouveauCours)

                val prefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("TOTAL_SCANS", prefs.getInt("TOTAL_SCANS", 0) + 1).apply()

                withContext(Dispatchers.Main) {
                    masquerPanneauRpgSauvegarde()
                    afficherConfirmationDivine(matiere, titreCours)
                    lifecycleScope.launch {
                        delay(1850)
                        afficherChoixPostSauvegardeQuiz(
                            matiere = matiere,
                            titreCours = titreCours,
                            folderName = folderName
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // BLOC B : Conversion Toast → Dialogue RPG
                    DialogRPGManager.showTechnicalError(
                        activity = this@ResultActivity,
                        errorType = TechnicalErrorType.GEMINI_API_ERROR
                    )
                }
            }
        }
    }

    private fun afficherChoixPostSauvegardeQuiz(matiere: String, titreCours: String, folderName: String) {
        val dialogRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            try { setBackgroundResource(R.drawable.bg_rpg_dialog) } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#E61A1A2E"))
            }
        }

        dialogRoot.addView(TextView(this).apply {
            text = "⚡ Le savoir a été gravé"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            typeface = try { resources.getFont(R.font.cinzel_bold) } catch (_: Exception) { Typeface.DEFAULT_BOLD }
        })

        dialogRoot.addView(TextView(this).apply {
            text = if (folderName.isBlank()) {
                "« $titreCours » repose désormais dans le temple de $matiere. Choisis maintenant ton destin."
            } else {
                "« $titreCours » repose désormais dans le temple de $matiere. Choisis maintenant ton destin."
            }
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(0, dp(10), 0, dp(12))
            typeface = try { resources.getFont(R.font.exo2) } catch (_: Exception) { Typeface.DEFAULT }
        })

        val buttonsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        dialogRoot.addView(buttonsContainer)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
            .setView(dialogRoot)
            .setCancelable(false)
            .create()

        fun addChoice(label: String, action: () -> Unit) {
            val button = TextView(this).apply {
                text = label
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(14), dp(14), dp(14))
                typeface = try { resources.getFont(R.font.cinzel) } catch (_: Exception) { Typeface.DEFAULT_BOLD }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
                try { setBackgroundResource(R.drawable.bg_rpg_dialog) } catch (_: Exception) {
                    setBackgroundColor(Color.parseColor("#661A1A2E"))
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    dialog.dismiss()
                    action()
                }
            }
            buttonsContainer.addView(button)
        }

        addChoice("⚡ Lancer le quiz avec timer") { lancerQuiz(matiere, true) }
        addChoice("🧘 Lancer le quiz sans timer") { lancerQuiz(matiere, false) }
        addChoice("📚 Garder seulement le résumé") {
            afficherMessageReminderDieu(matiere)
            binding.btnBack.visibility = View.VISIBLE
            binding.btnStartQuiz.visibility = View.GONE
            binding.btnInvoke.visibility = View.GONE
            binding.layoutActions.visibility = View.VISIBLE
        }
        addChoice("🔮 Revenir à l'Oracle") { finish() }
        addChoice("🏛 Revenir au Dashboard") {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        dialog.setOnShowListener {
            try {
                dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
            }
        }
        dialog.show()
    }


    // ═══════════════════════════════════════════════════════════════
    // SAUVEGARDE SANS QUIZ
    // ═══════════════════════════════════════════════════════════════

    /**
     * AJOUT v10 — Sauvegarde le cours dans Room SANS lancer le quiz.
     * Le mode Entraînement rappellera Gemini pour un QCM toujours différent.
     */
    private fun sauvegarderCoursSeulement(matiere: String, titreCours: String, folderName: String = "") {
        masquerPanneauRpgSauvegarde()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@ResultActivity)

                val nouveauCours = CourseEntry(
                    id = UUID.randomUUID().toString(),
                    subject = matiere,
                    title = titreCours,
                    dateAdded = System.currentTimeMillis(),
                    extractedText = generatedSummary,
                    keyConceptsString = "Nouveau,Généré",
                    difficultyLevel = 1,
                    folderName = folderName.trim(),
                    customTitle = titreCours
                )

                db.iAristoteDao().insertCourse(nouveauCours)

                val prefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("TOTAL_SCANS", prefs.getInt("TOTAL_SCANS", 0) + 1).apply()

                val ctx = BadgeManager.buildContext(this@ResultActivity)
                val nouveauxBadges = BadgeManager.evaluateAll(this@ResultActivity, ctx)

                withContext(Dispatchers.Main) {
                    afficherConfirmationDivine(matiere, titreCours)

                    if (nouveauxBadges.isNotEmpty()) {
                        // BLOC B : Conversion Toast → Dialogue RPG
                        DialogRPGManager.showReward(
                            activity = this@ResultActivity,
                            godId = "zeus",
                            message = "Bravo, héros ! Tu as débloqué un nouveau succès !",
                            additionalLabel = "BADGE",
                            additionalText = nouveauxBadges.first().nom
                        )
                        jouerSfx(R.raw.sfx_badge_unlock)
                    }

                    afficherMessageReminderDieu(matiere)
                    binding.btnBack.visibility = View.VISIBLE
                    binding.btnStartQuiz.visibility = View.GONE
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // BLOC B : Conversion Toast → Dialogue RPG
                    DialogRPGManager.showTechnicalError(
                        activity = this@ResultActivity,
                        errorType = TechnicalErrorType.GEMINI_API_ERROR
                    )
                }
            }
        }
    }

    /**
     * AJOUT v10 — Dialogue de rappel du dieu après sauvegarde sans quiz.
     * Informe l'utilisateur que le mode Entraînement génère un quiz frais.
     */
    private fun afficherMessageReminderDieu(matiere: String) {
        val godProfile = GodManager.fromMatiere(matiere)
        val nomDieu = godProfile?.nomDieu ?: "Zeus"

        val messageReminder = when (matiere) {
            "Mathématiques" -> "Le parchemin est sécurisé dans mes archives. Reviens le tester en mode Entraînement — chaque session forgera un quiz inédit !"
            "Français" -> "Ma chouette veille sur ce parchemin. Reviens l'éprouver — chaque retour génère des questions nouvelles."
            "SVT" -> "Conservé dans les abysses ! Mais la mémoire s'efface vite. Reviens en mode Entraînement pour un quiz régénéré à chaque fois."
            "Histoire" -> "Gravé dans les annales ! Un vrai guerrier revient s'entraîner régulièrement. Chaque session forgera de nouvelles épreuves."
            "Art/Musique" -> "L'œuvre est préservée ! Reviens vite — en mode Entraînement, je composerai un quiz inédit à chaque session."
            "Anglais" -> "Scroll saved! Come back to train — each session will forge a brand new quiz for you!"
            "Géographie" -> "Territoire cartographié. Reviens t'entraîner : chaque session générera un quiz différent sur ce cours."
            "Physique-Chimie" -> "Archivé dans ma forge cosmique ! Reviens — je forgerai un quiz unique à chaque session d'entraînement."
            "Philo/SES" -> "Conservé. Une idée non éprouvée reste fragile. Reviens — chaque session produira de nouvelles questions philosophiques."
            "Vie & Projets" -> "Le feu est préservé. Reviens l'alimenter en mode Entraînement pour des quiz toujours renouvelés !"
            else -> "Cours sauvegardé. Reviens en mode Entraînement : chaque session génère un quiz différent !"
        }

        val godId = when (nomDieu.lowercase()) {
            "zeus" -> "zeus"
            "athéna", "athena" -> "athena"
            "poséidon", "poseidon" -> "poseidon"
            "arès", "ares" -> "ares"
            "aphrodite" -> "aphrodite"
            "hermès", "hermes" -> "hermes"
            "déméter", "demeter" -> "demeter"
            "héphaïstos", "hephaistos", "hephaestus" -> "hephaestus"
            "apollon", "apollo" -> "apollo"
            "prométhée", "promethee", "prometheus" -> "prometheus"
            else -> "zeus"
        }

        DialogRPGManager.showInfo(
            activity = this,
            godId = godId,
            title = "$nomDieu — Rappel du temple",
            message = messageReminder
        )
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * PACK 2 — CONFIRMATION DIVINE ANIMÉE
     * ═══════════════════════════════════════════════════════════════
     *
     * Overlay fullscreen temporaire (1.5s) avec :
     * - Flash doré plein écran
     * - Portrait du dieu de la matière en scale-in
     * - Texte "✦ [Dieu] accueille ton savoir ! ✦"
     * - SFX thunder_confirm
     * - Tentative Lottie en bonus
     */
    private fun afficherConfirmationDivine(matiere: String, titreCours: String) {
        if (isFinishing || isDestroyed) return

        val godProfile = GodManager.fromMatiere(matiere)
        val nomDieu = godProfile?.nomDieu ?: "Zeus"

        val root = window.decorView.rootView as? FrameLayout ?: return

        val overlay = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#CC000000"))
            alpha = 0f
            elevation = 1000f
        }

        val flash = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#FFD700"))
            alpha = 0f
        }
        overlay.addView(flash)

        val portrait = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                dp(180), dp(180)
            ).apply {
                gravity = Gravity.CENTER
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            scaleX = 0.3f
            scaleY = 0.3f
            alpha = 0f

            try {
                when (matiere) {
                    "Mathématiques" -> setImageResource(R.drawable.ic_zeus_mini)
                    "Français" -> setImageResource(R.drawable.ic_athena_mini)
                    "SVT" -> setImageResource(R.drawable.ic_poseidon_mini)
                    "Histoire" -> setImageResource(R.drawable.ic_ares_mini)
                    "Art/Musique", "Art" -> setImageResource(R.drawable.ic_aphrodite_mini)
                    "Langues", "Anglais" -> setImageResource(R.drawable.ic_hermes_mini)
                    "Géographie" -> setImageResource(R.drawable.ic_demeter_mini)
                    "Physique-Chimie" -> setImageResource(R.drawable.ic_hephaistos_mini)
                    "Philo/SES" -> setImageResource(R.drawable.ic_apollon_mini)
                    "Vie & Projets" -> setImageResource(R.drawable.ic_prometheus_mini)
                    else -> setImageResource(R.drawable.ic_zeus_mini)
                }
            } catch (_: Exception) {
                setImageResource(R.drawable.ic_zeus_mini)
            }
        }
        overlay.addView(portrait)

        val text = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                topMargin = dp(220)
            }
            text = "✦ $nomDieu accueille ton savoir ! ✦"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            try {
                typeface = androidx.core.content.res.ResourcesCompat.getFont(this@ResultActivity, R.font.cinzel)
            } catch (_: Exception) {
                typeface = Typeface.DEFAULT_BOLD
            }
            gravity = Gravity.CENTER
            alpha = 0f
        }
        overlay.addView(text)

        root.addView(overlay)

        try { jouerSfx(R.raw.sfx_thunder_confirm) } catch (e: Exception) {
            Log.w("REVIZEUS_RESULT", "showDivineConfirmationOverlay: SFX tonnerre indisponible", e)
        }

        lifecycleScope.launch {
            ObjectAnimator.ofFloat(overlay, "alpha", 0f, 1f).apply {
                duration = 200
                start()
            }

            delay(100)

            ObjectAnimator.ofFloat(flash, "alpha", 0f, 0.7f, 0f).apply {
                duration = 600
                start()
            }

            val scaleX = ObjectAnimator.ofFloat(portrait, "scaleX", 0.3f, 1f).apply { duration = 500 }
            val scaleY = ObjectAnimator.ofFloat(portrait, "scaleY", 0.3f, 1f).apply { duration = 500 }
            val alphaPortrait = ObjectAnimator.ofFloat(portrait, "alpha", 0f, 1f).apply { duration = 400 }

            AnimatorSet().apply {
                playTogether(scaleX, scaleY, alphaPortrait)
                interpolator = android.view.animation.OvershootInterpolator()
                start()
            }

            delay(200)

            ObjectAnimator.ofFloat(text, "alpha", 0f, 1f).apply {
                duration = 300
                start()
            }

            try {
                val lottieView = com.airbnb.lottie.LottieAnimationView(this@ResultActivity).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    val lottieRes = resources.getIdentifier("lottie_divine_confirmation", "raw", packageName)
                    if (lottieRes != 0) {
                        setAnimation(lottieRes)
                        loop(false)
                        alpha = 0.6f
                        playAnimation()
                    }
                }
                overlay.addView(lottieView, 0)
            } catch (e: Exception) {
                Log.d("REVIZEUS", "Lottie confirmation indisponible : ${e.message}")
            }

            delay(1500)

            ObjectAnimator.ofFloat(overlay, "alpha", 1f, 0f).apply {
                duration = 300
                start()
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        try { root.removeView(overlay) } catch (e: Exception) {
                            Log.w("REVIZEUS_RESULT", "showDivineConfirmationOverlay: suppression overlay impossible", e)
                        }
                    }
                })
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MÉTHODES EXISTANTES — CONSERVATION INTÉGRALE
    // ═══════════════════════════════════════════════════════════════

    private fun startScanAnimation() {
        try {
            isScanning = true

            binding.oracleScanOverlay.visibility = View.VISIBLE
            binding.oracleScanOverlay.alpha = 1f
            binding.oracleScanOverlay.bringToFront()

            binding.scanFxOverlay.visibility = View.VISIBLE
            binding.scanFxOverlay.alpha = 0.16f
            binding.scanFxOverlay.bringToFront()

            binding.scanFlash.visibility = View.VISIBLE
            binding.scanFlash.alpha = 0f
            binding.scanFlash.bringToFront()

            binding.scanLine.visibility = View.VISIBLE
            binding.scanLine.alpha = 1f
            binding.scanLine.translationY = 0f
            binding.scanLine.bringToFront()

            scanAnimator?.cancel(); scanAnimator = null
            scanPulseAnimator?.cancel(); scanPulseAnimator = null
            scanFlashAnimator?.cancel(); scanFlashAnimator = null

            scanFlashAnimator = ObjectAnimator.ofFloat(
                binding.scanFlash, View.ALPHA, 0f, 0.70f, 0f
            ).apply {
                duration = 380L
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }

            scanPulseAnimator = ObjectAnimator.ofFloat(
                binding.scanLine, View.ALPHA, 0.55f, 1f, 0.72f
            ).apply {
                duration = 650L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }

            binding.oracleScanOverlay.post {
                val overlayHeight = binding.oracleScanOverlay.height
                val screenHeight = resources.displayMetrics.heightPixels
                val realScanHeight = maxOf(overlayHeight, screenHeight).toFloat()
                val laserHeight = binding.scanLine.height.toFloat().coerceAtLeast(24f)
                val destinationY = (realScanHeight - laserHeight).coerceAtLeast(0f)

                binding.scanLine.translationY = 0f
                binding.scanLine.bringToFront()

                scanAnimator = ObjectAnimator.ofFloat(
                    binding.scanLine, View.TRANSLATION_Y, 0f, destinationY
                ).apply {
                    duration = 1600L
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    interpolator = LinearInterpolator()
                    start()
                }
            }

            try {
                SoundManager.setMusicVolume(scanBgmVolume)
                SoundManager.playLoopingScan(
                    context = this,
                    resId = R.raw.sfx_oracle_scan,
                    volume = scanLoopVolume
                )
            } catch (e: Exception) {
                Log.e("REVIZEUS_RESULT", "Erreur audio scan : ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("REVIZEUS_RESULT", "Erreur startScanAnimation : ${e.message}", e)
        }
    }

    private fun stopScanAnimation() {
        try {
            isScanning = false
            scanAnimator?.cancel(); scanAnimator = null
            scanPulseAnimator?.cancel(); scanPulseAnimator = null
            scanFlashAnimator?.cancel(); scanFlashAnimator = null

            binding.scanLine.visibility = View.GONE
            binding.scanLine.translationY = 0f
            binding.scanLine.alpha = 1f
            binding.scanFlash.visibility = View.GONE
            binding.scanFlash.alpha = 0f
            binding.scanFxOverlay.visibility = View.GONE
            binding.scanFxOverlay.alpha = 0f
            binding.oracleScanOverlay.visibility = View.GONE
            binding.oracleScanOverlay.alpha = 0f

            try { SoundManager.stopLoopingScan() }
            catch (e: Exception) { Log.e("REVIZEUS_RESULT", "Erreur stopLoopingScan : ${e.message}") }

            try { SoundManager.setMusicVolume(normalBgmVolume) }
            catch (e: Exception) { Log.e("REVIZEUS_RESULT", "Erreur restauration volume BGM : ${e.message}") }
        } catch (e: Exception) {
            Log.e("REVIZEUS_RESULT", "Erreur stopScanAnimation : ${e.message}", e)
        }
    }

    private fun restaurerBgmOffrandeSiNecessaire() {
        try {
            SoundManager.rememberMusic(R.raw.bgm_offrande)
            SoundManager.setMusicVolume(normalBgmVolume)
            if (!SoundManager.isPlayingMusic() ||
                SoundManager.getCurrentMusicResId() != R.raw.bgm_offrande
            ) {
                SoundManager.playMusicDelayed(this, R.raw.bgm_offrande, 120L)
            }
        } catch (e: Exception) {
            Log.e("REVIZEUS_RESULT", "Erreur restaurerBgmOffrandeSiNecessaire : ${e.message}")
        }
    }

    private fun rebindLoadingStateIfNeeded() {
        if (invokeAnalysisJob?.isActive == true) {
            binding.btnInvoke.visibility = View.GONE
            binding.layoutActions.visibility = View.GONE
            binding.layoutLoading.visibility = View.VISIBLE
            binding.tvResult.visibility = View.GONE
            if (!isScanning) {
                startScanAnimation()
            }
        } else {
            binding.layoutLoading.visibility = View.GONE
            binding.layoutActions.visibility = View.VISIBLE
            if (generatedSummary.isNotBlank()) {
                binding.btnInvoke.visibility = View.GONE
                binding.btnStartQuiz.visibility = View.VISIBLE
            } else {
                binding.btnInvoke.visibility = View.VISIBLE
            }
        }
    }

    private fun initialiserPanneauRpgSauvegarde() {
        binding.layoutGodSaveOverlay.visibility = View.GONE
        binding.layoutGodSaveOverlay.background?.alpha = 178
        binding.layoutMatterChoices.removeAllViews()
        binding.etCourseTitle.visibility = View.GONE
        binding.btnGodConfirm.visibility = View.GONE
        binding.btnGodCancel.visibility = View.GONE

        binding.btnGodCancel.setOnClickListener { masquerPanneauRpgSauvegarde() }

        binding.btnGodConfirm.setOnClickListener {
            val matiere = selectedMatiereForSave ?: return@setOnClickListener
            val titreChoisi = binding.etCourseTitle.text?.toString()?.trim().orEmpty()
            sauvegarderCoursPuisAfficherChoixQuiz(
                matiere = matiere,
                titreCours = titreChoisi.ifBlank { suggererNomCours(matiere) },
                folderName = ""
            )
        }
    }

    private fun ouvrirEtapeChoixMatiereRpg() {
        selectedMatiereForSave = null
        binding.layoutMatterChoices.removeAllViews()
        binding.etCourseTitle.setText("")
        binding.etCourseTitle.visibility = View.GONE
        binding.btnGodConfirm.visibility = View.GONE
        binding.btnGodCancel.visibility = View.VISIBLE
        binding.layoutGodSaveOverlay.visibility = View.VISIBLE

        val matiereInitiale = PantheonConfig.GODS.firstOrNull()?.matiere ?: "Mathématiques"
        appliquerDieuSurPanneau(matiereInitiale)
        parlerCommeDieu(
            matiere = matiereInitiale,
            texte = "Le titre a été suggéré par l'Oracle, mais c'est à toi de choisir le temple qui gardera ce savoir."
        )

        PantheonConfig.GODS.forEach { godInfo ->
            val ligne = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
                try { setBackgroundResource(R.drawable.bg_rpg_dialog) } catch (_: Exception) {
                    setBackgroundColor(Color.parseColor("#661A1A2E"))
                }
                elevation = dp(3).toFloat()
                isClickable = true
                isFocusable = true
            }

            val icon = ImageView(this).apply {
                val resId = resources.getIdentifier(godInfo.iconResName, "drawable", packageName)
                if (resId != 0) setImageResource(resId)
                layoutParams = LinearLayout.LayoutParams(52, 52)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

            val textBlock = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply { marginStart = 14 }
            }

            textBlock.addView(TextView(this).apply {
                text = godInfo.matiere
                textSize = 15f
                setTextColor(Color.WHITE)
            })
            textBlock.addView(TextView(this).apply {
                text = godInfo.divinite
                textSize = 11f
                setTextColor(Color.parseColor("#D8C690"))
            })

            ligne.addView(icon)
            ligne.addView(textBlock)
            ligne.setOnClickListener {
                try { SoundManager.playSFX(this@ResultActivity, R.raw.sfx_dialogue_blip) }
                catch (e: Exception) {
                    Log.w("REVIZEUS_RESULT", "ouvrirEtapeChoixMatiereRpg: SFX dialogue indisponible", e)
                }
                ouvrirEtapeNommageCoursRpg(godInfo.matiere)
            }
            binding.layoutMatterChoices.addView(ligne)
        }
    }

    private fun ouvrirEtapeNommageCoursRpg(matiere: String) {
        selectedMatiereForSave = matiere
        appliquerDieuSurPanneau(matiere)
        binding.etCourseTitle.visibility = View.VISIBLE
        binding.btnGodConfirm.visibility = View.VISIBLE
        binding.btnGodCancel.visibility = View.VISIBLE

        val suggestion = titreAutoGenere.ifBlank { suggererNomCours(matiere) }
        binding.etCourseTitle.setText(suggestion)
        binding.etCourseTitle.setSelection(binding.etCourseTitle.text?.length ?: 0)

        val god = GodManager.fromMatiere(matiere)
        val texte = when (matiere) {
            "Mathématiques" -> "Mortel, comment veux-tu nommer ce parchemin de Mathématiques dans ma bibliothèque céleste ?"
            "Français" -> "Donne un nom précis à ce parchemin de Français."
            "SVT" -> "Quel nom veux-tu donner à ce savoir vivant pour mes archives marines ?"
            "Histoire" -> "Grave un titre digne des annales pour ce parchemin d'Histoire."
            "Art/Musique" -> "Quel nom veux-tu offrir à cette œuvre de savoir ?"
            "Anglais" -> "Name this scroll wisely, hero. Comment veux-tu l'appeler ?"
            "Géographie" -> "Choisis un nom clair pour ce parchemin des terres et des climats."
            "Physique-Chimie" -> "Nommons correctement cette forge de connaissances. Quel titre ?"
            "Philo/SES" -> "Comment vas-tu nommer cette idée, afin qu'elle survive au temps ?"
            "Vie & Projets" -> "Donne un nom visionnaire à ce parchemin. Je garderai son feu allumé."
            else -> god?.getDialogue(GodDialogContext.NOUVEAU_COURS)
                ?: "Comment veux-tu nommer ce parchemin ?"
        }
        parlerCommeDieu(matiere = matiere, texte = texte)
    }

    private fun appliquerDieuSurPanneau(matiere: String) {
        appliquerFondPremiumResultPourMatiere(matiere)
        val god = GodManager.fromMatiere(matiere)
        if (god != null) {
            binding.imgGodSave.setImageResource(god.avatarDialogRes)
            binding.tvGodSaveName.text = god.nomDieu
            binding.tvGodSaveName.setTextColor(Color.parseColor(god.couleurHex))
        } else {
            binding.tvGodSaveName.text = "ZEUS"
            binding.tvGodSaveName.setTextColor(Color.parseColor("#FFD700"))
            binding.imgGodSave.setImageResource(R.drawable.avatar_zeus_dialog)
        }
    }

    private fun parlerCommeDieu(matiere: String, texte: String) {
        appliquerDieuSurPanneau(matiere)
        godTypewriterJob?.cancel()
        godTypewriterJob = godSpeechAnimator.typewriteSimple(
            scope = lifecycleScope,
            chibiView = binding.imgGodSave,
            textView = binding.tvGodSaveSpeech,
            text = texte,
            context = this@ResultActivity
        )
        val god = GodManager.fromMatiere(matiere)
        binding.viewGodAccent.setBackgroundColor(
            Color.parseColor(god?.couleurHex ?: "#FFD700")
        )
    }

    private fun masquerPanneauRpgSauvegarde() {
        godTypewriterJob?.cancel()
        godSpeechAnimator.stopSpeaking(binding.imgGodSave)
        binding.layoutGodSaveOverlay.visibility = View.GONE
        binding.layoutMatterChoices.removeAllViews()
        binding.etCourseTitle.visibility = View.GONE
        binding.btnGodConfirm.visibility = View.GONE
    }

    private fun suggererNomCours(matiere: String): String {
        val premiereLigne = generatedSummary.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?.take(40)
            .orEmpty()
        return if (premiereLigne.isNotBlank()) "$matiere — $premiereLigne"
        else "$matiere — Nouveau parchemin"
    }

    /**
     * MODIFIÉ v10 — Sauvegarde le cours ET lance le quiz immédiatement.
     * Le quiz est chargé depuis quizGenere (en mémoire live du scan Oracle).
     * Il n'est PAS persisté. Le mode Entraînement rappellera Gemini.
     */
    private fun sauvegarderCoursEtLancerQuiz(matiere: String, titreCours: String, folderName: String = "", isTimedMode: Boolean = false) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@ResultActivity)

                val nouveauCours = CourseEntry(
                    id = UUID.randomUUID().toString(),
                    subject = matiere,
                    title = titreCours,
                    dateAdded = System.currentTimeMillis(),
                    extractedText = generatedSummary,
                    keyConceptsString = "Nouveau,Généré",
                    difficultyLevel = 1,
                    folderName = folderName.trim(),
                    customTitle = titreCours
                )

                db.iAristoteDao().insertCourse(nouveauCours)

                val prefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("TOTAL_SCANS", prefs.getInt("TOTAL_SCANS", 0) + 1).apply()

                val ctx = BadgeManager.buildContext(this@ResultActivity)
                val nouveauxBadges = BadgeManager.evaluateAll(this@ResultActivity, ctx)

                withContext(Dispatchers.Main) {
                    masquerPanneauRpgSauvegarde()
                    // BLOC B : Conversion Toast → Dialogue RPG
                    DialogRPGManager.showInfo(
                        activity = this@ResultActivity,
                        godId = DialogRPGManager.selectGodForContext(DialogContext.PEDAGOGY),
                        message = "Athéna a confié ton parchemin « $titreCours » au temple de $matiere !"
                    )

                    if (nouveauxBadges.isNotEmpty()) {
                        // BLOC B : Conversion Toast → Dialogue RPG
                        DialogRPGManager.showReward(
                            activity = this@ResultActivity,
                            godId = "zeus",
                            message = "Bravo, héros ! Tu as débloqué un nouveau succès !",
                            additionalLabel = "BADGE",
                            additionalText = nouveauxBadges.first().nom
                        )
                        jouerSfx(R.raw.sfx_badge_unlock)
                    }

                    lancerQuiz(matiere, isTimedMode)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // BLOC B : Conversion Toast → Dialogue RPG
                    DialogRPGManager.showTechnicalError(
                        activity = this@ResultActivity,
                        errorType = TechnicalErrorType.GEMINI_API_ERROR
                    )
                }
            }
        }
    }

    private fun lancerQuiz(matiere: String, isTimedMode: Boolean) {
        QuizActivity.pendingQuestions = ArrayList(quizGenere)
        QuizActivity.currentMatiere = matiere
        QuizActivity.isTimedMode = isTimedMode
        val intent = Intent(this, QuizActivity::class.java).apply {
            putExtra("SUMMARY_TEXT", generatedSummary)
            putExtra("IS_TIMED_MODE", isTimedMode)
        }
        startActivity(intent)
        finish()
    }

    // ═══════════════════════════════════════════════════════════════
    // AJOUT v11 — SPEAKER TTS RESULT / PANNEAU DIVIN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Injecte les boutons speaker sans toucher au XML existant.
     * - un bouton pour lire le résumé / message principal
     * - un bouton pour lire le panneau divin de sauvegarde
     */

    /**
     * Applique un rendu homogène aux icônes speaker pour éviter l'effet zoomé.
     */
    private fun styleSpeakerButton(button: ImageButton, sizeDp: Int, paddingDp: Int = 6) {
        button.layoutParams = ViewGroup.MarginLayoutParams(dp(sizeDp), dp(sizeDp))
        button.scaleType = ImageView.ScaleType.FIT_CENTER
        button.adjustViewBounds = true
        button.setPadding(dp(paddingDp), dp(paddingDp), dp(paddingDp), dp(paddingDp))
        button.background = null
        button.cropToPadding = false
        button.alpha = 0.88f
    }

    private fun setupResultSpeakerButtons() {
        setupSummarySpeakerButton()
        setupGodSaveSpeakerButton()
    }

    /**
     * Bouton speaker sous le résumé analysé.
     * Lecture :
     * - résumé généré si disponible
     * - sinon texte actuellement visible dans tvResult
     */
    private fun setupSummarySpeakerButton() {
        try {
            val parent = binding.tvResult.parent as? ViewGroup ?: return
            val existing = parent.findViewWithTag<View>("btn_tts_result_summary")
            if (existing != null) {
                existing.setOnClickListener { speakSummaryText() }
                return
            }

            val btnTts = ImageButton(this).apply {
                tag = "btn_tts_result_summary"
                contentDescription = "Lire le résumé à voix haute"
                try { setImageResource(R.drawable.ic_speaker_tts) }
                catch (_: Exception) { setImageResource(android.R.drawable.ic_btn_speak_now) }
                styleSpeakerButton(this, 40, 7)
                setOnClickListener { speakSummaryText() }
            }

            val size = dp(40)
            val marginTop = dp(8)

            when (parent) {
                is LinearLayout -> {
                    btnTts.layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        topMargin = marginTop
                        gravity = Gravity.END
                    }
                    val index = parent.indexOfChild(binding.tvResult)
                    if (index >= 0 && index < parent.childCount - 1) {
                        parent.addView(btnTts, index + 1)
                    } else {
                        parent.addView(btnTts)
                    }
                }
                else -> {
                    btnTts.layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                        topMargin = marginTop
                    }
                    parent.addView(btnTts)
                }
            }
        } catch (e: Exception) {
            Log.w("REVIZEUS_RESULT", "setupSummarySpeakerButton: injection bouton TTS impossible", e)
        }
    }

    private fun speakSummaryText() {
        val textToRead = ttsSafeSummary.ifBlank {
            generatedSummary.ifBlank {
                binding.tvResult.text?.toString()?.trim().orEmpty()
            }
        }

        if (textToRead.isNotBlank()) {
            tts.stop()
            tts.speak(textToRead)
        }
    }

    /**
     * Bouton speaker du panneau RPG de sauvegarde.
     * Lit le dieu actif, son texte et le titre courant du parchemin.
     */
    private fun setupGodSaveSpeakerButton() {
        try {
            val container = binding.layoutGodSaveCard
            val existing = container.findViewWithTag<View>("btn_tts_result_godsave")
            if (existing != null) {
                existing.setOnClickListener { speakGodSaveOverlay() }
                return
            }

            val btnTts = ImageButton(this).apply {
                tag = "btn_tts_result_godsave"
                contentDescription = "Lire le panneau divin à voix haute"
                try { setImageResource(R.drawable.ic_speaker_tts) }
                catch (_: Exception) { setImageResource(android.R.drawable.ic_btn_speak_now) }
                styleSpeakerButton(this, 36, 6)
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                    marginStart = dp(8)
                }
                setOnClickListener { speakGodSaveOverlay() }
            }

            container.addView(btnTts)
        } catch (e: Exception) {
            Log.w("REVIZEUS_RESULT", "setupGodSaveSpeakerButton: injection bouton TTS impossible", e)
        }
    }

    private fun speakGodSaveOverlay() {
        val nomDieu = binding.tvGodSaveName.text?.toString()?.trim().orEmpty()
        val speech = binding.tvGodSaveSpeech.text?.toString()?.trim().orEmpty()
        val titre = binding.etCourseTitle.text?.toString()?.trim().orEmpty()

        val textToRead = buildString {
            if (nomDieu.isNotBlank()) append("$nomDieu. ")
            if (speech.isNotBlank()) append("$speech. ")
            if (titre.isNotBlank()) append("Titre actuel du parchemin : $titre.")
        }.trim()

        if (textToRead.isNotBlank()) {
            tts.speak(textToRead)
        }
    }

    private fun loadAndCompressBitmap(uri: Uri): Bitmap? {
        return try {
            val optionsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            var inputStream: InputStream? = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, optionsBounds)
            inputStream?.close()

            val maxDim = 1600
            var sampleSize = 1
            while (optionsBounds.outWidth / sampleSize > maxDim ||
                optionsBounds.outHeight / sampleSize > maxDim
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            inputStream = contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()
            bmp
        } catch (_: Exception) { null }
    }

    private fun afficherErreurDivine(message: String) {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutActions.visibility = View.VISIBLE
        binding.layoutSummaryContent.removeAllViews()
        binding.tvResult.visibility = View.VISIBLE
        binding.tvResult.text = message
        ttsSafeSummary = ""
        binding.btnInvoke.isEnabled = true
        binding.btnInvoke.visibility = View.VISIBLE
        binding.btnStartQuiz.visibility = View.GONE
    }


    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
    ).toInt()

    override fun onResume() {
        super.onResume()
        appliquerFondPremiumResultPourMatiere(selectedMatiereForSave ?: matiereAutoDetectee)

        if (generatedSummary.isNotBlank()) {
            val parsedSummary = parseOracleSummaryForDisplay(generatedSummary)
            renderPremiumSummary(parsedSummary)
            if (ttsSafeSummary.isBlank()) {
                ttsSafeSummary = parsedSummary.plainText.ifBlank { formatSummarySafe(generatedSummary).second }
            }
        }

        rebindLoadingStateIfNeeded()
        if (invokeAnalysisJob?.isActive != true) {
            restaurerBgmOffrandeSiNecessaire()
        }
    }

    override fun onPause() {
        super.onPause()
        animatedBackgroundHelper?.stop()
        tts.stop()
        godTypewriterJob?.cancel()
        godSpeechAnimator.stopSpeaking(binding.imgGodSave)
        stopScanAnimation()
        initialBgmJob?.cancel()
        if (isFinishing) {
            try { SoundManager.stopLoopingScan() }
            catch (e: Exception) { Log.e("REVIZEUS_RESULT", "stopLoopingScan : ${e.message}") }
        }
    }

    override fun onDestroy() {
        animatedBackgroundHelper?.release()
        animatedBackgroundHelper = null
        initialBgmJob?.cancel()
        invokeAnalysisJob?.cancel()
        initialBgmJob = null
        invokeAnalysisJob = null
        super.onDestroy()
        tts.release()
        stopScanAnimation()
        try { SoundManager.stopLoopingScan() }
        catch (e: Exception) { Log.e("REVIZEUS_RESULT", "stopLoopingScan onDestroy : ${e.message}") }
        godTypewriterJob?.cancel()
        godSpeechAnimator.release(binding.imgGodSave)
    }
}

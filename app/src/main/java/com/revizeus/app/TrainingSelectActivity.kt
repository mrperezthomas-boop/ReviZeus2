package com.revizeus.app

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.databinding.ActivityTrainingBinding
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.CourseEntry
import com.revizeus.app.models.IAristoteEngine
import com.revizeus.app.models.QuizQuestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ═══════════════════════════════════════════════════════════════
 * TRAINING SELECT ACTIVITY
 * ═══════════════════════════════════════════════════════════════
 *
 * CORRECTION v10 — GÉNÉRATION QUIZ À LA DEMANDE :
 *
 * Principe : chaque lancement de quiz en mode Entraînement appelle
 * GeminiManager.genererContenuOracle() avec le texte extrait du cours
 * (CourseEntry.extractedText stocké dans Room).
 *
 * Pourquoi c'est mieux que stocker des questions :
 * - Chaque session produit un QCM différent (Gemini varie ses formulations)
 * - Pas de répétition en boucle des mêmes questions
 * - Le texte du cours reste la source de vérité unique
 * - Compatible avec les cours scannés avant v10 (ils ont déjà leur texte)
 *
 * Flux :
 * 1. Sélection d'un cours ou Épreuve Ultime
 * 2. Affichage loading (dieu qui parle + animation)
 * 3. Appel Gemini en coroutine IO
 * 4. Parsing IAristoteEngine.decoderReponse()
 * 5. Injection QuizActivity.pendingQuestions + lancement TrainingQuizActivity
 *
 * Fallback : si Gemini échoue, message d'erreur du dieu, pas de crash.
 *
 * AJOUT BLOC 1 — FEEDBACK DE CHARGEMENT DÈS LE CLIC SUR UN DIEU :
 * - Affiche immédiatement le loader dès qu'une matière est choisie
 * - Empêche les doubles clics pendant la préparation de la liste de cours
 * - Retire le loader juste avant que le texte du dieu commence à s'afficher
 * - Ne modifie pas l'architecture existante ni le flux quiz déjà en place
 *
 * BLOC 1C — RÉCOMPENSES DE L’ENTRAÎNEMENT ULTIME :
 * - garantit qu'une question ultime transporte bien sa matière source réelle
 * - évite de laisser "Panthéon" comme matière de scoring
 * - sécurise la sérialisation subject / courseId / difficulty jusqu'au résultat
 * ═══════════════════════════════════════════════════════════════
 */
class TrainingSelectActivity : BaseActivity() {

    private lateinit var binding: ActivityTrainingBinding
    private var animatedBackgroundHelper: AnimatedBackgroundHelper? = null
    private var olympianParticlesView: OlympianParticlesView? = null

    private val godAnim = GodSpeechAnimator()
    private var typewriterJob: Job? = null

    /** Job de génération Gemini en cours — annulable si l'utilisateur navigue ailleurs. */
    private var generationJob: Job? = null

    /**
     * Job de préparation d'une sélection de matière.
     * Sert à annuler proprement une ancienne préparation si l'utilisateur spamme les clics.
     */
    private var trainingSelectionJob: Job? = null

    /**
     * Verrou UI simple : évite les doubles clics pendant le court chargement
     * entre la sélection du dieu et l'apparition effective du dialogue.
     */
    private var isPreparingTrainingSelection: Boolean = false

    private var matiereThemeActive: String = "Mathématiques"
    private var currentTrainingBgmResId: Int = R.raw.bgm_training_select

    // LOADER IA GLOBAL — l'entraînement doit utiliser explicitement le LoadingDivineDialog
    // pour toute attente perceptible liée à l'IA ou à la préparation des épreuves.
    private var divineTrainingLoadingDialog: LoadingDivineDialog? = null
    private var interactionShieldView: View? = null

    // ═══════════════════════════════════════════════════════════════
    // THÈMES PAR MATIÈRE
    // ═══════════════════════════════════════════════════════════════

    private fun getMatiereBackgroundRes(matiere: String): Int {
        return when (matiere.trim()) {
            "Mathématiques" -> R.drawable.bg_select_maths_zeus
            "Français" -> R.drawable.bg_select_francais_athena
            "SVT" -> R.drawable.bg_select_svt_poseidon
            "Histoire" -> R.drawable.bg_select_histoire_ares
            "Art/Musique", "Art" -> R.drawable.bg_select_art_aphrodite
            "Langues", "Anglais" -> R.drawable.bg_select_anglais_hermes
            "Géographie" -> R.drawable.bg_select_geographie_demeter
            "Physique-Chimie" -> R.drawable.bg_select_physique_hephaistos
            "Philo/SES" -> R.drawable.bg_select_philo_apollon
            "Vie & Projets" -> R.drawable.bg_select_vie_promethee
            else -> R.drawable.bg_select_maths_zeus
        }
    }

    private fun getMatiereBgmRes(matiere: String): Int {
        return when (matiere.trim()) {
            "Mathématiques" -> R.raw.bgm_select_maths_zeus
            "Français" -> R.raw.bgm_select_francais_athena
            "SVT" -> R.raw.bgm_select_svt_poseidon
            "Histoire" -> R.raw.bgm_select_histoire_ares
            "Art/Musique", "Art" -> R.raw.bgm_select_art_aphrodite
            "Langues", "Anglais" -> R.raw.bgm_select_anglais_hermes
            "Géographie" -> R.raw.bgm_select_geographie_demeter
            "Physique-Chimie" -> R.raw.bgm_select_physique_hephaistos
            "Philo/SES" -> R.raw.bgm_select_philo_apollon
            "Vie & Projets" -> R.raw.bgm_select_vie_promethee
            else -> R.raw.bgm_training_select
        }
    }

    private fun getMatiereAvatarRes(matiere: String): Int {
        return when (matiere.trim()) {
            "Mathématiques" -> R.drawable.avatar_zeus_dialog
            "Français" -> R.drawable.avatar_athena_dialog
            "SVT" -> R.drawable.avatar_poseidon_dialog
            "Histoire" -> R.drawable.avatar_ares_dialog
            "Art/Musique", "Art" -> R.drawable.avatar_aphrodite_dialog
            "Langues", "Anglais" -> R.drawable.avatar_hermes_dialog
            "Géographie" -> R.drawable.avatar_demeter_dialog
            "Physique-Chimie" -> R.drawable.avatar_hephaistos_dialog
            "Philo/SES" -> R.drawable.avatar_apollon_dialog
            "Vie & Projets" -> R.drawable.avatar_promethee_dialog
            else -> R.drawable.avatar_zeus_dialog
        }
    }

    private fun appliquerThemeMatiere(matiere: String, specialBgm: Int? = null) {
        matiereThemeActive = matiere
        try {
            binding.ivTrainingBackground.setImageResource(getMatiereBackgroundRes(matiere))
            binding.imgGodChibi.setImageResource(getMatiereAvatarRes(matiere))

            val bgmToPlay = specialBgm ?: getMatiereBgmRes(matiere)
            currentTrainingBgmResId = bgmToPlay
            SoundManager.rememberMusic(bgmToPlay)
            SoundManager.playMusic(this, bgmToPlay)
        } catch (e: Exception) {
            Log.w("REVIZEUS_TRAINING", "appliquerThemeMatiere: application thème/audio échouée pour $matiere", e)
        }

        val godProfile = PantheonConfig.findByMatiere(matiere)
            ?: PantheonConfig.findByDivinite("Zeus")
            ?: PantheonConfig.GODS.first()

        animatedBackgroundHelper?.configurePremiumBackground(
            staticDrawableRes = getMatiereBackgroundRes(matiere),
            videoRawRes = getRawResByName("creation_quiz"),
            imageAlpha = 0.25f,
            loopVideo = true,
            videoVolume = 0.50f
        )
        animatedBackgroundHelper?.start(
            accentColor = godProfile.couleur,
            mode = OlympianParticlesView.ParticleMode.TRAINING
        )
    }

    private fun faireParlerLeDieu(texte: String) {
        typewriterJob?.cancel()
        typewriterJob = godAnim.typewriteWithShake(
            scope = lifecycleScope,
            chibiView = binding.imgGodChibi,
            textView = binding.tvGodSpeech,
            text = texte,
            delayMs = 35L,
            intensity = 2.5f,
            onChar = {
                try { SoundManager.playSFXLow(this, R.raw.sfx_dialogue_blip) } catch (e: Exception) {
                    Log.w("REVIZEUS_TRAINING", "faireParlerLeDieu: SFX dialogue indisponible", e)
                }
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        installerAmbianceOlympienne()

        currentTrainingBgmResId = R.raw.bgm_training_select
        try {
            SoundManager.rememberMusic(currentTrainingBgmResId)
            SoundManager.playMusic(this, currentTrainingBgmResId)
        } catch (e: Exception) {
            Log.w("REVIZEUS_TRAINING", "onCreate: impossible de lancer la BGM training", e)
        }
        faireParlerLeDieu("Je suis Zeus. Choisis la matière que tu veux affronter, ou tente l'Épreuve Ultime.")

        actualiserCompteurs()

        binding.btnZeus.setOnClickListener { launchTraining("Mathématiques") }
        binding.btnAthena.setOnClickListener { launchTraining("Français") }
        binding.btnPoseidon.setOnClickListener { launchTraining("SVT") }
        binding.btnAres.setOnClickListener { launchTraining("Histoire") }
        binding.btnAphrodite.setOnClickListener { launchTraining("Art/Musique") }
        binding.btnHermes.setOnClickListener { launchTraining("Langues") }
        binding.btnDemeter.setOnClickListener { launchTraining("Géographie") }
        binding.btnHephaestus.setOnClickListener { launchTraining("Physique-Chimie") }
        binding.btnApollon.setOnClickListener { launchTraining("Philo/SES") }
        binding.btnPrometheus.setOnClickListener { launchTraining("Vie & Projets") }

        binding.btnEpreuveUltime.setOnClickListener { launchEpreuveUltime() }
        binding.btnBack.setOnClickListener { finish() }
    }

    // ═══════════════════════════════════════════════════════════════
    // SÉLECTION D'UNE MATIÈRE
    // ═══════════════════════════════════════════════════════════════

    private fun launchTraining(matiereCible: String) {
        if (isPreparingTrainingSelection) {
            return
        }

        isPreparingTrainingSelection = true
        trainingSelectionJob?.cancel()

        showTrainingDivineLoadingDialog(godName = PantheonConfig.findByMatiere(matiereCible)?.divinite ?: "Zeus")

        trainingSelectionJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@TrainingSelectActivity)
                val tousLesCours = db.iAristoteDao().getAllCourses()
                val coursDeMatiere = tousLesCours.filter { correspondAMatiere(it.subject, matiereCible) }

                val dialogue = try {
                    GodLoreManager.buildTrainingDialogue(matiereCible, coursDeMatiere.size)
                } catch (_: Exception) {
                    when (matiereCible) {
                        "Géographie" -> "Déméter — Ces terres de savoir réclament ton attention, héros. Viens nourrir ce parchemin avant que mon jardin ne se fane. Chaque révision l'arrose et fait refleurir ta mémoire."
                        else -> "${PantheonConfig.findByMatiere(matiereCible)?.divinite ?: "Zeus"} — L'arène t'attend, héros. Choisis maintenant le parchemin que tu veux travailler."
                    }
                }

                withContext(Dispatchers.Main) {
                    appliquerThemeMatiere(matiereCible)
                    hideTrainingDivineLoadingDialog()

                    faireParlerLeDieu(dialogue)

                    if (coursDeMatiere.isEmpty()) {
                        DialogRPGManager.showInfo(
                            activity = this@TrainingSelectActivity,
                            godId = PantheonConfig.findByMatiere(matiereCible)?.divinite?.lowercase() ?: "prometheus",
                            message = "Aucun parchemin n'est encore rangé dans le temple de $matiereCible. Passe d'abord par l'Oracle pour consacrer un savoir.",
                            title = "Temple encore silencieux"
                        )
                    } else {
                        afficherChoixCoursPourMatiere(matiereCible, coursDeMatiere)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideTrainingDivineLoadingDialog()
                    faireParlerLeDieu(
                        "Un souffle divin a brouillé l'arène. Réessaie dans un instant."
                    )
                    DialogRPGManager.showTechnicalError(
                        activity = this@TrainingSelectActivity,
                        errorType = TechnicalErrorType.GEMINI_API_ERROR
                    )
                    Log.e("REVIZEUS_TRAINING", "Erreur préparation matière : ${e.message}", e)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isPreparingTrainingSelection = false
                    hideTrainingDivineLoadingDialog()
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ÉPREUVE ULTIME (multi-cours ou Panthéon complet)
    // ═══════════════════════════════════════════════════════════════

    private fun launchEpreuveUltime() {
        appliquerThemeMatiere("Mathématiques", specialBgm = R.raw.bgm_training_quiz)

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@TrainingSelectActivity)
            val tousLesCours = db.iAristoteDao().getAllCourses()

            withContext(Dispatchers.Main) {
                hideTrainingDivineLoadingDialog()
                if (tousLesCours.isEmpty()) {
                    faireParlerLeDieu("La bibliothèque est vide, héros. Reviens après tes révisions.")
                    return@withContext
                }

                faireParlerLeDieu(GodLoreManager.buildUltimeDialogue(totalCours = tousLesCours.size))

                val options = mutableListOf<String>()
                options.add("⚡ Épreuve Totale — Toutes les matières")

                PantheonConfig.GODS.forEach { god ->
                    val count = compterCoursPourMatiere(tousLesCours, god.matiere)
                    if (count > 0) options.add("${god.divinite} — ${god.matiere} ($count)")
                }

                showRpgChoiceDialog(
                    title = "Choisir l'Épreuve Ultime",
                    subtitle = "Sélectionne la voie suprême que l'Olympe va forger pour toi.",
                    options = options
                ) { which ->
                    vibrerAppareil()
                    if (which == 0) {
                        ouvrirChoixFormatUltime(
                            cours = tousLesCours,
                            matiere = "Panthéon",
                            divinite = "Zeus",
                            mode = "ULTIME_GLOBAL"
                        )
                    } else {
                        val validGods = PantheonConfig.GODS.filter { g ->
                            tousLesCours.any { c -> correspondAMatiere(c.subject, g.matiere) }
                        }
                        val selectedGod = validGods[which - 1]
                        val coursDeMatiere = tousLesCours.filter {
                            correspondAMatiere(it.subject, selectedGod.matiere)
                        }
                        ouvrirChoixFormatUltime(
                            cours = coursDeMatiere,
                            matiere = selectedGod.matiere,
                            divinite = selectedGod.divinite,
                            mode = "ULTIME_MATIERE"
                        )
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CORRECTION v10 — GÉNÉRATION QUIZ VIA GEMINI À CHAQUE SESSION
    // ═══════════════════════════════════════════════════════════════

    /**
     * BLOC 1C :
     * Annote les questions générées sans casser le parsing existant.
     *
     * Objectifs métier :
     * - SINGLE_COURSE / ULTIME_MATIERE : matière connue directement
     * - ULTIME_GLOBAL : chaque question doit sortir avec une vraie matière source,
     *   pas avec "Panthéon"
     * - courseId : préservé quand il existe déjà
     * - difficulty : préservée au minimum à 1
     */
    private fun annotateGeneratedQuestions(
        questions: List<QuizQuestion>,
        cours: List<CourseEntry>,
        mode: String,
        fallbackMatiere: String,
        fallbackCourseId: String = ""
    ): List<QuizQuestion> {
        val distinctSubjects = cours
            .map { canonicalizeQuizSubject(it.subject) }
            .filter { it.isNotBlank() && it != "Panthéon" }
            .distinct()

        val safeFallbackSubject = canonicalizeQuizSubject(fallbackMatiere)
            .let { canonical ->
                if (canonical.isBlank() || canonical == "Panthéon") {
                    distinctSubjects.firstOrNull() ?: "Mathématiques"
                } else {
                    canonical
                }
            }

        val annotated = questions.map { question ->
            val resolvedSubject = when {
                mode == "ULTIME_GLOBAL" -> {
                    val inferred = inferSubjectFromQuestion(question, distinctSubjects)
                    canonicalizeQuizSubject(inferred).let { canonical ->
                        if (canonical.isBlank() || canonical == "Panthéon") {
                            distinctSubjects.firstOrNull() ?: "Mathématiques"
                        } else {
                            canonical
                        }
                    }
                }
                else -> {
                    val explicitSubject = canonicalizeQuizSubject(question.subject)
                    if (explicitSubject.isBlank() || explicitSubject == "Panthéon") {
                        safeFallbackSubject
                    } else {
                        explicitSubject
                    }
                }
            }

            val inferredCourseId = when {
                fallbackCourseId.isNotBlank() -> fallbackCourseId
                cours.size == 1 -> cours.first().id
                else -> ""
            }

            question.copy(
                subject = resolvedSubject,
                courseId = inferredCourseId.ifBlank { question.courseId },
                difficulty = question.difficulty.coerceAtLeast(1)
            )
        }

        return when (mode) {
            "ULTIME_GLOBAL", "ULTIME_MATIERE" -> annotated.shuffled().take(40)
            else -> annotated.shuffled().take(30)
        }
    }

    /**
     * BLOC 1C :
     * Tente d'inférer la matière réelle d'une question ultime globale
     * à partir de son texte et de ses options.
     */
    private fun inferSubjectFromQuestion(question: QuizQuestion, availableSubjects: List<String>): String {
        if (availableSubjects.isEmpty()) return "Mathématiques"

        val haystack = listOf(question.text, question.optionA, question.optionB, question.optionC)
            .joinToString(" ")
            .lowercase()

        fun scoreFor(subject: String): Int {
            val keywords = when (canonicalizeQuizSubject(subject)) {
                "Mathématiques" -> listOf("équation", "calcul", "fonction", "fraction", "géométr", "théorème", "angle")
                "Français" -> listOf("grammaire", "verbe", "sujet", "participe", "conjug", "phrase", "orthographe")
                "SVT" -> listOf("cellule", "adn", "photosynth", "organisme", "gène", "espèce", "respiration")
                "Histoire" -> listOf("guerre", "révolution", "roi", "siècle", "traité", "empire", "bataille")
                "Physique-Chimie" -> listOf("atome", "molécule", "force", "énergie", "tension", "réaction", "électron")
                "Géographie" -> listOf("climat", "population", "territoire", "carte", "région", "flux", "urbanisation")
                "Art/Musique" -> listOf("œuvre", "peinture", "musique", "rythme", "couleur", "partition", "mélodie")
                "Langues" -> listOf("english", "grammar", "vocabulary", "verb", "present", "past", "future")
                "Philo/SES" -> listOf("liberté", "justice", "économie", "marché", "sociologie", "raisonnement", "thèse")
                "Vie & Projets" -> listOf("projet", "orientation", "métier", "formation", "compétence", "avenir")
                else -> emptyList()
            }
            return keywords.count { haystack.contains(it.lowercase()) }
        }

        val best = availableSubjects.maxByOrNull { scoreFor(it) }
        return if (best.isNullOrBlank()) {
            availableSubjects.first()
        } else {
            best
        }
    }

    /**
     * BLOC 1C :
     * Canonicalise les matières pour éviter les divergences de scoring
     * entre génération, tracking et récompenses.
     */
    private fun canonicalizeQuizSubject(subject: String): String {
        val raw = subject.trim()
        if (raw.isBlank()) return ""
        return when (raw.lowercase()) {
            "mathématiques", "mathematiques", "maths" -> "Mathématiques"
            "français", "francais" -> "Français"
            "svt" -> "SVT"
            "histoire" -> "Histoire"
            "art", "art/musique", "art / musique", "musique" -> "Art/Musique"
            "langues", "anglais", "english" -> "Langues"
            "géographie", "geographie" -> "Géographie"
            "physique-chimie", "physique / chimie", "physique", "chimie" -> "Physique-Chimie"
            "philo/ses", "philo / ses", "philosophie", "ses" -> "Philo/SES"
            "vie & projets", "vie et projets", "projets", "orientation" -> "Vie & Projets"
            "panthéon", "pantheon" -> "Panthéon"
            else -> raw
        }
    }

    /**
     * CORRECTION v10 + BLOC 2C — Lance un entraînement normal sur un savoir unique.
     *
     * ÉVOLUTION BLOC 2C :
     * - Utilise NormalTrainingBuilder pour générer 30 questions de qualité
     * - Fait plusieurs appels Gemini si nécessaire
     * - Filtre les doublons et répétitions
     * - Session d'approfondissement profonde sur un savoir précis
     *
     * CONSERVATION :
     * - Flux UI / BGM / navigation conservés à 100%
     * - Fallback ancien système si NormalTrainingBuilder échoue
     * - Compatibilité totale avec TrainingQuizActivity
     */
    private fun lancerEntrainementSurCoursUnique(matiereCible: String, course: CourseEntry) {
        generationJob?.cancel()

        val god = PantheonConfig.findByMatiere(matiereCible)
        val nomDieu = god?.divinite ?: "Zeus"

        appliquerThemeMatiere(matiereCible, specialBgm = R.raw.bgm_training_quiz)
        faireParlerLeDieu("$nomDieu consulte les astres pour forger ton épreuve… Patiente, héros.")

        val prefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        val age = prefs.getInt("USER_AGE", 15)
        val classe = prefs.getString("USER_CLASS", "3ème") ?: "3ème"
        val mood = prefs.getString("CURRENT_MOOD", "Neutre") ?: "Neutre"
        val examFormat = prefs.getString("TRAINING_EXAM_FORMAT", "CLASSIC") ?: "CLASSIC"

        generationJob = lifecycleScope.launch {
            showTrainingDivineLoadingDialog(godName = nomDieu)

            try {
                // BLOC 2C — APPROFONDISSEMENT 30 QUESTIONS
                val questions = withContext(Dispatchers.IO) {
                    com.revizeus.app.core.NormalTrainingBuilder.buildNormalTraining(
                        context = this@TrainingSelectActivity,
                        course = course,
                        matiere = matiereCible,
                        divinite = nomDieu,
                        ethos = god?.ethos ?: "Sagesse",
                        userAge = age,
                        userClass = classe,
                        userMood = mood
                    )
                }

                if (questions.isEmpty()) {
                    faireParlerLeDieu(
                        "$nomDieu — Le parchemin a résisté à la forge sacrée. " +
                            "Rescanne-le via l'Oracle pour améliorer sa qualité."
                    )
                    return@launch
                }

                if (questions.size < 10) {
                    faireParlerLeDieu(
                        "$nomDieu — Seulement ${questions.size} épreuves forgées. " +
                            "Le savoir est trop court pour un entraînement complet."
                    )
                    // On peut continuer avec moins de questions, mais on informe le joueur
                }

                Log.d("REVIZEUS_TRAINING", "Entraînement normal généré : ${questions.size} questions pour ${course.displayTitle()}")

                prefs.edit()
                    .putString("TRAINING_COURSE_TEXT", course.extractedText)
                    .putString("TRAINING_MODE", "SINGLE_COURSE")
                    .putString("TRAINING_SELECTED_MATIERE", matiereCible)
                    .putString("TRAINING_SELECTED_DIVINITE", nomDieu)
                    .putString("TRAINING_SELECTED_COURSE_ID", course.id)
                    .putString("TRAINING_EXAM_FORMAT", "CLASSIC")
                    .apply()

                QuizActivity.pendingQuestions = ArrayList(questions)
                QuizActivity.currentMatiere = matiereCible

                faireParlerLeDieu("$nomDieu — ${questions.size} épreuves forgées ! Que le combat commence !")

                kotlinx.coroutines.delay(1200L)
                if (!isActive || isFinishing || isDestroyed) return@launch
                hideTrainingDivineLoadingDialog()

                startActivity(Intent(this@TrainingSelectActivity, TrainingQuizActivity::class.java))
            } catch (e: CancellationException) {
                Log.d("REVIZEUS_TRAINING", "Génération entraînement annulée proprement")
                throw e
            } catch (e: Exception) {
                Log.e("REVIZEUS_TRAINING", "Erreur génération quiz : ${e.message}", e)
                faireParlerLeDieu(
                    "$nomDieu — Une tempête divine a interrompu la forge. Réessaie dans un instant."
                )
            } finally {
                if (!isFinishing && !isDestroyed) {
                    hideTrainingDivineLoadingDialog()
                }
            }
        }
    }

    /**
     * CORRECTION v10 + BLOC 2A — Lance l'Épreuve Ultime sur plusieurs cours.
     *
     * ÉVOLUTION BLOC 2A :
     * - Mode ULTIME_GLOBAL : utilise UltimateQuizBuilder pour alternance dynamique 40 questions
     * - Mode ULTIME_MATIERE : conserve l'ancienne logique (agrégation Gemini)
     *
     * CONSERVATION :
     * - Flux UI / BGM / navigation conservés à 100%
     * - Fallback ancien système si UltimateQuizBuilder échoue
     * - Compatibilité totale avec TrainingQuizActivity
     */
    private fun genererEtLancerQuiz(
        cours: List<CourseEntry>,
        matiere: String,
        divinite: String,
        mode: String
    ) {
        generationJob?.cancel()

        appliquerThemeMatiere(matiere, specialBgm = R.raw.bgm_training_quiz)
        faireParlerLeDieu(
            "$divinite — ${cours.size} parchemin${if (cours.size > 1) "s" else ""} entrent " +
                "dans la forge sacrée. L'épreuve ultime se construit…"
        )

        val prefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        val age = prefs.getInt("USER_AGE", 15)
        val classe = prefs.getString("USER_CLASS", "3ème") ?: "3ème"
        val mood = prefs.getString("CURRENT_MOOD", "Neutre") ?: "Neutre"
        val examFormat = prefs.getString("TRAINING_EXAM_FORMAT", "CLASSIC") ?: "CLASSIC"

        generationJob = lifecycleScope.launch {
            showTrainingDivineLoadingDialog(godName = divinite)

            try {
                // BLOC 2A — ALTERNANCE DYNAMIQUE POUR ULTIME_GLOBAL
                if (mode == "ULTIME_GLOBAL") {
                    val questions = withContext(Dispatchers.IO) {
                        com.revizeus.app.core.UltimateQuizBuilder.buildUltimateQuiz(
                            context = this@TrainingSelectActivity,
                            userAge = age
                        )
                    }

                    if (questions.isEmpty()) {
                        faireParlerLeDieu(
                            "$divinite — Les parchemins de l'Olympe sont insuffisants. " +
                                "Scanne davantage de savoirs pour forger une épreuve digne des dieux !"
                        )
                        return@launch
                    }

                    if (questions.size < 40) {
                        faireParlerLeDieu(
                            "$divinite — Seulement ${questions.size} épreuves forgées. " +
                                "Continue d'alimenter les temples pour atteindre les 40 épreuves titanesques !"
                        )
                        // On peut continuer avec moins de questions, mais on informe le joueur
                    }

                    Log.d("REVIZEUS_TRAINING", "Quiz Ultime Global généré : ${questions.size} questions alternées")

                    prefs.edit()
                        .putString("TRAINING_MODE", mode)
                        .putString("TRAINING_SELECTED_MATIERE", "Panthéon")
                        .putString("TRAINING_SELECTED_DIVINITE", divinite)
                        .putString("TRAINING_EXAM_FORMAT", examFormat)
                        .apply()

                    QuizActivity.pendingQuestions = ArrayList(questions)
                    QuizActivity.currentMatiere = "Panthéon"

                    faireParlerLeDieu(
                        "$divinite — ${questions.size} épreuves titanesques alternées forgées ! " +
                            "L'Olympe attend son champion !"
                    )

                    kotlinx.coroutines.delay(1400L)
                    if (!isActive || isFinishing || isDestroyed) return@launch
                    hideTrainingDivineLoadingDialog()

                    startActivity(Intent(this@TrainingSelectActivity, TrainingQuizActivity::class.java))
                    return@launch
                }

                // ANCIEN SYSTÈME — ULTIME_MATIERE et autres modes
                val maxCharsParCours = 1200
                val texteAggrege = buildString {
                    append("FORMAT_EXAMEN=")
                    append(examFormat)
                    append("\n\n")
                    append(cours.joinToString("\n\n---\n\n") { course ->
                    val header = "### ${course.title.ifBlank { course.subject }}"
                    val contenu = course.extractedText.take(maxCharsParCours)
                    "$header\n$contenu"
                })
                }

                val raw = withContext(Dispatchers.IO) {
                    GeminiManager.genererContenuOracle(
                        texte = texteAggrege,
                        age = age,
                        classe = classe,
                        matiere = matiere,
                        divinite = divinite,
                        ethos = "Omniscience",
                        mood = mood
                    )
                }

                if (raw == null) {
                    faireParlerLeDieu(
                        "$divinite — L'Olympe est silencieux (erreur réseau). " +
                            "Vérifie ta connexion et réessaie."
                    )
                    return@launch
                }

                val data = IAristoteEngine.decoderReponse(raw)

                if (data == null || data.second.isEmpty()) {
                    faireParlerLeDieu(
                        "$divinite — Les parchemins ont résisté à la forge sacrée. " +
                            "Rescanne tes cours via l'Oracle pour améliorer leur qualité."
                    )
                    return@launch
                }

                val questions = annotateGeneratedQuestions(
                    questions = data.second,
                    cours = cours,
                    mode = mode,
                    fallbackMatiere = matiere
                )
                Log.d("REVIZEUS_TRAINING", "Épreuve Ultime générée : ${questions.size} questions, mode=$mode")

                prefs.edit()
                    .putString("TRAINING_COURSE_TEXT", texteAggrege)
                    .putString("TRAINING_MODE", mode)
                    .putString("TRAINING_SELECTED_MATIERE", matiere)
                    .putString("TRAINING_SELECTED_DIVINITE", divinite)
                    .apply()

                QuizActivity.pendingQuestions = ArrayList(questions)
                QuizActivity.currentMatiere = matiere

                faireParlerLeDieu(
                    "$divinite — ${questions.size} épreuves titanesques forgées ! " +
                        "L'Olympe attend son champion !"
                )

                kotlinx.coroutines.delay(1400L)
                if (!isActive || isFinishing || isDestroyed) return@launch
                hideTrainingDivineLoadingDialog()

                startActivity(Intent(this@TrainingSelectActivity, TrainingQuizActivity::class.java))
            } catch (e: CancellationException) {
                Log.d("REVIZEUS_TRAINING", "Génération épreuve ultime annulée proprement")
                throw e
            } catch (e: Exception) {
                Log.e("REVIZEUS_TRAINING", "Erreur génération épreuve ultime : ${e.message}", e)
                faireParlerLeDieu(
                    "$divinite — Une tempête cosmique a brisé la forge. Réessaie dans un instant."
                )
            } finally {
                if (!isFinishing && !isDestroyed) {
                    hideTrainingDivineLoadingDialog()
                }
            }
        }
    }



    /**
     * Nouveau choix demandé pour l'entraînement normal :
     * - quiz avec timer
     * - quiz sans timer (récompenses divisées par deux côté résultat)
     */
    private fun ouvrirChoixModeEntrainement(matiereCible: String, course: CourseEntry) {
        val labels = arrayOf(
            "⚡ Lancer le quiz avec timer",
            "🧘 Lancer le quiz sans timer (-50% récompenses)"
        )

        showRpgChoiceDialog(
            title = "Choisir le mode d'entraînement",
            subtitle = course.displayTitle(),
            options = labels.toList()
        ) { which ->
            val isTimedMode = which == 0
            getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("TRAINING_IS_TIMED_MODE", isTimedMode)
                .putString("TRAINING_EXAM_FORMAT", "CLASSIC")
                .apply()
            lancerEntrainementSurCoursUnique(matiereCible, course)
        }
    }

    /**
     * Nouveau choix demandé pour l'entraînement ultime :
     * - format Brevet
     * - format Bac
     * puis timer / sans timer
     */
    private fun ouvrirChoixFormatUltime(
        cours: List<CourseEntry>,
        matiere: String,
        divinite: String,
        mode: String
    ) {
        val labels = arrayOf(
            "📘 Format Brevet",
            "🎓 Format Bac"
        )

        showRpgChoiceDialog(
            title = "Choisir le format de l'Épreuve Ultime",
            subtitle = "$divinite prépare la structure de cette épreuve.",
            options = labels.toList()
        ) { which ->
            val examFormat = if (which == 0) "BREVET" else "BAC"
            ouvrirChoixTimerUltime(
                cours = cours,
                matiere = matiere,
                divinite = divinite,
                mode = mode,
                examFormat = examFormat
            )
        }
    }

    private fun ouvrirChoixTimerUltime(
        cours: List<CourseEntry>,
        matiere: String,
        divinite: String,
        mode: String,
        examFormat: String
    ) {
        val labels = arrayOf(
            "⚡ Lancer l'épreuve avec timer",
            "🧘 Lancer l'épreuve sans timer (-50% récompenses)"
        )

        showRpgChoiceDialog(
            title = "Choisir le rythme de l'Épreuve Ultime",
            subtitle = "Les récompenses divines restent plus généreuses quand le sablier est actif.",
            options = labels.toList()
        ) { which ->
            val isTimedMode = which == 0
            getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("TRAINING_IS_TIMED_MODE", isTimedMode)
                .putString("TRAINING_EXAM_FORMAT", examFormat)
                .apply()

            genererEtLancerQuiz(
                cours = cours,
                matiere = matiere,
                divinite = divinite,
                mode = mode
            )
        }
    }



    private fun lockTrainingInteractions() {
        try {
            val root = binding.rootView as? ViewGroup ?: return
            val shield = interactionShieldView ?: View(this).apply {
                setBackgroundColor(Color.parseColor("#6605050A"))
                isClickable = true
                isFocusable = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }.also { interactionShieldView = it }
            if (shield.parent == null) {
                root.addView(shield)
            } else {
                shield.bringToFront()
            }
        } catch (_: Exception) {
        }
    }

    private fun unlockTrainingInteractions() {
        try {
            val shield = interactionShieldView ?: return
            (shield.parent as? ViewGroup)?.removeView(shield)
        } catch (_: Exception) {
        }
    }

    private fun showRpgChoiceDialog(
        title: String,
        subtitle: String = "",
        options: List<String>,
        negativeLabel: String = "Annuler",
        onSelected: (Int) -> Unit
    ) {
        hideTrainingDivineLoadingDialog()
        unlockTrainingInteractions()

        val overlayRoot = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#2205050A"))
            isClickable = true
            isFocusable = true
        }

        val centerWrapper = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.BOTTOM
            ).apply {
                leftMargin = dp(18)
                rightMargin = dp(18)
                topMargin = dp(24)
                bottomMargin = dp(18)
            }
        }

        val cardRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(14))
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#E61A1A2E"))
            }
            elevation = dp(10).toFloat()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
            translationY = dp(64).toFloat()
            alpha = 0f
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val godPortrait = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(58), dp(58)).apply {
                marginEnd = dp(12)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            try {
                setImageResource(getMatiereAvatarRes(matiereThemeActive))
            } catch (_: Exception) {
                setImageResource(R.drawable.avatar_zeus_dialog)
            }
        }
        headerRow.addView(godPortrait)

        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleView = TextView(this).apply {
            text = title
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) resources.getFont(R.font.cinzel) else Typeface.DEFAULT_BOLD
            } catch (_: Exception) {
                Typeface.DEFAULT_BOLD
            }
        }
        titleColumn.addView(titleView)

        val topSubtitle = TextView(this).apply {
            text = PantheonConfig.findByMatiere(matiereThemeActive)?.divinite ?: "Zeus"
            setTextColor(Color.parseColor("#B8A56A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(2), 0, 0)
        }
        titleColumn.addView(topSubtitle)
        headerRow.addView(titleColumn)
        cardRoot.addView(headerRow)

        val separator = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(10)
            }
            setBackgroundColor(Color.parseColor("#55FFD700"))
        }
        cardRoot.addView(separator)

        if (subtitle.isNotBlank()) {
            val subtitleView = TextView(this).apply {
                text = subtitle
                setTextColor(Color.parseColor("#F2F2F2"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                gravity = Gravity.CENTER_HORIZONTAL
                setLineSpacing(dp(3).toFloat(), 1f)
                setPadding(dp(6), 0, dp(6), dp(12))
            }
            cardRoot.addView(subtitleView)
        }

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            isVerticalScrollBarEnabled = false
        }

        val optionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        var dialogRef: AlertDialog? = null

        options.forEachIndexed { index, label ->
            val optionFrame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(58)
                ).apply {
                    bottomMargin = dp(9)
                }
                isClickable = true
                isFocusable = true
                foregroundGravity = Gravity.CENTER
            }

            val backgroundImage = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_XY
                try {
                    setImageResource(R.drawable.bg_temple_button)
                } catch (_: Exception) {
                    setBackgroundColor(Color.parseColor("#C7A84B"))
                }
            }
            optionFrame.addView(backgroundImage)

            val overlayImage = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_XY
                alpha = 0.24f
                try {
                    setImageResource(R.drawable.bg_textelayout)
                } catch (_: Exception) {
                }
            }
            optionFrame.addView(overlayImage)

            val contentRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setPadding(dp(14), dp(10), dp(14), dp(10))
            }

            val badge = TextView(this).apply {
                text = (index + 1).toString()
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#2B1A00"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).apply {
                    marginEnd = dp(10)
                }
                try {
                    setBackgroundResource(R.drawable.badge_count)
                } catch (_: Exception) {
                    setBackgroundColor(Color.parseColor("#FFF0A8"))
                }
            }
            contentRow.addView(badge)

            val optionText = TextView(this).apply {
                text = label
                setTextColor(Color.parseColor("#241400"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                typeface = Typeface.DEFAULT_BOLD
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            contentRow.addView(optionText)

            val arrow = TextView(this).apply {
                text = "›"
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#5F3C00"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dp(8)
                }
            }
            contentRow.addView(arrow)

            optionFrame.addView(contentRow)
            optionsContainer.addView(optionFrame)

            optionFrame.setOnClickListener {
                try {
                    SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
                } catch (_: Exception) {
                }
                vibrerAppareil()
                try {
                    dialogRef?.dismiss()
                } catch (_: Exception) {
                }
                onSelected(index)
            }
        }

        scroll.addView(optionsContainer)
        cardRoot.addView(scroll)

        val cancelFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
            isClickable = true
            isFocusable = true
        }

        val cancelBackground = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_XY
            try {
                setImageResource(R.drawable.bg_temple_button_inset)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#33283C"))
            }
        }
        cancelFrame.addView(cancelBackground)

        val cancelText = TextView(this).apply {
            text = negativeLabel.uppercase(Locale.getDefault())
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#EAD7A2"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            typeface = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) resources.getFont(R.font.cinzel) else Typeface.DEFAULT_BOLD
            } catch (_: Exception) {
                Typeface.DEFAULT_BOLD
            }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        cancelFrame.addView(cancelText)
        cardRoot.addView(cancelFrame)

        centerWrapper.addView(cardRoot)
        overlayRoot.addView(centerWrapper)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(overlayRoot)
            .setCancelable(true)
            .create()

        dialogRef = dialog

        overlayRoot.setOnClickListener {
            try {
                SoundManager.playSFXLow(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
            dialog.dismiss()
        }

        centerWrapper.setOnClickListener {
        }

        cancelFrame.setOnClickListener {
            try {
                SoundManager.playSFXLow(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            try {
                dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                dialog.window?.setDimAmount(0.12f)
            } catch (_: Exception) {
            }
        }

        dialog.show()

        cardRoot.post {
            try {
                cardRoot.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(260L)
                    .start()
            } catch (_: Exception) {
            }
        }
    }

    private fun showCourseSelectionRpgDialog(matiereCible: String, coursDeMatiere: List<CourseEntry>) {
        val labels = coursDeMatiere.mapIndexed { index, course -> construireLabelCours(index, course) }
        showRpgChoiceDialog(
            title = "Choisir un parchemin — $matiereCible",
            subtitle = "Appuie pour lancer l'entraînement. Maintiens pour consulter toutes les voies possibles de ce savoir.",
            options = labels
        ) { which ->
            ouvrirChoixModeEntrainement(matiereCible, coursDeMatiere[which])
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES — CONSERVATION INTÉGRALE
    // ═══════════════════════════════════════════════════════════════

    private fun actualiserCompteurs() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@TrainingSelectActivity)
                val tousLesCours = db.iAristoteDao().getAllCourses()
                withContext(Dispatchers.Main) {
                    binding.tvCountZeus.text = compterCoursPourMatiere(tousLesCours, "Mathématiques").toString()
                    binding.tvCountAthena.text = compterCoursPourMatiere(tousLesCours, "Français").toString()
                    binding.tvCountPoseidon.text = compterCoursPourMatiere(tousLesCours, "SVT").toString()
                    binding.tvCountAres.text = compterCoursPourMatiere(tousLesCours, "Histoire").toString()
                    binding.tvCountAphrodite.text = compterCoursPourMatiere(tousLesCours, "Art/Musique").toString()
                    binding.tvCountHermes.text = compterCoursPourMatiere(tousLesCours, "Langues").toString()
                    binding.tvCountDemeter.text = compterCoursPourMatiere(tousLesCours, "Géographie").toString()
                    binding.tvCountHephaestus.text = compterCoursPourMatiere(tousLesCours, "Physique-Chimie").toString()
                    binding.tvCountApollon.text = compterCoursPourMatiere(tousLesCours, "Philo/SES").toString()
                    binding.tvCountPrometheus.text = compterCoursPourMatiere(tousLesCours, "Vie & Projets").toString()
                    binding.tvCountUltime.text = tousLesCours.size.toString()
                }
            } catch (_: Exception) {}
        }
    }

    private fun afficherChoixCoursPourMatiere(matiereCible: String, coursDeMatiere: List<CourseEntry>) {
        val labels = coursDeMatiere
            .mapIndexed { index, course -> construireLabelCours(index, course) }
            .toTypedArray()

        showCourseSelectionRpgDialog(matiereCible, coursDeMatiere)
    }

    private fun compterCoursPourMatiere(tousLesCours: List<CourseEntry>, matiere: String): Int {
        return tousLesCours.count { correspondAMatiere(it.subject, matiere) }
    }

    private fun correspondAMatiere(subject: String, matiereCible: String): Boolean {
        val s = subject.trim().lowercase()
        val m = matiereCible.trim().lowercase()
        if (s == m) return true
        if ((m == "art/musique" || m == "art") && (s == "art" || s == "art/musique")) return true
        if ((m == "langues" || m == "anglais") && (s == "anglais" || s == "langues")) return true
        return false
    }

    private fun construireLabelCours(index: Int, course: CourseEntry): String {
        val date = try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(course.dateAdded))
        } catch (_: Exception) {
            "date"
        }
        val apercu = course.extractedText
            .replace("\n", " ")
            .trim()
            .take(70)
            .let { if (it.length >= 70) "$it…" else it }
        return "Parchemin ${index + 1} • $date\n$apercu"
    }


    private fun getRawResByName(resName: String): Int {
        return resources.getIdentifier(resName, "raw", packageName)
    }

    private fun vibrerAppareil() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    private fun installerAmbianceOlympienne() {
        val root = binding.rootView as? ViewGroup ?: return
        olympianParticlesView = OlympianParticlesView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = false
            isFocusable = false
        }
        root.addView(olympianParticlesView, 0)
        animatedBackgroundHelper = AnimatedBackgroundHelper(
            targetView = binding.rootView,
            particlesView = olympianParticlesView,
            backgroundImageView = binding.ivTrainingBackground
        )
        animatedBackgroundHelper?.configurePremiumBackground(
            staticDrawableRes = getMatiereBackgroundRes(matiereThemeActive),
            videoRawRes = getRawResByName("creation_quiz"),
            imageAlpha = 0.25f,
            loopVideo = true,
            videoVolume = 0.50f
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════


    private fun showTrainingDivineLoadingDialog(godName: String? = null) {
        try {
            if (isFinishing || isDestroyed) return
            lockTrainingInteractions()
            val tag = "loading_divine_training"
            val existing = supportFragmentManager.findFragmentByTag(tag)
            if (existing is LoadingDivineDialog) {
                divineTrainingLoadingDialog = existing
                return
            }
            val dialog = LoadingDivineDialog.newQuizInstance(godName)
            dialog.isCancelable = false
            divineTrainingLoadingDialog = dialog
            dialog.show(supportFragmentManager, tag)
        } catch (e: Exception) {
            Log.e("REVIZEUS_TRAINING", "Impossible d'afficher le loader divin d'entraînement : ${e.message}", e)
        }
    }

    private fun hideTrainingDivineLoadingDialog() {
        try {
            divineTrainingLoadingDialog?.dismissAllowingStateLoss()
            val existing = supportFragmentManager.findFragmentByTag("loading_divine_training")
            if (existing is LoadingDivineDialog) {
                existing.dismissAllowingStateLoss()
            }
        } catch (e: Exception) {
            Log.e("REVIZEUS_TRAINING", "Impossible de fermer le loader divin d'entraînement : ${e.message}", e)
        } finally {
            divineTrainingLoadingDialog = null
            unlockTrainingInteractions()
        }
    }

    override fun onResume() {
        super.onResume()
        actualiserCompteurs()
        val godProfile = PantheonConfig.findByMatiere(matiereThemeActive)
            ?: PantheonConfig.findByDivinite("Zeus")
            ?: PantheonConfig.GODS.first()
        animatedBackgroundHelper?.start(
            accentColor = godProfile.couleur,
            mode = OlympianParticlesView.ParticleMode.TRAINING
        )
        try {
            SoundManager.rememberMusic(currentTrainingBgmResId)
            if (!SoundManager.isPlayingMusic() || SoundManager.getCurrentMusicResId() != currentTrainingBgmResId) {
                SoundManager.playMusicDelayed(this, currentTrainingBgmResId, 120L)
            }
        } catch (_: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        typewriterJob?.cancel()
        godAnim.stopSpeaking(binding.imgGodChibi)
        animatedBackgroundHelper?.stop()
        generationJob?.cancel()
        trainingSelectionJob?.cancel()
        isPreparingTrainingSelection = false
        hideTrainingDivineLoadingDialog()
        unlockTrainingInteractions()
    }

    override fun onDestroy() {
        animatedBackgroundHelper?.release()
        animatedBackgroundHelper = null
        super.onDestroy()
        typewriterJob?.cancel()
        generationJob?.cancel()
        trainingSelectionJob?.cancel()
        godAnim.release(binding.imgGodChibi)
        isPreparingTrainingSelection = false
        hideTrainingDivineLoadingDialog()
        unlockTrainingInteractions()
    }


    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

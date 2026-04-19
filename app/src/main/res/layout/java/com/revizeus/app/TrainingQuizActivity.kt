package com.revizeus.app

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.core.QuizTimerManager
import com.revizeus.app.core.XpCalculator
import com.revizeus.app.databinding.ActivityQuizBinding
import com.revizeus.app.models.QuizQuestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.ImageView

/**
 * TrainingQuizActivity — Mode Entraînement
 *
 * CORRECTIONS TTS :
 * - Icône speaker placée à côté du compteur de question
 * - Icône speaker placée au bout de chacun des 3 choix
 * - Un tap pendant la lecture coupe immédiatement la voix
 * - L'icône n'est plus zoomée : vrai FIT_CENTER + padding interne
 * - La BGM est baissée pendant la voix puis restaurée automatiquement
 *
 * CONSERVATION TOTALE :
 * Toute la mécanique entraînement (BGM adaptée au mode, SFX,
 * tracking ML, typewriter, navigation QuizResultActivity) est conservée.
 *
 * BLOC 1A :
 * - Training et Ultime restent chronométrés
 *
 * BLOC 1B :
 * - Le résultat reçoit IS_TIMED_MODE = true
 * - L'XP est calculée ici pour les modes chronométrés
 */
class TrainingQuizActivity : BaseActivity() {

    private lateinit var binding: ActivityQuizBinding
    private var questions = listOf<QuizQuestion>()
    private var currentIndex = 0
    private var score = 0
    private var answerLocked = false
    private var userAnswersList = ArrayList<String>()

    private val godAnim = GodSpeechAnimator()
    private var typewriterJob: Job? = null
    private var currentMatiere: String = "Mathématiques"

    // ── TTS ───────────────────────────────────────────────────────────
    private val tts: SpeakerTtsHelper by lazy { SpeakerTtsHelper(this) }

    private var btnProgressTts: ImageButton? = null
    private var btnOptATts: ImageButton? = null
    private var btnOptBTts: ImageButton? = null
    private var btnOptCTts: ImageButton? = null

    private val COLOR_CORRECT = 0xFF2E7D32.toInt()
    private val COLOR_WRONG   = 0xFFC62828.toInt()
    private val COLOR_NEUTRAL = 0xFF333333.toInt()

    // ── ML Phase 1 ────────────────────────────────────────────────────
    private var sessionId: String = ""
    private var questionStartTime: Long = 0L

    // ── TIMER QUIZ PREMIUM ────────────────────────────────────────────
    private var countdownTimer: CountDownTimer? = null
    private var userAge: Int = 15
    private var currentQuizIsTimedMode: Boolean = true
    private var hasAnswered = false
    private var hasTimedOut = false
    private var hasPlayedTimerAlert = false

    // BLOC A — mémoire défensive du timer et de la BGM courante.
    // Cela évite les questions figées au retour d’arrière-plan et permet
    // une reprise plus propre sans casser le flow existant.
    private var currentQuizBgmRes: Int = R.raw.music_quiz
    private var maxQuestionSeconds: Int = 0
    private var remainingQuestionSeconds: Int = 0

    /**
     * BLOC BLIP FANTÔME :
     * l'ancienne question ne peut plus continuer à écrire ou sonner après
     * un changement d'écran ou après l'affichage d'une nouvelle question.
     */
    private var typewriterSessionId: Long = 0L

    // Fond premium Quiz Entraînement / Ultime.
    private var animatedBackgroundHelper: AnimatedBackgroundHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        installerFondPremiumQuizTraining()

        val prefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        val trainingMode = prefs.getString("TRAINING_MODE", "SINGLE_COURSE") ?: "SINGLE_COURSE"
        currentQuizIsTimedMode = prefs.getBoolean("TRAINING_IS_TIMED_MODE", true)
        currentQuizBgmRes = if (trainingMode.contains("ULTIME", ignoreCase = true)) {
            R.raw.bgm_training_quiz
        } else {
            R.raw.music_quiz
        }
        try {
            SoundManager.playMusic(this, currentQuizBgmRes)
            SoundManager.rememberMusic(currentQuizBgmRes)
        } catch (e: Exception) {
            Log.w("REVIZEUS_TRAINING_QUIZ", "onCreate: lancement BGM quiz impossible", e)
        }

        try {
            AnalyticsManager.initialize(this)
            sessionId = AnalyticsManager.generateSessionId()
        } catch (e: Exception) {
            Log.w("REVIZEUS_TRAINING_QUIZ", "onCreate: initialisation analytics impossible", e)
        }

        currentMatiere = prefs.getString("TRAINING_SELECTED_MATIERE", QuizActivity.currentMatiere)
            ?: QuizActivity.currentMatiere

        loadUserAge()

        questions = QuizActivity.pendingQuestions.filter { it.isUsable() }

        if (questions.isEmpty()) {
            Toast.makeText(this, "Aucune question valide dans ce parchemin !", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        updateDivineInterface()
        installTtsUi()
        displayQuestion()

        binding.btnOptA.setOnClickListener { checkAnswer("A") }
        binding.btnOptB.setOnClickListener { checkAnswer("B") }
        binding.btnOptC.setOnClickListener { checkAnswer("C") }

        binding.btnNext.setOnClickListener {
            currentIndex++
            if (currentIndex < questions.size) displayQuestion() else terminerQuiz()
        }
    }


    private fun installerFondPremiumQuizTraining() {
        animatedBackgroundHelper = AnimatedBackgroundHelper(
            targetView = binding.root,
            backgroundImageView = binding.ivBgQuiz
        )
        animatedBackgroundHelper?.configurePremiumBackground(
            staticDrawableRes = getDrawableResOrFallback("bg_quiz_animated", "bg_olympus_dark"),
            videoRawRes = getRawResByName("bg_quiz_animated"),
            imageAlpha = 0.18f,
            loopVideo = true,
            videoVolume = 0.50f
        )
    }

    private fun appliquerFondPremiumQuizTrainingPourMatiere(matiere: String) {
        val accentColor = PantheonConfig.findByMatiere(matiere)?.couleur
            ?: Color.parseColor("#1E90FF")
        animatedBackgroundHelper?.start(
            accentColor = accentColor,
            mode = OlympianParticlesView.ParticleMode.TRAINING
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

    private fun updateDivineInterface() {
        val godInfo = PantheonConfig.findByMatiere(currentMatiere) ?: PantheonConfig.GODS.first()
        val resId = resources.getIdentifier(godInfo.iconResName, "drawable", packageName)
        if (resId != 0) binding.ivZeusQuiz.setImageResource(resId)
        binding.tvGodName.text = godInfo.divinite.uppercase()
        appliquerFondPremiumQuizTrainingPourMatiere(currentMatiere)
    }

    private fun displayQuestion() {
        answerLocked = false
        hasAnswered = false
        hasTimedOut = false
        hasPlayedTimerAlert = false
        remainingQuestionSeconds = 0
        binding.btnNext.visibility = View.GONE
        val q = questions[currentIndex]
        try {
            binding.tvQuestion.setOnClickListener { revealCurrentQuestionImmediately() }
        } catch (_: Exception) {
        }

        typewriterSessionId += 1L
        val localSessionId = typewriterSessionId

        typewriterJob?.cancel()
        SoundManager.stopAllDialogueBlips()

        binding.tvQuestion.text = q.text

        typewriterJob = lifecycleScope.launch {
            binding.tvQuestion.text = ""
            for (i in q.text.indices) {
                if (localSessionId != typewriterSessionId || !isQuestionDialogueUsable()) break

                binding.tvQuestion.text = q.text.substring(0, i + 1)
                try { SoundManager.playSFXLow(this@TrainingQuizActivity, R.raw.sfx_dialogue_blip) } catch (e: Exception) {
                    Log.w("REVIZEUS_TRAINING_QUIZ", "displayQuestion: blip typewriter indisponible", e)
                }
                delay(30)
            }

            if (localSessionId == typewriterSessionId) {
                binding.tvQuestion.text = q.text
            }
        }

        binding.btnOptA.text = "A) ${q.optionA}"
        binding.btnOptB.text = "B) ${q.optionB}"
        binding.btnOptC.text = "C) ${q.optionC}"

        binding.tvProgress.text = "Question ${currentIndex + 1} / ${questions.size}"
        binding.pbQuestionProgress.max = questions.size
        binding.pbQuestionProgress.progress = currentIndex + 1

        resetButtonColors()
        bindTtsActionsForQuestion(q)

        questionStartTime = System.currentTimeMillis()
        if (currentQuizIsTimedMode) {
            maxQuestionSeconds = QuizTimerManager.getSecondsForAge(userAge)
            remainingQuestionSeconds = maxQuestionSeconds
            startQuestionTimer()
        } else {
            stopQuestionTimer(resetRemaining = true)
            binding.layoutQuestionTimer.visibility = View.GONE
        }
    }

    private fun isQuestionDialogueUsable(): Boolean {
        return !isFinishing &&
            !isDestroyed &&
            lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
            binding.tvQuestion.isAttachedToWindow &&
            binding.tvQuestion.visibility == View.VISIBLE
    }

    private fun revealCurrentQuestionImmediately() {
        val safeQuestion = questions.getOrNull(currentIndex)?.text ?: return
        typewriterSessionId += 1L
        typewriterJob?.cancel()
        SoundManager.stopAllDialogueBlips()
        binding.tvQuestion.text = safeQuestion
    }

    private fun loadUserAge() {
        lifecycleScope.launch {
            try {
                val db = com.revizeus.app.models.AppDatabase.getDatabase(this@TrainingQuizActivity)
                val profile = db.iAristoteDao().getUserProfile()
                userAge = profile?.age ?: 15
            } catch (_: Exception) {
                val prefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
                userAge = prefs.getInt("USER_AGE", 15)
            }
        }
    }

    private fun startQuestionTimer() {
        countdownTimer?.cancel()

        hasTimedOut = false
        hasAnswered = false
        hasPlayedTimerAlert = false

        if (maxQuestionSeconds <= 0) {
            maxQuestionSeconds = QuizTimerManager.getSecondsForAge(userAge)
        }

        if (remainingQuestionSeconds <= 0 || remainingQuestionSeconds > maxQuestionSeconds) {
            remainingQuestionSeconds = maxQuestionSeconds
        }

        val startSeconds = remainingQuestionSeconds.coerceIn(1, maxQuestionSeconds)

        binding.layoutQuestionTimer.visibility = View.VISIBLE
        binding.tvQuestionTimer.text = formatTimerSeconds(startSeconds)
        binding.pbQuestionTimer.max = maxQuestionSeconds
        binding.pbQuestionTimer.progress = startSeconds
        binding.tvQuestionTimer.setTextColor(Color.parseColor("#F7E7AE"))

        countdownTimer = object : CountDownTimer(startSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000L).toInt().coerceAtLeast(0)
                remainingQuestionSeconds = secondsLeft

                binding.tvQuestionTimer.text = formatTimerSeconds(secondsLeft)
                binding.pbQuestionTimer.progress = secondsLeft.coerceIn(0, maxQuestionSeconds)

                if (secondsLeft <= 5 && !hasPlayedTimerAlert && !hasAnswered && !hasTimedOut) {
                    hasPlayedTimerAlert = true
                    try {
                        SoundManager.playSFX(this@TrainingQuizActivity, R.raw.sfx_timer_alert)
                    } catch (_: Exception) {
                    }
                }

                binding.tvQuestionTimer.setTextColor(
                    when {
                        secondsLeft <= 3 -> Color.parseColor("#F44336")
                        secondsLeft <= 6 -> Color.parseColor("#FFB300")
                        else -> Color.parseColor("#F7E7AE")
                    }
                )
            }

            override fun onFinish() {
                if (hasAnswered || hasTimedOut) return
                hasTimedOut = true

                binding.tvQuestionTimer.text = formatTimerSeconds(0)
                remainingQuestionSeconds = 0
                binding.pbQuestionTimer.progress = 0
                binding.tvQuestionTimer.setTextColor(Color.parseColor("#F44336"))

                onQuestionTimedOut()
            }
        }.start()
    }

    private fun stopQuestionTimer(resetRemaining: Boolean = false) {
        countdownTimer?.cancel()
        countdownTimer = null

        if (resetRemaining) {
            remainingQuestionSeconds = 0
            maxQuestionSeconds = 0
        }
    }

    private fun formatTimerSeconds(secondsLeft: Int): String {
        val safe = secondsLeft.coerceAtLeast(0)
        return "00:${safe.toString().padStart(2, '0')}"
    }

    private fun onQuestionTimedOut() {
        if (answerLocked || hasAnswered) return
        answerLocked = true
        hasTimedOut = true
        userAnswersList.add("TIMEOUT")

        val q = questions[currentIndex]
        val correct = q.normalizedCorrectAnswer()
        val responseTime = System.currentTimeMillis() - questionStartTime

        lifecycleScope.launch {
            try {
                AnalyticsManager.trackQuizAnswer(
                    subject = if (q.subject.isNotBlank()) q.subject else currentMatiere,
                    questionText = q.text,
                    userAnswer = "TIMEOUT",
                    correctAnswer = correct,
                    isCorrect = false,
                    responseTime = responseTime,
                    sessionId = sessionId
                )
            } catch (e: Exception) {
                Log.w("REVIZEUS_TRAINING_QUIZ", "onQuestionTimedOut: tracking analytics impossible", e)
            }
        }

        val btnMap = mapOf("A" to binding.btnOptA, "B" to binding.btnOptB, "C" to binding.btnOptC)
        btnMap[correct]?.setBackgroundColor(COLOR_CORRECT)
        try { SoundManager.playSFX(this, R.raw.sfx_error) } catch (e: Exception) {
            Log.w("REVIZEUS_TRAINING_QUIZ", "onQuestionTimedOut: SFX erreur indisponible", e)
        }

        lifecycleScope.launch {
            delay(800L)
            currentIndex++
            if (currentIndex < questions.size) displayQuestion() else terminerQuiz()
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // TTS UI
    // ─────────────────────────────────────────────────────────────────

    private fun installTtsUi() {
        btnProgressTts = installProgressSpeaker()
        btnOptATts = installChoiceSpeaker(binding.btnOptA, "btn_tts_training_opt_a")
        btnOptBTts = installChoiceSpeaker(binding.btnOptB, "btn_tts_training_opt_b")
        btnOptCTts = installChoiceSpeaker(binding.btnOptC, "btn_tts_training_opt_c")
    }

    private fun bindTtsActionsForQuestion(q: QuizQuestion) {
        btnProgressTts?.setOnClickListener {
            tts.speak(
                "Question ${currentIndex + 1} sur ${questions.size}. " +
                    "${q.text}. " +
                    "Option A : ${q.optionA}. " +
                    "Option B : ${q.optionB}. " +
                    "Option C : ${q.optionC}."
            )
        }

        btnOptATts?.setOnClickListener { tts.speak("Option A. ${q.optionA}.") }
        btnOptBTts?.setOnClickListener { tts.speak("Option B. ${q.optionB}.") }
        btnOptCTts?.setOnClickListener { tts.speak("Option C. ${q.optionC}.") }
    }

    private fun installProgressSpeaker(): ImageButton? {
        val tv = binding.tvProgress
        val parent = tv.parent as? ViewGroup ?: return null

        val existing = parent.findViewWithTag<ImageButton>("btn_tts_training_progress")
        if (existing != null) return existing

        return try {
            val index = parent.indexOfChild(tv)
            val originalLp = tv.layoutParams
            parent.removeView(tv)

            val row = LinearLayout(this).apply {
                tag = "layout_training_progress_tts"
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = originalLp
            }

            tv.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val btn = buildSpeakerButton("btn_tts_training_progress").apply {
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).also {
                    it.marginStart = dp(8)
                }
            }

            row.addView(tv)
            row.addView(btn)

            parent.addView(row, index)
            btn
        } catch (_: Exception) {
            null
        }
    }

    private fun installChoiceSpeaker(targetButton: TextView, tag: String): ImageButton? {
        val currentParent = targetButton.parent as? ViewGroup ?: return null
        val existing = currentParent.findViewWithTag<ImageButton>(tag)
        if (existing != null) return existing

        return try {
            val oldLp = targetButton.layoutParams
            val parent = targetButton.parent as? ViewGroup ?: return null
            val index = parent.indexOfChild(targetButton)
            parent.removeView(targetButton)

            val wrapper = FrameLayout(this).apply {
                layoutParams = oldLp
                clipChildren = false
                clipToPadding = false
            }

            targetButton.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

            val icon = buildSpeakerButton(tag).apply {
                layoutParams = FrameLayout.LayoutParams(dp(32), dp(32), Gravity.END or Gravity.CENTER_VERTICAL).also {
                    it.marginEnd = dp(8)
                }
            }

            targetButton.setPadding(
                targetButton.paddingLeft,
                targetButton.paddingTop,
                targetButton.paddingRight + dp(36),
                targetButton.paddingBottom
            )

            wrapper.addView(targetButton)
            wrapper.addView(icon)
            parent.addView(wrapper, index)

            icon
        } catch (_: Exception) {
            null
        }
    }

    private fun buildSpeakerButton(tagValue: String): ImageButton {
        return ImageButton(this).apply {
            tag = tagValue
            background = null
            contentDescription = "Lire à voix haute"
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            alpha = 0.92f
            setPadding(dp(5), dp(5), dp(5), dp(5))
            try {
                setImageResource(R.drawable.ic_speaker_tts)
            } catch (_: Exception) {
                setImageResource(android.R.drawable.ic_btn_speak_now)
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ─────────────────────────────────────────────────────────────────
    // MÉCANIQUE
    // ─────────────────────────────────────────────────────────────────

    private fun checkAnswer(answer: String) {
        if (answerLocked || hasTimedOut) return
        hasAnswered = true
        answerLocked = true
        stopQuestionTimer(resetRemaining = true)
        userAnswersList.add(answer)

        val q = questions[currentIndex]
        val correct = q.normalizedCorrectAnswer()
        val isCorrect = correct == answer
        if (isCorrect) score++

        try {
            if (isCorrect) SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            else SoundManager.playSFX(this, R.raw.sfx_error)
        } catch (e: Exception) {
            Log.w("REVIZEUS_TRAINING_QUIZ", "checkAnswer: SFX réponse indisponible", e)
        }

        val responseTime = System.currentTimeMillis() - questionStartTime
        lifecycleScope.launch {
            try {
                AnalyticsManager.trackQuizAnswer(
                    subject = if (q.subject.isNotBlank()) q.subject else currentMatiere,
                    questionText = q.text,
                    userAnswer = answer,
                    correctAnswer = correct,
                    isCorrect = isCorrect,
                    responseTime = responseTime,
                    sessionId = sessionId
                )
            } catch (e: Exception) {
                Log.w("REVIZEUS_TRAINING_QUIZ", "checkAnswer: tracking analytics impossible", e)
            }
        }

        val btnMap = mapOf("A" to binding.btnOptA, "B" to binding.btnOptB, "C" to binding.btnOptC)
        btnMap[answer]?.setBackgroundColor(if (isCorrect) COLOR_CORRECT else COLOR_WRONG)
        if (!isCorrect) btnMap[correct]?.setBackgroundColor(COLOR_CORRECT)
        binding.btnNext.visibility = View.VISIBLE
    }

    private fun terminerQuiz() {
        val trainingMode = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
            .getString("TRAINING_MODE", "SINGLE_COURSE") ?: "SINGLE_COURSE"

        /**
         * Entraînement normal / ultime :
         * - mode chronométré   -> XP pleine
         * - mode sans timer    -> XP divisée par 2
         */
        val total = questions.size.coerceAtLeast(1)
        val streak = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE).getInt("STREAK", 0)
        val rawXpGained = XpCalculator.calculateQuizXp(score, total, streak)
        val xpGained = if (currentQuizIsTimedMode) rawXpGained else kotlin.math.max(0, kotlin.math.ceil(rawXpGained / 2.0).toInt())

        val intent = Intent(this, QuizResultActivity::class.java).apply {
            putExtra("SCORE", score)
            putExtra("TOTAL", total)
            putExtra("XP_GAINED", xpGained)
            putExtra("MATIERE", currentMatiere)
            putExtra("IS_TIMED_MODE", currentQuizIsTimedMode)
            putExtra("TRAINING_MODE", trainingMode)
            putExtra("QUESTIONS_LIST", ArrayList(questions))
            putExtra("USER_ANSWERS", userAnswersList)
        }
        startActivity(intent)
        finish()
    }

    private fun resetButtonColors() {
        binding.btnOptA.setBackgroundColor(COLOR_NEUTRAL)
        binding.btnOptB.setBackgroundColor(COLOR_NEUTRAL)
        binding.btnOptC.setBackgroundColor(COLOR_NEUTRAL)
    }

    // ─────────────────────────────────────────────────────────────────
    // CYCLE DE VIE
    // ─────────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        appliquerFondPremiumQuizTrainingPourMatiere(currentMatiere)

        // BLOC A — reprise défensive de la BGM du quiz si l'app revient
        // de l'arrière-plan et qu'aucune autre musique n'est déjà active.
        try {
            SoundManager.rememberMusic(currentQuizBgmRes)
            if (!SoundManager.isPlayingMusic()) {
                SoundManager.resumeRememberedMusicDelayed(this, 120L)
            }
        } catch (_: Exception) {
        }

        // BLOC A — reprise du timer restant si la question était encore active.
        if (
            currentQuizIsTimedMode &&
            !answerLocked &&
            !hasTimedOut &&
            currentIndex < questions.size &&
            countdownTimer == null &&
            remainingQuestionSeconds > 0
        ) {
            startQuestionTimer()
        }
    }

    override fun onPause() {
        super.onPause()
        animatedBackgroundHelper?.stop()
        typewriterSessionId += 1L
        typewriterJob?.cancel()
        SoundManager.stopAllDialogueBlips()
        stopQuestionTimer(resetRemaining = false)
        godAnim.stopSpeaking(binding.ivZeusQuiz)
        tts.stop()
    }

    override fun onDestroy() {
        stopQuestionTimer(resetRemaining = false)
        typewriterJob?.cancel()
        SoundManager.stopAllDialogueBlips()
        tts.release()

        try {
            godAnim.release(binding.ivZeusQuiz)
        } catch (_: Exception) {
        }

        animatedBackgroundHelper?.release()
        animatedBackgroundHelper = null

        super.onDestroy()
    }
}

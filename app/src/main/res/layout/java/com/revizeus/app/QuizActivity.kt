package com.revizeus.app

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.core.XpCalculator
import com.revizeus.app.core.QuizTimerManager
import com.revizeus.app.databinding.ActivityQuizBinding
import com.revizeus.app.models.QuizQuestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.ImageView

/**
 * QuizActivity — Oracle Quiz
 *
 * CORRECTIONS TTS :
 * - Icône speaker placée à côté du compteur de question
 * - Icône speaker placée au bout de chacun des 3 choix
 * - Un tap pendant la lecture coupe immédiatement la voix
 * - L'icône n'est plus zoomée : vrai FIT_CENTER + padding interne
 * - La BGM est baissée pendant la voix puis restaurée automatiquement
 *
 * CONSERVATION TOTALE :
 * Toute la mécanique Oracle (questions, score, SFX, tracking ML,
 * typewriter, navigation QuizResultActivity) est conservée.
 *
 * BLOC 1A — CORRECTIF FINAL :
 * - Le quiz Oracle déclenché après l'envoi à l'Oracle n'est PAS chronométré
 * - Le timer visuel XML est donc masqué sur cet écran
 * - Les variables timer sont conservées pour ne rien casser dans l'architecture,
 *   mais elles ne sont pas utilisées ici puisque seul Training / Ultime est chronométré
 *
 * BLOC 1B — CORRECTIF FINAL :
 * - Le résultat reçoit maintenant IS_TIMED_MODE = false
 * - TRAINING_MODE reste "ORACLE"
 */
class QuizActivity : BaseActivity() {

    companion object {
        var pendingQuestions: List<QuizQuestion> = emptyList()
        var currentMatiere: String = "Mathématiques"
        var isTimedMode: Boolean = false
    }

    private lateinit var binding: ActivityQuizBinding
    private var questions = listOf<QuizQuestion>()
    private var currentIndex = 0
    private var score = 0
    private var answerLocked = false
    private var userAnswersList = ArrayList<String>()

    private var typewriterJob: Job? = null
    private var typewriterSessionId: Long = 0L

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
    // Conservé volontairement pour ne pas casser l'architecture ni les imports,
    // même si le quiz Oracle n'utilise plus de chrono.
    private var countdownTimer: CountDownTimer? = null
    private var userAge: Int = 15
    private var currentQuizIsTimedMode: Boolean = false
    private var hasAnswered = false
    private var hasTimedOut = false
    private var hasPlayedTimerAlert = false

    // Fond premium Quiz Oracle.
    private var animatedBackgroundHelper: AnimatedBackgroundHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        installerFondPremiumQuiz()

        try { SoundManager.playMusic(this, R.raw.music_quiz) } catch (_: Exception) {}

        try {
            AnalyticsManager.initialize(this)
            sessionId = AnalyticsManager.generateSessionId()
        } catch (_: Exception) {}

        updateDivineInterface()

        currentQuizIsTimedMode = intent.getBooleanExtra("IS_TIMED_MODE", isTimedMode)
        isTimedMode = currentQuizIsTimedMode

        questions = pendingQuestions.filter { it.isUsable() }
        if (questions.isEmpty()) {
            finish()
            return
        }

        installTtsUi()
        loadUserAge()

        // Oracle peut désormais être lancé avec ou sans timer depuis ResultActivity.
        if (currentQuizIsTimedMode) {
            binding.layoutQuestionTimer.visibility = View.VISIBLE
        } else {
            binding.layoutQuestionTimer.visibility = View.GONE
        }

        displayQuestion()

        binding.btnOptA.setOnClickListener { checkAnswer("A") }
        binding.btnOptB.setOnClickListener { checkAnswer("B") }
        binding.btnOptC.setOnClickListener { checkAnswer("C") }

        binding.btnNext.setOnClickListener {
            currentIndex++
            if (currentIndex < questions.size) displayQuestion() else terminerQuiz()
        }
    }


    private fun installerFondPremiumQuiz() {
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

    private fun appliquerFondPremiumQuizPourMatiere(matiere: String) {
        val accentColor = PantheonConfig.findByMatiere(matiere)?.couleur
            ?: Color.parseColor("#1E90FF")
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

    private fun updateDivineInterface() {
        val godInfo = PantheonConfig.findByMatiere(currentMatiere) ?: PantheonConfig.GODS.first()
        val resId = resources.getIdentifier(godInfo.iconResName, "drawable", packageName)
        if (resId != 0) binding.ivZeusQuiz.setImageResource(resId)
        binding.tvGodName.text = godInfo.divinite.uppercase()
        appliquerFondPremiumQuizPourMatiere(currentMatiere)
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

    private fun displayQuestion() {
        answerLocked = false
        hasAnswered = false
        hasTimedOut = false
        hasPlayedTimerAlert = false
        binding.btnNext.visibility = View.GONE

        val q = questions[currentIndex]
        afficherTexteRPG(q.text)
        try {
            binding.tvQuestion.setOnClickListener { revealCurrentQuestionImmediately() }
        } catch (_: Exception) {
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

        // Oracle peut être chronométré ou non selon le choix post-sauvegarde.
        if (currentQuizIsTimedMode) {
            binding.layoutQuestionTimer.visibility = View.VISIBLE
            startQuestionTimer()
        } else {
            stopQuestionTimer()
            binding.layoutQuestionTimer.visibility = View.GONE
        }
    }

    private fun loadUserAge() {
        lifecycleScope.launch {
            try {
                val db = com.revizeus.app.models.AppDatabase.getDatabase(this@QuizActivity)
                val profile = db.iAristoteDao().getUserProfile()
                userAge = profile?.age ?: 15
            } catch (_: Exception) {
                val prefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
                userAge = prefs.getInt("USER_AGE", 15)
            }
        }
    }

    /**
     * Conservé pour compatibilité d'architecture.
     * Ce moteur n'est plus utilisé dans QuizActivity car Oracle = non chronométré.
     */
    private fun startQuestionTimer() {
        countdownTimer?.cancel()

        hasTimedOut = false
        hasAnswered = false
        hasPlayedTimerAlert = false

        val maxSeconds = QuizTimerManager.getSecondsForAge(userAge)

        binding.layoutQuestionTimer.visibility = View.VISIBLE
        binding.tvQuestionTimer.text = formatTimerSeconds(maxSeconds)
        binding.pbQuestionTimer.max = maxSeconds
        binding.pbQuestionTimer.progress = maxSeconds
        binding.tvQuestionTimer.setTextColor(Color.parseColor("#F7E7AE"))

        countdownTimer = object : CountDownTimer(maxSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000L).toInt().coerceAtLeast(0)

                binding.tvQuestionTimer.text = formatTimerSeconds(secondsLeft)
                binding.pbQuestionTimer.progress = secondsLeft.coerceIn(0, maxSeconds)

                if (secondsLeft <= 5 && !hasPlayedTimerAlert && !hasAnswered && !hasTimedOut) {
                    hasPlayedTimerAlert = true
                    try {
                        SoundManager.playSFX(this@QuizActivity, R.raw.sfx_timer_alert)
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
                binding.pbQuestionTimer.progress = 0
                binding.tvQuestionTimer.setTextColor(Color.parseColor("#F44336"))

                onQuestionTimedOut()
            }
        }.start()
    }

    private fun stopQuestionTimer() {
        countdownTimer?.cancel()
        countdownTimer = null
    }

    private fun formatTimerSeconds(secondsLeft: Int): String {
        val safe = secondsLeft.coerceAtLeast(0)
        return "00:${safe.toString().padStart(2, '0')}"
    }

    /**
     * Conservé pour compatibilité d'architecture.
     * Normalement inutilisé en mode Oracle non chronométré.
     */
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
            } catch (_: Exception) {
            }
        }

        val btnMap = mapOf("A" to binding.btnOptA, "B" to binding.btnOptB, "C" to binding.btnOptC)
        btnMap[correct]?.setBackgroundColor(COLOR_CORRECT)
        try { SoundManager.playSFX(this, R.raw.sfx_error) } catch (_: Exception) {}

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
        btnOptATts = installChoiceSpeaker(binding.btnOptA, "btn_tts_opt_a")
        btnOptBTts = installChoiceSpeaker(binding.btnOptB, "btn_tts_opt_b")
        btnOptCTts = installChoiceSpeaker(binding.btnOptC, "btn_tts_opt_c")
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

        val existing = parent.findViewWithTag<ImageButton>("btn_tts_progress")
        if (existing != null) return existing

        return try {
            val index = parent.indexOfChild(tv)
            val originalLp = tv.layoutParams
            parent.removeView(tv)

            val row = LinearLayout(this).apply {
                tag = "layout_progress_tts"
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = originalLp
            }

            tv.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val btn = buildSpeakerButton("btn_tts_progress").apply {
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
    // MÉCANIQUE QUIZ
    // ─────────────────────────────────────────────────────────────────

    private fun checkAnswer(answer: String) {
        if (answerLocked || hasTimedOut) return
        hasAnswered = true
        answerLocked = true
        stopQuestionTimer()
        userAnswersList.add(answer)

        val q = questions[currentIndex]
        val correct = q.normalizedCorrectAnswer()
        val isCorrect = correct == answer
        if (isCorrect) score++

        try {
            if (isCorrect) SoundManager.playSFX(this, R.raw.sfx_epic_chibi_select)
            else SoundManager.playSFX(this, R.raw.sfx_error)
        } catch (_: Exception) {}

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
            } catch (_: Exception) {}
        }

        val btnMap = mapOf("A" to binding.btnOptA, "B" to binding.btnOptB, "C" to binding.btnOptC)
        btnMap[answer]?.setBackgroundColor(if (isCorrect) COLOR_CORRECT else COLOR_WRONG)
        if (!isCorrect) btnMap[correct]?.setBackgroundColor(COLOR_CORRECT)

        binding.btnNext.visibility = View.VISIBLE
    }

    private fun afficherTexteRPG(texte: String) {
        typewriterSessionId += 1L
        val localSessionId = typewriterSessionId

        typewriterJob?.cancel()
        SoundManager.stopAllDialogueBlips()
        binding.tvQuestion.text = texte

        typewriterJob = lifecycleScope.launch {
            try {
                if (!isQuestionDialogueUsable()) {
                    binding.tvQuestion.text = texte
                    return@launch
                }

                binding.tvQuestion.text = ""

                for (i in texte.indices) {
                    if (localSessionId != typewriterSessionId || !isQuestionDialogueUsable()) {
                        break
                    }

                    binding.tvQuestion.text = texte.substring(0, i + 1)
                    try {
                        SoundManager.playSFXLow(this@QuizActivity, R.raw.sfx_dialogue_blip)
                    } catch (_: Exception) {
                    }
                    delay(30)
                }

                if (localSessionId == typewriterSessionId) {
                    binding.tvQuestion.text = texte
                }
            } catch (_: Exception) {
                binding.tvQuestion.text = texte
            }
        }
    }

    private fun terminerQuiz() {
        val sharedPref = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        val streak = sharedPref.getInt("STREAK", 0)
        val total = questions.size.coerceAtLeast(1)

        /**
         * Oracle :
         * - mode chronométré   -> récompenses pleines
         * - mode sans timer    -> XP réduite de moitié
         */
        val rawXpGained = XpCalculator.calculateQuizXp(score, total, streak)
        val xpGained = if (currentQuizIsTimedMode) {
            rawXpGained
        } else {
            kotlin.math.max(0, kotlin.math.ceil(rawXpGained / 2.0).toInt())
        }

        val intent = Intent(this, QuizResultActivity::class.java).apply {
            putExtra("SCORE", score)
            putExtra("TOTAL", total)
            putExtra("XP_GAINED", xpGained)
            putExtra("MATIERE", currentMatiere)
            putExtra("IS_TIMED_MODE", currentQuizIsTimedMode)
            putExtra("TRAINING_MODE", "ORACLE")
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
        appliquerFondPremiumQuizPourMatiere(currentMatiere)
    }

    override fun onPause() {
        super.onPause()
        animatedBackgroundHelper?.stop()
        stopQuestionTimer()
        tts.stop()
    }

    override fun onDestroy() {
        animatedBackgroundHelper?.release()
        animatedBackgroundHelper = null
        super.onDestroy()
        stopQuestionTimer()
        typewriterJob?.cancel()
        tts.release()
    }
}

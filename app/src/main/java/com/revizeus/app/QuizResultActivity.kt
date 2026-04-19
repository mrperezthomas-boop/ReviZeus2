package com.revizeus.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.core.GodTriggerEngine
import com.revizeus.app.core.UserAnalyticsEngine
import com.revizeus.app.core.XpCalculator
import com.revizeus.app.databinding.ActivityQuizResultBinding
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.QuizQuestion
import com.revizeus.app.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuizResultActivity : BaseActivity() {

    private lateinit var binding: ActivityQuizResultBinding
    private val godAnim = GodSpeechAnimator()
    private var typewriterJob: Job? = null

    private val tts: SpeakerTtsHelper by lazy { SpeakerTtsHelper(this) }

    private lateinit var currentMatiere: String
    private var questions = listOf<QuizQuestion>()
    private var userAnswers = listOf<String>()
    private var animatedBackgroundHelper: AnimatedBackgroundHelper? = null
    private var lootAnimationJob: Job? = null
    private var rewardCounterAnimator: ValueAnimator? = null
    private var eclatCounterAnimator: ValueAnimator? = null
    private var ambroisieCounterAnimator: ValueAnimator? = null
    private var dominantReactionJob: Job? = null
    private var lastFragmentsBySubject: Map<String, Int> = emptyMap()
    private var lastDominantFragmentSubject: String = ""
    private val quizStatsPrefsName: String = "RevizeusQuizStats"

    private fun persistQuizCompletionStats(starsEarned: Int) {
        try {
            val prefs = getSharedPreferences(quizStatsPrefsName, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putInt("quiz_completed_total", prefs.getInt("quiz_completed_total", 0) + 1)
            val safeStars = starsEarned.coerceIn(0, 6)
            editor.putInt("quiz_stars_" + safeStars, prefs.getInt("quiz_stars_" + safeStars, 0) + 1)
            editor.apply()
        } catch (e: Exception) {
            Log.e("REVIZEUS", "Impossible d'enregistrer les stats de quiz complétés : ${e.message}")
        }
    }

    private fun installerFondPremiumQuizResult() {
        animatedBackgroundHelper = AnimatedBackgroundHelper(
            targetView = binding.root,
            particlesView = binding.perfectParticles,
            backgroundImageView = binding.imgBackgroundResult
        )
        animatedBackgroundHelper?.configurePremiumBackground(
            staticDrawableRes = getDrawableResOrFallback("bg_resultat_animated", "bg_olympus_dark"),
            videoRawRes = getRawResByName("bg_resultat_animated"),
            imageAlpha = 0.16f,
            loopVideo = true,
            videoVolume = 0.50f
        )
    }

    private fun appliquerFondPremiumQuizResultPourMatiere(matiere: String) {
        val accentColor = PantheonConfig.findByMatiere(matiere)?.couleur
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        installerFondPremiumQuizResult()

        val score = intent.getIntExtra("SCORE", 0)
        val total = intent.getIntExtra("TOTAL", 20)
        val xpGained = intent.getIntExtra("XP_GAINED", 0)
        currentMatiere = intent.getStringExtra("MATIERE") ?: "Mathématiques"
        val isEpreuveUltime = intent.getBooleanExtra("IS_EPREUVE_ULTIME", false)
        val isTimedMode = intent.getBooleanExtra("IS_TIMED_MODE", false)
        val trainingMode = intent.getStringExtra("TRAINING_MODE") ?: ""

        appliquerFondPremiumQuizResultPourMatiere(currentMatiere)

        @Suppress("UNCHECKED_CAST")
        questions = (intent.getSerializableExtra("QUESTIONS_LIST") as? ArrayList<QuizQuestion>) ?: arrayListOf()
        userAnswers = intent.getStringArrayListExtra("USER_ANSWERS") ?: arrayListOf()

        binding.tvScore.text = "$score / $total"
        binding.tvXpGained.text = "+$xpGained XP"

        val percentage = if (total > 0) (score * 100) / total else 0

        val rewardMultiplier = when {
            trainingMode.equals("ORACLE", ignoreCase = true) -> 1.0
            !isTimedMode -> 0.5
            else -> 1.0
        }

        val eclatsGain = (score * 5 * rewardMultiplier).toInt()

        val ambroisieGain = when {
            percentage == 100 -> (6 * rewardMultiplier).toInt()
            percentage >= 90 -> (3 * rewardMultiplier).toInt()
            else -> 0
        }

        val fragmentsBySubject = calculateFragmentRewardsBySubject(
            questions = questions,
            userAnswers = userAnswers,
            fallbackMatiere = currentMatiere,
            trainingMode = trainingMode,
            isTimedMode = isTimedMode
        )
        val fragmentsGain = fragmentsBySubject.values.sum()

        lastFragmentsBySubject = fragmentsBySubject
        lastDominantFragmentSubject = selectDominantFragmentSubject(fragmentsBySubject)

        binding.tvRewardEclats.text = "+$eclatsGain Éclats de Savoir"
        binding.tvRewardAmbroisie.text = if (ambroisieGain > 0) "+$ambroisieGain Ambroisie" else "+0 Ambroisie"
        binding.tvRewardFragments.text = buildRewardFragmentsLabel(fragmentsGain)
        displayFragmentsBreakdown(fragmentsBySubject)

        val iconSubject = selectRewardIconSubject(currentMatiere, fragmentsBySubject)
        val fragmentIconRes = KnowledgeFragmentManager.getFragmentIconRes(this, iconSubject)
        if (fragmentIconRes != 0) {
            binding.imgRewardFragments.setImageResource(fragmentIconRes)
        }

        binding.pbQuizScore.max = total
        binding.pbQuizScore.progress = score.coerceAtMost(total.coerceAtLeast(0))

        setupStarsAndMusic(score, total)

        startPhase3LootPresentation(
            eclatsGain = eclatsGain,
            ambroisieGain = ambroisieGain,
            fragmentsGain = fragmentsGain,
            fragmentsBySubject = fragmentsBySubject
        )

        afficherVerdictDivin(score, total, currentMatiere, isEpreuveUltime)
        crediterXPetStreak(
            xpGained = xpGained,
            score = score,
            total = total,
            fragmentsBySubject = fragmentsBySubject,
            eclatsGain = eclatsGain,
            ambroisieGain = ambroisieGain
        )

        setupReviewList(questions, userAnswers)
        setupTopResultSpeakerButton(score, total, xpGained, eclatsGain, ambroisieGain, fragmentsGain)

        binding.btnReturnDashboard.setOnClickListener { handleBackPressed() }
    }

    private fun calculateFragmentRewardsBySubject(
        questions: List<QuizQuestion>,
        userAnswers: List<String>,
        fallbackMatiere: String,
        trainingMode: String,
        isTimedMode: Boolean
    ): Map<String, Int> {
        val rewards = linkedMapOf<String, Int>()
        val isOracle = trainingMode.equals("ORACLE", ignoreCase = true)
        val isUltimeGlobal = trainingMode.equals("ULTIME_GLOBAL", ignoreCase = true)
        val isUltimeMatiere = trainingMode.equals("ULTIME_MATIERE", ignoreCase = true)
        val isUltime = isUltimeGlobal || isUltimeMatiere
        val baseRewardPerCorrect = if (isUltime) 3 else 1

        questions.forEachIndexed { index, question ->
            val userAnswer = userAnswers.getOrNull(index)?.trim()?.uppercase() ?: ""
            val isCorrect = userAnswer == question.normalizedCorrectAnswer()
            if (isCorrect) {
                val resolvedSubject = resolveRewardSubject(question, fallbackMatiere, isUltimeGlobal)
                if (resolvedSubject.isNotBlank() && resolvedSubject != "Panthéon") {
                    rewards[resolvedSubject] = rewards.getOrDefault(resolvedSubject, 0) + baseRewardPerCorrect
                }
            }
        }

        if (!isTimedMode && !isOracle) {
            return rewards.mapValues { (_, value) -> kotlin.math.max(0, kotlin.math.ceil(value / 2.0).toInt()) }
        }
        return rewards
    }

    private fun resolveRewardSubject(question: QuizQuestion, fallbackMatiere: String, isUltimeGlobal: Boolean): String {
        val explicitSubject = canonicalizeRewardSubject(question.subject)
        if (explicitSubject.isNotBlank() && explicitSubject != "Panthéon") return explicitSubject
        if (isUltimeGlobal) {
            val inferred = inferRewardSubjectFromQuestion(question)
            if (inferred.isNotBlank() && inferred != "Panthéon") return inferred
            return ""
        }
        val fallback = canonicalizeRewardSubject(fallbackMatiere)
        return if (fallback == "Panthéon") "" else fallback
    }

    private fun inferRewardSubjectFromQuestion(question: QuizQuestion): String {
        val haystack = listOf(question.text, question.optionA, question.optionB, question.optionC).joinToString(" ").lowercase()
        fun scoreFor(subject: String): Int {
            val keywords = when (subject) {
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
        val allSubjects = listOf("Mathématiques", "Français", "SVT", "Histoire", "Physique-Chimie", "Géographie", "Art/Musique", "Langues", "Philo/SES", "Vie & Projets")
        val best = allSubjects.maxByOrNull { scoreFor(it) } ?: ""
        return if (scoreFor(best) <= 0) "" else best
    }

    private fun canonicalizeRewardSubject(subject: String): String {
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

    private fun buildRewardFragmentsLabel(fragmentsGain: Int): String = "+$fragmentsGain Fragments"

    private fun selectRewardIconSubject(fallbackMatiere: String, fragmentsBySubject: Map<String, Int>): String {
        val canonicalFallback = canonicalizeRewardSubject(fallbackMatiere)
        return when {
            canonicalFallback.isNotBlank() && canonicalFallback != "Panthéon" -> canonicalFallback
            fragmentsBySubject.isNotEmpty() -> fragmentsBySubject.entries.maxByOrNull { it.value }?.key ?: "Savoir"
            else -> "Savoir"
        }
    }

    private fun selectDominantFragmentSubject(fragmentsBySubject: Map<String, Int>): String {
        return fragmentsBySubject.filter { it.value > 0 && it.key != "Panthéon" }.maxByOrNull { it.value }?.key.orEmpty()
    }

    private fun displayFragmentsBreakdown(fragmentsBySubject: Map<String, Int>) {
        val container = binding.layoutFragmentsBreakdown
        container.removeAllViews()
        val cleanEntries = fragmentsBySubject.entries.filter { it.value > 0 && it.key != "Panthéon" }.sortedByDescending { it.value }
        if (cleanEntries.size <= 1) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        container.orientation = LinearLayout.VERTICAL
        container.gravity = Gravity.CENTER_HORIZONTAL

        cleanEntries.forEachIndexed { index, entry ->
            val subject = entry.key
            val amount = entry.value
            val godProfile = PantheonConfig.findByMatiere(subject)
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                alpha = 0f
                scaleX = 0.92f
                scaleY = 0.92f
                setPadding(dp(10), dp(6), dp(10), dp(6))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val icon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(6) }
                val res = KnowledgeFragmentManager.getFragmentIconRes(this@QuizResultActivity, subject)
                if (res != 0) setImageResource(res)
            }
            val label = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = if (godProfile != null) "${godProfile.divinite} ($subject)" else subject
                setTextColor(Color.parseColor("#F5F5F5"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
            }
            val text = TextView(this).apply {
                text = "+$amount"
                setTextColor(KnowledgeFragmentManager.getFragmentColorInt(subject))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, Typeface.BOLD)
            }
            itemLayout.addView(icon)
            itemLayout.addView(label)
            itemLayout.addView(text)
            container.addView(itemLayout)
            itemLayout.postDelayed({ animateFragmentBreakdownRow(itemLayout, subject) }, (index * 140L))
        }
    }

    private fun animateFragmentBreakdownRow(row: View, subject: String) {
        try {
            row.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(240L).setInterpolator(OvershootInterpolator(1.1f)).withStartAction {
                try { SoundManager.playSFX(this, R.raw.sfx_orb_open) } catch (_: Exception) {}
            }.start()
            val accent = KnowledgeFragmentManager.getFragmentColorInt(subject)
            val pulseX = ObjectAnimator.ofFloat(row, View.SCALE_X, 1f, 1.035f, 1f)
            val pulseY = ObjectAnimator.ofFloat(row, View.SCALE_Y, 1f, 1.035f, 1f)
            val glow = ObjectAnimator.ofArgb(row, "backgroundColor", Color.TRANSPARENT, adjustAlpha(accent, 40), Color.TRANSPARENT)
            glow.setEvaluator(ArgbEvaluator())
            AnimatorSet().apply {
                playTogether(pulseX, pulseY, glow)
                duration = 380L
                interpolator = AccelerateDecelerateInterpolator()
                startDelay = 40L
                start()
            }
        } catch (_: Exception) {}
    }

    private fun startPhase3LootPresentation(eclatsGain: Int, ambroisieGain: Int, fragmentsGain: Int, fragmentsBySubject: Map<String, Int>) {
        lootAnimationJob?.cancel()
        dominantReactionJob?.cancel()
        binding.layoutCurrencyRewards.alpha = 0f
        binding.layoutCurrencyRewards.scaleX = 0.92f
        binding.layoutCurrencyRewards.scaleY = 0.92f
        binding.tvRewardFragments.alpha = 0.86f
        binding.imgRewardFragments.alpha = 0.86f
        lootAnimationJob = lifecycleScope.launch {
            delay(120L)
            animateRewardCardEntrance()
            delay(120L)
            animateRewardCounters(eclatsGain, ambroisieGain, fragmentsGain)
            delay(200L)
            animateFragmentTotalHeroShot(fragmentsGain)
            delay(160L)
            triggerDominantFragmentReaction(fragmentsBySubject)
        }
    }

    private fun animateRewardCardEntrance() {
        binding.layoutCurrencyRewards.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(280L).setInterpolator(OvershootInterpolator(1.08f)).start()
    }

    private fun animateRewardCounters(eclatsGain: Int, ambroisieGain: Int, fragmentsGain: Int) {
        eclatCounterAnimator?.cancel()
        ambroisieCounterAnimator?.cancel()
        rewardCounterAnimator?.cancel()
        eclatCounterAnimator = ValueAnimator.ofInt(0, eclatsGain).apply {
            duration = 520L
            interpolator = DecelerateInterpolator()
            addUpdateListener { binding.tvRewardEclats.text = "+${it.animatedValue as Int} Éclats de Savoir" }
            start()
        }
        ambroisieCounterAnimator = ValueAnimator.ofInt(0, ambroisieGain).apply {
            duration = 520L
            startDelay = 80L
            interpolator = DecelerateInterpolator()
            addUpdateListener { binding.tvRewardAmbroisie.text = "+${it.animatedValue as Int} Ambroisie" }
            start()
        }
        rewardCounterAnimator = ValueAnimator.ofInt(0, fragmentsGain).apply {
            duration = 680L
            startDelay = 120L
            interpolator = OvershootInterpolator(0.95f)
            addUpdateListener { binding.tvRewardFragments.text = buildRewardFragmentsLabel(it.animatedValue as Int) }
            start()
        }
    }

    private fun animateFragmentTotalHeroShot(fragmentsGain: Int) {
        if (fragmentsGain <= 0) return
        val iconSubject = if (lastDominantFragmentSubject.isNotBlank()) lastDominantFragmentSubject else selectRewardIconSubject(currentMatiere, lastFragmentsBySubject)
        val accent = KnowledgeFragmentManager.getFragmentColorInt(iconSubject)
        val totalScaleX = ObjectAnimator.ofFloat(binding.tvRewardFragments, View.SCALE_X, 1f, 1.14f, 1f)
        val totalScaleY = ObjectAnimator.ofFloat(binding.tvRewardFragments, View.SCALE_Y, 1f, 1.14f, 1f)
        val iconScaleX = ObjectAnimator.ofFloat(binding.imgRewardFragments, View.SCALE_X, 1f, 1.18f, 1f)
        val iconScaleY = ObjectAnimator.ofFloat(binding.imgRewardFragments, View.SCALE_Y, 1f, 1.18f, 1f)
        val cardScaleX = ObjectAnimator.ofFloat(binding.layoutCurrencyRewards, View.SCALE_X, 1f, 1.03f, 1f)
        val cardScaleY = ObjectAnimator.ofFloat(binding.layoutCurrencyRewards, View.SCALE_Y, 1f, 1.03f, 1f)
        val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), binding.tvRewardFragments.currentTextColor, accent, Color.parseColor("#F3E6B3")).apply {
            duration = 520L
            addUpdateListener { binding.tvRewardFragments.setTextColor(it.animatedValue as Int) }
        }
        AnimatorSet().apply {
            playTogether(totalScaleX, totalScaleY, iconScaleX, iconScaleY, cardScaleX, cardScaleY, colorAnimator)
            duration = 520L
            interpolator = OvershootInterpolator(1.15f)
            start()
        }
        spawnFloatingFragmentBurst(iconSubject, kotlin.math.min(8, kotlin.math.max(4, fragmentsGain.coerceAtMost(8))))
        vibrateLootPulse(fragmentsGain)
    }

    private fun triggerDominantFragmentReaction(fragmentsBySubject: Map<String, Int>) {
        dominantReactionJob?.cancel()
        val dominantSubject = selectDominantFragmentSubject(fragmentsBySubject)
        if (dominantSubject.isBlank()) return
        val dominantAmount = fragmentsBySubject[dominantSubject] ?: 0
        if (dominantAmount <= 0) return
        val godProfile = PantheonConfig.findByMatiere(dominantSubject)
        val accent = KnowledgeFragmentManager.getFragmentColorInt(dominantSubject)
        val subLabel = if (godProfile != null) "Butin dominant : ${godProfile.divinite} +$dominantAmount fragments" else "Butin dominant : $dominantSubject +$dominantAmount fragments"
        dominantReactionJob = lifecycleScope.launch {
            binding.tvVerdictSubLabel.text = subLabel
            binding.tvVerdictSubLabel.setTextColor(accent)
            binding.tvVerdictSubLabel.alpha = 0f
            binding.tvVerdictSubLabel.translationY = dp(6).toFloat()
            binding.tvVerdictSubLabel.animate().alpha(1f).translationY(0f).setDuration(260L).setInterpolator(DecelerateInterpolator()).start()
            try {
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(binding.imgZeusResult, View.SCALE_X, 1f, 1.04f, 1f),
                        ObjectAnimator.ofFloat(binding.imgZeusResult, View.SCALE_Y, 1f, 1.04f, 1f)
                    )
                    duration = 340L
                    interpolator = OvershootInterpolator(1f)
                    start()
                }
            } catch (_: Exception) {}
        }
    }

    private fun spawnFloatingFragmentBurst(subject: String, burstCount: Int) {
        val root = window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: return
        val iconRes = KnowledgeFragmentManager.getFragmentIconRes(this, subject)
        if (iconRes == 0) return
        val overlay = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            isClickable = false
            isFocusable = false
        }
        root.addView(overlay)
        val baseAngles = listOf(-52f, -32f, -14f, 12f, 30f, 52f, -70f, 72f)
        val centerY = (binding.layoutCurrencyRewards.y + binding.layoutCurrencyRewards.height / 2f).takeIf { it > 0f } ?: (resources.displayMetrics.heightPixels * 0.34f)
        repeat(burstCount) { index ->
            val orb = ImageView(this).apply {
                setImageResource(iconRes)
                alpha = 0f
                scaleX = 0.6f
                scaleY = 0.6f
                translationY = centerY
                translationX = 0f
                layoutParams = FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER_HORIZONTAL or Gravity.TOP)
            }
            overlay.addView(orb)
            val angle = baseAngles.getOrNull(index % baseAngles.size) ?: 0f
            val horizontal = dp((22 + (index * 8)) * if (angle >= 0) 1 else -1).toFloat()
            val vertical = dp(52 + (index * 10)).toFloat()
            orb.animate().alpha(0.95f).scaleX(1f).scaleY(1f).translationX(horizontal).translationY(centerY - vertical).setStartDelay((index * 28L)).setDuration(460L).setInterpolator(DecelerateInterpolator()).withEndAction {
                orb.animate().alpha(0f).translationY(centerY - vertical - dp(12)).setDuration(220L).withEndAction {
                    try {
                        overlay.removeView(orb)
                        if (overlay.childCount == 0) root.removeView(overlay)
                    } catch (_: Exception) {}
                }.start()
            }.start()
        }
        overlay.postDelayed({ try { root.removeView(overlay) } catch (_: Exception) {} }, 1400L)
    }

    private fun vibrateLootPulse(fragmentsGain: Int) {
        if (fragmentsGain <= 0) return
        try {
            val intensityPattern = when {
                fragmentsGain >= 12 -> longArrayOf(0L, 28L, 34L, 44L)
                fragmentsGain >= 6 -> longArrayOf(0L, 20L, 24L, 30L)
                else -> longArrayOf(0L, 16L)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(intensityPattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(intensityPattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(intensityPattern, -1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun adjustAlpha(color: Int, alpha: Int): Int = Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun buildGodTriggerVerdictDivineRequestContext(
        matiere: String,
        percentage: Int,
        profile: UserProfile,
        godTrigger: GodTriggerEngine.GodTrigger,
        aresTriggered: Boolean
    ): DivineRequestContext {
        val ctxNote = godTrigger.contextNote.trim()
        val raw = buildString {
            append("score=${percentage}%")
            if (ctxNote.isNotEmpty()) {
                append(" · ")
                append(ctxNote.take(380))
            }
        }
        return DivineRequestContext(
            subject = matiere,
            actionType = DivineActionType.DIVINE_VERDICT,
            screenSource = "quizresult_bloc4b_godtrigger",
            userAge = profile.age,
            userClassLevel = profile.classLevel,
            currentMood = profile.mood,
            successState = when {
                percentage >= 75 -> true
                percentage < 50 -> false
                else -> null
            },
            difficulty = profile.level,
            rawInput = raw,
            validatedSummary = null,
            questionText = null,
            userAnswer = null,
            correctAnswer = null,
            metadata = mapOf(
                "trigger_god" to godTrigger.godName,
                "trigger_reason" to godTrigger.reason.take(160),
                "trigger_priority" to godTrigger.priority.toString(),
                "ares_streak_event" to aresTriggered.toString()
            )
        )
    }

    private fun afficherVerdictDivin(score: Int, total: Int, matiere: String, isEpreuveUltime: Boolean) {
        val percentage = if (total > 0) (score * 100) / total else 0
        val prefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        val aresStreakKey = "ARES_STREAK"
        val currentStreak = prefs.getInt(aresStreakKey, 0)
        val newStreak = if (percentage >= 95) currentStreak + 1 else 0
        prefs.edit().putInt(aresStreakKey, newStreak).apply()
        val aresTriggered = (newStreak >= 3)

        lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getDatabase(this@QuizResultActivity)
                    db.iAristoteDao().getUserProfile() ?: UserProfile(id = 1, age = 15, classLevel = "Terminale", mood = "Prêt", xp = 0, streak = 0, cognitivePattern = "Général")
                } catch (_: Exception) {
                    UserProfile(id = 1, age = 15, classLevel = "Terminale", mood = "Prêt", xp = 0, streak = 0, cognitivePattern = "Général")
                }
            }
            val insights = withContext(Dispatchers.IO) {
                try {
                    UserAnalyticsEngine.analyzeUser(context = this@QuizResultActivity, subject = matiere, recentOnly = true)
                } catch (e: Exception) {
                    Log.w("REVIZEUS_BLOC4B", "Analyse insights échouée: ${e.message}")
                    emptyList()
                }
            }
            val godTrigger = withContext(Dispatchers.IO) {
                GodTriggerEngine.analyzeAndSelectGod(context = this@QuizResultActivity, insights = insights, subject = matiere, scorePercent = percentage, isArèsStreak = aresTriggered)
            }
            if (godTrigger == null) {
                Log.w("REVIZEUS_BLOC4B", "GodTrigger null, fallback classique")
                afficherVerdictClassique(score, total, matiere, isEpreuveUltime, profile, aresTriggered)
                return@launch
            }
            Log.d("REVIZEUS_BLOC4B", "Dieu sélectionné: ${godTrigger.godName} (raison: ${godTrigger.reason})")
            PantheonConfig.findByDivinite(godTrigger.godName)?.let { godProfile ->
                val resId = resources.getIdentifier(godProfile.iconResName, "drawable", packageName)
                if (resId != 0) binding.imgZeusResult.setImageResource(resId)
            }
            if (aresTriggered) {
                prefs.edit().putInt(aresStreakKey, 0).apply()
                try { SoundManager.playSFX(this@QuizResultActivity, R.raw.sfx_thunder_confirm) } catch (e: Exception) { Log.e("REVIZEUS", "Arès SFX erreur : ${e.message}") }
            }
            val geminiPrompt = GodTriggerEngine.buildGodDialoguePrompt(trigger = godTrigger, subject = matiere, userProfile = profile, scorePercent = percentage)
            val verdictDivineCtx = buildGodTriggerVerdictDivineRequestContext(matiere, percentage, profile, godTrigger, aresTriggered)
            val godResponse = withContext(Dispatchers.IO) {
                try {
                    GeminiManager.generateDialog(prompt = geminiPrompt, matiere = matiere, divineRequestContext = verdictDivineCtx, adaptiveContextNote = null)
                } catch (e: Exception) {
                    Log.e("REVIZEUS_BLOC4B", "Gemini dialogue échoué: ${e.message}", e)
                    GeminiManager.GodResponse(
                        text = buildFallbackMessage(godTrigger.godName, percentage),
                        mnemo = "",
                        tone = "Encourageant",
                        godName = godTrigger.godName,
                        matiere = matiere,
                        suggestedAction = "Continue à t'entraîner"
                    )
                }
            }
            typewriterJob?.cancel()
            typewriterJob = godAnim.typewriteSimple(
                scope = lifecycleScope,
                chibiView = binding.imgZeusResult,
                textView = binding.tvZeusMessage,
                text = godResponse?.text ?: buildFallbackMessage(godTrigger.godName, percentage),
                context = this@QuizResultActivity
            )
            Log.d("REVIZEUS_BLOC4B", "Dialogue affiché: ${godResponse?.text?.take(100) ?: ""}...")
        }
    }

    private fun afficherVerdictClassique(score: Int, total: Int, matiere: String, isEpreuveUltime: Boolean, profile: UserProfile, aresTriggered: Boolean) {
        lifecycleScope.launch {
            if (aresTriggered) {
                val aresResponse = GodLoreManager.buildAresChallenge(matiere, profile)
                typewriterJob?.cancel()
                typewriterJob = godAnim.typewriteSimple(
                    scope = lifecycleScope,
                    chibiView = binding.imgZeusResult,
                    textView = binding.tvZeusMessage,
                    text = aresResponse.text,
                    context = this@QuizResultActivity
                )
            } else {
                val percentage = if (total > 0) (score * 100) / total else 0
                val adaptiveContext = AdaptiveLearningContextResolver.resolve(this@QuizResultActivity, matiere, profile.age, profile.classLevel, profile.mood)
                val response = GodLoreManager.buildQuizResultDialogue(matiere, percentage, profile, isEpreuveUltime, adaptiveContext.toPromptNote())
                typewriterJob?.cancel()
                typewriterJob = godAnim.typewriteSimple(
                    scope = lifecycleScope,
                    chibiView = binding.imgZeusResult,
                    textView = binding.tvZeusMessage,
                    text = response.text,
                    context = this@QuizResultActivity
                )
            }
        }
    }

    private fun buildFallbackMessage(godName: String, scorePercent: Int): String = when (godName) {
        "Zeus" -> when {
            scorePercent >= 90 -> "Excellent travail, héros ! Ma foudre célèbre ta maîtrise !"
            scorePercent >= 75 -> "Bien joué ! Continue sur cette voie."
            scorePercent >= 60 -> "Correct, mais tu peux mieux faire."
            else -> "Il faut reprendre les bases, héros."
        }
        "Athéna" -> "Ta stratégie porte ses fruits ! Continue d'analyser et de progresser."
        "Poséidon" -> "Calme les flots de ton esprit. La régularité viendra avec la pratique."
        "Arès" -> "Guerrier ! Je te lance un défi. Prouve ta valeur !"
        "Aphrodite" -> "Prends soin de toi, héros. Une pause te fera du bien."
        "Hermès" -> "Ralentis, messager ! La vitesse sans contrôle mène au chaos."
        "Déméter" -> "Ce savoir a besoin de ta lumière. Reviens le cultiver régulièrement."
        "Héphaïstos" -> "Reconstruis tes bases, forgeron. Méthode et patience."
        "Apollon" -> "Tu brilles comme le soleil ! Ta maîtrise illumine l'Olympe !"
        "Prométhée" -> "Ose une nouvelle approche, héros. Le feu de l'innovation est en toi."
        else -> "Bien joué ! Continue à progresser."
    }

    private fun setupStarsAndMusic(score: Int, total: Int) {
        val percentage = if (total > 0) (score * 100) / total else 0
        val starsEarned = when {
            percentage == 0 -> 0
            percentage in 1..20 -> 1
            percentage in 21..40 -> 2
            percentage in 41..60 -> 3
            percentage in 61..80 -> 4
            percentage in 81..99 -> 5
            percentage == 100 -> 6
            else -> 0
        }
        val bgmRes = when {
            percentage == 0 -> R.raw.bgm_result_fail
            percentage in 1..20 -> R.raw.bgm_result_fail
            percentage in 21..40 -> R.raw.bgm_result_moyen
            percentage in 41..60 -> R.raw.bgm_result_moyen
            percentage in 61..80 -> R.raw.bgm_result_bien
            percentage in 81..99 -> R.raw.bgm_result_bien
            percentage == 100 -> R.raw.bgm_result_parfait
            else -> R.raw.bgm_result_fail
        }
        applyStarsVisualState(starsEarned)
        persistQuizCompletionStats(starsEarned)
        try {
            BadgeManager.recordQuizCompleted(context = this, matiere = currentMatiere, scorePercent = percentage, durationSeconds = 0, isEpreuveUltime = currentMatiere == "Panthéon")
        } catch (e: Exception) {
            Log.e("REVIZEUS", "Impossible de mettre à jour le total de quiz complétés : ${e.message}")
        }
        try {
            SoundManager.playMusic(this, bgmRes)
            SoundManager.rememberMusic(bgmRes)
        } catch (e: Exception) {
            Log.e("REVIZEUS", "Erreur lecture musique résultat : ${e.message}")
        }
        if (percentage == 100) triggerPerfectAnimation()
    }

    private fun applyStarsVisualState(starsEarned: Int) {
        val normalStars = listOf(binding.star1, binding.star2, binding.star3, binding.star4, binding.star5)
        normalStars.forEachIndexed { index, imageView ->
            imageView.setImageResource(if (index < starsEarned.coerceAtMost(5)) R.drawable.ic_star_filled else R.drawable.ic_star_empty)
        }
        if (starsEarned == 6) {
            binding.star6.visibility = View.VISIBLE
            binding.star6.setImageResource(R.drawable.ic_star_divine)
        } else {
            binding.star6.visibility = View.GONE
            binding.star6.setImageResource(R.drawable.ic_star_divine)
        }
    }

    private fun triggerPerfectAnimation() {
        binding.star6.visibility = View.VISIBLE
        binding.star6.setImageResource(R.drawable.ic_star_divine)
        binding.flashOverlay.visibility = View.VISIBLE
        binding.flashOverlay.alpha = 0f
        val flashAnimation = AlphaAnimation(0f, 1f).apply {
            duration = 260L
            repeatCount = 1
            repeatMode = AlphaAnimation.REVERSE
            interpolator = DecelerateInterpolator()
            fillAfter = false
        }
        flashAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) { binding.flashOverlay.visibility = View.VISIBLE }
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                binding.flashOverlay.alpha = 0f
                binding.flashOverlay.visibility = View.GONE
            }
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
        binding.flashOverlay.startAnimation(flashAnimation)
        binding.perfectParticles.visibility = View.VISIBLE
        tryStartPerfectParticles()
        vibrateEpicPerfect()
        try { SoundManager.playSFX(this, R.raw.sfx_thunder_confirm) } catch (e: Exception) { Log.e("REVIZEUS", "Erreur SFX perfect result : ${e.message}") }
    }

    private fun tryStartPerfectParticles() {
        try {
            val particlesView = binding.perfectParticles
            val candidateMethods = listOf("start", "startParticles", "startEmission", "startEmitting", "burst", "play")
            var invoked = false
            for (methodName in candidateMethods) {
                try {
                    val method = particlesView.javaClass.methods.firstOrNull { it.name == methodName && it.parameterCount == 0 }
                    if (method != null) {
                        method.invoke(particlesView)
                        invoked = true
                        break
                    }
                } catch (_: Exception) {}
            }
            if (!invoked) particlesView.invalidate()
        } catch (e: Exception) {
            Log.e("REVIZEUS", "Impossible d'activer perfectParticles : ${e.message}")
        }
    }

    private fun vibrateEpicPerfect() {
        try {
            val pattern = longArrayOf(0L, 90L, 60L, 140L, 70L, 220L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.e("REVIZEUS", "Erreur vibration parfaite : ${e.message}")
        }
    }

    private fun crediterXPetStreak(xpGained: Int, score: Int, total: Int, fragmentsBySubject: Map<String, Int>, eclatsGain: Int, ambroisieGain: Int) {
        val percentage = if (total > 0) (score * 100) / total else 0
        val didWin = percentage > 75
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@QuizResultActivity)
                val fragmentsReward = withContext(Dispatchers.IO) {
                    var profile = db.iAristoteDao().getUserProfile()
                    if (profile == null) {
                        profile = UserProfile(id = 1, age = 15, classLevel = "Terminale", mood = "Prêt", xp = 0, streak = 0, cognitivePattern = "Général")
                        db.iAristoteDao().saveUserProfile(profile)
                    }
                    if (xpGained > 0) profile.xp += xpGained
                    profile.eclatsSavoir += eclatsGain
                    profile.ambroisie += ambroisieGain
                    profile.winStreak = if (didWin) profile.winStreak + 1 else 0
                    profile.bestWinStreak = maxOf(profile.bestWinStreak, profile.winStreak)
                    if (didWin) profile.lastWinQuizAt = System.currentTimeMillis()
                    if (profile.dayStreak <= 0 && profile.streak > 0) profile.dayStreak = profile.streak
                    if (profile.bestDayStreak <= 0 && profile.bestStreakEver > 0) profile.bestDayStreak = profile.bestStreakEver
                    profile.level = XpCalculator.calculateLevel(profile.xp)
                    db.iAristoteDao().updateUserProfile(profile)
                    if (xpGained > 0) db.iAristoteDao().recordQuizResult(xpGained)
                    db.iAristoteDao().updateCoursesLastReviewedBySubject(matiere = currentMatiere, timestamp = System.currentTimeMillis())
                    var reward = 0
                    fragmentsBySubject.forEach { (subject, amount) ->
                        if (amount > 0 && subject != "Panthéon") {
                            profile.addFragments(subject, amount)
                            reward += amount
                            Log.d("REVIZEUS", "Fragments : +$amount Fragment(s) de '$subject' | Total : ${profile.getFragmentCount(subject)}")
                        }
                    }
                    if (reward > 0) db.iAristoteDao().updateUserProfile(profile)
                    Log.d("REVIZEUS", "Récompenses : +$xpGained XP, +$eclatsGain éclats, +$ambroisieGain ambroisie | Total XP=${profile.xp}")
                    reward
                }
                if (fragmentsReward > 0) {
                    showFragmentRewardToast(fragmentsReward, fragmentsBySubject)
                    tryPlayFragmentRewardLottie()
                }
            } catch (e: Exception) {
                Log.e("REVIZEUS", "Erreur crédit récompenses : ${e.message}")
            }
        }
    }

    private fun showFragmentRewardToast(reward: Int, fragmentsBySubject: Map<String, Int>) {
        try {
            val iconSubject = selectRewardIconSubject(currentMatiere, fragmentsBySubject)
            val displayName = KnowledgeFragmentManager.getDisplayName(iconSubject)
            val compactBreakdown = if (fragmentsBySubject.size > 1) fragmentsBySubject.entries.joinToString(" • ") { "${it.key} +${it.value}" } else "$displayName +$reward"
            DialogRPGManager.showReward(
                activity = this,
                godId = DialogRPGManager.selectGodForContext(DialogContext.PEDAGOGY),
                title = "Récompense divine",
                message = "Bravo ! Tu as gagné $reward fragments de savoir.",
                additionalLabel = "RÉPARTITION",
                additionalText = compactBreakdown
            )
        } catch (e: Exception) {
            Log.e("REVIZEUS", "Dialogue fragments impossible : ${e.message}")
            DialogRPGManager.showReward(
                activity = this,
                godId = DialogRPGManager.selectGodForContext(DialogContext.PEDAGOGY),
                message = "Bravo ! Tu as gagné $reward Fragments de Savoir !",
                additionalLabel = "PROGRESSION",
                additionalText = "Ces fragments te permettront de forger des objets divins."
            )
        }
    }

    private fun tryPlayFragmentRewardLottie() {
        try {
            val root = window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: return
            val lottieClass = Class.forName("com.airbnb.lottie.LottieAnimationView")
            val lottieView = lottieClass.getConstructor(Context::class.java).newInstance(this) as View
            root.addView(lottieView, FrameLayout.LayoutParams(dp(180), dp(180), Gravity.CENTER))
            lottieClass.getMethod("setAnimation", String::class.java).invoke(lottieView, "lottie_fragment_reward_burst.json")
            lottieClass.getMethod("setRepeatCount", Int::class.javaPrimitiveType).invoke(lottieView, 0)
            lottieClass.getMethod("playAnimation").invoke(lottieView)
            lottieView.postDelayed({ try { root.removeView(lottieView) } catch (_: Exception) {} }, 1400L)
        } catch (e: Exception) {
            Log.d("REVIZEUS", "Lottie fragments indisponible : ${e.message}")
        }
    }

    private fun setupTopResultSpeakerButton(score: Int, total: Int, xpGained: Int, eclatsGain: Int, ambroisieGain: Int, fragmentsGain: Int) {
        try {
            val parent = binding.tvZeusMessage.parent as? ViewGroup ?: return
            val existing = parent.findViewWithTag<View>("btn_tts_quiz_result_top")
            if (existing != null) {
                existing.setOnClickListener { speakTopResultSummary(score, total, xpGained, eclatsGain, ambroisieGain, fragmentsGain) }
                return
            }
            val btnTts = ImageButton(this).apply {
                tag = "btn_tts_quiz_result_top"
                background = null
                alpha = 0.88f
                contentDescription = "Lire le verdict à voix haute"
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                setPadding(0, 0, 0, 0)
                try { setImageResource(R.drawable.ic_speaker_tts) } catch (_: Exception) { setImageResource(android.R.drawable.ic_btn_speak_now) }
                setOnClickListener { speakTopResultSummary(score, total, xpGained, eclatsGain, ambroisieGain, fragmentsGain) }
            }
            when (parent) {
                is LinearLayout -> {
                    btnTts.layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply { topMargin = dp(8); gravity = Gravity.END }
                    parent.addView(btnTts)
                }
                else -> {
                    btnTts.layoutParams = ViewGroup.MarginLayoutParams(dp(38), dp(38)).apply { topMargin = dp(8) }
                    parent.addView(btnTts)
                }
            }
        } catch (_: Exception) {}
    }

    private fun speakTopResultSummary(score: Int, total: Int, xpGained: Int, eclatsGain: Int, ambroisieGain: Int, fragmentsGain: Int) {
        val verdict = binding.tvZeusMessage.text?.toString()?.trim().orEmpty()
        val textToRead = buildString {
            append("Résultat du quiz. ")
            append("Score : $score sur $total. ")
            append("Récompenses : plus $xpGained expérience, plus $eclatsGain éclats de savoir, plus $ambroisieGain ambroisie, plus $fragmentsGain fragments. ")
            if (lastDominantFragmentSubject.isNotBlank()) {
                val god = PantheonConfig.findByMatiere(lastDominantFragmentSubject)?.divinite ?: lastDominantFragmentSubject
                append("Butin dominant : $god. ")
            }
            if (verdict.isNotBlank()) append("Verdict divin. $verdict")
        }.trim()
        tts.speak(textToRead, PantheonConfig.findByMatiere(currentMatiere)?.divinite ?: "ZEUS", resolveCurrentAgeForTts())
    }

    private fun speakReviewQuestion(numero: Int, question: QuizQuestion, userAnswer: String, isCorrect: Boolean) {
        val safeUserAnswer = if (userAnswer.isBlank()) "Aucune réponse" else userAnswer
        val status = if (isCorrect) "Réussie" else "À revoir"
        val textToRead = buildString {
            append("Question $numero. ")
            append("${question.text}. ")
            append("Option A : ${question.optionA}. ")
            append("Option B : ${question.optionB}. ")
            append("Option C : ${question.optionC}. ")
            append("Ton choix : $safeUserAnswer. ")
            append("Bonne réponse : ${question.correctAnswer}. ")
            append("Statut : $status.")
        }.trim()
        tts.speak(textToRead, PantheonConfig.findByMatiere(currentMatiere)?.divinite ?: "ZEUS", resolveCurrentAgeForTts())
    }

    private fun setupReviewList(questions: List<QuizQuestion>, answers: List<String>) {
        val container = binding.containerQuizReview
        container.removeAllViews()
        if (questions.isEmpty()) return
        questions.forEachIndexed { index, question ->
            val userAnswer = answers.getOrNull(index) ?: ""
            val isCorrect = isReviewAnswerCorrect(question, userAnswer)
            container.addView(createQuestionCard(index + 1, question, userAnswer, isCorrect))
        }
    }

    private fun resolveCurrentAgeForTts(): Int = try {
        getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE).getInt("USER_AGE", 15)
    } catch (_: Exception) { 15 }

    private fun dp(value: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    private fun createSectionLabel(textValue: String, textColorValue: Int, backgroundColorValue: Int): TextView {
        return TextView(this).apply {
            text = textValue
            setTextColor(textColorValue)
            setBackgroundColor(backgroundColorValue)
            setPadding(dp(10), dp(5), dp(10), dp(5))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun createInfoText(textValue: String, textColorValue: Int, textSizeSp: Float, isBold: Boolean = false, topPaddingDp: Int = 0): TextView {
        return TextView(this).apply {
            text = textValue
            setTextColor(textColorValue)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            if (isBold) typeface = Typeface.DEFAULT_BOLD
            if (topPaddingDp > 0) setPadding(0, dp(topPaddingDp), 0, 0)
            setLineSpacing(dp(2).toFloat(), 1f)
        }
    }

    private fun createQuestionCard(numero: Int, question: QuizQuestion, userAnswer: String, isCorrect: Boolean): LinearLayout {
        val outerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#101522"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14) }
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }
        outerCard.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3))
            setBackgroundColor(if (isCorrect) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
        })
        val innerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#171C2B"))
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        outerCard.addView(innerCard)
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        headerRow.addView(TextView(this).apply {
            text = "Q$numero"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
        })
        headerRow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })
        headerRow.addView(createSectionLabel(if (isCorrect) "RÉUSSIE" else "À REVOIR", Color.WHITE, if (isCorrect) Color.parseColor("#2E7D32") else Color.parseColor("#B71C1C")))
        innerCard.addView(headerRow)
        innerCard.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { topMargin = dp(12); bottomMargin = dp(12) }
            setBackgroundColor(Color.parseColor("#33FFD700"))
        })
        innerCard.addView(createSectionLabel("ÉNONCÉ", Color.parseColor("#FFD700"), Color.parseColor("#1FFFFFFF")))
        innerCard.addView(createInfoText(question.text, Color.parseColor("#F5F5F5"), 15f, topPaddingDp = 10))
        innerCard.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { topMargin = dp(12); bottomMargin = dp(12) }
            setBackgroundColor(Color.parseColor("#22FFFFFF"))
        })
        innerCard.addView(createSectionLabel("TA RÉPONSE", if (isCorrect) Color.parseColor("#A5D6A7") else Color.parseColor("#FFCDD2"), if (isCorrect) Color.parseColor("#2232A852") else Color.parseColor("#22D32F2F")))
        innerCard.addView(createInfoText(if (userAnswer.isBlank()) "Aucune réponse" else userAnswer, if (isCorrect) Color.parseColor("#81C784") else Color.parseColor("#EF9A9A"), 14f, isBold = true, topPaddingDp = 8))
        innerCard.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { topMargin = dp(12); bottomMargin = dp(12) }
            setBackgroundColor(Color.parseColor("#18FFD700"))
        })
        innerCard.addView(createSectionLabel(if (isCorrect) "RÉPONSE VALIDÉE" else "BONNE RÉPONSE", Color.parseColor("#C8E6C9"), Color.parseColor("#2232A852")))
        innerCard.addView(createInfoText(question.correctAnswer, Color.parseColor("#A5D6A7"), 14f, isBold = true, topPaddingDp = 8))
        innerCard.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { topMargin = dp(14); bottomMargin = dp(12) }
            setBackgroundColor(Color.parseColor("#14FFFFFF"))
        })
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        actionRow.addView(ImageButton(this).apply {
            tag = "btn_tts_review_$numero"
            background = null
            alpha = 0.88f
            contentDescription = "Lire cette question à voix haute"
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginEnd = dp(8) }
            try { setImageResource(R.drawable.ic_speaker_tts) } catch (_: Exception) { setImageResource(android.R.drawable.ic_btn_speak_now) }
            setOnClickListener { speakReviewQuestion(numero, question, userAnswer, isCorrect) }
        })
        val actionHint = createInfoText("Appuie pour recevoir l’explication divine", Color.parseColor("#CCFFD700"), 12f)
        actionHint.gravity = Gravity.END
        actionHint.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        actionRow.addView(actionHint)
        innerCard.addView(actionRow)
        outerCard.isClickable = true
        outerCard.isFocusable = true
        outerCard.setOnClickListener { afficherExplicationDieu(numero, question, userAnswer, isCorrect) }
        return outerCard
    }

    private fun normalizeReviewAnswer(answer: String): String = answer.trim().uppercase()

    private fun isReviewAnswerCorrect(question: QuizQuestion, userAnswer: String): Boolean {
        val normalizedUserAnswer = normalizeReviewAnswer(userAnswer)
        val normalizedQuestionAnswer = question.normalizedCorrectAnswer().trim().uppercase()
        val normalizedRawCorrectAnswer = question.correctAnswer.trim().uppercase()
        return normalizedUserAnswer.isNotBlank() && (normalizedUserAnswer == normalizedQuestionAnswer || normalizedUserAnswer == normalizedRawCorrectAnswer)
    }

    private fun canonicalizeReviewSubject(subject: String?): String {
        val raw = subject?.trim().orEmpty()
        if (raw.isBlank()) return ""
        return when (raw.lowercase()) {
            "mathématiques", "mathematiques", "maths" -> "Mathématiques"
            "français", "francais" -> "Français"
            "svt" -> "SVT"
            "histoire" -> "Histoire"
            "art", "art/musique", "art / musique", "musique" -> "Art/Musique"
            "anglais", "english", "langues", "langue" -> "Anglais"
            "géographie", "geographie" -> "Géographie"
            "physique-chimie", "physique / chimie", "physique", "chimie" -> "Physique-Chimie"
            "philo/ses", "philo / ses", "philosophie", "ses" -> "Philo/SES"
            "vie & projets", "vie et projets", "projets", "orientation" -> "Vie & Projets"
            "panthéon", "pantheon" -> "Panthéon"
            else -> raw
        }
    }

    private fun resolveReviewSubject(question: QuizQuestion): String {
        val questionSubject = canonicalizeReviewSubject(question.subject)
        if (PantheonConfig.findByMatiere(questionSubject) != null) return questionSubject
        val screenSubject = canonicalizeReviewSubject(currentMatiere)
        if (PantheonConfig.findByMatiere(screenSubject) != null) return screenSubject
        return if (questionSubject.isNotBlank()) questionSubject else screenSubject.ifBlank { currentMatiere }
    }

    private fun resolveReviewGodProfile(question: QuizQuestion, response: GeminiManager.GodResponse? = null): PantheonConfig.GodInfo {
        response?.godName?.takeIf { it.isNotBlank() }?.let { PantheonConfig.findByDivinite(it) }?.let { return it }
        response?.matiere?.takeIf { it.isNotBlank() }?.let { PantheonConfig.findByMatiere(canonicalizeReviewSubject(it)) }?.let { return it }
        PantheonConfig.findByMatiere(resolveReviewSubject(question))?.let { return it }
        return PantheonConfig.findByDivinite("Zeus") ?: PantheonConfig.GODS.first()
    }

    private fun buildQuizCorrectionDivineRequestContext(numero: Int, question: QuizQuestion, userAnswer: String, profile: UserProfile, reviewSubject: String, isCorrect: Boolean): DivineRequestContext {
        val safeUserAnswer = userAnswer.ifBlank { "Aucune réponse" }
        val safeCorrectAnswer = question.correctAnswer.trim()
        val raw = buildString {
            append("review_question=$numero")
            append(" · subject=")
            append(reviewSubject)
            append(" · success=")
            append(isCorrect)
            append(" · userAnswer=")
            append(safeUserAnswer.take(120))
            append(" · correctAnswer=")
            append(safeCorrectAnswer.take(120))
        }
        return DivineRequestContext(
            subject = reviewSubject,
            actionType = DivineActionType.QUIZ_CORRECTION,
            screenSource = "quizresult_review_detail",
            userAge = profile.age,
            userClassLevel = profile.classLevel,
            currentMood = profile.mood,
            successState = isCorrect,
            difficulty = profile.level,
            rawInput = raw,
            validatedSummary = null,
            questionText = question.text,
            userAnswer = safeUserAnswer,
            correctAnswer = safeCorrectAnswer,
            metadata = mapOf(
                "review_index" to numero.toString(),
                "current_screen_subject" to currentMatiere,
                "question_subject" to question.subject.orEmpty(),
                "ui_mode" to "premium_detail_dialog"
            )
        )
    }

    private fun afficherExplicationDieu(numero: Int, question: QuizQuestion, userAnswer: String, isCorrect: Boolean) {
        showLoading()
        lifecycleScope.launch {
            try {
                val profile = withContext(Dispatchers.IO) {
                    try {
                        val db = AppDatabase.getDatabase(this@QuizResultActivity)
                        db.iAristoteDao().getUserProfile() ?: UserProfile(id = 1, age = 15, classLevel = "Terminale", mood = "Prêt", xp = 0, streak = 0, cognitivePattern = "Général")
                    } catch (_: Exception) {
                        UserProfile(id = 1, age = 15, classLevel = "Terminale", mood = "Prêt", xp = 0, streak = 0, cognitivePattern = "Général")
                    }
                }
                val reviewSubject = resolveReviewSubject(question)
                val adaptiveContext = AdaptiveLearningContextResolver.resolve(this@QuizResultActivity, reviewSubject, profile.age, profile.classLevel, profile.mood)
                val response = withContext(Dispatchers.IO) {
                    GodLoreManager.buildCorrectionDialogue(
                        matiere = reviewSubject,
                        question = question,
                        bonneReponse = question.correctAnswer,
                        reponseUser = userAnswer,
                        profile = profile,
                        adaptiveContextNote = adaptiveContext.toPromptNote()
                    )
                }
                val displayGodProfile = resolveReviewGodProfile(question, response)
                val dialogView = LayoutInflater.from(this@QuizResultActivity).inflate(R.layout.dialog_god_explanation, null, false)
                val imgGodPortrait = dialogView.findViewById<ImageView>(R.id.imgGodPortrait)
                val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
                val tvGodExplanation = dialogView.findViewById<TextView>(R.id.tvGodExplanation)
                val tvGodMnemo = dialogView.findViewById<TextView>(R.id.tvGodMnemo)
                val btnDialogConfirm = dialogView.findViewById<TextView>(R.id.btnDialogConfirm)
                val resId = resources.getIdentifier(displayGodProfile.iconResName, "drawable", packageName)
                if (resId != 0) imgGodPortrait.setImageResource(resId)
                tvDialogTitle.text = "${displayGodProfile.divinite} — Question $numero"
                tvGodExplanation.text = response.text
                tvGodMnemo.text = buildString {
                    if (response.mnemo.isNotBlank()) append(response.mnemo.trim())
                    if (response.suggestedAction.isNotBlank()) {
                        if (isNotBlank()) append("\n\n")
                        append("Conseil : ${response.suggestedAction.trim()}")
                    }
                }.trim()
                try {
                    val speakerBtn = ImageButton(this@QuizResultActivity).apply {
                        background = null
                        alpha = 0.88f
                        contentDescription = "Lire l'explication divine"
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        adjustViewBounds = true
                        setPadding(0, 0, 0, 0)
                        layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { gravity = Gravity.END; topMargin = dp(8) }
                        try { setImageResource(R.drawable.ic_speaker_tts) } catch (_: Exception) { setImageResource(android.R.drawable.ic_btn_speak_now) }
                        setOnClickListener {
                            val textToRead = buildString {
                                append("${displayGodProfile.divinite}. ")
                                append("${response.text}. ")
                                if (response.mnemo.isNotBlank()) append("Mnémo : ${response.mnemo}. ")
                                if (response.suggestedAction.isNotBlank()) append("Conseil : ${response.suggestedAction}.")
                            }.trim()
                            tts.speak(textToRead, displayGodProfile.divinite, resolveCurrentAgeForTts())
                        }
                    }
                    (dialogView as? LinearLayout)?.addView(speakerBtn)
                } catch (_: Exception) {}
                val dialog = AlertDialog.Builder(this@QuizResultActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen).setView(dialogView).create()
                btnDialogConfirm.setOnClickListener {
                    try { SoundManager.playSFX(this@QuizResultActivity, R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
                    dialog.dismiss()
                }
                try { SoundManager.playSFX(this@QuizResultActivity, R.raw.sfx_dialogue_blip) } catch (_: Exception) {}
                hideLoading()
                dialog.show()
                dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rpg_dialog)
            } catch (e: Exception) {
                hideLoading()
                Log.e("REVIZEUS", "Erreur explication divine : ${e.message}", e)
                DialogRPGManager.showTechnicalError(activity = this@QuizResultActivity, errorType = TechnicalErrorType.GEMINI_API_ERROR)
            } finally {
                hideLoading()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appliquerFondPremiumQuizResultPourMatiere(currentMatiere)
        try {
            if (!SoundManager.isPlayingMusic()) SoundManager.resumeRememberedMusicDelayed(this, 120L)
        } catch (_: Exception) {}
    }

    override fun handleBackPressed() {
        try { SpeakerTtsHelper.stopAll() } catch (_: Exception) {}
        finish()
    }

    override fun onPause() {
        super.onPause()
        animatedBackgroundHelper?.stop()
        tts.stop()
        typewriterJob?.cancel()
        godAnim.stopSpeaking(binding.imgZeusResult)
        lootAnimationJob?.cancel()
        dominantReactionJob?.cancel()
        rewardCounterAnimator?.cancel()
        eclatCounterAnimator?.cancel()
        ambroisieCounterAnimator?.cancel()
    }

    override fun onDestroy() {
        animatedBackgroundHelper?.release()
        animatedBackgroundHelper = null
        super.onDestroy()
        tts.release()
        typewriterJob?.cancel()
        godAnim.release(binding.imgZeusResult)
        lootAnimationJob?.cancel()
        dominantReactionJob?.cancel()
        rewardCounterAnimator?.cancel()
        eclatCounterAnimator?.cancel()
        ambroisieCounterAnimator?.cancel()
    }
}

package com.revizeus.app

import android.os.Bundle
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.databinding.ActivityQuizBinding
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.QuizQuestion
import com.revizeus.app.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ARCHITECTURE RÉVIZEUS - BASE 2 (Logique de Jeu)
 * Gère l'UI commune (Header) et l'accès aux données de progression.
 *
 * CONSOLIDATION BLOC A :
 * - Chargement réel du profil utilisateur depuis Room
 * - Support d'un contentView custom via ViewBinding sans casser les autres écrans
 * - Méthodes communes pour harmoniser QuizActivity et TrainingQuizActivity
 * - Conservation de l'architecture générale existante
 */
abstract class BaseGameActivity : BaseActivity() {

    protected lateinit var currentUser: UserProfile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hérité de la BaseActivity
        setupImmersiveMode()

        val customContentView = provideCustomContentView()
        if (customContentView != null) {
            setContentView(customContentView)
        } else {
            setContentView(getLayoutResourceId())
        }

        // Initialisation des données (Session)
        loadUserSession()

        // UI Commune
        setupDivineHeader()

        onPostCreateActivity()
    }

    abstract fun getLayoutResourceId(): Int
    abstract fun onPostCreateActivity()

    /**
     * Permet aux écrans modernes basés sur ViewBinding d'injecter leur vue complète
     * sans casser les écrans qui continuent d'utiliser un layout resource classique.
     */
    protected open fun provideCustomContentView(): View? = null

    private fun loadUserSession() {
        // Valeur par défaut immédiate pour éviter tout accès à un lateinit non initialisé.
        currentUser = UserProfile(
            id = 1,
            age = 15,
            classLevel = "Terminale",
            mood = "Prêt",
            xp = 0,
            streak = 0,
            cognitivePattern = "Standard"
        )

        lifecycleScope.launch {
            currentUser = withContext(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getDatabase(this@BaseGameActivity)
                    var profile = db.iAristoteDao().getUserProfile()

                    if (profile == null) {
                        profile = UserProfile(
                            id = 1,
                            age = 15,
                            classLevel = "Terminale",
                            mood = "Prêt",
                            xp = 0,
                            streak = 0,
                            cognitivePattern = "Standard"
                        )
                        db.iAristoteDao().saveUserProfile(profile)
                    }

                    profile
                } catch (_: Exception) {
                    currentUser
                }
            }

            onUserSessionLoaded(currentUser)
        }
    }

    /**
     * Hook optionnel pour les activités qui veulent réagir dès que le vrai profil
     * a été chargé depuis Room.
     */
    protected open fun onUserSessionLoaded(profile: UserProfile) {
        // Optionnel selon les écrans.
    }

    protected fun setupDivineHeader() {
        // Liaison de l'Avatar Chibi et de la barre d'XP (Module RPG)
    }

    // Méthodes de navigation centralisées
    fun goToDailyQuest() { /* Intent */ }
    fun goToOrb() { /* Intent */ }

    // ══════════════════════════════════════════════════════════
    // MÉTHODES COMMUNES QUIZ — BLOC A
    // Harmonise QuizActivity + TrainingQuizActivity sans casser le XML existant.
    // ══════════════════════════════════════════════════════════

    protected fun setupQuizButtons(
        binding: ActivityQuizBinding,
        onAnswerSelected: (String) -> Unit,
        onNext: () -> Unit
    ) {
        binding.btnOptA.setOnClickListener { onAnswerSelected("A") }
        binding.btnOptB.setOnClickListener { onAnswerSelected("B") }
        binding.btnOptC.setOnClickListener { onAnswerSelected("C") }
        binding.btnNext.setOnClickListener { onNext() }
    }

    protected fun showQuizButtons(binding: ActivityQuizBinding) {
        binding.btnOptA.visibility = View.VISIBLE
        binding.btnOptB.visibility = View.VISIBLE
        binding.btnOptC.visibility = View.VISIBLE
    }

    protected fun hideQuizButtons(binding: ActivityQuizBinding) {
        binding.btnOptA.visibility = View.GONE
        binding.btnOptB.visibility = View.GONE
        binding.btnOptC.visibility = View.GONE
    }

    protected fun resetQuizButtonColors(
        binding: ActivityQuizBinding,
        neutralColor: Int
    ) {
        binding.btnOptA.setBackgroundColor(neutralColor)
        binding.btnOptB.setBackgroundColor(neutralColor)
        binding.btnOptC.setBackgroundColor(neutralColor)
    }

    protected fun displayQuizQuestion(
        binding: ActivityQuizBinding,
        scope: LifecycleCoroutineScope,
        godAnim: GodSpeechAnimator?,
        currentJob: Job?,
        question: QuizQuestion,
        currentIndex: Int,
        totalQuestions: Int,
        neutralColor: Int,
        contextForTypewriter: android.content.Context?,
        delayMs: Long = 25L,
        useTypewriter: Boolean = true
    ): Job? {
        binding.tvProgress.text = "Question ${currentIndex + 1} / $totalQuestions"

        currentJob?.cancel()

        val newJob = if (useTypewriter && godAnim != null && contextForTypewriter != null) {
            godAnim.typewriteSimple(
                scope = scope,
                chibiView = binding.ivZeusQuiz,
                textView = binding.tvQuestion,
                text = question.text,
                delayMs = delayMs,
                context = contextForTypewriter
            )
        } else {
            binding.tvQuestion.text = question.text
            null
        }

        binding.btnOptA.text = "A) ${question.optionA}"
        binding.btnOptB.text = "B) ${question.optionB}"
        binding.btnOptC.text = "C) ${question.optionC}"

        resetQuizButtonColors(binding, neutralColor)
        binding.btnNext.visibility = View.GONE
        binding.btnNext.text = "SUIVANT ⚡"

        return newJob
    }

    protected fun applyQuizAnswerFeedback(
        binding: ActivityQuizBinding,
        selectedAnswer: String,
        correctAnswer: String,
        correctColor: Int,
        wrongColor: Int,
        isLastQuestion: Boolean
    ) {
        when (selectedAnswer) {
            "A" -> binding.btnOptA.setBackgroundColor(if (selectedAnswer == correctAnswer) correctColor else wrongColor)
            "B" -> binding.btnOptB.setBackgroundColor(if (selectedAnswer == correctAnswer) correctColor else wrongColor)
            "C" -> binding.btnOptC.setBackgroundColor(if (selectedAnswer == correctAnswer) correctColor else wrongColor)
        }

        if (selectedAnswer != correctAnswer) {
            when (correctAnswer) {
                "A" -> binding.btnOptA.setBackgroundColor(correctColor)
                "B" -> binding.btnOptB.setBackgroundColor(correctColor)
                "C" -> binding.btnOptC.setBackgroundColor(correctColor)
            }
        }

        binding.btnNext.visibility = View.VISIBLE
        if (isLastQuestion) {
            binding.btnNext.text = "VOIR LE VERDICT ⚡"
        }
    }
}
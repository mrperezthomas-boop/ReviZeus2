package com.revizeus.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ============================================================
 * GodSpeechAnimator.kt — RéviZeus
 * Anime le chibi du dieu pendant qu'il parle + gère le
 * typewriter enrichi avec tremblement synchronisé.
 *
 * CONSOLIDATION :
 * - Suppression de la dépendance à LifecycleCoroutineScope
 * - Utilisation d'un CoroutineScope standard
 * - Arrêt systématique du tremblement à la fin / à l'annulation
 * - Conservation de toutes les mécaniques existantes
 * ============================================================
 */
class GodSpeechAnimator {

    private var shakeAnimator: AnimatorSet? = null

    /**
     * BLOC A :
     * On mémorise désormais les jobs de typewriter actifs par TextView
     * afin d'éviter les superpositions de textes quand un écran relance
     * une nouvelle phrase avant la fin de la précédente.
     */
    private val activeTypewriterJobs = mutableMapOf<Int, Job>()

    /**
     * BLOC BLIP FANTÔME :
     * chaque TextView de dialogue possède maintenant un token de session.
     * Dès qu'une nouvelle phrase démarre, l'ancienne session devient invalide
     * et ne peut plus ni écrire du texte, ni jouer des blips, ni stopper le
     * tremblement de la nouvelle phrase.
     */
    private val activeTypewriterSessions = mutableMapOf<Int, Long>()
    private var sessionCounter: Long = 0L

    fun startSpeaking(
        chibiView: View,
        intensity: Float = 2.5f,
        speedMs: Long = 120L
    ) {
        shakeAnimator?.cancel()
        shakeAnimator = null

        val density = chibiView.resources.displayMetrics.density
        val amp = intensity * density

        val shakeX = ObjectAnimator.ofFloat(
            chibiView,
            View.TRANSLATION_X,
            0f, amp, 0f, -amp, 0f
        ).apply {
            duration = speedMs * 2
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
        }

        val shakeY = ObjectAnimator.ofFloat(
            chibiView,
            View.TRANSLATION_Y,
            0f, -amp * 0.4f, 0f, amp * 0.4f, 0f
        ).apply {
            duration = (speedMs * 2.5f).toLong()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = speedMs / 3
        }

        val shakeRot = ObjectAnimator.ofFloat(
            chibiView,
            View.ROTATION,
            0f, 1.2f, 0f, -1.2f, 0f
        ).apply {
            duration = (speedMs * 3f).toLong()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = speedMs / 2
        }

        shakeAnimator = AnimatorSet().apply {
            playTogether(shakeX, shakeY, shakeRot)
            start()
        }
    }

    fun stopSpeaking(chibiView: View) {
        shakeAnimator?.cancel()
        shakeAnimator = null

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(chibiView, View.TRANSLATION_X, chibiView.translationX, 0f),
                ObjectAnimator.ofFloat(chibiView, View.TRANSLATION_Y, chibiView.translationY, 0f),
                ObjectAnimator.ofFloat(chibiView, View.ROTATION, chibiView.rotation, 0f)
            )
            duration = 150L
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    fun impactShake(chibiView: View, onDone: (() -> Unit)? = null) {
        shakeAnimator?.cancel()
        shakeAnimator = null

        val density = chibiView.resources.displayMetrics.density
        val amp = 7f * density

        val impactX = ObjectAnimator.ofFloat(
            chibiView,
            View.TRANSLATION_X,
            0f, amp, -amp, amp * 0.5f, -amp * 0.5f, 0f
        ).apply {
            duration = 400L
            interpolator = AccelerateDecelerateInterpolator()
        }

        val impactRot = ObjectAnimator.ofFloat(
            chibiView,
            View.ROTATION,
            0f, 4f, -4f, 2f, -2f, 0f
        ).apply {
            duration = 400L
            interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(impactX, impactRot)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onDone?.invoke()
                }
            })
            start()
        }
    }

    /**
     * Typewriter enrichi avec tremblement synchronisé.
     * Utilise maintenant un CoroutineScope standard pour éviter
     * toute dépendance directe à lifecycleScope.
     */
    fun typewriteWithShake(
        scope: CoroutineScope,
        chibiView: View,
        textView: TextView,
        text: String,
        delayMs: Long = 35L,
        intensity: Float = 2.5f,
        onChar: (() -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ): Job {
        cancelExistingTypewriter(textView)
        SoundManager.stopAllDialogueBlips()

        val key = resolveTextViewKey(textView)
        val sessionToken = beginTypewriterSession(key)
        startSpeaking(chibiView, intensity = intensity)

        val job = if (text.isBlank()) {
            scope.launch {
                try {
                    if (isTypewriterSessionUsable(textView, chibiView, key, sessionToken)) {
                        textView.text = ""
                        onComplete?.invoke()
                    }
                } finally {
                    if (isTypewriterSessionActive(key, sessionToken)) {
                        stopSpeaking(chibiView)
                    }
                }
            }
        } else {
            scope.launch {
                try {
                    if (!isTypewriterSessionUsable(textView, chibiView, key, sessionToken)) {
                        return@launch
                    }

                    textView.text = ""

                    for (i in text.indices) {
                        if (!isActive || !isTypewriterSessionUsable(textView, chibiView, key, sessionToken)) {
                            break
                        }

                        textView.text = text.substring(0, i + 1)

                        if (isTypewriterSessionUsable(textView, chibiView, key, sessionToken)) {
                            onChar?.invoke()
                        }

                        delay(delayMs)
                    }

                    if (isTypewriterSessionUsable(textView, chibiView, key, sessionToken)) {
                        onComplete?.invoke()
                    }
                } finally {
                    if (isTypewriterSessionActive(key, sessionToken)) {
                        stopSpeaking(chibiView)
                    }
                }
            }
        }

        return registerTypewriterJob(textView, sessionToken, job)
    }

    /**
     * Variante simple conservée pour compatibilité avec le reste du projet.
     */
    fun typewriteSimple(
        scope: CoroutineScope,
        chibiView: View,
        textView: TextView,
        text: String,
        delayMs: Long = 35L,
        context: Context? = null,
        onComplete: (() -> Unit)? = null
    ): Job {
        cancelExistingTypewriter(textView)
        SoundManager.stopAllDialogueBlips()

        val key = resolveTextViewKey(textView)
        val sessionToken = beginTypewriterSession(key)
        startSpeaking(chibiView)

        val job = if (text.isBlank()) {
            scope.launch {
                try {
                    if (isTypewriterSessionUsable(textView, chibiView, key, sessionToken)) {
                        textView.text = ""
                        onComplete?.invoke()
                    }
                } finally {
                    if (isTypewriterSessionActive(key, sessionToken)) {
                        stopSpeaking(chibiView)
                    }
                }
            }
        } else {
            scope.launch {
                try {
                    if (!isTypewriterSessionUsable(textView, chibiView, key, sessionToken)) {
                        return@launch
                    }

                    textView.text = ""

                    for (i in text.indices) {
                        if (!isActive || !isTypewriterSessionUsable(textView, chibiView, key, sessionToken)) {
                            break
                        }

                        textView.text = text.substring(0, i + 1)

                        if (context != null && isTypewriterSessionUsable(textView, chibiView, key, sessionToken)) {
                            try {
                                SoundManager.playSFXDialogueBlip(context, R.raw.sfx_dialogue_blip)
                            } catch (_: Exception) {
                            }
                        }

                        delay(delayMs)
                    }

                    if (isTypewriterSessionUsable(textView, chibiView, key, sessionToken)) {
                        onComplete?.invoke()
                    }
                } finally {
                    if (isTypewriterSessionActive(key, sessionToken)) {
                        stopSpeaking(chibiView)
                    }
                }
            }
        }

        return registerTypewriterJob(textView, sessionToken, job)
    }

    /**
     * Libère proprement l'état visuel.
     */
    fun release(chibiView: View) {
        SoundManager.stopAllDialogueBlips()

        activeTypewriterJobs.values.toList().forEach { job ->
            try {
                job.cancel()
            } catch (_: Exception) {
            }
        }
        activeTypewriterJobs.clear()
        activeTypewriterSessions.clear()

        shakeAnimator?.cancel()
        shakeAnimator = null

        try {
            chibiView.animate().cancel()
        } catch (_: Exception) {
        }

        chibiView.translationX = 0f
        chibiView.translationY = 0f
        chibiView.rotation = 0f
    }

    private fun registerTypewriterJob(textView: TextView, sessionToken: Long, job: Job): Job {
        val key = resolveTextViewKey(textView)
        activeTypewriterJobs[key] = job

        job.invokeOnCompletion {
            if (activeTypewriterJobs[key] === job) {
                activeTypewriterJobs.remove(key)
            }
            if (activeTypewriterSessions[key] == sessionToken) {
                activeTypewriterSessions.remove(key)
            }
        }

        return job
    }

    private fun cancelExistingTypewriter(textView: TextView) {
        val key = resolveTextViewKey(textView)
        activeTypewriterSessions.remove(key)
        activeTypewriterJobs.remove(key)?.let { existingJob ->
            try {
                existingJob.cancel()
            } catch (_: Exception) {
            }
        }
        SoundManager.stopAllDialogueBlips()
    }

    private fun beginTypewriterSession(key: Int): Long {
        sessionCounter += 1L
        activeTypewriterSessions[key] = sessionCounter
        return sessionCounter
    }

    private fun isTypewriterSessionActive(key: Int, sessionToken: Long): Boolean {
        return activeTypewriterSessions[key] == sessionToken
    }

    private fun isTypewriterSessionUsable(
        textView: TextView,
        chibiView: View,
        key: Int,
        sessionToken: Long
    ): Boolean {
        if (!isTypewriterSessionActive(key, sessionToken)) return false
        if (!textView.isAttachedToWindow || !chibiView.isAttachedToWindow) return false
        if (textView.visibility != View.VISIBLE) return false
        if (chibiView.visibility != View.VISIBLE) return false
        return true
    }

    private fun resolveTextViewKey(textView: TextView): Int {
        return if (textView.id != View.NO_ID) {
            textView.id
        } else {
            System.identityHashCode(textView)
        }
    }
}

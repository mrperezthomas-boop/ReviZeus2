package com.revizeus.app

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet

/**
 * ============================================================
 * SpeakerTtsHelper.kt — RéviZeus
 * Moteur de lecture audio des dialogues, questions et réponses
 * ============================================================
 *
 * VERSION CONSOLIDÉE :
 * ✅ Tap sur le speaker pendant la lecture = stop immédiat
 * ✅ Ducking BGM automatique pendant la voix
 * ✅ Restauration propre du volume après la voix
 * ✅ Arrêt global possible depuis BaseActivity quand on fait retour
 * ✅ Aucune rupture des appels existants : speak(), stop(), release()
 *
 * IMPORTANT :
 * Le helper gère maintenant lui-même la coexistence avec SoundManager
 * sans changer l’architecture des Activities existantes.
 * ============================================================
 */
class SpeakerTtsHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var isSpeakingNow = false
    private var lastTargetMusicVolume = 1f
    private var currentUtteranceId: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Callback optionnel déclenché quand la lecture se termine. */
    var onSpeakingDone: (() -> Unit)? = null

    init {
        INSTANCES.add(this)

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.FRENCH)
                isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

                if (!isReady) {
                    tts?.setLanguage(Locale.getDefault())
                    isReady = true
                }

                try {
                    tts?.setSpeechRate(0.96f)
                    tts?.setPitch(1.0f)
                } catch (e: Exception) {
                    Log.w("SpeakerTts", "Impossible d'appliquer les réglages vocaux initiaux", e)
                }

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        mainHandler.post {
                            if (utteranceId == null || utteranceId == currentUtteranceId) {
                                isSpeakingNow = true
                            }
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post {
                            finishSpeakingInternal(utteranceId)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post {
                            finishSpeakingInternal(utteranceId)
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        mainHandler.post {
                            finishSpeakingInternal(utteranceId)
                        }
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        mainHandler.post {
                            finishSpeakingInternal(utteranceId)
                        }
                    }
                })

                Log.d("SpeakerTts", "TTS initialisé. isReady=$isReady")
            } else {
                Log.w("SpeakerTts", "Initialisation TTS échouée. status=$status")
            }
        }
    }

    /**
     * Lit le texte à voix haute.
     * Si une lecture est déjà en cours, un nouvel appui coupe immédiatement
     * la voix au lieu de la relancer. C'est le comportement demandé en UX.
     */
    fun speak(text: String, godName: String? = null, profileAge: Int? = null) {
        if (!isReady) return
        if (tts == null) return

        if (isSpeaking()) {
            stop()
            return
        }

        val clean = text
            .replace(Regex("<[^>]+>"), "")
            .replace("...", ".")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (clean.isBlank()) return

        /**
         * BLOC A — correction compile + sécurisation du ducking.
         * On mémorise le volume cible avant de le baisser afin de le restaurer
         * proprement à la fin de la lecture, même si la BGM n'était pas active.
         */
        try {
            lastTargetMusicVolume = SoundManager.getMusicVolume().coerceIn(0f, 1f)
        } catch (_: Exception) {
            lastTargetMusicVolume = 1f
        }

        try {
            val duckedVolume = (lastTargetMusicVolume * 0.28f).coerceIn(0.08f, 1f)
            SoundManager.setMusicVolume(duckedVolume)
        } catch (e: Exception) {
            Log.w("SpeakerTts", "Ducking BGM impossible", e)
        }

        try {
            configureVoiceForContext(godName, profileAge)
            currentUtteranceId = UUID.randomUUID().toString()

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }

            tts?.speak(
                clean,
                TextToSpeech.QUEUE_FLUSH,
                params,
                currentUtteranceId
            )
        } catch (e: Exception) {
            Log.e("SpeakerTts", "Erreur speak : ${e.message}")
            restoreMusicVolume()
        }
    }

    /**
     * Arrête la lecture en cours sans libérer le moteur.
     * Appelé manuellement ou sur changement d'écran.
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w("SpeakerTts", "stop() TTS a échoué", e)
        }
        finishSpeakingInternal()
    }

    /**
     * Libère complètement le moteur TTS.
     */
    fun release() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w("SpeakerTts", "shutdown() TTS a échoué", e)
        }
        tts = null
        isReady = false
        currentUtteranceId = null
        mainHandler.removeCallbacksAndMessages(null)
        INSTANCES.remove(this)
    }

    fun isReady(): Boolean = isReady

    fun isSpeaking(): Boolean = isSpeakingNow

    private fun finishSpeakingInternal(finishedUtteranceId: String? = null) {
        /**
         * BLOC A :
         * On ignore les callbacks de fin qui appartiennent à une ancienne
         * utterance déjà remplacée, pour éviter une restauration prématurée
         * du volume ou un faux onSpeakingDone.
         */
        if (
            finishedUtteranceId != null &&
            currentUtteranceId != null &&
            finishedUtteranceId != currentUtteranceId
        ) {
            return
        }

        val wasSpeaking = isSpeakingNow
        isSpeakingNow = false
        currentUtteranceId = null
        restoreMusicVolume()

        if (wasSpeaking) {
            try {
                onSpeakingDone?.invoke()
            } catch (_: Exception) {
            }
        }
    }

    private fun configureVoiceForContext(godName: String?, profileAge: Int?) {
        try {
            val godKey = godName.orEmpty().trim().uppercase()
            val age = profileAge ?: 15

            val baseRate = when {
                age <= 10 -> 0.86f
                age <= 13 -> 0.90f
                age <= 16 -> 0.95f
                else -> 1.0f
            }

            val basePitch = when (godKey) {
                "ZEUS" -> 0.90f
                "ATHENA", "ATHÉNA" -> 0.98f
                "HERMES", "HERMÈS" -> 1.08f
                "ARES", "ARÈS" -> 0.94f
                "POSEIDON" -> 0.92f
                "HEPHAISTOS", "HÉPHAÏSTOS" -> 0.88f
                "APHRODITE" -> 1.10f
                "APOLLON" -> 1.02f
                "DEMETER", "DÉMÉTER" -> 0.96f
                "PROMETHEE", "PROMÉTHÉE" -> 0.97f
                else -> 1.0f
            }

            val finalRate = when (godKey) {
                "HERMES", "HERMÈS" -> (baseRate + 0.08f).coerceAtMost(1.12f)
                "ZEUS" -> (baseRate - 0.04f).coerceAtLeast(0.80f)
                "APOLLON" -> (baseRate - 0.02f).coerceAtLeast(0.82f)
                else -> baseRate
            }

            tts?.setSpeechRate(finalRate)
            tts?.setPitch(basePitch)
        } catch (_: Exception) {
        }
    }

    private fun restoreMusicVolume() {
        try {
            SoundManager.setMusicVolume(lastTargetMusicVolume.coerceIn(0f, 1f))
        } catch (e: Exception) {
            Log.w("SpeakerTts", "Restauration volume musique impossible", e)
        }
    }

    companion object {
        /**
         * Registre global pour arrêter toutes les voix si l'utilisateur fait retour
         * pendant un dialogue ou change brutalement d'écran.
         */
        private val INSTANCES = CopyOnWriteArraySet<SpeakerTtsHelper>()

        fun stopAll() {
            INSTANCES.forEach {
                try {
                    it.stop()
                } catch (_: Exception) {
                }
            }
        }

        fun releaseAll() {
            INSTANCES.forEach {
                try {
                    it.release()
                } catch (_: Exception) {
                }
            }
            INSTANCES.clear()
        }
    }
}

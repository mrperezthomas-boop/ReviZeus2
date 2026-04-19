package com.revizeus.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * ============================================================
 * SoundManager.kt — RéviZeus v9+  ✅ ÉTENDU SANS CASSER L'EXISTANT
 * Gestionnaire audio centralisé — "L'Écho de l'Olympe"
 *
 * Gère :
 *   - La musique de fond (BGM) via MediaPlayer principal
 *   - Les effets sonores courts (SFX) via SoundPool
 *   - Le scan Oracle en boucle via un MediaPlayer secondaire dédié
 *
 * EXTENSION CONSERVATIVE :
 *   - Aucun renommage des méthodes existantes
 *   - Aucun changement destructeur sur les appels actuels
 *   - Ajout d'un second canal audio pour permettre :
 *       BGM + scan loop simultanés
 *   - Ajout d'outils de transition sûrs pour les changements d'écran
 *   - Ajout d'une reprise différée pour respecter les transitions RéviZeus
 * ============================================================
 */
object SoundManager {
    private const val TAG = "REVIZEUS_SOUND"

    // ── MediaPlayer principal pour la BGM ─────────────────────
    private var mediaPlayer: MediaPlayer? = null
    private var currentMusicResId: Int = -1

    // ── MediaPlayer secondaire pour le scan en boucle ─────────
    private var scanLoopPlayer: MediaPlayer? = null
    private var currentScanLoopResId: Int = -1

    // ── SoundPool pour les SFX courts ─────────────────────────
    private var soundPool: SoundPool? = null
    private val loadedSounds = mutableMapOf<Int, Int>()

    /**
     * BLOC A — stabilisation des chargements asynchrones SoundPool.
     * On sépare désormais :
     * - les sons connus (loadedSounds)
     * - les samples réellement prêts (readySoundIds)
     * - les requêtes de lecture faites avant la fin du chargement
     *
     * Cela évite qu'un second appel sur un sample encore en cours de load
     * soit perdu silencieusement.
     */
    private val readySoundIds = mutableSetOf<Int>()

    /**
     * BLOC BLIP FANTÔME :
     * on mémorise désormais les lectures en attente avec leur nature.
     * Les blips de dialogue reçoivent une génération logique pour qu'un
     * ancien écran ne puisse plus jouer ses sons une fois invalidé.
     */
    private data class PendingSoundPlay(
        val volume: Float,
        val isDialogueManaged: Boolean,
        val dialogueGeneration: Long
    )

    private val pendingLoadedSoundPlays = mutableMapOf<Int, MutableList<PendingSoundPlay>>()
    private val activeDialogueStreamIds = mutableListOf<Int>()
    private var dialogueBlipGeneration: Long = 0L

    // ── Volumes globaux ───────────────────────────────────────
    private var currentMusicVol: Float = 0.8f
    private var currentScanLoopVol: Float = 0.35f
    private var globalSfxVolume: Float = 1.0f

    // ── Mémoire légère pour les transitions entre écrans ─────
    // On mémorise la dernière BGM voulue pour pouvoir la relancer proprement
    // après une transition d'Activity sans casser les appels existants.
    private var pendingMusicResId: Int = -1
    private var pendingMusicLoop: Boolean = true

    // ── Handler principal pour les relances différées ────────
    private val mainHandler = Handler(Looper.getMainLooper())
    private var delayedMusicRunnable: Runnable? = null

    // ══════════════════════════════════════════════════════════
    // MUSIQUE DE FOND (BGM)
    // ══════════════════════════════════════════════════════════

    /**
     * Joue une musique de fond.
     * Si la même musique est déjà en cours, ne fait rien.
     * Si une autre musique jouait, elle est arrêtée proprement.
     *
     * IMPORTANT :
     * - N'interrompt PAS le canal scanLoopPlayer.
     * - Permet donc BGM + scan loop simultanés.
     */
    fun playMusic(context: Context, resId: Int, loop: Boolean = true) {
        cancelDelayedMusic()

        // On mémorise la dernière musique explicitement demandée.
        pendingMusicResId = resId
        pendingMusicLoop = loop

        if (resId == currentMusicResId && mediaPlayer?.isPlaying == true) return

        mediaPlayer?.apply {
            try {
                if (isPlaying) stop()
            } catch (_: Exception) {
            }
            try {
                release()
            } catch (_: Exception) {
            }
        }
        mediaPlayer = null
        currentMusicResId = -1

        try {
            mediaPlayer = MediaPlayer.create(context.applicationContext, resId)?.apply {
                isLooping = loop
                setVolume(currentMusicVol, currentMusicVol)
                start()
            }
            currentMusicResId = resId
        } catch (e: Exception) {
            Log.w(TAG, "playMusic failed for resId=$resId", e)
        }
    }

    /**
     * Met en pause la musique de fond.
     */
    fun pauseMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.w(TAG, "pauseMusic failed", e)
        }
    }

    /**
     * Reprend la musique de fond si elle était en pause.
     */
    fun resumeMusic() {
        try {
            val mp = mediaPlayer
            if (mp != null && !mp.isPlaying) {
                mp.setVolume(currentMusicVol, currentMusicVol)
                mp.start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "resumeMusic failed", e)
        }
    }

    /**
     * Arrête uniquement la BGM principale.
     * Le scan loop reste volontairement indépendant.
     */
    fun stopMusic() {
        cancelDelayedMusic()

        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "stopMusic failed", e)
        }
        mediaPlayer = null
        currentMusicResId = -1
    }

    /**
     * Change le volume de la BGM en temps réel.
     */
    fun setVolume(volume: Float) {
        currentMusicVol = volume.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(currentMusicVol, currentMusicVol)
        } catch (_: Exception) {
        }
    }

    /**
     * Alias explicite pour SettingsActivity.
     */
    fun setMusicVolume(volume: Float) = setVolume(volume)

    /**
     * Retourne le volume musical actuellement mémorisé.
     */
    fun getMusicVolume(): Float = currentMusicVol

    /**
     * Change le volume global de tous les SFX SoundPool.
     */
    fun setSfxVolume(volume: Float) {
        globalSfxVolume = volume.coerceIn(0f, 1f)
    }

    /**
     * Retourne true si une musique principale est en cours.
     */
    fun isPlayingMusic(): Boolean =
        try {
            mediaPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }

    /**
     * Retourne l'id de ressource de la musique en cours.
     */
    fun getCurrentMusicResId(): Int = currentMusicResId

    // ══════════════════════════════════════════════════════════
    // EXTENSIONS CONSERVATIVES — TRANSITIONS D'ÉCRANS
    // ══════════════════════════════════════════════════════════

    /**
     * Mémorise la musique voulue sans la jouer immédiatement.
     * Utile quand un écran sait déjà quelle musique doit reprendre ensuite.
     */
    fun rememberMusic(resId: Int, loop: Boolean = true) {
        pendingMusicResId = resId
        pendingMusicLoop = loop
    }

    /**
     * Relance une musique après un délai.
     *
     * Cas d'usage RéviZeus :
     * - une Activity ferme sa musique dans onPause()
     * - l'écran suivant lance sa propre BGM légèrement après
     * - cela évite les conflits entre stopMusic() et playMusic()
     *
     * Le délai par défaut respecte la règle de transition système évoquée
     * dans ton référentiel de projet.
     */
    fun playMusicDelayed(
        context: Context,
        resId: Int,
        delayMs: Long = 300L,
        loop: Boolean = true
    ) {
        cancelDelayedMusic()

        pendingMusicResId = resId
        pendingMusicLoop = loop

        delayedMusicRunnable = Runnable {
            playMusic(context.applicationContext, resId, loop)
        }
        mainHandler.postDelayed(delayedMusicRunnable!!, delayMs)
    }

    /**
     * Rejoue la dernière musique mémorisée après un délai.
     * Très pratique pour reprendre automatiquement une BGM d'ambiance.
     */
    fun resumeRememberedMusicDelayed(
        context: Context,
        delayMs: Long = 300L
    ) {
        val resId = pendingMusicResId
        if (resId == -1) return
        playMusicDelayed(context.applicationContext, resId, delayMs, pendingMusicLoop)
    }

    /**
     * Retourne l'id de la dernière musique mémorisée.
     * Permet de déboguer facilement quel écran a réservé quelle BGM.
     */
    fun getRememberedMusicResId(): Int = pendingMusicResId

    /**
     * Annule une relance différée éventuellement programmée.
     */
    fun cancelDelayedMusic() {
        delayedMusicRunnable?.let { runnable ->
            try {
                mainHandler.removeCallbacks(runnable)
            } catch (_: Exception) {
            }
        }
        delayedMusicRunnable = null
    }

    // ══════════════════════════════════════════════════════════
    // CANAL SECONDAIRE — SCAN LOOP
    // ══════════════════════════════════════════════════════════

    /**
     * Joue un son de scan en boucle sur un canal secondaire.
     * Conçu pour fonctionner par-dessus la BGM principale.
     */
    fun playLoopingScan(context: Context, resId: Int, volume: Float = currentScanLoopVol) {
        val safeVolume = volume.coerceIn(0f, 1f)
        currentScanLoopVol = safeVolume

        if (resId == currentScanLoopResId && scanLoopPlayer?.isPlaying == true) {
            try {
                scanLoopPlayer?.setVolume(currentScanLoopVol, currentScanLoopVol)
            } catch (_: Exception) {
            }
            return
        }

        stopLoopingScan()

        try {
            scanLoopPlayer = MediaPlayer.create(context.applicationContext, resId)?.apply {
                isLooping = true
                setVolume(currentScanLoopVol, currentScanLoopVol)
                start()
            }
            currentScanLoopResId = resId
        } catch (e: Exception) {
            Log.w(TAG, "playLoopingScan failed for resId=$resId", e)
        }
    }

    /**
     * Arrête uniquement le scan loop secondaire.
     */
    fun stopLoopingScan() {
        try {
            scanLoopPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "stopLoopingScan failed", e)
        }
        scanLoopPlayer = null
        currentScanLoopResId = -1
    }

    /**
     * Ajuste le volume du canal scan secondaire.
     */
    fun setScanLoopVolume(volume: Float) {
        currentScanLoopVol = volume.coerceIn(0f, 1f)
        try {
            scanLoopPlayer?.setVolume(currentScanLoopVol, currentScanLoopVol)
        } catch (_: Exception) {
        }
    }

    /**
     * Retourne true si le scan loop secondaire est en lecture.
     */
    fun isPlayingScanLoop(): Boolean =
        try {
            scanLoopPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }

    // ══════════════════════════════════════════════════════════
    // EFFETS SONORES (SFX) — SOUNDPOOL
    // ══════════════════════════════════════════════════════════

    /**
     * Initialise le SoundPool si nécessaire.
     */
    private fun ensureSoundPool() {
        if (soundPool != null) return

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build().apply {
                setOnLoadCompleteListener { pool, sampleId, status ->
                    if (status != 0) {
                        pendingLoadedSoundPlays.remove(sampleId)
                        return@setOnLoadCompleteListener
                    }

                    readySoundIds.add(sampleId)

                    val queuedRequests = pendingLoadedSoundPlays.remove(sampleId).orEmpty()
                    queuedRequests.forEach { pendingRequest ->
                        if (pendingRequest.isDialogueManaged && pendingRequest.dialogueGeneration != dialogueBlipGeneration) {
                            return@forEach
                        }

                        try {
                            val streamId = pool.play(sampleId, pendingRequest.volume, pendingRequest.volume, 1, 0, 1f)
                            if (pendingRequest.isDialogueManaged && streamId != 0) {
                                registerDialogueStreamId(streamId)
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
    }

    /**
     * Joue un effet sonore au volume normal.
     */
    fun playSFX(context: Context, resId: Int) {
        playSFXAtVolumeInternal(context, resId, 1.0f * globalSfxVolume, forceDialogueManaged = false)
    }

    /**
     * Joue un effet sonore à volume réduit.
     */
    fun playSFXLow(context: Context, resId: Int) {
        playSFXAtVolumeInternal(context, resId, 0.15f * globalSfxVolume, forceDialogueManaged = false)
    }

    /**
     * Blip typewriter Zeus.
     */
    fun playSFXDialogueBlip(context: Context, resId: Int) {
        playSFXAtVolumeInternal(context, resId, 0.08f * globalSfxVolume, forceDialogueManaged = true)
    }

    /**
     * Blip ultra discret.
     */
    fun playSFXChatBlip(context: Context, resId: Int) {
        playSFXAtVolumeInternal(context, resId, 0.04f * globalSfxVolume, forceDialogueManaged = true)
    }

    /**
     * Joue un SFX à volume personnalisé.
     */
    fun playSFXAtVolume(context: Context, resId: Int, volume: Float) {
        playSFXAtVolumeInternal(context, resId, volume, forceDialogueManaged = false)
    }

    /**
     * Invalide immédiatement tous les blips de dialogue encore en attente
     * ou encore en cours. À appeler dès qu'un écran quitte son dialogue RPG.
     */
    fun stopAllDialogueBlips() {
        dialogueBlipGeneration += 1L
        stopTrackedDialogueStreams()

        val iterator = pendingLoadedSoundPlays.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.value.removeAll { it.isDialogueManaged }
            if (entry.value.isEmpty()) {
                iterator.remove()
            }
        }
    }

    private fun playSFXAtVolumeInternal(
        context: Context,
        resId: Int,
        volume: Float,
        forceDialogueManaged: Boolean
    ) {
        try {
            ensureSoundPool()
            val pool = soundPool ?: return
            val v = volume.coerceIn(0f, 1f)
            val isDialogueManaged = forceDialogueManaged || isDialogueManagedResource(resId)
            val generationSnapshot = if (isDialogueManaged) dialogueBlipGeneration else -1L

            val soundId = loadedSounds[resId]
            when {
                soundId != null && readySoundIds.contains(soundId) -> {
                    if (isDialogueManaged && generationSnapshot != dialogueBlipGeneration) {
                        return
                    }

                    val streamId = pool.play(soundId, v, v, 1, 0, 1f)
                    if (isDialogueManaged && streamId != 0) {
                        registerDialogueStreamId(streamId)
                    }
                }

                soundId != null -> {
                    pendingLoadedSoundPlays
                        .getOrPut(soundId) { mutableListOf() }
                        .add(
                            PendingSoundPlay(
                                volume = v,
                                isDialogueManaged = isDialogueManaged,
                                dialogueGeneration = generationSnapshot
                            )
                        )
                }

                else -> {
                    val newSoundId = pool.load(context.applicationContext, resId, 1)
                    loadedSounds[resId] = newSoundId
                    pendingLoadedSoundPlays
                        .getOrPut(newSoundId) { mutableListOf() }
                        .add(
                            PendingSoundPlay(
                                volume = v,
                                isDialogueManaged = isDialogueManaged,
                                dialogueGeneration = generationSnapshot
                            )
                        )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "playSFXAtVolumeInternal failed for resId=$resId", e)
        }
    }

    private fun isDialogueManagedResource(resId: Int): Boolean {
        return resId == R.raw.sfx_dialogue_blip
    }

    private fun registerDialogueStreamId(streamId: Int) {
        activeDialogueStreamIds.add(streamId)

        if (activeDialogueStreamIds.size > 24) {
            val overflowStreamId = activeDialogueStreamIds.removeAt(0)
            try {
                soundPool?.stop(overflowStreamId)
            } catch (_: Exception) {
            }
        }
    }

    private fun stopTrackedDialogueStreams() {
        val pool = soundPool ?: run {
            activeDialogueStreamIds.clear()
            return
        }

        activeDialogueStreamIds.toList().forEach { streamId ->
            try {
                pool.stop(streamId)
            } catch (_: Exception) {
            }
        }
        activeDialogueStreamIds.clear()
    }

    // ══════════════════════════════════════════════════════════
    // LIBÉRATION DES RESSOURCES
    // ══════════════════════════════════════════════════════════

    /**
     * Libère toutes les ressources audio.
     */
    fun release() {
        cancelDelayedMusic()

        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "release: mediaPlayer cleanup failed", e)
        }
        mediaPlayer = null
        currentMusicResId = -1

        try {
            scanLoopPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "release: scanLoopPlayer cleanup failed", e)
        }
        scanLoopPlayer = null
        currentScanLoopResId = -1

        stopAllDialogueBlips()

        try {
            soundPool?.setOnLoadCompleteListener(null)
            soundPool?.release()
        } catch (e: Exception) {
            Log.w(TAG, "release: soundPool cleanup failed", e)
        }
        soundPool = null
        loadedSounds.clear()
        readySoundIds.clear()
        pendingLoadedSoundPlays.clear()
        activeDialogueStreamIds.clear()
        dialogueBlipGeneration = 0L

        /**
         * BLOC A :
         * Lors d'une vraie libération globale, on nettoie aussi la mémoire
         * des transitions pour éviter qu'une ancienne BGM ressuscite.
         */
        pendingMusicResId = -1
        pendingMusicLoop = true
    }
}

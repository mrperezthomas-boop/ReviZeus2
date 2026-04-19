package com.revizeus.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.revizeus.app.databinding.ActivityIntroVideoBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ═══════════════════════════════════════════════════════════════
 * INTRO VIDEO ACTIVITY — Cinématique RéviZeus avec son vidéo
 * ═══════════════════════════════════════════════════════════════
 *
 * Correctif appliqué :
 * - On conserve le son natif de la vidéo revizeus_intro
 * - On coupe agressivement toute musique parasite héritée de l'écran précédent
 * - On sécurise l'écran sur plusieurs points du cycle de vie pour empêcher
 *   qu'une BGM d'AvatarActivity ou d'un autre écran se relance par-dessus
 *
 * Intention :
 * - vidéo = garde son audio propre
 * - aucune musique de fond parasite ne doit survivre derrière
 * ═══════════════════════════════════════════════════════════════
 */
class IntroVideoActivity : BaseActivity() {

    private lateinit var binding: ActivityIntroVideoBinding
    private var introPlayer: ExoPlayer? = null
    private var skipArmed = false
    private var skipResetJob: Job? = null
    private var hasNavigated = false

    private val introPlayerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                goToMood()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Coupe immédiate de toute musique résiduelle au tout début de l'écran.
        // On garde ensuite le son de la vidéo lui-même.
        SoundManager.stopMusic()

        setupIntroPlayer()
        setupSkipOverlay()
    }

    override fun onStart() {
        super.onStart()

        // Sécurité supplémentaire :
        // si une musique retardée de l'écran précédent tente de repartir quand
        // l'Activity devient visible, on l'écrase ici.
        SoundManager.stopMusic()
    }

    override fun onResume() {
        super.onResume()

        // Garde-fou principal :
        // certains parasites audio reviennent au moment du focus / resume.
        // On stoppe uniquement la musique de fond, puis ExoPlayer continue
        // normalement la piste audio intégrée à la vidéo.
        SoundManager.stopMusic()
    }

    private fun setupIntroPlayer() {
        introPlayer = ExoPlayer.Builder(this).build().apply {
            // IMPORTANT :
            // on garde le son de la vidéo, car le problème visé ici n'est pas
            // l'audio natif de revizeus_intro mais la musique héritée d'un écran précédent.
            volume = 1f
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(introPlayerListener)
            setMediaItem(
                MediaItem.fromUri(
                    Uri.parse("android.resource://$packageName/${R.raw.revizeus_intro}")
                )
            )
            prepare()
        }

        binding.pvIntroVideo.player = introPlayer
        binding.pvIntroVideo.useController = false
        binding.pvIntroVideo.setShutterBackgroundColor(Color.BLACK)
    }

    private fun setupSkipOverlay() {
        binding.root.setOnClickListener {
            if (!skipArmed) {
                armSkip()
            } else {
                goToMood()
            }
        }
    }

    private fun armSkip() {
        skipArmed = true
        binding.tvSkipHint.visibility = View.VISIBLE
        binding.tvSkipHint.alpha = 0f
        binding.tvSkipHint.animate().alpha(1f).setDuration(160L).start()

        skipResetJob?.cancel()
        skipResetJob = lifecycleScope.launch {
            delay(1800L)
            disarmSkip()
        }
    }

    private fun disarmSkip() {
        skipArmed = false
        binding.tvSkipHint.animate()
            .alpha(0f)
            .setDuration(160L)
            .withEndAction {
                binding.tvSkipHint.visibility = View.GONE
            }
            .start()
    }

    private fun goToMood() {
        if (hasNavigated) return
        hasNavigated = true

        skipResetJob?.cancel()

        // On évite qu'une musique parasite survive au changement d'écran.
        SoundManager.stopMusic()

        introPlayer?.stop()
        introPlayer?.release()
        introPlayer = null

        startActivity(Intent(this, MoodActivity::class.java))
        finish()
    }

    override fun onPause() {
        // Sécurité défensive :
        // si un parasite audio est relancé pendant un changement de focus,
        // on le coupe ici aussi sans toucher au flux de navigation.
        SoundManager.stopMusic()
        super.onPause()
    }

    override fun onDestroy() {
        skipResetJob?.cancel()
        introPlayer?.removeListener(introPlayerListener)
        introPlayer?.release()
        introPlayer = null

        // Dernier garde-fou pour que l'intro ne laisse aucune trace audio parasite.
        SoundManager.stopMusic()

        super.onDestroy()
    }
}
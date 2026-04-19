package com.revizeus.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.revizeus.app.databinding.ActivityVideoPlayerBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * VideoPlayerActivity — Cinématique demarrer_aventure.mp4
 * ──────────────────────────────────────────────────────────
 * Joue R.raw.demarrer_aventure en plein écran.
 *
 * PASSABLE (double-tap) :
 * Même mécanique qu'IntroVideoActivity :
 * ① 1er tap  → affiche le hint "Touchez encore pour passer"
 * ② 2ème tap → passe directement à la destination
 * Le hint disparaît si aucun 2ème tap dans 1,8s.
 *
 * DESTINATION CONFIGURABLE (Extra) :
 * - EXTRA_DESTINATION = DEST_TITLE_SCREEN → TitleScreenActivity (défaut)
 * - EXTRA_DESTINATION = DEST_LOGIN        → LoginActivity
 * - EXTRA_SKIPPABLE   = true/false (défaut true)
 *
 * Son natif de la vidéo activé (SoundManager coupé avant lecture).
 *
 * USAGE ACTUEL :
 * SplashActivity → VideoPlayerActivity(demarrer_aventure) → TitleScreenActivity
 *
 * ÉVOLUTION FUTURE :
 * - Ajouter DEST_GENDER pour le flux HeroSelectActivity → "Créer un héros"
 *   si une vidéo d'intro différente est souhaitée à ce stade.
 */
class VideoPlayerActivity : BaseActivity() {

    companion object {
        const val EXTRA_DESTINATION = "DEST"
        const val EXTRA_SKIPPABLE   = "SKIPPABLE"

        const val DEST_TITLE_SCREEN = "TITLE_SCREEN"
        const val DEST_LOGIN        = "LOGIN"
    }

    private lateinit var binding: ActivityVideoPlayerBinding
    private var player: ExoPlayer? = null

    // Mécanisme double-tap (identique à IntroVideoActivity)
    private var skipArmed    = false
    private var skipResetJob: Job? = null
    private var hasNavigated = false

    private var destination = DEST_TITLE_SCREEN
    private var skippable   = true

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) naviguerVersSuite()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setupImmersiveMode()

        destination = intent.getStringExtra(EXTRA_DESTINATION) ?: DEST_TITLE_SCREEN
        skippable   = intent.getBooleanExtra(EXTRA_SKIPPABLE, true)

        // Couper toute BGM avant la cinématique
        SoundManager.stopMusic()

        setupPlayer()
        setupSkipOverlay()
    }

    // ─────────────────────────────────────────────────────────────────
    // PLAYER
    // ─────────────────────────────────────────────────────────────────

    private fun setupPlayer() {
        val videoResId = try {
            resources.getIdentifier("demarrer_aventure", "raw", packageName)
        } catch (_: Exception) { 0 }

        if (videoResId == 0) {
            // Vidéo absente → passer directement
            naviguerVersSuite()
            return
        }

        player = ExoPlayer.Builder(this).build().apply {
            volume        = 1f          // Son natif de la vidéo activé
            playWhenReady = true
            repeatMode    = Player.REPEAT_MODE_OFF
            addListener(playerListener)
            setMediaItem(MediaItem.fromUri(
                Uri.parse("android.resource://$packageName/$videoResId")
            ))
            prepare()
        }

        binding.pvIntroVideo.player = player
        binding.pvIntroVideo.useController = false
        binding.pvIntroVideo.setShutterBackgroundColor(Color.BLACK)
    }

    // ─────────────────────────────────────────────────────────────────
    // SKIP DOUBLE-TAP (identique à IntroVideoActivity)
    // ─────────────────────────────────────────────────────────────────

    private fun setupSkipOverlay() {
        if (!skippable) return  // Non-passable si l'extra le demande

        binding.root.setOnClickListener {
            if (!skipArmed) armSkip() else naviguerVersSuite()
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
            .withEndAction { binding.tvSkipHint.visibility = View.GONE }
            .start()
    }

    // ─────────────────────────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────────────────────────

    private fun naviguerVersSuite() {
        if (hasNavigated) return
        hasNavigated = true
        skipResetJob?.cancel()
        player?.stop()
        player?.release()
        player = null

        val intent = when (destination) {
            DEST_LOGIN        -> Intent(this, LoginActivity::class.java)
            else              -> Intent(this, TitleScreenActivity::class.java)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    // ─────────────────────────────────────────────────────────────────
    // BACK PRESS — non-passable si skippable=false
    // ─────────────────────────────────────────────────────────────────

    override fun handleBackPressed() {
        if (skippable) naviguerVersSuite()
        // Si non-passable : ne rien faire (intentionnellement vide)
    }

    // ─────────────────────────────────────────────────────────────────
    // CYCLE DE VIE
    // ─────────────────────────────────────────────────────────────────

    override fun onPause()   { super.onPause();   player?.pause() }
    override fun onResume()  { super.onResume();  player?.play()  }

    override fun onDestroy() {
        skipResetJob?.cancel()
        player?.removeListener(playerListener)
        player?.release()
        player = null
        super.onDestroy()
    }
}

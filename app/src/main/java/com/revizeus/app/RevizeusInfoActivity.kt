package com.revizeus.app

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.revizeus.app.databinding.ActivityRevizeusInfoBinding

/**
 * ============================================================
 * RevizeusInfoActivity.kt — RéviZeus
 * Écran premium d'information globale
 *
 * Utilité :
 * - Présenter RéviZeus via texte + ambiance vidéo
 * - Servir de point d'entrée depuis Login, Mood, Settings
 *
 * Connexions :
 * - LoginActivity
 * - MoodActivity
 * - SettingsActivity
 * - AssetTextReader
 * - SoundManager
 *
 * DIRECTION TECHNIQUE :
 * - Vidéos MP4 en res/raw
 * - ExoPlayer MUET pour les vidéos
 * - BGM jouée EXCLUSIVEMENT par SoundManager
 * - Rotation manuelle de fond toutes les 6 secondes
 *
 * RESSOURCES ATTENDUES :
 * - assets/revizeus.txt
 * - raw/revizeus_info_bg_01 ... revizeus_info_bg_10
 * - raw/info_revizeus
 * ============================================================
 */
class RevizeusInfoActivity : BaseActivity() {

    private lateinit var binding: ActivityRevizeusInfoBinding
    private var exoPlayer: ExoPlayer? = null
    private var currentVideoIndex: Int = 0

    private val videoResIds: List<Int> by lazy {
        listOf(
            R.raw.revizeus_info_bg_01,
            R.raw.revizeus_info_bg_02,
            R.raw.revizeus_info_bg_03,
            R.raw.revizeus_info_bg_04,
            R.raw.revizeus_info_bg_05,
            R.raw.revizeus_info_bg_06,
            R.raw.revizeus_info_bg_07,
            R.raw.revizeus_info_bg_08,
            R.raw.revizeus_info_bg_09,
            R.raw.revizeus_info_bg_10,
            R.raw.bg_quiz_animated,
            R.raw.bg_oracle_animated,
            R.raw.bg_resultat_animated,
            R.raw.bg_resumer_animated,
            R.raw.creation_quiz
        )
    }

    private val rotationRunnable = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed || videoResIds.isEmpty()) return
            passerAuFondSuivant()
            binding.root.postDelayed(this, 6_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRevizeusInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        initialiserUi()
        initialiserTexte()
        initialiserPlayerVideo()
        initialiserBoutons()

        try {
            SoundManager.playMusic(this, R.raw.info_revizeus)
        } catch (_: Exception) {
        }
    }

    private fun initialiserUi() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    private fun initialiserTexte() {
        val contenu = AssetTextReader.readTextAsset(
            context = this,
            fileName = "revizeus.txt",
            fallback = "RéviZeus — Le sanctuaire sacré du savoir est encore en train d'écrire son propre mythe."
        )
        binding.tvInfoContent.text = contenu
    }

    private fun initialiserPlayerVideo() {
        if (videoResIds.isEmpty()) return

        exoPlayer = ExoPlayer.Builder(this).build().apply {
            volume = 0f
            binding.playerViewInfo.player = this
            jouerVideo(videoResIds[currentVideoIndex])
            playWhenReady = true
        }
    }

    private fun initialiserBoutons() {
        binding.btnInfoRetour.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            finish()
        }

        binding.btnInfoNextBg.setOnClickListener {
            try {
                SoundManager.playSFXLow(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
            passerAuFondSuivant()
        }
    }

    private fun jouerVideo(resId: Int) {
        val uri = Uri.parse("android.resource://$packageName/$resId")
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            volume = 0f
        }
    }

    private fun passerAuFondSuivant() {
        if (videoResIds.isEmpty()) return
        currentVideoIndex = (currentVideoIndex + 1) % videoResIds.size
        jouerVideo(videoResIds[currentVideoIndex])
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.playWhenReady = true
        binding.root.removeCallbacks(rotationRunnable)
        binding.root.postDelayed(rotationRunnable, 6_000L)

        try {
            SoundManager.resumeMusic()
        } catch (_: Exception) {
            try {
                SoundManager.playMusic(this, R.raw.info_revizeus)
            } catch (_: Exception) {
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.root.removeCallbacks(rotationRunnable)
        exoPlayer?.playWhenReady = false
        SoundManager.pauseMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.root.removeCallbacks(rotationRunnable)
        try {
            exoPlayer?.release()
        } catch (_: Exception) {
        }
        exoPlayer = null

        // ── BLOC 5 : RESTAURATION BGM DASHBOARD ─────────────────────────
        // Lorsque l'utilisateur quitte l'écran info, on restaure la musique
        // principale du jeu afin d'éviter de rester sur info_revizeus.
        try {
            SoundManager.stopMusic()
            SoundManager.playMusic(this, R.raw.bgm_dashboard)
        } catch (_: Exception) {}

    }
}

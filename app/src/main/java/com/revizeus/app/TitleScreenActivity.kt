package com.revizeus.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * TitleScreenActivity — L'Antichambre de l'Olympe
 * ──────────────────────────────────────────────────────────
 * Premier écran après le splash cinématique Zeus.
 *
 * NOUVELLES FONCTIONNALITÉS :
 * ✅ Idle timer 30s → vidéo secrète easter egg
 * ✅ Bouton "MAJ" rouge en haut à gauche
 * ✅ Popup mise à jour affiché automatiquement au premier lancement
 * ✅ Reset timer sur toute interaction
 */
class TitleScreenActivity : BaseActivity() {

    private var backgroundPlayer: ExoPlayer? = null
    private var glowAnimator: AnimatorSet? = null
    private var clickAnimator: AnimatorSet? = null
    private var hasStartedTransition: Boolean = false

    // Idle timer et vidéo secrète
    private var idleTimer: CountDownTimer? = null
    private var isPlayingSecretVideo: Boolean = false

    // Popup MAJ
    private var isMajPopupShowing: Boolean = false

    companion object {
        private var hasShownMajOnce: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_title_screen)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setupImmersiveMode()

        SoundManager.stopMusic()
        SoundManager.playMusicDelayed(this, R.raw.bgm_debut, 300L)

        setupBackgroundVideo()
        setupGlowAnimation()
        setupStartButton()
        setupSupportButton()
        setupMajButton()
        
        // Démarrer l'idle timer
        startIdleTimer()

        // Afficher popup MAJ au premier lancement
        if (!hasShownMajOnce) {
            lifecycleScope.launch {
                delay(800L)
                showMajPopup()
                hasShownMajOnce = true
            }
        }
    }

    private fun setupBackgroundVideo() {
        val playerView = findViewById<PlayerView>(R.id.pvTitleBackground)

        val videoResId = try {
            resources.getIdentifier("bg_debut_animated", "raw", packageName)
        } catch (_: Exception) {
            0
        }

        if (videoResId == 0) return

        try {
            backgroundPlayer = ExoPlayer.Builder(this).build().also { player ->
                playerView.player = player
                playerView.useController = false
                player.volume = 0f
                player.repeatMode = Player.REPEAT_MODE_ONE

                val uri = Uri.parse("android.resource://$packageName/$videoResId")
                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
                player.play()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Démarre le timer d'inactivité de 30 secondes
     */
    private fun startIdleTimer() {
        idleTimer?.cancel()
        if (isPlayingSecretVideo) return
        
        idleTimer = object : CountDownTimer(30000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                if (!isPlayingSecretVideo) {
                    playSecretVideo()
                }
            }
        }.start()
    }

    /**
     * Reset le timer sur interaction utilisateur
     */
    private fun resetIdleTimer() {
        if (!isPlayingSecretVideo) {
            startIdleTimer()
        }
    }

    /**
     * Joue la vidéo secrète easter egg
     */
    private fun playSecretVideo() {
        isPlayingSecretVideo = true
        idleTimer?.cancel()
        glowAnimator?.cancel()

        val playerView = findViewById<PlayerView>(R.id.pvTitleBackground)
        val secretVideoResId = try {
            resources.getIdentifier("secret_titre", "raw", packageName)
        } catch (_: Exception) {
            0
        }

        if (secretVideoResId == 0) {
            // Pas de vidéo secrète, revenir au cycle normal
            isPlayingSecretVideo = false
            startIdleTimer()
            setupGlowAnimation()
            return
        }

        try {
            backgroundPlayer?.release()
            backgroundPlayer = ExoPlayer.Builder(this).build().also { player ->
                playerView.player = player
                playerView.useController = false
                player.volume = 0f
                player.repeatMode = Player.REPEAT_MODE_OFF // Une seule lecture

                val uri = Uri.parse("android.resource://$packageName/$secretVideoResId")
                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
                player.play()

                // Listener pour revenir à la vidéo normale après la fin
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            isPlayingSecretVideo = false
                            setupBackgroundVideo()
                            setupGlowAnimation()
                            startIdleTimer()
                        }
                    }
                })
            }
        } catch (_: Exception) {
            isPlayingSecretVideo = false
            startIdleTimer()
            setupGlowAnimation()
        }
    }

    private fun setupGlowAnimation() {
        val ivDemarrer = findViewById<ImageView>(R.id.ivDemarrer)

        val alphaAnim = ObjectAnimator.ofFloat(ivDemarrer, "alpha", 0.42f, 1.0f).apply {
            duration = 1400L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = DecelerateInterpolator()
        }

        val scaleXAnim = ObjectAnimator.ofFloat(ivDemarrer, "scaleX", 0.97f, 1.0f).apply {
            duration = 1400L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = DecelerateInterpolator()
        }

        val scaleYAnim = ObjectAnimator.ofFloat(ivDemarrer, "scaleY", 0.97f, 1.0f).apply {
            duration = 1400L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = DecelerateInterpolator()
        }

        glowAnimator?.cancel()
        glowAnimator = AnimatorSet().also { set ->
            set.playTogether(alphaAnim, scaleXAnim, scaleYAnim)
            set.start()
        }
    }

    private fun setupStartButton() {
        val ivDemarrer = findViewById<ImageView>(R.id.ivDemarrer)

        ivDemarrer.setOnClickListener {
            resetIdleTimer() // Reset timer sur clic
            
            if (hasStartedTransition) return@setOnClickListener
            hasStartedTransition = true

            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }

            glowAnimator?.cancel()
            glowAnimator = null

            val alphaFlash = ObjectAnimator.ofFloat(ivDemarrer, "alpha", 1.0f, 0.35f, 1.0f).apply {
                duration = 250L
                repeatCount = 3
                repeatMode = ObjectAnimator.REVERSE
            }

            val scaleX = ObjectAnimator.ofFloat(ivDemarrer, "scaleX", ivDemarrer.scaleX, 1.08f, 1.0f).apply {
                duration = 1000L
                interpolator = OvershootInterpolator(1.2f)
            }

            val scaleY = ObjectAnimator.ofFloat(ivDemarrer, "scaleY", ivDemarrer.scaleY, 1.08f, 1.0f).apply {
                duration = 1000L
                interpolator = OvershootInterpolator(1.2f)
            }

            clickAnimator?.cancel()
            clickAnimator = AnimatorSet().also { set ->
                set.playTogether(alphaFlash, scaleX, scaleY)
                set.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        startActivity(Intent(this@TitleScreenActivity, MainMenuActivity::class.java))
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                    }
                })
                set.start()
            }
        }
    }

    private fun setupSupportButton() {
        findViewById<FrameLayout>(R.id.btnSupportTitle)?.setOnClickListener {
            resetIdleTimer() // Reset timer sur clic
            
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:Revizeus@gmail.com")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("Revizeusiaristote@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Support Technique RéviZeus")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Bonjour,\n\nJe vous contacte concernant RéviZeus.\n\nProblème rencontré : \n\nAppareil / version Android : \n\nMerci."
                )
            }

            try {
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(
                    this,
                    "Aucune application email n'est disponible sur cet appareil.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Configure le bouton MAJ rouge en haut à gauche
     */
    private fun setupMajButton() {
        findViewById<ImageView>(R.id.btnMajTitle)?.setOnClickListener {
            resetIdleTimer() // Reset timer sur clic
            
            try {
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
            showMajPopup()
        }
    }

    /**
     * Affiche le popup de mise à jour style Zeus
     */
    private fun showMajPopup() {
        if (isMajPopupShowing || isFinishing) return
        isMajPopupShowing = true

        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#12111E"))
            }
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        // Header Zeus
        val headerRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        headerRow.addView(ImageView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(68), dp(68))
            scaleType = ImageView.ScaleType.FIT_CENTER
            val resId = resources.getIdentifier("avatar_zeus_dialog", "drawable", packageName)
            if (resId != 0) setImageResource(resId)
            else {
                val fb = resources.getIdentifier("ic_zeus_chibi", "drawable", packageName)
                if (fb != 0) setImageResource(fb)
            }
        })

        val col = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = dp(14) }
        }

        col.addView(TextView(this).apply {
            text = "Mise à Jour de l'Olympe"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = try {
                resources.getFont(R.font.cinzel)
            } catch (_: Exception) {
                Typeface.DEFAULT_BOLD
            }
        })

        col.addView(TextView(this).apply {
            text = "Zeus, Maître de l'Olympe"
            setTextColor(Color.parseColor("#776655"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        })

        headerRow.addView(col)
        root.addView(headerRow)

        // Séparateur
        root.addView(View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
            setBackgroundColor(Color.parseColor("#33FFD700"))
        })

        // Contenu du changelog - prend tout l'espace disponible
        val changelogText = loadChangelogFromAssets()
        val scrollView = android.widget.ScrollView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f // Weight pour prendre tout l'espace disponible
            )
        }

        scrollView.addView(TextView(this).apply {
            text = changelogText
            setTextColor(Color.parseColor("#DEDEDE"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLineSpacing(dp(3).toFloat(), 1f)
            setPadding(dp(4), 0, dp(4), 0)
        })

        root.addView(scrollView)

        // Bouton Compris
        val btnFrame = FrameLayout(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)
            ).apply { topMargin = dp(12) }
        }

        btnFrame.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            try {
                setImageResource(R.drawable.bg_temple_button)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#1A1A2E"))
            }
            scaleType = ImageView.ScaleType.FIT_XY
        })

        val btnLabel = TextView(this).apply {
            text = "COMPRIS"
            setTextColor(Color.parseColor("#A5D6A7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            typeface = try {
                resources.getFont(R.font.cinzel)
            } catch (_: Exception) {
                Typeface.DEFAULT_BOLD
            }
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        btnFrame.addView(btnLabel)
        root.addView(btnFrame)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(root)
            .setCancelable(true)
            .create()

        btnLabel.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            isMajPopupShowing = false
        }

        dialog.show()
        try {
            dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rpg_dialog)
        } catch (_: Exception) {
        }
        
        // Masquer la barre de navigation système pour éviter qu'elle cache le bouton
        dialog.window?.let { window ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // Android 11+ (API 30+)
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.let { controller ->
                    controller.hide(android.view.WindowInsets.Type.systemBars())
                    controller.systemBarsBehavior = 
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                // Android 10 et inférieur
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
            }
        }
    }

    /**
     * Charge le changelog depuis assets/maj.txt
     */
    private fun loadChangelogFromAssets(): String {
        return try {
            val inputStream = assets.open("maj.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readText()
        } catch (_: Exception) {
            "🏛 MISE À JOUR DE L'OLYMPE\n\nBienvenue dans RéviZeus !\n\nLes dieux de l'Olympe ont préparé de nombreuses améliorations pour ton entraînement.\n\n— Zeus, Maître de l'Olympe"
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onPause() {
        super.onPause()
        idleTimer?.cancel()
        try {
            backgroundPlayer?.pause()
        } catch (_: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isPlayingSecretVideo) {
            startIdleTimer()
        }
        try {
            backgroundPlayer?.play()
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        idleTimer?.cancel()
        idleTimer = null
        clickAnimator?.cancel()
        clickAnimator = null
        glowAnimator?.cancel()
        glowAnimator = null

        try {
            backgroundPlayer?.release()
        } catch (_: Exception) {
        }
        backgroundPlayer = null

        super.onDestroy()
    }

    /**
     * Override pour reset le timer sur toute interaction tactile
     */
    override fun onUserInteraction() {
        super.onUserInteraction()
        resetIdleTimer()
    }
}

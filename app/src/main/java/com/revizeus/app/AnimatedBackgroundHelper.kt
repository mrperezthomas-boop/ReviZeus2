package com.revizeus.app

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * ═══════════════════════════════════════════════════════════════
 * ANIMATED BACKGROUND HELPER
 * ═══════════════════════════════════════════════════════════════
 * Moteur premium réutilisable pour les fonds RéviZeus.
 *
 * CAPACITÉS :
 * - fond gradient vivant + respiration lumineuse
 * - fallback image fixe si une vidéo n'existe pas
 * - support vidéo premium en raw si disponible
 * - support particules Olympiennes si la vue existe déjà
 * - création automatique d'une couche particules si besoin
 *
 * PHILOSOPHIE :
 * - zéro casse de l'existant
 * - accepte un ImageView de fond déjà présent dans le XML
 * - n'impose pas de nouvelles ressources : si la vidéo n'existe pas,
 *   l'image fixe reste visible et le gradient animé continue de vivre
 * - permet un réglage ciblé de l'alpha et du volume de la vidéo pour
 *   les écrans Oracle / Résumé / Quiz / Résultat sans impacter le reste
 * ═══════════════════════════════════════════════════════════════
 */
class AnimatedBackgroundHelper(
    private val targetView: View,
    private val particlesView: OlympianParticlesView? = null,
    private val backgroundImageView: ImageView? = null
) {

    private val baseGradient = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(
            Color.parseColor("#070A12"),
            Color.parseColor("#0B1020"),
            Color.parseColor("#05070D")
        )
    ).apply {
        cornerRadius = 0f
    }

    private val pulseGradient = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(
            Color.parseColor("#001E90FF"),
            Color.parseColor("#0000E5FF"),
            Color.parseColor("#00FFD700")
        )
    ).apply {
        cornerRadius = 0f
        alpha = 90
    }

    private val combinedBackground = LayerDrawable(
        arrayOf(baseGradient, pulseGradient)
    )

    private var pulseAnimator: ValueAnimator? = null
    private var colorAnimator: ValueAnimator? = null

    private val hostGroup: ViewGroup? = when {
        targetView is ViewGroup -> targetView
        targetView.parent is ViewGroup -> targetView.parent as? ViewGroup
        else -> null
    }

    private var managedParticlesView: OlympianParticlesView? = particlesView
    private var backgroundPlayerView: PlayerView? = null
    private var backgroundPlayer: ExoPlayer? = null

    private var configuredStaticDrawableRes: Int = 0
    private var configuredVideoRawRes: Int = 0
    private var configuredImageAlpha: Float = 0.22f
    private var configuredLoopVideo: Boolean = true
    private var configuredVideoAlpha: Float = 0.96f
    private var configuredVideoVolume: Float = 1.0f

    init {
        targetView.background = combinedBackground
    }

    /**
     * Configure les ressources premium d'un écran.
     *
     * @param staticDrawableRes image fixe affichée dans le XML ou en fallback.
     * @param videoRawRes vidéo raw optionnelle. Si 0 ou absente, aucun crash.
     * @param imageAlpha alpha de l'image de fond existante.
     * @param loopVideo boucle vidéo continue si la ressource existe.
     * @param videoAlpha alpha de la couche vidéo pour doser sa présence.
     * @param videoVolume volume de la vidéo (0f..1f).
     */
    fun configurePremiumBackground(
        staticDrawableRes: Int,
        videoRawRes: Int = 0,
        imageAlpha: Float = 0.22f,
        loopVideo: Boolean = true,
        videoAlpha: Float = 0.96f,
        videoVolume: Float = 1.0f
    ) {
        configuredStaticDrawableRes = staticDrawableRes
        configuredVideoRawRes = videoRawRes
        configuredImageAlpha = imageAlpha.coerceIn(0f, 1f)
        configuredLoopVideo = loopVideo
        configuredVideoAlpha = videoAlpha.coerceIn(0f, 1f)
        configuredVideoVolume = videoVolume.coerceIn(0f, 1f)

        backgroundImageView?.apply {
            if (staticDrawableRes != 0) {
                try {
                    setImageResource(staticDrawableRes)
                } catch (_: Exception) {
                }
            }
            alpha = configuredImageAlpha
            visibility = View.VISIBLE
        }

        backgroundPlayerView?.alpha = configuredVideoAlpha
        try {
            backgroundPlayer?.volume = configuredVideoVolume
        } catch (_: Exception) {
        }
    }

    /**
     * Démarre l'animation du fond + les particules + la vidéo si disponible.
     *
     * @param accentColor couleur dominante de l'écran / dieu.
     * @param mode profil de particules cohérent avec l'écran.
     */
    fun start(
        accentColor: Int = Color.parseColor("#1E90FF"),
        mode: OlympianParticlesView.ParticleMode = OlympianParticlesView.ParticleMode.SAVOIR
    ) {
        stop()
        ensureParticlesView(mode)
        startGradientAnimations(accentColor)
        startPremiumVideoIfAvailable()
    }

    /**
     * Arrête proprement les animations vivantes et met en pause la vidéo.
     * On ne libère pas ici le player pour permettre une reprise rapide au onResume().
     */
    fun stop() {
        pulseAnimator?.cancel()
        colorAnimator?.cancel()
        pulseAnimator = null
        colorAnimator = null

        try {
            backgroundPlayer?.pause()
        } catch (_: Exception) {
        }

        try {
            managedParticlesView?.stop()
        } catch (_: Exception) {
        }
    }

    /**
     * Libère totalement les ressources lourdes.
     * À appeler en onDestroy().
     */
    fun release() {
        stop()

        try {
            backgroundPlayerView?.player = null
        } catch (_: Exception) {
        }

        try {
            backgroundPlayer?.release()
        } catch (_: Exception) {
        }

        backgroundPlayer = null

        if (hostGroup != null && backgroundPlayerView != null) {
            try {
                hostGroup.removeView(backgroundPlayerView)
            } catch (_: Exception) {
            }
        }
        backgroundPlayerView = null
    }

    /**
     * Démarre uniquement les animations gradient/pulse.
     */
    private fun startGradientAnimations(accentColor: Int) {
        try {
            managedParticlesView?.start()
        } catch (_: Exception) {
        }

        pulseAnimator = ValueAnimator.ofInt(50, 125).apply {
            duration = 4200L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = FastOutSlowInInterpolator()

            addUpdateListener { animator ->
                val alphaValue = animator.animatedValue as Int
                pulseGradient.alpha = alphaValue
                targetView.invalidate()
            }
        }

        val startColorA = withAlpha(accentColor, 34)
        val endColorA = withAlpha(Color.parseColor("#00E5FF"), 28)
        val startColorB = withAlpha(Color.parseColor("#FFD700"), 18)
        val endColorB = withAlpha(accentColor, 24)

        colorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 6800L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = FastOutSlowInInterpolator()

            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                val evaluator = ArgbEvaluator()

                val animatedA = evaluator.evaluate(fraction, startColorA, endColorA) as Int
                val animatedB = evaluator.evaluate(fraction, startColorB, endColorB) as Int

                pulseGradient.colors = intArrayOf(
                    animatedA,
                    Color.TRANSPARENT,
                    animatedB
                )
                targetView.invalidate()
            }
        }

        pulseAnimator?.start()
        colorAnimator?.start()
    }

    /**
     * Crée ou récupère la couche particules et l'insère au bon niveau.
     * Elle doit rester AU-DESSUS du fond image/vidéo, mais SOUS le contenu UI.
     */
    private fun ensureParticlesView(mode: OlympianParticlesView.ParticleMode) {
        if (managedParticlesView == null) {
            val group = hostGroup ?: return
            val ctx = targetView.context
            managedParticlesView = OlympianParticlesView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isClickable = false
                isFocusable = false
                alpha = 0.9f
            }
            val insertIndex = computeBackgroundOverlayInsertionIndex(group)
            try {
                group.addView(managedParticlesView, insertIndex)
            } catch (_: Exception) {
            }
        }

        try {
            managedParticlesView?.configure(mode)
        } catch (_: Exception) {
        }
    }

    /**
     * Lance la vidéo premium si la ressource raw est disponible.
     * Sinon, on reste simplement sur l'image fixe + gradient.
     */
    private fun startPremiumVideoIfAvailable() {
        val rawRes = configuredVideoRawRes
        if (rawRes == 0) {
            ensureFallbackImageVisible()
            return
        }

        val group = hostGroup ?: run {
            ensureFallbackImageVisible()
            return
        }

        try {
            val exoPlayer = backgroundPlayer ?: ExoPlayer.Builder(targetView.context).build().also {
                backgroundPlayer = it
            }

            val playerView = backgroundPlayerView ?: PlayerView(targetView.context).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                alpha = configuredVideoAlpha
                setShutterBackgroundColor(Color.TRANSPARENT)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isClickable = false
                isFocusable = false
                this.player = exoPlayer
            }.also {
                backgroundPlayerView = it
                val insertIndex = computeVideoInsertionIndex(group)
                group.addView(it, insertIndex)
            }

            playerView.alpha = configuredVideoAlpha
            playerView.player = exoPlayer

            exoPlayer.volume = configuredVideoVolume
            exoPlayer.repeatMode = if (configuredLoopVideo) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            exoPlayer.setMediaItem(MediaItem.fromUri(buildRawUri(targetView.context, rawRes)))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true

            backgroundImageView?.visibility = View.VISIBLE
            backgroundImageView?.alpha = configuredImageAlpha
        } catch (_: Exception) {
            ensureFallbackImageVisible()
        }
    }

    private fun ensureFallbackImageVisible() {
        backgroundImageView?.apply {
            if (configuredStaticDrawableRes != 0) {
                try {
                    setImageResource(configuredStaticDrawableRes)
                } catch (_: Exception) {
                }
            }
            alpha = configuredImageAlpha
            visibility = View.VISIBLE
        }
    }

    private fun computeVideoInsertionIndex(group: ViewGroup): Int {
        val existingBackgroundIndex = backgroundImageView?.let { group.indexOfChild(it) } ?: -1
        return when {
            existingBackgroundIndex >= 0 -> (existingBackgroundIndex + 1).coerceAtMost(group.childCount)
            else -> 0
        }
    }

    private fun computeBackgroundOverlayInsertionIndex(group: ViewGroup): Int {
        val playerIndex = backgroundPlayerView?.let { group.indexOfChild(it) } ?: -1
        if (playerIndex >= 0) {
            return (playerIndex + 1).coerceAtMost(group.childCount)
        }

        val backgroundIndex = backgroundImageView?.let { group.indexOfChild(it) } ?: -1
        return if (backgroundIndex >= 0) {
            (backgroundIndex + 1).coerceAtMost(group.childCount)
        } else {
            0
        }
    }

    /**
     * Ajoute une alpha à une couleur pour doser l'effet lumineux.
     */
    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    private fun buildRawUri(context: Context, rawRes: Int): String {
        return "android.resource://${context.packageName}/$rawRes"
    }
}

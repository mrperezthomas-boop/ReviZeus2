package com.revizeus.app

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.revizeus.app.databinding.ActivityWorldMapBinding
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class WorldMapActivity : BaseAdventureActivity() {

    private lateinit var binding: ActivityWorldMapBinding
    private var backgroundPlayer: ExoPlayer? = null
    private var worldState: WorldMapState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorldMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackWorld.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
            finish()
        }

        lifecycleScope.launch {
            val state = AdventureManager.loadWorldState(this@WorldMapActivity)
            worldState = state
            bindWorldState(state)
            showArrivalDialogue()
        }
    }

    private fun bindWorldState(state: WorldMapState) {
        val theme = WorldMapThemeResolver.resolve(state.totalRestorationScore)
        binding.tvWorldTitle.text = "Carte du Monde"
        binding.tvWorldSubtitle.text = "Restauration globale : ${state.totalRestorationScore} / 200"
        binding.tvWorldTier.text = "PALIER ${theme.tier}"

        applyWorldBackground(theme)
        applyOverlay(binding.ivWorldLightOverlay, theme.lightOverlayName)
        applyOverlay(binding.ivWorldCorruptionOverlay, theme.corruptionOverlayName)
        renderWorldSlots(state)
        renderChaosCore()
        playWorldMusic()
    }

    private fun applyWorldBackground(theme: WorldMapThemeResolver.WorldMapTheme) {
        val rawVideoResId = resolveRawResId(theme.backgroundVideoRawName)
        if (rawVideoResId != 0) {
            binding.playerWorldBackground.visibility = View.VISIBLE
            binding.ivWorldBackground.visibility = View.INVISIBLE
            backgroundPlayer?.release()
            backgroundPlayer = ExoPlayer.Builder(this).build().also { player ->
                binding.playerWorldBackground.player = player
                player.volume = 0f
                player.repeatMode = ExoPlayer.REPEAT_MODE_ALL
                player.setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://$packageName/$rawVideoResId")))
                player.prepare()
                player.playWhenReady = true
            }
        } else {
            binding.playerWorldBackground.visibility = View.GONE
            binding.ivWorldBackground.visibility = View.VISIBLE
            val drawableId = resolveDrawableResId(theme.backgroundDrawableName)
            if (drawableId != 0) {
                binding.ivWorldBackground.setImageResource(drawableId)
            } else {
                binding.ivWorldBackground.setImageResource(R.drawable.bg_olympus_dark)
            }
        }
    }

    private fun applyOverlay(target: ImageView, drawableName: String) {
        val drawableId = resolveDrawableResId(drawableName)
        if (drawableId != 0) {
            target.visibility = View.VISIBLE
            target.setImageResource(drawableId)
        } else {
            target.visibility = View.GONE
        }
    }

    private fun renderWorldSlots(state: WorldMapState) {
        binding.worldCanvas.removeAllViews()
        binding.worldCanvas.post {
            val width = binding.worldCanvas.width
            val height = binding.worldCanvas.height
            if (width <= 0 || height <= 0) return@post

            val centerX = width / 2f
            val centerY = height / 2f
            val radius = min(width, height) * 0.34f

            state.slots.forEach { slot ->
                val angleRad = Math.toRadians(slot.angleDegrees.toDouble())
                val x = centerX + (cos(angleRad) * radius).toFloat()
                val y = centerY + (sin(angleRad) * radius).toFloat()
                binding.worldCanvas.addView(createTempleSlotView(slot, x, y))
            }
        }
    }

    private fun renderChaosCore() {
        binding.worldCanvas.post {
            val width = binding.worldCanvas.width
            val height = binding.worldCanvas.height
            if (width <= 0 || height <= 0) return@post
            val centerX = width / 2f
            val centerY = height / 2f
            binding.worldCanvas.addView(createChaosView(centerX, centerY))
        }
    }

    private fun createTempleSlotView(slot: WorldMapTempleSlot, centerX: Float, centerY: Float): View {
        val size = dp(110)
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                leftMargin = (centerX - size / 2f).toInt()
                topMargin = (centerY - size / 2f).toInt()
            }
            isClickable = true
            isFocusable = true
        }

        val base = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(82), dp(82), Gravity.CENTER)
            val baseRes = if (slot.isUnlocked) resolveDrawableResId("island_base_divine") else resolveDrawableResId("island_base_chaos")
            if (baseRes != 0) {
                setImageResource(baseRes)
            } else {
                setBackgroundColor(Color.parseColor("#44222222"))
            }
            alpha = if (slot.isUnlocked) 1f else 0.5f
        }

        val icon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(58), dp(58), Gravity.CENTER)
            val iconName = if (slot.templeLevel > 0) "${slot.iconPrefix}${slot.templeLevel.coerceIn(1, 20)}" else slot.iconPrefix.removeSuffix("_")
            val iconRes = resolveDrawableResId(iconName)
            if (iconRes != 0) {
                setImageResource(iconRes)
            } else {
                setImageResource(resolveFallbackTempleIcon(slot.godId))
            }
            alpha = if (slot.isUnlocked) 1f else 0.35f
        }

        val label = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            text = when {
                slot.isUnlocked -> "${slot.displayName}\nNiv.${slot.templeLevel}"
                else -> "${slot.displayName}\nÀ venir"
            }
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 10f
            setPadding(dp(2), dp(2), dp(2), dp(2))
            setShadowLayer(6f, 0f, 0f, Color.BLACK)
        }

        container.addView(base)
        container.addView(icon)
        container.addView(label)

        container.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
            if (!slot.isUnlocked) {
                DialogRPGManager.showInfo(
                    activity = this,
                    godId = "prometheus",
                    title = slot.displayName,
                    message = "Ce temple est déjà réservé sur l'anneau du monde, mais sa route n'est pas encore ouverte dans ce premier bloc aventure."
                )
            } else {
                startActivity(AdventureManager.buildTempleIntent(this, slot))
            }
        }

        container.setOnLongClickListener {
            DialogRPGManager.showInfo(
                activity = this,
                godId = if (slot.isUnlocked) slot.godId else "prometheus",
                title = slot.displayName,
                message = if (slot.isUnlocked) {
                    "Temple actif de ${slot.subject}. Depuis ici, tu peux entrer dans sa carte locale, là où vivent réellement les nodes d'aventure."
                } else {
                    "Temple encore endormi. Son emplacement existe déjà sur la carte du monde, mais il sera relié plus tard à sa propre aventure."
                }
            )
            true
        }

        return container
    }

    private fun createChaosView(centerX: Float, centerY: Float): View {
        val size = dp(136)
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                leftMargin = (centerX - size / 2f).toInt()
                topMargin = (centerY - size / 2f).toInt()
            }
        }

        val base = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(104), dp(104), Gravity.CENTER)
            val res = resolveDrawableResId("island_base_chaos")
            if (res != 0) setImageResource(res)
        }
        val icon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(76), dp(76), Gravity.CENTER)
            val res = resolveDrawableResId("ic_world_chaos_core")
            if (res != 0) setImageResource(res) else setImageResource(R.drawable.ic_prometheus_mini)
        }
        val label = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            text = "Chaos central"
            setTextColor(Color.parseColor("#FFBF00"))
            textSize = 11f
            setShadowLayer(6f, 0f, 0f, Color.BLACK)
        }
        container.addView(base)
        container.addView(icon)
        container.addView(label)
        container.setOnClickListener {
            DialogRPGManager.showInfo(
                activity = this,
                godId = "zeus",
                title = "Chaos central",
                message = "Le cœur du Chaos reste verrouillé. Quand davantage de temples auront été restaurés, sa route s'ouvrira pour de vrai."
            )
        }
        return container
    }

    private fun showArrivalDialogue() {
        DialogRPGManager.showInfo(
            activity = this,
            godId = "prometheus",
            title = "Carte du Monde",
            message = "Le Chaos siège au centre. Les temples entourent le monde en cercle. Choisis un temple actif pour ouvrir sa carte locale en paysage."
        )
    }

    private fun playWorldMusic() {
        try {
            SoundManager.rememberMusic(R.raw.bgm_savoir)
            SoundManager.playMusicDelayed(this, R.raw.bgm_savoir, 300L)
        } catch (_: Exception) {
        }
    }

    private fun resolveDrawableResId(name: String?): Int {
        if (name.isNullOrBlank()) return 0
        return resources.getIdentifier(name, "drawable", packageName)
    }

    private fun resolveRawResId(name: String?): Int {
        if (name.isNullOrBlank()) return 0
        val exact = resources.getIdentifier(name, "raw", packageName)
        if (exact != 0) return exact
        return resources.getIdentifier("${name}_animated", "raw", packageName)
    }

    private fun resolveFallbackTempleIcon(godId: String): Int {
        return when (godId.lowercase()) {
            "zeus" -> R.drawable.ic_zeus_chibi
            "athena" -> resolveDrawableResId("ic_athena_mini").takeIf { it != 0 } ?: R.drawable.ic_zeus_chibi
            "ares" -> resolveDrawableResId("ic_ares_mini").takeIf { it != 0 } ?: R.drawable.ic_zeus_chibi
            else -> R.drawable.ic_prometheus_mini
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onPause() {
        super.onPause()
        try {
            backgroundPlayer?.pause()
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        try {
            backgroundPlayer?.release()
        } catch (_: Exception) {
        }
        backgroundPlayer = null
        super.onDestroy()
    }
}

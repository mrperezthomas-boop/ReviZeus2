package com.revizeus.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.revizeus.app.databinding.ActivityMapTempleBinding
import com.revizeus.app.models.TempleAdventureNodeProgressEntity
import kotlinx.coroutines.launch

class MapTempleActivity : BaseAdventureActivity() {

    companion object {
        const val EXTRA_GOD_ID = "extra_god_id"
        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_TEMPLE_LEVEL = "extra_temple_level"
        const val EXTRA_DISPLAY_NAME = "extra_display_name"
        const val EXTRA_MAP_INDEX = "extra_map_index"
    }

    private lateinit var binding: ActivityMapTempleBinding
    private var backgroundPlayer: ExoPlayer? = null
    private lateinit var config: TempleMapConfig
    private var selectedNodeId: String? = null
    private var nodeStates: List<TempleAdventureNodeProgressEntity> = emptyList()
    private val encounterLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val nodeId = data.getStringExtra(TempleNodeEncounterActivity.RESULT_NODE_ID).orEmpty()
        val validated = data.getBooleanExtra(TempleNodeEncounterActivity.RESULT_VALIDATED, false)
        if (!validated || nodeId.isBlank()) return@registerForActivityResult

        val node = config.nodeList.firstOrNull { it.nodeId == nodeId } ?: return@registerForActivityResult
        val resolution = TempleNodeResolver.resolve(config, node)
        if (!resolution.shouldMarkComplete) return@registerForActivityResult

        lifecycleScope.launch {
            val progress = TempleAdventureProgressManager.markNodeCompletedAndLoad(
                context = this@MapTempleActivity,
                config = config,
                nodeId = nodeId
            )
            nodeStates = progress.nodes
            renderTempleMap(nodeStates)
            updateBottomPanel(nodeId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapTempleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackTemple.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
            finish()
        }

        val godId = intent.getStringExtra(EXTRA_GOD_ID).orEmpty().ifBlank { "zeus" }
        val subject = intent.getStringExtra(EXTRA_SUBJECT).orEmpty().ifBlank { "Mathématiques" }
        val templeLevel = intent.getIntExtra(EXTRA_TEMPLE_LEVEL, 1)
        val mapIndex = intent.getIntExtra(EXTRA_MAP_INDEX, 1)
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME).orEmpty().ifBlank { "Temple" }

        binding.tvTempleTitle.text = displayName
        binding.tvTempleSubtitle.text = "$subject • Niveau $templeLevel"
        binding.btnEnterNode.isEnabled = false

        config = TempleMapManager.getTempleMapConfig(godId, subject, templeLevel, mapIndex)
        applyTempleTheme(godId, templeLevel)
        playTempleMusic()

        lifecycleScope.launch {
            val progress = TempleAdventureProgressManager.loadOrCreateMapProgress(this@MapTempleActivity, config)
            nodeStates = progress.nodes
            renderTempleMap(nodeStates)
            selectFirstAvailableNodeIfNeeded()
            showTempleIntro(config)
        }

        binding.btnEnterNode.setOnClickListener {
            val nodeId = selectedNodeId ?: return@setOnClickListener
            val targetNode = config.nodeList.firstOrNull { it.nodeId == nodeId } ?: return@setOnClickListener
            val targetState = nodeStates.firstOrNull { it.nodeId == nodeId } ?: return@setOnClickListener
            if (!targetState.isUnlocked) return@setOnClickListener
            val resolution = TempleNodeResolver.resolve(config, targetNode)
            val intent = Intent(this, TempleNodeEncounterActivity::class.java).apply {
                putExtra(TempleNodeEncounterActivity.EXTRA_NODE_ID, targetNode.nodeId)
                putExtra(TempleNodeEncounterActivity.EXTRA_NODE_TITLE, resolution.title)
                putExtra(TempleNodeEncounterActivity.EXTRA_NODE_TYPE, targetNode.nodeType.name)
                putExtra(TempleNodeEncounterActivity.EXTRA_NODE_DESCRIPTION, targetNode.description)
                putExtra(TempleNodeEncounterActivity.EXTRA_GOD_ID, resolution.godId)
                putExtra(TempleNodeEncounterActivity.EXTRA_RESOLUTION_MESSAGE, resolution.message)
                putExtra(TempleNodeEncounterActivity.EXTRA_CAN_VALIDATE, resolution.shouldMarkComplete)
            }
            encounterLauncher.launch(intent)
        }
    }

    private fun applyTempleTheme(godId: String, templeLevel: Int) {
        val theme = TempleMapThemeResolver.resolve(godId, templeLevel)
        val rawVideoResId = resolveRawResId(theme.backgroundVideoRawName)
        if (rawVideoResId != 0) {
            binding.playerTempleBackground.visibility = View.VISIBLE
            binding.ivTempleBackground.visibility = View.INVISIBLE
            backgroundPlayer?.release()
            backgroundPlayer = ExoPlayer.Builder(this).build().also { player ->
                binding.playerTempleBackground.player = player
                player.volume = 0f
                player.repeatMode = ExoPlayer.REPEAT_MODE_ALL
                player.setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://$packageName/$rawVideoResId")))
                player.prepare()
                player.playWhenReady = true
            }
        } else {
            binding.playerTempleBackground.visibility = View.GONE
            binding.ivTempleBackground.visibility = View.VISIBLE
            val drawableId = resolveDrawableResId(theme.backgroundDrawableName)
            if (drawableId != 0) {
                binding.ivTempleBackground.setImageResource(drawableId)
            } else {
                binding.ivTempleBackground.setImageResource(theme.fallbackDrawableResId)
            }
        }

        try {
            binding.nodeDetailsCard.setBackgroundResource(R.drawable.bg_rpg_dialog)
            binding.btnEnterNode.setBackgroundResource(R.drawable.bg_temple_button)
            binding.tvTempleTitle.setTextColor(Color.parseColor(theme.accentColorHex))
        } catch (_: Exception) {
        }
    }

    private fun renderTempleMap(states: List<TempleAdventureNodeProgressEntity>) {
        binding.templeCanvas.removeAllViews()
        binding.templeCanvas.post {
            val canvasWidth = binding.templeCanvas.width
            val canvasHeight = binding.templeCanvas.height
            if (canvasWidth <= 0 || canvasHeight <= 0) return@post

            config.edgeList.forEach { edge ->
                val from = config.nodeList.firstOrNull { it.nodeId == edge.fromNodeId } ?: return@forEach
                val to = config.nodeList.firstOrNull { it.nodeId == edge.toNodeId } ?: return@forEach
                binding.templeCanvas.addView(createEdgeView(from, to, canvasWidth, canvasHeight))
            }

            config.nodeList.forEach { node ->
                val state = states.firstOrNull { it.nodeId == node.nodeId }
                binding.templeCanvas.addView(createNodeView(node, state, canvasWidth, canvasHeight))
            }
        }
    }

    private fun createEdgeView(from: TempleMapNode, to: TempleMapNode, canvasWidth: Int, canvasHeight: Int): View {
        val startX = (from.xRatio * canvasWidth).toInt()
        val endX = (to.xRatio * canvasWidth).toInt()
        val startY = (from.yRatio * canvasHeight).toInt()
        val endY = (to.yRatio * canvasHeight).toInt()
        val left = minOf(startX, endX)
        val top = minOf(startY, endY)
        val width = kotlin.math.abs(endX - startX).coerceAtLeast(dp(4))
        val height = kotlin.math.abs(endY - startY).coerceAtLeast(dp(4))

        return View(this).apply {
            layoutParams = FrameLayout.LayoutParams(width, height).apply {
                leftMargin = left
                topMargin = top
            }
            setBackgroundColor(Color.parseColor("#66FFD700"))
            alpha = 0.35f
        }
    }

    private fun createNodeView(
        node: TempleMapNode,
        state: TempleAdventureNodeProgressEntity?,
        canvasWidth: Int,
        canvasHeight: Int
    ): View {
        val isUnlocked = state?.isUnlocked == true
        val isCompleted = state?.isCompleted == true
        val size = dp(82)
        val centerX = node.xRatio * canvasWidth
        val centerY = node.yRatio * canvasHeight
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                leftMargin = (centerX - size / 2f).toInt()
                topMargin = (centerY - size / 2f).toInt()
            }
            isClickable = true
            isFocusable = true
        }

        val halo = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(64), dp(64), Gravity.CENTER)
            setBackgroundColor(
                when {
                    node.nodeId == selectedNodeId -> Color.parseColor("#66FFD700")
                    isCompleted -> Color.parseColor("#554CAF50")
                    isUnlocked -> Color.parseColor("#331E90FF")
                    else -> Color.parseColor("#33000000")
                }
            )
        }

        val icon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER)
            setImageResource(resolveNodeIcon(node.nodeType))
            alpha = when {
                isCompleted -> 1f
                isUnlocked -> 0.95f
                else -> 0.35f
            }
        }

        val label = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            text = when {
                isCompleted -> "✓"
                isUnlocked -> node.difficultyTier.toString()
                else -> "🔒"
            }
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 10f
            setShadowLayer(6f, 0f, 0f, Color.BLACK)
        }

        container.addView(halo)
        container.addView(icon)
        container.addView(label)

        container.setOnClickListener {
            if (!isUnlocked) {
                DialogRPGManager.showInfo(
                    activity = this,
                    godId = config.godId,
                    title = node.title,
                    message = "Ce node reste verrouillé. Avance d'abord sur la route déjà ouverte du temple."
                )
                return@setOnClickListener
            }
            selectedNodeId = node.nodeId
            renderTempleMap(nodeStates)
            updateBottomPanel(node.nodeId)
            try {
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
        }

        return container
    }

    private fun selectFirstAvailableNodeIfNeeded() {
        if (selectedNodeId != null) return
        val first = nodeStates.firstOrNull { it.isUnlocked } ?: return
        selectedNodeId = first.nodeId
        updateBottomPanel(first.nodeId)
    }

    private fun updateBottomPanel(nodeId: String) {
        val node = config.nodeList.firstOrNull { it.nodeId == nodeId } ?: return
        val state = nodeStates.firstOrNull { it.nodeId == nodeId }
        selectedNodeId = nodeId
        binding.tvNodeTitle.text = node.title
        binding.tvNodeType.text = node.nodeType.name.replace('_', ' ')
        binding.tvNodeDescription.text = node.description
        binding.tvNodeStatus.text = when {
            state?.isCompleted == true -> "Statut : Terminé • Rejoué ${state.completionCount} fois"
            state?.isUnlocked == true -> "Statut : Disponible"
            else -> "Statut : Verrouillé"
        }
        val canEnter = state?.isUnlocked == true
        binding.btnEnterNode.isEnabled = canEnter
        binding.btnEnterNode.alpha = if (canEnter) 1f else 0.5f
    }

    private fun showTempleIntro(config: TempleMapConfig) {
        DialogRPGManager.showInfo(
            activity = this,
            godId = config.godId,
            title = config.title,
            message = config.introDialogue
        )
    }

    private fun playTempleMusic() {
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

    private fun resolveNodeIcon(type: TempleNodeType): Int {
        TempleNodeResolver.resolveNodeIconCandidates(type).forEach { candidate ->
            val resId = resolveDrawableResId(candidate)
            if (resId != 0) return resId
        }
        return R.drawable.ic_prometheus_mini
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

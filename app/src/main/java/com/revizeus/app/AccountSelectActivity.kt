package com.revizeus.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.revizeus.app.models.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AccountSelectActivity — Le Registre de Tous les Héros
 * ──────────────────────────────────────────────────────────
 * Affiche TOUS les héros de TOUS les comptes enregistrés
 * sur cet appareil (via AccountRegistry.getAllHeroesCaches()).
 *
 * Accessible depuis MainMenuActivity → bouton "CHARGER UN HÉROS".
 *
 * LOGIQUE DE SÉLECTION :
 * • Tap sur une carte héros
 *   → setActiveUid(uid) + setActiveSlot(uid, slot)
 *   → AppDatabase.resetInstance()
 *   → MoodActivity directement
 *
 * • Appui long sur une carte héros
 *   → menu RPG custom bg_rpg_dialog
 *   → jouer / supprimer ce héros local du téléphone
 *
 * • Bouton "+ Nouvelle Aventure"
 *   → LoginActivity (inscription)
 *
 * IMPORTANT :
 * - aucune police dynamique runtime
 * - aucune mécanique existante supprimée
 * - portrait plein écran conservé
 * - audio géré uniquement par SoundManager
 */
class AccountSelectActivity : BaseActivity() {

    private lateinit var adapter: AccountCardAdapter
    private val heroList = mutableListOf<AccountRegistry.AccountCache>()
    private var backgroundPlayer: ExoPlayer? = null
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_select)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setupImmersiveMode()

        SoundManager.rememberMusic(R.raw.bgm_title_select)
        if (!SoundManager.isPlayingMusic()) {
            SoundManager.playMusicDelayed(this, R.raw.bgm_title_select, 300L)
        }

        setupBackgroundVideo()
        setupRecyclerView()
        setupBoutonNouvelleAventure()
        setupRetourHeader()
        setupRetourSysteme()
        chargerHeros()
    }

    // ─────────────────────────────────────────────────────────────────
    // NAVIGATION RETOUR
    // ─────────────────────────────────────────────────────────────────

    private fun setupRetourHeader() {
        findViewById<View>(R.id.btnBackAccountSelect)?.setOnClickListener {
            retournerTitre()
        }
    }

    private fun setupRetourSysteme() {
        onBackPressedDispatcher.addCallback(this) {
            retournerTitre()
        }
    }

    private fun retournerTitre() {
        try {
            SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
        } catch (_: Exception) {
        }

        startActivity(
            Intent(this, TitleScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    // ─────────────────────────────────────────────────────────────────
    // FOND ANIMÉ
    // ─────────────────────────────────────────────────────────────────

    private fun setupBackgroundVideo() {
        val playerView = findViewById<PlayerView>(R.id.pvAccountBackground)

        val videoResId = try {
            resources.getIdentifier("bg_account_select", "raw", packageName)
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

    // ─────────────────────────────────────────────────────────────────
    // RECYCLERVIEW
    // ─────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvAccountCards)

        adapter = AccountCardAdapter(
            context = this,
            accounts = heroList,
            activeUid = AccountRegistry.getActiveUid(this),
            onAccountSelected = { account ->
                selectionnerHeros(account)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        /**
         * Correctif demandé :
         * on ajoute ici la détection d'appui long sans dépendre
         * d'une modification de l'AccountCardAdapter existant.
         */
        val gestureDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onLongPress(e: MotionEvent) {
                    val child = recyclerView.findChildViewUnder(e.x, e.y) ?: return
                    val position = recyclerView.getChildAdapterPosition(child)
                    if (position == RecyclerView.NO_POSITION) return
                    if (position < 0 || position >= heroList.size) return

                    val account = heroList[position]
                    afficherMenuHero(account)
                }

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    return false
                }
            }
        )

        recyclerView.addOnItemTouchListener(
            object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    gestureDetector.onTouchEvent(e)
                    return false
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // CHARGEMENT DES HÉROS LOCAUX
    // ─────────────────────────────────────────────────────────────────

    private fun chargerHeros() {
        val progress = findViewById<ProgressBar>(R.id.progressAccountLoad)
        progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    AccountRegistry.rebuildAllMissingCachesFromRoom(this@AccountSelectActivity)
                }

                val caches = withContext(Dispatchers.IO) {
                    AccountRegistry.getAllHeroesCaches(this@AccountSelectActivity)
                        .sortedWith(
                            compareBy<AccountRegistry.AccountCache> {
                                AccountRegistry.getRememberedAccountEmail(
                                    this@AccountSelectActivity,
                                    it.uid
                                ).ifBlank { it.uid }
                            }.thenBy { it.slotNumber }
                        )
                }

                if (caches.isEmpty()) {
                    progress.visibility = View.GONE
                    Toast.makeText(
                        this@AccountSelectActivity,
                        "Aucun héros local n'a été trouvé sur cet appareil.",
                        Toast.LENGTH_LONG
                    ).show()
                    retournerTitre()
                    return@launch
                }

                heroList.clear()
                heroList.addAll(caches)

                adapter = AccountCardAdapter(
                    context = this@AccountSelectActivity,
                    accounts = heroList,
                    activeUid = AccountRegistry.getActiveUid(this@AccountSelectActivity),
                    onAccountSelected = { account ->
                        selectionnerHeros(account)
                    }
                )

                recyclerView.adapter = adapter
            } catch (_: Exception) {
                Toast.makeText(
                    this@AccountSelectActivity,
                    "Impossible de charger les héros locaux.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // SÉLECTION D'UN HÉROS LOCAL
    // ─────────────────────────────────────────────────────────────────

    private fun selectionnerHeros(account: AccountRegistry.AccountCache) {
        try {
            SoundManager.playSFX(this, R.raw.sfx_thunder_confirm)
        } catch (_: Exception) {
        }

        AccountRegistry.setActiveUid(this, account.uid)
        AccountRegistry.setActiveSlot(this, account.uid, account.slotNumber)
        AppDatabase.resetInstance()

        lifecycleScope.launch {
            delay(180L)
            SoundManager.stopMusic()
            startActivity(Intent(this@AccountSelectActivity, MoodActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // MENU D'ACTIONS RPG
    // ─────────────────────────────────────────────────────────────────

    private fun afficherMenuHero(account: AccountRegistry.AccountCache) {
        try {
            SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
        } catch (_: Exception) {
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(12))
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#1A0A00"))
            }
        }

        val title = TextView(this).apply {
            text = account.pseudo.ifBlank { "Héros" }
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFD700"))
            typeface = Typeface.DEFAULT_BOLD
        }

        val subtitle = TextView(this).apply {
            text = "Compte : ${
                AccountRegistry.getRememberedAccountEmail(this@AccountSelectActivity, account.uid)
                    .ifBlank { account.uid }
            }\nSlot : ${account.slotNumber}"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#F4E7B7"))
        }

        val btnPlay = creerBoutonRpg("JOUER CE HÉROS", "#1A0A00")
        val btnDelete = creerBoutonRpg("SUPPRIMER CE HÉROS DU TÉLÉPHONE", "#FFD7D7")
        val btnCancel = creerBoutonRpg("ANNULER", "#FFD700")

        content.addView(title)
        content.addView(espace(8))
        content.addView(subtitle)
        content.addView(espace(14))
        content.addView(btnPlay)
        content.addView(espace(8))
        content.addView(btnDelete)
        content.addView(espace(8))
        content.addView(btnCancel)

        val dialog = AlertDialog.Builder(this)
            .setView(content)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnPlay.setOnClickListener {
            dialog.dismiss()
            selectionnerHeros(account)
        }

        btnDelete.setOnClickListener {
            dialog.dismiss()
            confirmerSuppressionHero(account)
        }

        btnCancel.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun confirmerSuppressionHero(account: AccountRegistry.AccountCache) {
        try {
            SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
        } catch (_: Exception) {
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(12))
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#1A0A00"))
            }
        }

        val title = TextView(this).apply {
            text = "Effacer ce héros ?"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFD700"))
            typeface = Typeface.DEFAULT_BOLD
        }

        val message = TextView(this).apply {
            text = "${account.pseudo} sera retiré de ce téléphone.\nLe compte email associé restera disponible."
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#F4E7B7"))
        }

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val btnDelete = creerBoutonRpgLigne("EFFACER", "#FFD7D7", 1f, dp(6), 0)
        val btnCancel = creerBoutonRpgLigne("ANNULER", "#FFD700", 1f, 0, dp(6))

        buttons.addView(btnDelete)
        buttons.addView(btnCancel)

        content.addView(title)
        content.addView(espace(10))
        content.addView(message)
        content.addView(espace(14))
        content.addView(buttons)

        val dialog = AlertDialog.Builder(this)
            .setView(content)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnDelete.setOnClickListener {
            dialog.dismiss()
            supprimerHeroLocal(account)
        }

        btnCancel.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun supprimerHeroLocal(account: AccountRegistry.AccountCache) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    AccountRegistry.deleteHeroLocal(
                        this@AccountSelectActivity,
                        account.uid,
                        account.slotNumber
                    )
                }

                Toast.makeText(
                    this@AccountSelectActivity,
                    "Héros effacé du téléphone.",
                    Toast.LENGTH_LONG
                ).show()

                chargerHeros()
            } catch (_: Exception) {
                Toast.makeText(
                    this@AccountSelectActivity,
                    "Impossible d'effacer ce héros.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // BOUTON BAS : NOUVELLE AVENTURE
    // ─────────────────────────────────────────────────────────────────

    private fun setupBoutonNouvelleAventure() {
        findViewById<View>(R.id.btnNouvelleAventure)?.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }

            lifecycleScope.launch {
                delay(120L)
                startActivity(
                    Intent(this@AccountSelectActivity, LoginActivity::class.java).apply {
                        putExtra(LoginActivity.EXTRA_MODE, LoginActivity.MODE_INSCRIPTION)
                    }
                )
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // UI HELPERS
    // ─────────────────────────────────────────────────────────────────

    private fun creerBoutonRpg(texte: String, couleurTexte: String): FrameLayout {
        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
            isClickable = true
            isFocusable = true
        }

        frame.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_XY
            try {
                setImageResource(R.drawable.bg_temple_button)
            } catch (_: Exception) {
            }
        })

        frame.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_XY
            alpha = 0.30f
            try {
                setImageResource(R.drawable.bg_textelayout)
            } catch (_: Exception) {
            }
        })

        frame.addView(TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            text = texte
            textSize = 13f
            try {
                setTextColor(Color.parseColor(couleurTexte))
            } catch (_: Exception) {
                setTextColor(Color.WHITE)
            }
            typeface = Typeface.DEFAULT_BOLD
        })

        return frame
    }

    private fun creerBoutonRpgLigne(
        texte: String,
        couleurTexte: String,
        weight: Float,
        marginEnd: Int,
        marginStart: Int
    ): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                dp(46),
                weight
            ).also {
                it.marginEnd = marginEnd
                it.marginStart = marginStart
            }
            gravity = Gravity.CENTER
            text = texte
            textSize = 12f
            try {
                setTextColor(Color.parseColor(couleurTexte))
            } catch (_: Exception) {
                setTextColor(Color.WHITE)
            }
            typeface = Typeface.DEFAULT_BOLD
            try {
                setBackgroundResource(R.drawable.bg_temple_button)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#2A1200"))
            }
            isClickable = true
            isFocusable = true
            setPadding(dp(10), 0, dp(10), 0)
        }
    }

    private fun espace(value: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(value))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    // ─────────────────────────────────────────────────────────────────
    // CYCLE DE VIE
    // ─────────────────────────────────────────────────────────────────

    override fun onPause() {
        super.onPause()
        try {
            backgroundPlayer?.pause()
        } catch (_: Exception) {
        }
        SoundManager.pauseMusic()
    }

    override fun onResume() {
        super.onResume()
        try {
            backgroundPlayer?.play()
        } catch (_: Exception) {
        }

        if (!SoundManager.isPlayingMusic()) {
            SoundManager.resumeRememberedMusicDelayed(this, 300L)
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
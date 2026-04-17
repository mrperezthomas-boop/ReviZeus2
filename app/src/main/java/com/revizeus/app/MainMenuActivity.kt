package com.revizeus.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MainMenuActivity — Les Portes de l'Olympe
 * ──────────────────────────────────────────────────────────
 * Correctif ciblé :
 * - retour au flux stable pour "Nouveau compte"
 * - suppression de la dépendance à AccountEntryChoiceActivity
 * - conservation du reste de l'architecture existante
 *
 * Flux retenus :
 * ① Nouveau compte        → LoginActivity MODE_INSCRIPTION
 * ② Charger un compte     → LoginActivity MODE_CONNEXION
 * ③ Charger un héros      → AccountSelectActivity
 */
class MainMenuActivity : BaseActivity() {

    private var typewriterJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setupImmersiveMode()

        SoundManager.rememberMusic(R.raw.bgm_title_select)
        SoundManager.playMusicDelayed(this, R.raw.bgm_title_select, 300L)

        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@MainMenuActivity, TitleScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }

        setupBoutons()
    }

    private fun setupBoutons() {
        findViewById<FrameLayout>(R.id.btnDemarrerAventure).setOnClickListener {
            try { SoundManager.playSFX(this, R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            lifecycleScope.launch {
                delay(120L)
                SoundManager.stopMusic()
                startActivity(Intent(this@MainMenuActivity, LoginActivity::class.java).apply {
                    putExtra(LoginActivity.EXTRA_MODE, LoginActivity.MODE_INSCRIPTION)
                })
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }

        findViewById<FrameLayout>(R.id.btnChargerPartie).setOnClickListener {
            try { SoundManager.playSFX(this, R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            lifecycleScope.launch {
                delay(120L)
                chargerUnCompte()
            }
        }

        findViewById<FrameLayout>(R.id.btnChargerHeros).setOnClickListener {
            try { SoundManager.playSFX(this, R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            lifecycleScope.launch {
                delay(120L)
                chargerUnHeros()
            }
        }
    }

    private fun chargerUnCompte() {
        SoundManager.stopMusic()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            putExtra(LoginActivity.EXTRA_MODE, LoginActivity.MODE_CONNEXION)
        })
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun chargerUnHeros() {
        lifecycleScope.launch {
            var heroes = AccountRegistry.getAllHeroesCaches(this@MainMenuActivity)
            if (heroes.isEmpty()) {
                withContext(Dispatchers.IO) {
                    AccountRegistry.rebuildAllMissingCachesFromRoom(this@MainMenuActivity)
                }
                heroes = AccountRegistry.getAllHeroesCaches(this@MainMenuActivity)
            }

            if (heroes.isEmpty()) {
                afficherDialogueAucunHeros()
            } else {
                startActivity(Intent(this@MainMenuActivity, AccountSelectActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }
    }

    private fun afficherDialogueAucunHeros() {
        try { SoundManager.playSFX(this, R.raw.sfx_dialogue_blip) } catch (_: Exception) {}

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(12))
            try { setBackgroundResource(R.drawable.bg_rpg_dialog) }
            catch (_: Exception) { setBackgroundColor(Color.parseColor("#1A0A00")) }
        }

        val ivZeus = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(72)).also {
                it.gravity = Gravity.CENTER_HORIZONTAL
                it.bottomMargin = dp(12)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            try { setImageResource(R.drawable.ic_zeus_chibi) }
            catch (_: Exception) { setImageResource(android.R.drawable.sym_def_app_icon) }
        }

        val tvSpeech = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(20) }
            textSize = 14f
            setTextColor(Color.parseColor("#FFE0B2"))
            gravity = Gravity.CENTER
            typeface = Typeface.SERIF
            text = ""
        }

        val llBoutons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        layout.addView(ivZeus)
        layout.addView(tvSpeech)
        layout.addView(llBoutons)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(layout)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnCreer = creerBouton("⚔  CRÉER UN HÉROS", "#1A0A00", 1f, dp(6), 0)
        btnCreer.setOnClickListener {
            try { SoundManager.playSFX(this, R.raw.sfx_thunder_confirm) } catch (_: Exception) {}
            typewriterJob?.cancel()
            dialog.dismiss()
            lifecycleScope.launch {
                delay(150L)
                SoundManager.stopMusic()
                startActivity(Intent(this@MainMenuActivity, LoginActivity::class.java).apply {
                    putExtra(LoginActivity.EXTRA_MODE, LoginActivity.MODE_INSCRIPTION)
                })
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }

        val btnFermer = creerBouton("PLUS TARD", "#FFD700", 1f, 0, dp(0))
        btnFermer.setOnClickListener {
            try { SoundManager.playSFX(this, R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            typewriterJob?.cancel()
            dialog.dismiss()
        }

        llBoutons.addView(btnCreer)
        llBoutons.addView(btnFermer)
        dialog.show()

        val message = "Aucun héros n'a encore posé le pied sur cet appareil. Veux-tu en créer un maintenant ?"
        typewriterJob?.cancel()
        typewriterJob = lifecycleScope.launch {
            val builder = StringBuilder()
            message.forEach { c ->
                builder.append(c)
                tvSpeech.text = builder.toString()
                try { SoundManager.playSFXLow(this@MainMenuActivity, R.raw.sfx_dialogue_blip) } catch (_: Exception) {}
                delay(18L)
            }
        }
    }

    private fun creerBouton(
        texte: String,
        colorHex: String,
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
            setTextColor(Color.parseColor(colorHex))
            typeface = Typeface.DEFAULT_BOLD
            try { setBackgroundResource(R.drawable.bg_temple_button) }
            catch (_: Exception) { setBackgroundColor(Color.parseColor("#2A1200")) }
            isClickable = true
            isFocusable = true
            setPadding(dp(10), 0, dp(10), 0)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onPause() {
        super.onPause()
        SoundManager.pauseMusic()
    }

    override fun onResume() {
        super.onResume()
        if (!SoundManager.isPlayingMusic()) {
            SoundManager.resumeRememberedMusicDelayed(this, 300L)
        }
    }

    /**
     * BLOC A — Cleanup explicite du typewriterJob.
     * Évite les fuites mémoire si l'utilisateur navigue pendant l'animation
     * de dialogue lettre par lettre.
     */
    override fun onDestroy() {
        typewriterJob?.cancel()
        SoundManager.stopAllDialogueBlips()
        super.onDestroy()
    }
}

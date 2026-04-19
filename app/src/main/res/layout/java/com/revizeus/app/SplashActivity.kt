package com.revizeus.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ÉCRAN DE LANCEMENT — Séquence cinématique Zeus
 * ────────────────────────────────────────────────
 * Flux v8 :
 *  └── TOUJOURS → VideoPlayerActivity (demarrer_aventure.mp4, passable)
 *       └── TitleScreenActivity → MainMenuActivity
 *
 * CONSERVATION TOTALE :
 * - Toute la cinématique (logo, Zeus, foudre) est 100% conservée.
 * - Seule la destination finale change : on va d'abord vers
 *   VideoPlayerActivity qui joue demarrer_aventure.mp4 avant
 *   TitleScreenActivity.
 * - VideoPlayerActivity est skippable (double-tap), contrairement
 *   à IntroVideoActivity (revizeus_intro) qui reste non-passable.
 *
 * CORRECTIONS v5 (conservées) :
 *  ✅ FIX #11 : Premier éclair (flashOverlay) supprimé
 *  ✅ declencherVibrationFoudre() héritée de BaseActivity
 *
 * AJOUT v10 — MISE À JOUR DIVINE POST-SPLASH :
 * - Juste après le Splash, on intercepte désormais le flux pour savoir
 *   si une release non appliquée doit proposer son "réalignement".
 * - Si oui → GameUpdateActivity
 * - Sinon → VideoPlayerActivity comme avant
 * - La cinématique du Splash ne change pas : seul le point de sortie est enrichi.
 */
class SplashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        setupImmersiveMode()
        orchestrerLancementDivin()
    }

    private fun orchestrerLancementDivin() {
        val imgLogo = findViewById<ImageView>(R.id.imgLogo)
        val imgZeus = findViewById<ImageView>(R.id.imgBackgroundZeus)
        val flashOverlay = findViewById<ImageView>(R.id.flashOverlay)
        val imgFinalLightning = findViewById<ImageView>(R.id.imgFinalLightning)
        val viewFlash = findViewById<View>(R.id.viewFlash)

        val shakeAnim = AnimationUtils.loadAnimation(this, R.anim.shake)
        val flashAnim = AnimationUtils.loadAnimation(this, R.anim.flash_fade_out)

        lifecycleScope.launch {

            try {
                SoundManager.playSFX(this@SplashActivity, R.raw.sfx_app_start_pop)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            imgLogo.visibility = View.VISIBLE
            delay(2000)

            imgLogo.visibility = View.GONE
            imgZeus.visibility = View.VISIBLE
            try {
                SoundManager.playSFX(this@SplashActivity, R.raw.theme_splash)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            imgZeus.startAnimation(shakeAnim)
            declencherVibrationFoudre(300)
            flashOverlay.visibility = View.GONE
            delay(4000)

            try {
                SoundManager.playSFX(this@SplashActivity, R.raw.sfx_transition_thunder)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            imgFinalLightning.visibility = View.VISIBLE
            viewFlash.visibility = View.VISIBLE
            viewFlash.startAnimation(flashAnim)
            declencherVibrationFoudre(600)
            delay(500)

            naviguerVersProchaineEtape()
        }
    }

    /**
     * Point de sortie unique du Splash.
     *
     * On garde VideoPlayerActivity comme route normale.
     * La seule insertion est GameUpdateActivity lorsqu'une release doit
     * réaligner les données dérivées sans toucher aux progrès du héros.
     *
     * En v2, GameUpdateManager filtre aussi :
     * - premier lancement
     * - mode dev/debuggable
     * pour éviter toute gêne de boot côté développement.
     */
    private fun naviguerVersProchaineEtape() {
        val gate = GameUpdateManager.evaluate(this)

        val intent = if (gate.shouldShow) {
            Intent(this, GameUpdateActivity::class.java)
        } else {
            Intent(this, VideoPlayerActivity::class.java).apply {
                putExtra(VideoPlayerActivity.EXTRA_DESTINATION, VideoPlayerActivity.DEST_TITLE_SCREEN)
                putExtra(VideoPlayerActivity.EXTRA_SKIPPABLE, true)
            }
        }

        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
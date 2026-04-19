package com.revizeus.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Animatable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.load
import coil.request.ImageRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ═══════════════════════════════════════════════════════════════
 * GENDER ACTIVITY — Choix de genre avec labels WebP transparents
 * ═══════════════════════════════════════════════════════════════
 * Corrections apportées :
 * - txt_hero_blue / txt_heroine_pink restent fixes par défaut
 * - au clic, le label sélectionné passe en animated WebP transparent
 * - si l'on change d'option ou si l'écran est recréé, on revient au visuel fixe
 * - plus de carré noir car on n'utilise plus une vidéo MP4 pour ces labels
 * - correction Coil : plus d'appel invalide à imageLoader(...) dans le builder load
 *
 * CORRECTIFS v2 :
 * - le premier dialogue Zeus part désormais via post { } pour éviter le layout RPG vide
 * - la garde lifecycle STARTED a été retirée de la validation du typewriter
 *   car elle coupait le premier texte au lancement sur certains appareils
 * - le blip legacy est conservé pour respecter l'architecture de cet écran
 * ═══════════════════════════════════════════════════════════════
 */
class GenderActivity : BaseActivity() {

    private var sfxSelectPlayer: MediaPlayer? = null
    private var sfxBlipPlayer: MediaPlayer? = null
    private var selectedGender: String? = null
    private var typewriterJob: Job? = null

    /**
     * BLOC BLIP FANTÔME :
     * un nouveau texte rend immédiatement l'ancien muet, même si l'activité
     * n'a pas encore fini sa destruction.
     */
    private var typewriterSessionId: Long = 0L

    private lateinit var imgHero: ImageView
    private lateinit var imgHeroine: ImageView
    private lateinit var imgTxtHero: ImageView
    private lateinit var imgTxtHeroine: ImageView

    /**
     * Loader Coil dédié aux labels animés WebP/GIF.
     * On le garde en lazy pour éviter de le recréer sans arrêt.
     */
    private val animatedLabelLoader: ImageLoader by lazy {
        ImageLoader.Builder(this)
            .components {
                add(ImageDecoderDecoder.Factory())
                add(GifDecoder.Factory())
            }
            .build()
    }

    private val grayFilter: ColorMatrixColorFilter by lazy {
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        ColorMatrixColorFilter(matrix)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gender)

        val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
        (rootLayout.background as? Animatable)?.start()

        try {
            SoundManager.playMusic(this, R.raw.music_gender_selection)
        } catch (_: Exception) {
        }

        sfxSelectPlayer = MediaPlayer.create(this, R.raw.sfx_statue_select)
            ?: MediaPlayer.create(this, R.raw.sfx_epic_chibi_select)

        sfxBlipPlayer = MediaPlayer.create(this, R.raw.sfx_dialogue_blip)
        sfxBlipPlayer?.setVolume(0.15f, 0.15f)

        val btnHero = findViewById<LinearLayout>(R.id.btnHero)
        val btnHeroine = findViewById<LinearLayout>(R.id.btnHeroine)
        imgHero = findViewById(R.id.imgHero)
        imgHeroine = findViewById(R.id.imgHeroine)
        imgTxtHero = findViewById(R.id.imgTxtHero)
        imgTxtHeroine = findViewById(R.id.imgTxtHeroine)
        val tvZeusSpeech = findViewById<TextView>(R.id.tvZeusSpeech)
        val layoutConfirm = findViewById<LinearLayout>(R.id.layoutConfirmButtons)
        val btnYes = findViewById<Button>(R.id.btnYes)
        val btnNo = findViewById<Button>(R.id.btnNo)

        onBackPressedDispatcher.addCallback(this) {
            try { SoundManager.playSFX(this@GenderActivity, R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            retournerEcranPrecedent()
        }

        applyDeselectedStyle(imgHero, imgTxtHero)
        applyDeselectedStyle(imgHeroine, imgTxtHeroine)
        resetAnimatedLabels()

        tvZeusSpeech.post {
            typeWriter(
                tvZeusSpeech,
                "Choisis ton essence. Le panneau sélectionné prendra vie sous tes yeux."
            )
        }

        btnHero.setOnClickListener {
            selectedGender = "Garçon"
            onGenderSelected(isHero = true)
            typeWriter(tvZeusSpeech, "Tu as choisi la voie des Héros. Le tonnerre guide déjà tes pas.")
            layoutConfirm.visibility = View.VISIBLE
        }

        btnHeroine.setOnClickListener {
            selectedGender = "Fille"
            onGenderSelected(isHero = false)
            typeWriter(tvZeusSpeech, "Tu as choisi la voie des Héroïnes. La sagesse divine veille sur toi.")
            layoutConfirm.visibility = View.VISIBLE
        }

        btnYes.setOnClickListener {
            val gender = selectedGender ?: return@setOnClickListener
            lifecycleScope.launch {
                delay(300L)
                startActivity(
                    Intent(this@GenderActivity, AvatarActivity::class.java)
                        .putExtra("USER_GENDER", gender)
                )
                finish()
            }
        }

        btnNo.setOnClickListener {
            selectedGender = null
            resetAnimatedLabels()
            applyDeselectedStyle(imgHero, imgTxtHero)
            applyDeselectedStyle(imgHeroine, imgTxtHeroine)
            layoutConfirm.visibility = View.GONE
            typeWriter(
                tvZeusSpeech,
                "Très bien. Observe mieux les deux voies avant de sceller ton destin."
            )
        }
    }

    private fun onGenderSelected(isHero: Boolean) {
        try {
            sfxSelectPlayer?.start()
        } catch (_: Exception) {
        }

        if (isHero) {
            try {
                SoundManager.playMusic(this, R.raw.bgm_avatar_homme)
            } catch (_: Exception) {
            }

            applySelectedStyle(imgHero, imgTxtHero)
            applyDeselectedStyle(imgHeroine, imgTxtHeroine)
            loadAnimatedHeroLabel()
            setStaticHeroineLabel()
        } else {
            try {
                SoundManager.playMusic(this, R.raw.bgm_avatar_fille)
            } catch (_: Exception) {
            }

            applySelectedStyle(imgHeroine, imgTxtHeroine)
            applyDeselectedStyle(imgHero, imgTxtHero)
            loadAnimatedHeroineLabel()
            setStaticHeroLabel()
        }
    }

    private fun applySelectedStyle(statue: ImageView, label: ImageView) {
        statue.clearColorFilter()
        label.clearColorFilter()

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(statue, View.SCALE_X, statue.scaleX, 1.08f),
                ObjectAnimator.ofFloat(statue, View.SCALE_Y, statue.scaleY, 1.08f),
                ObjectAnimator.ofFloat(label, View.SCALE_X, label.scaleX, 1.04f),
                ObjectAnimator.ofFloat(label, View.SCALE_Y, label.scaleY, 1.04f)
            )
            duration = 220L
            start()
        }

        try {
            statue.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse_glow))
        } catch (_: Exception) {
        }
    }

    private fun applyDeselectedStyle(statue: ImageView, label: ImageView) {
        statue.colorFilter = grayFilter
        label.colorFilter = grayFilter
        statue.animate().scaleX(0.94f).scaleY(0.94f).setDuration(160L).start()
        label.animate().scaleX(0.96f).scaleY(0.96f).setDuration(160L).start()
    }

    private fun resetAnimatedLabels() {
        setStaticHeroLabel()
        setStaticHeroineLabel()
    }

    /**
     * Label fixe héros.
     */
    private fun setStaticHeroLabel() {
        imgTxtHero.load(R.drawable.txt_hero_blue) {
            crossfade(false)
            allowHardware(false)
        }
    }

    /**
     * Label fixe héroïne.
     */
    private fun setStaticHeroineLabel() {
        imgTxtHeroine.load(R.drawable.txt_heroine_pink) {
            crossfade(false)
            allowHardware(false)
        }
    }

    /**
     * Charge le WebP animé transparent du label héros avec un ImageLoader dédié.
     */
    private fun loadAnimatedHeroLabel() {
        val request = ImageRequest.Builder(this)
            .data(R.drawable.txt_hero_animated)
            .target(imgTxtHero)
            .crossfade(false)
            .allowHardware(false)
            .build()

        animatedLabelLoader.enqueue(request)
    }

    /**
     * Charge le WebP animé transparent du label héroïne avec un ImageLoader dédié.
     */
    private fun loadAnimatedHeroineLabel() {
        val request = ImageRequest.Builder(this)
            .data(R.drawable.txt_heroine_animated)
            .target(imgTxtHeroine)
            .crossfade(false)
            .allowHardware(false)
            .build()

        animatedLabelLoader.enqueue(request)
    }

    private fun typeWriter(target: TextView, text: String) {
        typewriterSessionId += 1L
        val localSessionId = typewriterSessionId

        typewriterJob?.cancel()
        stopLegacyDialogueBlipPlayer()
        SoundManager.stopAllDialogueBlips()

        typewriterJob = lifecycleScope.launch {
            target.text = ""
            for (i in text.indices) {
                if (localSessionId != typewriterSessionId || !isDialogueScreenUsable(target)) break

                target.text = text.substring(0, i + 1)
                try {
                    sfxBlipPlayer?.seekTo(0)
                    SoundManager.playSFXDialogueBlip(this@GenderActivity, R.raw.sfx_dialogue_blip)                } catch (_: Exception) {
                }
                delay(26L)
            }
        }
    }

    private fun isDialogueScreenUsable(target: TextView): Boolean {
        return !isFinishing &&
            !isDestroyed &&
            target.isAttachedToWindow &&
            target.visibility == View.VISIBLE
    }

    private fun stopLegacyDialogueBlipPlayer() {
        try {
            if (sfxBlipPlayer?.isPlaying == true) {
                sfxBlipPlayer?.pause()
            }
            sfxBlipPlayer?.seekTo(0)
        } catch (_: Exception) {
        }
    }

    private fun retournerEcranPrecedent() {
        val activeUid = AccountRegistry.getActiveUid(this)
        val hasSession = FirebaseAuthManager.hasActiveSession()

        val intent = when {
            hasSession && activeUid.isNotBlank() -> Intent(this, HeroSelectActivity::class.java).apply {
                putExtra(HeroSelectActivity.EXTRA_FIREBASE_UID, activeUid)
            }

            OnboardingSession.isReady() -> Intent(this, AuthActivity::class.java)

            else -> Intent(this, TitleScreenActivity::class.java)
        }

        startActivity(intent)
        finish()
    }

    override fun onPause() {
        super.onPause()
        typewriterSessionId += 1L
        typewriterJob?.cancel()
        SoundManager.stopAllDialogueBlips()
        stopLegacyDialogueBlipPlayer()
    }

    override fun onDestroy() {
        typewriterJob?.cancel()
        SoundManager.stopAllDialogueBlips()
        stopLegacyDialogueBlipPlayer()

        try {
            animatedLabelLoader.shutdown()
        } catch (_: Exception) {
        }

        sfxSelectPlayer?.release()
        sfxSelectPlayer = null

        sfxBlipPlayer?.release()
        sfxBlipPlayer = null

        super.onDestroy()
    }
}

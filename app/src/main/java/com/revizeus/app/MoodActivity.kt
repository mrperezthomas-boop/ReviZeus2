package com.revizeus.app

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.databinding.ActivityMoodBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * MOOD ACTIVITY — La pesée de l'âme
 * ──────────────────────────────────────────
 * Nom : MoodActivity
 * Utilité : Capture l'état émotionnel pour influencer le ton de l'IA.
 * Connexion : Transitionne vers DashboardActivity.
 * * MODIFICATIONS APPLIQUÉES :
 * - FIX BGM : Lancement prioritaire de la musique.
 * - PRÉ-CHARGEMENT SFX : On "chauffe" le SoundPool avant le dialogue pour éviter le lag audio.
 * - SÉCURITÉ : Délai de 600ms pour laisser le MediaPlayer se stabiliser.
 */
class MoodActivity : BaseActivity() {

    private lateinit var binding: ActivityMoodBinding

    // Moteur d'animation RPG
    private val godAnim = GodSpeechAnimator()
    private var typewriterJob: Job? = null

    // Prophéties variées pour les connexions récurrentes
    private val prophetiesHumeur = listOf(
        "Alors, Héros... comment vibre ta foudre intérieure aujourd'hui ?",
        "Ton esprit est-il calme comme l'éther ou agité comme une tempête ?",
        "Dis-moi dans quel état se trouve ton âme avant d'ouvrir les vannes du savoir.",
        "Même les dieux ont leurs jours sombres. Quelle est la couleur de ton ciel intérieur ?",
        "L'Olympe t'écoute. Ton énergie actuelle dictera ma prochaine prophétie.",
        "Sens-tu le poids du monde ou l'étincelle de la victoire dans ton cœur ?"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Règle d'or : Format Portrait forcé
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // 1. LANCEMENT PRIORITAIRE DU BGM
        try {
            SoundManager.playMusic(this, R.raw.bgm_mood_selection)
        } catch (_: Exception) {
            try { SoundManager.playMusic(this, R.raw.bgm_dashboard) } catch(_: Exception){}
        }

        // 2. PRÉ-CHARGEMENT DU BLIIP (Optimisation SoundManager)
        // On force le SoundManager à charger le son maintenant, en silence (volume 0),
        // pour qu'il soit prêt en mémoire vive avant que le typewriter ne commence.
        SoundManager.playSFXAtVolume(this, R.raw.sfx_dialogue_blip, 0f)

        val sharedPref = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        val isFirstTime = sharedPref.getBoolean("IS_FIRST_TIME", true)

        // Activation du panneau de dialogue RPG
        binding.layoutDialogRPG.visibility = View.VISIBLE

        // 3. LANCEMENT DU DIALOGUE AVEC DÉLAI DE SÉCURITÉ
        // 600ms permettent au BGM de démarrer son flux sans conflit avec le SoundPool.
        lifecycleScope.launch {
            delay(600)
            if (isFirstTime) {
                appelerTexteRPG("Bienvenue, jeune prodige. Pour que j'adapte ma pédagogie divine, je dois savoir comment tu te sens.")
                sharedPref.edit().putBoolean("IS_FIRST_TIME", false).apply()
            } else {
                val texteAleatoire = prophetiesHumeur[Random.nextInt(prophetiesHumeur.size)]
                appelerTexteRPG(texteAleatoire)
            }
        }

        setupMoodButtons()

        binding.btnInfoMood.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            appelerTexteRPG("Approche, mortel. Je vais te révéler ce qu’est RéviZeus.")
            startActivity(Intent(this, RevizeusInfoActivity::class.java))
        }
    }

    /**
     * Configuration des clics sur les boutons d'humeur.
     */
    private fun setupMoodButtons() {
        binding.btnMoodHappy.setOnClickListener    { saveMoodAndGo("JOYEUX") }
        binding.btnMoodTired.setOnClickListener    { saveMoodAndGo("FATIGUÉ") }
        binding.btnMoodStressed.setOnClickListener { saveMoodAndGo("STRESSÉ") }
    }

    /**
     * Sauvegarde l'humeur pour l'IA et change d'écran.
     */
    private fun saveMoodAndGo(mood: String) {
        val sfx = when (mood) {
            "JOYEUX"  -> R.raw.sfx_mood_happy
            "FATIGUÉ" -> R.raw.sfx_mood_tired
            "STRESSÉ" -> R.raw.sfx_mood_stressed
            else      -> R.raw.sfx_avatar_confirm
        }
        try { SoundManager.playSFX(this, sfx) } catch (_: Exception) {}

        val sharedPref = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("CURRENT_MOOD", mood).apply()

        startActivity(Intent(this, DashboardActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    /**
     * Fait parler Zeus avec l'effet typewriter.
     */
    private fun appelerTexteRPG(texte: String) {
        typewriterJob?.cancel()
        typewriterJob = godAnim.typewriteSimple(
            scope = lifecycleScope,
            chibiView = binding.imgZeusDialog,
            textView = binding.tvZeusSpeech,
            text = texte,
            context = this
        )
    }

    override fun onPause() {
        super.onPause()
        typewriterJob?.cancel()
        godAnim.stopSpeaking(binding.imgZeusDialog)
    }

    override fun onDestroy() {
        super.onDestroy()
        godAnim.release(binding.imgZeusDialog)
    }
}
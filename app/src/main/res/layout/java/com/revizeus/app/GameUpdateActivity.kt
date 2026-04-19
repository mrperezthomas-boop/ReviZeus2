package com.revizeus.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.databinding.ActivityGameUpdateBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * GameUpdateActivity
 * ─────────────────────────────────────────────────────────────
 * V2 PROPRE
 *
 * RÈGLES :
 * - pas de maj.txt
 * - design centré sur panneaux RPG propres
 * - pas de bloc "release notes"
 * - timeout métier géré côté manager
 * - auto-sortie courte après succès
 *
 * CORRECTIF AUDIO :
 * - cet écran possède désormais sa propre BGM dédiée (`bgm_migration`)
 * - la musique est lancée uniquement sur cet écran
 * - la musique est arrêtée proprement avant la sortie vers VideoPlayerActivity
 * - on évite ainsi tout chevauchement avec l'audio du splash / de la vidéo / du dashboard
 */
class GameUpdateActivity : BaseActivity() {

    private lateinit var binding: ActivityGameUpdateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setupImmersiveMode()

        // Correctif audio : on force ici une BGM spécifique à l'écran de migration.
        // L'usage du délai évite les conflits de transition après le Splash.
        SoundManager.playMusicDelayed(this, R.raw.bgm_migration)

        configurerEcran()
    }

    private fun configurerEcran() {
        val gate = GameUpdateManager.evaluate(this)

        binding.tvTitle.text = "⚡ RÉALIGNEMENT DE L’OLYMPE ⚡"
        binding.tvSubtitle.text = "Les systèmes secondaires du monde peuvent être remis en ordre sans toucher à la progression du héros."
        binding.tvVersionValue.text = "v${gate.currentVersionName} (${gate.currentVersionCode})"
        binding.tvReasonValue.text = gate.reason

        if (gate.isRecoveryMode) {
            binding.tvRecoveryBadge.visibility = View.VISIBLE
            binding.tvRecoveryBadge.text = "RÉCUPÉRATION OBLIGATOIRE"
            binding.btnLater.visibility = View.GONE
        } else {
            binding.tvRecoveryBadge.visibility = View.GONE
            binding.tvRecoveryBadge.text = ""
            binding.btnLater.visibility = View.VISIBLE
        }

        binding.progressUpdate.max = 100
        binding.progressUpdate.progress = 0
        binding.tvProgressPercent.text = "0%"
        binding.tvProgressStep.text = "Athéna vérifie les archives sacrées..."
        binding.tvUpdateHint.text = "Le profil, l’avatar, les monnaies, les savoirs, l’inventaire et la mémoire seront conservés."

        lifecycleScope.launch {
            val summary = GameUpdateManager.loadPreservationSummaryText(this@GameUpdateActivity)
            binding.tvPreservedContent.text = summary
        }

        binding.btnApplyUpdate.setOnClickListener {
            lancerMiseAJour()
        }

        binding.btnLater.setOnClickListener {
            GameUpdateManager.snoozeCurrentVersion(this)
            allerVersVideoPlayer()
        }
    }

    private fun lancerMiseAJour() {
        binding.btnApplyUpdate.isEnabled = false
        binding.btnApplyUpdate.alpha = 0.65f

        binding.btnLater.isEnabled = false
        binding.btnLater.alpha = 0.65f

        binding.progressUpdate.progress = 0
        binding.tvProgressPercent.text = "0%"
        binding.tvProgressStep.text = "Zeus invoque le réalignement..."
        binding.tvUpdateHint.text = "Ne ferme pas l’application pendant le rituel."

        lifecycleScope.launch {
            val result = GameUpdateManager.applyUpdate(this@GameUpdateActivity) { progress, message ->
                binding.progressUpdate.progress = progress
                binding.tvProgressPercent.text = "$progress%"
                binding.tvProgressStep.text = message
            }

            if (result.success) {
                binding.tvProgressPercent.text = "100%"
                binding.tvProgressStep.text = "⚡ L’Olympe est réaligné."
                binding.tvUpdateHint.text = result.message
                delay(450)
                allerVersVideoPlayer()
            } else {
                binding.tvProgressStep.text = "❌ Le réalignement a échoué."
                binding.tvUpdateHint.text = result.message

                binding.btnApplyUpdate.isEnabled = true
                binding.btnApplyUpdate.alpha = 1f

                val gate = GameUpdateManager.evaluate(this@GameUpdateActivity)
                if (!gate.isRecoveryMode) {
                    binding.btnLater.isEnabled = true
                    binding.btnLater.alpha = 1f
                }
            }
        }
    }

    private fun allerVersVideoPlayer() {
        // Correctif audio cross-activity : on coupe explicitement la BGM de migration
        // avant la vidéo de démarrage pour éviter tout chevauchement sonore.
        SoundManager.stopMusic()

        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
            putExtra(VideoPlayerActivity.EXTRA_DESTINATION, VideoPlayerActivity.DEST_TITLE_SCREEN)
            putExtra(VideoPlayerActivity.EXTRA_SKIPPABLE, true)
        }

        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Sécurité supplémentaire : si l'activité est détruite hors navigation normale,
        // on arrête aussi la BGM pour ne jamais laisser un fond migration persister.
        if (isFinishing) {
            SoundManager.stopMusic()
        }
    }
}

package com.revizeus.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.databinding.ActivityOraclePromptBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OraclePromptActivity : BaseActivity() {

    private lateinit var binding: ActivityOraclePromptBinding
    private var backgroundHelper: AnimatedBackgroundHelper? = null
    private var previousRememberedMusicResId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOraclePromptBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        previousRememberedMusicResId = try { SoundManager.getRememberedMusicResId() } catch (_: Exception) { -1 }

        backgroundHelper = AnimatedBackgroundHelper(
            targetView = binding.root,
            particlesView = binding.oraclePromptParticles
        )
        backgroundHelper?.start(
            accentColor = 0xFFFFD700.toInt(),
            mode = OlympianParticlesView.ParticleMode.SAVOIR
        )

        binding.btnBackPrompt.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_transition_thunder)
            } catch (_: Exception) {}

            val intent = Intent(this, DashboardActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            finish()
        }

        binding.btnInvokePrompt.setOnClickListener {
            val promptText = binding.etOraclePrompt.text?.toString()?.trim().orEmpty()

            if (promptText.isBlank()) {
                Toast.makeText(this, "Écris d'abord ta demande à l'Oracle.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading()
            binding.btnInvokePrompt.isEnabled = false
            binding.btnBackPrompt.isEnabled = false

            lifecycleScope.launch {
                delay(220L)

                try {
                    val intent = Intent(this@OraclePromptActivity, ResultActivity::class.java).apply {
                        putExtra("FREE_TEXT_INPUT", promptText)
                    }
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Log.e("REVIZEUS_ORACLE_PROMPT", "Erreur ouverture ResultActivity : ${e.message}", e)
                    Toast.makeText(
                        this@OraclePromptActivity,
                        "Impossible d'invoquer le résultat divin.",
                        Toast.LENGTH_SHORT
                    ).show()
                    hideLoading()
                    binding.btnInvokePrompt.isEnabled = true
                    binding.btnBackPrompt.isEnabled = true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            SoundManager.cancelDelayedMusic()
            SoundManager.playMusic(this, R.raw.bgm_oracle_prompt)
            SoundManager.rememberMusic(R.raw.bgm_oracle_prompt)
        } catch (e: Exception) {
            Log.e("REVIZEUS_ORACLE_PROMPT", "Erreur BGM OraclePrompt : ${e.message}", e)
        }
    }

    override fun onPause() {
        super.onPause()
        backgroundHelper?.stop()
    }

    override fun onDestroy() {
        backgroundHelper?.stop()

        if (isFinishing) {
            try {
                val fallbackRes = if (previousRememberedMusicResId != -1) {
                    previousRememberedMusicResId
                } else {
                    R.raw.bgm_oracle
                }
                SoundManager.rememberMusic(fallbackRes)
            } catch (_: Exception) {
            }
        }

        super.onDestroy()
    }
}

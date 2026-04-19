package com.revizeus.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.databinding.ActivityAuthBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthActivity : BaseActivity() {

    private lateinit var binding: ActivityAuthBinding
    private var typewriterJob: Job? = null
    private var typewriterSessionId: Long = 0L
    private var arrowView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val hasFirebaseSession = FirebaseAuthManager.hasActiveSession()
        val hasPendingOnboarding = OnboardingSession.isReady()

        if (!hasFirebaseSession && !hasPendingOnboarding) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        try {
            SoundManager.playMusic(this, R.raw.music_gender_selection)
        } catch (_: Exception) {
        }

        val classes = arrayOf(
            "CP", "CE1", "CE2", "CM1", "CM2",
            "6ème", "5ème", "4ème", "3ème",
            "2nde", "1ère", "Terminale",
            "Hors scolarité"
        )

        val adapter = ArrayAdapter(this, R.layout.spinner_item, classes)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        binding.spinnerClass.adapter = adapter

        binding.etParentEmail.setText(ParentSummaryManager.getParentEmail(this))
        binding.switchParentSummary.isChecked = ParentSummaryManager.isParentSummaryEnabled(this)

        // Le bouton retour XML est caché. On le remplace par une flèche texte
        // jaune en HAUT À GAUCHE comme demandé.
        binding.btnReturnTitle.visibility = View.GONE

        ajouterFlecheRetour()
        repositionnerForgerMonDestinCommeDemande()

        binding.switchParentSummary.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                afficherTexteRPG("Les dieux enverront tes résultats à tes parents.")
            }
        }

        binding.tvZeusSpeech.post {
            afficherTexteRPG("Dis-moi qui tu es, jeune héros.")
            repositionnerForgerMonDestinCommeDemande()
        }

        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@AuthActivity, TitleScreenActivity::class.java))
            finish()
        }

        binding.btnRegister.setOnClickListener {
            val pseudo = binding.etPseudo.text.toString().trim()
            val ageStr = binding.etAge.text.toString().trim()
            val userClass = binding.spinnerClass.selectedItem.toString()
            val parentEmail = binding.etParentEmail.text.toString().trim()
            val parentSummaryEnabled = binding.switchParentSummary.isChecked

            if (pseudo.isEmpty() || ageStr.isEmpty()) {
                Toast.makeText(this, "Pseudo et âge requis", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = FirebaseAuthManager.getCurrentUser()
            val sharedPref = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
            val onboardingEmail = if (OnboardingSession.isReady()) OnboardingSession.email else ""

            with(sharedPref.edit()) {
                putString("AVATAR_PSEUDO", pseudo)
                putInt("USER_AGE", ageStr.toIntOrNull() ?: 15)
                putString("USER_CLASS", userClass)

                if (currentUser != null) {
                    putBoolean("HAS_ACCOUNT", true)
                    putString("ACCOUNT_EMAIL", currentUser.email ?: onboardingEmail)
                    putString("FIREBASE_UID", currentUser.uid)
                } else if (onboardingEmail.isNotBlank()) {
                    putString("PENDING_ACCOUNT_EMAIL", onboardingEmail)
                }

                putString("PARENT_EMAIL", parentEmail)
                putBoolean("PARENT_SUMMARY_ENABLED", parentSummaryEnabled)
                apply()
            }

            currentUser?.uid?.let {
                AccountRegistry.registerUid(this, it)
                AccountRegistry.setActiveUid(this, it)
            }

            lifecycleScope.launch {
                delay(200)
                startActivity(Intent(this@AuthActivity, GenderActivity::class.java))
                finish()
            }
        }
    }

    /**
     * Place "Forger mon destin" dans la zone située ENTRE le switch parent
     * et le bloc bg_rpg_dialog de Zeus, au lieu de l'envoyer tout en haut.
     *
     * On travaille avec les positions réelles à l'écran :
     * - haut de zone = bas du switch parent + marge
     * - bas de zone  = haut du conteneur du dialogue Zeus - hauteur bouton - marge
     * Puis on place le bouton au milieu de cette zone.
     *
     * Cette approche garde le bouton dans sa vraie zone visuelle,
     * sans cast invalide ni translation arbitraire vers le haut.
     */
    private fun repositionnerForgerMonDestinCommeDemande() {
        binding.root.post {
            try {
                val dialogueContainer = binding.tvZeusSpeech.parent as? View ?: return@post

                val topZone = binding.switchParentSummary.y + binding.switchParentSummary.height + dp(16)
                val bottomZone = dialogueContainer.y - binding.btnRegister.height - dp(16)

                if (bottomZone <= topZone) {
                    // Si la zone est trop petite, on garde simplement le bouton visible,
                    // au-dessus du dialogue, sans le propulser en haut de l'écran.
                    binding.btnRegister.bringToFront()
                    binding.btnRegister.elevation = 50f
                    return@post
                }

                val desiredY = topZone + ((bottomZone - topZone) / 2f)

                binding.btnRegister.y = desiredY
                binding.btnRegister.translationY = 0f
                binding.btnRegister.bringToFront()
                binding.btnRegister.elevation = 50f
            } catch (_: Exception) {
                try {
                    binding.btnRegister.translationY = 0f
                    binding.btnRegister.bringToFront()
                    binding.btnRegister.elevation = 50f
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Ajoute une grosse flèche texte jaune en HAUT À GAUCHE.
     * Pas de drawable, juste un caractère comme demandé.
     */
    private fun ajouterFlecheRetour() {
        val root = binding.root as? ConstraintLayout ?: return

        val existingArrow = arrowView
        if (existingArrow != null) {
            try {
                existingArrow.bringToFront()
                existingArrow.elevation = 60f
            } catch (_: Exception) {
            }
            return
        }

        val arrow = TextView(this).apply {
            id = View.generateViewId()
            text = "←"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFD700.toInt())
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = dp(18)
                marginStart = dp(18)
            }
            setOnClickListener {
                startActivity(Intent(this@AuthActivity, TitleScreenActivity::class.java))
                finish()
            }
        }

        root.addView(arrow)
        arrow.bringToFront()
        arrow.elevation = 60f
        arrowView = arrow
    }

    private fun afficherTexteRPG(texte: String) {
        typewriterSessionId += 1L
        val localId = typewriterSessionId

        typewriterJob?.cancel()

        typewriterJob = lifecycleScope.launch {
            binding.tvZeusSpeech.text = ""

            for (i in texte.indices) {
                if (localId != typewriterSessionId) break

                binding.tvZeusSpeech.text = texte.substring(0, i + 1)

                try {
                    SoundManager.playSFXLow(this@AuthActivity, R.raw.sfx_dialogue_blip)
                } catch (_: Exception) {
                }

                delay(30)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        ajouterFlecheRetour()
        repositionnerForgerMonDestinCommeDemande()
    }

    override fun onPause() {
        super.onPause()
        typewriterSessionId += 1L
        typewriterJob?.cancel()
        SoundManager.stopAllDialogueBlips()
    }

    override fun onDestroy() {
        typewriterJob?.cancel()
        SoundManager.stopAllDialogueBlips()
        super.onDestroy()
    }
}

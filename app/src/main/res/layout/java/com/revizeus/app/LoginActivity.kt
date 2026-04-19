package com.revizeus.app

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.revizeus.app.databinding.ActivityLoginBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * LoginActivity — version corrigée compilable.
 *
 * CORRECTIF AUTH PRINCIPAL :
 * - Le mode inscription ne tente plus de connexion Firebase.
 * - Le mode connexion reste le seul chemin qui appelle signIn Firebase.
 * - La création Firebase réelle reste en AvatarActivity.
 *
 * CORRECTIF COMPILATION :
 * - Suppression de toute dépendance à des API fantômes comme
 *   AccountRegistry.getAllLocalAccounts(...) ou LocalAccountSummary.
 * - Le centre de gestion de compte reste présent, mais en version compatible
 *   avec le socle réellement visible dans ton projet.
 */
class LoginActivity : BaseActivity() {

    companion object {
        const val EXTRA_MODE = "LOGIN_MODE"
        const val MODE_CONNEXION = "connexion"
        const val MODE_INSCRIPTION = "inscription"

        /**
         * Clé de flow persistée temporairement pour distinguer clairement :
         * - l'inscription d'un nouveau compte
         * - l'ajout d'un héros sur un compte déjà connecté
         *
         * On évite ainsi que AvatarActivity réutilise par erreur
         * un ancien OnboardingSession encore présent en mémoire.
         */
        private const val PREF_HERO_CREATION_MODE = "HERO_CREATION_MODE"
        private const val HERO_CREATION_MODE_NEW_ACCOUNT = "new_account"
        private const val HERO_CREATION_MODE_EXISTING_ACCOUNT = "existing_account"
    }

    private lateinit var binding: ActivityLoginBinding
    private var isRegisterMode = true
    private var typewriterJob: Job? = null
    private var isPasswordVisible = false
    private var hasNavigated = false
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val idToken = account.idToken.orEmpty()

            if (idToken.isBlank()) {
                verrouillerUiPendantRequete(false)
                showError("Impossible de récupérer le jeton Google.")
                afficherTexteRPG("Le sceau Google n'a pas pu être confirmé par l'Olympe.")
                return@registerForActivityResult
            }

            FirebaseAuthManager.signInWithGoogle(
                idToken = idToken,
                onSuccess = { user ->
                    finaliserConnexionFirebase(user, normalizeEmailForFirebase(user.email.orEmpty()))
                },
                onError = { message ->
                    runOnUiThread {
                        verrouillerUiPendantRequete(false)
                        showError(message)
                        afficherTexteRPG("L'accès par Google a été refusé pour l'instant.")
                    }
                }
            )
        } catch (_: Exception) {
            verrouillerUiPendantRequete(false)
            showError("Connexion Google annulée ou invalide.")
            afficherTexteRPG("Le portail Google s'est refermé avant la bénédiction finale.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Désactive l'autofill système / Samsung sur l'écran d'authentification
        // pour éviter la proposition intrusive d'enregistrement du mot de passe.
        try {
            binding.root.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            binding.etEmail.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            binding.etPassword.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        } catch (_: Exception) {
        }

        binding.tvSwitchMode.paintFlags =
            binding.tvSwitchMode.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.tvForgotPassword.paintFlags =
            binding.tvForgotPassword.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        try {
            SoundManager.playMusic(this, R.raw.music_gender_selection)
        } catch (_: Exception) {
        }

        isRegisterMode = when (intent.getStringExtra(EXTRA_MODE)) {
            MODE_CONNEXION -> false
            MODE_INSCRIPTION -> true
            else -> true
        }

        setupGoogleSignIn()
        setupGoogleInlineUi()
        updateUI()
        appliquerEtatVisibiliteMotDePasse()
        afficherTexteRPG("Bienvenue, mortel ! Forge ton compte pour accéder à l'Olympe du Savoir !")

        binding.btnSubmit.setOnClickListener { handleSubmit() }
        binding.btnGoogleInline.setOnClickListener { handleGoogleInlineClick() }

        binding.tvSwitchMode.setOnClickListener {
            isRegisterMode = !isRegisterMode
            updateUI()
        }

        binding.tvForgotPassword.setOnClickListener { handleForgotPassword() }

        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            appliquerEtatVisibiliteMotDePasse()
        }

        binding.btnInfoLogin.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            afficherTexteRPG("Approche, mortel. Je vais te révéler ce qu'est RéviZeus.")
            startActivity(Intent(this, RevizeusInfoActivity::class.java))
        }

        ajouterBoutonGestionCompte()
    }

    private fun handleSubmit() {
        val rawEmail = binding.etEmail.text.toString()
        val email = normalizeEmailForFirebase(rawEmail)
        val password = binding.etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showError("Zeus exige un email et un mot de passe !")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Cet email n'est pas valide.")
            return
        }

        if (isRegisterMode) {

            if (password.length < 6) {
                showError("Mot de passe trop court (6 caractères minimum).")
                return
            }

            /**
             * CORRECTIF CRITIQUE 2026 :
             * En mode inscription, il ne faut jamais pré-tester l'email via signIn().
             *
             * Pourquoi :
             * - Firebase peut activer la protection contre l'énumération des emails.
             * - Dans ce cas, signInWithEmailAndPassword(...) peut renvoyer une erreur générique
             *   de type "invalid login credentials" même pour un email qui n'existe pas.
             * - Résultat : l'app croit à tort que l'email existe déjà.
             *
             * Flow désormais voulu :
             * 1) on continue le flow d'inscription normal vers AuthActivity / AvatarActivity
             * 2) AvatarActivity tente le vrai createAccount(...)
             * 3) si l'email existe déjà, AvatarActivity le détecte proprement via
             *    createUserWithEmailAndPassword(...) puis rattache automatiquement le joueur
             *    au compte existant si le mot de passe est correct
             *
             * Ce comportement respecte exactement la règle produit :
             * - nouveau mail => création normale
             * - mail existant + bon mot de passe => ouverture automatique du Panthéon existant
             * - mail existant + mauvais mot de passe => message clair
             */
            inscrireEnLocal(email, password)

        } else {
            connecterCompteFirebase(email, password)
        }
    }

    @Suppress("unused")
    private fun tenterConnexionOuInscription(email: String, password: String) {
        // Ancienne logique conservée uniquement pour historique architectural.
        // Elle n'est plus utilisée afin d'éviter le faux comportement
        // "email existe déjà / mot de passe incorrect" pendant la création de compte.
        Log.w("ReviZeusAuth", "Ancienne logique hybride appelée par erreur. Redirection vers inscription locale.")
        inscrireEnLocal(email, password)
    }

    /**
     * Vérifie d'abord si l'email existe déjà côté Firebase.
     *
     * Pourquoi ce détour :
     * - en mode inscription, un email déjà existant ne doit plus aller jusqu'à AvatarActivity
     *   pour échouer uniquement au moment du createAccount()
     * - si le compte existe déjà et que le mot de passe est bon, on l'ouvre immédiatement
     *   dans HeroSelectActivity pour permettre la création d'un nouveau héros sur ce mail
     * - si le compte n'existe pas, on conserve le flow local existant vers AuthActivity
     */
    private fun verifierSiCompteExistantOuDemarrerInscription(email: String, password: String) {
        verrouillerUiPendantRequete(true)
        afficherTexteRPG("Je consulte les archives divines pour voir si ce sceau email existe déjà...")

        FirebaseAuthManager.signIn(
            email = email,
            password = password,
            onSuccess = { user ->
                Log.d("ReviZeusAuth", "Compte existant détecté pendant l'inscription. uid=${user.uid}")
                runOnUiThread {
                    afficherTexteRPG("Cet email possède déjà un compte. Je t'ouvre directement le Panthéon de ses héros.")
                }
                finaliserConnexionFirebase(user, email)
            },
            onError = { message ->
                runOnUiThread {
                    val lower = message.lowercase()
                    when {
                        lower.contains("aucun compte trouvé") || lower.contains("no user") || lower.contains("user not found") -> {
                            verrouillerUiPendantRequete(false)
                            inscrireEnLocal(email, password)
                        }

                        lower.contains("mot de passe incorrect") || lower.contains("invalid login credentials") || lower.contains("password") -> {
                            verrouillerUiPendantRequete(false)
                            showError("Cet email existe déjà. Connecte-toi avec le bon mot de passe pour créer ou charger un héros sur ce compte.")
                            afficherTexteRPG("Ce sceau email existe déjà, mais le mot de passe invoqué n'est pas le bon.")
                        }

                        else -> {
                            verrouillerUiPendantRequete(false)
                            showError(message)
                            afficherTexteRPG("Les archives de l'Olympe sont troublées. Réessaie dans un instant.")
                        }
                    }
                }
            }
        )
    }

    private fun inscrireEnLocal(email: String, password: String) {
        OnboardingSession.store(email, password)
        afficherTexteRPG("Ton destin commence... Choisis ton identité, jeune héros !")

        getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString("PENDING_ACCOUNT_EMAIL", email)
            .putString(PREF_HERO_CREATION_MODE, HERO_CREATION_MODE_NEW_ACCOUNT)
            .apply()

        playSfx(R.raw.sfx_thunder_confirm)

        lifecycleScope.launch {
            delay(400L)
            naviguerVers(AuthActivity::class.java)
        }
    }

    private fun connecterCompteFirebase(email: String, password: String) {
        verrouillerUiPendantRequete(true)
        afficherTexteRPG("Approche du portail sacré... Je vérifie ton sceau divin.")

        Log.d("ReviZeusAuth", "Tentative connexion Firebase pour : $email")
        Log.d("ReviZeusAuth", "EMAIL=[$email]")
        Log.d("ReviZeusAuth", "PASSWORD_LENGTH=${password.length}")

        FirebaseAuthManager.signIn(
            email = email,
            password = password,
            onSuccess = { user ->
                Log.d("ReviZeusAuth", "Connexion Firebase réussie. uid=${user.uid}")
                finaliserConnexionFirebase(user, email)
            },
            onError = { message ->
                Log.e("ReviZeusAuth", "Erreur connexion : $message")
                runOnUiThread {
                    verrouillerUiPendantRequete(false)
                    showError(message)
                    afficherTexteRPG("L'Olympe refuse ton accès pour l'instant.")
                }
            }
        )
    }

    private fun finaliserConnexionFirebase(
        user: com.google.firebase.auth.FirebaseUser,
        email: String
    ) {
        try {
            val sharedPref = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
            val resolvedEmail = normalizeEmailForFirebase(user.email ?: email)

            // 🔥 CORRECTION MULTI-COMPTES — Détection nouveau compte Google
            val isNewAccount = !AccountRegistry.isRegistered(this, user.uid)

            sharedPref.edit()
                .putBoolean("HAS_ACCOUNT", true)
                .putString("ACCOUNT_EMAIL", resolvedEmail)
                .putString("RECOVERY_EMAIL", resolvedEmail)
                .putString("FIREBASE_UID", user.uid)
                .putBoolean("IS_EMAIL_VERIFIED", user.isEmailVerified)
                .putString(PREF_HERO_CREATION_MODE, HERO_CREATION_MODE_EXISTING_ACCOUNT)
                .remove("PENDING_ACCOUNT_EMAIL")
                .apply()

            AccountRegistry.registerUid(this, user.uid)
            AccountRegistry.setActiveUid(this, user.uid)
            AccountRegistry.rememberAccountEmail(this, user.uid, resolvedEmail)

            // Sécurité critique :
            // si l'on vient d'un compte existant, toute ancienne session d'onboarding
            // doit être effacée pour éviter qu'AvatarActivity tente un createAccount() fantôme.
            OnboardingSession.clear()

            runOnUiThread {
                if (isNewAccount) {
                    // 🆕 NOUVEAU COMPTE GOOGLE → Onboarding complet
                    // Stockage transitoire pour que AuthActivity accepte l'accès
                    OnboardingSession.store(resolvedEmail, "google_sso_placeholder")

                    afficherTexteRPG("Bienvenue dans l'Olympe, nouveau héros ! Dis-moi qui tu es...")
                    verrouillerUiPendantRequete(false)

                    lifecycleScope.launch {
                        delay(600L)
                        naviguerVers(AuthActivity::class.java)
                    }
                } else {
                    // ✅ COMPTE EXISTANT → Sélection héros
                    synchroniserCodeDeSecoursPuisOuvrirHeroSelect(
                        firebaseUid = user.uid,
                        accountEmail = resolvedEmail
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ReviZeusAuth", "Crash post-traitement connexion Google", e)
            runOnUiThread {
                verrouillerUiPendantRequete(false)
                showError("La connexion Google a réussi, mais l'Olympe a rencontré une erreur locale.")
            }
        }
    }

    private fun synchroniserCodeDeSecoursPuisOuvrirHeroSelect(
        firebaseUid: String,
        accountEmail: String
    ) {
        if (firebaseUid.isBlank()) {
            verrouillerUiPendantRequete(false)
            showError("UID Firebase introuvable après connexion.")
            return
        }

        val hasLocalCode = AccountRecoveryManager.hasLocalRecoveryCode(this, firebaseUid)
        val isAcknowledged = AccountRecoveryManager.isRecoveryCodeAcknowledged(this, firebaseUid)

        if (hasLocalCode && isAcknowledged) {
            naviguerVersHeroSelect(firebaseUid)
            return
        }

        if (hasLocalCode) {
            val rawCode = AccountRecoveryManager.getLocalRecoveryCode(this, firebaseUid)
            val hint = AccountRecoveryManager.getLocalRecoveryHint(this, firebaseUid)

            runOnUiThread {
                afficherDialogueCodeDeSecours(
                    rawCode = rawCode,
                    hint = hint,
                    onDismissValidated = {
                        AccountRecoveryManager.markRecoveryCodeAcknowledged(this, firebaseUid)
                        naviguerVersHeroSelect(firebaseUid)
                    }
                )
            }
            return
        }

        val rawCode = AccountRecoveryManager.generateRecoveryCode()

        AccountRecoveryManager.saveRecoveryCode(
            context = this,
            firebaseUid = firebaseUid,
            accountEmail = accountEmail,
            rawCode = rawCode,
            onSuccess = {
                runOnUiThread {
                    afficherDialogueCodeDeSecours(
                        rawCode = rawCode,
                        hint = AccountRecoveryManager.buildHint(rawCode),
                        onDismissValidated = {
                            AccountRecoveryManager.markRecoveryCodeAcknowledged(this, firebaseUid)
                            naviguerVersHeroSelect(firebaseUid)
                        }
                    )
                }
            },
            onError = { message ->
                runOnUiThread {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    afficherTexteRPG("La connexion est restaurée, mais le sceau de secours n'a pas encore été synchronisé.")
                    naviguerVersHeroSelect(firebaseUid)
                }
            }
        )
    }

    private fun afficherDialogueCodeDeSecours(
        rawCode: String,
        hint: String,
        onDismissValidated: () -> Unit
    ) {
        val message = if (rawCode.isNotBlank()) {
            """
        Recopie ce code et garde-le dans un endroit sûr.

        $rawCode

        Il protège ton compte si tu perds l'accès à ton email.
        """.trimIndent()
        } else {
            """
        Le code complet n'est pas disponible en clair sur cet appareil.

        Indice local : $hint

        Valide seulement si tu l'as déjà conservé ailleurs.
        """.trimIndent()
        }

        AlertDialog.Builder(this)
            .setTitle("Code de secours du compte")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("J'ai bien gardé ce code") { _, _ ->
                afficherTexteRPG("Ton sceau de secours est désormais validé. Tu pourras le revoir plus tard depuis l'onglet Compte.")
                onDismissValidated()
            }
            .show()
    }

    private fun naviguerVersHeroSelect(firebaseUid: String) {
        verrouillerUiPendantRequete(false)
        val intent = Intent(this, HeroSelectActivity::class.java).apply {
            putExtra(HeroSelectActivity.EXTRA_FIREBASE_UID, firebaseUid)
        }
        naviguerVersIntent(intent)
    }

    private fun afficherDialogueMaxComptes() {
        try {
            SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
        } catch (_: Exception) {
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(12))
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#1A0A00"))
            }
        }

        val ivZeus = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(72)).also {
                it.gravity = Gravity.CENTER_HORIZONTAL
                it.bottomMargin = dp(12)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            try {
                setImageResource(R.drawable.ic_zeus_chibi)
            } catch (_: Exception) {
                setImageResource(android.R.drawable.sym_def_app_icon)
            }
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
            text = "L'Olympe est complet, mortel ! Cinq héros occupent déjà les temples sacrés de cet appareil. Retire un héros existant depuis les Réglages pour accueillir une nouvelle âme."
        }

        val btnCompris = creerBouton("COMPRIS", "#FFD700")
        layout.addView(ivZeus)
        layout.addView(tvSpeech)
        layout.addView(btnCompris)

        val dialog = AlertDialog.Builder(this)
            .setView(layout)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        btnCompris.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun handleForgotPassword() {
        val email = normalizeEmailForFirebase(binding.etEmail.text.toString())
        if (email.isEmpty()) {
            showError("Indique d'abord ton email pour appeler les Oracles.")
            afficherTexteRPG("Indique ton email pour que les Oracles t'envoient un parchemin de récupération.")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Cet email n'est pas valide.")
            return
        }

        Log.d("ReviZeusAuth", "EMAIL=[$email]")
        Log.d("ReviZeusAuth", "PASSWORD_LENGTH=0")

        verrouillerUiPendantRequete(true)
        afficherTexteRPG("Traverse les enfers pour retrouver l'âme de ton héros.")
        FirebaseAuthManager.sendPasswordReset(
            email = email,
            onSuccess = {
                runOnUiThread {
                    verrouillerUiPendantRequete(false)
                    Toast.makeText(
                        this,
                        "Les Oracles ont envoyé un parchemin sacré sur ton email.",
                        Toast.LENGTH_LONG
                    ).show()
                    afficherTexteRPG("Les Oracles ont entendu ton appel. Consulte ton email, jeune héros.")
                }
            },
            onError = { message ->
                runOnUiThread {
                    verrouillerUiPendantRequete(false)
                    showError(message)
                    afficherTexteRPG("Les Oracles n'ont pas pu répondre pour l'instant.")
                }
            }
        )
    }

    private fun afficherTexteRPG(texteComplet: String) {
        typewriterJob?.cancel()
        typewriterJob = lifecycleScope.launch {
            binding.tvZeusSpeech.text = ""
            for (i in texteComplet.indices) {
                binding.tvZeusSpeech.text = texteComplet.substring(0, i + 1)
                try {
                    SoundManager.playSFXLow(this@LoginActivity, R.raw.sfx_dialogue_blip)
                } catch (_: Exception) {
                }
                delay(35)
            }
        }
    }

    private fun appliquerEtatVisibiliteMotDePasse() {
        if (isPasswordVisible) {
            binding.etPassword.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.ivTogglePassword.alpha = 1.0f
        } else {
            binding.etPassword.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.ivTogglePassword.alpha = 0.65f
        }
        binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
    }

    private fun setupGoogleSignIn() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, options)
    }

    private fun setupGoogleInlineUi() {
        try {
            binding.btnGoogleInline.setImageResource(R.drawable.ic_google_olympus)
        } catch (_: Exception) {
            binding.btnGoogleInline.setImageResource(android.R.drawable.ic_menu_search)
        }
    }

    private fun handleGoogleInlineClick() {
        verrouillerUiPendantRequete(true)
        afficherTexteRPG("J'ouvre le sceau olympien de Google. Choisis ton compte, mortel.")
        try {
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        } catch (_: Exception) {
            verrouillerUiPendantRequete(false)
            showError("Google Sign-In n'est pas prêt sur cet appareil.")
        }
    }

    private fun updateUI() {
        if (isRegisterMode) {
            binding.tvTitle.text = "REJOINS L'OLYMPE"
            binding.btnSubmit.text = "FORGER MON COMPTE"
            binding.tvSwitchMode.text = "Déjà initié ? → Se connecter"
            binding.tvForgotPassword.alpha = 0.75f
        } else {
            binding.tvTitle.text = "RETOUR À L'OLYMPE"
            binding.btnSubmit.text = "ENTRER DANS LE PANTHÉON"
            binding.tvSwitchMode.text = "Pas encore de compte ? → S'inscrire"
            binding.tvForgotPassword.alpha = 1.0f
        }
    }

    private fun naviguerVers(destination: Class<*>) {
        if (hasNavigated || isFinishing || isDestroyed) return
        hasNavigated = true
        startActivity(Intent(this, destination))
        finish()
    }

    private fun naviguerVersIntent(intent: Intent) {
        if (hasNavigated || isFinishing || isDestroyed) return
        hasNavigated = true
        startActivity(intent)
        finish()
    }

    private fun verrouillerUiPendantRequete(verrouiller: Boolean) {
        binding.btnSubmit.isEnabled = !verrouiller
        binding.tvSwitchMode.isEnabled = !verrouiller
        binding.tvForgotPassword.isEnabled = !verrouiller
        binding.ivTogglePassword.isEnabled = !verrouiller
        binding.etEmail.isEnabled = !verrouiller
        binding.etPassword.isEnabled = !verrouiller
        binding.btnSubmit.alpha = if (verrouiller) 0.7f else 1f
        binding.btnGoogleInline.isEnabled = !verrouiller
        binding.btnGoogleInline.alpha = if (verrouiller) 0.6f else 1f
    }

    private fun normalizeEmailForFirebase(rawEmail: String): String {
        return rawEmail
            .replace("\\s".toRegex(), "")
            .trim()
    }

    private fun ajouterBoutonGestionCompte() {
        val root = binding.root as? ConstraintLayout ?: return
        val frame = FrameLayout(this).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(dp(52), dp(52)).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = dp(10)
                marginStart = dp(4)
            }
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
            alpha = 0.35f
            scaleType = ImageView.ScaleType.FIT_XY
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
            text = "✕"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FFD7D7"))
        })

        frame.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            ouvrirCentreGestionCompte()
        }

        root.addView(frame)
    }

    private fun ouvrirCentreGestionCompte() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(10))
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
            }
        }

        val scroll = ScrollView(this).apply {
            addView(container)
        }

        val etEmail = EditText(this).apply {
            hint = "Email du compte"
            setText(binding.etEmail.text?.toString().orEmpty())
            setSingleLine()
            setPadding(dp(14), dp(12), dp(14), dp(12))
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
            }
        }

        val etPassword = EditText(this).apply {
            hint = "Mot de passe pour suppression définitive"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine()
            setPadding(dp(14), dp(12), dp(14), dp(12))
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
            }
        }

        val header = TextView(this).apply {
            text = "Récupérer ou effacer"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFD700"))
            typeface = Typeface.DEFAULT_BOLD
        }

        val infos = TextView(this).apply {
            text = """
                • Envoyer un vrai email de récupération
                • Supprimer définitivement le compte Firebase avec email + mot de passe
                • Accéder au flux sécurisé sans casser la compilation du projet
            """.trimIndent()
            textSize = 13f
            setTextColor(Color.parseColor("#FFF2CC"))
        }

        val btnReset = creerBouton("ENVOYER EMAIL DE RÉCUPÉRATION", "#FFD700")
        val btnDeleteRemote = creerBouton("SUPPRIMER DÉFINITIVEMENT LE COMPTE", "#FFD7D7")

        container.addView(header)
        container.addView(espace(10))
        container.addView(infos)
        container.addView(espace(12))
        container.addView(etEmail)
        container.addView(espace(10))
        container.addView(etPassword)
        container.addView(espace(12))
        container.addView(btnReset)
        container.addView(espace(8))
        container.addView(btnDeleteRemote)

        val dialog = AlertDialog.Builder(this)
            .setView(scroll)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnReset.setOnClickListener {
            val email = normalizeEmailForFirebase(etEmail.text.toString())
            if (email.isBlank()) {
                showError("Indique d'abord l'email du compte.")
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError("Cet email n'est pas valide.")
                return@setOnClickListener
            }

            verrouillerUiPendantRequete(true)
            FirebaseAuthManager.sendPasswordReset(
                email = email,
                onSuccess = {
                    runOnUiThread {
                        verrouillerUiPendantRequete(false)
                        Toast.makeText(
                            this,
                            "Email de récupération envoyé.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onError = { message ->
                    runOnUiThread {
                        verrouillerUiPendantRequete(false)
                        showError(message)
                    }
                }
            )
        }

        btnDeleteRemote.setOnClickListener {
            val email = normalizeEmailForFirebase(etEmail.text.toString())
            val password = etPassword.text.toString()
            if (email.isBlank() || password.isBlank()) {
                showError("Email et mot de passe sont requis pour une suppression définitive.")
                return@setOnClickListener
            }
            confirmerSuppressionFirebase(email, password, dialog)
        }

        dialog.show()
    }

    private fun confirmerSuppressionFirebase(
        email: String,
        password: String,
        parentDialog: AlertDialog
    ) {
        AlertDialog.Builder(this)
            .setTitle("Suppression définitive")
            .setMessage("Cette action supprimera le compte Firebase réel puis effacera ses données locales sur ce téléphone.")
            .setPositiveButton("Supprimer") { _, _ ->
                verrouillerUiPendantRequete(true)
                FirebaseAuthManager.signIn(
                    email = email,
                    password = password,
                    onSuccess = { user ->
                        FirebaseAuthManager.reauthenticateAndDeleteAccount(
                            email = email,
                            password = password,
                            onSuccess = {
                                runOnUiThread {
                                    AccountRegistry.deleteAccountLocal(this, user.uid)
                                    if ((getSharedPreferences(
                                            "ReviZeusPrefs",
                                            Context.MODE_PRIVATE
                                        ).getString("FIREBASE_UID", "") ?: "") == user.uid
                                    ) {
                                        getSharedPreferences(
                                            "ReviZeusPrefs",
                                            Context.MODE_PRIVATE
                                        ).edit()
                                            .remove("ACCOUNT_EMAIL")
                                            .remove("RECOVERY_EMAIL")
                                            .remove("FIREBASE_UID")
                                            .remove("IS_REGISTERED")
                                            .remove("IS_EMAIL_VERIFIED")
                                            .apply()
                                    }
                                    verrouillerUiPendantRequete(false)
                                    parentDialog.dismiss()
                                    Toast.makeText(this, "Compte Firebase supprimé.", Toast.LENGTH_LONG).show()
                                }
                            },
                            onError = { message ->
                                runOnUiThread {
                                    verrouillerUiPendantRequete(false)
                                    showError(message)
                                }
                            }
                        )
                    },
                    onError = { message ->
                        runOnUiThread {
                            verrouillerUiPendantRequete(false)
                            showError(message)
                        }
                    }
                )
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun espace(value: Int, horizontal: Boolean = false): View {
        return View(this).apply {
            layoutParams = if (horizontal) {
                LinearLayout.LayoutParams(dp(value), 1)
            } else {
                LinearLayout.LayoutParams(1, dp(value))
            }
        }
    }

    private fun showError(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun playSfx(resId: Int) {
        try {
            SoundManager.playSFX(this, resId)
        } catch (_: Exception) {
        }
    }

    private fun creerBouton(texte: String, couleurTexte: String): FrameLayout {
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onPause() {
        super.onPause()
        SoundManager.pauseMusic()
    }

    override fun onResume() {
        super.onResume()
        try {
            SoundManager.resumeMusic()
        } catch (_: Exception) {
            try {
                SoundManager.playMusic(this, R.raw.music_gender_selection)
            } catch (_: Exception) {
            }
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

    override fun handleBackPressed() {
        try {
            SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
        } catch (_: Exception) {
        }
        startActivity(Intent(this, TitleScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }
}

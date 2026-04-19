package com.revizeus.app

import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.util.Patterns
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.databinding.ActivitySettingsBinding
import com.revizeus.app.models.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ============================================================
 * SettingsActivity.kt — RéviZeus v9
 * Écran des Réglages — "Le Conseil de l'Olympe"
 *
 * Onglets :
 *   0 = Audio
 *   1 = Affichage
 *   2 = Accessibilité  ★ NOUVEAU v9
 *   3 = Notifications
 *   4 = Compte
 *   5 = Gameplay       (enrichi v9)
 *   6 = Avancé         ★ NOUVEAU v9 (certaines options 🔒)
 *   7 = JukeBox
 *
 * Sons : sfx_settings_tab | sfx_settings_save | sfx_thunder_confirm
 * Orientation : PORTRAIT forcé
 *
 * MULTI-COMPTES v7 :
 * ─────────────────────────────────────────────────────────
 * Ajout du bouton "SE DÉCONNECTER" dans l'onglet Compte.
 * Ce bouton est créé programmatiquement dans initialiserCompte()
 * pour ne pas toucher au XML activity_settings.xml existant.
 * Il est inséré après le bouton btnDeleteAccount dans le panelCompte.
 *
 * Flux déconnexion :
 *   1. endSessionAndSaveTime() → cumule le temps de session dans le cache
 *   2. addPlayTimeSeconds()    → persiste le temps en DB Room
 *   3. AppDatabase.resetInstance() → libère la connexion DB active
 *   4. FirebaseAuthManager.signOut() → déconnexion Firebase
 *   5. ReviZeusPrefs nettoyées (sauf UIDs enregistrés dans ReviZeusAccounts)
 *   6. SoundManager.stopMusic() + navigation vers TitleScreenActivity
 * ============================================================
 */
class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: SettingsManager

    /**
     * Jukebox — piste actuellement jouée depuis les réglages.
     */
    private var currentJukeboxTrackResId: Int = -1

    /**
     * BGM du jeu avant l'entrée dans le jukebox.
     * Permet de restaurer l'ambiance actuelle.
     */
    private var previousGameMusicResId: Int = -1

    /**
     * Adaptateur du Temple des Mélodies.
     */
    private var jukeboxAdapter: JukeboxAdapter? = null

    // Onglet actif (0..7)
    private var ongletActif = 0

    /**
     * Patch Bloc A :
     * On passe sur BaseActivity pour bénéficier du socle transverse,
     * mais on conserve ici le comportement audio historique de Settings.
     */
    override fun shouldBaseActivityPauseMusicOnPause(): Boolean = false

    override fun shouldBaseActivityAttemptMusicRecoveryOnResume(): Boolean = false

    // Références aux LinearLayouts d'onglets (pour la boucle de reset)
    private val tabLayouts: List<LinearLayout> by lazy {
        listOf(
            binding.tabAudio,
            binding.tabAffichage,
            binding.tabAccessibilite,
            binding.tabNotifications,
            binding.tabCompte,
            binding.tabGameplay,
            binding.tabAvance,
            binding.tabJukebox
        )
    }

    // Références aux panneaux de contenu (pour la boucle de reset)
    private val panels: List<View> by lazy {
        listOf(
            binding.panelAudio,
            binding.panelAffichage,
            binding.panelAccessibilite,
            binding.panelNotifications,
            binding.panelCompte,
            binding.panelGameplay,
            binding.panelAvance,
            binding.panelJukebox
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // IMPORTANT : SettingsManager doit être instancié AVANT toute lecture des préférences.
        settings = SettingsManager(this)

        appliquerTailleTexteGlobale()

        // Musique : on garde celle du Dashboard sans interruption
        previousGameMusicResId = SoundManager.getCurrentMusicResId()
        SoundManager.setVolume(settings.getVolumeMusiquef())

        binding.btnInfoSettings.setOnClickListener {
            jouerSfxTab()
            startActivity(Intent(this, RevizeusInfoActivity::class.java))
        }

        initialiserAudio()
        initialiserAffichage()
        initialiserAccessibilite()
        initialiserNotifications()
        initialiserCompte()
        initialiserGameplay()
        initialiserAvance()
        initialiserJukeboxPanel()

        configurerOnglets()
        configurerBoutonsBas()

        afficherOnglet(0)
        binding.root.post { montrerTutorielGeneralReglagesSiNecessaire() }
    }

    // ══════════════════════════════════════════════════════════
    // 0 — AUDIO
    // ══════════════════════════════════════════════════════════

    private fun initialiserAudio() {
        binding.seekMusiqueVolume.progress = settings.volumeMusique
        binding.tvMusiqueVolume.text       = settings.volumeMusique.toString()
        binding.seekSfxVolume.progress     = settings.volumeSfx
        binding.tvSfxVolume.text           = settings.volumeSfx.toString()
        binding.seekDialogueVolume.progress = settings.volumeDialogue
        binding.tvDialogueVolume.text      = settings.volumeDialogue.toString()
        binding.switchMuet.isChecked       = settings.muetGeneral

        val adapterQ = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            SettingsManager.QUALITE_AUDIO_OPTIONS)
        adapterQ.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerQualiteAudio.adapter = adapterQ
        binding.spinnerQualiteAudio.setSelection(
            SettingsManager.QUALITE_AUDIO_OPTIONS.indexOf(settings.qualiteAudio).coerceAtLeast(0))

        binding.seekMusiqueVolume.setOnSeekBarChangeListener(seekListener { p ->
            binding.tvMusiqueVolume.text = p.toString()
            SoundManager.setVolume(p / 100f)
            settings.volumeMusique = p
        })

        binding.seekSfxVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, user: Boolean) {
                binding.tvSfxVolume.text = p.toString()
                settings.volumeSfx = p
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                try { SoundManager.playSFX(this@SettingsActivity, R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            }
        })

        binding.seekDialogueVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, user: Boolean) {
                binding.tvDialogueVolume.text = p.toString()
                settings.volumeDialogue = p
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                try { SoundManager.playSFXLow(this@SettingsActivity, R.raw.sfx_dialogue_blip) } catch (_: Exception) {}
            }
        })

        binding.switchMuet.setOnCheckedChangeListener { _, checked ->
            settings.muetGeneral = checked
            if (checked) SoundManager.pauseMusic()
            else try { SoundManager.resumeMusic() } catch (_: Exception) {}
        }

        binding.spinnerQualiteAudio.onItemSelectedListener = spinnerListener { position ->
            settings.qualiteAudio = SettingsManager.QUALITE_AUDIO_OPTIONS
                .getOrNull(position) ?: SettingsManager.DEFAULT_QUALITE_AUDIO
        }

        binding.btnResetAudio.setOnClickListener {
            settings.resetAudio()
            initialiserAudio()
            SoundManager.setVolume(settings.getVolumeMusiquef())
            jouerSfxSave()
        }
    }

    // ══════════════════════════════════════════════════════════
    // 1 — AFFICHAGE
    // ══════════════════════════════════════════════════════════

    private fun initialiserAffichage() {
        binding.seekTailleTexte.progress    = settings.tailleTexte
        binding.seekVitesseDialogue.progress = settings.vitesseDialogue

        binding.seekTailleTexte.setOnSeekBarChangeListener(seekListener { p ->
            settings.tailleTexte = p
        })

        binding.seekVitesseDialogue.setOnSeekBarChangeListener(seekListener { p ->
            settings.vitesseDialogue = p
        })

        binding.btnResetAffichage.setOnClickListener {
            settings.resetAffichage()
            initialiserAffichage()
            jouerSfxSave()
        }
    }

    // ══════════════════════════════════════════════════════════
    // 2 — ACCESSIBILITÉ
    // ══════════════════════════════════════════════════════════

    private fun initialiserAccessibilite() {
        binding.switchDaltonien.isChecked       = settings.modeDaltonien
        binding.switchContrasteEleve.isChecked  = settings.contrasteEleve
        binding.switchReduireFlashs.isChecked   = settings.reduireFlashs
        binding.switchAnimationsOff.isChecked   = settings.animationsDesactivees

        val adapterD = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            SettingsManager.DALTONISME_OPTIONS)
        adapterD.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDaltonisme.adapter = adapterD
        binding.spinnerDaltonisme.setSelection(
            SettingsManager.DALTONISME_OPTIONS.indexOf(settings.typeDaltonisme).coerceAtLeast(0))

        binding.layoutDaltonismeType.visibility =
            if (settings.modeDaltonien) View.VISIBLE else View.GONE

        binding.switchDaltonien.setOnCheckedChangeListener { _, checked ->
            binding.layoutDaltonismeType.visibility = if (checked) View.VISIBLE else View.GONE
            settings.modeDaltonien = checked
        }

        binding.spinnerDaltonisme.onItemSelectedListener = spinnerListener { position ->
            settings.typeDaltonisme = SettingsManager.DALTONISME_OPTIONS
                .getOrNull(position) ?: SettingsManager.DEFAULT_TYPE_DALTONISME
        }

        binding.switchContrasteEleve.setOnCheckedChangeListener { _, checked ->
            settings.contrasteEleve = checked
        }

        binding.switchReduireFlashs.setOnCheckedChangeListener { _, checked ->
            settings.reduireFlashs = checked
        }

        binding.switchAnimationsOff.setOnCheckedChangeListener { _, checked ->
            settings.animationsDesactivees = checked
        }

        binding.btnResetAccessibilite.setOnClickListener {
            settings.resetAccessibilite()
            initialiserAccessibilite()
            jouerSfxSave()
        }
    }

    // ══════════════════════════════════════════════════════════
    // 3 — NOTIFICATIONS
    // ══════════════════════════════════════════════════════════

    private fun initialiserNotifications() {
        binding.btnHeureRappel.text             = settings.heureRappel
        binding.switchNotifQuotidien.isChecked  = settings.notifQuotidien
        binding.switchNotifQcm.isChecked        = settings.notifQcm
        binding.switchNotifRecompenses.isChecked = settings.notifRecompenses
        binding.switchNotifStreak.isChecked     = settings.notifStreak

        binding.btnHeureRappel.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                val label = "${h}h${m.toString().padStart(2, '0')}"
                binding.btnHeureRappel.text = label
                settings.heureRappel        = label
                settings.heureRappelHeure   = h
                settings.heureRappelMinute  = m
                jouerSfxTab()
            }, settings.heureRappelHeure, settings.heureRappelMinute, true).show()
        }

        binding.switchNotifQuotidien.setOnCheckedChangeListener { _, checked ->
            settings.notifQuotidien = checked
        }

        binding.switchNotifQcm.setOnCheckedChangeListener { _, checked ->
            settings.notifQcm = checked
        }

        binding.switchNotifRecompenses.setOnCheckedChangeListener { _, checked ->
            settings.notifRecompenses = checked
        }

        binding.switchNotifStreak.setOnCheckedChangeListener { _, checked ->
            settings.notifStreak = checked
        }

        binding.btnResetNotifs.setOnClickListener {
            settings.resetNotifications()
            initialiserNotifications()
            jouerSfxSave()
        }
    }

    // ══════════════════════════════════════════════════════════
    // 4 — COMPTE
    // ══════════════════════════════════════════════════════════

    private fun initialiserCompte() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@SettingsActivity)
                val profil = withContext(Dispatchers.IO) { db.iAristoteDao().getUserProfile() }
                withContext(Dispatchers.Main) {
                    if (profil != null) {
                        binding.tvNomHeros.text = profil.pseudo.ifBlank { "—" }
                        val level = com.revizeus.app.core.XpCalculator.calculateLevel(profil.xp)
                        binding.tvNiveauCompte.text = "LVL $level"
                        binding.tvXpCompte.text = "${profil.xp} XP"
                        binding.tvStreakCompte.text = "${profil.streak} 🔥"
                        binding.tvCoursCompte.text = "— cours"
                        binding.etParentEmailSettings.setText(profil.parentEmail)
                        binding.switchNotifParents.isChecked = profil.isParentSummaryEnabled
                    } else {
                        binding.tvNomHeros.text = "—"
                        binding.tvNiveauCompte.text = "LVL 1"
                        binding.tvXpCompte.text = "0 XP"
                        binding.tvStreakCompte.text = "0 🔥"
                        binding.tvCoursCompte.text = "0 cours"
                        binding.etParentEmailSettings.setText(ParentSummaryManager.getParentEmail(this@SettingsActivity))
                        binding.switchNotifParents.isChecked = ParentSummaryManager.isParentSummaryEnabled(this@SettingsActivity)
                    }
                }
                val nbCours = withContext(Dispatchers.IO) { db.iAristoteDao().countCourses() }
                withContext(Dispatchers.Main) {
                    binding.tvCoursCompte.text = "$nbCours cours scannés"
                }
            } catch (_: Exception) {
                binding.tvNomHeros.text = "—"
            }
        }

        // Bouton changer d'avatar → 🔒 désactivé
        binding.btnChangerAvatar.isEnabled = false
        binding.btnChangerAvatar.alpha     = 0.4f

        // Bouton modifier pseudo → 🔒 désactivé
        binding.btnModifierPseudo.isEnabled = false
        binding.btnModifierPseudo.alpha     = 0.4f

        binding.switchNotifParents.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                Toast.makeText(
                    this,
                    "Les dieux enverront un résumé hebdomadaire au parent si l'email est valide.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.btnSaveParentSettings.setOnClickListener {
            sauvegarderParentsLocalement(fermerApres = false)
        }

        binding.btnReinitialiser.setOnClickListener { confirmerReinitialisation() }
        binding.btnDeleteAccount.setOnClickListener { confirmerSuppressionCompte() }
        binding.btnRestartAdventure.setOnClickListener { confirmerRecommencerAventure() }

        // ──────────────────────────────────────────────────────────
        // MULTI-COMPTES v7 — BOUTON "SE DÉCONNECTER"
        // Créé programmatiquement pour ne pas altérer le XML existant.
        // Injecté dynamiquement après le bouton btnDeleteAccount via
        // la méthode findViewById sur le LinearLayout parent du panelCompte.
        // ──────────────────────────────────────────────────────────
        ajouterBoutonDeconnexion()

        // Ajout premium : permet de réafficher le code de secours du compte
        // depuis l'onglet Compte après validation initiale.
        ajouterBoutonCodeSecoursCompte()
    }

    /**
     * MULTI-COMPTES v7 — Injection du bouton SE DÉCONNECTER
     *
     * Le bouton est construit comme un FrameLayout 3 couches
     * (Règle design #10 du système) et injecté dans le LinearLayout
     * fils du ScrollView panelCompte, avant la section "Parents".
     *
     * Il est inséré APRÈS btnDeleteAccount dans la hiérarchie de vues
     * pour respecter la logique "actions destructives en bas".
     */
    private fun ajouterBoutonDeconnexion() {
        try {
            // Le ScrollView panelCompte contient un LinearLayout (son unique enfant)
            val scrollViewCompte = binding.panelCompte
            val llCompte = scrollViewCompte.getChildAt(0) as? android.widget.LinearLayout
                ?: return

            // Éviter les doublons si initialiserCompte() est rappelé
            if (llCompte.findViewWithTag<View>("btn_deconnexion_tag") != null) return

            // ── Séparateur visuel avant le bouton ─────────────────
            val separator = View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).also { p ->
                    p.topMargin    = dpToPx(12)
                    p.bottomMargin = dpToPx(8)
                }
                setBackgroundColor(android.graphics.Color.parseColor("#33FFD700"))
            }

            // ── FrameLayout 3 couches — Règle design #10 ──────────
            val frame = android.widget.FrameLayout(this).apply {
                tag = "btn_deconnexion_tag"
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(44)
                ).also { p ->
                    p.bottomMargin = dpToPx(14)
                }
                isClickable = true
                isFocusable = true
                // Ripple
                val attrs = intArrayOf(android.R.attr.selectableItemBackground)
                val ta = obtainStyledAttributes(attrs)
                foreground = ta.getDrawable(0)
                ta.recycle()
            }

            // Couche 1 : bg_temple_button
            val bg1 = android.widget.ImageView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = android.widget.ImageView.ScaleType.FIT_XY
                try { setImageResource(R.drawable.bg_temple_button) } catch (_: Exception) {}
            }

            // Couche 2 : bg_textelayout alpha 35% (bouton RETOUR/DÉCONNEXION = teinte bleue/rouge)
            val bg2 = android.widget.ImageView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = android.widget.ImageView.ScaleType.FIT_XY
                alpha = 0.35f
                try { setImageResource(R.drawable.bg_textelayout) } catch (_: Exception) {}
            }

            // Couche 3 : Label Cinzel or
            val label = android.widget.TextView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                gravity = android.view.Gravity.CENTER
                text = "🚪  SE DÉCONNECTER"
                setTextColor(android.graphics.Color.parseColor("#FFD700"))
                textSize = 13f
                try { typeface = android.graphics.Typeface.create(
                    resources.getFont(R.font.cinzel_bold), android.graphics.Typeface.BOLD
                )} catch (_: Exception) {}
                letterSpacing = 0.06f
            }

            frame.addView(bg1)
            frame.addView(bg2)
            frame.addView(label)

            frame.setOnClickListener {
                confirmerDeconnexion()
            }

            llCompte.addView(separator)
            llCompte.addView(frame)

        } catch (e: Exception) {
            // Injection silencieuse — ne jamais crasher pour un bouton optionnel
            android.util.Log.e("SettingsActivity", "ajouterBoutonDeconnexion : ${e.message}")
        }
    }

    /**
     * Ajoute un bouton dédié pour revoir le code de secours du compte actif.
     *
     * RÈGLES :
     * - ne casse pas le XML existant
     * - s'insère dans l'onglet Compte
     * - permet de revoir le vrai code si le brut local existe encore
     * - sinon affiche honnêtement l'indice seulement
     */
    private fun ajouterBoutonCodeSecoursCompte() {
        try {
            val scrollViewCompte = binding.panelCompte
            val llCompte = scrollViewCompte.getChildAt(0) as? android.widget.LinearLayout ?: return

            if (llCompte.findViewWithTag<View>("btn_code_secours_tag") != null) return

            val frame = android.widget.FrameLayout(this).apply {
                tag = "btn_code_secours_tag"
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(40)
                ).also { p ->
                    p.topMargin = dpToPx(10)
                    p.bottomMargin = dpToPx(10)
                }
                isClickable = true
                isFocusable = true
                val attrs = intArrayOf(android.R.attr.selectableItemBackground)
                val ta = obtainStyledAttributes(attrs)
                foreground = ta.getDrawable(0)
                ta.recycle()
            }

            val bg1 = android.widget.ImageView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = android.widget.ImageView.ScaleType.FIT_XY
                try { setImageResource(R.drawable.bg_temple_button_inset) } catch (_: Exception) {}
            }

            val label = android.widget.TextView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                gravity = android.view.Gravity.CENTER
                text = "🛡  REVOIR LE CODE DE SECOURS"
                setTextColor(android.graphics.Color.parseColor("#FFD700"))
                textSize = 12f
                try {
                    typeface = android.graphics.Typeface.create(
                        resources.getFont(R.font.cinzel_bold),
                        android.graphics.Typeface.BOLD
                    )
                } catch (_: Exception) {}
                letterSpacing = 0.04f
            }

            frame.addView(bg1)
            frame.addView(label)

            frame.setOnClickListener {
                afficherCodeDeSecoursDepuisCompte()
            }

            val indexDelete = try { llCompte.indexOfChild(binding.btnDeleteAccount) } catch (_: Exception) { -1 }
            if (indexDelete >= 0) {
                llCompte.addView(frame, indexDelete + 1)
            } else {
                llCompte.addView(frame)
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "ajouterBoutonCodeSecoursCompte : ${e.message}")
        }
    }


/**
 * Affiche le vrai code de secours si cet appareil le possède encore.
 * Sinon, on affiche l'indice disponible et un message honnête.
 */
    /**
     * Affiche le vrai code de secours si cet appareil le possède encore.
     * Sinon, on affiche l'indice disponible et un message honnête.
     */
    private fun afficherCodeDeSecoursDepuisCompte() {
        try { SoundManager.playSFX(this, R.raw.sfx_settings_tab) } catch (_: Exception) {}

        val uid = AccountRegistry.getActiveUid(this)
        if (uid.isBlank()) {
            DialogRPGManager.showInfo(
                activity = this,
                godId = "prometheus",
                message = "Aucun compte actif n'est détecté sur cet appareil pour le moment.",
                title = "Compte introuvable"
            )
            return
        }

        val rawCode = try { AccountRecoveryManager.getLocalRecoveryCode(this, uid) } catch (_: Exception) { "" }
        val hint = try { AccountRecoveryManager.getLocalRecoveryHint(this, uid) } catch (_: Exception) { "" }

        if (rawCode.isNotBlank()) {
            AlertDialog.Builder(this)
                .setTitle("Code de secours du compte")
                .setMessage(
                    """
                Voici le sceau de secours actuellement conservé sur cet appareil.

                $rawCode

                Garde-le hors du téléphone, dans un endroit sûr.
                """.trimIndent()
                )
                .setPositiveButton("Compris", null)
                .setCancelable(true)
                .show()
            return
        }

        val message = if (hint.isNotBlank()) {
            """
        Le code complet n'est plus disponible en clair sur cet appareil, mais voici son indice local :

        $hint

        Si tu veux un nouveau code complet, il faudra prévoir plus tard une rotation de code.
        """.trimIndent()
        } else {
            "Aucun code de secours complet n'est disponible sur cet appareil pour le compte actif."
        }

        DialogRPGManager.showInfo(
            activity = this,
            godId = "prometheus",
            title = "Code de secours",
            message = message
        )
    }

// ══════════════════════════════════════════════════════════
// DÉCONNEXION — MULTI-COMPTES v7
    // ══════════════════════════════════════════════════════════

    /**
     * Dialogue de confirmation avant déconnexion.
     */
    private fun confirmerDeconnexion() {
        try { SoundManager.playSFX(this, R.raw.sfx_settings_tab) } catch (_: Exception) {}

        AlertDialog.Builder(this)
            .setTitle("🚪 Se déconnecter")
            .setMessage(
                "Tu vas quitter ce profil.\n\n" +
                "Tes données sont sauvegardées. Tu pourras retrouver " +
                "ton héros sur l'écran de sélection de compte."
            )
            .setPositiveButton("Se déconnecter") { _, _ ->
                deconnecterCompte()
            }
            .setNegativeButton("Rester") { d, _ -> d.dismiss() }
            .setCancelable(true)
            .show()
    }

    /**
     * Flux de déconnexion MULTI-COMPTES v7 :
     *
     * 1. endSessionAndSaveTime() → calcule et cache le temps de session
     * 2. addPlayTimeSeconds()    → persiste en DB Room
     * 3. updateCacheFromProfile() → met à jour toutes les stats du cache
     * 4. AppDatabase.resetInstance() → libère la connexion DB active
     * 5. FirebaseAuthManager.signOut() → déconnexion Firebase
     * 6. ReviZeusPrefs partiel nettoyé (FIREBASE_UID, IS_REGISTERED, etc.)
     *    NB : HAS_ACCOUNT est conservé pour que LoginActivity ouvre
     *    d'emblée en mode "connexion" (et non "inscription").
     * 7. SoundManager.stopMusic() → silence avant TitleScreenActivity
     * 8. Navigation vers TitleScreenActivity (CLEAR_TASK)
     */
    private fun deconnecterCompte() {
        val uid = AccountRegistry.getActiveUid(this)

        lifecycleScope.launch {
            try {
                // ── Étape 1+2 : Temps de jeu ─────────────────────────
                val elapsed = AccountRegistry.endSessionAndSaveTime(this@SettingsActivity, uid)
                if (elapsed > 0L && uid.isNotBlank()) {
                    try {
                        withContext(Dispatchers.IO) {
                            AppDatabase.getDatabase(this@SettingsActivity)
                                .iAristoteDao()
                                .addPlayTimeSeconds(elapsed)
                        }
                    } catch (_: Exception) {}
                }

                // ── Étape 3 : Mise à jour cache complet ──────────────
                if (uid.isNotBlank()) {
                    try {
                        val db = withContext(Dispatchers.IO) {
                            AppDatabase.getDatabase(this@SettingsActivity)
                        }
                        val profil = withContext(Dispatchers.IO) {
                            db.iAristoteDao().getUserProfile()
                        }
                        val nbCours = withContext(Dispatchers.IO) {
                            db.iAristoteDao().countCourses()
                        }
                        if (profil != null) {
                            AccountRegistry.updateCacheFromProfile(
                                context           = this@SettingsActivity,
                                uid               = uid,
                                profile           = profil,
                                totalCoursScanned = nbCours
                            )
                        }
                    } catch (_: Exception) {}
                }

                // ── Étape 4 : Libérer la DB ───────────────────────────
                withContext(Dispatchers.IO) {
                    AppDatabase.resetInstance()
                }

                // ── Étape 5 : Déconnexion Firebase ────────────────────
                try { FirebaseAuthManager.signOut() } catch (e: Exception) {
                    Log.w("REVIZEUS_SETTINGS", "Déconnexion Firebase impossible pendant deconnecterCompte()", e)
                }

                // ── Étape 6 : Nettoyage partiel des prefs ─────────────
                // On conserve HAS_ACCOUNT=true pour que LoginActivity
                // s'ouvre en mode connexion.
                // On conserve aussi les UID enregistrés dans ReviZeusAccounts.
                val prefs = getSharedPreferences("ReviZeusPrefs", MODE_PRIVATE)
                val hasAccount = prefs.getBoolean("HAS_ACCOUNT", false)
                prefs.edit()
                    .clear()
                    .putBoolean("HAS_ACCOUNT", hasAccount)
                    .apply()

                withContext(Dispatchers.Main) {
                    // ── Étape 7 : Silence ─────────────────────────────
                    SoundManager.stopMusic()
                    try { SoundManager.playSFX(this@SettingsActivity, R.raw.sfx_thunder_confirm) } catch (_: Exception) {}
                    declencherVibration(150L)

                    // ── Étape 8 : Navigation vers TitleScreenActivity ──
                    val intent = Intent(this@SettingsActivity, TitleScreenActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finishAffinity()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toast("Erreur lors de la déconnexion : ${e.message}")
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // 5 — GAMEPLAY
    // ══════════════════════════════════════════════════════════

    private fun initialiserGameplay() {
        val adapterDiff = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            SettingsManager.DIFFICULTE_OPTIONS)
        adapterDiff.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDifficulte.adapter = adapterDiff
        binding.spinnerDifficulte.setSelection(
            SettingsManager.DIFFICULTE_OPTIONS.indexOf(settings.difficulteQuiz).coerceAtLeast(0))

        val adapterNb = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            SettingsManager.NB_QUESTIONS_OPTIONS)
        adapterNb.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerNbQuestions.adapter = adapterNb
        val idxNb = SettingsManager.NB_QUESTIONS_OPTIONS.indexOf(settings.nbQuestionsQuiz.toString())
        binding.spinnerNbQuestions.setSelection(idxNb.coerceAtLeast(0))

        val adapterTemps = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            SettingsManager.TEMPS_LIMITE_OPTIONS)
        adapterTemps.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTempsLimite.adapter = adapterTemps
        val idxTemps = SettingsManager.TEMPS_LIMITE_VALEURS.indexOf(settings.tempsLimiteQuestion)
        binding.spinnerTempsLimite.setSelection(idxTemps.coerceAtLeast(0))

        binding.switchCorrections.isChecked      = settings.afficherCorrections
        binding.switchRevisionEspacee.isChecked  = settings.revisionEspaceeActive
        binding.switchHaptique.isChecked         = settings.haptiqueActif

        binding.spinnerDifficulte.onItemSelectedListener = spinnerListener { position ->
            settings.difficulteQuiz = SettingsManager.DIFFICULTE_OPTIONS
                .getOrNull(position) ?: SettingsManager.DEFAULT_DIFFICULTE
        }

        binding.spinnerNbQuestions.onItemSelectedListener = spinnerListener { position ->
            settings.nbQuestionsQuiz = SettingsManager.NB_QUESTIONS_OPTIONS
                .getOrNull(position)?.toIntOrNull() ?: SettingsManager.DEFAULT_NB_QUESTIONS
        }

        binding.spinnerTempsLimite.onItemSelectedListener = spinnerListener { position ->
            settings.tempsLimiteQuestion = SettingsManager.TEMPS_LIMITE_VALEURS
                .getOrNull(position) ?: SettingsManager.DEFAULT_TEMPS_LIMITE
        }

        binding.switchCorrections.setOnCheckedChangeListener { _, checked ->
            settings.afficherCorrections = checked
        }

        binding.switchRevisionEspacee.setOnCheckedChangeListener { _, checked ->
            settings.revisionEspaceeActive = checked
        }

        binding.switchHaptique.setOnCheckedChangeListener { _, checked ->
            settings.haptiqueActif = checked
        }

        binding.btnResetGameplay.setOnClickListener {
            settings.resetGameplay()
            initialiserGameplay()
            jouerSfxSave()
        }
    }

    // ══════════════════════════════════════════════════════════
    // 6 — AVANCÉ
    // ══════════════════════════════════════════════════════════

    private fun initialiserAvance() {
        val adapterLang = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            SettingsManager.LANGUE_OPTIONS)
        adapterLang.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLangue.adapter = adapterLang
        binding.spinnerLangue.setSelection(
            SettingsManager.LANGUE_OPTIONS.indexOf(settings.langue).coerceAtLeast(0))
        binding.spinnerLangue.isEnabled = false
        binding.spinnerLangue.alpha     = 0.4f
        binding.tvLangueLock.visibility = View.VISIBLE

        val adapterTheme = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            SettingsManager.THEME_OPTIONS)
        adapterTheme.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTheme.adapter = adapterTheme
        binding.spinnerTheme.setSelection(
            SettingsManager.THEME_OPTIONS.indexOf(settings.themeVisuel).coerceAtLeast(0))
        binding.spinnerTheme.isEnabled = false
        binding.spinnerTheme.alpha     = 0.4f
        binding.tvThemeLock.visibility = View.VISIBLE

        binding.switchHorsLigne.isChecked = settings.modeHorsLigne
        binding.switchHorsLigne.isEnabled = false
        binding.switchHorsLigne.alpha     = 0.4f
        binding.tvHorsLigneLock.visibility = View.VISIBLE

        binding.btnViderCache.setOnClickListener { confirmerViderCache() }

        binding.btnExporter.isEnabled = false
        binding.btnExporter.alpha     = 0.4f
        binding.tvExportLock.visibility = View.VISIBLE

        binding.btnResetAvance.setOnClickListener {
            settings.resetAvance()
            initialiserAvance()
            jouerSfxSave()
        }
    }

    // ══════════════════════════════════════════════════════════
    // NAVIGATION ONGLETS
    // ══════════════════════════════════════════════════════════

    private fun configurerOnglets() {
        tabLayouts.forEachIndexed { index, tab ->
            tab.setOnClickListener { changerOnglet(index) }
        }
    }

    private fun changerOnglet(index: Int) {
        if (index == ongletActif) return
        ongletActif = index
        jouerSfxTab()
        afficherOnglet(index)
    }

    private fun afficherOnglet(index: Int) {
        panels.forEach { it.visibility = View.GONE }
        tabLayouts.forEach { tab ->
            tab.setBackgroundResource(android.R.color.transparent)
            setOngletTextColor(tab, "#B8A060")
        }
        panels[index].visibility = View.VISIBLE
        tabLayouts[index].setBackgroundResource(R.drawable.bg_tab_selected)
        setOngletTextColor(tabLayouts[index], "#FFD700")
        montrerTutorielOngletSiNecessaire(index)
    }

    private fun montrerTutorielGeneralReglagesSiNecessaire() {
        TutorialManager.showHeroTutorialIfNeeded(
            activity = this,
            stepId = "settings_first_entry_v2",
            godId = "hephaestus",
            title = "⚙️ Conseil de l'Olympe ⚙️",
            message = "Bienvenue dans l'atelier des réglages. Chaque onglet affine une partie de ton Olympe : sons, lisibilité, confort, notifications, compte, gameplay, paramètres avancés et jukebox. Certains sceaux sont déjà actifs, d'autres sont encore verrouillés et seront reliés plus tard."
        )
    }

    private fun montrerTutorielOngletSiNecessaire(index: Int) {
        when (index) {
            0 -> TutorialManager.showHeroTutorialIfNeeded(this, "settings_tab_audio", "apollo", "🔊 Onglet Audio", "Ici, tu règles la puissance de la musique, des effets et des voix divines. Cet onglet est déjà opérationnel pour la majorité du confort sonore.")
            1 -> TutorialManager.showHeroTutorialIfNeeded(this, "settings_tab_affichage", "athena", "👁️ Onglet Affichage", "Ici, tu ajustes la taille du texte et la vitesse des dialogues. C'est l'onglet qui façonne la lisibilité de tes échanges RPG.")
            2 -> TutorialManager.showHeroTutorialIfNeeded(this, "settings_tab_accessibilite", "prometheus", "♿ Onglet Accessibilité", "Daltonisme, contraste élevé, réduction des flashs et coupure d'animations : cette zone protège le confort de jeu. Certaines options sont directes, d'autres demandent encore des branchements transversaux complets.")
            3 -> TutorialManager.showHeroTutorialIfNeeded(this, "settings_tab_notifications", "hermes", "🔔 Onglet Notifications", "Cet onglet gouverne les rappels divins. La façade est présente, mais toutes les liaisons profondes de notifications ne sont pas encore totalement consacrées.")
            4 -> TutorialManager.showHeroTutorialIfNeeded(this, "settings_tab_compte", "zeus", "🪪 Onglet Compte", "Tu y gères le compte actif, le code de secours et la déconnexion. Certaines interactions sont déjà réelles, mais la gestion complète multi-comptes reste encore à perfectionner selon tes prochaines décisions produit.")
            5 -> TutorialManager.showHeroTutorialIfNeeded(this, "settings_tab_gameplay", "ares", "⚔️ Onglet Gameplay", "Difficulté, nombre de questions, timer, corrections et révision espacée : c'est ici que tu règles les lois du combat scolaire.")
            6 -> TutorialManager.showHeroTutorialIfNeeded(this, "settings_tab_avance", "prometheus", "🧪 Onglet Avancé", "Cet onglet contient des pouvoirs encore scellés : langue, thème, mode hors ligne et export avancé. Ils apparaissent pour préparer l'avenir, mais plusieurs connexions restent à brancher proprement.")
            7 -> TutorialManager.showHeroTutorialIfNeeded(this, "settings_tab_jukebox", "apollo", "🎵 Onglet Jukebox", "Le Temple des Mélodies te permet d'écouter les bandes-son de RéviZeus. La lecture locale est en place ; l'extension vers des créations musicales plus profondes se fera plus tard.")
        }
    }

    private fun setOngletTextColor(tabLayout: LinearLayout, colorHex: String) {
        try {
            (tabLayout.getChildAt(1) as? TextView)
                ?.setTextColor(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) {}
    }

    // ══════════════════════════════════════════════════════════
    // JUKEBOX — TEMPLE DES MÉLODIES
    // ══════════════════════════════════════════════════════════

    private fun initialiserJukeboxPanel() {
        val tracks = OlympianMusicCatalog.buildTracks()

        if (jukeboxAdapter == null) {
            jukeboxAdapter = JukeboxAdapter(
                tracks = tracks,
                onPlayClicked = { track ->
                    if (previousGameMusicResId == -1) {
                        previousGameMusicResId = SoundManager.getCurrentMusicResId()
                    }
                    try {
                        SoundManager.playMusic(this, track.resId)
                        currentJukeboxTrackResId = track.resId
                        binding.tvCurrentTrack.text = "Lecture : ${track.title}"
                        jukeboxAdapter?.setActiveTrack(track.resId)
                        try { SoundManager.playSFX(this, R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
                    } catch (_: Exception) {
                        Toast.makeText(this, "Impossible de lancer cette piste.", Toast.LENGTH_SHORT).show()
                    }
                },
                onStopClicked = {
                    SoundManager.stopMusic()
                    currentJukeboxTrackResId = -1
                    binding.tvCurrentTrack.text = "Aucune piste du jukebox en lecture."
                    jukeboxAdapter?.setActiveTrack(-1)
                    jouerSfxSave()
                }
            )

            binding.rvJukeboxTracks.layoutManager = LinearLayoutManager(this)
            binding.rvJukeboxTracks.adapter = jukeboxAdapter
        }

        binding.btnStopJukebox.setOnClickListener {
            SoundManager.stopMusic()
            currentJukeboxTrackResId = -1
            binding.tvCurrentTrack.text = "Aucune piste du jukebox en lecture."
            jukeboxAdapter?.setActiveTrack(-1)
            jouerSfxSave()
        }

        binding.btnResumeGameMusic.setOnClickListener {
            reprendreAmbianceDuJeu()
        }
    }

    private fun reprendreAmbianceDuJeu() {
        if (previousGameMusicResId != -1) {
            try {
                SoundManager.playMusic(this, previousGameMusicResId)
                currentJukeboxTrackResId = -1
                binding.tvCurrentTrack.text = "Ambiance du jeu restaurée."
                jukeboxAdapter?.setActiveTrack(-1)
                jouerSfxSave()
            } catch (_: Exception) {}
        } else {
            Toast.makeText(this, "Aucune ambiance précédente mémorisée.", Toast.LENGTH_SHORT).show()
        }
    }

    // ══════════════════════════════════════════════════════════
    // COMPTE — SUPPRESSION / RESTART AVENTURE
    // ══════════════════════════════════════════════════════════

    private fun confirmerSuppressionCompte() {
        val currentUser = FirebaseAuthManager.getCurrentUser()
        val prefs = getSharedPreferences("ReviZeusPrefs", MODE_PRIVATE)
        val email = currentUser?.email
            ?: prefs.getString("ACCOUNT_EMAIL", "")
            ?: ""

        val input = EditText(this).apply {
            hint = "Mot de passe du compte"
            setText("")
        }

        AlertDialog.Builder(this)
            .setTitle("⚠ Supprimer le compte")
            .setMessage(
                "Pour supprimer réellement le compte Firebase, confirme le mot de passe du compte :\n$email\n\n" +
                "La progression locale sera aussi effacée."
            )
            .setView(input)
            .setPositiveButton("Supprimer") { _, _ ->
                val password = input.text?.toString()?.trim().orEmpty()
                if (password.isBlank()) {
                    toast("Le mot de passe est requis pour supprimer le compte Firebase.")
                } else {
                    supprimerCompteEtDonnees(password)
                }
            }
            .setNegativeButton("Annuler") { d, _ -> d.dismiss() }
            .setCancelable(true)
            .show()
    }

    private fun supprimerCompteEtDonnees(password: String) {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("ReviZeusPrefs", MODE_PRIVATE)
            val email = FirebaseAuthManager.getCurrentUser()?.email
                ?: prefs.getString("ACCOUNT_EMAIL", "")
                ?: ""

            // Retirer le UID du registre avant suppression Firebase
            val uid = AccountRegistry.getActiveUid(this@SettingsActivity)

            FirebaseAuthManager.reauthenticateAndDeleteAccount(
                email = email,
                password = password,
                onSuccess = {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            try { AppDatabase.getDatabase(this@SettingsActivity).clearAllTables() }
                            catch (_: Exception) {}

                            // MULTI-COMPTES v7 : retirer du registre
                            if (uid.isNotBlank()) {
                                AccountRegistry.removeUid(this@SettingsActivity, uid)
                            }

                            prefs.edit().clear().apply()

                            try {
                                ParentSummaryManager.saveToPrefs(
                                    context = this@SettingsActivity,
                                    parentEmail = "",
                                    enabled = false
                                )
                            } catch (_: Exception) {}

                            AppDatabase.resetInstance()
                        }

                        toast("Compte Firebase supprimé. Les archives ont été effacées.")
                        jouerSfxSave()
                        ouvrirLoginEnNouvellePile()
                    }
                },
                onError = { message ->
                    runOnUiThread { toast("Suppression refusée : $message") }
                }
            )
        }
    }

    private fun confirmerRecommencerAventure() {
        AlertDialog.Builder(this)
            .setTitle("↺ Recommencer l'aventure")
            .setMessage(
                "Le héros sera recréé depuis la sélection de genre.\n\n" +
                "La progression, les cours, les fragments, l'inventaire et les statistiques seront effacés, " +
                "mais le compte Firebase restera actif."
            )
            .setPositiveButton("Recommencer") { _, _ -> recommencerAventureDepuisGenre() }
            .setNegativeButton("Annuler") { d, _ -> d.dismiss() }
            .setCancelable(true)
            .show()
    }

    private fun recommencerAventureDepuisGenre() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    try { AppDatabase.getDatabase(this@SettingsActivity).clearAllTables() }
                    catch (_: Exception) {}

                    val prefs = getSharedPreferences("ReviZeusPrefs", MODE_PRIVATE)
                    val hasAccount    = prefs.getBoolean("HAS_ACCOUNT", false)
                    val accountEmail  = prefs.getString("ACCOUNT_EMAIL", "") ?: ""
                    val recoveryEmail = prefs.getString("RECOVERY_EMAIL", "") ?: ""
                    val firebaseUid   = prefs.getString("FIREBASE_UID", "") ?: ""
                    val isEmailVerified = prefs.getBoolean("IS_EMAIL_VERIFIED", false)

                    prefs.edit().clear().apply()
                    prefs.edit()
                        .putBoolean("HAS_ACCOUNT", hasAccount)
                        .putString("ACCOUNT_EMAIL", accountEmail)
                        .putString("RECOVERY_EMAIL", recoveryEmail)
                        .putString("FIREBASE_UID", firebaseUid)
                        .putBoolean("IS_EMAIL_VERIFIED", isEmailVerified)
                        .apply()

                    try {
                        ParentSummaryManager.saveToPrefs(
                            context = this@SettingsActivity,
                            parentEmail = "",
                            enabled = false
                        )
                    } catch (_: Exception) {}
                }

                withContext(Dispatchers.Main) {
                    jouerSfxSave()
                    declencherVibration(140L)
                    val intent = Intent(this@SettingsActivity, GenderActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finishAffinity()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Erreur : ${e.message}") }
            }
        }
    }

    private fun ouvrirLoginEnNouvellePile() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finishAffinity()
    }

    // ══════════════════════════════════════════════════════════
    // PARENTS — PHASE LOCALE
    // ══════════════════════════════════════════════════════════

    private fun sauvegarderParentsLocalement(fermerApres: Boolean) {
        val parentEmail = binding.etParentEmailSettings.text.toString().trim()
        val enabled = binding.switchNotifParents.isChecked

        if (parentEmail.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(parentEmail).matches()) {
            DialogRPGManager.showAlert(
                activity = this,
                godId = "athena",
                message = "L'adresse parentale indiquée n'a pas la forme d'un email valide. Corrige-la avant de sceller l'espace parents.",
                title = "Adresse invalide"
            )
            return
        }

        ParentSummaryManager.saveToPrefs(context = this, parentEmail = parentEmail, enabled = enabled)

        lifecycleScope.launch {
            try {
                ParentSummaryManager.syncToRoom(
                    context = this@SettingsActivity,
                    parentEmail = parentEmail,
                    enabled = enabled
                )
            } catch (_: Exception) {}
        }

        DialogRPGManager.showReward(
            activity = this,
            godId = "prometheus",
            title = "Espace parents",
            message = "L'espace parents a bien été sauvegardé sur cet appareil."
        )
        jouerSfxSave()
        if (fermerApres) finish()
    }

    // ══════════════════════════════════════════════════════════
    // BOUTONS BAS
    // ══════════════════════════════════════════════════════════

    private fun configurerBoutonsBas() {
        binding.btnRetour.setOnClickListener {
            jouerSfxTab()
            finish()
        }

        binding.btnOk.setOnClickListener {
            sauvegarderTout()
            jouerSfxSave()
            declencherVibration(80L)
            finish()
        }
    }

    // ══════════════════════════════════════════════════════════
    // SAUVEGARDE GLOBALE
    // ══════════════════════════════════════════════════════════

    private fun sauvegarderTout() {
        settings.volumeMusique  = binding.seekMusiqueVolume.progress
        settings.volumeSfx      = binding.seekSfxVolume.progress
        settings.volumeDialogue = binding.seekDialogueVolume.progress
        settings.muetGeneral    = binding.switchMuet.isChecked
        settings.qualiteAudio   = binding.spinnerQualiteAudio.selectedItem?.toString()
            ?: SettingsManager.DEFAULT_QUALITE_AUDIO
        SoundManager.setVolume(settings.getVolumeMusiquef())
        if (settings.muetGeneral) SoundManager.pauseMusic()

        settings.tailleTexte     = binding.seekTailleTexte.progress
        settings.vitesseDialogue = binding.seekVitesseDialogue.progress

        settings.modeDaltonien       = binding.switchDaltonien.isChecked
        settings.typeDaltonisme      = binding.spinnerDaltonisme.selectedItem?.toString()
            ?: SettingsManager.DEFAULT_TYPE_DALTONISME
        settings.contrasteEleve      = binding.switchContrasteEleve.isChecked
        settings.reduireFlashs       = binding.switchReduireFlashs.isChecked
        settings.animationsDesactivees = binding.switchAnimationsOff.isChecked

        settings.notifQuotidien    = binding.switchNotifQuotidien.isChecked
        settings.notifQcm          = binding.switchNotifQcm.isChecked
        settings.notifRecompenses  = binding.switchNotifRecompenses.isChecked
        settings.notifStreak       = binding.switchNotifStreak.isChecked

        settings.difficulteQuiz = binding.spinnerDifficulte.selectedItem?.toString()
            ?: SettingsManager.DEFAULT_DIFFICULTE
        val idxNb = binding.spinnerNbQuestions.selectedItemPosition.coerceAtLeast(0)
        settings.nbQuestionsQuiz = SettingsManager.NB_QUESTIONS_OPTIONS
            .getOrNull(idxNb)?.toIntOrNull() ?: SettingsManager.DEFAULT_NB_QUESTIONS
        val idxTemps = binding.spinnerTempsLimite.selectedItemPosition.coerceAtLeast(0)
        settings.tempsLimiteQuestion = SettingsManager.TEMPS_LIMITE_VALEURS
            .getOrNull(idxTemps) ?: SettingsManager.DEFAULT_TEMPS_LIMITE
        settings.afficherCorrections   = binding.switchCorrections.isChecked
        settings.revisionEspaceeActive = binding.switchRevisionEspacee.isChecked
        settings.haptiqueActif         = binding.switchHaptique.isChecked
    }

    // ══════════════════════════════════════════════════════════
    // DIALOGS DE CONFIRMATION
    // ══════════════════════════════════════════════════════════

    private fun confirmerReinitialisation() {
        AlertDialog.Builder(this)
            .setTitle("⚠ Zeus est en colère")
            .setMessage(
                "Es-tu certain de vouloir effacer toute ta progression ?\n\n" +
                "XP, niveaux, cours scannés et scores seront perdus à jamais."
            )
            .setPositiveButton("Oui, tout effacer") { _, _ -> reinitialiserProgression() }
            .setNegativeButton("Non, je recule") { d, _ -> d.dismiss() }
            .setCancelable(true).show()
    }

    private fun reinitialiserProgression() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@SettingsActivity)
                withContext(Dispatchers.IO) {
                    db.iAristoteDao().resetUserProfile()
                    db.iAristoteDao().deleteAllCourses()
                    db.iAristoteDao().deleteAllMemoryScores()
                }
                withContext(Dispatchers.Main) {
                    try { SoundManager.playSFX(this@SettingsActivity, R.raw.sfx_thunder_confirm) } catch (_: Exception) {}
                    declencherVibration(300L)
                    initialiserCompte()
                    DialogRPGManager.showReward(
                        activity = this@SettingsActivity,
                        godId = "zeus",
                        message = "La progression a été réinitialisée. Zeus approuve la décision prise."
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Erreur : ${e.message}") }
            }
        }
    }

    private fun confirmerViderCache() {
        AlertDialog.Builder(this)
            .setTitle("🗑 Vider le cache")
            .setMessage("Supprimer uniquement les cours scannés ?\n\nTon XP, niveau et streak seront conservés.")
            .setPositiveButton("Vider") { _, _ -> viderCacheCours() }
            .setNegativeButton("Annuler") { d, _ -> d.dismiss() }
            .setCancelable(true).show()
    }

    private fun viderCacheCours() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@SettingsActivity)
                withContext(Dispatchers.IO) {
                    db.iAristoteDao().deleteAllCourses()
                    db.iAristoteDao().deleteAllMemoryScores()
                }
                withContext(Dispatchers.Main) {
                    jouerSfxSave()
                    initialiserCompte()
                    DialogRPGManager.showReward(
                        activity = this@SettingsActivity,
                        godId = "hephaestus",
                        message = "Le cache des cours a été vidé. L'ordre du temple a été restauré."
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Erreur : ${e.message}") }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════

    private fun seekListener(onChanged: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar?, p: Int, user: Boolean) { onChanged(p) }
        override fun onStartTrackingTouch(sb: SeekBar?) {}
        override fun onStopTrackingTouch(sb: SeekBar?) {}
    }

    private fun spinnerListener(onItemSelected: (Int) -> Unit) =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onItemSelected(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

    private fun jouerSfxTab() {
        try { SoundManager.playSFX(this, R.raw.sfx_settings_tab) } catch (_: Exception) {}
    }

    private fun jouerSfxSave() {
        try { SoundManager.playSFX(this, R.raw.sfx_settings_save) } catch (_: Exception) {}
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    /**
     * Convertit des dp en pixels.
     * Utile pour créer programmatiquement des vues avec des tailles cohérentes.
     */
    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun declencherVibration(dureeMs: Long) {
        if (!settings.haptiqueActif) return
        try {
            val v = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(android.os.VibrationEffect.createOneShot(
                    dureeMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") v.vibrate(dureeMs)
            }
        } catch (_: Exception) {}
    }

    private fun appliquerTailleTexteGlobale() {
        val facteur = when (settings.tailleTexte) {
            in 0..20 -> 0.9f
            in 21..40 -> 1.0f
            in 41..60 -> 1.1f
            in 61..80 -> 1.2f
            else -> 1.35f
        }

        fun appliquer(view: View) {
            if (view is TextView) {
                view.textSize = view.textSize / resources.displayMetrics.scaledDensity * facteur
            }
            if (view is android.view.ViewGroup) {
                for (i in 0 until view.childCount) appliquer(view.getChildAt(i))
            }
        }

        appliquer(binding.root)
    }

    // ══════════════════════════════════════════════════════════
    // CYCLE DE VIE
    // ══════════════════════════════════════════════════════════

    override fun onResume() {
        super.onResume()
        try { SoundManager.resumeMusic() } catch (e: Exception) {
            Log.w("REVIZEUS_SETTINGS", "onResume: reprise musique impossible", e)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing && currentJukeboxTrackResId != -1 && previousGameMusicResId != -1) {
            try { SoundManager.playMusic(this, previousGameMusicResId) } catch (e: Exception) {
                Log.w("REVIZEUS_SETTINGS", "onPause: restauration musique précédente impossible", e)
            }
        }
    }
}

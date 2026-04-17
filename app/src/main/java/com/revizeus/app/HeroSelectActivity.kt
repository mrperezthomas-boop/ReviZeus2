package com.revizeus.app

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.revizeus.app.core.XpCalculator
import com.revizeus.app.models.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HeroSelectActivity : BaseActivity() {

    companion object {
        const val EXTRA_FIREBASE_UID = "FIREBASE_UID"

        /**
         * Même clé de flow temporaire que LoginActivity.
         * HeroSelectActivity l'utilise pour indiquer explicitement que
         * le prochain passage par Auth/Gender/Avatar correspond à un ajout de héros
         * sur un compte déjà existant.
         */
        private const val PREF_HERO_CREATION_MODE = "HERO_CREATION_MODE"
        private const val HERO_CREATION_MODE_EXISTING_ACCOUNT = "existing_account"
    }

    private val TAG = "HeroSelect"
    private var animatedBackgroundHelper: AnimatedBackgroundHelper? = null
    private var olympianParticlesView: OlympianParticlesView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hero_select)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setupImmersiveMode()

        SoundManager.rememberMusic(R.raw.bgm_title_select)
        if (!SoundManager.isPlayingMusic()) {
            SoundManager.playMusicDelayed(this, R.raw.bgm_title_select, 300L)
        }

        try {
            olympianParticlesView = findViewById(R.id.olympianParticlesView)
            animatedBackgroundHelper = AnimatedBackgroundHelper(
                targetView = findViewById(R.id.rootHeroSelect),
                particlesView = olympianParticlesView
            )
            animatedBackgroundHelper?.start(
                accentColor = Color.parseColor("#1E90FF"),
                mode = OlympianParticlesView.ParticleMode.SAVOIR
            )
        } catch (_: Exception) {
        }

        val uid = intent.getStringExtra(EXTRA_FIREBASE_UID) ?: AccountRegistry.getActiveUid(this)
        if (uid.isBlank()) {
            Log.e(TAG, "UID vide — retour LoginActivity")
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        val email = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
            .getString("ACCOUNT_EMAIL", "") ?: AccountRegistry.getRememberedAccountEmail(this, uid)
        findViewById<TextView>(R.id.tvHeroSelectEmail).text = email

        findViewById<View>(R.id.btnBackHeroSelect)?.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            retournerTitre()
        }

        onBackPressedDispatcher.addCallback(this) {
            try {
                SoundManager.playSFX(this@HeroSelectActivity, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            retournerTitre()
        }

        findViewById<RecyclerView>(R.id.rvHeroSlots).layoutManager = LinearLayoutManager(this)
        chargerSlots(uid)
    }

    /**
     * CORRECTION : Rafraîchir TOUS les caches depuis Room avant d'afficher les cartes
     * pour que les stats (level, éclats, ambroisie, temps joué) soient à jour.
     */
    private fun chargerSlots(uid: String) {
        lifecycleScope.launch {
            // ÉTAPE 1 : Rafraîchir tous les caches depuis Room
            withContext(Dispatchers.IO) {
                for (slot in 1..AccountRegistry.MAX_SLOTS_PER_UID) {
                    rafraichirCacheSlot(uid, slot)
                }
            }

            // ÉTAPE 2 : Charger les slots avec les caches fraîchement mis à jour
            val slots = AccountRegistry.ensureSlotsForUid(this@HeroSelectActivity, uid)
            findViewById<RecyclerView>(R.id.rvHeroSlots).adapter = HeroSlotAdapter(
                slots = slots,
                uid = uid,
                onPlaySlot = { slot -> jouerSlot(uid, slot) },
                onCreateHero = { slot -> creerHero(uid, slot) },
                onLongPressHero = { slot, cache -> afficherMenuHero(uid, slot, cache) },
                context = this@HeroSelectActivity
            )
        }
    }

    /**
     * Rafraîchit le cache d'un slot depuis Room pour avoir les vraies valeurs actuelles.
     */
    private suspend fun rafraichirCacheSlot(uid: String, slot: Int) = withContext(Dispatchers.IO) {
        try {
            val mainPrefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
            val previousUid = mainPrefs.getString("FIREBASE_UID", "") ?: ""
            val previousSlot = AccountRegistry.getActiveSlot(this@HeroSelectActivity, uid)

            try {
                // Basculer temporairement vers ce slot
                mainPrefs.edit().putString("FIREBASE_UID", uid).apply()
                AccountRegistry.setActiveSlot(this@HeroSelectActivity, uid, slot)
                AppDatabase.resetInstance()

                val db = AppDatabase.getDatabase(this@HeroSelectActivity)
                val profile = db.iAristoteDao().getUserProfile()

                if (profile != null) {
                    // Compter le nombre de savoirs (cours scannés)
                    val coursCount = db.iAristoteDao().countCourses()
                    
                    // Recalculer le level depuis l'XP
                    val calculatedLevel = XpCalculator.calculateLevel(profile.xp)
                    
                    // Mettre à jour le profil avec le level calculé
                    profile.level = calculatedLevel
                    db.iAristoteDao().updateUserProfile(profile)
                    
                    // Mettre à jour le cache avec les vraies valeurs actuelles
                    AccountRegistry.updateCacheFromProfile(
                        context = this@HeroSelectActivity,
                        uid = uid,
                        profile = profile,
                        totalCoursScanned = coursCount
                    )
                    
                    Log.d(TAG, "Cache slot $slot rafraîchi: level=$calculatedLevel, eclats=${profile.eclatsSavoir}, ambroisie=${profile.ambroisie}, savoirs=$coursCount")
                }
            } finally {
                // Restaurer l'UID et slot d'origine
                mainPrefs.edit().putString("FIREBASE_UID", previousUid).apply()
                if (uid.isNotBlank()) {
                    AccountRegistry.setActiveSlot(this@HeroSelectActivity, uid, previousSlot)
                }
                AppDatabase.resetInstance()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur rafraîchissement cache slot $slot: ${e.message}", e)
        }
    }

    private fun jouerSlot(uid: String, slot: Int) {
        try {
            SoundManager.playSFX(this, R.raw.sfx_thunder_confirm)
        } catch (_: Exception) {
        }

        AccountRegistry.setActiveUid(this, uid)
        AccountRegistry.setActiveSlot(this, uid, slot)
        AppDatabase.resetInstance()

        lifecycleScope.launch {
            delay(200L)
            SoundManager.stopMusic()
            startActivity(Intent(this@HeroSelectActivity, MoodActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun creerHero(uid: String, slot: Int) {
        try {
            SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
        } catch (_: Exception) {
        }

        AccountRegistry.setActiveUid(this, uid)
        AccountRegistry.setActiveSlot(this, uid, slot)

        // Correctif critique multi-héros :
        // on marque explicitement le flow comme "ajout de héros sur compte existant"
        // pour empêcher AvatarActivity de réutiliser un ancien OnboardingSession.
        getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_HERO_CREATION_MODE, HERO_CREATION_MODE_EXISTING_ACCOUNT)
            .remove("PENDING_ACCOUNT_EMAIL")
            .apply()

        OnboardingSession.clear()
        AppDatabase.resetInstance()

        lifecycleScope.launch {
            delay(150L)
            startActivity(Intent(this@HeroSelectActivity, AuthActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun afficherMenuHero(uid: String, slot: Int, cache: AccountRegistry.AccountCache) {
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
            text = cache.pseudo.ifBlank { "Héros" }
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFD700"))
            typeface = Typeface.DEFAULT_BOLD
        }

        val subtitle = TextView(this).apply {
            text = "Slot $slot\nCompte : ${
                AccountRegistry.getRememberedAccountEmail(this@HeroSelectActivity, uid).ifBlank { uid }
            }"
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
            jouerSlot(uid, slot)
        }

        btnDelete.setOnClickListener {
            dialog.dismiss()
            confirmerSuppressionHero(uid, slot, cache.pseudo)
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

    private fun confirmerSuppressionHero(uid: String, slot: Int, pseudo: String) {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(14))
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#1A0A00"))
            }
        }

        val title = TextView(this).apply {
            text = "⚠ SUPPRESSION DÉFINITIVE"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FF6B6B"))
            typeface = Typeface.DEFAULT_BOLD
        }

        val message = TextView(this).apply {
            text = "Tu es sur le point de supprimer définitivement le héros \"$pseudo\" (Slot $slot).\n\nCette action est irréversible.\n\n" +
                    "⚠ IMPORTANT : Cela supprime uniquement les données locales de ce téléphone. " +
                    "Ton compte Firebase reste intact et tu peux recréer un héros dans ce slot."
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#F4E7B7"))
            setLineSpacing(dp(3).toFloat(), 1f)
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val btnConfirm = creerBoutonRpgLigne("SUPPRIMER", "#FF6B6B", 1f, dp(6), 0)
        val btnCancel = creerBoutonRpgLigne("ANNULER", "#A5D6A7", 1f, 0, dp(6))

        row.addView(btnCancel)
        row.addView(btnConfirm)

        content.addView(title)
        content.addView(espace(10))
        content.addView(message)
        content.addView(row)

        val dialog = AlertDialog.Builder(this)
            .setView(content)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnConfirm.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_transition_thunder)
            } catch (_: Exception) {
            }
            dialog.dismiss()
            AccountRegistry.deleteHeroLocal(this, uid, slot)
            Toast.makeText(this, "Héros supprimé localement", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                delay(300L)
                chargerSlots(uid)
            }
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

    private fun retournerTitre() {
        startActivity(Intent(this, TitleScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onPause() {
        super.onPause()
        try {
            animatedBackgroundHelper?.stop()
        } catch (_: Exception) {
        }
        SoundManager.pauseMusic()
    }

    override fun onResume() {
        super.onResume()
        try {
            animatedBackgroundHelper?.start(
                accentColor = Color.parseColor("#1E90FF"),
                mode = OlympianParticlesView.ParticleMode.SAVOIR
            )
        } catch (_: Exception) {
        }

        try {
            SoundManager.resumeMusic()
        } catch (_: Exception) {
            SoundManager.resumeRememberedMusicDelayed(this, 300L)
        }

        if (!SoundManager.isPlayingMusic()) {
            SoundManager.resumeRememberedMusicDelayed(this, 300L)
        }
        
        // Rafraîchir les cartes de héros au retour sur cet écran
        val uid = intent.getStringExtra(EXTRA_FIREBASE_UID) ?: AccountRegistry.getActiveUid(this)
        if (uid.isNotBlank()) {
            chargerSlots(uid)
        }
    }

    override fun onDestroy() {
        try {
            animatedBackgroundHelper?.stop()
        } catch (_: Exception) {
        }
        animatedBackgroundHelper = null
        olympianParticlesView = null
        super.onDestroy()
    }

    private class HeroSlotAdapter(
        private val slots: Map<Int, AccountRegistry.AccountCache?>,
        private val uid: String,
        private val onPlaySlot: (Int) -> Unit,
        private val onCreateHero: (Int) -> Unit,
        private val onLongPressHero: (Int, AccountRegistry.AccountCache) -> Unit,
        private val context: Context
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val slotList = (1..AccountRegistry.MAX_SLOTS_PER_UID).toList()

        private class OccupiedVH(view: View) : RecyclerView.ViewHolder(view)
        private class EmptyVH(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int = slotList.size

        override fun getItemViewType(position: Int): Int {
            return if (slots[slotList[position]] != null) 0 else 1
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = android.view.LayoutInflater.from(context)
            return if (viewType == 0) {
                OccupiedVH(inflater.inflate(R.layout.item_hero_slot_card, parent, false))
            } else {
                EmptyVH(inflater.inflate(R.layout.item_hero_slot_empty, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val slot = slotList[position]
            val cache = slots[slot]

            if (holder is OccupiedVH && cache != null) {
                bindOccupied(holder.itemView, cache, slot)
            } else if (holder is EmptyVH) {
                bindEmpty(holder.itemView, slot)
            }
        }

        private fun bindOccupied(view: View, cache: AccountRegistry.AccountCache, slot: Int) {
            val avatar = view.findViewById<ImageView>(R.id.ivSlotAvatar)
            val pseudo = view.findViewById<TextView>(R.id.tvSlotPseudo)
            val level = view.findViewById<TextView>(R.id.tvSlotLevel)
            val line1 = view.findViewById<TextView>(R.id.tvSlotLine1)
            val playTime = view.findViewById<TextView>(R.id.tvSlotPlayTime)
            val savoirs = view.findViewById<TextView>(R.id.tvSlotSavoirs)
            val eclats = view.findViewById<TextView>(R.id.tvSlotEclats)
            val ambroisie = view.findViewById<TextView>(R.id.tvSlotAmbroisie)

            val resId = context.resources.getIdentifier(cache.avatarResName, "drawable", context.packageName)
            avatar.setImageResource(if (resId != 0) resId else android.R.drawable.sym_def_app_icon)

            pseudo.text = cache.pseudo.ifBlank { "Héros" }
            level.text = "LVL ${cache.level}"
            line1.text = "Slot $slot"
            playTime.text = "⏱ ${AccountRegistry.formatPlayTime(cache.totalPlayTimeSeconds)}"
            savoirs.text = "📚 ${cache.totalCoursScanned} savoir" + if (cache.totalCoursScanned > 1) "s" else ""
            eclats.text = "${cache.eclatsSavoir} Éclats de savoir"
            ambroisie.text = "${cache.ambroisie} Ambroisie"

            view.setOnClickListener { onPlaySlot(slot) }

            view.setOnLongClickListener {
                onLongPressHero(slot, cache)
                true
            }
        }

        private fun bindEmpty(view: View, slot: Int) {
            view.findViewById<TextView>(R.id.tvEmptySlotLabel).text = "SLOT $slot"
            view.findViewById<FrameLayout>(R.id.btnCreateHero).setOnClickListener { onCreateHero(slot) }
            view.setOnClickListener { onCreateHero(slot) }
        }
    }
}

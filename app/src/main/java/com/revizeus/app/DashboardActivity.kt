package com.revizeus.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.revizeus.app.core.InsightCache
import com.revizeus.app.core.XpCalculator
import com.revizeus.app.databinding.ActivityDashboardBinding
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.CourseEntry
import com.revizeus.app.models.UserProfile
import com.revizeus.app.ui.DashboardInsightsWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * DASHBOARD — L'Olympe Quotidien
 *
 * HUD BAR v5 — Architecture unifiée :
 * ✅ layoutCurrenciesHud = barre pleine largeur (match_parent)
 * ├─ groupChipsResources [weight=1] : chipEclat, chipAmbroisie,
 * │   chipDayStreak, chipWinStreak, chipForge + futurs chips
 * └─ groupIconsNav [wrap_content] : btnInventoryDash, btnBadgeBookDash,
 * btnSettings, frameDebugML + futurs boutons
 * ✅ Plus de scaleX → plus de débordement
 * ✅ Plus de layout_toStartOf → plus de conflits d'ordre XML
 * ✅ Extensible : ajouter un chip = copier-coller dans groupChipsResources
 * ajouter un bouton = copier-coller dans groupIconsNav
 * ✅ chipForge = FrameLayout clickable → ForgeActivity
 * ✅ viewForgeNotif : point rouge via CraftingSystem.affordableRecipes()
 * ✅ Popup Prométhée : clic sur chipEclat/chipAmbroisie/chipDayStreak/chipWinStreak
 *
 * ══════════════════════════════════════════════════════════════════
 * CORRECTIFS ARCHITECTE SUPRÊME :
 * ✅ OnBackPressed dispatcher reprogrammé avec le UI Builder Kotlin.
 * ✅ Pop-Up de sortie Zeus intégré "en dur" (pas de nouveau XML pour éviter les crashs d'ID).
 * ✅ Imite 100% le rendu Premium de Prométhée mais avec Zeus.
 * ✅ Ajout des 4 actions de navigation demandées.
 * ✅ Texte Zeus vivant, lettre par lettre, sans changer le design.
 * ✅ Popups Prométhée gardés visuellement identiques mais rendus vivants en typewriter.
 * ✅ Tap sur les textes Zeus / Prométhée = révélation immédiate.
 * ══════════════════════════════════════════════════════════════════
 */
class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding

    private var lastAppliedMood: String = ""
    private val godAnim = GodSpeechAnimator()
    private var welcomeTypewriterJob: Job? = null
    private var prometheusTypewriterJob: Job? = null

    // Système de fond animé
    private var backgroundPlayer: ExoPlayer? = null

    /**
     * Garde-fou de session pour éviter de réafficher l'alerte Déméter
     * à chaque micro-retour au Dashboard tant que l'Activity reste vivante.
     */
    private var hasShownGardenDialogueThisSession: Boolean = false

    /**
     * Garde-fou anti double-popup pour le parchemin de mise à jour.
     */
    private var isMajPopupShowing: Boolean = false

    companion object {
        /**
         * Garde-fou global de "connexion/session".
         * Cette variable vit tant que le process de l'application reste en mémoire.
         *
         * Résultat :
         * - le popup de mise à jour s'affiche une seule fois par ouverture de l'app / session
         * - il ne se réaffiche plus à chaque retour sur le Dashboard
         * - aucun renommage ni refonte d'architecture nécessaire
         */
        private var hasShownMajPopupThisConnection: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        binding.tvLevel.text = "LVL 1"
        binding.tvXpTotal.text = "0 / ${XpCalculator.xpThresholdForLevel(1)} XP"
        binding.pbXp.max = XpCalculator.xpThresholdForLevel(1)
        binding.pbXp.progress = 0

        binding.tvCurrencyEclat.text = "0"
        binding.tvCurrencyAmbroisie.text = "0"
        binding.tvDayStreakHud.text = "0"
        binding.tvWinStreakHud.text = "0"

        loadUserData()
        setupMenu()
        setupHeroShortcuts()
        setupHudTooltips()
        setupPrometheusLongPresses()
        setupDashboardBackNavigation()
        applyMoodTheme()

        binding.root.post {
            try {
                TutorialManager.runHeroFirstTimeFeature(this@DashboardActivity, "dashboard") {}
            } catch (_: Exception) {
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        applyMoodTheme()
    }

    private fun applyMoodTheme() {
        val rawMood = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
            .getString("CURRENT_MOOD", "JOYEUX") ?: "JOYEUX"

        val mood = rawMood
            .trim()
            .uppercase()
            .replace("É", "E")
            .replace("È", "E")
            .replace("Ê", "E")
            .replace("À", "A")
            .replace("Â", "A")
            .replace("Ù", "U")
            .replace("Û", "U")
            .replace("Î", "I")
            .replace("Ï", "I")
            .replace("Ô", "O")
            .replace("Ö", "O")
            .replace("Ç", "C")

        // Map mood → ressources vidéo et fallback statique
        val videoResName = when (mood) {
            "JOYEUX" -> "bg_dashboard_happy_animated"
            "FATIGUE" -> "bg_dashboard_tired_animated"
            "STRESSE" -> "bg_dashboard_stressed_animated"
            else -> null
        }

        val fallbackDrawable = when (mood) {
            "JOYEUX" -> R.drawable.bg_dashboard_happy
            "FATIGUE" -> R.drawable.bg_dashboard_tired
            "STRESSE" -> R.drawable.bg_dashboard_stressed
            else -> R.drawable.bg_olympus_dark
        }

        // Tenter de charger la vidéo animée, sinon fallback statique
        if (videoResName != null) {
            val videoResId = resources.getIdentifier(videoResName, "raw", packageName)
            if (videoResId != 0) {
                setupAnimatedBackground(videoResId)
            } else {
                setupStaticBackground(fallbackDrawable)
            }
        } else {
            setupStaticBackground(fallbackDrawable)
        }

        // Gestion de la musique selon l'humeur
        val musicRes = when (mood) {
            "JOYEUX" -> R.raw.bgm_dashboard_happy
            "FATIGUE" -> R.raw.bgm_dashboard_tired
            "STRESSE" -> R.raw.bgm_dashboard_stressed
            else -> R.raw.bgm_dashboard
        }

        if (mood != lastAppliedMood.uppercase()) {
            SoundManager.stopMusic()
            lastAppliedMood = mood
        }

        val settings = SettingsManager(this)
        val musicVolume = (settings.volumeMusique / 100f).coerceIn(0f, 1f)
        SoundManager.setVolume(musicVolume)

        if (settings.muetGeneral) {
            SoundManager.pauseMusic()
            return
        }

        try {
            SoundManager.playMusic(this, musicRes)
            SoundManager.rememberMusic(musicRes)
        } catch (e: Exception) {
            Log.e("REVIZEUS", "Dashboard: musique mood erreur: ${e.message}")
            try {
                SoundManager.playMusic(this, R.raw.bgm_dashboard)
                SoundManager.rememberMusic(R.raw.bgm_dashboard)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Configure un fond vidéo animé selon l'humeur
     * Volume = 0f car SoundManager gère la musique
     */
    private fun setupAnimatedBackground(videoResId: Int) {
        try {
            binding.pvDashboardBackground.visibility = View.VISIBLE
            binding.ivDashboardBackground.visibility = View.GONE

            backgroundPlayer?.release()
            backgroundPlayer = ExoPlayer.Builder(this).build().also { player ->
                binding.pvDashboardBackground.player = player
                player.volume = 0f
                player.repeatMode = Player.REPEAT_MODE_ONE

                val uri = Uri.parse("android.resource://$packageName/$videoResId")
                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
                player.play()
            }
        } catch (e: Exception) {
            Log.e("REVIZEUS", "Erreur setup vidéo animée: ${e.message}")
            setupStaticBackground(R.drawable.bg_olympus_dark)
        }
    }

    /**
     * Configure un fond image statique (fallback si vidéo absente)
     */
    private fun setupStaticBackground(drawableResId: Int) {
        try {
            backgroundPlayer?.release()
            backgroundPlayer = null

            binding.pvDashboardBackground.visibility = View.GONE
            binding.ivDashboardBackground.visibility = View.VISIBLE
            binding.ivDashboardBackground.setImageResource(drawableResId)
        } catch (e: Exception) {
            Log.e("REVIZEUS", "Erreur setup fond statique: ${e.message}")
        }
    }

    private fun afficherMiniNotifBadge(badge: BadgeDefinition) {
        try {
            SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
        } catch (_: Exception) {
        }

        android.widget.Toast.makeText(
            this,
            "🏆 Succès débloqué : ${badge.nom}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    private fun afficherDerniersBadges() {
        try {
            val container = binding.llRecentBadges
            container.removeAllViews()
            val density = resources.displayMetrics.density
            val taille = (32 * density).toInt()

            val derniers = BadgeManager.getDerniersDebloques(this, 3)
            if (derniers.isEmpty()) return

            derniers.forEach { badge ->
                val iv = android.widget.ImageView(this).apply {
                    setImageResource(badge.iconDrawable)
                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                    layoutParams = android.widget.LinearLayout.LayoutParams(taille, taille)
                        .also { it.marginEnd = (6 * density).toInt() }
                    setOnClickListener {
                        try {
                            SoundManager.playSFX(this@DashboardActivity, R.raw.sfx_avatar_confirm)
                        } catch (_: Exception) {
                        }
                        startActivity(Intent(this@DashboardActivity, BadgeBookActivity::class.java))
                    }
                }
                container.addView(iv)
            }
        } catch (_: Exception) {
        }
    }

    private fun loadUserData() {
        val prefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        val pseudoPref = prefs.getString("AVATAR_PSEUDO", "Héros") ?: "Héros"
        val avatarResPref = prefs.getInt("SELECTED_AVATAR_RES", android.R.drawable.sym_def_app_icon)

        binding.tvHeroName.text = pseudoPref

        try {
            binding.imgAvatarHero.setImageResource(avatarResPref)
        } catch (_: Exception) {
            binding.imgAvatarHero.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@DashboardActivity)
                val dao = db.iAristoteDao()
                val profile = dao.getUserStats()

                if (profile != null) {
                    updateDayStreak(profile, db)
                }

                val thresholdTime = System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L)
                val fadingCourse = dao.getFadingCourse(thresholdTime)

                val safeProfile = profile ?: UserProfile(
                    id = 1,
                    age = 15,
                    classLevel = "Terminale",
                    mood = "Prêt",
                    xp = 0,
                    streak = 0,
                    cognitivePattern = "Général"
                )

                val gardenResponse = if (fadingCourse != null) {
                    try {
                        val fadingLessonTitle = fadingCourse.displayTitle()
                        GodLoreManager.buildGardenDialogue(
                            fadingLessonTitle = fadingLessonTitle,
                            profile = safeProfile
                        )
                    } catch (e: Exception) {
                        Log.e("REVIZEUS", "Dialogue Déméter impossible : ${e.message}")
                        null
                    }
                } else {
                    null
                }

                withContext(Dispatchers.Main) {
                    if (profile != null) {
                        val level = XpCalculator.calculateLevel(profile.xp)
                        val xpInLevel = XpCalculator.xpInCurrentLevel(profile.xp)
                        val seuil = XpCalculator.xpThresholdForLevel(level)

                        binding.tvLevel.text = "LVL $level"
                        binding.tvXpTotal.text = "$xpInLevel / $seuil XP"
                        binding.pbXp.max = seuil
                        binding.pbXp.progress = xpInLevel

                        binding.tvCurrencyEclat.text = profile.eclatsSavoir.toString()
                        binding.tvCurrencyAmbroisie.text = profile.ambroisie.toString()
                        binding.tvDayStreakHud.text = profile.dayStreak.toString()
                        binding.tvWinStreakHud.text = profile.winStreak.toString()

                        try {
                            val craftAvailable = CraftingSystem.affordableRecipes(profile).isNotEmpty()
                            binding.viewForgeNotif.visibility =
                                if (craftAvailable) View.VISIBLE else View.GONE
                        } catch (_: Exception) {
                        }

                        lifecycleScope.launch {
                            try {
                                val insights = withContext(Dispatchers.IO) {
                                    InsightCache.getInsights(
                                        context = this@DashboardActivity,
                                        userId = 1,
                                        forceRefresh = false
                                    )
                                }

                                val insightsWidget = DashboardInsightsWidget.buildInsightsContainer(
                                    context = this@DashboardActivity,
                                    insights = insights,
                                    maxVisible = 3
                                )

                                try {
                                    val container = findViewById<android.widget.LinearLayout>(
                                        resources.getIdentifier("insightsContainer", "id", packageName)
                                    )
                                    if (container != null) {
                                        container.removeAllViews()
                                        container.addView(insightsWidget)
                                        Log.d("REVIZEUS_BLOC3C", "Widget insights affiché : ${insights.size} insights")
                                    } else {
                                        Log.w("REVIZEUS_BLOC3C", "Container insightsContainer non trouvé dans le layout")
                                    }
                                } catch (e: Exception) {
                                    Log.e("REVIZEUS_BLOC3C", "Erreur ajout widget au container: ${e.message}")
                                }

                            } catch (e: Exception) {
                                Log.e("REVIZEUS_BLOC3C", "Erreur affichage insights", e)
                            }
                        }

                        if (profile.pseudo.isNotBlank()) {
                            binding.tvHeroName.text = profile.pseudo
                        }

                        try {
                            val avatarResId = profile.getAvatarResId(this@DashboardActivity)
                            if (avatarResId != 0) {
                                binding.imgAvatarHero.setImageResource(avatarResId)
                            } else {
                                binding.imgAvatarHero.setImageResource(android.R.drawable.sym_def_app_icon)
                            }
                        } catch (e: Exception) {
                            Log.e("REVIZEUS", "Dashboard avatar erreur: ${e.message}")
                            binding.imgAvatarHero.setImageResource(android.R.drawable.sym_def_app_icon)
                        }

                        lifecycleScope.launch {
                            BadgeManager.recordLogin(this@DashboardActivity)

                            try {
                                withContext(Dispatchers.IO) {
                                    dao.updateLastLogin(System.currentTimeMillis())
                                    dao.updateBestStreakIfNeeded()
                                }
                            } catch (_: Exception) {
                            }

                            try {
                                val ctx = BadgeManager.buildContext(this@DashboardActivity)
                                val nouveaux = BadgeManager.evaluateAll(this@DashboardActivity, ctx)
                                if (nouveaux.isNotEmpty()) {
                                    afficherMiniNotifBadge(nouveaux.first())
                                }
                            } catch (_: Exception) {
                            }

                            afficherDerniersBadges()

                            if (
                                fadingCourse != null &&
                                gardenResponse != null &&
                                !hasShownGardenDialogueThisSession &&
                                !isFinishing &&
                                !isDestroyed
                            ) {
                                hasShownGardenDialogueThisSession = true
                                afficherGardenDialogue(
                                    response = gardenResponse,
                                    fadingLessonTitle = fadingCourse.displayTitle(),
                                    fadingCourse = fadingCourse
                                )
                            }
                        }
                    } else {
                        binding.tvCurrencyEclat.text = "0"
                        binding.tvCurrencyAmbroisie.text = "0"
                        binding.tvDayStreakHud.text = "0"
                        binding.tvWinStreakHud.text = "0"

                        lifecycleScope.launch {
                            if (
                                fadingCourse != null &&
                                gardenResponse != null &&
                                !hasShownGardenDialogueThisSession &&
                                !isFinishing &&
                                !isDestroyed
                            ) {
                                hasShownGardenDialogueThisSession = true
                                afficherGardenDialogue(
                                    response = gardenResponse,
                                    fadingLessonTitle = fadingCourse.displayTitle(),
                                    fadingCourse = fadingCourse
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("REVIZEUS", "DB Dashboard: ${e.message}")
            }
        }
    }

    private suspend fun updateDayStreak(
        profile: UserProfile,
        db: AppDatabase
    ) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayKey = formatter.format(System.currentTimeMillis())
        val previousKey = profile.lastLoginDayKey
        val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayKey = formatter.format(yesterdayCalendar.time)

        when {
            previousKey.isBlank() -> {
                profile.dayStreak = 1
            }
            previousKey == todayKey -> {
            }
            previousKey == yesterdayKey -> {
                profile.dayStreak += 1
            }
            else -> {
                profile.dayStreak = 1
            }
        }

        profile.bestDayStreak = maxOf(profile.bestDayStreak, profile.dayStreak)
        profile.lastLoginDayKey = todayKey

        profile.streak = profile.dayStreak
        profile.bestStreakEver = maxOf(profile.bestStreakEver, profile.dayStreak)

        db.iAristoteDao().updateUserProfile(profile)
    }

    private fun afficherGardenDialogue(
        response: GeminiManager.GodResponse,
        fadingLessonTitle: String,
        fadingCourse: CourseEntry
    ) {
        val dialogRoot = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_rpg_dialog)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val headerRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val portrait = android.widget.ImageView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(96), dp(96))
            setImageResource(R.drawable.avatar_demeter_dialog)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            contentDescription = null
        }
        headerRow.addView(portrait)

        val titleColumn = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dp(14)
            }
        }

        val title = android.widget.TextView(this).apply {
            text = "DÉMÉTER — Jardin du Savoir"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
        }
        titleColumn.addView(title)

        val subtitle = android.widget.TextView(this).apply {
            text = "Un savoir réclame ta révision : $fadingLessonTitle"
            setTextColor(Color.parseColor("#CCFFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(4), 0, 0)
        }
        titleColumn.addView(subtitle)

        headerRow.addView(titleColumn)
        dialogRoot.addView(headerRow)

        val messageLabel = android.widget.TextView(this).apply {
            text = "MESSAGE"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(16), 0, dp(8))
        }
        dialogRoot.addView(messageLabel)

        val message = android.widget.TextView(this).apply {
            text = ""
            setTextColor(Color.parseColor("#F5F5F5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setLineSpacing(dp(4).toFloat(), 1f)
        }
        dialogRoot.addView(message)

        val mnemo = android.widget.TextView(this).apply {
            text = "Mnémotechnique : ${response.mnemo}"
            setTextColor(Color.parseColor("#CCFFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(14), 0, 0)
        }
        dialogRoot.addView(mnemo)

        val action = android.widget.TextView(this).apply {
            text = "Action suggérée : ${response.suggestedAction}"
            setTextColor(Color.parseColor("#A5D6A7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(8), 0, 0)
        }
        dialogRoot.addView(action)

        val openLessonButton = android.widget.TextView(this).apply {
            text = "OUVRIR CE SAVOIR MAINTENANT"
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_temple_button)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
            isClickable = true
            isFocusable = true
        }
        dialogRoot.addView(openLessonButton)

        val confirmButton = android.widget.TextView(this).apply {
            text = "COMPRIS"
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_rpg_dialog)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
            isClickable = true
            isFocusable = true
        }
        dialogRoot.addView(confirmButton)

        val dialog = AlertDialog.Builder(
            this,
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )
            .setView(dialogRoot)
            .setCancelable(true)
            .create()

        openLessonButton.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            welcomeTypewriterJob?.cancel()
            dialog.dismiss()
            try {
                startActivity(
                    Intent(this, GodMatiereActivity::class.java)
                        .putExtra("MATIERE", fadingCourse.subject)
                        .putExtra("CANONICAL_SUBJECT", fadingCourse.subject)
                        .putExtra("OPEN_COURSE_ID", fadingCourse.id)
                )
            } catch (e: Exception) {
                Log.e("REVIZEUS", "Impossible d'ouvrir directement le savoir fanant : ${e.message}")
            }
        }

        confirmButton.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            welcomeTypewriterJob?.cancel()
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            welcomeTypewriterJob?.cancel()
        }

        try {
            SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
        } catch (_: Exception) {
        }

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rpg_dialog)

        welcomeTypewriterJob?.cancel()
        welcomeTypewriterJob = godAnim.typewriteSimple(
            scope = lifecycleScope,
            chibiView = portrait,
            textView = message,
            text = response.text,
            context = this
        )
    }

    private fun showMajPopup() {
        if (isFinishing || isDestroyed || isMajPopupShowing || hasShownMajPopupThisConnection) return

        val majText = try {
            assets.open("maj.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("REVIZEUS", "Impossible de lire assets/maj.txt : ${e.message}")
            return
        }

        isMajPopupShowing = true
        hasShownMajPopupThisConnection = true

        try {
            SoundManager.playSFX(this, R.raw.sfx_badge_unlock)
        } catch (_: Exception) {
        }

        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_rpg_dialog)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val title = android.widget.TextView(this).apply {
            text = "📜 PARCHEMIN DES MISES À JOUR"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
            typeface = try {
                resources.getFont(R.font.cinzel)
            } catch (_: Exception) {
                Typeface.DEFAULT_BOLD
            }
        }
        root.addView(title)

        val subtitle = android.widget.TextView(this).apply {
            text = "Les Chroniques de l'Olympe"
            setTextColor(Color.parseColor("#B8A56A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }
        root.addView(subtitle)

        val separator = View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(14)
                bottomMargin = dp(12)
            }
            setBackgroundColor(Color.parseColor("#55FFD700"))
        }
        root.addView(separator)

        val scroll = android.widget.ScrollView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        val patchNotes = android.widget.TextView(this).apply {
            text = formatDivinePatchNotes(majText)
            setTextColor(Color.parseColor("#EEEEEE"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setLineSpacing(dp(4).toFloat(), 1f)
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        scroll.addView(patchNotes)
        root.addView(scroll)

        val btnFrame = android.widget.FrameLayout(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                topMargin = dp(14)
            }
        }

        btnFrame.addView(android.widget.ImageView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            try {
                setImageResource(R.drawable.bg_temple_button)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#1A1A2E"))
            }
            scaleType = android.widget.ImageView.ScaleType.FIT_XY
        })

        btnFrame.addView(android.widget.ImageView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            try {
                setImageResource(R.drawable.bg_textelayout)
            } catch (_: Exception) {
            }
            scaleType = android.widget.ImageView.ScaleType.FIT_XY
            alpha = 0.30f
        })

        val close = android.widget.TextView(this).apply {
            text = "FERMER"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = try {
                resources.getFont(R.font.cinzel)
            } catch (_: Exception) {
                Typeface.DEFAULT_BOLD
            }
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = true
            isFocusable = true
        }

        btnFrame.addView(close)
        root.addView(btnFrame)

        val dialog = AlertDialog.Builder(
            this,
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )
            .setView(root)
            .setCancelable(false)
            .create()

        close.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            isMajPopupShowing = false
        }

        dialog.show()

        try {
            dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rpg_dialog)
        } catch (_: Exception) {
        }
    }

    private fun formatDivinePatchNotes(rawText: String): CharSequence {
        val builder = SpannableStringBuilder()
        val lines = rawText.split("\n")
        val gold = Color.parseColor("#FFD700")

        for (line in lines) {
            val start = builder.length
            builder.append(line)
            val end = builder.length

            val isTitle =
                line.startsWith("⚡") ||
                    line.startsWith("🏛") ||
                    line.startsWith("⭐") ||
                    line.startsWith("🎶") ||
                    line.startsWith("📜") ||
                    line.startsWith("👁") ||
                    line.startsWith("🌿") ||
                    line.startsWith("⚔") ||
                    line.startsWith("⚒") ||
                    line.startsWith("💎") ||
                    line.startsWith("🎒") ||
                    line.startsWith("🌋") ||
                    line.startsWith("✨") ||
                    line.startsWith("🧙") ||
                    line.startsWith("🎭") ||
                    line.startsWith("🎬") ||
                    line.startsWith("🌀") ||
                    line.startsWith("⚙") ||
                    line.startsWith("🔐") ||
                    line.startsWith("👨‍👩‍👦") ||
                    line.startsWith("🌌")

            if (isTitle) {
                builder.setSpan(
                    ForegroundColorSpan(gold),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            builder.append("\n")
        }

        return builder
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    /**
     * Construit un message vivant pour Zeus dans le popup de retour Dashboard.
     *
     * IMPORTANT :
     * - aucun changement graphique ;
     * - uniquement le contenu du texte varie ;
     * - plusieurs variantes pour éviter l'effet figé ;
     * - légère adaptation au contexte joueur déjà disponible localement ;
     * - aucune dépendance externe pour éviter de recasser le build.
     */
    private fun buildZeusDashboardExitMessage(): String {
        val prefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        val rawMood = prefs.getString("CURRENT_MOOD", "JOYEUX") ?: "JOYEUX"
        val mood = rawMood
            .trim()
            .uppercase()
            .replace("É", "E")
            .replace("È", "E")
            .replace("Ê", "E")
            .replace("À", "A")
            .replace("Â", "A")
            .replace("Ù", "U")
            .replace("Û", "U")
            .replace("Î", "I")
            .replace("Ï", "I")
            .replace("Ô", "O")
            .replace("Ö", "O")
            .replace("Ç", "C")

        val pseudo = binding.tvHeroName.text?.toString()?.trim().orEmpty().ifBlank { "héros" }
        val levelText = binding.tvLevel.text?.toString()?.trim().orEmpty()
        val levelNumber = levelText
            .replace("LVL", "", ignoreCase = true)
            .trim()
            .toIntOrNull() ?: 1

        val streak = binding.tvDayStreakHud.text?.toString()?.trim()?.toIntOrNull() ?: 0

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val periodTag = when {
            hour in 5..11 -> "morning"
            hour in 12..17 -> "day"
            hour in 18..22 -> "evening"
            else -> "night"
        }

        val moodMessages = when (mood) {
            "FATIGUE" -> listOf(
                "$pseudo, même la foudre sait quand reprendre son souffle. Où souhaites-tu aller ?",
                "Ton regard se trouble un peu, $pseudo. Choisis ta prochaine route sans défier Chronos inutilement.",
                "Je vois la fatigue sur ton front, $pseudo. Dis-moi si tu restes, si tu changes de héros, ou si tu quittes l'Olympe."
            )

            "STRESSE" -> listOf(
                "$pseudo, cesse d'affronter cent tempêtes à la fois. Choisis calmement ta prochaine destination.",
                "L'Olympe tient encore debout, $pseudo. Respire, puis décide de la route que tu veux suivre.",
                "Même Zeus préfère une décision claire à une panique glorieuse. Que choisis-tu, $pseudo ?"
            )

            else -> listOf(
                "$pseudo, ton entraînement n'est pas terminé. Où souhaites-tu aller ?",
                "Le tonnerre gronde encore au-dessus du Panthéon, $pseudo. Quelle voie choisis-tu ?",
                "Tu quittes déjà cette salle sacrée, $pseudo ? Très bien. Indique simplement ta destination.",
                "L'Olympe ne s'écroulera pas pendant ton absence... enfin, probablement. Où vas-tu, $pseudo ?"
            )
        }

        val levelMessages = if (levelNumber >= 15) {
            listOf(
                "Tu as déjà la prestance d'un héros confirmé, $pseudo. Où diriges-tu maintenant ta foudre ?",
                "Un héros de ton rang ne sort pas sans décision nette. Quelle est ta prochaine route, $pseudo ?"
            )
        } else {
            listOf(
                "Chaque choix forge encore ton ascension, $pseudo. Où veux-tu poursuivre ton chemin ?",
                "Tu grandis dans l'Olympe, $pseudo. Dis-moi quelle porte tu veux franchir à présent."
            )
        }

        val streakMessages = if (streak >= 7) {
            listOf(
                "Ta constance force presque mon respect, $pseudo. Ne gâche pas ton élan : où vas-tu ?",
                "Une telle flamme de régularité mérite une décision digne. Que choisis-tu, $pseudo ?"
            )
        } else {
            emptyList()
        }

        val periodMessages = when (periodTag) {
            "morning" -> listOf(
                "Le jour s'élève sur l'Olympe, $pseudo. Où porteras-tu ta volonté maintenant ?"
            )

            "evening" -> listOf(
                "Le ciel du soir observe encore tes choix, $pseudo. Quelle route prends-tu ?"
            )

            "night" -> listOf(
                "Même à cette heure, l'Olympe ne dort pas tout à fait. Que décides-tu, $pseudo ?",
                "Chronos veille tard aujourd'hui... et toi aussi, manifestement. Où souhaites-tu aller, $pseudo ?"
            )

            else -> emptyList()
        }

        val allMessages = buildList {
            addAll(moodMessages)
            addAll(levelMessages)
            addAll(streakMessages)
            addAll(periodMessages)
        }

        return if (allMessages.isNotEmpty()) {
            allMessages.random()
        } else {
            "$pseudo, ton entraînement n'est pas terminé. Où souhaites-tu aller ?"
        }
    }

    /**
     * Construit un texte vivant pour les popups Prométhée.
     *
     * Le visuel reste identique ; seul le texte est enrichi.
     */
    private fun buildPrometheusMessage(baseText: String): String {
        val prefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        val rawMood = prefs.getString("CURRENT_MOOD", "JOYEUX") ?: "JOYEUX"
        val mood = rawMood
            .trim()
            .uppercase()
            .replace("É", "E")
            .replace("È", "E")
            .replace("Ê", "E")
            .replace("À", "A")
            .replace("Â", "A")
            .replace("Ù", "U")
            .replace("Û", "U")
            .replace("Î", "I")
            .replace("Ï", "I")
            .replace("Ô", "O")
            .replace("Ö", "O")
            .replace("Ç", "C")

        val intro = when (mood) {
            "FATIGUE" -> listOf(
                "Prends ton temps, héros.",
                "Respire un instant, mortel studieux.",
                "Allons doucement, l'Olympe ne s'effondrera pas dans la minute."
            )
            "STRESSE" -> listOf(
                "Rien ne sert de courir dans tous les temples à la fois.",
                "Calmons le feu avant de lire le parchemin.",
                "Même les titans comprenaient mieux après une respiration."
            )
            else -> listOf(
                "Écoute bien, héros.",
                "Petit éclair de clarté, signé Prométhée.",
                "Voici de quoi éviter de te perdre dans les couloirs de l'Olympe."
            )
        }.random()

        return "$intro $baseText"
    }

    /**
     * Construit une petite phrase d'accompagnement vivante pour Prométhée.
     */
    private fun buildPrometheusComment(baseComment: String): String {
        val extras = listOf(
            "Oui, je sais, j'aurais pu graver cela sur une tablette divine plus courte.",
            "Promis, même Hermès lirait ça sans se tromper de parchemin.",
            "C'est la version claire. La version d'Héphaïstos avait douze schémas et trois leviers.",
            "Même Zeus valide ce résumé. Enfin... il n'a pas dit non.",
            "Si tu oublies, Prométhée ne juge pas. Il soupire seulement avec élégance."
        )
        return "$baseComment ${extras.random()}"
    }

    private fun buildPrometheusTitleHint(title: String): String {
        val variants = listOf(
            "Guide utile du jour",
            "Petit éclair de clarté",
            "Note de Prométhée",
            "Rappel bienveillant",
            "Conseil de survie olympien"
        )
        return "$title — ${variants.random()}"
    }

    private fun setupHeroShortcuts() {
        binding.imgAvatarHero.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            startActivity(Intent(this, HeroProfileActivity::class.java))
        }

        binding.btnBadgeBookDash.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            startActivity(Intent(this, BadgeBookActivity::class.java))
        }

        binding.btnInventoryDash.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            runFirstTimeFeatureThen("inventory") {
                startActivity(Intent(this, InventoryActivity::class.java))
            }
        }

        binding.chipForge.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_thunder_confirm)
            } catch (_: Exception) {
            }
            runFirstTimeFeatureThen("forge") {
                try {
                    startActivity(Intent(this, ForgeActivity::class.java))
                } catch (e: Exception) {
                    Log.e("REVIZEUS", "ForgeActivity non déclarée dans le Manifest : ${e.message}")
                }
            }
        }
    }

    private fun setupHudTooltips() {
        binding.chipEclat.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
            afficherPopupPrometheus(
                titre = "Éclats de Savoir",
                icone = R.drawable.ic_currency_eclat_savoir,
                accent = "#FFE27A",
                texte = "Les Éclats de Savoir sont la monnaie principale de l'Olympe. " +
                    "Tu en reçois après chaque quiz, en proportion de ton score. " +
                    "Plus tu réponds juste et rapidement, plus tu en accumules.",
                comment = "Fais des quiz → +Éclats selon ton score."
            )
        }

        binding.chipAmbroisie.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
            afficherPopupPrometheus(
                titre = "Ambroisie",
                icone = R.drawable.ic_currency_ambroisie,
                accent = "#F1DAFF",
                texte = "L'Ambroisie est la boisson sacrée des dieux — rare et précieuse. " +
                    "Elle récompense les performances d'exception : score parfait, " +
                    "défi d'Arès relevé, ou streak légendaire maintenu.",
                comment = "Score parfait ou défi Arès réussi → +Ambroisie."
            )
        }

        binding.chipDayStreak.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
            afficherPopupPrometheus(
                titre = "Flamme de Constance",
                icone = R.drawable.ic_day_streak_olympian,
                accent = "#FFB35A",
                texte = "La Flamme brûle chaque jour où tu reviens t'entraîner. " +
                    "Enchaîne les jours consécutifs pour la faire grandir. " +
                    "Si tu manques une journée… elle s'éteint et repart de zéro.",
                comment = "Reviens chaque jour → +1. Manque un jour → retour à zéro."
            )
        }

        binding.chipWinStreak.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
            afficherPopupPrometheus(
                titre = "Étincelle de Victoire",
                icone = R.drawable.ic_win_streak_olympian,
                accent = "#FFE27A",
                texte = "L'Étincelle compte tes quiz réussis à la suite. " +
                    "Enchaîne les bonnes performances pour la faire monter. " +
                    "Un seul quiz raté et tout s'arrête — c'est le chemin des champions.",
                comment = "Quiz réussi → +1 Étincelle. Quiz raté → retour à zéro."
            )
        }
    }

    private fun afficherPopupPrometheus(
        titre: String,
        icone: Int,
        accent: String,
        texte: String,
        comment: String
    ) {
        if (isFinishing || isDestroyed) return

        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#12111E"))
            }
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val headerRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val prometheusPortrait = android.widget.ImageView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(68), dp(68))
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            val resId = resources.getIdentifier("avatar_promethee_dialog", "drawable", packageName)
            if (resId != 0) {
                setImageResource(resId)
            } else {
                val fb = resources.getIdentifier("ic_zeus_chibi", "drawable", packageName)
                if (fb != 0) setImageResource(fb)
            }
        }
        headerRow.addView(prometheusPortrait)

        val col = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { marginStart = dp(14) }
        }

        val titreRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titreRow.addView(android.widget.ImageView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(20), dp(20))
                .apply { marginEnd = dp(6) }
            try {
                setImageResource(icone)
            } catch (_: Exception) {
            }
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        })
        titreRow.addView(android.widget.TextView(this).apply {
            text = buildPrometheusTitleHint(titre)
            setTextColor(Color.parseColor(accent))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = try {
                resources.getFont(R.font.cinzel)
            } catch (_: Exception) {
                Typeface.DEFAULT_BOLD
            }
        })
        col.addView(titreRow)
        col.addView(android.widget.TextView(this).apply {
            text = "PROMÉTHÉE — Guide des Mortels"
            setTextColor(Color.parseColor("#776655"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(3) }
        })
        headerRow.addView(col)
        root.addView(headerRow)

        root.addView(View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
            setBackgroundColor(Color.parseColor("#33${accent.removePrefix("#")}"))
        })

        val messageView = android.widget.TextView(this).apply {
            text = ""
            setTextColor(Color.parseColor("#DEDEDE"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLineSpacing(dp(2).toFloat(), 1f)
        }
        root.addView(messageView)

        val commentContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#1A1A2E"))
            }
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

        commentContainer.addView(android.widget.TextView(this).apply {
            text = "⚡  "
            setTextColor(Color.parseColor(accent))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        })

        val commentView = android.widget.TextView(this).apply {
            text = ""
            setTextColor(Color.parseColor("#AAAACC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        commentContainer.addView(commentView)
        root.addView(commentContainer)

        val btnFrame = android.widget.FrameLayout(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
            ).apply { topMargin = dp(14) }
        }
        btnFrame.addView(android.widget.ImageView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            try {
                setImageResource(R.drawable.bg_temple_button)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#1A1A2E"))
            }
            scaleType = android.widget.ImageView.ScaleType.FIT_XY
        })
        btnFrame.addView(android.widget.ImageView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            try {
                setImageResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
            }
            scaleType = android.widget.ImageView.ScaleType.FIT_XY
            alpha = 0.30f
        })
        val btnLabel = android.widget.TextView(this).apply {
            text = "COMPRIS"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            typeface = try {
                resources.getFont(R.font.cinzel)
            } catch (_: Exception) {
                Typeface.DEFAULT_BOLD
            }
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = true
            isFocusable = true
        }
        btnFrame.addView(btnLabel)
        root.addView(btnFrame)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(root)
            .setCancelable(true)
            .create()

        val finalMessage = buildPrometheusMessage(texte)
        val finalComment = buildPrometheusComment(comment)

        messageView.setOnClickListener {
            try {
                prometheusTypewriterJob?.cancel()
                messageView.text = finalMessage
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
                messageView.text = finalMessage
            }
        }

        commentView.setOnClickListener {
            try {
                prometheusTypewriterJob?.cancel()
                commentView.text = finalComment
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
                commentView.text = finalComment
            }
        }

        btnLabel.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            prometheusTypewriterJob?.cancel()
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            prometheusTypewriterJob?.cancel()
        }

        dialog.show()
        try {
            dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rpg_dialog)
        } catch (_: Exception) {
        }

        // Premier passage : texte principal.
        prometheusTypewriterJob?.cancel()
        prometheusTypewriterJob = godAnim.typewriteSimple(
            scope = lifecycleScope,
            chibiView = prometheusPortrait,
            textView = messageView,
            text = finalMessage,
            context = this
        )

        // Deuxième passage : commentaire / astuce, avec petit décalage.
        lifecycleScope.launch {
            try {
                kotlinx.coroutines.delay(500L)
                godAnim.typewriteSimple(
                    scope = lifecycleScope,
                    chibiView = prometheusPortrait,
                    textView = commentView,
                    text = finalComment,
                    context = this@DashboardActivity
                )
            } catch (_: Exception) {
                commentView.text = finalComment
            }
        }
    }

    private fun runFirstTimeFeatureThen(featureId: String, action: () -> Unit) {
        try {
            TutorialManager.runHeroFirstTimeFeature(this, featureId) {
                if (!isFinishing && !isDestroyed) {
                    action()
                }
            }
        } catch (_: Exception) {
            action()
        }
    }

    private fun showZeusExitConfirmation(
        title: String,
        message: String,
        confirmLabel: String,
        dialogToDismiss: AlertDialog,
        onConfirm: () -> Unit
    ) {
        try {
            DialogRPGManager.showConfirmation(
                activity = this,
                godId = "zeus",
                title = title,
                message = message,
                confirmLabel = confirmLabel,
                cancelLabel = "RETOUR AU CHOIX",
                onConfirm = {
                    try { dialogToDismiss.dismiss() } catch (_: Exception) {}
                    onConfirm()
                },
                onCancel = {
                    try {
                        if (!dialogToDismiss.isShowing && !isFinishing && !isDestroyed) {
                            dialogToDismiss.show()
                        }
                    } catch (_: Exception) {
                    }
                }
            )
        } catch (_: Exception) {
            try { dialogToDismiss.dismiss() } catch (_: Exception) {}
            onConfirm()
        }
    }

    private fun setupMenu() {
        binding.btnNewCourse.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_orb_open)
            } catch (_: Exception) {
            }
            runFirstTimeFeatureThen("oracle") {
                try {
                    startActivity(Intent(this, OracleActivity::class.java))
                } catch (_: Exception) {
                }
            }
        }

        binding.btnDailyTraining.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
            runFirstTimeFeatureThen("training") {
                try {
                    startActivity(Intent(this, TrainingSelectActivity::class.java))
                } catch (_: Exception) {
                }
            }
        }

        binding.btnLibrary.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
            runFirstTimeFeatureThen("library") {
                try {
                    startActivity(Intent(this, SavoirActivity::class.java))
                } catch (_: Exception) {
                }
            }
        }

        binding.btnMoodDash.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_mood_happy)
            } catch (_: Exception) {
            }
            runFirstTimeFeatureThen("mood") {
                try {
                    startActivity(Intent(this, MoodActivity::class.java))
                } catch (_: Exception) {
                }
            }
        }

        binding.btnAdventureLocked.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }

            try {
                DialogRPGManager.showInfo(
                    activity = this,
                    godId = "zeus",
                    title = "Mode Aventure scellé",
                    message = "Héros... j’ai contemplé les astres, interrogé les éclairs, et même réveillé Prométhée de sa sieste sacrée : le Mode Aventure est encore en conceptualisation cosmique. En clair, l’Olympe cuisine encore la carte du destin... et pour l’instant, même le Chaos n’a pas reçu la version finale."
                )
            } catch (_: Exception) {
            }
        }

        binding.btnSettings.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_settings_tab)
            } catch (_: Exception) {
            }
            try {
                startActivity(Intent(this, SettingsActivity::class.java))
            } catch (_: Exception) {
            }
        }

        binding.btnDebugML.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_settings_tab)
            } catch (_: Exception) {
            }
            try {
                startActivity(Intent(this, DebugAnalyticsActivity::class.java))
            } catch (e: Exception) {
                Log.e("REVIZEUS", "DebugAnalyticsActivity non déclarée dans le Manifest : ${e.message}")
            }
        }
    }

    private fun setupPrometheusLongPresses() {
        binding.chipForge.setOnLongClickListener {
            afficherPopupPrometheus(
                titre = "Forge d'Héphaïstos",
                icone = R.drawable.ic_forge_hammer,
                accent = "#FF8C00",
                texte = "La Forge transforme tes ressources en objets utiles : bonus, artefacts et outils pour la progression du héros.",
                comment = "Appui court : ouvrir la Forge. Appui long : explication."
            )
            true
        }

        binding.btnInventoryDash.setOnLongClickListener {
            afficherPopupPrometheus(
                titre = "Inventaire",
                icone = R.drawable.ic_inventory_bag_olympian,
                accent = "#87CEEB",
                texte = "Ton Inventaire rassemble les objets gagnés, forgés ou offerts par les dieux. Certains servent au style, d'autres à la progression.",
                comment = "Appui court : ouvrir l'Inventaire."
            )
            true
        }

        binding.btnBadgeBookDash.setOnLongClickListener {
            afficherPopupPrometheus(
                titre = "Livre des badges",
                icone = R.drawable.ic_badge_book_olympian,
                accent = "#FFD700",
                texte = "Les badges immortalisent tes exploits : régularité, score, maîtrise ou exploits rares dans l'Olympe.",
                comment = "Appui court : ouvrir le Livre des badges."
            )
            true
        }

        binding.btnMoodDash.setOnLongClickListener {
            afficherPopupPrometheus(
                titre = "Humeur du héros",
                icone = R.drawable.ic_mood_oracle,
                accent = "#40E0D0",
                texte = "L'humeur influence la musique, l'ambiance, et la manière dont l'IA adapte ses résumés, ses questions et ses corrections.",
                comment = "Appui court : changer l'humeur."
            )
            true
        }

        binding.btnAdventureLocked.setOnLongClickListener {
            afficherPopupPrometheus(
                titre = "Mode Aventure",
                icone = R.drawable.ic_aventure_locked,
                accent = "#FFBF00",
                texte = "Le Mode Aventure est encore en conceptualisation cosmique. Les dieux tracent la carte, le Chaos râle dans son coin, et les temples attendent encore leur destin final.",
                comment = "Appui court : annonce divine de fermeture temporaire."
            )
            true
        }

        binding.btnNewCourse.setOnLongClickListener {
            afficherPopupPrometheus(
                titre = "Oracle",
                icone = R.drawable.ic_menu_camera,
                accent = "#FFD700",
                texte = "L'Oracle transforme tes cours, tes images ou tes demandes en résumé clair puis en quiz généré par les dieux.",
                comment = "Appui court : ouvrir l'Oracle."
            )
            true
        }

        binding.btnLibrary.setOnLongClickListener {
            afficherPopupPrometheus(
                titre = "Bibliothèque des savoirs",
                icone = R.drawable.ic_library_scroll,
                accent = "#87CEEB",
                texte = "La Bibliothèque conserve tes parchemins déjà forgés, pour révision, lecture divine et entraînement ultérieur.",
                comment = "Appui court : ouvrir la Bibliothèque."
            )
            true
        }

        binding.btnDailyTraining.setOnLongClickListener {
            afficherPopupPrometheus(
                titre = "Arène d'entraînement",
                icone = R.drawable.ic_ultime_crown,
                accent = "#DAA520",
                texte = "L'Arène d'entraînement te permet de rejouer, renforcer ou tester ce que tu as déjà appris, matière par matière ou en ultime.",
                comment = "Appui court : ouvrir l'Arène."
            )
            true
        }
    }

    private fun setupDashboardBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                afficherDialogueRetourDashboard()
            }
        })
    }

    // ══════════════════════════════════════════════════════════════
    // POP-UP DE SORTIE ZEUS (PROGRAMMATIQUE)
    // Le design visuel reste strictement intact.
    // Le texte devient vivant + affichage lettre par lettre + son.
    // ══════════════════════════════════════════════════════════════
    private fun afficherDialogueRetourDashboard() {
        if (isFinishing || isDestroyed) return

        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#12111E"))
            }
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val headerRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val zeusPortrait = android.widget.ImageView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(68), dp(68))
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER

            val resId = resources.getIdentifier("avatar_zeus_dialog", "drawable", packageName)
            if (resId != 0) {
                setImageResource(resId)
            } else {
                val fb = resources.getIdentifier("ic_zeus_chibi", "drawable", packageName)
                if (fb != 0) setImageResource(fb)
            }
        }
        headerRow.addView(zeusPortrait)

        val col = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { marginStart = dp(14) }
        }

        col.addView(android.widget.TextView(this).apply {
            text = "Zeus"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = try {
                resources.getFont(R.font.cinzel)
            } catch (_: Exception) {
                Typeface.DEFAULT_BOLD
            }
        })

        col.addView(android.widget.TextView(this).apply {
            text = "Maître de l'Olympe"
            setTextColor(Color.parseColor("#776655"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        })

        headerRow.addView(col)
        root.addView(headerRow)

        root.addView(View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
            setBackgroundColor(Color.parseColor("#33FFD700"))
        })

        val messageView = android.widget.TextView(this).apply {
            text = ""
            setTextColor(Color.parseColor("#DEDEDE"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setLineSpacing(dp(2).toFloat(), 1f)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }
        root.addView(messageView)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(root)
            .setCancelable(true)
            .create()

        fun createButton(label: String, colorHex: String, action: () -> Unit) {
            val btnFrame = android.widget.FrameLayout(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(48)
                ).apply {
                    bottomMargin = dp(8)
                }
            }
            btnFrame.addView(android.widget.ImageView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                try {
                    setImageResource(R.drawable.bg_temple_button)
                } catch (_: Exception) {
                    setBackgroundColor(Color.parseColor("#1A1A2E"))
                }
                scaleType = android.widget.ImageView.ScaleType.FIT_XY
            })
            btnFrame.addView(android.widget.ImageView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                try {
                    setImageResource(R.drawable.bg_rpg_dialog)
                } catch (_: Exception) {
                }
                scaleType = android.widget.ImageView.ScaleType.FIT_XY
                alpha = 0.30f
            })
            val btnLabel = android.widget.TextView(this).apply {
                text = label
                setTextColor(Color.parseColor(colorHex))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
                typeface = try {
                    resources.getFont(R.font.cinzel)
                } catch (_: Exception) {
                    Typeface.DEFAULT_BOLD
                }
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                isClickable = true
                isFocusable = true
            }
            btnLabel.setOnClickListener { action() }
            btnFrame.addView(btnLabel)
            root.addView(btnFrame)
        }

        createButton("CHANGER DE HÉROS", "#87CEEB") {
            try {
                SoundManager.playSFX(this, R.raw.sfx_transition_thunder)
            } catch (_: Exception) {
            }
            showZeusExitConfirmation(
                title = "Retour au Temple des Héros",
                message = "Si tu quittes ce panthéon maintenant, ta progression reste gravée, mais tu reviendras au choix des héros pour changer de destin. Es-tu certain de vouloir quitter ce héros pour l'instant ?",
                confirmLabel = "CHANGER DE HÉROS",
                dialogToDismiss = dialog
            ) {
                welcomeTypewriterJob?.cancel()
                startActivity(Intent(this, AccountSelectActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                finish()
            }
        }

        createButton("REVENIR À L'ÉCRAN TITRE", "#DAA520") {
            try {
                SoundManager.playSFX(this, R.raw.sfx_transition_thunder)
            } catch (_: Exception) {
            }
            showZeusExitConfirmation(
                title = "Retour à l'Écran Titre",
                message = "En revenant à l'écran titre, tu quittes l'Olympe actif de ce héros. Rien n'est perdu, mais ton aventure en cours s'interrompt ici et tu devras réinvoker ton chemin depuis le portail principal.",
                confirmLabel = "RETOUR TITRE",
                dialogToDismiss = dialog
            ) {
                welcomeTypewriterJob?.cancel()
                startActivity(Intent(this, TitleScreenActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                finishAffinity()
            }
        }

        createButton("QUITTER L'OLYMPE", "#FF6B6B") {
            try {
                SoundManager.playSFX(this, R.raw.sfx_transition_thunder)
            } catch (_: Exception) {
            }
            showZeusExitConfirmation(
                title = "Quitter l'Olympe",
                message = "Si tu fermes maintenant les portes de l'Olympe, ta progression restera sauvegardée, mais ta session divine s'achèvera immédiatement. Veux-tu vraiment éteindre le sanctuaire pour l'instant ?",
                confirmLabel = "QUITTER",
                dialogToDismiss = dialog
            ) {
                welcomeTypewriterJob?.cancel()
                try { dialog.dismiss() } catch (_: Exception) {}
                finishAffinity()
            }
        }

        createButton("RESTER ICI", "#A5D6A7") {
            try {
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
            }
            welcomeTypewriterJob?.cancel()
            dialog.dismiss()
        }

        val zeusMessage = buildZeusDashboardExitMessage()
        messageView.text = zeusMessage

        messageView.setOnClickListener {
            try {
                welcomeTypewriterJob?.cancel()
                messageView.text = zeusMessage
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
                messageView.text = zeusMessage
            }
        }

        dialog.setOnDismissListener {
            welcomeTypewriterJob?.cancel()
        }

        dialog.setOnShowListener {
            try {
                messageView.text = ""
                welcomeTypewriterJob?.cancel()
                welcomeTypewriterJob = godAnim.typewriteSimple(
                    scope = lifecycleScope,
                    chibiView = zeusPortrait,
                    textView = messageView,
                    text = zeusMessage,
                    context = this
                )
            } catch (_: Exception) {
                messageView.text = zeusMessage
            }
        }

        dialog.show()
        try {
            dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rpg_dialog)
        } catch (_: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        welcomeTypewriterJob?.cancel()
        prometheusTypewriterJob?.cancel()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@DashboardActivity)
                val profile = db.iAristoteDao().getUserProfile()
                if (profile != null) {
                    val coursCount = db.iAristoteDao().countCourses()
                    val uid = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
                        .getString("FIREBASE_UID", "") ?: ""
                    if (uid.isNotBlank()) {
                        AccountRegistry.updateCacheFromProfile(
                            context = this@DashboardActivity,
                            uid = uid,
                            profile = profile,
                            totalCoursScanned = coursCount
                        )
                        Log.d(
                            "REVIZEUS",
                            "Cache sauvegardé: level=${profile.level}, eclats=${profile.eclatsSavoir}, savoirs=$coursCount"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("REVIZEUS", "Erreur sauvegarde cache: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        welcomeTypewriterJob?.cancel()
        prometheusTypewriterJob?.cancel()

        try {
            backgroundPlayer?.release()
        } catch (_: Exception) {
        }
        backgroundPlayer = null
    }
}

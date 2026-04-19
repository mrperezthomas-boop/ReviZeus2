package com.revizeus.app

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.core.XpCalculator
import com.revizeus.app.databinding.ActivityHeroProfileBinding
import com.revizeus.app.models.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * HeroProfileActivity — RéviZeus
 *
 * Correctif visuel demandé :
 * - avatar réellement affiché
 * - suppression de la grille de badges sur cet écran
 * - boutons dédiés Livre des Badges + Sac dans la zone héros
 * - cartes de stats plus cohérentes avec la DA
 *
 * AJOUT AAA :
 * - popup de succès en haut à droite, style notification console
 * - son sfx_succes avec fallback sécurisé si la ressource n'existe pas encore
 * - aspiration visuelle du badge vers le Livre des Badges
 * - pulse du Livre à l'impact
 * * ══════════════════════════════════════════════════════════════════
 * CORRECTIFS ARCHITECTE SUPRÊME (Mise à jour) :
 * ✅ Rafraîchissement en temps réel du profil dans onResume()
 * ✅ Fix du "Total XP = 0" en forçant l'affichage depuis profil.xp
 * ══════════════════════════════════════════════════════════════════
 */
class HeroProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityHeroProfileBinding

    private var bgAnim: AnimatedBackgroundHelper? = null
    private var olympianParticlesView: OlympianParticlesView? = null

    /**
     * Évite d'empiler plusieurs fermetures automatiques si plusieurs badges
     * sont évalués dans un laps de temps court.
     */
    private var notificationDismissRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        binding = ActivityHeroProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        installerAmbianceOlympienne()
        preparerPopupSucces()

        binding.btnBackProfile.setOnClickListener {
            try { SoundManager.playSFX(this, R.raw.sfx_avatar_scroll) } catch (_: Exception) {}
            finish()
        }

        binding.btnBadgeBook.setOnClickListener {
            try { SoundManager.playSFX(this, R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            startActivity(Intent(this, BadgeBookActivity::class.java))
        }

        binding.btnInventory.setOnClickListener {
            try { SoundManager.playSFX(this, R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            startActivity(Intent(this, InventoryActivity::class.java))
        }

        chargerProfil()
        evaluerEtNotifier()
    }

    override fun onResume() {
        super.onResume()

        bgAnim?.start(
            accentColor = Color.parseColor("#FFD700"),
            mode = OlympianParticlesView.ParticleMode.TEMPLE
        )

        // ══════════════════════════════════════════════════════════
        // AJOUT ARCHITECTE : ACTUALISATION TEMPS RÉEL
        // Force le rechargement des données de la DB quand on revient
        // sur l'écran pour éviter que l'XP et les monnaies ne se figent.
        // ══════════════════════════════════════════════════════════
        chargerProfil()

        lifecycleScope.launch {
            val ctx = BadgeManager.buildContext(
                applicationContext,
                hasVisitedProfile = true
            )
            BadgeManager.evaluateAll(applicationContext, ctx)
        }
    }

    override fun onPause() {
        super.onPause()
        bgAnim?.stop()
        binding.layoutNotifBadge.removeCallbacks(notificationDismissRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        bgAnim?.stop()
        binding.layoutNotifBadge.removeCallbacks(notificationDismissRunnable)
    }

    private fun installerAmbianceOlympienne() {
        val root = binding.root as? ViewGroup ?: return

        olympianParticlesView = OlympianParticlesView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = false
            isFocusable = false
        }

        root.addView(olympianParticlesView, 0)

        bgAnim = AnimatedBackgroundHelper(
            targetView = binding.root,
            particlesView = olympianParticlesView
        )
    }

    /**
     * Prépare l'état initial de la popup de succès.
     * On la garde hors écran à droite pour obtenir un vrai slide-in.
     */
    private fun preparerPopupSucces() {
        binding.layoutNotifBadge.visibility = View.GONE
        binding.layoutNotifBadge.alpha = 0f

        binding.layoutNotifBadge.post {
            binding.layoutNotifBadge.translationX = binding.layoutNotifBadge.width.toFloat() + dp(24)
            binding.layoutNotifBadge.translationY = 0f
        }

        binding.btnNotifClose.setOnClickListener {
            masquerNotification(immediate = false)
        }
    }

    private fun chargerProfil() {
        lifecycleScope.launch {
            val profil = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@HeroProfileActivity)
                    .iAristoteDao().getUserProfile()
            }
            val totalScans = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@HeroProfileActivity)
                    .iAristoteDao().countCourses()
            }
            val quizStats = lireStatsQuizComplets()

            if (profil == null) {
                afficherProfilVide()
            } else {
                afficherProfil(profil, totalScans, quizStats)
            }

            animerEntreeStagger()
        }
    }

    private fun afficherProfilVide() {
        binding.tvProfileName.text = "HÉROS INCONNU"
        binding.tvProfileRang.text = "Mortel"
        binding.tvProfileLevel.text = "LVL 1"
        binding.tvProfileXp.text = "0 / ${XpCalculator.xpThresholdForLevel(1)} XP"
        binding.tvProfileDayStreak.text = "0"
        binding.tvProfileBestDayStreak.text = "0"
        binding.tvProfileWinStreak.text = "0"
        binding.tvProfileBestWinStreak.text = "0"
        binding.tvProfileTotalXp.text = "0"
        binding.tvProfileQuizCount.text = "0"
        binding.tvProfileScanCount.text = "0"
        binding.tvProfileEclats.text = "0"
        binding.tvProfileAmbroisie.text = "0"
        binding.pbProfileXp.progress = 0
        binding.tvBadgesResume.text = "0 / 0"
        binding.tvBadgePrestigeNom.text = "Aucun trophée encore"
        binding.tvBadgePrestigeRarete.text = ""
    }

    private fun afficherProfil(
        profil: com.revizeus.app.models.UserProfile,
        totalScans: Int,
        quizStats: Pair<Int, IntArray>
    ) {
        binding.tvProfileName.text = profil.pseudo.uppercase().ifBlank { "HÉROS" }
        binding.tvProfileRang.text = profil.rang

        if (profil.titleEquipped.isNotEmpty()) {
            binding.tvProfileTitre.text = profil.titleEquipped
            binding.tvProfileTitre.visibility = View.VISIBLE
        } else {
            binding.tvProfileTitre.visibility = View.GONE
        }

        binding.tvProfileClasse.text = profil.userClass

        /**
         * AVATAR — correction sécurité
         */
        try {
            val avatarResId = profil.getAvatarResId(this)
            if (avatarResId != 0) {
                binding.ivProfileAvatar.setImageResource(avatarResId)
            } else {
                binding.ivProfileAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        } catch (_: Exception) {
            binding.ivProfileAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        val accentColor = when {
            profil.level >= 50 -> "#FFD700"
            profil.level >= 20 -> "#9B59B6"
            profil.level >= 10 -> "#1E90FF"
            profil.level >= 5 -> "#C0C0C0"
            else -> "#806040"
        }
        binding.viewHeroAccent.setBackgroundColor(Color.parseColor(accentColor))

        binding.tvProfileLevel.text = "LVL ${profil.level}"
        binding.tvProfileXp.text = "${profil.xpDansNiveau} / ${profil.xpSeuilNiveau} XP"
        binding.pbProfileXp.progress = profil.progressionNiveauPct

        // ══════════════════════════════════════════════════════════
        // CORRECTIF ARCHITECTE : L'XP restait à 0 car totalXpEarned n'était
        // potentiellement pas bien hydraté. On utilise directement profil.xp
        // qui est la source de vérité absolue de l'entité Room.
        // ══════════════════════════════════════════════════════════
        binding.tvProfileTotalXp.text = formatNombre(profil.xp)

        binding.tvProfileDayStreak.text = profil.dayStreak.toString()
        binding.tvProfileBestDayStreak.text = profil.bestDayStreak.toString()
        binding.tvProfileWinStreak.text = profil.winStreak.toString()
        binding.tvProfileBestWinStreak.text = profil.bestWinStreak.toString()

        val totalQuizComplets = quizStats.first.coerceAtLeast(BadgeManager.getQuizCount(this))
        binding.tvProfileQuizCount.text = totalQuizComplets.toString()
        binding.tvProfileQuizCount.alpha = if (totalQuizComplets > 0) 1f else 0.92f
        binding.tvProfileQuizCount.setOnClickListener {
            afficherDetailQuizRpg(totalQuizComplets, quizStats.second)
        }
        binding.tvProfileQuizCount.setOnLongClickListener {
            afficherDetailQuizRpg(totalQuizComplets, quizStats.second)
            true
        }
        binding.tvProfileScanCount.text = totalScans.toString()

        binding.tvProfileEclats.text = formatNombre(profil.eclatsSavoir)
        binding.tvProfileAmbroisie.text = formatNombre(profil.ambroisie)

        val prestige = BadgeManager.getBadgePrestige(this)
        if (prestige != null) {
            binding.ivBadgePrestige.setImageResource(prestige.iconDrawable)
            binding.tvBadgePrestigeNom.text = prestige.nom
            binding.tvBadgePrestigeRarete.text = prestige.rarete.label
            binding.tvBadgePrestigeRarete.setTextColor(
                Color.parseColor(prestige.rarete.colorHex)
            )
        }

        val (debloques, total, pct) = BadgeManager.getResume(this)
        binding.tvBadgesResume.text = "$debloques / $total"
        animerBarre(binding.pbBadgesProgression, pct)
    }

    /**
     * Évalue les badges au retour sur la stèle du héros.
     * La première nouveauté trouvée déclenche la popup AAA + l'aspiration vers le Livre.
     */
    private fun evaluerEtNotifier() {
        lifecycleScope.launch {
            try {
                val ctx = BadgeManager.buildContext(applicationContext)
                val nouveaux = BadgeManager.evaluateAll(applicationContext, ctx)
                if (nouveaux.isNotEmpty()) {
                    afficherNotificationBadge(nouveaux.first())
                    delay(200)
                    chargerProfil()
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Popup type console en haut à droite.
     * Après le temps de lecture, le badge est aspiré vers le bouton Livre des Badges.
     */
    private fun afficherNotificationBadge(badge: BadgeDefinition) {
        jouerSfxSucces()

        with(binding) {
            ivNotifBadgeIcon.setImageResource(badge.iconDrawable)
            tvNotifBadgeNom.text = badge.nom
            tvNotifBadgeRarete.text = badge.rarete.label
            tvNotifBadgeRarete.setTextColor(Color.parseColor(badge.rarete.colorHex))

            if (badge.xpRecompense > 0) {
                tvNotifBadgeXp.text = "+${badge.xpRecompense} XP"
                tvNotifBadgeXp.visibility = View.VISIBLE
            } else {
                tvNotifBadgeXp.visibility = View.GONE
            }

            layoutNotifBadge.removeCallbacks(notificationDismissRunnable)
            layoutNotifBadge.visibility = View.VISIBLE
            layoutNotifBadge.alpha = 0f
            layoutNotifBadge.translationY = 0f
            layoutNotifBadge.translationX = layoutNotifBadge.width.toFloat() + dp(24)

            layoutNotifBadge.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(360L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            notificationDismissRunnable = Runnable {
                aspirerBadgeVersLivre(badge)
            }

            layoutNotifBadge.postDelayed(notificationDismissRunnable, 2300L)
        }
    }

    /**
     * Animation principale demandée :
     * le badge quitté la popup, vole en courbe, puis percute le Livre des Badges.
     */
    private fun aspirerBadgeVersLivre(badge: BadgeDefinition) {
        try {
            HudRewardAnimator.animateIconToTarget(
                activity = this,
                sourceView = binding.ivNotifBadgeIcon,
                targetView = binding.btnBadgeBook,
                drawableRes = badge.iconDrawable,
                durationMs = 820L
            ) {
                pulseBadgeBook()
            }
        } catch (_: Exception) {
            pulseBadgeBook()
        }

        masquerNotification(immediate = false)
    }

    /**
     * Fermeture de la popup.
     * immediate = true est gardé pour un repli sécurité si nécessaire.
     */
    private fun masquerNotification(immediate: Boolean) {
        binding.layoutNotifBadge.removeCallbacks(notificationDismissRunnable)

        if (immediate) {
            binding.layoutNotifBadge.visibility = View.GONE
            binding.layoutNotifBadge.alpha = 0f
            binding.layoutNotifBadge.translationX = binding.layoutNotifBadge.width.toFloat() + dp(24)
            return
        }

        binding.layoutNotifBadge.animate()
            .translationX(binding.layoutNotifBadge.width.toFloat() + dp(24))
            .alpha(0f)
            .setDuration(260L)
            .withEndAction {
                binding.layoutNotifBadge.visibility = View.GONE
            }
            .start()
    }

    /**
     * Feedback à l'impact sur le Livre des Badges.
     */
    private fun pulseBadgeBook() {
        binding.btnBadgeBook.animate()
            .scaleX(1.12f)
            .scaleY(1.12f)
            .setDuration(140L)
            .withEndAction {
                binding.btnBadgeBook.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180L)
                    .start()
            }
            .start()
    }

    /**
     * Essaye d'utiliser sfx_succes si la ressource existe.
     * Sinon, on retombe sur le son de confirmation déjà présent.
     */
    private fun jouerSfxSucces() {
        try {
            val successRes = resources.getIdentifier("sfx_succes", "raw", packageName)
            if (successRes != 0) {
                SoundManager.playSFX(this, successRes)
            } else {
                SoundManager.playSFX(this, R.raw.sfx_avatar_confirm)
            }
        } catch (_: Exception) {
            try { SoundManager.playSFX(this, R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
        }
    }


    private fun lireStatsQuizComplets(): Pair<Int, IntArray> {
        return try {
            val prefs = getSharedPreferences("RevizeusQuizStats", Context.MODE_PRIVATE)
            val distribution = IntArray(7) { star -> prefs.getInt("quiz_stars_" + star, 0) }
            val total = prefs.getInt("quiz_completed_total", 0).coerceAtLeast(distribution.sum())
            total to distribution
        } catch (_: Exception) {
            0 to IntArray(7)
        }
    }

    private fun afficherDetailQuizRpg(totalQuizComplets: Int, distribution: IntArray) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18).toInt(), dp(18).toInt(), dp(18).toInt(), dp(18).toInt())
            try { setBackgroundResource(R.drawable.bg_rpg_dialog) } catch (_: Exception) {}
        }

        val title = TextView(this).apply {
            text = "ARCHIVES DES ÉPREUVES"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Quiz complets terminés : $totalQuizComplets"
            setTextColor(Color.parseColor("#F5F5F5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(0, dp(8).toInt(), 0, dp(12).toInt())
        }
        root.addView(subtitle)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (stars in 1..6) {
            val count = distribution.getOrElse(stars) { 0 }
            val pct = if (totalQuizComplets > 0) ((count * 100f) / totalQuizComplets) else 0f
            val line = TextView(this).apply {
                text = "${stars}★ : $count quiz • ${String.format(java.util.Locale.getDefault(), "%.1f", pct)}%"
                setTextColor(Color.parseColor("#FFF8E1"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setPadding(dp(10).toInt(), dp(10).toInt(), dp(10).toInt(), dp(10).toInt())
                try { setBackgroundResource(R.drawable.bg_divine_card) } catch (_: Exception) {}
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(8).toInt()
            content.addView(line, lp)
        }
        scroll.addView(content)
        root.addView(scroll)

        AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(root)
            .setPositiveButton("REFERMER LES ARCHIVES", null)
            .show()
    }

    private fun animerEntreeStagger() {
        val vues = listOf(
            binding.cardHero,
            binding.cardDayStreak,
            binding.cardBestDayStreak,
            binding.cardWinStreak,
            binding.cardBestWinStreak,
            binding.cardSucces,
            binding.cardSecondaires
        )
        val density = resources.displayMetrics.density

        vues.forEachIndexed { index, vue ->
            vue.translationY = 60f * density
            vue.alpha = 0f
            vue.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400L)
                .setStartDelay((index * 70).toLong())
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun animerBarre(barre: android.widget.ProgressBar, valeurCible: Int) {
        ObjectAnimator.ofInt(barre, "progress", 0, valeurCible).apply {
            duration = 900L
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun formatNombre(n: Int): String =
        String.format("%,d", n).replace(",", " ")

    private fun dp(value: Int): Float = value * resources.displayMetrics.density
}
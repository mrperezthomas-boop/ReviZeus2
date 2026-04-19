package com.revizeus.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.models.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════
 * DEBUG ANALYTICS ACTIVITY — ML
 * ═══════════════════════════════════════════════════════════════
 * Écran de debug développeur pour visualiser les données ML.
 *
 * Affiche :
 *  - Profil de compétences par matière (maîtrise, confiance, réussite)
 *  - Statistiques globales (pratiques, réussite, révisions à faire)
 *  - Fragments de Forge par matière + crafts disponibles
 *  - Dernières réponses enregistrées (raw analytics)
 *
 * CORRECTIONS v3 :
 * ✅ BGM bgm_debug.mp3 : joué à l'entrée (onCreate), stoppé à finish() (Règle 8 : délai 300ms)
 * ✅ Bouton retour : bg_temple_button (ImageView fond) + label Cinzel or — design premium
 * ✅ stopMusic() appelé avant finish() pour couper bgm_debug proprement
 * ═══════════════════════════════════════════════════════════════
 */
class DebugAnalyticsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Portrait forcé + mode immersif gérés par BaseActivity.onCreate

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#05050A"))
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        scrollView.addView(container)
        setContentView(scrollView)

        // ── BGM DEBUG ─────────────────────────────────────────────────────────
        // Joue bgm_debug dès l'entrée dans l'écran de debug ML.
        // RÈGLE 8 : stopMusic() d'abord, délai 300ms, puis playMusic().
        lifecycleScope.launch {
            try {
                SoundManager.stopMusic()
                kotlinx.coroutines.delay(300L)
                SoundManager.playMusic(this@DebugAnalyticsActivity, R.raw.bgm_debug)
            } catch (_: Exception) {}
        }

        AnalyticsManager.initialize(this)

        lifecycleScope.launch {

            // ── EN-TÊTE ──────────────────────────────────────────────────
            container.addView(creerEntete())
            container.addView(creerSeparateur())

            // ── COMPÉTENCES ───────────────────────────────────────────────
            val skills = withContext(Dispatchers.IO) {
                AnalyticsManager.getSkillsSummary()
            }

            if (skills.isEmpty()) {
                container.addView(creerTexteInfo("Aucune donnée ML enregistrée."))
                container.addView(creerTexteInfo("Fais quelques quiz pour commencer l'analyse !"))
            } else {
                container.addView(creerTitreSection("📊 PROFIL DE COMPÉTENCES"))

                skills.sortedByDescending { it.masteryLevel }.forEach { skill ->
                    container.addView(creerCarteSkill(skill))
                }

                // ── STATISTIQUES GLOBALES ─────────────────────────────────
                container.addView(creerSeparateur())
                container.addView(creerTitreSection("📈 STATISTIQUES GLOBALES"))

                val totalPractice = skills.sumOf { it.practiceCount }
                val avgSuccess    = skills.map { it.successRate }.average().toFloat()
                val avgMastery    = skills.map { it.masteryLevel }.average().toFloat()
                val needReview    = skills.count { it.needsReview }

                container.addView(creerTexteStatistique("Total pratiques : $totalPractice"))
                container.addView(creerTexteStatistique("Taux réussite moyen : ${(avgSuccess * 100).toInt()}%"))
                container.addView(creerTexteStatistique("Maîtrise moyenne : ${(avgMastery * 100).toInt()}%"))
                container.addView(creerTexteStatistique("Matières à réviser : $needReview"))

                // ── DERNIÈRES RÉPONSES ────────────────────────────────────
                container.addView(creerSeparateur())
                container.addView(creerTitreSection("📝 DERNIÈRES RÉPONSES"))

                val recentAnalytics = withContext(Dispatchers.IO) {
                    try {
                        val db = AppDatabase.getDatabase(this@DebugAnalyticsActivity)
                        db.userAnalyticsDao().getRecent(1, limit = 10)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                if (recentAnalytics.isEmpty()) {
                    container.addView(creerTexteInfo("Aucune réponse enregistrée."))
                } else {
                    recentAnalytics.forEach { analytics ->
                        container.addView(creerCarteAnalytics(analytics))
                    }
                }
            }

            // ── FRAGMENTS FORGE ───────────────────────────────────────────
            container.addView(creerSeparateur())
            container.addView(creerTitreSection("⚗ FRAGMENTS DE CONNAISSANCE"))
            try {
                val profile = withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(this@DebugAnalyticsActivity)
                    db.iAristoteDao().getUserStats()
                }
                if (profile != null) {
                    val json = org.json.JSONObject(profile.knowledgeFragments)
                    if (json.length() == 0) {
                        container.addView(creerTexteInfo("Aucun fragment. Réponds correctement en quiz !"))
                    } else {
                        json.keys().forEach { matiere ->
                            val count = json.optInt(matiere, 0)
                            container.addView(creerTexteStatistique("$matiere : $count fragment(s)"))
                        }
                    }
                    val affordable = try {
                        CraftingSystem.affordableRecipes(profile)
                    } catch (_: Exception) { emptyList() }

                    container.addView(creerTexteInfo(
                        if (affordable.isEmpty()) "— Aucun craft disponible actuellement."
                        else "✦ ${affordable.size} recette(s) craftable(s) → Ouvre la Forge !"
                    ))
                } else {
                    container.addView(creerTexteInfo("Profil introuvable en base."))
                }
            } catch (e: Exception) {
                container.addView(creerTexteInfo("Erreur lecture fragments : ${e.message}"))
            }

            // ── BOUTON RETOUR ─────────────────────────────────────────────
            container.addView(creerSeparateur())
            container.addView(creerBoutonRetour())
        }
    }

    // ══════════════════════════════════════════════════════════════
    // WIDGETS DE CONSTRUCTION UI
    // ══════════════════════════════════════════════════════════════

    private fun creerEntete(): TextView {
        return TextView(this).apply {
            text = "⚙  DEBUG ML ANALYTICS"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextColor(Color.parseColor("#FFD700"))
            setPadding(0, dp(16), 0, dp(16))
            gravity = Gravity.CENTER
            typeface = try { resources.getFont(R.font.cinzel) }
                       catch (_: Exception) { Typeface.DEFAULT_BOLD }
        }
    }

    private fun creerTitreSection(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#E8D07A"))
            setPadding(0, dp(18), 0, dp(8))
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.06f
        }
    }

    private fun creerTexteInfo(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#666677"))
            setPadding(0, dp(5), 0, dp(5))
        }
    }

    private fun creerTexteStatistique(text: String): TextView {
        return TextView(this).apply {
            this.text = "▸  $text"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.parseColor("#CCCCCC"))
            setPadding(dp(10), dp(4), 0, dp(4))
        }
    }

    private fun creerSeparateur(): android.view.View {
        return android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
            setBackgroundColor(Color.parseColor("#22FFD700"))
        }
    }

    private fun creerCarteSkill(skill: com.revizeus.app.models.UserSkillProfile): LinearLayout {
        val masteryColor = when {
            skill.masteryLevel >= 0.90f -> "#4CAF50"
            skill.masteryLevel >= 0.75f -> "#8BC34A"
            skill.masteryLevel >= 0.60f -> "#FFC107"
            skill.masteryLevel >= 0.40f -> "#FF9800"
            else                        -> "#F44336"
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.parseColor("#0E0E18"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

        // Titre + pourcentage maîtrise sur même ligne
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(this).apply {
            text = "📚 ${skill.subject}"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#FFD700"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = "${(skill.masteryLevel * 100).toInt()}%"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Color.parseColor(masteryColor))
            typeface = Typeface.DEFAULT_BOLD
        })
        card.addView(row)

        // Métriques compactes sur une ligne
        val meta = "Réussite ${(skill.successRate * 100).toInt()}%  ·  " +
                   "Confiance ${(skill.confidence * 100).toInt()}%  ·  " +
                   "${skill.practiceCount} quiz  ·  " +
                   "${skill.avgResponseTime / 1000}s"
        card.addView(TextView(this).apply {
            text = meta
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(Color.parseColor("#666677"))
            setPadding(0, dp(5), 0, 0)
        })

        if (skill.needsReview) {
            card.addView(TextView(this).apply {
                text = "⚠  RÉVISION RECOMMANDÉE"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(Color.parseColor("#FF5722"))
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(5), 0, 0)
            })
        }

        return card
    }

    private fun creerCarteAnalytics(analytics: com.revizeus.app.models.UserAnalytics): LinearLayout {
        val resultColor = if (analytics.isCorrect) "#4CAF50" else "#F44336"
        val resultIcon  = if (analytics.isCorrect) "✓" else "✗"
        val resultLabel = if (analytics.isCorrect) "CORRECT" else "FAUX"

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.parseColor("#08080F"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        }

        card.addView(TextView(this).apply {
            text = "$resultIcon  ${analytics.subject} — $resultLabel  ·  ${analytics.responseTime / 1000}s"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor(resultColor))
            typeface = Typeface.DEFAULT_BOLD
        })

        val question = analytics.questionText?.take(90)?.let { "$it…" } ?: "—"
        card.addView(TextView(this).apply {
            text = question
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(Color.parseColor("#444455"))
            setPadding(0, dp(3), 0, 0)
        })

        return card
    }

    /**
     * Bouton retour premium — 2 couches superposées dans un FrameLayout :
     *   1. bg_temple_button  : fond texturé grec doré (ImageView FIT_XY)
     *   2. bg_textelayout    : surcouche légère pour profondeur (ImageView, alpha 0.45)
     *   3. Label Cinzel or   : texte centré, SFX + stopMusic + finish
     *
     * stopMusic() coupe bgm_debug avant de sortir (RÈGLE 8 respectée).
     */
    private fun creerBoutonRetour(): FrameLayout {
        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58)
            ).apply {
                topMargin    = dp(10)
                bottomMargin = dp(28)
                leftMargin   = dp(8)
                rightMargin  = dp(8)
            }
            isClickable = true
            isFocusable = true
        }

        // Couche 1 — fond texturé bg_temple_button
        frame.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            try { setImageResource(R.drawable.bg_temple_button) }
            catch (_: Exception) { setBackgroundColor(Color.parseColor("#1A1A2E")) }
            scaleType = ImageView.ScaleType.FIT_XY
        })

        // Couche 2 — bg_textelayout en surcouche, alpha réduit pour profondeur
        frame.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            try { setImageResource(R.drawable.bg_rpg_dialog) }
            catch (_: Exception) {}
            scaleType = ImageView.ScaleType.FIT_XY
            alpha = 0.35f
        })

        // Couche 3 — label cliquable Cinzel or centré
        frame.addView(TextView(this).apply {
            text = "← RETOUR À L'OLYMPE"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Color.parseColor("#FFD700"))
            gravity = Gravity.CENTER
            typeface = try { resources.getFont(R.font.cinzel) }
                       catch (_: Exception) { Typeface.DEFAULT_BOLD }
            letterSpacing = 0.06f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
                try { SoundManager.stopMusic() } catch (_: Exception) {}
                finish()
            }
        })

        return frame
    }

    // ══════════════════════════════════════════════════════════════
    // COMPANION — Animation point debug rouge (Dashboard)
    // ══════════════════════════════════════════════════════════════

    companion object {
        /**
         * Lance la pulsation AlphaAnimation sur le point rouge Debug
         * dans le Dashboard (binding.viewDebugDot).
         *
         * USAGE dans DashboardActivity.setupMenu() :
         * ```kotlin
         * DebugAnalyticsActivity.animerPointDebug(binding.viewDebugDot)
         * ```
         *
         * Cycle : 25% → 100% → 25% opacité, 900ms par demi-cycle, boucle infinie.
         * Silencieux en cas d'erreur — l'animation est cosmétique, pas critique.
         */
        fun animerPointDebug(vue: android.view.View) {
            try {
                AlphaAnimation(0.25f, 1.0f).apply {
                    duration = 900L
                    repeatCount = Animation.INFINITE
                    repeatMode = Animation.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                    vue.startAnimation(this)
                }
            } catch (_: Exception) {}
        }
    }

    // ══════════════════════════════════════════════════════════════
    // HELPER DP
    // ══════════════════════════════════════════════════════════════

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
    ).toInt()
}

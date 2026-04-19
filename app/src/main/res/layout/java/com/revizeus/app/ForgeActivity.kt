package com.revizeus.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup                          // ✅ UNE SEULE fois — doublon ligne 24 supprimé
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView          // ✅ Lottie (ajouté dans build.gradle.kts)
import com.revizeus.app.databinding.ActivityForgeBinding
import com.revizeus.app.databinding.ItemRecipeCardBinding
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════
 * ForgeActivity — LA FORGE D'HÉPHAÏSTOS
 * PHASE C — RéviZeus
 * ═══════════════════════════════════════════════════════════════
 *
 * Écran de craft : l'élève transforme ses Fragments de Connaissance
 * (gagnés en donnant des bonnes réponses aux quiz) en objets divins.
 *
 * FLUX PRINCIPAL :
 *   onCreate → loadProfileAndSetup() → afficherFragmentHud() + setupRecyclerView()
 *
 * FLUX DE CRAFT :
 *   btnForge.click → vibrerMarteau() → CraftingSystem.deductCost()
 *   → insertInventoryItem() → BadgeManager.recordItemForged()
 *   → sfx_forge_success → lancerAnimationLottie() → afficherDialogueHephaistos()
 *   → evaluateAll() → afficherToastBadge() si badge débloqué
 *
 * MISE À JOUR v3 :
 *   ✅ BUG FIX — import android.view.ViewGroup dupliqué supprimé (ERREUR COMPILE)
 *   ✅ BUG FIX — import ValueAnimator inutilisé supprimé
 *   ✅ lancerParticulesOlympiennes() → lancerAnimationLottie()
 *      Joue res/raw/lottie_forge_fire.json une seule fois en overlay plein écran
 *      Fallback automatique : lancerParticulesNatives() si fichier .json absent
 *   ✅ Fallback icône 3 niveaux dans onBindViewHolder :
 *        1. ic_[resultResName]          icône spécifique de l'objet
 *        2. ic_hud_[type]_generic       icône générique par catégorie
 *        3. ic_forge_hephaistos         dernier recours absolu
 *   ✅ Badge "Apprenti Forgeron" (forge_first) après premier craft :
 *        BadgeManager.recordItemForged() → buildContext() → evaluateAll()
 *        → afficherToastBadge() pour tous les badges FORGE débloqués
 *
 * ÉVOLUTION FUTURE :
 *   - Filtrage par type via onglets (TabLayout + ViewPager2 ou ChipGroup)
 *   - Notification "Forge prête !" depuis DashboardActivity via affordableRecipes()
 *   - Objets équipables : bonus quiz actif sélectionné dans InventoryActivity
 *   - Icônes Gemini dynamiques : buildItemIcon(resName, desc) → base64 → Coil
 * ═══════════════════════════════════════════════════════════════
 */
class ForgeActivity : BaseActivity() {

    private lateinit var binding: ActivityForgeBinding
    private var currentProfile: UserProfile? = null

    private val allRecipes: List<CraftingSystem.Recipe> = CraftingSystem.availableRecipes
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setupHeader()
        setupRecyclerView()
        loadProfileAndSetup()
    }

    override fun onResume() {
        super.onResume()
        try {
            SoundManager.playMusic(this, R.raw.bgm_forge)
        } catch (_: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            SoundManager.stopMusic()
        } catch (_: Exception) {
        }
    }

    // ══════════════════════════════════════════════════════════
    // SETUP
    // ══════════════════════════════════════════════════════════

    private fun setupHeader() {
        binding.btnForgeBack.setOnClickListener {
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            finish()
        }
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(
            recipes = getDisplayRecipes(),  // ✅ PACK 1 — Tri craftable-first
            profile = null,
            onForgeClicked = { recipe -> onForgeClic(recipe) }
        )
        binding.rvRecipes.apply {
            layoutManager = LinearLayoutManager(this@ForgeActivity)
            adapter = recipeAdapter
            itemAnimator = null
        }
        binding.tvForgeRecipeCount.text = "${allRecipes.size} recettes"
    }

    private fun loadProfileAndSetup() {
        lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getDatabase(this@ForgeActivity)
                    db.iAristoteDao().getUserProfile() ?: UserProfile()
                } catch (e: Exception) {
                    Log.e("REVIZEUS", "ForgeActivity — erreur chargement profil : ${e.message}")
                    UserProfile()
                }
            }
            currentProfile = profile
            afficherFragmentHud(profile)
            recipeAdapter.updateProfile(profile)
        }
    }

    // ══════════════════════════════════════════════════════════
    // HUD DES FRAGMENTS
    // ══════════════════════════════════════════════════════════

    private fun afficherFragmentHud(profile: UserProfile) {
        val container = binding.llFragmentHud
        container.removeAllViews()
        allRecipes.flatMap { it.cost.keys }.toSortedSet().forEach { matiere ->
            container.addView(creerChipFragment(matiere, profile.getFragmentCount(matiere)))
        }
    }

    private fun creerChipFragment(matiere: String, count: Int): LinearLayout {
        val textColor = when {
            count >= 30 -> Color.parseColor("#FFD700")
            count >= 10 -> Color.parseColor("#E7E7E7")
            else -> Color.parseColor("#B8AFAF")
        }
        val iconRes = KnowledgeFragmentManager.getFragmentIconRes(this, matiere)

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(6), dp(4), dp(6), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(10) }

            addView(TextView(this@ForgeActivity).apply {
                text = count.toString()
                setTextColor(Color.parseColor("#FFD700"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            })

            addView(ImageView(this@ForgeActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                    topMargin = dp(2)
                }
                if (iconRes != 0) {
                    setImageResource(iconRes)
                }
                alpha = 0.95f
                scaleType = ImageView.ScaleType.FIT_CENTER
            })

            addView(TextView(this@ForgeActivity).apply {
                text = matiere
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                gravity = Gravity.CENTER
                maxLines = 2
                setPadding(0, dp(3), 0, 0)
            })
        }
    }

    // ══════════════════════════════════════════════════════════
    // LOGIQUE DE CRAFT
    // ══════════════════════════════════════════════════════════

    /**
     * Déclenchée au clic sur "FORGER" d'une recette.
     *
     * Flux :
     * 1.  Vérification canAfford() — guard
     * 2.  vibrerMarteau() + sfx_thunder_confirm (immédiat)
     * 3.  IO : deductCost() + updateUserProfile()
     * 4.  IO : insertion ou incrémentation inventaire
     * 5.  IO : BadgeManager.recordItemForged()
     * 6.  delay(300ms) + sfx_forge_success        [Règle 8]
     * 7.  lancerAnimationLottie() — feu de forge
     * 8.  IO : buildForgeSuccessDialogue() Héphaïstos
     * 9.  afficherDialogueHephaistos()
     * 10. IO : buildContext() + evaluateAll()
     * 11. afficherToastBadge() pour chaque badge FORGE débloqué
     * 12. Rafraîchissement HUD + adapter
     */
    private fun onForgeClic(recipe: CraftingSystem.Recipe) {
        val profile = currentProfile ?: return

        if (!CraftingSystem.canAfford(recipe, profile)) {
            val missing = CraftingSystem.missingFragments(recipe, profile)
            val detail = missing.entries.joinToString(", ") { (m, n) -> "$n ${m.take(6)}" }
            // Badge forge_impatient — cliquer 10x sans ressources
            BadgeManager.incrementStat(this, "stat_forge_fail_click")
            // BLOC B : Conversion Toast → Dialogue RPG
            DialogRPGManager.showAlert(
                activity = this,
                godId = "hephaestus",
                message = "Héphaïstos constate que tu n'as pas assez de Fragments de Savoir pour cette recette.",
                title = "⚒ FORGE DIVINE ⚒"
            )
            return
        }

        // Étapes 1–2 : feedback immédiat
        vibrerMarteau()
        try { jouerSfx(R.raw.sfx_thunder_confirm) } catch (_: Exception) {}

        lifecycleScope.launch {

            // ── ÉTAPES 3–5 : DB IO ─────────────────────────────────────────
            val dbSuccess = withContext(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getDatabase(this@ForgeActivity)

                    // 3. Déduction fragments
                    CraftingSystem.deductCost(recipe, profile)
                    db.iAristoteDao().updateUserProfile(profile)

                    // 4. Inventaire : insert ou incrément
                    val existingItem = db.iAristoteDao().getInventoryItemByName(recipe.name)
                    if (existingItem != null) {
                        existingItem.quantity += 1
                        db.iAristoteDao().updateInventoryItem(existingItem)
                    } else {
                        db.iAristoteDao().insertInventoryItem(
                            CraftingSystem.buildItemFromRecipe(recipe)
                        )
                    }

                    // 5. Stat badge — incrémente le compteur de crafts
                    BadgeManager.recordItemForged(this@ForgeActivity)

                    Log.d("REVIZEUS", "Forge réussie : ${recipe.name}")
                    true
                } catch (e: Exception) {
                    Log.e("REVIZEUS", "Erreur forge DB : ${e.message}")
                    false
                }
            }

            if (!dbSuccess || isFinishing || isDestroyed) return@launch

            // ── ÉTAPE 6 : délai Règle 8 + SFX succès ──────────────────────
            delay(300L)
            try { jouerSfx(R.raw.sfx_forge_success) } catch (_: Exception) {}

            // ── ÉTAPE 7 : animation Lottie ─────────────────────────────────
            lancerAnimationLottie()

            // ── ÉTAPE 8 : dialogue IA Héphaïstos ──────────────────────────
            val response = withContext(Dispatchers.IO) {
                try {
                    GodLoreManager.buildForgeSuccessDialogue(
                        recipeName = recipe.name,
                        profile = profile
                    )
                } catch (e: Exception) {
                    Log.e("REVIZEUS", "Forge dialogue erreur : ${e.message}")
                    null
                }
            }

            if (isFinishing || isDestroyed) return@launch

            // ── ÉTAPE 9 : dialogue visuel ──────────────────────────────────
            response?.let { afficherDialogueHephaistos(recipe, it) }

            // ── ÉTAPES 10–11 : check badges ───────────────────────────────
            val nouveauxBadges = withContext(Dispatchers.IO) {
                try {
                    val ctx = BadgeManager.buildContext(this@ForgeActivity)
                    BadgeManager.evaluateAll(this@ForgeActivity, ctx)
                } catch (e: Exception) {
                    Log.e("REVIZEUS", "Badge eval erreur : ${e.message}")
                    emptyList()
                }
            }

            // Afficher uniquement les badges FORGE débloqués pendant ce craft
            nouveauxBadges
                .filter { it.categorie == BadgeCategorie.FORGE }
                .forEach { badge -> afficherToastBadge(badge) }

            // ── ÉTAPE 12 : rafraîchissement HUD ───────────────────────────
            afficherFragmentHud(profile)
            recipeAdapter.updateProfile(profile)
        }
    }

    // ══════════════════════════════════════════════════════════
    // ANIMATION LOTTIE — FEU DE FORGE
    // ══════════════════════════════════════════════════════════

    /**
     * Joue l'animation Lottie "lottie_forge_fire" en overlay plein écran.
     *
     * FICHIER REQUIS : res/raw/lottie_forge_fire.json
     *   Style recommandé : flammes dorées/orangées, loop=false, durée 2–3s
     *   Source gratuite : https://lottiefiles.com → chercher "fire", "forge", "gold flames"
     *   Exporter en "Lottie JSON", placer dans app/src/main/res/raw/
     *
     * Fonctionnement :
     *   - LottieAnimationView créé programmatiquement sur le DecorView
     *   - repeatCount = 0 → joué UNE SEULE FOIS
     *   - Retiré du rootView automatiquement via AnimatorListener.onAnimationEnd
     *   - isClickable = false → transparent aux événements tactiles
     *
     * Fallback :
     *   - Si lottie_forge_fire.json absent → lancerParticulesNatives()
     *   - Si rootView inaccessible → lancerParticulesNatives()
     *   - Tout protégé par try/catch → jamais de crash
     */
    private fun lancerAnimationLottie() {
        try {
            val rootView = window.decorView.rootView as? FrameLayout
                ?: return lancerParticulesNatives()

            val lottieResId = resources.getIdentifier("lottie_forge_fire", "raw", packageName)
            if (lottieResId == 0) {
                // Fichier JSON absent → fallback particules Android natif
                return lancerParticulesNatives()
            }

            val lottieView = LottieAnimationView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setAnimation(lottieResId)
                repeatCount = 0              // Joué 1 seule fois
                scaleType = ImageView.ScaleType.CENTER_CROP
                elevation = 900f
                alpha = 0.92f
                isClickable = false          // Transparent aux touches
                isFocusable = false
            }

            rootView.addView(lottieView)

            lottieView.addAnimatorListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    try { rootView.removeView(lottieView) } catch (_: Exception) {}
                }
            })

            lottieView.playAnimation()

        } catch (e: Exception) {
            Log.d("REVIZEUS", "Lottie non lancée : ${e.message} → fallback particules")
            try { lancerParticulesNatives() } catch (_: Exception) {}
        }
    }

    /**
     * Fallback : flash doré + 12 éclairs convergents (AnimatorSet natif Android).
     * Utilisé automatiquement si lottie_forge_fire.json est absent.
     * Protégé par try/catch — ne bloque jamais le flux de craft.
     */
    private fun lancerParticulesNatives() {
        try {
            val rootView = window.decorView.rootView as? FrameLayout ?: return
            val w = rootView.width.toFloat()
            val h = rootView.height.toFloat()
            if (w == 0f || h == 0f) return

            // Flash doré plein écran (350ms)
            val flash = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#FFD700"))
                alpha = 0f; elevation = 900f
            }
            rootView.addView(flash)
            ObjectAnimator.ofFloat(flash, "alpha", 0f, 0.55f, 0f).apply {
                duration = 350
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        try { rootView.removeView(flash) } catch (_: Exception) {}
                    }
                })
            }.start()

            // 12 éclairs depuis les 4 bords vers le centre
            val cx = w / 2f; val cy = h / 2f
            val ps = dp(10).toFloat()
            listOf(
                w * 0.2f to 0f, cx to 0f, w * 0.8f to 0f,
                w * 0.2f to h,  cx to h,  w * 0.8f to h,
                0f to h * 0.3f, 0f to cy, 0f to h * 0.7f,
                w to h * 0.3f,  w to cy,  w to h * 0.7f
            ).forEachIndexed { idx, (sx, sy) ->
                val spark = View(this).apply {
                    layoutParams = FrameLayout.LayoutParams(ps.toInt(), ps.toInt())
                    setBackgroundColor(Color.parseColor("#FFD700"))
                    x = sx; y = sy; alpha = 0.9f; elevation = 890f; rotation = idx * 30f
                }
                rootView.addView(spark)
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(spark, "x", sx, cx - ps / 2f),
                        ObjectAnimator.ofFloat(spark, "y", sy, cy - ps / 2f),
                        ObjectAnimator.ofFloat(spark, "alpha", 0.9f, 0f),
                        ObjectAnimator.ofFloat(spark, "scaleX", 1f, 0.3f)
                    )
                    duration = 500L + idx * 30L; startDelay = 80L
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            try { rootView.removeView(spark) } catch (_: Exception) {}
                        }
                    })
                }.start()
            }
        } catch (e: Exception) {
            Log.d("REVIZEUS", "Particules natives ignorées : ${e.message}")
        }
    }

    // ══════════════════════════════════════════════════════════
    // BADGE — TOAST VISUEL
    // ══════════════════════════════════════════════════════════

    /**
     * Toast doré affiché quand un badge FORGE est débloqué pendant un craft.
     *
     * Badges possibles :
     *   forge_first (Apprenti Forgeron)  — premier craft
     *   forge_5     (Main de Fer)        — 5 crafts
     *   forge_10    (Maître Artisan)     — 10 crafts
     *   fragment_100 / fragment_1000     — accumulation fragments
     *   forge_impatient                  — 10 clics sans ressources
     *
     * SFX : sfx_thunder_confirm (bref, distinct du sfx_forge_success)
     */
    private fun afficherToastBadge(badge: BadgeDefinition) {
        try { jouerSfx(R.raw.sfx_thunder_confirm) } catch (_: Exception) {}
        // BLOC B : Conversion Toast → Dialogue RPG
        DialogRPGManager.showReward(
            activity = this,
            godId = "hephaestus",
            message = "Héphaïstos a forgé un nouveau badge pour toi !",
            additionalLabel = "BADGE",
            additionalText = badge.nom
        )
    }

    // ══════════════════════════════════════════════════════════
    // VIBRATION — MARTEAU D'HÉPHAÏSTOS
    // ══════════════════════════════════════════════════════════

    private fun vibrerMarteau() {
        try {
            val pattern = longArrayOf(0L, 70L, 80L, 110L, 100L, 180L)
            val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator ?: return
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.d("REVIZEUS", "Vibration marteau ignorée : ${e.message}")
        }
    }

    // ══════════════════════════════════════════════════════════
    // DIALOGUE DE SUCCÈS HÉPHAÏSTOS
    // ══════════════════════════════════════════════════════════

    private fun afficherDialogueHephaistos(
        recipe: CraftingSystem.Recipe,
        response: GeminiManager.GodResponse
    ) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_rpg_dialog)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        // Portrait + nom dieu + nom objet
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val portrait = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(72))
            scaleType = ImageView.ScaleType.FIT_CENTER
            listOf("ic_hephaistos_chibi", "ic_forge_hephaistos", "ic_zeus_chibi")
                .map { resources.getIdentifier(it, "drawable", packageName) }
                .firstOrNull { it != 0 }
                ?.let { setImageResource(it) }
        }
        headerRow.addView(portrait)

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dp(14) }
        }
        titleCol.addView(TextView(this).apply {
            text = "HÉPHAÏSTOS"
            setTextColor(Color.parseColor("#FF6B00"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
        })
        titleCol.addView(TextView(this).apply {
            text = "✦ ${recipe.name} forgé"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
        })
        headerRow.addView(titleCol)
        root.addView(headerRow)

        // Séparateur
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ).apply { topMargin = dp(12); bottomMargin = dp(12) }
            setBackgroundColor(Color.parseColor("#4DFF6B00"))
        })

        // Texte IA
        root.addView(TextView(this).apply {
            text = response.text
            setTextColor(Color.parseColor("#F0F0F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setLineSpacing(dp(3).toFloat(), 1f)
        })

        // Mnemo doré
        if (response.mnemo.isNotBlank()) {
            root.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                ).apply { topMargin = dp(14); bottomMargin = dp(10) }
                setBackgroundColor(Color.parseColor("#33FFD700"))
            })
            root.addView(TextView(this).apply {
                text = "⚒ ${response.mnemo}"
                setTextColor(Color.parseColor("#CCFFD700"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.defaultFromStyle(Typeface.ITALIC)
            })
        }

        // Bouton COMPRIS
        val btnClose = TextView(this).apply {
            text = "COMPRIS"
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_textelayout)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(18) }
            isClickable = true; isFocusable = true
        }
        root.addView(btnClose)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(root).setCancelable(false).create()
        btnClose.setOnClickListener {
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            dialog.dismiss()
        }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rpg_dialog)
    }

    // ══════════════════════════════════════════════════════════
    // DIALOGUE DE CONFIRMATION CRAFT — PACK 1
    // ══════════════════════════════════════════════════════════

    /**
     * PACK 1 — Aperçu détaillé d'une recette avant craft.
     * 
     * Affiche un dialogue de confirmation avec :
     * - Portrait Héphaïstos + icône objet + nom/type
     * - Description complète
     * - Coût détaillé par matière (fragments possédés / requis)
     * - Lore divine si disponible
     * - Boutons ANNULER / FORGER
     * 
     * SFX :
     * - sfx_dialogue_blip à l'ouverture
     * - sfx_avatar_confirm à l'annulation
     * - Le craft réel se fait via onForgeClic() après confirmation
     */
    private fun afficherDialogueConfirmationCraft(recipe: CraftingSystem.Recipe) {
        if (isFinishing || isDestroyed) return

        val prof = currentProfile ?: return
        val affordable = CraftingSystem.canAfford(recipe, prof)
        if (!affordable) return // Sécurité : ne devrait jamais arriver ici si non craftable

        try { jouerSfx(R.raw.sfx_dialogue_blip) } catch (_: Exception) {}

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#12111E"))
            }
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        // ── Header : portrait Héphaïstos + icône objet + nom ──────────────────
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Portrait Héphaïstos
        val portrait = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(68), dp(68))
            scaleType = ImageView.ScaleType.FIT_CENTER
            listOf("ic_hephaistos_chibi", "ic_forge_hephaistos", "ic_zeus_chibi")
                .map { resources.getIdentifier(it, "drawable", packageName) }
                .firstOrNull { it != 0 }
                ?.let { setImageResource(it) }
        }
        headerRow.addView(portrait)

        // Icône objet
        val iconItem = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
                .apply { marginStart = dp(12); marginEnd = dp(12) }
            scaleType = ImageView.ScaleType.FIT_CENTER
            val specificId = resources.getIdentifier(recipe.resultResName, "drawable", packageName)
            val fallbackId = resources.getIdentifier("ic_forge_hephaistos", "drawable", packageName)
            if (specificId != 0) setImageResource(specificId)
            else if (fallbackId != 0) setImageResource(fallbackId)
        }
        headerRow.addView(iconItem)

        // Colonne titre
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(this).apply {
            text = recipe.type
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
        })
        titleCol.addView(TextView(this).apply {
            text = recipe.name
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        })
        headerRow.addView(titleCol)
        root.addView(headerRow)

        // Séparateur
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ).apply { topMargin = dp(12); bottomMargin = dp(12) }
            setBackgroundColor(Color.parseColor("#33FFD700"))
        })

        // Description
        root.addView(TextView(this).apply {
            text = recipe.description
            setTextColor(Color.parseColor("#C8C8C8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLineSpacing(dp(3).toFloat(), 1f)
        })

        // Coût détaillé
        root.addView(TextView(this).apply {
            text = "Coût de fabrication :"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12); bottomMargin = dp(6) }
        })

        recipe.cost.forEach { (matiere, qty) ->
            val has = prof.getFragmentCount(matiere)
            root.addView(TextView(this).apply {
                text = "  ⚗ $has / $qty fragments de $matiere"
                setTextColor(Color.parseColor("#CCFFD700"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                try {
                    typeface = androidx.core.content.res.ResourcesCompat.getFont(this@ForgeActivity, R.font.vt323_regular)                } catch (_: Exception) {}
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(4) }
            })
        }

        // Lore si disponible
        if (recipe.lore.isNotBlank()) {
            root.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                ).apply { topMargin = dp(10); bottomMargin = dp(10) }
                setBackgroundColor(Color.parseColor("#22FFD700"))
            })
            root.addView(TextView(this).apply {
                text = recipe.lore
                setTextColor(Color.parseColor("#88FFD700"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.defaultFromStyle(Typeface.ITALIC)
            })
        }

        // Boutons ANNULER / FORGER
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
        }

        val btnCancel = TextView(this).apply {
            text = "ANNULER"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#888888"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            try {
                typeface = androidx.core.content.res.ResourcesCompat.getFont(this@ForgeActivity, R.font.cinzel)  // ✅ this@ForgeActivity = Context
            } catch (_: Exception) {
                typeface = Typeface.DEFAULT_BOLD
            }
            setPadding(dp(16), dp(10), dp(16), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(10) }
            isClickable = true
            isFocusable = true
        }

        val btnConfirm = TextView(this).apply {
            text = "FORGER"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#0A0A14"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            try {
                typeface = androidx.core.content.res.ResourcesCompat.getFont(this@ForgeActivity, R.font.cinzel)  // ✅ CORRIGÉ
            } catch (_: Exception) {
                typeface = Typeface.DEFAULT_BOLD
            }
            try {
                setBackgroundResource(R.drawable.bg_temple_button)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#FFD700"))
            }
            setPadding(dp(20), dp(12), dp(20), dp(12))
            isClickable = true
            isFocusable = true
        }
        btnRow.addView(btnCancel)
        btnRow.addView(btnConfirm)
        root.addView(btnRow)

        val dialog = AlertDialog.Builder(this)
            .setView(root)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            onForgeClic(recipe) // Lancer le craft réel
        }

        try {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        } catch (_: Exception) {}
    }

    // ══════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
    ).toInt()

    // ══════════════════════════════════════════════════════════
    // TRI CRAFTABLE-FIRST — PACK 1
    // ══════════════════════════════════════════════════════════

    /**
     * Retourne les recettes triées par disponibilité craftable.
     * 
     * PACK 1 — FORGE CRAFTABLE-FIRST :
     * Les recettes que le joueur peut forger immédiatement apparaissent en premier,
     * puis les recettes non accessibles triées alphabétiquement.
     * 
     * Tri :
     * 1. CraftingSystem.canAfford() = true → en haut
     * 2. Tri alphabétique ensuite
     */
    private fun getDisplayRecipes(): List<CraftingSystem.Recipe> {
        val prof = currentProfile
        if (prof == null) return allRecipes

        return allRecipes.sortedWith(
            compareByDescending<CraftingSystem.Recipe> { recipe ->
                CraftingSystem.canAfford(recipe, prof)
            }.thenBy { it.name.lowercase() }
        )
    }

    // ══════════════════════════════════════════════════════════
    // INNER CLASS — RECIPE ADAPTER
    // ══════════════════════════════════════════════════════════

    /**
     * RecyclerView.Adapter pour la liste des recettes.
     *
     * SYSTÈME DE FALLBACK ICÔNE (3 niveaux) :
     *   Niveau 1 — ic_[resultResName]       Icône unique de l'objet (4 nouvelles créées)
     *   Niveau 2 — ic_hud_[type]_generic    Icône générique par catégorie
     *   Niveau 3 — ic_forge_hephaistos      Dernier recours absolu
     *
     * Drawables niveau 2 attendus (à créer, style simple 256×256) :
     *   ic_hud_bouclier_generic   — bouclier simple doré
     *   ic_hud_arme_generic       — épée simple dorée
     *   ic_hud_artefact_generic   — gemme ou médaillon générique
     *   ic_hud_parchemin_generic  — rouleau de parchemin
     *   ic_hud_offrande_generic   — flamme ou autel simplifié
     */
    private inner class RecipeAdapter(
        private val recipes: List<CraftingSystem.Recipe>,
        private var profile: UserProfile?,
        private val onForgeClicked: (CraftingSystem.Recipe) -> Unit
    ) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

        inner class RecipeViewHolder(val binding: ItemRecipeCardBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
            return RecipeViewHolder(
                ItemRecipeCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun getItemCount() = recipes.size

        override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
            val recipe = recipes[position]
            val prof = profile
            val affordable = prof != null && CraftingSystem.canAfford(recipe, prof)

            with(holder.binding) {

                // ── Icône — 3 niveaux de fallback ────────────────────────────
                val specificId   = resources.getIdentifier(recipe.resultResName, "drawable", packageName)
                val genericId    = resources.getIdentifier(fallbackIconForType(recipe.type), "drawable", packageName)
                val lastResortId = resources.getIdentifier("ic_forge_hephaistos", "drawable", packageName)
                when {
                    specificId    != 0 -> imgRecipeIcon.setImageResource(specificId)
                    genericId     != 0 -> imgRecipeIcon.setImageResource(genericId)
                    lastResortId  != 0 -> imgRecipeIcon.setImageResource(lastResortId)
                }

                // ── Badge type ────────────────────────────────────────────────
                tvRecipeType.text = recipe.type
                tvRecipeType.setBackgroundColor(colorForType(recipe.type))

                // ── Nom + description (ScrollView dans item_recipe_card.xml) ──
                tvRecipeName.text = recipe.name
                tvRecipeDescription.text = recipe.description

                // ── Coût "⚗ 12/50 Maths  ·  5/25 Philo" ────────────────────
                tvRecipeCost.text = recipe.cost.entries.joinToString("  ·  ") { (mat, qty) ->
                    val has = prof?.getFragmentCount(mat) ?: 0
                    "⚗ $has/$qty ${mat.take(7)}"
                }
                tvRecipeCost.setTextColor(
                    if (affordable) Color.parseColor("#FFD700") else Color.parseColor("#886655")
                )

                // ── Lore ──────────────────────────────────────────────────────
                tvRecipeLore.visibility = if (recipe.lore.isNotBlank()) {
                    tvRecipeLore.text = recipe.lore; View.VISIBLE
                } else View.GONE

                // ── Accent strip ──────────────────────────────────────────────
                accentStrip.setBackgroundColor(
                    if (affordable) Color.parseColor("#4CAF50") else Color.parseColor("#444444")
                )

                // ── Bouton FORGER ─────────────────────────────────────────────
                if (affordable) {
                    btnForge.setTextColor(Color.parseColor("#0A0A14"))
                    btnForge.alpha = 1.0f
                    btnForge.setBackgroundResource(R.drawable.bg_temple_button)
                    // PACK 1 — Dialogue de confirmation avant craft
                    btnForge.setOnClickListener { 
                        this@ForgeActivity.afficherDialogueConfirmationCraft(recipe) 
                    }
                } else {
                    btnForge.setTextColor(Color.parseColor("#555555"))
                    btnForge.alpha = 0.4f
                    btnForge.setBackgroundColor(Color.parseColor("#222222"))
                    btnForge.setOnClickListener {
                        val missing = prof?.let {
                            CraftingSystem.missingFragments(recipe, it)
                        } ?: return@setOnClickListener
                        // BLOC B : Conversion Toast → Dialogue RPG
                        val detail = missing.entries.joinToString("\n") { (m, n) -> "• $n fragment(s) de $m" }
                        DialogRPGManager.show(
                            activity = this@ForgeActivity,
                            config = DialogRPGConfig(
                                mainText = "Il te manque des Fragments de Savoir pour forger ceci.",
                                godId = "hephaestus",
                                category = DialogCategory.ALERT,
                                additionalLabel = "DÉTAILS",
                                additionalText = detail,
                                button1Label = "COMPRIS"
                            )
                        )
                    }
                }
            }
        }

        fun updateProfile(newProfile: UserProfile) {
            profile = newProfile
            notifyDataSetChanged()
        }

        /**
         * PACK 1 — Met à jour la liste de recettes (utilisé après craft).
         * Permet de re-trier craftable-first après déduction des fragments.
         */
        fun updateRecipes(newRecipes: List<CraftingSystem.Recipe>) {
            // Note : recipes est val dans le constructeur, on ne peut pas la modifier
            // Cette fonction sert surtout à forcer un notifyDataSetChanged() après updateProfile
            // Le vrai tri est fait dans getDisplayRecipes() appelé depuis ForgeActivity
            notifyDataSetChanged()
        }

        /**
         * Retourne le nom du drawable générique (niveau 2 fallback) selon le TYPE.
         * Si ce drawable est également absent, le niveau 3 (ic_forge_hephaistos)
         * prend le relais dans onBindViewHolder.
         */
        private fun fallbackIconForType(type: String): String = when (type.uppercase()) {
            "BOUCLIER"  -> "ic_hud_bouclier_generic"
            "ARME"      -> "ic_hud_arme_generic"
            "ARTEFACT"  -> "ic_hud_artefact_generic"
            "PARCHEMIN" -> "ic_hud_parchemin_generic"
            "OFFRANDE"  -> "ic_hud_offrande_generic"
            else        -> "ic_forge_hephaistos"
        }

        private fun colorForType(type: String): Int = when (type.uppercase()) {
            "BOUCLIER"  -> Color.parseColor("#1565C0")
            "ARME"      -> Color.parseColor("#B71C1C")
            "ARTEFACT"  -> Color.parseColor("#6A1B9A")
            "PARCHEMIN" -> Color.parseColor("#2E7D32")
            "OFFRANDE"  -> Color.parseColor("#E65100")
            else        -> Color.parseColor("#333333")
        }
    }
}

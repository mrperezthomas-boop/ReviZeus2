package com.revizeus.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.databinding.ActivitySavoirBinding
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.CourseEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════
 * SAVOIR v9 - Bibliothèque des cours par matière
 * ═══════════════════════════════════════════════════════════════
 * CHANGEMENTS :
 * ✅ Initialisation avec BGM par défaut (bgm_savoir)
 * ✅ Correction de la logique de sélection de thème au démarrage
 * ✅ Conservation de l'architecture et des commentaires
 * ═══════════════════════════════════════════════════════════════
 */
class SavoirActivity : BaseActivity() {

    private lateinit var binding: ActivitySavoirBinding
    private var allCourses: List<CourseEntry> = emptyList()
    private val coursesCountBySubject = mutableMapOf<String, Int>()

    // Nouveau : gestion fond animé / particules
    private var animatedBackgroundHelper: AnimatedBackgroundHelper? = null
    private var olympianParticlesView: OlympianParticlesView? = null

    /**
     * Matière visuellement active sur l'écran.
     * Modifié : Initialisé à vide pour forcer le thème par défaut au démarrage.
     */
    private var matiereThemeActive: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavoirBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.post {
            TutorialManager.showHeroTutorialIfNeeded(
                activity = this,
                stepId = "savoir_first_entry_v2",
                godId = "athena",
                title = "📚 Bibliothèque du Savoir 📚",
                message = "Ici reposent les savoirs consacrés par l'Oracle. Choisis un dieu pour ouvrir son temple. Une fois dans un temple, l'appui long sur un savoir dévoile des actions divines de gestion et d'entraînement. Garde-le en tête : c'est une mécanique clé de RéviZeus."
            )
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        installerAmbianceOlympienne()

        // On applique le thème. Si matiereThemeActive est vide,
        // getMatiereBgmRes renverra bgm_savoir par défaut.
        appliquerThemeMatiere(matiereThemeActive, restartMusic = true)

        binding.btnBackSavoir.setOnClickListener {
            try { jouerSfx(R.raw.sfx_dialogue_blip) } catch (_: Exception) {}
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadCoursesAndRefreshUI()

        // Si aucune matière n'est sélectionnée, on prend le premier dieu pour la couleur des particules
        val god = PantheonConfig.findByMatiere(matiereThemeActive) ?: PantheonConfig.GODS.first()
        animatedBackgroundHelper?.start(
            accentColor = god.couleur,
            mode = OlympianParticlesView.ParticleMode.SAVOIR
        )
    }

    override fun onPause() {
        super.onPause()
        animatedBackgroundHelper?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        animatedBackgroundHelper?.stop()
    }

    /**
     * Injecte discrètement la couche de particules derrière le contenu existant.
     * On ne casse pas le layout XML déjà en place.
     */
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

        // Insertion au fond du layout pour ne gêner aucun bouton
        root.addView(olympianParticlesView, 0)

        animatedBackgroundHelper = AnimatedBackgroundHelper(
            targetView = binding.root,
            particlesView = olympianParticlesView
        )
    }

    /**
     * Mapping background matière → res/drawable.
     */
    private fun getMatiereBackgroundRes(matiere: String): Int {
        return when (matiere.trim()) {
            "Mathématiques" -> R.drawable.bg_select_maths_zeus
            "Français" -> R.drawable.bg_select_francais_athena
            "SVT" -> R.drawable.bg_select_svt_poseidon
            "Histoire" -> R.drawable.bg_select_histoire_ares
            "Art/Musique", "Art" -> R.drawable.bg_select_art_aphrodite
            "Langues", "Anglais" -> R.drawable.bg_select_anglais_hermes
            "Géographie" -> R.drawable.bg_select_geographie_demeter
            "Physique-Chimie" -> R.drawable.bg_select_physique_hephaistos
            "Philo/SES" -> R.drawable.bg_select_philo_apollon
            "Vie & Projets" -> R.drawable.bg_select_vie_promethee
            else -> R.drawable.bg_olympus_dark
        }
    }

    /**
     * Mapping BGM matière → res/raw.
     * Modifié : Si la chaîne est vide ou inconnue, joue bgm_savoir.
     */
    private fun getMatiereBgmRes(matiere: String): Int {
        if (matiere.isEmpty()) return R.raw.bgm_savoir

        return when (matiere.trim()) {
            "Mathématiques" -> R.raw.bgm_select_maths_zeus
            "Français" -> R.raw.bgm_select_francais_athena
            "SVT" -> R.raw.bgm_select_svt_poseidon
            "Histoire" -> R.raw.bgm_select_histoire_ares
            "Art/Musique", "Art" -> R.raw.bgm_select_art_aphrodite
            "Langues", "Anglais" -> R.raw.bgm_select_anglais_hermes
            "Géographie" -> R.raw.bgm_select_geographie_demeter
            "Physique-Chimie" -> R.raw.bgm_select_physique_hephaistos
            "Philo/SES" -> R.raw.bgm_select_philo_apollon
            "Vie & Projets" -> R.raw.bgm_select_vie_promethee
            else -> R.raw.bgm_savoir
        }
    }

    /**
     * Applique la DA matière à l'écran Savoir.
     */
    private fun appliquerThemeMatiere(
        matiere: String,
        restartMusic: Boolean
    ) {
        matiereThemeActive = matiere

        // Si aucune matière, on utilise le premier dieu pour les défauts textuels/couleurs
        val god = PantheonConfig.findByMatiere(matiere) ?: PantheonConfig.GODS.first()

        try {
            binding.ivSavoirBackground.setImageResource(getMatiereBackgroundRes(matiere))
        } catch (_: Exception) {
        }

        // Texte dynamique selon si une matière est active ou non
        if (matiere.isEmpty()) {
            binding.tvSavoirSubtitle.text = "Choisis un dieu pour explorer ses cours"
        } else {
            binding.tvSavoirSubtitle.text = "Choisis un dieu pour explorer ses cours — ${god.divinite}"
        }

        if (restartMusic) {
            try {
                // Utilise maintenant bgm_savoir si matiere est vide
                SoundManager.playMusic(this, getMatiereBgmRes(matiere))
            } catch (_: Exception) {
            }
        }

        animatedBackgroundHelper?.start(
            accentColor = god.couleur,
            mode = OlympianParticlesView.ParticleMode.SAVOIR
        )
    }

    private fun loadCoursesAndRefreshUI() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@SavoirActivity)
                allCourses = db.iAristoteDao().getAllCourses()
                
                // PACK 2 — Récupération des 3 derniers cours pour la section dédiée.
                val recentCourses = try {
                    db.iAristoteDao().getRecentCourses(3)
                } catch (_: Exception) {
                    emptyList()
                }

                coursesCountBySubject.clear()

                for (god in PantheonConfig.GODS) {
                    val aliases = getAliasesForSubject(god.matiere)
                    val count = db.iAristoteDao().countCoursesBySubjects(aliases)
                    coursesCountBySubject[normalizeSubjectKey(god.matiere)] = count
                }

                withContext(Dispatchers.Main) {
                    // PACK 2 — Affichage savoirs récents
                    afficherSavoirsRecents(recentCourses)
                    populateGodsList()
                }
            } catch (e: Exception) {
                Log.e("REVIZEUS", "Erreur SavoirActivity: ${e.message}")
            }
        }
    }

    private fun populateGodsList() {
        val container = binding.llGodsContainer
        container.removeAllViews()

        for (god in PantheonConfig.GODS) {
            // CHANTIER 0 — Lecture sécurisée via clé canonique.
            val count = getSafeCountForSubject(god.matiere)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 32) }
                gravity = Gravity.CENTER_VERTICAL
                setPadding(32, 32, 32, 32)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#22161B24"))
                    cornerRadius = 24f
                    setStroke(2, god.couleur)
                }

                setOnClickListener {
                    try { jouerSfx(R.raw.sfx_dialogue_blip) } catch (_: Exception) {}

                    // On applique d'abord l'identité visuelle / sonore du dieu sélectionné.
                    appliquerThemeMatiere(god.matiere, restartMusic = true)

                    // CHANTIER 0 — Relecture sécurisée au clic pour éviter tout faux négatif.
                    val safeCount = getSafeCountForSubject(god.matiere)

                    if (safeCount > 0) {
                        val canonicalSubject = normalizeSubjectKey(god.matiere)
                        val aliases = getAliasesForSubject(god.matiere)

                        val intent = Intent(this@SavoirActivity, GodMatiereActivity::class.java).apply {
                            putExtra("MATIERE", god.matiere)
                            putExtra("DIVINITE", god.divinite)
                            putExtra("COULEUR", god.couleur)

                            // Anciennes clés conservées pour compatibilité
                            putExtra("SUBJECT", god.matiere)
                            putExtra("GOD_NAME", god.divinite)

                            // CHANTIER 0 — Extras non destructifs pour le filtrage robuste.
                            putExtra("CANONICAL_SUBJECT", canonicalSubject)
                            putExtra("SUBJECT_ALIASES", aliases.toTypedArray())
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            this@SavoirActivity,
                            "${god.divinite}: ${god.emptyMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            val avatarView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                    marginEnd = 24
                    gravity = Gravity.CENTER_VERTICAL
                }

                val resId = resources.getIdentifier(god.iconResName, "drawable", packageName)
                if (resId != 0) setImageResource(resId)
            }
            card.addView(avatarView)

            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            textCol.addView(TextView(this).apply {
                text = "${god.divinite} - ${god.matiere}"
                setTextColor(god.couleur)
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                try {
                    typeface = ResourcesCompat.getFont(this@SavoirActivity, R.font.exo2)
                } catch (_: Exception) {}
            })

            textCol.addView(TextView(this).apply {
                text = if (count == 0) "Aucun cours" else "$count cours"
                setTextColor(Color.parseColor("#C9D6E8"))
                textSize = 12f
            })

            card.addView(textCol)

            if (count > 0) {
                card.addView(TextView(this).apply {
                    text = ">"
                    setTextColor(god.couleur)
                    textSize = 22f
                })
            }

            container.addView(card)
        }
    }

    // CHANTIER 0 — Normalisation locale de la matière pour éviter les écarts
    // "Art" / "Art/Musique", "Anglais" / "Langues", etc.
    private fun normalizeSubjectKey(subject: String?): String {
        val cleaned = subject.orEmpty().trim()

        return when (cleaned.lowercase()) {
            "mathématiques", "mathematiques", "maths", "mathématiques / maths" -> "Mathématiques"
            "français", "francais" -> "Français"
            "svt" -> "SVT"
            "histoire" -> "Histoire"
            "art", "art/musique", "art / musique", "musique" -> "Art/Musique"
            "langues", "anglais", "english" -> "Langues"
            "géographie", "geographie" -> "Géographie"
            "physique-chimie", "physique / chimie", "physique", "chimie" -> "Physique-Chimie"
            "philo/ses", "philo / ses", "philosophie", "ses" -> "Philo/SES"
            "vie & projets", "vie et projets", "projets", "orientation" -> "Vie & Projets"
            else -> cleaned
        }
    }

    // CHANTIER 0 — Alias utilisés par le DAO pour compter correctement les cours.
    private fun getAliasesForSubject(subject: String): List<String> {
        return when (normalizeSubjectKey(subject)) {
            "Mathématiques" -> listOf("Mathématiques", "Mathematiques", "Maths", "Mathématiques / Maths")
            "Français" -> listOf("Français", "Francais")
            "SVT" -> listOf("SVT")
            "Histoire" -> listOf("Histoire")
            "Art/Musique" -> listOf("Art/Musique", "Art / Musique", "Art", "Musique")
            "Langues" -> listOf("Langues", "Anglais", "English")
            "Géographie" -> listOf("Géographie", "Geographie")
            "Physique-Chimie" -> listOf("Physique-Chimie", "Physique / Chimie", "Physique", "Chimie")
            "Philo/SES" -> listOf("Philo/SES", "Philo / SES", "Philosophie", "SES")
            "Vie & Projets" -> listOf("Vie & Projets", "Vie et Projets", "Projets", "Orientation")
            else -> listOf(subject.trim())
        }
    }

    // CHANTIER 0 — Lecture sécurisée du compteur interne.
    private fun getSafeCountForSubject(subject: String): Int {
        return coursesCountBySubject[normalizeSubjectKey(subject)] ?: 0
    }

    // ═══════════════════════════════════════════════════════════════
    // PACK 2 — SAVOIRS RÉCENTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Affiche les 7 savoirs les plus récents en haut de l'écran.
     * Format compact : icône dieu + titre tronqué + date relative
     */
    private fun afficherSavoirsRecents(recentCourses: List<CourseEntry>) {
        binding.layoutSavoirsRecents.removeAllViews()
        
        if (recentCourses.isEmpty()) {
            binding.layoutSavoirsRecents.visibility = View.GONE
            return
        }
        
        binding.layoutSavoirsRecents.visibility = View.VISIBLE
        
        val header = TextView(this).apply {
            text = "✦ SAVOIRS RÉCENTS ✦"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            try {
                typeface = ResourcesCompat.getFont(this@SavoirActivity, R.font.cinzel)
            } catch (_: Exception) {
                typeface = Typeface.DEFAULT_BOLD
            }
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(12))
        }
        binding.layoutSavoirsRecents.addView(header)
        
        recentCourses.forEach { course ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(6)
                }
                try {
                    setBackgroundResource(R.drawable.bg_hud_chip)
                } catch (_: Exception) {
                    setBackgroundColor(Color.parseColor("#22FFFFFF"))
                }
                isClickable = true
                isFocusable = true
            }
            
            val icon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply {
                    marginEnd = dp(10)
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                // PACK 2 — Icône dieu (fallback robuste)
                try {
                    when (course.subject) {
                        "Mathématiques" -> setImageResource(R.drawable.ic_zeus_mini)
                        "Français" -> setImageResource(R.drawable.ic_athena_mini)
                        "SVT" -> setImageResource(R.drawable.ic_poseidon_mini)
                        "Histoire" -> setImageResource(R.drawable.ic_ares_mini)
                        "Art/Musique", "Art" -> setImageResource(R.drawable.ic_aphrodite_mini)
                        "Langues", "Anglais" -> setImageResource(R.drawable.ic_hermes_mini)
                        "Géographie" -> setImageResource(R.drawable.ic_demeter_mini)
                        "Physique-Chimie" -> setImageResource(R.drawable.ic_hephaistos_mini)
                        "Philo/SES" -> setImageResource(R.drawable.ic_apollon_mini)
                        "Vie & Projets" -> setImageResource(R.drawable.ic_prometheus_mini)
                        else -> setImageResource(R.drawable.ic_zeus_mini)
                    }
                } catch (_: Exception) {
                    setImageResource(R.drawable.ic_zeus_mini)
                }
            }
            card.addView(icon)
            
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            val title = TextView(this).apply {
                text = course.displayTitle().take(30) + if (course.displayTitle().length > 30) "…" else ""
                setTextColor(Color.parseColor("#FFFFFF"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = Typeface.DEFAULT_BOLD
            }
            textCol.addView(title)
            
            val dateText = TextView(this).apply {
                text = formatDateRelative(course.dateAdded)
                setTextColor(Color.parseColor("#AAAAAA"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            }
            textCol.addView(dateText)
            
            card.addView(textCol)
            
            card.setOnClickListener {
                try { jouerSfx(R.raw.sfx_dialogue_blip) } catch (_: Exception) {}
                ouvrirSavoirRecent(course)
            }
            
            binding.layoutSavoirsRecents.addView(card)
        }
    }

    /**
     * Ouvre le temple de la matière du savoir récent et déclenche l'ouverture
     * automatique du bon résumé via l'extra OPEN_COURSE_ID.
     */
    private fun ouvrirSavoirRecent(course: CourseEntry) {
        val canonicalSubject = normalizeSubjectKey(course.subject)
        val aliases = getAliasesForSubject(canonicalSubject)
        val god = PantheonConfig.findByMatiere(canonicalSubject)

        val intent = Intent(this@SavoirActivity, GodMatiereActivity::class.java).apply {
            putExtra("MATIERE", canonicalSubject)
            putExtra("DIVINITE", god?.divinite ?: "")
            putExtra("COULEUR", god?.couleur ?: Color.parseColor("#FFD700"))
            putExtra("SUBJECT", canonicalSubject)
            putExtra("GOD_NAME", god?.divinite ?: "")
            putExtra("CANONICAL_SUBJECT", canonicalSubject)
            putExtra("SUBJECT_ALIASES", aliases.toTypedArray())
            putExtra("OPEN_COURSE_ID", course.id)
        }
        startActivity(intent)
    }

    private fun formatDateRelative(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            seconds < 60 -> "à l'instant"
            minutes < 60 -> "il y a ${minutes}min"
            hours < 24 -> "il y a ${hours}h"
            days == 1L -> "hier"
            days < 7 -> "il y a ${days}j"
            else -> {
                val date = java.util.Date(timestamp)
                java.text.SimpleDateFormat("dd MMM", java.util.Locale.FRENCH).format(date)
            }
        }
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
    ).toInt()
}

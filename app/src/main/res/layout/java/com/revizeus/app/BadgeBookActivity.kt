package com.revizeus.app

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.databinding.ActivityBadgeBookBinding
import kotlinx.coroutines.launch

/**
 * BadgeBookActivity — Version Premium par Catégories (Icônes masquées si verrouillées)
 *
 * AJOUT :
 * - BGM dédiée : bgm_badge
 * - La musique est jouée uniquement sur cet écran
 * - arrêt propre dans onPause()
 */
class BadgeBookActivity : BaseActivity() {

    private lateinit var binding: ActivityBadgeBookBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Respect de la charte : Format Portrait obligatoire
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        binding = ActivityBadgeBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.post {
            TutorialManager.showHeroTutorialIfNeeded(
                activity = this,
                stepId = "badge_book_first_entry_v2",
                godId = "athena",
                title = "🏅 Livre des Badges 🏅",
                message = "Voici le registre des exploits du héros actif. Chaque badge raconte une prouesse précise. Certains sont encore masqués tant que tu n'as pas accompli leur rituel."
            )
        }

        binding.btnBackBadgeBook.setOnClickListener { finish() }

        lifecycleScope.launch {
            val (debloques, total, _) = BadgeManager.getResume(this@BadgeBookActivity)
            binding.tvBadgeBookResume.text = "$debloques / $total"
            genererInterfaceBadges()
        }
    }

    /**
     * ▶ Lecture BGM spécifique à cet écran
     * bgm_badge ne doit jouer que dans le Livre des Badges
     */
    override fun onResume() {
        super.onResume()
        SoundManager.playMusic(this, R.raw.bgm_badge)
    }

    /**
     * ▶ Stop musique en quittant l'écran
     * évite qu'elle continue sur d'autres Activities
     */
    override fun onPause() {
        super.onPause()
        SoundManager.stopMusic()
    }

    private fun genererInterfaceBadges() {
        binding.containerBadgeBook.removeAllViews()

        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels - (28 * density).toInt()
        val cellSize = (screenWidth / 3).toInt()

        val allBadges = BadgeCatalogue.tous
        val unlockedIds = BadgeManager.getUnlockedIds(this)

        BadgeCategorie.entries.forEach { categorie ->
            val badgesDeLaCategorie = allBadges.filter { it.categorie == categorie }

            if (badgesDeLaCategorie.isNotEmpty()) {

                val separator = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (1.5f * density).toInt()
                    ).apply {
                        setMargins(0, (24 * density).toInt(), 0, (12 * density).toInt())
                    }
                    setBackgroundColor(Color.parseColor("#FFD700"))
                }
                binding.containerBadgeBook.addView(separator)

                val header = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (46 * density).toInt()
                    )
                    background = ContextCompat.getDrawable(this@BadgeBookActivity, R.drawable.bg_temple_button)
                    gravity = Gravity.CENTER
                }

                val title = TextView(this).apply {
                    text = "${categorie.emoji}  ${categorie.label.uppercase()}"
                    setTextColor(Color.parseColor("#FFD700"))
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                }

                header.addView(title)
                binding.containerBadgeBook.addView(header)

                val grid = GridLayout(this).apply {
                    columnCount = 3
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, (16 * density).toInt(), 0, 0)
                    }
                }

                badgesDeLaCategorie.forEach { badge ->
                    val isUnlocked = badge.id in unlockedIds
                    grid.addView(creerCelluleBadge(badge, isUnlocked, cellSize))
                }

                binding.containerBadgeBook.addView(grid)
            }
        }
    }

    private fun creerCelluleBadge(badge: BadgeDefinition, isUnlocked: Boolean, cellSize: Int): LinearLayout {
        val density = resources.displayMetrics.density

        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = GridLayout.LayoutParams().apply {
                width = cellSize
                height = GridLayout.LayoutParams.WRAP_CONTENT
            }
            setPadding(0, (8 * density).toInt(), 0, (12 * density).toInt())
            setOnClickListener { afficherDetailBadge(badge, isUnlocked) }
        }

        val frameStack = RelativeLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams((70 * density).toInt(), (70 * density).toInt())
        }

        val ivFrame = ImageView(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            setImageResource(badge.rarete.frameDrawable)
            alpha = if (isUnlocked) 1.0f else 0.3f
        }

        val ivIcon = ImageView(this).apply {
            val margin = (14 * density).toInt()
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            ).apply { setMargins(margin, margin, margin, margin) }

            if (isUnlocked) {
                setImageResource(badge.iconDrawable)
            } else {
                setImageResource(R.drawable.ic_lock_placeholder)
                alpha = 0.5f
            }
        }

        frameStack.addView(ivFrame)
        frameStack.addView(ivIcon)

        val tvLabel = TextView(this).apply {
            text = if (isUnlocked) badge.nom else "???"
            setTextColor(if (isUnlocked) Color.parseColor("#EAD8A3") else Color.parseColor("#5A5240"))
            textSize = 10f
            gravity = Gravity.CENTER
            setPadding((4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt(), 0)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }

        item.addView(frameStack)
        item.addView(tvLabel)

        return item
    }

    private fun afficherDetailBadge(badge: BadgeDefinition, isUnlocked: Boolean) {
        val msg = buildString {
            append("Catégorie : ${badge.categorie.emoji} ${badge.categorie.label}\n")
            append("Rareté : ${badge.rarete.label}\n\n")
            append(if (isUnlocked) badge.description else badge.descriptionBloquee)
            if (isUnlocked && badge.xpRecompense > 0) {
                append("\n\nRécompense obtenue : +${badge.xpRecompense} XP")
            }
        }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(if (isUnlocked) badge.nom else "Badge Verrouillé")
            .setMessage(msg)
            .setPositiveButton("Honneur au Panthéon", null)
            .show()
    }
}

package com.revizeus.app

import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.databinding.ActivityInventoryBinding
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.InventoryItem
import com.revizeus.app.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InventoryActivity : BaseActivity() {

    private lateinit var binding: ActivityInventoryBinding
    private var currentSortMode: String = "OBTENTION"
    private var cachedProfile: UserProfile? = null
    private var cachedInventory: List<InventoryItem> = emptyList()
    private var currentTab: String = "FRAGMENTS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        binding = ActivityInventoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.post {
            TutorialManager.showHeroTutorialIfNeeded(
                activity = this,
                stepId = "inventory_first_entry_v2",
                godId = "hephaestus",
                title = "🎒 Sac et reliques 🎒",
                message = "Le sac rassemble fragments, artefacts, équipements, objets et reliques. Change d'onglet pour explorer chaque catégorie, puis trie le contenu pour lire plus vite l'état réel de tes possessions."
            )
        }

        binding.btnBackInventory.setOnClickListener {
            try { SoundManager.playSFX(this, R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            finish()
        }

        // Les anciens onglets du haut restent dans le layout pour préserver le ViewBinding,
        // mais on les masque afin d'utiliser la ligne de chips colorée comme navigation principale.
        binding.scrollInventoryTabs.visibility = View.GONE

        binding.tabFragments.setOnClickListener {
            jouerSfxTabSafe()
            currentTab = "FRAGMENTS"
            styliserOnglets()
            rafraichirContenu()
        }

        binding.tabArtefacts.setOnClickListener {
            jouerSfxTabSafe()
            currentTab = "ARTEFACTS"
            styliserOnglets()
            rafraichirContenu()
        }

        binding.tabEquipements.setOnClickListener {
            jouerSfxTabSafe()
            currentTab = "ÉQUIPEMENTS"
            styliserOnglets()
            rafraichirContenu()
        }

        binding.tabObjets.setOnClickListener {
            jouerSfxTabSafe()
            currentTab = "OBJETS"
            styliserOnglets()
            rafraichirContenu()
        }

        binding.tabReliques.setOnClickListener {
            jouerSfxTabSafe()
            currentTab = "RELIQUES"
            styliserOnglets()
            rafraichirContenu()
        }

        binding.btnSortMenu.setOnClickListener {
            jouerSfxTabSafe()
            afficherMenuTri()
        }

        styliserOnglets()
        mettreAJourLibelleTri()
        configurerChipsCommeNavigationPrincipale()
        chargerDonnees()
    }

    override fun onResume() {
        super.onResume()
        try {
            SoundManager.rememberMusic(R.raw.bgm_inventaire)
            SoundManager.playMusic(this, R.raw.bgm_inventaire)
        } catch (_: Exception) {
        }
        chargerDonnees()
    }

    override fun onPause() {
        super.onPause()
        try {
            SoundManager.stopMusic()
        } catch (_: Exception) {
        }
    }

    /**
     * Réutilise la ligne de chips colorée comme vraie navigation principale.
     * Cela supprime visuellement le doublon d'onglets sans casser l'architecture existante.
     */
    private fun configurerChipsCommeNavigationPrincipale() {
        try {
            binding.chipTous.text = "FRAGMENTS"
            binding.chipBouclier.text = "ARTEFACTS"
            binding.chipArme.text = "ÉQUIPEMENTS"
            binding.chipArtefact.text = "OBJETS"
            binding.chipParchemin.text = "RELIQUES"
            binding.chipOffrande.visibility = View.GONE

            binding.chipGroupFilters.setOnCheckedChangeListener { _, checkedId ->
                jouerSfxTabSafe()
                currentTab = when (checkedId) {
                    R.id.chipTous -> "FRAGMENTS"
                    R.id.chipBouclier -> "ARTEFACTS"
                    R.id.chipArme -> "ÉQUIPEMENTS"
                    R.id.chipArtefact -> "OBJETS"
                    R.id.chipParchemin -> "RELIQUES"
                    else -> "FRAGMENTS"
                }
                styliserOnglets()
                rafraichirContenu()
            }

            binding.chipTous.isChecked = true
        } catch (_: Exception) {
        }
    }

    private fun afficherMenuTri() {
        val popup = PopupMenu(this, binding.btnSortMenu)
        popup.menu.add(0, 1, 0, "Ordre d'obtention")
        popup.menu.add(0, 2, 1, "Alphabétique")
        popup.menu.add(0, 3, 2, "Rareté")
        popup.menu.add(0, 4, 3, "Quantité")
        popup.setOnMenuItemClickListener { item ->
            currentSortMode = when (item.itemId) {
                2 -> "ALPHABETIQUE"
                3 -> "RARETE"
                4 -> "QUANTITE"
                else -> "OBTENTION"
            }
            mettreAJourLibelleTri()
            rafraichirContenu()
            true
        }
        popup.show()
    }

    private fun mettreAJourLibelleTri() {
        binding.tvSortCurrent.text = when (currentSortMode) {
            "ALPHABETIQUE" -> "Tri : Alphabétique"
            "RARETE" -> "Tri : Rareté"
            "QUANTITE" -> "Tri : Quantité"
            else -> "Tri : Obtention"
        }
    }

    private fun chargerDonnees() {
        lifecycleScope.launch {
            val pair = withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(this@InventoryActivity)
                val profile = try { db.iAristoteDao().getUserProfile() } catch (_: Exception) { null }
                val inventory = try { db.iAristoteDao().getInventory() } catch (_: Exception) { emptyList() }
                profile to inventory
            }
            cachedProfile = pair.first
            cachedInventory = pair.second
            rafraichirContenu()
        }
    }

    private fun rafraichirContenu() {
        binding.inventoryContainer.removeAllViews()

        when (currentTab) {
            "FRAGMENTS" -> afficherFragments(cachedProfile)
            "ARTEFACTS", "ÉQUIPEMENTS", "OBJETS", "RELIQUES" -> {
                // PACK 2 — Tous les onglets utilisent afficherInventaire avec filtres
                afficherInventaire(cachedInventory)
            }
        }
    }

    private fun afficherFragments(profile: UserProfile?) {
        val container = binding.inventoryContainer

        if (profile == null) {
            container.addView(createPlaceholderText("Aucun profil trouvé dans les archives du héros."))
            return
        }

        val json = try {
            JSONObject(profile.knowledgeFragments.ifBlank { "{}" })
        } catch (_: Exception) {
            JSONObject()
        }

        val fragments = mutableListOf<FragmentUiItem>()
        val iterator = json.keys()
        while (iterator.hasNext()) {
            val matiere = iterator.next()
            val count = json.optInt(matiere, 0)
            if (count > 0) {
                fragments.add(
                    FragmentUiItem(
                        matiere = matiere,
                        objectName = buildFragmentObjectName(matiere),
                        iconRes = KnowledgeFragmentManager.getFragmentIconRes(this, matiere),
                        quantity = count,
                        rarete = computeFragmentRarete(count),
                        obtainedAt = count.toLong()
                    )
                )
            }
        }

        if (fragments.isEmpty()) {
            container.addView(createPlaceholderText("Aucun fragment n'a encore été forgé. Réussis des quiz pour remplir ce sac."))
            return
        }

        val sorted = when (currentSortMode) {
            "ALPHABETIQUE" -> fragments.sortedBy { it.objectName.lowercase(Locale.ROOT) }
            "RARETE" -> fragments.sortedWith(compareByDescending<FragmentUiItem> { rareteWeight(it.rarete) }.thenBy { it.objectName.lowercase(Locale.ROOT) })
            "QUANTITE" -> fragments.sortedWith(compareByDescending<FragmentUiItem> { it.quantity }.thenBy { it.objectName.lowercase(Locale.ROOT) })
            else -> fragments.sortedWith(compareByDescending<FragmentUiItem> { it.obtainedAt }.thenBy { it.objectName.lowercase(Locale.ROOT) })
        }

        addSectionTitle("FRAGMENTS")
        renderTwoColumnFragmentGrid(sorted)
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * PACK 2 — AFFICHAGE INVENTAIRE AVEC FILTRES + FAVORI + DATE
     * ═══════════════════════════════════════════════════════════════
     */
    private fun afficherInventaire(items: List<InventoryItem>) {
        val container = binding.inventoryContainer
        
        val filteredItems = when (currentTab) {
            "ARTEFACTS" -> items.filter { it.type.equals("ARTEFACT", ignoreCase = true) }
            "ÉQUIPEMENTS" -> items.filter {
                it.type.equals("BOUCLIER", ignoreCase = true) ||
                it.type.equals("ARME", ignoreCase = true) ||
                it.type.equals("EQUIPEMENT", ignoreCase = true) ||
                it.type.equals("ÉQUIPEMENT", ignoreCase = true)
            }
            "OBJETS" -> items.filter {
                it.type.equals("OFFRANDE", ignoreCase = true) ||
                it.type.equals("OBJET", ignoreCase = true)
            }
            "RELIQUES" -> items.filter {
                it.type.equals("PARCHEMIN", ignoreCase = true) ||
                it.type.equals("RELIQUE", ignoreCase = true)
            }
            else -> items
        }
        
        // PACK 2 — Tri avec favoris prioritaires
        val sortedItems = filteredItems.sortedWith(
            compareByDescending<InventoryItem> { it.isFavorite } // Favoris en haut
                .then(when (currentSortMode) {
                    "ALPHABETIQUE" -> compareBy { it.name.lowercase() }
                    "RARETE" -> compareByDescending { it.rareteWeight() }
                    "QUANTITE" -> compareByDescending { it.quantity }
                    else -> compareByDescending { it.obtainedAt }
                })
        )
        
        if (sortedItems.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Aucun objet dans cette catégorie"
                setTextColor(Color.parseColor("#888888"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(32), dp(16), dp(32))
            }
            container.addView(empty)
            return
        }
        
        sortedItems.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }
                try {
                    setBackgroundResource(R.drawable.bg_rpg_dialog)
                } catch (_: Exception) {
                    setBackgroundColor(Color.parseColor("#1A1A2E"))
                }
                elevation = dp(2).toFloat()
                isClickable = true
                isFocusable = true
                
                // PACK 2 — Long press pour toggle favori
                setOnLongClickListener {
                    toggleFavorite(item)
                    true
                }
            }
            
            // Icône objet
            val icon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply {
                    marginEnd = dp(12)
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                val iconRes = resources.getIdentifier(item.imageResName, "drawable", packageName)
                if (iconRes != 0) setImageResource(iconRes)
                else try { setImageResource(R.drawable.ic_forge_hephaistos) } catch (_: Exception) {}
            }
            card.addView(icon)
            
            // Colonne texte
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            // Ligne nom + étoile favori
            val nameRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            
            val name = TextView(this).apply {
                text = item.name
                setTextColor(Color.parseColor("#FFD700"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            nameRow.addView(name)
            
            // PACK 2 — Étoile favori
            if (item.isFavorite) {
                val star = TextView(this).apply {
                    text = "⭐"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setPadding(dp(6), 0, 0, 0)
                }
                nameRow.addView(star)
            }
            
            textCol.addView(nameRow)
            
            // Type + rareté
            val meta = TextView(this).apply {
                text = "${item.type} • ${item.rarete}"
                setTextColor(Color.parseColor("#AAAAAA"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            }
            textCol.addView(meta)
            
            // PACK 2 — Date obtention
            val date = TextView(this).apply {
                text = "Obtenu ${formatDateRelative(item.obtainedAt)}"
                setTextColor(Color.parseColor("#888888"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setPadding(0, dp(2), 0, 0)
            }
            textCol.addView(date)
            
            card.addView(textCol)
            
            // Quantité
            val qty = TextView(this).apply {
                text = "×${item.quantity}"
                setTextColor(if (item.quantity > 0) Color.parseColor("#FFD700") else Color.parseColor("#555555"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(12), 0, 0, 0)
            }
            card.addView(qty)
            
            card.setOnClickListener {
                try { jouerSfx(R.raw.sfx_dialogue_blip) } catch (_: Exception) {}
                afficherDetailObjet(item)
            }
            
            container.addView(card)
        }
    }

    private fun afficherSoonCard(titre: String, description: String) {
        addSectionTitle(titre)
        binding.inventoryContainer.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(18), dp(20), dp(18), dp(20))
                try {
                    setBackgroundResource(R.drawable.bg_hero_panel)
                } catch (_: Exception) {
                    setBackgroundColor(Color.parseColor("#33111122"))
                }

                addView(TextView(this@InventoryActivity).apply {
                    text = "ARRIVE BIENTÔT"
                    setTextColor(Color.parseColor("#FFD700"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                })

                addView(TextView(this@InventoryActivity).apply {
                    text = description
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    gravity = Gravity.CENTER
                    setPadding(0, dp(10), 0, 0)
                })
            }
        )
    }

    private fun renderTwoColumnFragmentGrid(items: List<FragmentUiItem>) {
        val container = binding.inventoryContainer
        var currentRow: LinearLayout? = null

        items.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(12)
                    }
                }
                container.addView(currentRow!!)
            }

            currentRow?.addView(createFragmentItemCard(item))
        }
    }

    private fun createFragmentItemCard(item: FragmentUiItem): LinearLayout {
        val accentColor = KnowledgeFragmentManager.getFragmentColorInt(item.matiere)

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(6)
                marginEnd = dp(6)
            }
            setPadding(dp(10), dp(10), dp(10), dp(10))

            try {
                setBackgroundResource(R.drawable.bg_hero_panel)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#33111122"))
            }

            addView(FrameLayout(this@InventoryActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(88), dp(88))

                addView(ImageView(this@InventoryActivity).apply {
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    if (item.iconRes != 0) {
                        setImageResource(item.iconRes)
                    }
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    alpha = 0.92f
                })

                addView(TextView(this@InventoryActivity).apply {
                    text = item.quantity.toString()
                    setTextColor(accentColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(dp(34), dp(34), Gravity.BOTTOM or Gravity.END)
                    try {
                        setBackgroundResource(R.drawable.badge_count)
                    } catch (_: Exception) {
                        setBackgroundColor(Color.parseColor("#CC1A1A2E"))
                    }
                })
            })

            addView(TextView(this@InventoryActivity).apply {
                text = item.objectName
                setTextColor(Color.parseColor("#FFD700"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, 0)
            })

            addView(TextView(this@InventoryActivity).apply {
                text = item.rarete
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            })

            setOnClickListener { afficherDetailsFragment(item) }
        }
    }

    private fun createInventoryItemRow(item: InventoryItem): LinearLayout {
        val imageRes = resources.getIdentifier(item.imageResName, "drawable", packageName)

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
            try {
                setBackgroundResource(R.drawable.bg_hero_panel)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#33111122"))
            }

            addView(FrameLayout(this@InventoryActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(76), dp(76))
                addView(ImageView(this@InventoryActivity).apply {
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    if (imageRes != 0) setImageResource(imageRes)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                })
                addView(TextView(this@InventoryActivity).apply {
                    text = "x${item.quantity}"
                    setTextColor(Color.parseColor("#FFD700"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(dp(34), dp(34), Gravity.BOTTOM or Gravity.END)
                    try {
                        setBackgroundResource(R.drawable.badge_count)
                    } catch (_: Exception) {
                        setBackgroundColor(Color.parseColor("#CC1A1A2E"))
                    }
                })
            })

            addView(LinearLayout(this@InventoryActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(14)
                }

                addView(TextView(this@InventoryActivity).apply {
                    text = item.name
                    setTextColor(Color.parseColor("#FFD700"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    typeface = Typeface.DEFAULT_BOLD
                })

                addView(TextView(this@InventoryActivity).apply {
                    text = "${item.type} • ${item.rarete}"
                    setTextColor(Color.parseColor("#CCFFFFFF"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(0, dp(4), 0, 0)
                })

                addView(TextView(this@InventoryActivity).apply {
                    text = "Obtenu : ${formatDate(item.obtainedAt)}"
                    setTextColor(Color.parseColor("#B8B8B8"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(0, dp(4), 0, 0)
                })
            })

            setOnClickListener { afficherDetailsObjet(item) }
        }
    }

    private fun afficherDetailsObjet(item: InventoryItem) {
        val message = buildString {
            append(item.description)
            append("\n\n")
            append("Type : ${item.type}\n")
            append("Rareté : ${item.rarete}\n")
            append("Quantité : ${item.quantity}\n")
            append("Obtenu : ${formatDate(item.obtainedAt)}")
        }

        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setMessage(message)
            .setPositiveButton("Fermer", null)
            .show()
    }

    private fun afficherDetailsFragment(item: FragmentUiItem) {
        val message = buildString {
            append("Objet : ${item.objectName}\n")
            append("Matière liée : ${item.matiere}\n")
            append("Quantité : ${item.quantity}\n")
            append("Rareté affichée : ${item.rarete}\n\n")
            append("Ces fragments proviennent des quiz réussis et servent à la Forge d'Héphaïstos.")
        }

        AlertDialog.Builder(this)
            .setTitle(item.objectName)
            .setMessage(message)
            .setPositiveButton("Fermer", null)
            .show()
    }

    private fun addSectionTitle(title: String) {
        binding.inventoryContainer.addView(TextView(this).apply {
            text = title
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(2), 0, dp(14))
        })
    }

    private fun createPlaceholderText(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(30), dp(18), dp(18))
        }
    }

    private fun buildFragmentObjectName(matiere: String): String {
        val god = PantheonConfig.findByMatiere(matiere)?.divinite ?: "Olympien"
        return "Fragment de $god"
    }

    private fun computeFragmentRarete(count: Int): String {
        return when {
            count >= 50 -> "LÉGENDAIRE"
            count >= 25 -> "ÉPIQUE"
            count >= 10 -> "RARE"
            else -> "COMMUN"
        }
    }

    private fun rareteWeight(value: String): Int {
        return when (value.trim().uppercase()) {
            "LÉGENDAIRE", "LEGENDAIRE" -> 4
            "ÉPIQUE", "EPIQUE" -> 3
            "RARE" -> 2
            else -> 1
        }
    }

    private fun styliserOnglets() {
        val active = 1f
        val inactive = 0.68f

        binding.tabFragments.alpha = if (currentTab == "FRAGMENTS") active else inactive
        binding.tabArtefacts.alpha = if (currentTab == "ARTEFACTS") active else inactive
        binding.tabEquipements.alpha = if (currentTab == "ÉQUIPEMENTS") active else inactive
        binding.tabObjets.alpha = if (currentTab == "OBJETS") active else inactive
        binding.tabReliques.alpha = if (currentTab == "RELIQUES") active else inactive

        try {
            binding.chipTous.alpha = if (currentTab == "FRAGMENTS") active else inactive
            binding.chipBouclier.alpha = if (currentTab == "ARTEFACTS") active else inactive
            binding.chipArme.alpha = if (currentTab == "ÉQUIPEMENTS") active else inactive
            binding.chipArtefact.alpha = if (currentTab == "OBJETS") active else inactive
            binding.chipParchemin.alpha = if (currentTab == "RELIQUES") active else inactive
        } catch (_: Exception) {
        }
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp <= 0L) return "Inconnue"
        return DateFormat.format("dd/MM/yyyy HH:mm", Date(timestamp)).toString()
    }

    private fun jouerSfxTabSafe() {
        try {
            SoundManager.playSFXLow(this, R.raw.sfx_settings_tab)
        } catch (_: Exception) {
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * PACK 2 — TOGGLE FAVORI
     * ═══════════════════════════════════════════════════════════════
     */
    private fun toggleFavorite(item: InventoryItem) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    item.isFavorite = !item.isFavorite
                    AppDatabase.getDatabase(this@InventoryActivity)
                        .iAristoteDao()
                        .updateInventoryItem(item)
                } catch (e: Exception) {
                    android.util.Log.e("INVENTORY", "Erreur toggle favori : ${e.message}")
                }
            }
            
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            android.widget.Toast.makeText(
                this@InventoryActivity,
                if (item.isFavorite) "⭐ ${item.name} ajouté aux favoris" else "${item.name} retiré des favoris",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            
            chargerDonnees()
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * PACK 2 — DIALOGUE DÉTAIL OBJET
     * ═══════════════════════════════════════════════════════════════
     */
    private fun afficherDetailObjet(item: InventoryItem) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#12111E"))
            }
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64)).apply {
                marginEnd = dp(14)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            val iconRes = resources.getIdentifier(item.imageResName, "drawable", packageName)
            if (iconRes != 0) setImageResource(iconRes)
            else try { setImageResource(R.drawable.ic_forge_hephaistos) } catch (_: Exception) {}
        }
        header.addView(icon)
        
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        titleCol.addView(TextView(this).apply {
            text = item.name
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
        })
        
        titleCol.addView(TextView(this).apply {
            text = "${item.type} • ${item.rarete}"
            setTextColor(Color.parseColor("#AAAAAA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        })
        
        header.addView(titleCol)
        root.addView(header)
        
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
            setBackgroundColor(Color.parseColor("#33FFD700"))
        })
        
        root.addView(TextView(this).apply {
            text = item.description
            setTextColor(Color.parseColor("#C8C8C8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setLineSpacing(dp(3).toFloat(), 1f)
        })
        
        root.addView(TextView(this).apply {
            text = "Obtenu le : ${formatDateComplete(item.obtainedAt)}"
            setTextColor(Color.parseColor("#888888"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(12), 0, 0)
            typeface = Typeface.defaultFromStyle(Typeface.ITALIC)
        })
        
        root.addView(TextView(this).apply {
            text = "Quantité possédée : ×${item.quantity}"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(8), 0, 0)
        })
        
        val btnClose = TextView(this).apply {
            text = "COMPRIS"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#0A0A14"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            try {
                typeface = androidx.core.content.res.ResourcesCompat.getFont(this@InventoryActivity, R.font.cinzel)
                setBackgroundResource(R.drawable.bg_temple_button)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#FFD700"))
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }
            isClickable = true
            isFocusable = true
        }
        root.addView(btnClose)
        
        val dialog = AlertDialog.Builder(this)
            .setView(root)
            .setCancelable(true)
            .create()
        
        btnClose.setOnClickListener {
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            dialog.dismiss()
        }
        
        try {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        } catch (_: Exception) {}
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * PACK 2 — FORMATAGE DATES
     * ═══════════════════════════════════════════════════════════════
     */
    private fun formatDateComplete(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd MMMM yyyy 'à' HH'h'mm", Locale.FRENCH)
        return format.format(date)
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
                val date = Date(timestamp)
                SimpleDateFormat("dd MMM", Locale.FRENCH).format(date)
            }
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
    }

    private data class FragmentUiItem(
        val matiere: String,
        val objectName: String,
        val iconRes: Int,
        val quantity: Int,
        val rarete: String,
        val obtainedAt: Long
    )
}

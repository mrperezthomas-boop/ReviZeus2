package com.revizeus.app

import com.revizeus.app.models.InventoryItem
import com.revizeus.app.models.UserProfile

/**
 * ═══════════════════════════════════════════════════════════════
 * CraftingSystem.kt — RéviZeus — PHASE C : LA FORGE D'HÉPHAÏSTOS
 * ═══════════════════════════════════════════════════════════════
 *
 * MISE À JOUR v2 — IDENTITÉ VISUELLE :
 *   ✅ 4 icônes spécifiques pour les artefacts visuels :
 *        ic_bouclier_logique  (shield_logic   — symbole π)
 *        ic_sandales_hermes   (bow_language   — ailes ailées)
 *        ic_plume_athena      (sword_wisdom   — plume sagesse)
 *        ic_diademe_aphrodite (artifact_poet  — diadème cœur)
 *   ✅ 3 parchemins ont chacun leur icône (plus de partage ic_scroll_oracle)
 *   ✅ Prompts JSON de génération IA en bas de fichier (bloc ICON_PROMPTS)
 *
 * Moteur de recettes de la Forge d'Héphaïstos.
 * Manager statique (object) — aucune logique UI, aucun accès DB direct.
 *
 * Il centralise :
 *   - la définition de la data class Recipe
 *   - le catalogue availableRecipes (toutes les recettes du jeu)
 *   - la logique de vérification d'affordabilité (canAfford)
 *   - la logique de déduction des fragments (deductCost)
 *   - la construction de l'InventoryItem résultant (buildItemFromRecipe)
 *   - les helpers de filtrage/recherche de recettes
 *
 * PRINCIPE DES FRAGMENTS DE CONNAISSANCE :
 *   Chaque bonne réponse en quiz = +1 Fragment de la matière concernée.
 *   Les fragments sont le seul ingrédient de la Forge.
 *   Ils sont stockés en JSON dans UserProfile.knowledgeFragments.
 *   CraftingSystem les lit et les modifie via les helpers de UserProfile.
 *
 * CLÉS DE MATIÈRES (doivent correspondre exactement aux libellés des quiz) :
 *   "Mathématiques" | "Français" | "SVT" | "Histoire" | "Art/Musique"
 *   "Langues" | "Géographie" | "Physique-Chimie" | "Philo/SES" | "Vie & Projets"
 *
 * ÉVOLUTION FUTURE :
 *   - Recettes "FORGE_ULTIME" multi-matières pour joueurs polyvalents
 *   - Recettes saisonnières déverrouillées par des événements (Olympiades)
 *   - Parchemins à durabilité limitée (usages = 1, 3, 5...)
 *   - Génération de recettes personnalisées via Gemini (points faibles détectés)
 *   - Relier les objets craftés à des bonus mesurables en quiz (SettingsManager)
 *   - Badge "Premier Craft" déclenché depuis ForgeActivity
 * ═══════════════════════════════════════════════════════════════
 */
object CraftingSystem {

    // ══════════════════════════════════════════════════════════
    // DATA CLASS — RECETTE
    // ══════════════════════════════════════════════════════════

    /**
     * Représente une recette de craft de la Forge.
     *
     * @param id            Identifiant unique stable. Utilisé pour comparaisons,
     *                      persistances futures et liaison badge/événement.
     * @param name          Nom d'affichage de l'objet résultant.
     * @param type          Catégorie de l'objet : "BOUCLIER", "ARME", "ARTEFACT",
     *                      "PARCHEMIN" ou "OFFRANDE".
     * @param description   Description narrative et pédagogique de l'objet.
     * @param lore          Citation divine ou texte de lore immersif (optionnel,
     *                      affiché sous la description dans ForgeActivity).
     * @param cost          Map<matière, quantité de fragments> requise pour crafter.
     *                      Ex : mapOf("Mathématiques" to 50) = 50 fragments de Maths.
     *                      Plusieurs matières possibles pour les objets rares.
     * @param resultResName Nom de la ressource drawable de l'objet crafté.
     *                      Résolu dynamiquement par getIdentifier() — jamais R.drawable.xxx.
     */
    data class Recipe(
        val id: String,
        val name: String,
        val type: String,
        val description: String,
        val lore: String = "",
        val cost: Map<String, Int>,
        val resultResName: String
    )

    // ══════════════════════════════════════════════════════════
    // CATALOGUE DES RECETTES
    // ══════════════════════════════════════════════════════════

    /**
     * Toutes les recettes disponibles dans la Forge.
     *
     * CALIBRAGE DES COÛTS :
     *   1 fragment = 1 bonne réponse (quiz de 20 questions → max 20 fragments).
     *   Un quiz parfait en Maths = 20 fragments de Mathématiques.
     *   Bouclier de Logique à 50 = ~2,5 quiz parfaits, objectif atteignable.
     *   Les PARCHEMINS (consommables) coûtent moins cher que les BOUCLIERS.
     *   Les ARTEFACTS ont des coûts mixtes pour valoriser la polyvalence.
     *
     * ÉVOLUTION : cette liste peut évoluer sans migration Room
     * (les recettes ne sont pas en base, seulement les items craftés le sont).
     */
    val availableRecipes: List<Recipe> = listOf(

        // ── BOUCLIERS ──────────────────────────────────────────────────────────

        Recipe(
            id = "shield_logic",
            name = "Bouclier de Logique",
            type = "BOUCLIER",
            description = "Forgé des éclats de raisonnement pur. Symbole de la maîtrise " +
                "des Mathématiques et de la pensée structurée.",
            lore = "« Héphaïstos l'a trempé dans l'axiome et refroidi dans la preuve. »",
            cost = mapOf("Mathématiques" to 50),
            resultResName = "ic_bouclier_logique"
        ),

        Recipe(
            id = "shield_memory",
            name = "Égide de Mnémosyne",
            type = "BOUCLIER",
            description = "Taillée dans les fragments de récits et de mémoire. Symbole de la " +
                "maîtrise de l'Histoire et du Français réunies.",
            lore = "« Mnémosyne elle-même a soufflé sur les rivets dorés. »",
            cost = mapOf("Histoire" to 40, "Français" to 20),
            resultResName = "ic_shield_memory"
        ),

        Recipe(
            id = "shield_nature",
            name = "Carapace de Gaïa",
            type = "BOUCLIER",
            description = "Modelée dans la connaissance du vivant et de la Terre. " +
                "Forgée par la maîtrise de la SVT.",
            lore = "« La Terre elle-même l'a endurci dans ses strates profondes. »",
            cost = mapOf("SVT" to 50),
            resultResName = "ic_shield_nature"
        ),

        // ── ARMES ──────────────────────────────────────────────────────────────

        Recipe(
            id = "sword_wisdom",
            name = "Épée de Sagesse",
            type = "ARME",
            description = "Forgée des fragments de Philosophie et d'Art. Symbole de la " +
                "pensée créative et critique réunies.",
            lore = "« La lame tranche le doute, la garde protège l'incertitude. »",
            cost = mapOf("Philo/SES" to 40, "Art/Musique" to 20),
            resultResName = "ic_plume_athena"
        ),

        Recipe(
            id = "spear_science",
            name = "Lance du Chercheur",
            type = "ARME",
            description = "Aiguisée sur les formules et les réactions chimiques. Symbole de " +
                "la précision scientifique absolue.",
            lore = "« Héphaïstos a aligné chaque atome de l'alliage avec une règle d'or. »",
            cost = mapOf("Physique-Chimie" to 50),
            resultResName = "ic_spear_science"
        ),

        Recipe(
            id = "bow_language",
            name = "Arc d'Hermès",
            type = "ARME",
            description = "Tendu de fibres linguistiques rares. Symbole de la rapidité et " +
                "de la maîtrise des langues.",
            lore = "« Hermès lui-même a testé la corde avant de la confier au héros. »",
            cost = mapOf("Langues" to 40, "Français" to 20),
            resultResName = "ic_sandales_hermes"
        ),

        // ── PARCHEMINS (consommables) ──────────────────────────────────────────

        Recipe(
            id = "scroll_hint_math",
            name = "Parchemin de l'Oracle (Maths)",
            type = "PARCHEMIN",
            description = "Révèle l'indice caché d'une question de Mathématiques pendant un quiz. " +
                "Usage unique.",
            lore = "« L'Oracle parle une fois. Écoute chaque mot. »",
            cost = mapOf("Mathématiques" to 20),
            resultResName = "ic_scroll_oracle_maths"
        ),

        Recipe(
            id = "scroll_hint_svt",
            name = "Parchemin de l'Oracle (SVT)",
            type = "PARCHEMIN",
            description = "Révèle l'indice caché d'une question de SVT pendant un quiz. " +
                "Usage unique.",
            lore = "« La nature chuchote. Le parchemin traduit son secret. »",
            cost = mapOf("SVT" to 20),
            resultResName = "ic_scroll_oracle_svt"
        ),

        Recipe(
            id = "scroll_hint_histoire",
            name = "Parchemin de l'Oracle (Histoire)",
            type = "PARCHEMIN",
            description = "Révèle l'indice caché d'une question d'Histoire pendant un quiz. " +
                "Usage unique.",
            lore = "« Les morts ne mentent pas. Le parchemin t'ouvre leurs archives. »",
            cost = mapOf("Histoire" to 20),
            resultResName = "ic_scroll_oracle_histoire"
        ),

        // ── ARTEFACTS (cosmétiques / titres) ──────────────────────────────────

        Recipe(
            id = "artifact_polymath",
            name = "Médaillon du Polymathès",
            type = "ARTEFACT",
            description = "Accordé aux héros maîtrisant plusieurs domaines. " +
                "Débloque le titre « Polymathès de l'Olympe » dans le profil.",
            lore = "« Aristote l'aurait porté avec fierté et humilité. »",
            cost = mapOf(
                "Mathématiques" to 30,
                "Histoire" to 30,
                "SVT" to 30
            ),
            resultResName = "ic_artifact_polymath"
        ),

        Recipe(
            id = "artifact_geographer",
            name = "Globe d'Ératos",
            type = "ARTEFACT",
            description = "Forgé des fragments du monde entier. " +
                "Débloque le titre « Arpenteur des Terres » dans le profil.",
            lore = "« Ératosthène a calculé la Terre. Toi tu l'as mémorisée. »",
            cost = mapOf("Géographie" to 50),
            resultResName = "ic_artifact_geographer"
        ),

        Recipe(
            id = "artifact_poet",
            name = "Lyre d'Orphée",
            type = "ARTEFACT",
            description = "Symbole de l'élève qui maîtrise la langue et les arts à la fois. " +
                "Débloque le titre « Voix des Muses ».",
            lore = "« Orphée charmait les pierres. Toi tu charmes les savoirs. »",
            cost = mapOf("Français" to 40, "Art/Musique" to 30),
            resultResName = "ic_diademe_aphrodite"
        ),

        // ── OFFRANDES (sacrifices pour bonus rares) ────────────────────────────

        Recipe(
            id = "offering_zeus",
            name = "Offrande à Zeus",
            type = "OFFRANDE",
            description = "Sacrifie tes fragments les plus précieux en échange d'une " +
                "Ambroisie divine. Réservé aux héros prêts à tout sacrifier pour la gloire.",
            lore = "« Ce que tu sacrifies revient centuplé dans la faveur des dieux. »",
            cost = mapOf(
                "Mathématiques" to 25,
                "Physique-Chimie" to 25,
                "SVT" to 25
            ),
            resultResName = "ic_offering_zeus"
        ),

        Recipe(
            id = "offering_athena",
            name = "Offrande à Athéna",
            type = "OFFRANDE",
            description = "Un sacrifice de sagesse écrite et de pensée critique. " +
                "Récompensé par un bonus d'XP et un fragment d'Ambroisie.",
            lore = "« Athéna récompense ceux qui pensent autant qu'ils agissent. »",
            cost = mapOf(
                "Français" to 25,
                "Philo/SES" to 25
            ),
            resultResName = "ic_offering_athena"
        )
    )

    // ══════════════════════════════════════════════════════════
    // LOGIQUE DE CRAFT
    // ══════════════════════════════════════════════════════════

    /**
     * Vérifie si le joueur peut se permettre une recette.
     *
     * Compare les fragments disponibles (UserProfile.knowledgeFragments)
     * avec le coût de la recette, matière par matière.
     *
     * @param recipe  La recette à vérifier.
     * @param profile Le profil du joueur.
     * @return true si le joueur a assez de fragments dans chaque matière requise.
     *
     * ÉVOLUTION FUTURE :
     * - Retourner un Map<String, Int> de "manque par matière" pour afficher
     *   dans ForgeActivity ce qu'il reste à farm (ex : "Il manque 12 fragments de Maths").
     */
    fun canAfford(recipe: Recipe, profile: UserProfile): Boolean {
        return recipe.cost.all { (matiere, quantiteRequise) ->
            profile.getFragmentCount(matiere) >= quantiteRequise
        }
    }

    /**
     * Calcule le manque de fragments par matière pour une recette.
     * Retourne uniquement les matières où le joueur est insuffisant.
     * Retourne une Map vide si le joueur peut se permettre la recette.
     *
     * Utilisé dans ForgeActivity pour afficher un message du type :
     * "Il te manque 12 Fragments de Mathématiques et 5 de Physique."
     *
     * @param recipe  La recette cible.
     * @param profile Le profil du joueur.
     * @return Map<matière, fragments manquants>. Vide si canAfford = true.
     */
    fun missingFragments(recipe: Recipe, profile: UserProfile): Map<String, Int> {
        return recipe.cost
            .mapNotNull { (matiere, quantiteRequise) ->
                val disponible = profile.getFragmentCount(matiere)
                val manque = quantiteRequise - disponible
                if (manque > 0) matiere to manque else null
            }
            .toMap()
    }

    /**
     * Déduit les fragments du coût d'une recette depuis le profil.
     *
     * ATTENTION : modifie le profil EN MÉMOIRE uniquement.
     * L'appelant DOIT persister le profil en DB après l'appel :
     * ```kotlin
     * CraftingSystem.deductCost(recipe, profile)
     * db.iAristoteDao().updateUserProfile(profile)
     * ```
     *
     * Utilise addFragments() avec un delta négatif — la logique de
     * coerceAtLeast(0) dans addFragments() garantit l'absence de négatif.
     *
     * @param recipe  La recette craftée.
     * @param profile Le profil à muter.
     *
     * ÉVOLUTION FUTURE :
     * - Déclencher un événement "fragments_consumed" vers AnalyticsManager
     */
    fun deductCost(recipe: Recipe, profile: UserProfile) {
        recipe.cost.forEach { (matiere, quantiteRequise) ->
            profile.addFragments(matiere, -quantiteRequise)
        }
    }

    /**
     * Construit l'InventoryItem résultat depuis une recette.
     *
     * L'item retourné a quantity = 1.
     * Si l'objet existe déjà en inventaire (getInventoryItemByName != null),
     * l'appelant doit incrémenter item.quantity et appeler updateInventoryItem()
     * au lieu d'insérer un doublon.
     *
     * @param recipe La recette craftée.
     * @return Un InventoryItem prêt pour insertInventoryItem().
     */
    fun buildItemFromRecipe(recipe: Recipe): InventoryItem {
        val totalCost = recipe.cost.values.sum()

        val rarete = when {
            totalCost >= 50 -> "LÉGENDAIRE"
            totalCost >= 35 -> "ÉPIQUE"
            totalCost >= 20 -> "RARE"
            else -> "COMMUN"
        }

        return InventoryItem(
            name = recipe.name,
            type = recipe.type,
            description = recipe.description,
            imageResName = recipe.resultResName,
            quantity = 1,
            rarete = rarete,
            obtainedAt = System.currentTimeMillis()
        )
    }

    /**
     * Trouve une recette par son id stable.
     * Utilisé par ForgeActivity lors de la confirmation de craft.
     *
     * @param recipeId L'identifiant de la recette (ex : "shield_logic").
     * @return La recette ou null si introuvable.
     */
    fun findRecipeById(recipeId: String): Recipe? =
        availableRecipes.firstOrNull { it.id == recipeId }

    /**
     * Filtre les recettes par type d'objet.
     * Utilisé pour les onglets de ForgeActivity.
     *
     * @param type Ex : "BOUCLIER", "ARME", "PARCHEMIN", "ARTEFACT", "OFFRANDE".
     * @return Liste filtrée, vide si aucune recette du type.
     */
    fun recipesByType(type: String): List<Recipe> =
        availableRecipes.filter { it.type.equals(type, ignoreCase = true) }

    /**
     * Retourne la liste des recettes que le joueur peut se permettre MAINTENANT.
     * Pratique pour afficher un badge "Craft disponible !" dans le Dashboard.
     *
     * @param profile Le profil du joueur.
     * @return Liste des recettes affordables, vide si aucune.
     *
     * ÉVOLUTION FUTURE :
     * - Appeler depuis DashboardActivity pour afficher une notification "Forge prête"
     */
    fun affordableRecipes(profile: UserProfile): List<Recipe> =
        availableRecipes.filter { canAfford(it, profile) }
}


/*
════════════════════════════════════════════════════════════════════
ICON_PROMPTS — Prompts de génération IA pour les 4 nouveaux drawables
Style global : Flat icon · 2D · minimal · single color gold/bronze
               ancient greek style · transparent background · 256×256
════════════════════════════════════════════════════════════════════

[
  {
    "drawable": "ic_bouclier_logique",
    "recipe_id": "shield_logic",
    "color": "gold #FFD700",
    "prompt": "A round ancient Greek hoplite shield with the Pi (π) symbol engraved in the center, flat 2D icon, minimal geometric linework, single gold color #FFD700, no shading, no gradient, clean vector style, transparent background, 256x256 pixels"
  },
  {
    "drawable": "ic_sandales_hermes",
    "recipe_id": "bow_language",
    "color": "gold #FFD700",
    "prompt": "Ancient Greek winged sandal of Hermes seen from the side, one elegant wing sprouting from the ankle strap, flat 2D icon, minimal linework, single gold color #FFD700, no shading, no gradient, clean vector style, transparent background, 256x256 pixels"
  },
  {
    "drawable": "ic_plume_athena",
    "recipe_id": "sword_wisdom",
    "color": "gold #FFD700",
    "prompt": "A single elegant upright feather quill pen with a Greek key meander pattern engraved along the shaft, symbol of Athena's wisdom, flat 2D icon, minimal linework, single gold color #FFD700, no shading, no gradient, clean vector style, transparent background, 256x256 pixels"
  },
  {
    "drawable": "ic_diademe_aphrodite",
    "recipe_id": "artifact_poet",
    "color": "bronze #CD7F32",
    "prompt": "A delicate ancient Greek diadem crown with a heart motif in the center and laurel leaf decorations on each side, symbol of Aphrodite, flat 2D icon, minimal linework, single bronze color #CD7F32, no shading, no gradient, clean vector style, transparent background, 256x256 pixels"
  }
]

DRAWABLES RESTANTS À CRÉER (priorité descendante) :
  ic_shield_memory          — bouclier oval avec runes entrelacées argent
  ic_shield_nature          — bouclier rond feuille grecque verte
  ic_spear_science          — lance droite avec formule chimique gravée
  ic_artifact_polymath      — médaillon avec 3 symboles (π, feuille, livre)
  ic_artifact_geographer    — globe sphère méridiens style antique
  ic_offering_zeus          — éclair double pointe de Zeus
  ic_offering_athena        — hibou d'Athéna face caméra
  ic_scroll_oracle_maths    — parchemin roulé symbole ∑
  ic_scroll_oracle_svt      — parchemin roulé avec feuille/cellule
  ic_scroll_oracle_histoire — parchemin roulé colonne grecque ou sablier
════════════════════════════════════════════════════════════════════
*/

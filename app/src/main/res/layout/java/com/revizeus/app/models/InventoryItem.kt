package com.revizeus.app.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * ═══════════════════════════════════════════════════════════════
 * InventoryItem.kt — RéviZeus — PHASE C : LA FORGE D'HÉPHAÏSTOS
 * ═══════════════════════════════════════════════════════════════
 *
 * Entité Room représentant un objet détenu par le héros dans son sac.
 * Table : "inventory"
 *
 * TYPES D'OBJETS (champ `type`) :
 *   "BOUCLIER"  → protection passive (ex: bonus résistance aux erreurs)
 *   "ARME"      → boost offensif (ex: multiplicateur XP, précision quiz)
 *   "ARTEFACT"  → cosmétique / titre / particule de profil
 *   "PARCHEMIN" → consommable à usage unique (indice en quiz)
 *   "OFFRANDE"  → sacrifiable pour un bonus rare (Ambroisie, XP)
 *
 * ARCHITECTURE :
 *   - Indépendante de UserProfile et CourseEntry : zero couplage.
 *   - quantity = 0 autorisé (objet connu/historique mais épuisé).
 *   - imageResName résolu dynamiquement dans l'UI via getIdentifier(),
 *     jamais figé en R.drawable.xxx pour respecter la règle ressource.
 *
 * BLOC 2 — OPTION B :
 *   ✅ rarete        : utilisée pour le tri réel de l'inventaire
 *   ✅ obtainedAt    : utilisé pour le tri par ordre d'obtention
 *   ✅ helper rareteWeight() pour la hiérarchie UI
 *
 * PACK 2 — INVENTAIRE AVANCÉ :
 *   ✅ isFavorite    : toggle via long press, tri prioritaire
 *
 * ÉVOLUTION FUTURE :
 *   - Ajouter `isEquipped: Boolean = false` pour les objets passifs équipables
 *   - Ajouter `durability: Int = -1`  (-1 = infini, pour les consommables)
 *   - Ajouter `sourceRecipeId: String = ""` pour tracer la recette d'origine
 *   - Migrer vers un Flow<List<InventoryItem>> dans le DAO pour la réactivité
 * ═══════════════════════════════════════════════════════════════
 */
@Entity(tableName = "inventory")
data class InventoryItem(

    /**
     * Clé primaire auto-incrémentée.
     * Room gère la séquence — on ne force aucun id métier externe.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /**
     * Nom d'affichage de l'objet.
     * Affiché dans InventoryActivity, ForgeActivity et les dialogues de Forge.
     * Ex : "Bouclier de Logique", "Épée de Mémorisation", "Globe d'Ératos".
     */
    @ColumnInfo(name = "name")
    val name: String,

    /**
     * Catégorie de l'objet.
     * Valeurs attendues : "BOUCLIER", "ARME", "ARTEFACT", "PARCHEMIN", "OFFRANDE".
     * Extensible librement : c'est une String, aucune enum fixée.
     */
    @ColumnInfo(name = "type")
    val type: String,

    /**
     * Description narrative et pédagogique de l'objet.
     * Affichée dans la carte de l'objet dans InventoryActivity.
     * Ex : "Forgé des fragments de Logique — protège contre les pièges de déduction."
     */
    @ColumnInfo(name = "description")
    val description: String,

    /**
     * Nom de la ressource drawable associée à l'objet.
     * Résolu dynamiquement dans l'UI via :
     *   resources.getIdentifier(imageResName, "drawable", packageName)
     * Aucun R.drawable.xxx codé en dur ici — règle ressource respectée.
     * Ex : "ic_shield_logic", "ic_artifact_polymath", "ic_scroll_oracle".
     */
    @ColumnInfo(name = "image_res_name")
    val imageResName: String,

    /**
     * Nombre d'exemplaires possédés.
     * 0 = objet connu mais épuisé (historique visible dans l'inventaire).
     * La ligne n'est PAS supprimée à quantity=0 pour conserver l'historique.
     *
     * Incrémenter via updateInventoryItem() si l'objet existe déjà en DB
     * (détecté par getInventoryItemByName() dans ForgeActivity).
     */
    @ColumnInfo(name = "quantity")
    var quantity: Int = 1,

    /**
     * BLOC 2 — OPTION B
     * Rareté métier de l'objet.
     * Valeurs conseillées :
     * - COMMUN
     * - RARE
     * - ÉPIQUE
     * - LÉGENDAIRE
     */
    @ColumnInfo(name = "rarete")
    var rarete: String = "COMMUN",

    /**
     * BLOC 2 — OPTION B
     * Horodatage de création / obtention.
     * Utilisé pour le tri "ordre d'obtention".
     */
    @ColumnInfo(name = "obtained_at")
    var obtainedAt: Long = System.currentTimeMillis(),

    /**
     * PACK 2 — INVENTAIRE AVANCÉ
     * Favori / verrouillé.
     * Les objets favoris remontent en haut de la liste (tri prioritaire).
     * Toggle via long press dans InventoryActivity.
     */
    @ColumnInfo(name = "is_favorite")
    var isFavorite: Boolean = false
) {

    /**
     * Retourne un poids de tri stable selon la rareté.
     * Plus la valeur est élevée, plus l'objet est rare.
     */
    @Ignore
    fun rareteWeight(): Int {
        return when (rarete.trim().uppercase()) {
            "LÉGENDAIRE", "LEGENDAIRE" -> 4
            "ÉPIQUE", "EPIQUE" -> 3
            "RARE" -> 2
            else -> 1
        }
    }
}

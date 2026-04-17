package com.revizeus.app

import android.content.Context
import android.graphics.Color

/**
 * ═══════════════════════════════════════════════════════════════
 * KNOWLEDGE FRAGMENT MANAGER — RéviZeus
 * ═══════════════════════════════════════════════════════════════
 * Rôle :
 * Centralise toute la logique métier liée aux Fragments de Savoir.
 *
 * RECTIFICATION FINALE :
 * ✅ meilleure résolution des alias de matière (Langues/Anglais, etc.)
 * ✅ évite le fallback rouge badge_count pour Hermès quand la matière stockée
 *    ne correspond pas exactement au mapping du dieu
 * ✅ aucune variable existante renommée
 * ✅ aucune mécanique métier supprimée
 * ═══════════════════════════════════════════════════════════════
 */
object KnowledgeFragmentManager {

    /**
     * Calcule la récompense finale en Fragments de Savoir selon le pourcentage.
     */
    fun calculateReward(percentage: Int): Int {
        val safePercentage = percentage.coerceIn(0, 100)
        return when {
            safePercentage == 0 -> 0
            safePercentage in 1..20 -> 1
            safePercentage in 21..40 -> 4
            safePercentage in 41..60 -> 8
            safePercentage in 61..80 -> 12
            safePercentage in 81..95 -> 17
            else -> 25
        }
    }

    /**
     * Retourne le meilleur libellé UI pour une matière.
     */
    fun getDisplayName(matiere: String): String {
        return matiere.ifBlank { "Savoir" }
    }

    /**
     * Retourne la couleur d'accent associée à la matière / au dieu.
     */
    fun getFragmentColorInt(matiere: String): Int {
        val canonicalMatiere = canonicalizeSubject(matiere)

        val pantheonColor = PantheonConfig.findByMatiere(canonicalMatiere)?.couleur
        if (pantheonColor != null) return pantheonColor

        val godColor = GodManager.fromMatiere(canonicalMatiere)?.couleurInt
        if (godColor != null) return godColor

        return Color.parseColor("#FFD700")
    }

    /**
     * Retourne l'icône fragment la plus pertinente pour la matière.
     */
    fun getFragmentIconRes(context: Context, matiere: String): Int {
        val packageName = context.packageName
        val preferredNames = buildCandidateDrawableNames(matiere)

        preferredNames.forEach { drawableName ->
            val resId = context.resources.getIdentifier(drawableName, "drawable", packageName)
            if (resId != 0) return resId
        }

        return android.R.drawable.star_on
    }

    /**
     * Construit la liste ordonnée des drawables candidats.
     */
    private fun buildCandidateDrawableNames(matiere: String): List<String> {
        val canonicalMatiere = canonicalizeSubject(matiere)
        val divinite = resolveDivinitySlug(canonicalMatiere)

        val candidates = mutableListOf<String>()

        if (divinite.isNotBlank()) {
            candidates += "ic_fragment_$divinite"
        }

        // Fallbacks spécifiques par matière canonique.
        when (canonicalMatiere) {
            "Langues" -> {
                candidates += "ic_fragment_hermes"
            }
            "Art/Musique" -> {
                candidates += "ic_fragment_aphrodite"
            }
            "Vie & Projets" -> {
                candidates += "ic_fragment_promethee"
            }
        }

        candidates += "ic_fragment_savoir"
        candidates += "badge_count"

        return candidates.distinct()
    }

    /**
     * Résout la divinité attachée à la matière en slug de ressource.
     */
    private fun resolveDivinitySlug(matiere: String): String {
        val canonicalMatiere = canonicalizeSubject(matiere)

        val divinite = PantheonConfig.findByMatiere(canonicalMatiere)?.divinite
            ?: GodManager.fromMatiere(canonicalMatiere)?.nomDieu
            ?: when (canonicalMatiere) {
                "Langues" -> "Hermès"
                "Art/Musique" -> "Aphrodite"
                "Vie & Projets" -> "Prométhée"
                else -> ""
            }

        return when (normalize(divinite)) {
            "zeus" -> "zeus"
            "athena" -> "athena"
            "poseidon" -> "poseidon"
            "ares" -> "ares"
            "aphrodite" -> "aphrodite"
            "hermes" -> "hermes"
            "demeter" -> "demeter"
            "hephaistos" -> "hephaistos"
            "apollon" -> "apollon"
            "promethee" -> "promethee"
            "prometheus" -> "promethee"
            "hades" -> "hades"
            else -> ""
        }
    }

    /**
     * Canonicalisation locale des matières pour fiabiliser la résolution des icônes.
     */
    private fun canonicalizeSubject(matiere: String): String {
        return when (normalize(matiere)) {
            "mathematiques", "maths" -> "Mathématiques"
            "francais", "français" -> "Français"
            "svt" -> "SVT"
            "histoire" -> "Histoire"
            "art", "art/musique", "art / musique", "musique" -> "Art/Musique"
            "langues", "anglais", "english" -> "Langues"
            "geographie", "géographie" -> "Géographie"
            "physique-chimie", "physique / chimie", "physique", "chimie" -> "Physique-Chimie"
            "philo/ses", "philo / ses", "philosophie", "ses" -> "Philo/SES"
            "vie & projets", "vie et projets", "projets", "orientation" -> "Vie & Projets"
            else -> matiere.ifBlank { "Savoir" }
        }
    }

    /**
     * Normalisation légère pour tolérer les accents et variantes.
     */
    private fun normalize(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace("é", "e")
            .replace("è", "e")
            .replace("ê", "e")
            .replace("à", "a")
            .replace("â", "a")
            .replace("î", "i")
            .replace("ï", "i")
            .replace("ô", "o")
            .replace("ö", "o")
            .replace("ù", "u")
            .replace("û", "u")
            .replace("ü", "u")
            .replace("ç", "c")
        }
}

package com.revizeus.app

import com.revizeus.app.models.UserProfile
import org.json.JSONObject
import kotlin.math.max

object GodAffinityManager {

    private val AFFINITY_LEVEL_THRESHOLDS: List<Long> = listOf(
        0L,
        20L,
        120L,
        320L,
        720L,
        1_520L,
        3_120L,
        6_620L,
        12_420L,
        20_020L,
        40_220L,
        75_620L,
        100_420L,
        132_020L,
        200_220L,
        310_620L,
        400_420L,
        500_020L,
        590_220L,
        700_620L,
        999_999L
    )

    private val DEFAULT_AFFINITY_LABELS: List<String> = listOf(
        "Inconnu de l’Olympe",
        "Premier caillou sur l’autel",
        "Mortel vaguement repéré",
        "Regard divin entrouvert",
        "Étincelle qui ose briller",
        "Apprenti du temple",
        "Voix entendue dans le marbre",
        "Serviteur du savoir",
        "Disciple pas trop perdu",
        "Disciple confirmé",
        "Protégé en rodage",
        "Protégé reconnu",
        "Allié du temple",
        "Allié qui commence à peser",
        "Compagnon d’épreuve",
        "Compagnon divin",
        "Élu du domaine",
        "Favori dangereusement crédible",
        "Héros consacré",
        "Champion du temple",
        "Lien olympien"
    )

    private val AFFINITY_LABELS_BY_GOD: Map<String, List<String>> = mapOf(
        "zeus" to listOf(
            "Inconnu du Tonnerre",
            "Porteur d’une étincelle suspecte",
            "Compteur de nuages débutant",
            "Mortel repéré par l’éclair",
            "Étincelle sous surveillance",
            "Apprenti du Paratonnerre",
            "Calculateur de foudres mineures",
            "Disciple du Théorème Tonitruant",
            "Domptoir de fractions orageuses",
            "Géomètre du Ciel",
            "Protégé du Tonnerre",
            "Stratège des Éclairs",
            "Allié de la Foudre",
            "Maître des Nuages Chiffrés",
            "Compagnon de l’Orage Sacré",
            "Bras droit du Tonnerre",
            "Élu de l’Équation Suprême",
            "Favori du Roi des Dieux",
            "Héros de la Foudre Logique",
            "Champion du Mont Calculus",
            "Lien Olympien du Tonnerre"
        ),
        "athena" to listOf(
            "Inconnu de la Chouette",
            "Porteur d’une virgule timide",
            "Apprenti de la Phrase Propre",
            "Mortel repéré par la Sagesse",
            "Dompteur de brouillons rebelles",
            "Apprenti du Plan en Trois Parties",
            "Gardien du Verbe Bien Placé",
            "Disciple de la Chouette Sérieuse",
            "Stratège de la Syntaxe",
            "Chevalier de la Méthode",
            "Protégé de la Sagesse",
            "Correcteur de Chaos Grammatical",
            "Allié du Raisonnement Clair",
            "Architecte des Arguments",
            "Compagnon de la Dissertation Sacrée",
            "Bras droit de la Chouette",
            "Élu de la Pensée Structurée",
            "Favori d’Athéna",
            "Héros de la Clarté Absolue",
            "Champion du Temple de la Méthode",
            "Lien Olympien de la Sagesse"
        ),
        "poseidon" to listOf(
            "Inconnu des Marées",
            "Petit galet dans l’écume",
            "Observateur de flaques savantes",
            "Mortel mouillé mais motivé",
            "Éclaireur des Courants Vivants",
            "Apprenti des Abysses",
            "Nageur de Schémas Biologiques",
            "Disciple du Grand Cycle",
            "Gardien des Cellules Agitées",
            "Explorateur des Profondeurs",
            "Protégé des Marées",
            "Cartographe du Vivant",
            "Allié des Océans de Savoir",
            "Maître des Courants Cachés",
            "Compagnon de l’Abysse Sacré",
            "Bras droit du Trident",
            "Élu des Profondeurs Vivantes",
            "Favori de Poséidon",
            "Héros des Cycles Naturels",
            "Champion du Temple Marin",
            "Lien Olympien des Abysses"
        ),
        "ares" to listOf(
            "Inconnu du Champ de Bataille",
            "Porteur d’un cure-dent héroïque",
            "Recrue du Casque Cabossé",
            "Mortel qui tient encore debout",
            "Étincelle de Guerre Scolaire",
            "Apprenti du Bouclier",
            "Soldat des Dates Dangereuses",
            "Disciple des Batailles Révisées",
            "Chargeur de Chronologies",
            "Guerrier du Paragraphe Armé",
            "Protégé d’Arès",
            "Briseur d’Erreurs Historiques",
            "Allié du Champ de Bataille",
            "Stratège des Conflits",
            "Compagnon de la Lance Rouge",
            "Bras droit du Carnage Pédagogique",
            "Élu du Combat Méthodique",
            "Favori d’Arès",
            "Héros des Guerres Mémorisées",
            "Champion du Temple de la Guerre",
            "Lien Olympien du Fracas"
        ),
        "demeter" to listOf(
            "Inconnu du Jardin Sacré",
            "Petite graine pas encore arrosée",
            "Pousse timide du Savoir",
            "Mortel qui sait planter une carte",
            "Bourgeon de Géographie",
            "Apprenti du Champ Fertile",
            "Jardinier des Continents",
            "Disciple des Terres Habitées",
            "Semeur de Cartes Propres",
            "Gardien des Climats",
            "Protégé des Moissons",
            "Cultivateur de Territoires",
            "Allié des Champs de Savoir",
            "Maître des Sols et Frontières",
            "Compagnon de la Terre Sacrée",
            "Bras droit des Récoltes",
            "Élu des Saisons Savantes",
            "Favori de Déméter",
            "Héros des Terres Connues",
            "Champion du Temple des Moissons",
            "Lien Olympien de la Terre"
        ),
        "hephaestus" to listOf(
            "Inconnu de la Forge",
            "Porteur d’une étincelle bancale",
            "Apprenti Marteau Mal Réveillé",
            "Mortel toléré près de l’enclume",
            "Étincelle de Laboratoire",
            "Apprenti de la Forge",
            "Bricoleur d’Atomes",
            "Disciple du Boulon Cosmique",
            "Soudeur de Formules",
            "Forgeron des Réactions",
            "Protégé de l’Enclume",
            "Réparateur de Raisonnements",
            "Allié de la Forge Divine",
            "Maître des Mécanismes",
            "Compagnon du Métal Sacré",
            "Bras droit d’Héphaïstos",
            "Élu de l’Atome Incandescent",
            "Favori du Forgeron Divin",
            "Héros des Lois Physiques",
            "Champion du Temple de la Forge",
            "Lien Olympien du Feu Technique"
        ),
        "apollo" to listOf(
            "Inconnu de la Lyre",
            "Porteur d’une note hésitante",
            "Apprenti du Refrain Bancal",
            "Mortel qui chante presque juste",
            "Étincelle Mélodieuse",
            "Apprenti de la Lyre",
            "Rimeur de Savoirs",
            "Disciple du Rayon Doré",
            "Gardien du Refrain Utile",
            "Chanteur de Concepts",
            "Protégé de la Lumière",
            "Compositeur de Mémoire",
            "Allié de l’Harmonie",
            "Maître des Mnémos Solaires",
            "Compagnon du Chant Sacré",
            "Bras droit de la Lyre",
            "Élu du Rayon Pédagogique",
            "Favori d’Apollon",
            "Héros de l’Harmonie Savante",
            "Champion du Temple Solaire",
            "Lien Olympien de la Lumière"
        ),
        "hermes" to listOf(
            "Inconnu des Sandales Ailées",
            "Porteur d’un mot qui court vite",
            "Apprenti Messageur",
            "Mortel repéré en pleine fuite",
            "Étincelle de Traduction",
            "Apprenti des Ailes",
            "Messager du Vocabulaire",
            "Disciple du Verbe Rapide",
            "Sprinteur des Phrases",
            "Passe-Muraille Grammatical",
            "Protégé d’Hermès",
            "Traducteur des Nuages",
            "Allié des Messages Divins",
            "Maître des Langues Agiles",
            "Compagnon des Ailes Sacrées",
            "Bras droit du Messager",
            "Élu du Mot Juste",
            "Favori d’Hermès",
            "Héros des Langues Vives",
            "Champion du Temple des Messages",
            "Lien Olympien des Ailes"
        ),
        "aphrodite" to listOf(
            "Inconnu du Miroir Sacré",
            "Porteur d’un crayon timide",
            "Apprenti du Beau Brouillon",
            "Mortel presque présentable",
            "Étincelle d’Inspiration",
            "Apprenti de l’Éclat",
            "Peintre de Concepts",
            "Disciple du Regard Juste",
            "Styliste du Savoir",
            "Sculpteur d’Idées",
            "Protégé d’Aphrodite",
            "Enlumineur de Mémoire",
            "Allié de la Beauté Claire",
            "Maître des Images Mentales",
            "Compagnon de l’Éclat Sacré",
            "Bras droit de l’Inspiration",
            "Élu de la Forme Parfaite",
            "Favori d’Aphrodite",
            "Héros de la Beauté Savante",
            "Champion du Temple de l’Éclat",
            "Lien Olympien du Charme"
        ),
        "prometheus" to listOf(
            "Inconnu du Feu Volé",
            "Porteur d’une braise minuscule",
            "Apprenti Porte-Flamme",
            "Mortel qui n’a pas lâché",
            "Étincelle Courageuse",
            "Apprenti du Feu",
            "Gardien de la Braise",
            "Disciple de l’Élan Humain",
            "Porteur de Projet",
            "Bâtisseur de Chemin",
            "Protégé de Prométhée",
            "Allumeur d’Avenir",
            "Allié du Feu Sacré",
            "Maître des Premiers Pas",
            "Compagnon de la Flamme",
            "Bras droit du Courage",
            "Élu de l’Étincelle Humaine",
            "Favori de Prométhée",
            "Héros du Feu Intérieur",
            "Champion du Temple des Possibles",
            "Lien Olympien de la Flamme"
        )
    )

    data class GodAffinityProfile(
        val profilesByGod: Map<String, GodAffinitySnapshot>,
        val topGods: List<GodAffinitySnapshot>,
        val unknownSubjects: Map<String, Long>,
        val totalFragmentsCounted: Long,
        val summaryLine: String
    )

    data class GodAffinitySnapshot(
        val godId: String,
        val godDisplayName: String,
        val fragments: Long,
        val affinityLevel: Int,
        val affinityLabel: String,
        val progressToNextLevelPercent: Int,
        val fragmentsNeededForNextLevel: Long,
        val nextLevel: Int?,
        val promptHint: String,
        val debugExplain: String
    )

    fun buildProfile(profile: UserProfile): GodAffinityProfile {
        return buildProfileFromKnowledgeFragments(profile.knowledgeFragments)
    }

    fun buildProfileFromKnowledgeFragments(rawJson: String): GodAffinityProfile {
        val knownGodIds = knownGodIds()
        val fragmentsByGod = knownGodIds.associateWith { 0L }.toMutableMap()
        val unknownSubjects = linkedMapOf<String, Long>()

        safeParseKnowledgeFragments(rawJson).forEach { (subject, count) ->
            val godId = resolveGodIdForSubject(subject)
            if (godId == null) {
                unknownSubjects[subject] = (unknownSubjects[subject] ?: 0L) + count
            } else {
                fragmentsByGod[godId] = (fragmentsByGod[godId] ?: 0L) + count
            }
        }

        val profilesByGod = linkedMapOf<String, GodAffinitySnapshot>()
        knownGodIds.forEach { godId ->
            val fragments = fragmentsByGod[godId] ?: 0L
            val level = computeAffinityLevel(fragments)
            val progress = computeProgressPercent(fragments, level)
            val fragmentsNeeded = computeFragmentsNeeded(fragments, level)
            val nextLevel = nextLevelFor(level)
            val displayName = displayNameForGod(godId)
            val label = labelForLevel(godId, level)
            val promptHint = buildPromptHint(
                godDisplayName = displayName,
                level = level,
                label = label
            )
            val debugExplain = buildDebugExplain(
                level = level,
                progressPercent = progress,
                fragmentsNeeded = fragmentsNeeded,
                nextLevel = nextLevel
            )
            profilesByGod[godId] = GodAffinitySnapshot(
                godId = godId,
                godDisplayName = displayName,
                fragments = fragments,
                affinityLevel = level,
                affinityLabel = label,
                progressToNextLevelPercent = progress,
                fragmentsNeededForNextLevel = fragmentsNeeded,
                nextLevel = nextLevel,
                promptHint = promptHint,
                debugExplain = debugExplain
            )
        }

        val topGods = profilesByGod.values
            .filter { it.fragments > 0L }
            .sortedWith(
                compareByDescending<GodAffinitySnapshot> { it.fragments }
                    .thenByDescending { it.affinityLevel }
            )
            .take(3)

        val totalFragmentsCounted = profilesByGod.values.sumOf { it.fragments }

        return GodAffinityProfile(
            profilesByGod = profilesByGod,
            topGods = topGods,
            unknownSubjects = unknownSubjects.toMap(),
            totalFragmentsCounted = totalFragmentsCounted,
            summaryLine = buildSummaryLine(topGods)
        )
    }

    private fun safeParseKnowledgeFragments(rawJson: String): Map<String, Long> {
        if (rawJson.isBlank()) return emptyMap()
        return try {
            val json = JSONObject(rawJson)
            val result = linkedMapOf<String, Long>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val subject = keys.next().orEmpty().trim()
                if (subject.isBlank()) continue
                val count = try {
                    when (val value = json.get(subject)) {
                        is Number -> value.toLong()
                        is String -> value.toLongOrNull() ?: 0L
                        else -> json.optLong(subject, 0L)
                    }
                } catch (_: Exception) {
                    json.optLong(subject, 0L)
                }
                if (count > 0L) {
                    result[subject] = count
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun resolveGodIdForSubject(subject: String): String? {
        val godInfo = PantheonConfig.findByMatiere(subject) ?: return null
        val normalized = GodPersonalityEngine.normalizeGodId(godInfo.divinite)
        return normalized.takeIf { knownGodIds().contains(it) }
    }

    private fun knownGodIds(): List<String> = listOf(
        "zeus",
        "athena",
        "poseidon",
        "ares",
        "demeter",
        "hephaestus",
        "apollo",
        "hermes",
        "aphrodite",
        "prometheus"
    )

    private fun displayNameForGod(godId: String): String {
        return when (godId) {
            "zeus" -> "Zeus"
            "athena" -> "Athéna"
            "poseidon" -> "Poséidon"
            "ares" -> "Arès"
            "demeter" -> "Déméter"
            "hephaestus" -> "Héphaïstos"
            "apollo" -> "Apollon"
            "hermes" -> "Hermès"
            "aphrodite" -> "Aphrodite"
            "prometheus" -> "Prométhée"
            else -> godId.replaceFirstChar { it.uppercaseChar() }
        }
    }

    private fun computeAffinityLevel(fragments: Long): Int {
        if (fragments <= 0L) return 0
        for (i in AFFINITY_LEVEL_THRESHOLDS.indices.reversed()) {
            if (fragments >= AFFINITY_LEVEL_THRESHOLDS[i]) {
                return i.coerceIn(0, 20)
            }
        }
        return 0
    }

    private fun computeProgressPercent(fragments: Long, level: Int): Int {
        if (level >= 20) return 100
        val safeLevel = level.coerceIn(0, 19)
        val currentMin = AFFINITY_LEVEL_THRESHOLDS[safeLevel]
        val nextMin = AFFINITY_LEVEL_THRESHOLDS[safeLevel + 1]
        val span = max(1L, nextMin - currentMin)
        val earned = (fragments - currentMin).coerceIn(0L, span)
        return ((earned * 100L) / span).toInt().coerceIn(0, 100)
    }

    private fun computeFragmentsNeeded(fragments: Long, level: Int): Long {
        if (level >= 20) return 0L
        val safeLevel = level.coerceIn(0, 19)
        val nextMin = AFFINITY_LEVEL_THRESHOLDS[safeLevel + 1]
        return (nextMin - fragments).coerceAtLeast(0L)
    }

    private fun nextLevelFor(level: Int): Int? {
        return if (level >= 20) null else (level + 1).coerceAtMost(20)
    }

    private fun buildPromptHint(
        godDisplayName: String,
        level: Int,
        label: String
    ): String {
        return when (level.coerceIn(0, 20)) {
            0 -> "Aucune affinité notable avec $godDisplayName : rester neutre et pédagogique."
            in 1..4 -> "Affinité faible avec $godDisplayName ($label) : reconnaître un début d’effort sans exagérer."
            in 5..9 -> "Affinité naissante avec $godDisplayName ($label) : adopter une chaleur légère et personnalisée."
            in 10..14 -> "Affinité solide avec $godDisplayName ($label) : reconnaître la régularité du héros."
            in 15..19 -> "Affinité forte avec $godDisplayName ($label) : ton plus complice, toujours pédagogique."
            else -> "Affinité maximale avec $godDisplayName ($label) : reconnaissance rare mais sobre."
        }
    }

    private fun buildDebugExplain(
        level: Int,
        progressPercent: Int,
        fragmentsNeeded: Long,
        nextLevel: Int?
    ): String {
        if (level >= 20) {
            return "niveau 20/20, progression maximale atteinte"
        }
        return "niveau $level/20, progression $progressPercent%, $fragmentsNeeded fragment(s) avant niveau ${nextLevel ?: 20}"
    }

    private fun buildSummaryLine(topGods: List<GodAffinitySnapshot>): String {
        if (topGods.isEmpty()) {
            return "Aucune affinité divine significative pour le moment."
        }
        val top = topGods.joinToString(" | ") { snapshot ->
            "${snapshot.godDisplayName} : ${snapshot.affinityLabel}"
        }
        return "Affinités dominantes : $top. Utiliser cela comme nuance relationnelle subtile et pédagogique."
    }

    private fun labelForLevel(godId: String, level: Int): String {
        val safeLevel = level.coerceIn(0, 20)
        val labels = AFFINITY_LABELS_BY_GOD[godId]
        return labels?.getOrNull(safeLevel)
            ?: DEFAULT_AFFINITY_LABELS.getOrElse(safeLevel) { "Lien divin inconnu" }
    }
}

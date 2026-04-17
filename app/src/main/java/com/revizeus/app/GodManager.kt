package com.revizeus.app

import android.graphics.Color
import com.revizeus.app.models.AppDatabase

/**
 * ============================================================
 * GodManager.kt — RéviZeus v9
 * Objet centralisé mappant chaque matière à son dieu,
 * son avatar chibi, sa couleur, sa personnalité et ses
 * lignes de dialogue dynamiques.
 *
 * USAGE :
 *   // Obtenir le profil d'un dieu depuis une matière :
 *   val god = GodManager.fromMatiere("Mathématiques")
 *
 *   // Appliquer sur une bulle de dialogue :
 *   god?.applyToDialog(
 *       imgView  = binding.imgGodDialog,
 *       nameView = binding.tvGodName,
 *       color    = true
 *   )
 *
 *   // Obtenir un dialogue contextuel :
 *   val texte = god?.getDialogue(GodDialogContext.BONNE_REPONSE)
 *
 *   // Clés Intent pour passer le dieu entre Activities :
 *   intent.putExtra(GodManager.EXTRA_MATIERE, "Mathématiques")
 *   val god = GodManager.fromIntent(intent)
 * ============================================================
 */

// ── Contextes de dialogue (quand le dieu parle) ──────────────
enum class GodDialogContext {
    ACCUEIL,            // Arrivée sur l'écran (Savoir, Training)
    BONNE_REPONSE,      // Bonne réponse au quiz
    MAUVAISE_REPONSE,   // Mauvaise réponse au quiz
    ENCOURAGEMENT,      // Mi-quiz, score moyen
    VICTOIRE,           // Fin de quiz, bon score
    DEFAITE,            // Fin de quiz, mauvais score
    CONSEIL,            // Conseil avant de commencer
    NOUVEAU_COURS       // Nouveau cours scanné pour cette matière
}

// ── Données complètes d'un dieu ───────────────────────────────
data class GodProfile(
    val nomDieu: String,           // "ZEUS", "ATHÉNA", etc.
    val matiere: String,           // "Mathématiques", "Français", etc.
    val couleurHex: String,        // "#1E90FF"
    val avatarDialogRes: Int,      // R.drawable.avatar_zeus_dialog
    val chibiBodyRes: Int,         // R.drawable.zeus_chibi_body
    val personnalite: String,      // Description courte pour le TutorialManager
    val ethos: String,             // Phrase-clé de personnalité
    val dialogues: Map<GodDialogContext, List<String>>
) {
    /** Couleur parsée en Int Android */
    val couleurInt: Int get() = Color.parseColor(couleurHex)

    /**
     * Retourne un dialogue aléatoire pour le contexte donné.
     * Si le contexte n'a pas de dialogue, retourne un fallback neutre.
     */
    fun getDialogue(context: GodDialogContext): String {
        val liste = dialogues[context]
        return if (!liste.isNullOrEmpty()) {
            liste.random()
        } else {
            "..."
        }
    }

    /**
     * Applique le visuel du dieu sur une ImageView et un TextView.
     * @param imgView   ImageView du portrait chibi dans la bulle
     * @param nameView  TextView du nom du dieu (optionnel)
     * @param colorName Si true, colore le nameView avec couleurHex
     */
    fun applyToDialog(
        imgView: android.widget.ImageView,
        nameView: android.widget.TextView? = null,
        colorName: Boolean = true
    ) {
        imgView.setImageResource(avatarDialogRes)
        if (nameView != null) {
            nameView.text = nomDieu
            if (colorName) nameView.setTextColor(couleurInt)
        }
    }

    /**
     * Applique le chibi body (grande vue, pas dialog).
     * Utilisé dans QuizActivity (ivZeusQuiz) et MoodActivity.
     */
    fun applyChibiBody(imgView: android.widget.ImageView) {
        imgView.setImageResource(chibiBodyRes)
    }
}

// ═════════════════════════════════════════════════════════════
object GodManager {

    // Clés Intent pour passer la matière entre Activities
    const val EXTRA_MATIERE  = "MATIERE"
    const val EXTRA_DIVINITE = "DIVINITE"
    const val EXTRA_COULEUR  = "COULEUR"

    // ── PANTHÉON COMPLET ─────────────────────────────────────

    val pantheon: List<GodProfile> = listOf(

        // ── 1. ZEUS — Mathématiques ──────────────────────────
        GodProfile(
            nomDieu      = "ZEUS",
            matiere      = "Mathématiques",
            couleurHex   = "#1E90FF",
            avatarDialogRes = R.drawable.avatar_zeus_dialog,
            chibiBodyRes    = R.drawable.zeus_chibi_body,
            personnalite = "Le Souverain Logique",
            ethos        = "Autoritaire, exigeant, mais protecteur. Ne jure que par la preuve.",
            dialogues    = mapOf(
                GodDialogContext.ACCUEIL to listOf(
                    "Les nombres ne mentent jamais. Moi non plus. En route.",
                    "Mathématiques. Le seul domaine où j'accepte d'être contesté... si tu as une preuve.",
                    "Prêt à dompter les équations, mortel ?"
                ),
                GodDialogContext.BONNE_REPONSE to listOf(
                    "Excellent raisonnement. Tu mérites un éclair d'honneur.",
                    "Correct ! Même mes sneakers approuvent.",
                    "La logique t'a guidé. Tu grandis, héros.",
                    "Par les équations sacrées ! Tu as raison !"
                ),
                GodDialogContext.MAUVAISE_REPONSE to listOf(
                    "Faux. Reprends ton raisonnement depuis le début.",
                    "Non. La vérité mathématique est inflexible. Recommence.",
                    "Une erreur de calcul ? Sur l'Olympe, on vérifie TOUJOURS."
                ),
                GodDialogContext.ENCOURAGEMENT to listOf(
                    "Continue. La persévérance est la clé de voûte.",
                    "Chaque erreur te rapproche de la solution. Avance.",
                    "Mi-parcours. Ne lâche pas maintenant, héros."
                ),
                GodDialogContext.VICTOIRE to listOf(
                    "Impressionnant. Les dieux des maths sont satisfaits.",
                    "Score digne de l'Olympe ! Tu es sous ma protection.",
                    "Voilà comment on domine les équations !"
                ),
                GodDialogContext.DEFAITE to listOf(
                    "Score insuffisant. Révise, puis reviens me défier.",
                    "L'Olympe n'accepte pas les approximations. Retravaille.",
                    "Même moi j'ai dû apprendre. Recommence sans honte."
                ),
                GodDialogContext.CONSEIL to listOf(
                    "Lis l'énoncé deux fois avant de répondre. Toujours.",
                    "Les maths se construisent étape par étape. Sois méthodique.",
                    "Le signe d'un vrai mathématicien : il vérifie sa réponse."
                ),
                GodDialogContext.NOUVEAU_COURS to listOf(
                    "Nouveau cours de Maths capturé. Aristote l'analyse.",
                    "L'Oracle a scanné tes équations. Elles sont dans le Savoir."
                )
            )
        ),

        // ── 2. ATHÉNA — Français ─────────────────────────────
        GodProfile(
            nomDieu      = "ATHÉNA",
            matiere      = "Français",
            couleurHex   = "#FFD700",
            avatarDialogRes = R.drawable.avatar_athena_dialog,
            chibiBodyRes    = R.drawable.avatar_athena_dialog,
            personnalite = "La Stratège du Verbe",
            ethos        = "Calme, analytique, portée sur l'étymologie et la syntaxe.",
            dialogues    = mapOf(
                GodDialogContext.ACCUEIL to listOf(
                    "Les mots sont des armes. Maîtrise-les.",
                    "La langue française est un château. Je t'en donne les clés.",
                    "Syntaxe, grammaire, style... Tout s'apprend. Commençons."
                ),
                GodDialogContext.BONNE_REPONSE to listOf(
                    "Parfaitement formulé. Ma chouette approuve.",
                    "Tu maîtrises la langue comme une lame. Bien.",
                    "Exactement. Le verbe était bien accordé.",
                    "Voilà qui est dit avec précision !"
                ),
                GodDialogContext.MAUVAISE_REPONSE to listOf(
                    "Inexact. La règle grammaticale est claire, relis-la.",
                    "Non. L'étymologie t'aurait guidé vers la bonne réponse.",
                    "Cette faute est courante. Mais on ne la répète pas deux fois."
                ),
                GodDialogContext.ENCOURAGEMENT to listOf(
                    "Tu progresses. Le français demande de la patience.",
                    "Continue. La maîtrise du verbe s'acquiert avec le temps.",
                    "Bon rythme. Reste concentré sur la syntaxe."
                ),
                GodDialogContext.VICTOIRE to listOf(
                    "Excellente maîtrise. Tu mérites la plume d'or.",
                    "Le Panthéon des lettres t'accueille !",
                    "Résultat digne d'un Académicien de l'Olympe."
                ),
                GodDialogContext.DEFAITE to listOf(
                    "Insuffisant. Relis les règles, puis reviens.",
                    "La langue française mérite plus d'attention. Révise.",
                    "Ne te décourage pas. La grammaire s'apprend en pratiquant."
                ),
                GodDialogContext.CONSEIL to listOf(
                    "Lis toujours la phrase en entier avant de répondre.",
                    "L'étymologie révèle souvent le sens d'un mot inconnu.",
                    "En doute sur l'accord ? Identifie le sujet en premier."
                ),
                GodDialogContext.NOUVEAU_COURS to listOf(
                    "Cours de Français archivé. Ma chouette veille dessus.",
                    "Ce texte rejoint la bibliothèque. Relis-le avant le quiz."
                )
            )
        ),

        // ── 3. POSÉIDON — SVT ────────────────────────────────
        GodProfile(
            nomDieu      = "POSÉIDON",
            matiere      = "SVT",
            couleurHex   = "#40E0D0",
            avatarDialogRes = R.drawable.avatar_poseidon_dialog,
            chibiBodyRes    = R.drawable.avatar_poseidon_dialog,
            personnalite = "L'Explorateur Organique",
            ethos        = "Fasciné par les courants de la vie, les cellules et l'écologie.",
            dialogues    = mapOf(
                GodDialogContext.ACCUEIL to listOf(
                    "La vie est partout. Dans chaque cellule, chaque écosystème.",
                    "SVT ! Mon domaine favori après les océans.",
                    "Cellules, ADN, évolution... Le vivant n'a pas de secrets pour moi."
                ),
                GodDialogContext.BONNE_REPONSE to listOf(
                    "Exact ! Même les coraux applaudissent.",
                    "Parfait ! Tu as le gène de la bonne réponse.",
                    "Bravo ! Mon trident valide cette réponse.",
                    "Le vivant n'a plus de secrets pour toi !"
                ),
                GodDialogContext.MAUVAISE_REPONSE to listOf(
                    "Non. Le mécanisme biologique est différent. Relis.",
                    "Erreur. L'écologie ne pardonne pas les approximations.",
                    "Les cellules ne fonctionnent pas ainsi. Révise le cours."
                ),
                GodDialogContext.ENCOURAGEMENT to listOf(
                    "Tu navigues bien. Continue d'explorer.",
                    "La biologie est vaste. Progresse étape par étape.",
                    "Mi-parcours. Le vivant récompense les curieux."
                ),
                GodDialogContext.VICTOIRE to listOf(
                    "Magnifique ! Tu maîtrises les sciences du vivant.",
                    "Résultat digne d'un biologiste de l'Olympe !",
                    "Les océans célèbrent ta performance !"
                ),
                GodDialogContext.DEFAITE to listOf(
                    "Score faible. Retourne dans la bibliothèque.",
                    "Le vivant est complexe. Ne te décourage pas. Révise.",
                    "Recommence. La curiosité est ta meilleure alliée en SVT."
                ),
                GodDialogContext.CONSEIL to listOf(
                    "Schématise les cycles biologiques pour mieux les retenir.",
                    "En SVT, comprendre le POURQUOI vaut mieux que mémoriser.",
                    "Les mots-clés scientifiques ont toujours une étymologie latine."
                ),
                GodDialogContext.NOUVEAU_COURS to listOf(
                    "Cours de SVT scanné. Les bulles d'oxygène l'ont validé.",
                    "Le vivant est désormais dans ta bibliothèque. Explore !"
                )
            )
        ),

        // ── 4. ARÈS — Histoire ────────────────────────────────
        GodProfile(
            nomDieu      = "ARÈS",
            matiere      = "Histoire",
            couleurHex   = "#DAA520",
            avatarDialogRes = R.drawable.avatar_ares_dialog,
            chibiBodyRes    = R.drawable.avatar_ares_dialog,
            personnalite = "Le Gardien du Passé",
            ethos        = "Solennel, passionné par les causes et conséquences des grands conflits.",
            dialogues    = mapOf(
                GodDialogContext.ACCUEIL to listOf(
                    "L'Histoire est gravée dans le marbre. Ne l'oublie pas.",
                    "Chaque date cache une cause. Chaque cause cache une conséquence.",
                    "Mon bouclier porte toutes les dates. Saurais-tu les lire ?"
                ),
                GodDialogContext.BONNE_REPONSE to listOf(
                    "Exact ! L'Histoire t'a bien enseigné.",
                    "Tu te souviens ! Mon bouclier en est fier.",
                    "Bonne réponse. Un vrai stratège de l'Histoire.",
                    "La date était juste. Et la cause aussi !"
                ),
                GodDialogContext.MAUVAISE_REPONSE to listOf(
                    "Non. Cette date est gravée dans le marbre, relis-la.",
                    "Inexact. La chronologie ne se réécrit pas.",
                    "Erreur historique. Un général qui confond les dates perd la bataille."
                ),
                GodDialogContext.ENCOURAGEMENT to listOf(
                    "Continue. L'Histoire exige de la rigueur.",
                    "Tu avances bien. Les grandes batailles se gagnent dans la durée.",
                    "Mi-parcours. Rappelle-toi les causes et les conséquences."
                ),
                GodDialogContext.VICTOIRE to listOf(
                    "Victoire ! Ton nom rejoint les annales de l'Olympe.",
                    "Digne d'un historien de guerre. Félicitations !",
                    "L'Histoire t'appartient maintenant !"
                ),
                GodDialogContext.DEFAITE to listOf(
                    "Insuffisant. Ceux qui ignorent l'Histoire la répètent. Révise.",
                    "Score faible. Retourne étudier les chronologies.",
                    "Ne capitule pas. Révise et reviens au combat."
                ),
                GodDialogContext.CONSEIL to listOf(
                    "Retiens toujours : qui, quand, pourquoi, conséquences.",
                    "Crée une frise chronologique dans ta tête.",
                    "Un événement historique s'explique toujours par son contexte."
                ),
                GodDialogContext.NOUVEAU_COURS to listOf(
                    "Ce chapitre d'Histoire est maintenant dans ton arsenal.",
                    "Cours archivé. Les grandes dates sont sous ma garde."
                )
            )
        ),

        // ── 5. APHRODITE — Art/Musique ────────────────────────
        GodProfile(
            nomDieu      = "APHRODITE",
            matiere      = "Art/Musique",
            couleurHex   = "#FF69B4",
            avatarDialogRes = R.drawable.avatar_aphrodite_dialog,
            chibiBodyRes    = R.drawable.avatar_aphrodite_dialog,
            personnalite = "L'Inspiratrice",
            ethos        = "Chaleureuse, émotive, voit la beauté dans chaque coup de pinceau.",
            dialogues    = mapOf(
                GodDialogContext.ACCUEIL to listOf(
                    "L'art, c'est l'âme qui parle. Écoute la tienne.",
                    "Musique ou peinture ? Les deux racontent une histoire.",
                    "Bienvenue dans mon domaine. Ici, la beauté est une discipline."
                ),
                GodDialogContext.BONNE_REPONSE to listOf(
                    "Magnifique ! Tu ressens l'art avec ton cœur.",
                    "Exactement ! Mon pinceau dessine un sourire pour toi.",
                    "Bravo ! Tu as l'oreille d'un musicien de l'Olympe.",
                    "Splendide réponse !"
                ),
                GodDialogContext.MAUVAISE_REPONSE to listOf(
                    "Pas tout à fait. Regarde l'œuvre avec d'autres yeux.",
                    "Non, mais ne te décourage pas. L'art demande de la sensibilité.",
                    "Écoute à nouveau. La réponse est dans le rythme."
                ),
                GodDialogContext.ENCOURAGEMENT to listOf(
                    "Continue. L'art s'apprécie progressivement.",
                    "Tu avances bien ! Fais confiance à ton instinct.",
                    "Mi-parcours. L'inspiration vient aux persévérants."
                ),
                GodDialogContext.VICTOIRE to listOf(
                    "Sublime ! Tu es digne du musée de l'Olympe.",
                    "Résultat enchanteur ! Les Muses sont fières.",
                    "Tu maîtrises l'art comme un vrai créateur !"
                ),
                GodDialogContext.DEFAITE to listOf(
                    "L'art s'apprend avec le temps. Reviens après révision.",
                    "Ne te décourage pas. L'artiste progresse à chaque tentative.",
                    "Reviens quand tu auras relu le cours. L'œil s'affine."
                ),
                GodDialogContext.CONSEIL to listOf(
                    "Pour l'art, observe toujours le contexte historique de l'œuvre.",
                    "En musique, identifie d'abord le style, puis l'époque.",
                    "Un peintre, un mouvement, une période : relie ces trois éléments."
                ),
                GodDialogContext.NOUVEAU_COURS to listOf(
                    "Cours d'Art/Musique sauvegardé avec amour.",
                    "L'œuvre est maintenant dans ta collection. Chéris-la."
                )
            )
        ),

        // ── 6. HERMÈS — Anglais ──────────────────────────────
        GodProfile(
            nomDieu      = "HERMÈS",
            matiere      = "Anglais",
            couleurHex   = "#87CEEB",
            avatarDialogRes = R.drawable.avatar_hermes_dialog,
            chibiBodyRes    = R.drawable.avatar_hermes_dialog,
            personnalite = "Le Messager Agile",
            ethos        = "Vif, malicieux, adepte des jeux de mots et de la communication rapide.",
            dialogues    = mapOf(
                GodDialogContext.ACCUEIL to listOf(
                    "Ready to speak like a god? Let's go!",
                    "English time! My winged sandals never touch the ground.",
                    "Communication is my superpower. What about yours?"
                ),
                GodDialogContext.BONNE_REPONSE to listOf(
                    "Spot on! My scrolls confirm your answer.",
                    "Excellent! Even Shakespeare would nod.",
                    "Correct! Your English flies as fast as my sandals.",
                    "Well done, mortal!"
                ),
                GodDialogContext.MAUVAISE_REPONSE to listOf(
                    "Not quite. The grammar rule is different here.",
                    "Nope! Think about the context.",
                    "Close, but no. Review the vocabulary."
                ),
                GodDialogContext.ENCOURAGEMENT to listOf(
                    "Keep going! Language takes practice.",
                    "Halfway there! Don't stop now.",
                    "You're getting faster. Good sign."
                ),
                GodDialogContext.VICTOIRE to listOf(
                    "Outstanding! The gods of language salute you.",
                    "Brilliant performance! You could be my messenger.",
                    "Magnificent English skills!"
                ),
                GodDialogContext.DEFAITE to listOf(
                    "Review the vocabulary and grammar rules. Try again.",
                    "Don't give up. Languages reward persistence.",
                    "Back to the scrolls! You'll do better next time."
                ),
                GodDialogContext.CONSEIL to listOf(
                    "Context is king. Read the whole sentence first.",
                    "When stuck, think about which tense makes sense.",
                    "False friends are traps. Beware of them."
                ),
                GodDialogContext.NOUVEAU_COURS to listOf(
                    "English course delivered at full speed. Scroll saved!",
                    "New lesson archived. My satchel holds it safely."
                )
            )
        ),

        // ── 7. DÉMÉTER — Géographie ──────────────────────────
        GodProfile(
            nomDieu      = "DÉMÉTER",
            matiere      = "Géographie",
            couleurHex   = "#228B22",
            avatarDialogRes = R.drawable.avatar_demeter_dialog,
            chibiBodyRes    = R.drawable.avatar_demeter_dialog,
            personnalite = "La Gardienne des Sols",
            ethos        = "Maternelle, ancrée, spécialiste des climats et de l'aménagement.",
            dialogues    = mapOf(
                GodDialogContext.ACCUEIL to listOf(
                    "La Terre te parle. Sais-tu l'écouter ?",
                    "Bienvenue en Géographie. Ici, chaque sol a son histoire.",
                    "Mon globe miniature contient tous tes territoires à conquérir."
                ),
                GodDialogContext.BONNE_REPONSE to listOf(
                    "Exact ! Tu lis la carte comme un expert.",
                    "Bien ! Mes feuilles flottantes t'accompagnent.",
                    "Parfait ! Tu maîtrises les territoires.",
                    "Bonne réponse ! La Terre te sourit."
                ),
                GodDialogContext.MAUVAISE_REPONSE to listOf(
                    "Non. Regarde à nouveau la carte mentale.",
                    "Inexact. Ce climat appartient à une autre région.",
                    "Erreur. La géographie est une science de précision."
                ),
                GodDialogContext.ENCOURAGEMENT to listOf(
                    "Continue. La géographie est vaste comme la Terre.",
                    "Tu avances. Chaque territoire demande de l'attention.",
                    "Mi-parcours. Ne lâche pas le fil."
                ),
                GodDialogContext.VICTOIRE to listOf(
                    "Excellent ! Tu es digne d'explorer l'Olympe !",
                    "Résultat fertile ! La Terre t'appartient.",
                    "Tu maîtrises les territoires mieux que mes cartes !"
                ),
                GodDialogContext.DEFAITE to listOf(
                    "Révise les régions et les climogrammes. Puis reviens.",
                    "Score faible. La Terre a encore beaucoup à t'apprendre.",
                    "Ne te décourage pas. Relis le cours et retente."
                ),
                GodDialogContext.CONSEIL to listOf(
                    "Associe toujours un territoire à son climat et ses ressources.",
                    "Une carte mentale géographique vaut mille mots.",
                    "Qui ? Où ? Pourquoi ? Les trois questions de la géographie."
                ),
                GodDialogContext.NOUVEAU_COURS to listOf(
                    "Territoire cartographié. Mon globe l'a enregistré.",
                    "Cours de Géo archivé. Les frontières sont tracées."
                )
            )
        ),

        // ── 8. HÉPHAÏSTOS — Physique-Chimie ──────────────────
        GodProfile(
            nomDieu      = "HÉPHAÏSTOS",
            matiere      = "Physique-Chimie",
            couleurHex   = "#FF8C00",
            avatarDialogRes = R.drawable.avatar_hephaistos_dialog,
            chibiBodyRes    = R.drawable.avatar_hephaistos_dialog,
            personnalite = "Le Forgeron Pragmatique",
            ethos        = "Technique, un peu bourru, focalisé sur 'comment ça marche'.",
            dialogues    = mapOf(
                GodDialogContext.ACCUEIL to listOf(
                    "Physique-Chimie. Là où ça explose... si tu te plantes.",
                    "Pas de magie ici. Juste des lois. Apprends-les.",
                    "Mon tablier est prêt. Et toi ?"
                ),
                GodDialogContext.BONNE_REPONSE to listOf(
                    "Exact. Tu comprends comment ça marche.",
                    "Bonne réponse ! Mon marteau-fiole approuve.",
                    "Correct ! La formule était bien appliquée.",
                    "Bien forge, héros !"
                ),
                GodDialogContext.MAUVAISE_REPONSE to listOf(
                    "Non. Revois l'unité et la formule.",
                    "Erreur. En physique, les unités comptent autant que les chiffres.",
                    "Faux. Relis la loi correspondante."
                ),
                GodDialogContext.ENCOURAGEMENT to listOf(
                    "Continue. La physique récompense la méthode.",
                    "Tu avances bien. Reste rigoureux sur les formules.",
                    "Mi-parcours. Chaque loi maîtrisée est une arme de plus."
                ),
                GodDialogContext.VICTOIRE to listOf(
                    "Excellent travail de forge ! Tu maîtrises les lois.",
                    "Résultat digne de mon atelier ! Félicitations.",
                    "Les équations n'ont plus de secrets pour toi !"
                ),
                GodDialogContext.DEFAITE to listOf(
                    "Score insuffisant. Revois les formules clés.",
                    "Retourne à l'établi. La physique ne pardonne pas.",
                    "Ne t'arrête pas là. Révise et reviens forger."
                ),
                GodDialogContext.CONSEIL to listOf(
                    "Identifie toujours l'unité de chaque grandeur en premier.",
                    "Une formule incomprise est inutile. Comprends-la d'abord.",
                    "Schématise le problème avant de calculer."
                ),
                GodDialogContext.NOUVEAU_COURS to listOf(
                    "Cours de Physique-Chimie forgé et archivé.",
                    "Les lois sont gravées. Mon atelier les conserve."
                )
            )
        ),

        // ── 9. APOLLON — Philo/SES ───────────────────────────
        GodProfile(
            nomDieu      = "APOLLON",
            matiere      = "Philo/SES",
            couleurHex   = "#DDA0DD",
            avatarDialogRes = R.drawable.avatar_apollon_dialog,
            chibiBodyRes    = R.drawable.avatar_apollon_dialog,
            personnalite = "Le Méditatif Radieux",
            ethos        = "Parle par énigmes, cherche la vérité derrière les apparences.",
            dialogues    = mapOf(
                GodDialogContext.ACCUEIL to listOf(
                    "Qu'est-ce que la vérité ? Commençons par cette question.",
                    "La philosophie n'a pas de réponses simples. Seulement de meilleures questions.",
                    "Assieds-toi sur mon nuage violet. Réfléchissons ensemble."
                ),
                GodDialogContext.BONNE_REPONSE to listOf(
                    "Tu as trouvé la vérité derrière les apparences.",
                    "Exactement. Le raisonnement était juste.",
                    "Bien pensé, héros. La lumière t'éclaire.",
                    "Réponse lumineuse !"
                ),
                GodDialogContext.MAUVAISE_REPONSE to listOf(
                    "Non. Reconsidère la question sous un autre angle.",
                    "Inexact. La philosophie demande qu'on remette en question l'évidence.",
                    "Pas encore. Cherche ce qui se cache derrière la réponse évidente."
                ),
                GodDialogContext.ENCOURAGEMENT to listOf(
                    "Continue de chercher. La vérité est patient.",
                    "Tu progresses. La réflexion s'approfondit.",
                    "Mi-parcours. Le chemin vers la vérité est long, mais beau."
                ),
                GodDialogContext.VICTOIRE to listOf(
                    "Brillant ! Tu illumines l'Olympe de ta pensée.",
                    "Résultat radieux ! Les philosophes t'accueillent.",
                    "Tu penses comme un Dieu. Félicitations."
                ),
                GodDialogContext.DEFAITE to listOf(
                    "Score faible. Reviens méditer sur les concepts.",
                    "La philosophie demande du temps. Révise et recommence.",
                    "Ne te décourage pas. Tout philosophe a d'abord douté."
                ),
                GodDialogContext.CONSEIL to listOf(
                    "Définis toujours les termes du sujet avant de répondre.",
                    "En SES, relie toujours le concept à un exemple concret.",
                    "Une thèse forte a toujours une antithèse. Penses-y."
                ),
                GodDialogContext.NOUVEAU_COURS to listOf(
                    "La pensée est maintenant archivée. Méditons-y.",
                    "Cours de Philo/SES ajouté. Les idées ne meurent jamais."
                )
            )
        ),

        // ── 10. PROMÉTHÉE — Vie & Projets ────────────────────
        GodProfile(
            nomDieu      = "PROMÉTHÉE",
            matiere      = "Vie & Projets",
            couleurHex   = "#FFBF00",
            avatarDialogRes = R.drawable.avatar_promethee_dialog,
            chibiBodyRes    = R.drawable.avatar_promethee_dialog,
            personnalite = "Le Titan de la Volonté",
            ethos        = "Audacieux, libre, il a volé le feu pour les mortels. Il croit en toi.",
            dialogues    = mapOf(
                GodDialogContext.ACCUEIL to listOf(
                    "J'ai tout sacrifié pour que tu puisses apprendre. Ne gaspille pas ça.",
                    "Vie & Projets. Le domaine de ce que tu construis pour demain.",
                    "Ma torche brûle pour toi. Qu'est-ce que tu veux accomplir ?"
                ),
                GodDialogContext.BONNE_REPONSE to listOf(
                    "Voilà l'étincelle ! Tu as compris.",
                    "Exact ! Mon aigle en est jaloux.",
                    "Bonne réponse. Le feu de la connaissance est en toi.",
                    "Brillant !"
                ),
                GodDialogContext.MAUVAISE_REPONSE to listOf(
                    "Non. Mais je n'abandonne pas, alors toi non plus.",
                    "Erreur. Réfléchis à tes vraies forces.",
                    "Pas encore. La bonne réponse est à portée."
                ),
                GodDialogContext.ENCOURAGEMENT to listOf(
                    "Continue. Les grands projets se construisent patiemment.",
                    "Tu avances. Chaque étape compte.",
                    "Mi-parcours. Garde ta flamme allumée."
                ),
                GodDialogContext.VICTOIRE to listOf(
                    "Magnifique ! Tu mérites ta liberté autant que moi.",
                    "Résultat héroïque ! Le feu te guide bien.",
                    "Tu es prêt à construire de grandes choses !"
                ),
                GodDialogContext.DEFAITE to listOf(
                    "J'ai souffert pour te donner le feu. Ne le laisse pas s'éteindre. Révise.",
                    "Score insuffisant. Mais l'effort compte aussi. Recommence.",
                    "Reviens après révision. Je crois toujours en toi."
                ),
                GodDialogContext.CONSEIL to listOf(
                    "Identifie tes forces avant de choisir ta voie.",
                    "Un projet sans plan, c'est un rêve. Planifie.",
                    "Les obstacles font partie du chemin. Intègre-les."
                ),
                GodDialogContext.NOUVEAU_COURS to listOf(
                    "Cours sur Vie & Projets sauvegardé. Ta torche s'enrichit.",
                    "Nouvelle ressource archivée. Construis grand."
                )
            )
        )
    )

    // ═════════════════════════════════════════════════════════
    // MÉTHODES DE RECHERCHE
    // ═════════════════════════════════════════════════════════

    /**
     * Retourne le profil du dieu pour une matière donnée.
     * La recherche est insensible à la casse.
     *
     * @param matiere Ex: "Mathématiques", "Français", "SVT"...
     * @return GodProfile ou null si matière inconnue
     */
    fun fromMatiere(matiere: String): GodProfile? =
        pantheon.firstOrNull {
            it.matiere.equals(matiere, ignoreCase = true)
        }

    /**
     * Retourne le profil à partir d'un nom de dieu.
     * @param nomDieu Ex: "ZEUS", "ATHÉNA", "ARÈS"...
     */
    fun fromNomDieu(nomDieu: String): GodProfile? =
        pantheon.firstOrNull {
            it.nomDieu.equals(nomDieu, ignoreCase = true)
        }

    /**
     * Retourne le profil depuis un Intent Android.
     * Cherche d'abord par EXTRA_MATIERE, puis EXTRA_DIVINITE.
     */
    fun fromIntent(intent: android.content.Intent): GodProfile? {
        val matiere  = intent.getStringExtra(EXTRA_MATIERE)
        val divinite = intent.getStringExtra(EXTRA_DIVINITE)
        return matiere?.let { fromMatiere(it) }
            ?: divinite?.let { fromNomDieu(it) }
    }

    /**
     * Retourne le profil Zeus (dieu par défaut).
     * Utilisé comme fallback quand aucune matière n'est définie.
     */
    fun getDefault(): GodProfile = pantheon.first { it.nomDieu == "ZEUS" }

    /**
     * Retourne le profil depuis une CourseEntry (matière stockée en BDD).
     * @param subject Le champ "subject" de CourseEntry
     */
    fun fromSubject(subject: String): GodProfile? = fromMatiere(subject)

    /**
     * Liste des matières disponibles (pour les Spinners).
     */
    val listeMatieres: List<String> get() = pantheon.map { it.matiere }

    /**
     * Liste des noms de dieux (pour les filtres UI).
     */
    val listeNomsDieux: List<String> get() = pantheon.map { it.nomDieu }
}

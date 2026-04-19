package com.revizeus.app

import kotlin.random.Random

/**
 * Banque de micro-dialogues codés en dur mais MULTI-VARIANTES.
 *
 * Usage :
 * - micro-feedbacks trop petits pour un appel IA ;
 * - fallback si Gemini renvoie null ;
 * - transitions système où l'on veut garder de la vie sans latence.
 */
object DivineMicroCopyLibrary {

    enum class MicroCopyKey {
        GENERIC_SUCCESS,
        GENERIC_ERROR_SOFT,
        GENERIC_SAVED,
        GENERIC_RETRY,
        GENERIC_EMPTY,
        GENERIC_CONFIRMATION
    }

    fun pick(
        godId: String,
        key: MicroCopyKey,
        subjectHint: String? = null,
        explicitOutcome: String? = null
    ): String {
        val subjectPart = subjectHint?.takeIf { it.isNotBlank() }?.let { " pour $it" }.orEmpty()
        val outcomePart = explicitOutcome?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()

        val variants = when (GodPersonalityEngine.normalizeGodId(godId)) {
            "zeus" -> when (key) {
                MicroCopyKey.GENERIC_SUCCESS -> listOf(
                    "Bien. Ce point tient debout$subjectPart. Continuons avant que la foudre ne s'impatiente.",
                    "C'est validé$subjectPart. Pour une fois, l'Olympe n'a rien à redire.",
                    "Voilà une action correcte$subjectPart. Sobre, propre, efficace."
                )
                MicroCopyKey.GENERIC_ERROR_SOFT -> listOf(
                    "Quelque chose résiste$subjectPart$outcomePart. Reprends calmement.",
                    "Le tonnerre a buté sur un détail$subjectPart. Réessaie sans précipiter la preuve.",
                    "Ce n'est pas encore stable$subjectPart$outcomePart. On recommence proprement."
                )
                MicroCopyKey.GENERIC_SAVED -> listOf(
                    "C'est scellé dans les archives$subjectPart. Même les éclairs aiment l'ordre.",
                    "Enregistré$subjectPart. Le Panthéon apprécie les choses à leur place.",
                    "Sauvegarde accomplie$subjectPart. Zeus ne classera rien à ta place éternellement."
                )
                MicroCopyKey.GENERIC_RETRY -> listOf(
                    "Reprends depuis le début, mais avec plus de rigueur.",
                    "Encore une tentative. Cette fois, vise juste.",
                    "On recommence. Le chaos adore les clics trop rapides."
                )
                MicroCopyKey.GENERIC_EMPTY -> listOf(
                    "Il n'y a rien ici$subjectPart. Même le vide mérite un meilleur plan.",
                    "Le temple sonne creux$subjectPart. Il faudra bien le nourrir un jour.",
                    "Rien à montrer$subjectPart. L'Olympe attend encore son offrande."
                )
                MicroCopyKey.GENERIC_CONFIRMATION -> listOf(
                    "Choisis avec soin. Même un clic peut déclencher un petit destin.",
                    "Confirme si tu assumes la suite. Zeus déteste les regrets bruyants.",
                    "Décision majeure. Garde la tête froide avant de toucher au tonnerre."
                )
            }
            "athena" -> when (key) {
                MicroCopyKey.GENERIC_SUCCESS -> listOf(
                    "C'est bien enregistré$subjectPart. Une pensée claire laisse de bonnes traces.",
                    "Action validée$subjectPart. Propre, lisible, presque élégant.",
                    "Très bien$subjectPart. La structure tient."
                )
                MicroCopyKey.GENERIC_ERROR_SOFT -> listOf(
                    "Un détail bloque encore$subjectPart$outcomePart. Regarde la logique de l'ensemble.",
                    "Ce n'est pas grave$subjectPart. On clarifie et on repart.",
                    "Quelque chose reste flou$subjectPart$outcomePart. Reprenons par étapes."
                )
                MicroCopyKey.GENERIC_SAVED -> listOf(
                    "Sauvegardé$subjectPart. Voilà une archive qui saura se relire.",
                    "C'est en place$subjectPart. Le savoir aime les repères stables.",
                    "Enregistrement effectué$subjectPart. Les idées ordonnées respirent mieux."
                )
                MicroCopyKey.GENERIC_RETRY -> listOf(
                    "Réessaie calmement. La bonne méthode vaut mieux qu'un clic pressé.",
                    "On peut refaire cela proprement.",
                    "Encore un essai. Cette fois, suis le fil logique."
                )
                MicroCopyKey.GENERIC_EMPTY -> listOf(
                    "Rien n'apparaît encore$subjectPart. Il faut d'abord remplir la bibliothèque.",
                    "Le panneau est vide$subjectPart. Ce n'est qu'un début.",
                    "Aucun contenu ici$subjectPart. Athéna préfère la substance au décor."
                )
                MicroCopyKey.GENERIC_CONFIRMATION -> listOf(
                    "Confirme seulement si la conséquence est claire pour toi.",
                    "Avant d'agir, vérifie que ton choix suit bien ton intention.",
                    "Décide quand tu es prêt. Une bonne décision commence par une bonne lecture."
                )
            }
            else -> when (key) {
                MicroCopyKey.GENERIC_SUCCESS -> listOf(
                    "C'est validé$subjectPart. L'Olympe hoche la tête avec une gravité très professionnelle.",
                    "Action réussie$subjectPart. Même les colonnes semblent un peu fières.",
                    "Parfait$subjectPart. Enfin… suffisamment parfait pour aujourd'hui."
                )
                MicroCopyKey.GENERIC_ERROR_SOFT -> listOf(
                    "Petit accroc$subjectPart$outcomePart. Rien qu'un héros ne puisse corriger.",
                    "Le rituel a glissé$subjectPart. On réessaie sans drame divin.",
                    "Quelque chose a coincé$subjectPart$outcomePart. Même l'Olympe a ses jours brouillons."
                )
                MicroCopyKey.GENERIC_SAVED -> listOf(
                    "C'est sauvegardé$subjectPart. Les parchemins peuvent dormir tranquilles.",
                    "Enregistré$subjectPart. Une bonne chose de faite avant la prochaine catastrophe éducative.",
                    "Archive mise à jour$subjectPart. Les dieux adorent quand les choses ne se perdent pas."
                )
                MicroCopyKey.GENERIC_RETRY -> listOf(
                    "On retente. Avec un peu de grâce et moins de chaos.",
                    "Réessaie, le Panthéon n'a pas fermé boutique.",
                    "Encore un essai. Cette fois, même Hermès devrait suivre."
                )
                MicroCopyKey.GENERIC_EMPTY -> listOf(
                    "Rien ici$subjectPart. C'est très aéré, mais peu utile.",
                    "Le coin est vide$subjectPart. Même un dieu aurait du mal à improviser là-dessus.",
                    "Aucun contenu détecté$subjectPart. L'écho est joli, pas la progression."
                )
                MicroCopyKey.GENERIC_CONFIRMATION -> listOf(
                    "Confirme si tu veux vraiment lancer cela.",
                    "Dernière vérification avant l'action.",
                    "Un petit choix maintenant, un grand effet peut-être juste après."
                )
            }
        }

        return variants.random(Random(System.nanoTime()))
    }
}

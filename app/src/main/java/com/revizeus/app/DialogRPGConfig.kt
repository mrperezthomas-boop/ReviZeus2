package com.revizeus.app

import java.io.Serializable

/**
 * ═══════════════════════════════════════════════════════════════
 * DIALOG RPG CONFIG — Configuration complète d'un dialogue RPG
 * ═══════════════════════════════════════════════════════════════
 * 
 * BLOC B — DIALOGUES RPG UNIVERSELS
 * 
 * Utilité :
 * - Data class qui définit l'apparence, le comportement et le contenu
 *   d'un dialogue RPG universel.
 * - Utilisée par DialogRPGManager et DialogRPGFragment pour créer
 *   des dialogues narratifs immersifs avec typewriter, chibi animé,
 *   blip sonore, et sélection automatique du dieu parlant.
 * 
 * Exemples d'usage :
 * 
 * // Dialogue info simple
 * DialogRPGConfig(
 *     mainText = "Ton résumé a été sauvegardé !",
 *     godId = "athena",
 *     category = DialogCategory.INFO
 * )
 * 
 * // Dialogue confirmation
 * DialogRPGConfig(
 *     mainText = "Es-tu certain de vouloir supprimer ce héros ?",
 *     godId = "zeus",
 *     category = DialogCategory.CONFIRMATION,
 *     button1Label = "CONFIRMER",
 *     button1Action = { deleteHero() },
 *     button2Label = "ANNULER"
 * )
 * 
 * // Dialogue récompense avec détails
 * DialogRPGConfig(
 *     mainText = "Bravo ! Tu as débloqué un nouveau badge !",
 *     godId = "zeus",
 *     category = DialogCategory.REWARD,
 *     additionalLabel = "DÉTAILS",
 *     additionalText = "Badge « Marathonien du Savoir » : 100 quiz complétés",
 *     button1Label = "MERCI ! ⚡"
 * )
 * 
 * Architecture :
 * - Serializable pour passer via Bundle entre Activity/Fragment
 * - Tous les champs optionnels sauf mainText
 * - Actions de boutons stockées comme lambdas (non-sérialisables,
 *   donc transmises via companion object temporaire)
 * 
 * Évolutions futures :
 * - Support TTS optionnel pour dialogues longs
 * - Vibration synchronisée avec blip
 * - Animations de fond selon catégorie
 * - Support pagination pour textes très longs
 * 
 * ═══════════════════════════════════════════════════════════════
 */
data class DialogRPGConfig(
    // ── CONTENU TEXTUEL ──
    
    /**
     * Texte principal affiché avec effet typewriter.
     * OBLIGATOIRE - Le message narratif du dieu.
     */
    val mainText: String,
    
    /**
     * ID du dieu parlant.
     * Détermine le chibi affiché et le nom du dieu.
     * Valeurs possibles : "zeus", "athena", "poseidon", "ares", "apollo",
     * "hephaestus", "demeter", "hermes", "aphrodite", "prometheus"
     * 
     * Par défaut : "zeus" (dieu de l'autorité et des décisions majeures)
     */
    val godId: String = "zeus",
    
    /**
     * Titre optionnel affiché en haut du dialogue.
     * Si null, la zone de titre est masquée.
     * Exemple : "⚡ DÉCISION DIVINE ⚡", "Athéna — Explication"
     */
    val title: String? = null,
    
    /**
     * Label de la zone additionnelle optionnelle.
     * Si additionalText est fourni, ce label s'affiche au-dessus.
     * Exemples : "DÉTAILS", "MNÉMO DIVIN", "EXPLICATION"
     */
    val additionalLabel: String? = null,
    
    /**
     * Texte additionnel optionnel (statique, pas de typewriter).
     * Utile pour afficher des détails, un mnémotechnique, une explication
     * complémentaire, des statistiques, etc.
     */
    val additionalText: String? = null,
    
    // ── APPARENCE & CATÉGORIE ──
    
    /**
     * Catégorie visuelle du dialogue.
     * Détermine les couleurs, le fond, l'ambiance générale.
     * Voir DialogCategory enum pour la liste complète.
     * 
     * Par défaut : INFO (neutre, fond standard)
     */
    val category: DialogCategory = DialogCategory.INFO,
    
    // ── BOUTONS ──
    
    /**
     * Label du bouton principal (bouton 1).
     * Toujours visible. Exemples : "COMPRIS ⚡", "CONFIRMER", "MERCI !"
     * 
     * Par défaut : "COMPRIS ⚡"
     */
    val button1Label: String = "COMPRIS ⚡",
    
    /**
     * Action exécutée au clic sur le bouton 1.
     * Si null, le bouton ferme simplement le dialogue.
     * 
     * NOTE : Les lambdas ne sont pas Serializable. Pour passer les actions
     * via Bundle, DialogRPGManager utilise un companion object temporaire.
     */
    val button1Action: (() -> Unit)? = null,
    
    /**
     * Label du bouton secondaire (bouton 2).
     * Si null, le bouton 2 est masqué.
     * Exemples : "ANNULER", "REFUSER", "PLUS TARD"
     */
    val button2Label: String? = null,
    
    /**
     * Action exécutée au clic sur le bouton 2.
     * Si null, le bouton ferme simplement le dialogue.
     */
    val button2Action: (() -> Unit)? = null,
    
    /**
     * Label du bouton tertiaire (bouton 3).
     * Si null, le bouton 3 est masqué.
     * Exemples : "AUTRE CHOIX", "AIDE", "DÉTAILS"
     */
    val button3Label: String? = null,
    
    /**
     * Action exécutée au clic sur le bouton 3.
     * Si null, le bouton ferme simplement le dialogue.
     */
    val button3Action: (() -> Unit)? = null,
    
    // ── COMPORTEMENT ──
    
    /**
     * Le dialogue peut-il être fermé en appuyant sur back ou en touchant
     * à l'extérieur ?
     * 
     * Par défaut : true
     * Mettre à false pour forcer l'utilisateur à choisir un bouton.
     */
    val cancelable: Boolean = true,
    
    /**
     * L'utilisateur peut-il afficher tout le texte instantanément en tapant
     * sur la zone narrative ?
     * 
     * Par défaut : true
     * Tap 1 : Afficher tout le texte immédiatement
     * Tap 2+ : Rien (ou fermer si bouton unique - à implémenter)
     */
    val tapToSkipTypewriter: Boolean = true,
    
    /**
     * Vitesse du typewriter (délai entre chaque caractère en millisecondes).
     * 
     * Par défaut : 35ms (vitesse standard de GodSpeechAnimator)
     * Valeurs recommandées : 25-50ms
     */
    val typewriterSpeed: Long = 35L,
    
    // ── CALLBACKS ──
    
    /**
     * Callback exécuté à la fermeture du dialogue (dismiss).
     * Appelé que le dialogue soit fermé via bouton, back, ou tap extérieur.
     */
    val onDismiss: (() -> Unit)? = null
) : Serializable {
    
    companion object {
        /**
         * Version de sérialisation.
         * Incrémenter si la structure de DialogRPGConfig change.
         */
        private const val serialVersionUID = 1L
        
        /**
         * Stockage temporaire des actions de boutons (non-sérialisables).
         * Utilisé par DialogRPGManager pour transmettre les lambdas
         * au DialogRPGFragment via un ID unique.
         * 
         * Clé : ID unique généré par DialogRPGManager
         * Valeur : Map des actions (button1, button2, button3, onDismiss)
         */
        @JvmStatic
        internal val pendingActions = mutableMapOf<String, Map<String, (() -> Unit)?>>()
        
        /**
         * Nettoie les actions temporaires d'un dialogue.
         * Appelé par DialogRPGFragment dans onDestroyView().
         */
        @JvmStatic
        internal fun clearActions(actionId: String) {
            pendingActions.remove(actionId)
        }
    }
}

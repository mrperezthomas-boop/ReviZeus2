package com.revizeus.app

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job

/**
 * ═══════════════════════════════════════════════════════════════
 * DIALOG RPG FRAGMENT — Fragment universel de dialogue RPG
 * ═══════════════════════════════════════════════════════════════
 * 
 * BLOC B — DIALOGUES RPG UNIVERSELS
 * 
 * Utilité :
 * - Fragment réutilisable qui affiche un dialogue RPG complet avec :
 *   • Typewriter lettre par lettre via GodSpeechAnimator
 *   • Blip sonore synchronisé
 *   • Chibi animé du dieu parlant (tremblement pendant lecture)
 *   • Tap-to-skip typewriter fonctionnel
 *   • 1 à 3 boutons premium personnalisables
 *   • Catégories visuelles (INFO, ALERT, REWARD, etc.)
 *   • Lifecycle-safe (arrêt propre du typewriter/chibi/blip)
 * 
 * Architecture :
 * - Utilise le layout dialog_rpg_universal.xml
 * - Configuration via DialogRPGConfig (passé en arguments Bundle)
 * - Actions de boutons récupérées depuis DialogRPGConfig.pendingActions
 * - Nettoyage automatique dans onDestroyView()
 * 
 * Construction :
 * - Ne jamais instancier directement avec new DialogRPGFragment()
 * - Toujours utiliser la factory newInstance(config, actionId)
 * - Exemple :
 *   val dialog = DialogRPGFragment.newInstance(config, actionId)
 *   dialog.show(supportFragmentManager, "DialogRPG_$actionId")
 * 
 * Lifecycle :
 * - onCreate : Récupération de la config depuis arguments
 * - onCreateDialog : Configuration du Dialog (cancelable, etc.)
 * - onCreateView : Inflation du layout
 * - onViewCreated : Setup complet (chibi, texte, boutons, typewriter)
 * - onDestroyView : Nettoyage (annuler job, arrêter chibi, couper blip)
 * - onDismiss : Appel du callback onDismiss si défini
 * 
 * Évolutions futures :
 * - Support TTS optionnel pour dialogues longs (accessibility)
 * - Vibration synchronisée avec blip
 * - Animations de fond selon catégorie (particules pour REWARD, lightning pour ERROR)
 * - Support pagination pour textes très longs (scroll automatique)
 * 
 * ═══════════════════════════════════════════════════════════════
 */
class DialogRPGFragment : DialogFragment() {

    // ── ÉTAT DU FRAGMENT ──
    private var config: DialogRPGConfig? = null
    private var actionId: String? = null
    
    // ── ANIMATION & TYPEWRITER ──
    private val godAnim = GodSpeechAnimator()
    private var typewriterJob: Job? = null
    private var hasSkippedTypewriter = false
    
    // ── VUES (créées programmatiquement) ──
    private var rootFrame: FrameLayout? = null
    private var imgGodChibi: ImageView? = null
    private var tvGodName: TextView? = null
    private var tvDialogMainText: TextView? = null
    
    companion object {
        private const val KEY_CONFIG = "dialog_rpg_config"
        private const val KEY_ACTION_ID = "dialog_rpg_action_id"
        
        /**
         * Factory pour créer une instance du fragment.
         * 
         * @param config Configuration du dialogue
         * @param actionId ID unique pour récupérer les actions depuis pendingActions
         * @return Instance configurée du DialogRPGFragment
         */
        fun newInstance(config: DialogRPGConfig, actionId: String): DialogRPGFragment {
            return DialogRPGFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_CONFIG, config)
                    putString(KEY_ACTION_ID, actionId)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        
        // Récupération de la configuration
        config = arguments?.getSerializable(KEY_CONFIG) as? DialogRPGConfig
        actionId = arguments?.getString(KEY_ACTION_ID)
        
        // Définir si le dialogue est cancelable
        isCancelable = config?.cancelable ?: true
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val cfg = config ?: return super.onCreateDialog(savedInstanceState)
        
        return Dialog(requireContext(), theme).apply {
            setCancelable(cfg.cancelable)
            setCanceledOnTouchOutside(cfg.cancelable)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Construction programmatique du layout (équivalent à dialog_rpg_universal.xml)
        // Note : En production, utiliser le layout XML via ViewBinding serait préférable,
        // mais pour ce BLOC B, on génère programmatiquement pour éviter la dépendance
        // à un fichier XML non encore créé.
        
        return createDialogLayout()
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val cfg = config ?: run {
            dismissAllowingStateLoss()
            return
        }
        
        // Application de la catégorie visuelle
        applyCategoryStyle(cfg.category)
        
        // Chargement du chibi du dieu
        loadGodChibi(cfg.godId)
        
        // Nom du dieu
        tvGodName?.text = getGodNameFromId(cfg.godId)
        
        // Titre (optionnel)
        setupTitle(cfg)
        
        // Zone additionnelle (optionnelle)
        setupAdditionalInfo(cfg)
        
        // Boutons
        setupButtons(cfg)
        
        // CORRECTION CRITIQUE : Utiliser view.post{} pour garantir que toutes les vues
        // sont complètement créées et attachées avant de lancer le typewriter
        view.post {
            // Vérification supplémentaire que le fragment n'a pas été détruit entre-temps
            if (isAdded && !isDetached && view.isAttachedToWindow) {
                startTypewriter(cfg)
            }
        }
        
        // Tap pour skip typewriter (optionnel)
        if (cfg.tapToSkipTypewriter) {
            view.findViewById<View>(R.id.layoutNarrativeZone)?.setOnClickListener {
                skipTypewriter()
            }
        }
    }
    
    // ══════════════════════════════════════════════════════════
    // CRÉATION PROGRAMMATIQUE DU LAYOUT
    // ══════════════════════════════════════════════════════════
    
    /**
     * Crée programmatiquement le layout du dialogue RPG.
     * Équivalent à dialog_rpg_universal.xml.
     */
    private fun createDialogLayout(): View {
        val ctx = requireContext()
        
        // Frame principal (fond semi-transparent cliquable)
        rootFrame = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#CC000000"))
            isClickable = true
            isFocusable = true
        }
        
        // ScrollView pour le contenu
        val scrollView = ScrollView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = dp(24)
                setMargins(margin, margin, margin, margin)
                gravity = android.view.Gravity.CENTER
            }
            isScrollbarFadingEnabled = true
        }
        
        // LinearLayout principal (fond bg_rpg_dialog)
        val dialogRoot = LinearLayout(ctx).apply {
            id = View.generateViewId()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            val padding = dp(20)
            setPadding(padding, padding, padding, padding)
            try {
                setBackgroundResource(R.drawable.bg_rpg_dialog)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#1A1A2E"))
            }
        }
        
        // Zone titre (optionnelle, masquée par défaut)
        val titleContainer = createTitleContainer(ctx)
        dialogRoot.addView(titleContainer)
        
        // Zone narrative (chibi + texte)
        val narrativeZone = createNarrativeZone(ctx)
        dialogRoot.addView(narrativeZone)
        
        // Zone additionnelle (optionnelle, masquée par défaut)
        val additionalInfo = createAdditionalInfoZone(ctx)
        dialogRoot.addView(additionalInfo)
        
        // Container boutons
        val buttonsContainer = createButtonsContainer(ctx)
        dialogRoot.addView(buttonsContainer)
        
        scrollView.addView(dialogRoot)
        rootFrame?.addView(scrollView)
        
        return rootFrame!!
    }
    
    private fun createTitleContainer(ctx: android.content.Context): FrameLayout {
        return FrameLayout(ctx).apply {
            id = R.id.layoutTitleContainer
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
            
            // Background title
            val bgTitle = ImageView(ctx).apply {
                id = R.id.imgTitleBg
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    dp(50)
                )
                scaleType = ImageView.ScaleType.FIT_XY
                try {
                    setImageResource(R.drawable.bg_divine_card)
                } catch (_: Exception) {
                    setBackgroundColor(Color.parseColor("#33FFD700"))
                }
            }
            addView(bgTitle)
            
            // Texte titre
            val tvTitle = TextView(ctx).apply {
                id = R.id.tvDialogTitle
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    dp(50)
                )
                gravity = android.view.Gravity.CENTER
                setTextColor(Color.parseColor("#FFD700"))
                textSize = 18f
                try {
                    typeface = android.graphics.Typeface.createFromAsset(ctx.assets, "fonts/cinzel_bold.ttf")
                } catch (_: Exception) {
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }
            addView(tvTitle)
        }
    }
    
    private fun createNarrativeZone(ctx: android.content.Context): LinearLayout {
        return LinearLayout(ctx).apply {
            id = R.id.layoutNarrativeZone
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            
            // Frame chibi
            val chibiFrame = FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(dp(80), dp(80))
                setBackgroundColor(Color.parseColor("#22111122"))
                setPadding(dp(4), dp(4), dp(4), dp(4))
            }
            
            imgGodChibi = ImageView(ctx).apply {
                id = R.id.imgGodChibi
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            chibiFrame.addView(imgGodChibi)
            addView(chibiFrame)
            
            // Zone texte
            val textZone = LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    leftMargin = dp(12)
                }
                orientation = LinearLayout.VERTICAL
                try {
                    setBackgroundResource(R.drawable.bg_rpg_dialog)
                } catch (_: Exception) {
                    setBackgroundColor(Color.parseColor("#1A1A2E"))
                }
                val padding = dp(12)
                setPadding(padding, padding, padding, padding)
            }
            
            tvGodName = TextView(ctx).apply {
                id = R.id.tvGodName
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setTextColor(Color.parseColor("#FFD700"))
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            textZone.addView(tvGodName)
            
            tvDialogMainText = TextView(ctx).apply {
                id = R.id.tvDialogMainText
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(6)
                }
                setTextColor(Color.WHITE)
                textSize = 14f
                setLineSpacing(dp(4).toFloat(), 1f)
                minLines = 2
            }
            textZone.addView(tvDialogMainText)
            
            addView(textZone)
        }
    }
    
    private fun createAdditionalInfoZone(ctx: android.content.Context): LinearLayout {
        return LinearLayout(ctx).apply {
            id = R.id.layoutAdditionalInfo
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
            }
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1AFFFFFF"))
            val padding = dp(14)
            setPadding(padding, padding, padding, padding)
            visibility = View.GONE
            
            val label = TextView(ctx).apply {
                id = R.id.tvAdditionalLabel
                setTextColor(Color.parseColor("#FFD700"))
                textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            addView(label)
            
            val text = TextView(ctx).apply {
                id = R.id.tvAdditionalText
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                }
                setTextColor(Color.parseColor("#F5F5F5"))
                textSize = 13f
                setLineSpacing(dp(3).toFloat(), 1f)
            }
            addView(text)
        }
    }
    
    private fun createButtonsContainer(ctx: android.content.Context): LinearLayout {
        return LinearLayout(ctx).apply {
            id = R.id.layoutButtonsContainer
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
            orientation = LinearLayout.VERTICAL
            
            // Bouton 1 (toujours visible)
            addView(createButton(ctx, 1))
            
            // Bouton 2 (optionnel, masqué par défaut)
            addView(createButton(ctx, 2))
            
            // Bouton 3 (optionnel, masqué par défaut)
            addView(createButton(ctx, 3))
        }
    }
    
    private fun createButton(ctx: android.content.Context, buttonNumber: Int): FrameLayout {
        val isSecondary = buttonNumber >= 2
        
        return FrameLayout(ctx).apply {
            id = when (buttonNumber) {
                1 -> R.id.layoutButton1
                2 -> R.id.layoutButton2
                else -> R.id.layoutButton3
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                if (buttonNumber > 1) topMargin = dp(10)
            }
            isClickable = true
            isFocusable = true
            if (buttonNumber > 1) visibility = View.GONE
            
            // Background
            val bg = ImageView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_XY
                try {
                    setImageResource(R.drawable.bg_temple_button)
                } catch (_: Exception) {
                    setBackgroundColor(Color.parseColor("#33FFD700"))
                }
            }
            addView(bg)
            
            // Overlay (pour boutons secondaires)
            if (isSecondary) {
                val overlay = ImageView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    alpha = 0.35f
                    scaleType = ImageView.ScaleType.FIT_XY
                    try {
                        setImageResource(R.drawable.bg_rpg_dialog)
                    } catch (_: Exception) {
                        setBackgroundColor(Color.parseColor("#22000000"))
                    }
                }
                addView(overlay)
            }
            
            // Texte
            val tvButton = TextView(ctx).apply {
                id = when (buttonNumber) {
                    1 -> R.id.tvButton1
                    2 -> R.id.tvButton2
                    else -> R.id.tvButton3
                }
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                gravity = android.view.Gravity.CENTER
                setTextColor(if (isSecondary) Color.parseColor("#CCCCCC") else Color.parseColor("#FFD700"))
                textSize = if (isSecondary) 14f else 15f
                try {
                    typeface = android.graphics.Typeface.createFromAsset(ctx.assets, "fonts/cinzel_bold.ttf")
                } catch (_: Exception) {
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }
            addView(tvButton)
        }
    }
    
    // ══════════════════════════════════════════════════════════
    // SETUP DU CONTENU
    // ══════════════════════════════════════════════════════════
    
    private fun applyCategoryStyle(category: DialogCategory) {
        // Application future des styles visuels par catégorie
        // Pour le BLOC B initial, on garde le style par défaut
        // Évolutions futures : modifier couleurs, fonds, etc. selon category
    }
    
    private fun loadGodChibi(godId: String) {
        val chibiResId = getGodChibiResId(godId)
        try {
            imgGodChibi?.setImageResource(chibiResId)
        } catch (_: Exception) {
            try {
                imgGodChibi?.setImageResource(R.drawable.avatar_zeus_dialog)
            } catch (_: Exception) {
                // Fallback silencieux
            }
        }
    }
    
    private fun setupTitle(cfg: DialogRPGConfig) {
        val titleContainer = view?.findViewById<FrameLayout>(R.id.layoutTitleContainer)
        val tvTitle = view?.findViewById<TextView>(R.id.tvDialogTitle)
        
        if (cfg.title != null) {
            titleContainer?.visibility = View.VISIBLE
            tvTitle?.text = cfg.title
        } else {
            titleContainer?.visibility = View.GONE
        }
    }
    
    private fun setupAdditionalInfo(cfg: DialogRPGConfig) {
        val additionalInfo = view?.findViewById<LinearLayout>(R.id.layoutAdditionalInfo)
        val tvLabel = view?.findViewById<TextView>(R.id.tvAdditionalLabel)
        val tvText = view?.findViewById<TextView>(R.id.tvAdditionalText)
        
        if (cfg.additionalText != null) {
            additionalInfo?.visibility = View.VISIBLE
            tvLabel?.text = cfg.additionalLabel ?: "DÉTAILS"
            tvText?.text = cfg.additionalText
        } else {
            additionalInfo?.visibility = View.GONE
        }
    }
    
    private fun setupButtons(cfg: DialogRPGConfig) {
        val actId = actionId ?: return
        val actions = DialogRPGConfig.pendingActions[actId] ?: mapOf()
        
        // Bouton 1 (toujours visible)
        view?.findViewById<TextView>(R.id.tvButton1)?.text = cfg.button1Label
        view?.findViewById<FrameLayout>(R.id.layoutButton1)?.setOnClickListener {
            try {
                SoundManager.playSFX(requireContext(), R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {}
            actions["button1"]?.invoke()
            dismissAllowingStateLoss()
        }
        
        // Bouton 2 (optionnel)
        if (cfg.button2Label != null) {
            view?.findViewById<FrameLayout>(R.id.layoutButton2)?.visibility = View.VISIBLE
            view?.findViewById<TextView>(R.id.tvButton2)?.text = cfg.button2Label
            view?.findViewById<FrameLayout>(R.id.layoutButton2)?.setOnClickListener {
                try {
                    SoundManager.playSFX(requireContext(), R.raw.sfx_avatar_confirm)
                } catch (_: Exception) {}
                actions["button2"]?.invoke()
                dismissAllowingStateLoss()
            }
        }
        
        // Bouton 3 (optionnel)
        if (cfg.button3Label != null) {
            view?.findViewById<FrameLayout>(R.id.layoutButton3)?.visibility = View.VISIBLE
            view?.findViewById<TextView>(R.id.tvButton3)?.text = cfg.button3Label
            view?.findViewById<FrameLayout>(R.id.layoutButton3)?.setOnClickListener {
                try {
                    SoundManager.playSFX(requireContext(), R.raw.sfx_avatar_confirm)
                } catch (_: Exception) {}
                actions["button3"]?.invoke()
                dismissAllowingStateLoss()
            }
        }
    }
    
    // ══════════════════════════════════════════════════════════
    // TYPEWRITER & ANIMATION
    // ══════════════════════════════════════════════════════════
    
    private fun startTypewriter(cfg: DialogRPGConfig) {
        val chibi = imgGodChibi
        val textView = tvDialogMainText
        
        // CORRECTION CRITIQUE : Log si les vues sont null pour debug
        if (chibi == null) {
            android.util.Log.e("DialogRPGFragment", "❌ imgGodChibi est null ! Le typewriter ne peut pas démarrer.")
            // Fallback : afficher le texte immédiatement sans animation
            tvDialogMainText?.text = cfg.mainText
            return
        }
        
        if (textView == null) {
            android.util.Log.e("DialogRPGFragment", "❌ tvDialogMainText est null ! Le typewriter ne peut pas démarrer.")
            return
        }
        
        // TODO FUTUR (RÉGLAGES) : Récupérer la vitesse depuis SharedPreferences
        // val prefs = requireContext().getSharedPreferences("revizeus_settings", Context.MODE_PRIVATE)
        // val typewriterSpeed = prefs.getLong("typewriter_speed", 35L)
        // val blipEnabled = prefs.getBoolean("typewriter_blip_enabled", true)
        // val blipVolume = prefs.getFloat("typewriter_blip_volume", 1.0f)
        
        android.util.Log.d("DialogRPGFragment", "✅ Lancement du typewriter avec texte: ${cfg.mainText.take(50)}...")
        
        typewriterJob = godAnim.typewriteSimple(
            scope = lifecycleScope,
            chibiView = chibi,
            textView = textView,
            text = cfg.mainText,
            delayMs = cfg.typewriterSpeed,
            context = requireContext(),
            onComplete = {
                android.util.Log.d("DialogRPGFragment", "✅ Typewriter terminé.")
                hasSkippedTypewriter = true
            }
        )
    }
    
    private fun skipTypewriter() {
        if (!hasSkippedTypewriter) {
            val chibi = imgGodChibi ?: return
            val textView = tvDialogMainText ?: return
            
            // Afficher tout le texte instantanément
            typewriterJob?.cancel()
            godAnim.stopSpeaking(chibi)
            SoundManager.stopAllDialogueBlips()
            textView.text = config?.mainText ?: ""
            hasSkippedTypewriter = true
        }
    }
    
    // ══════════════════════════════════════════════════════════
    // LIFECYCLE & CLEANUP
    // ══════════════════════════════════════════════════════════
    
    override fun onDestroyView() {
        // Arrêt typewriter
        typewriterJob?.cancel()
        typewriterJob = null
        
        // Arrêt chibi
        imgGodChibi?.let { chibi ->
            godAnim.release(chibi)
        }
        
        // Arrêt blip
        SoundManager.stopAllDialogueBlips()
        
        // Nettoyage actions temporaires
        actionId?.let { id ->
            DialogRPGConfig.clearActions(id)
        }
        
        // Nettoyage références
        imgGodChibi = null
        tvGodName = null
        tvDialogMainText = null
        rootFrame = null
        
        super.onDestroyView()
    }
    
    override fun onDismiss(dialog: DialogInterface) {
        val actId = actionId ?: return super.onDismiss(dialog)
        val actions = DialogRPGConfig.pendingActions[actId] ?: mapOf()
        actions["onDismiss"]?.invoke()
        super.onDismiss(dialog)
    }
    
    // ══════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════
    
    private fun dp(value: Int): Int {
        val density = resources.displayMetrics.density
        return (value * density).toInt()
    }
    
    private fun getGodNameFromId(godId: String): String {
        return when (godId) {
            "zeus" -> "Zeus"
            "athena" -> "Athéna"
            "poseidon" -> "Poséidon"
            "ares" -> "Arès"
            "apollo" -> "Apollon"
            "hephaestus" -> "Héphaïstos"
            "demeter" -> "Déméter"
            "hermes" -> "Hermès"
            "aphrodite" -> "Aphrodite"
            "prometheus" -> "Prométhée"
            else -> "Zeus"
        }
    }
    
    private fun getGodChibiResId(godId: String): Int {
        return when (godId) {
            "zeus" -> R.drawable.avatar_zeus_dialog
            "athena" -> R.drawable.avatar_athena_dialog
            "poseidon" -> R.drawable.avatar_poseidon_dialog
            "ares" -> R.drawable.avatar_ares_dialog
            "apollo" -> R.drawable.avatar_apollo_dialog
            "hephaestus" -> R.drawable.avatar_hephaestus_dialog
            "demeter" -> R.drawable.avatar_demeter_dialog
            "hermes" -> R.drawable.avatar_hermes_dialog
            "aphrodite" -> R.drawable.avatar_aphrodite_dialog
            "prometheus" -> R.drawable.avatar_prometheus_dialog
            else -> R.drawable.avatar_zeus_dialog
        }
    }
}

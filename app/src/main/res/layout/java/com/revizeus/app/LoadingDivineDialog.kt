package com.revizeus.app

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.util.Log
import androidx.fragment.app.DialogFragment
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.load
import kotlin.random.Random

/**
 * ═══════════════════════════════════════════════════════════════
 * LOADING DIVINE DIALOG — VERROU GLOBAL DES ATTENTES IA
 * ═══════════════════════════════════════════════════════════════
 * Utilité :
 * - Bloque totalement l'UI pendant les appels IA ou les générations longues
 * - Affiche le chibi animé loading_chibi.webp
 * - Joue une boucle sonore légère sans couper la BGM principale
 * - Affiche une phrase WTF aléatoire qui change toutes les 7 secondes
 *
 * EXTENSION CONSERVATIVE — MODE MUSIQUE DIVINE :
 * - Conservation de toutes les phrases normales déjà existantes
 * - Ajout d'un second mode dédié à la génération musicale / Lyria
 * - Possibilité d'injecter le dieu courant pour personnaliser les phrases
 * - Aucun renommage destructeur des usages existants
 *
 * Notes techniques :
 * - isCancelable = false pour empêcher toute fermeture manuelle
 * - plein écran portrait / fond sombre semi-transparent
 * - Coil + ImageDecoderDecoder.Factory() pour animer le WebP sur Android compatibles
 * ═══════════════════════════════════════════════════════════════
 */
class LoadingDivineDialog : DialogFragment() {
    private val loadingTag = "REVIZEUS_LOADING"

    /**
     * Phrase affichée sous le chibi.
     * Chaque ouverture pioche une entrée au hasard pour casser la monotonie.
     */
    private val divineMessages = listOf(
        "Hermès livre vos données (il a pris un raccourci par les Enfers...)",
        "Consultation des Moires en cours... elles se disputent le fil.",
        "Zeus cherche ses lunettes pour lire votre réponse.",
        "Nettoyage des parchemins à l'eau bénite...",
        "Aiguisage des crayons d'Athéna. Un instant.",
        "Poseidon éponge le serveur, il y a eu une fuite.",
        "Calcul de votre destin intellectuel... (Ne paniquez pas).",
        "Extraction du savoir pur... ça picote un peu.",
        "Vérification que vous n'êtes pas un Titan infiltré.",
        "Le Sphinx refuse de répondre, on négocie...",
        "Déméter arrose tes neurones... Attention, ça va pousser d'un coup.",
        "Calcul de l'angle exact pour que l'éclair de Zeus ne grille pas ton Wi‑Fi.",
        "Négociation avec le Minotaure : il veut bien te laisser passer contre un cookie.",
        "Affûtage des flèches d'Artémis. Les fautes d'orthographe n'ont aucune chance.",
        "Récupération de ta motivation dans les tréfonds du Tartare... Trouvée !",
        "Ajustement de ta couronne de laurier. Il faut être stylé pour réviser.",
        "Hermès fait le plein de ses sandales ailées. C'est du Sans-Plomb 98.",
        "Vérification des lois de la physique... La gravité est toujours activée. Zut.",
        "Nettoyage du miroir de Narcisse. On voit ton génie d'ici !",
        "L'IA est en train de boire une ambroisie noisette avant de te répondre.",
        "Héphaïstos tape sur le serveur avec son marteau. Ça devrait repartir.",
        "Arès mène une guerre sans merci contre les bugs de connexion.",
        "Aphrodite retouche ton avatar... Tu es sublime, ne change rien.",
        "Chronos prend son temps. Littéralement. C'est son boulot.",
        "Héra vérifie si tu n'as pas trompé tes révisions avec une autre application.",
        "Dionysos a renversé du vin sur la carte mère. On éponge.",
        "Apollon accorde sa lyre... et ton processeur par la même occasion.",
        "Perséphone est remontée des Enfers juste pour valider ton score.",
        "Pan joue de la flûte pour calmer le ventilateur de ton téléphone.",
        "Éros décoche une flèche. Attention, tu vas tomber amoureux de cette leçon.",
        "Nettoyage des écuries d'Augias en cours... Préparez les masques.",
        "Recherche de ta réponse dans le Labyrinthe. Le Minotaure a perdu le plan.",
        "Hydre de Lerne détectée : un bug corrigé, deux qui apparaissent. On gère.",
        "Icare vole un peu trop près du Wi-Fi. Signal instable...",
        "Sisyphe pousse ton fichier en haut de la pente. Presque... presque...",
        "Ouverture de la boîte de Pandore... Ah non, c'était juste le cache.",
        "Pégase fait une pause foin. Livraison des données retardée.",
        "Cerbère a mangé tes cookies de navigation. Les trois têtes à la fois.",
        "Le fil d'Ariane est emmêlé. On sort les ciseaux (non, pas ceux d'Atropos !).",
        "Méduse regarde le code de trop près. Il est pétrifié.",
        "Consultation de l'Oracle de Delphes... La réponse est 'Peut-être'.",
        "Pesée de ton âme contre une plume d'oie. C'est serré.",
        "Les Muses sont en grève. On les corrompt avec du nectar.",
        "Atlas fait une pause. Ne bouge pas, le monde pourrait glisser.",
        "Vérification de ton quotient intellectuel chez les Satyres. Ils sont impressionnés.",
        "Lancement de la foudre 2.0. Chargement à 88mph.",
        "Le Styx est en crue. Le passeur demande un supplément bagage.",
        "Traduction du grec ancien vers le langage binaire...",
        "Calcul de la trajectoire du disque de l'intelligence. Vise bien.",
        "Narcisse s'est noyé dans la base de données. On le repêche.",
        "Recharge des batteries de l'Olympe. On a oublié l'adaptateur.",
        "Hermès a activé le mode avion. Mais il n'a pas d'avion.",
        "Midas a touché le bouton 'Enregistrer'. Tout est devenu brillant.",
        "Le Sphinx demande ton mot de passe. Indice : c'est pas 'l'Homme'.",
        "Circé transforme tes erreurs en porcs. C'est plus propre comme ça.",
        "Les Cyclopes installent une webcam. Ils n'en ont besoin que d'une.",
        "Prométhée a volé le code source. On essaie de ne pas se faire griller.",
        "Achille a une douleur au talon. Il va falloir marcher doucement sur les données.",
        "Ulysse a pris un détour par les Cyclades. Arrivée prévue dans 10 ans... ou 2 secondes.",
        "L'ambroisie est en cours de téléchargement. Saveur : 404 Noisette.",
        "Extraction de la sagesse de la barbe de Platon.",
        "Vérification que ton cerveau n'est pas en mode 'Cheval de Troie'.",
        "Éole souffle sur les paquets de données pour qu'ils aillent plus vite.",
        "Les Centaures galopent vers le serveur central.",
        "Morphée te surveille. Ne t'endors pas sur tes révisions !",
        "Socrate se demande si ce chargement existe vraiment.",
        "La Toison d'Or est cachée derrière cette barre de progression.",
        "Tirage au sort de ton prochain éclair de génie.",
        "Les Moires ont rajouté quelques mètres de fil à ton destin. Profites-en.",
        "Connexion établie avec le Wi-Fi du Mont Olympe. Mot de passe : ViveLeZeus."
    )

    /**
     * Phrases spécifiques à la création musicale.
     */
    private val divineMusicMessages = listOf(
        "Apollon accorde sa lyre... Une corde vient de casser, il jure en grec ancien.",
        "Héphaïstos forge le beat dans son volcan. Ça sent un peu le câble fondu.",
        "Hermès court après le tempo. Il dit qu'il revient dans trois battements de cœur.",
        "Athéna relit les paroles pour éviter une rime honteuse devant l'Olympe.",
        "Zeus exige une entrée tonitruante. Les nuages discutent encore de la tonalité.",
        "Poséidon ajoute de la réverbération marine. Oui, même les vagues ont le sens du rythme.",
        "Aphrodite refuse que le refrain soit laid. Elle rajoute du charme à chaque mesure.",
        "Arès veut transformer la chanson en hymne de guerre. On essaie de le calmer.",
        "Déméter fait pousser une mélodie bio, nourrie au soleil et à l'ambroisie.",
        "Prométhée a volé une étincelle de génie pour allumer la musique.",
        "Les Muses répètent le refrain... l'une d'elles chante faux, mais avec conviction.",
        "Le studio divin est occupé. Cerbère garde la porte et réclame un pass VIP.",
        "Apollon teste un solo. Même les colonnes du temple vibrent un peu.",
        "Le Panthéon débat : chef-d'œuvre immortel ou banger céleste ?",
        "Réglage de l'écho olympien... Zeus veut qu'on l'entende jusqu'au Tartare.",
        "Chargement d'une pluie de notes d'or. Merci de ne pas marcher dessus.",
        "Héphaïstos martèle les percussions. Les enclumes ont le groove aujourd'hui.",
        "Préparation d'un refrain si puissant qu'il pourrait réveiller les Titans.",
        "Les Muses injectent la formule mnémotechnique dans la mélodie. Ça va te rester en tête.",
        "Apollon murmure : “Encore une mesure...” mais ça fait déjà douze mesures."
    )



    /**
     * Phrases spécifiques aux attentes IA d'entraînement / quiz.
     */
    private val divineQuizMessages = listOf(
        "Arès aiguise la difficulté. Chaque question passe par sa lame.",
        "Athéna aligne les notions clés pour que rien n'échappe à ton esprit.",
        "Zeus jauge la prochaine épreuve. Il veut un défi digne de l'Olympe.",
        "Les parchemins s'ouvrent un à un. Les questions se forgent dans la lumière.",
        "Hermès transporte les données vers l'arène. Il court plus vite que le doute.",
        "Héphaïstos martèle les distracteurs. Même les mauvaises réponses doivent être crédibles.",
        "Le dieu de ce temple prépare sa prise de parole. Il choisit déjà ses mots.",
        "Les Muses relisent les consignes pour éviter qu'un piège stupide ne gâche l'épreuve.",
        "Prométhée dérobe une étincelle de lucidité pour rendre le quiz plus juste.",
        "Le Panthéon vérifie que chaque question reste fidèle à ton savoir réel."
    )
    private var startedOwnLoopAudio = false
    private var currentDisplayedMessage: String? = null
    private var currentLoadingMode: String = MODE_STANDARD
    private var currentGodName: String? = null

    // Gestionnaire de changement automatique de phrase
    private val handler = Handler(Looper.getMainLooper())
    private var messageUpdateRunnable: Runnable? = null
    private val ROTATION_DELAY = 7000L // 7 secondes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        isCancelable = false

        currentDisplayedMessage = savedInstanceState?.getString(KEY_DISPLAYED_MESSAGE)
        currentLoadingMode = savedInstanceState?.getString(KEY_LOADING_MODE)
            ?: arguments?.getString(ARG_LOADING_MODE)
                    ?: MODE_STANDARD
        currentGodName = savedInstanceState?.getString(KEY_GOD_NAME)
            ?: arguments?.getString(ARG_GOD_NAME)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), theme).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        try {
            if (!SoundManager.isPlayingScanLoop()) {
                SoundManager.playLoopingScan(requireContext(), R.raw.sfx_loading_loop, 0.22f)
                startedOwnLoopAudio = true
            } else {
                startedOwnLoopAudio = false
            }
        } catch (e: Exception) {
            Log.w(loadingTag, "onStart: impossible de lancer la boucle audio du loader", e)
            startedOwnLoopAudio = false
        }

        // Lancement de la rotation des messages
        startMessageRotation()
    }

    override fun onStop() {
        stopOwnedLoopAudioIfNeeded()
        stopMessageRotation()
        super.onStop()
    }

    override fun onDestroyView() {
        stopOwnedLoopAudioIfNeeded()
        stopMessageRotation()
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_DISPLAYED_MESSAGE, currentDisplayedMessage)
        outState.putString(KEY_LOADING_MODE, currentLoadingMode)
        outState.putString(KEY_GOD_NAME, currentGodName)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_loading_divine, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ivLoadingChibi = view.findViewById<ImageView>(R.id.ivLoadingChibi)
        val tvLoadingMessage = view.findViewById<TextView>(R.id.tvLoadingMessage)

        if (currentDisplayedMessage.isNullOrBlank()) {
            currentDisplayedMessage = pickRandomMessageForMode()
        }
        tvLoadingMessage.text = currentDisplayedMessage

        val imageLoader = ImageLoader.Builder(requireContext())
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()

        ivLoadingChibi.load(R.drawable.loading_chibi, imageLoader) {
            crossfade(false)
            allowHardware(false)
        }
    }

    /**
     * Démarre la boucle de mise à jour du texte
     */
    private fun startMessageRotation() {
        messageUpdateRunnable = object : Runnable {
            override fun run() {
                val tvLoadingMessage = view?.findViewById<TextView>(R.id.tvLoadingMessage)
                if (tvLoadingMessage != null) {
                    currentDisplayedMessage = pickRandomMessageForMode()
                    tvLoadingMessage.text = currentDisplayedMessage
                }
                handler.postDelayed(this, ROTATION_DELAY)
            }
        }
        handler.postDelayed(messageUpdateRunnable!!, ROTATION_DELAY)
    }

    /**
     * Arrête la boucle de mise à jour
     */
    private fun stopMessageRotation() {
        messageUpdateRunnable?.let { handler.removeCallbacks(it) }
        messageUpdateRunnable = null
    }

    private fun pickRandomMessageForMode(): String {
        val rawMessage = when (currentLoadingMode) {
            MODE_MUSIC_DIVINE -> divineMusicMessages.random()
            MODE_QUIZ_DIVINE -> divineQuizMessages.random()
            else -> divineMessages[Random.nextInt(divineMessages.size)]
        }

        val godName = currentGodName?.trim().orEmpty()
        return if (godName.isBlank()) {
            rawMessage
        } else {
            rawMessage
                .replace("Apollon", godName, ignoreCase = false)
                .replace("Zeus", godName, ignoreCase = false)
                .replace("Arès", godName, ignoreCase = false)
                .replace("Athéna", godName, ignoreCase = false)
        }
    }

    private fun stopOwnedLoopAudioIfNeeded() {
        if (!startedOwnLoopAudio) return
        try {
            SoundManager.stopLoopingScan()
        } catch (e: Exception) {
            Log.w(loadingTag, "stopOwnedLoopAudioIfNeeded: arrêt boucle audio impossible", e)
        }
        startedOwnLoopAudio = false
    }

    companion object {
        private const val KEY_DISPLAYED_MESSAGE = "loading_divine_displayed_message"
        private const val KEY_LOADING_MODE = "loading_divine_loading_mode"
        private const val KEY_GOD_NAME = "loading_divine_god_name"
        private const val ARG_LOADING_MODE = "arg_loading_mode"
        private const val ARG_GOD_NAME = "arg_god_name"

        const val MODE_STANDARD = "standard"
        const val MODE_MUSIC_DIVINE = "music_divine"
        const val MODE_QUIZ_DIVINE = "quiz_divine"

        fun newInstance(): LoadingDivineDialog {
            return LoadingDivineDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_LOADING_MODE, MODE_STANDARD)
                }
            }
        }

        fun newMusicInstance(godName: String? = null): LoadingDivineDialog {
            return LoadingDivineDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_LOADING_MODE, MODE_MUSIC_DIVINE)
                    putString(ARG_GOD_NAME, godName)
                }
            }
        }

        fun newQuizInstance(godName: String? = null): LoadingDivineDialog {
            return LoadingDivineDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_LOADING_MODE, MODE_QUIZ_DIVINE)
                    putString(ARG_GOD_NAME, godName)
                }
            }
        }
    }
}
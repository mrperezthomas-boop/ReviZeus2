package com.revizeus.app

import android.app.AlertDialog
import android.content.Intent
import android.content.Context
import android.content.ContentValues
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.CourseEntry
import com.revizeus.app.models.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Temple d'une matière : affiche les cours associés au dieu sélectionné.
 * Appui court  = lire le cours.
 * Appui long   = supprimer le cours.
 *
 * PHASE B — LYRE D'APOLLON :
 * ✅ Bouton "Invoquer la Lyre d'Apollon" dans chaque dialogue de cours
 * ✅ Dialogue hymne RPG avec effet typewriter via GodSpeechAnimator
 * ✅ Appel à GodLoreManager.buildEducationalHymn()
 * ✅ Dialog alimenté par dialog_course_hymn.xml
 * ✅ Toute la logique alias/filtrage du temple est conservée intacte
 *
 * EXTENSION CONSERVATIVE — FOND VIDÉO DIVIN :
 * ✅ Chaque temple joue sa vidéo de fond dédiée à l'ouverture
 * ✅ La vidéo ne joue qu'une seule fois par entrée dans le temple
 * ✅ À la fin, elle laisse automatiquement l'image fixe correspondante
 * ✅ Le tout est local à GodMatiereActivity, donc visible uniquement
 *    lorsqu'on entre dans la matière / divinité concernée
 * ✅ Aucun renommage ni suppression de mécanique existante
 *
 * BLOC 4 — CORRECTIONS UX TEMPLE :
 * ✅ La vidéo du temple ne bloque plus les interactions
 * ✅ Les cours restent cliquables pendant toute l'intro vidéo
 * ✅ Ajout d'un en-tête premium bg_divine_card avec opacité 0.7
 * ✅ Le titre du temple reste lisible au-dessus du fond animé / vidéo
 * ✅ Conservation totale du flux de lecture, suppression et lyre
 */
class GodMatiereActivity : BaseActivity() {
    private enum class SummaryBlockType { CHAPTER, SUBTITLE, TEXT }

    private data class SummaryDisplayBlock(
        val type: SummaryBlockType,
        val content: String
    )

    private data class ParsedOracleSummary(
        val title: String,
        val level: String,
        val blocks: List<SummaryDisplayBlock>
    )

    private lateinit var matiere: String
    private lateinit var divinite: String
    private var couleurHex: Int = Color.parseColor("#FFD700")

    private var animatedBackgroundHelper: AnimatedBackgroundHelper? = null
    private var olympianParticlesView: OlympianParticlesView? = null

    // CHANTIER 0 — Matière canonique locale pour fiabiliser le filtrage.
    private lateinit var canonicalMatiere: String

    // CHANTIER 0 — Alias de matière pour requêtes DAO robustes.
    private var subjectAliases: List<String> = emptyList()

    // PHASE B — LYRE D'APOLLON
    // Animateur partagé pour l'effet typewriter du dialogue hymne.
    // Conservé en propriété pour pouvoir l'arrêter dans onPause/onDestroy.
    private val godAnim = GodSpeechAnimator()

    // PHASE B — LYRE D'APOLLON
    // Job du typewriter actif pour annulation propre lors d'un changement de cycle de vie.
    private var hymnTypewriterJob: Job? = null

    // TTS — lecture vocale du résumé de savoir
    private val tts: SpeakerTtsHelper by lazy { SpeakerTtsHelper(this) }

    // ─────────────────────────────────────────────────────────────
    // EXTENSION CONSERVATIVE — FOND VIDÉO DIVIN
    // ─────────────────────────────────────────────────────────────
    // On conserve l'écran entièrement programmatique.
    // Ces vues sont ajoutées derrière le contenu du temple pour créer
    // l'introduction vidéo + le fallback image fixe.
    private var templeBackgroundImageView: ImageView? = null
    private var templeBackgroundVideoView: VideoView? = null

    // BLOC 4 — Nouveau lecteur TextureView-compatible pour que la vidéo
    // reste en vrai fond et ne capture plus la surface tactile.
    private var templeBackgroundPlayerView: PlayerView? = null
    private var templeBackgroundExoPlayer: ExoPlayer? = null

    // Référence conservée vers le vrai contenu principal pour le remettre
    // explicitement au premier plan au-dessus du fond vidéo.
    private var templeMainContentRoot: LinearLayout? = null

    // État local pour éviter de relancer la vidéo pendant le cycle de vie
    // normal de l'Activity (ex: ouverture de dialog, onResume, etc.).
    private var hasPlayedTempleIntroVideo: Boolean = false
    private var isTempleVideoCompleted: Boolean = false
    private var isLyreDialogVisible: Boolean = false
    private var templeBgmResId: Int = 0
    private var hymnMusicTransitionJob: Job? = null

    // BLOC LYRIA — lecteur audio dédié à la musique divine générée.
    // Séparé de la BGM du temple pour pouvoir stopper / relancer proprement.
    private var lyriaMusicPlayer: MediaPlayer? = null

    // BLOC LYRIA — dernier fichier audio généré pour permettre play / stop / sauvegarde.
    private var currentLyriaAudioFilePath: String? = null

    // BLOC LYRIA — loader spécialisé musique divine.
    // On conserve le loader standard existant pour tous les autres cas.
    private var divineMusicLoadingDialog: LoadingDivineDialog? = null

    // LOADER IA GLOBAL — utilisé pour les appels IA quiz / dialogues hors Lyre.
    private var divineGlobalLoadingDialog: LoadingDivineDialog? = null
    private var launchQuizJob: Job? = null
    private var pendingAutoOpenCourseId: String? = null

    // BLOC LYRIA — garde-fou UI pour empêcher les doubles invocations.
    private var isLyriaGenerationRunning: Boolean = false
    private var isLyriaMusicDialogVisible: Boolean = false

    /**
     * BLOC LYRIA — conserve une seule source de vérité pour les paroles :
     * le texte affiché à l'écran est aussi celui envoyé à la Cloud Function.
     *
     * On délègue le nettoyage léger à LyriaManager sans changer le sens du texte.
     */
    private fun getCanonicalLyricsForLyria(response: GeminiManager.GodResponse): String {
        return LyriaManager.normalizeLyricsForSinging(response.text)
    }

    /**
     * Mapping des backgrounds par matière.
     * Les noms correspondent exactement aux nouvelles ressources ajoutées.
     */
    private fun getMatiereBackgroundRes(matiere: String): Int {
        return when (matiere.trim()) {
            "Mathématiques" -> R.drawable.bg_select_maths_zeus
            "Français" -> R.drawable.bg_select_francais_athena
            "SVT" -> R.drawable.bg_select_svt_poseidon
            "Histoire" -> R.drawable.bg_select_histoire_ares
            "Art/Musique", "Art" -> R.drawable.bg_select_art_aphrodite
            "Langues", "Anglais" -> R.drawable.bg_select_anglais_hermes
            "Géographie" -> R.drawable.bg_select_geographie_demeter
            "Physique-Chimie" -> R.drawable.bg_select_physique_hephaistos
            "Philo/SES" -> R.drawable.bg_select_philo_apollon
            "Vie & Projets" -> R.drawable.bg_select_vie_promethee
            else -> R.drawable.bg_select_maths_zeus
        }
    }

    /**
     * Mapping des BGM par matière.
     * Les noms correspondent exactement aux nouvelles ressources ajoutées.
     */
    private fun getMatiereBgmRes(matiere: String): Int {
        return when (matiere.trim()) {
            "Mathématiques" -> R.raw.bgm_select_maths_zeus
            "Français" -> R.raw.bgm_select_francais_athena
            "SVT" -> R.raw.bgm_select_svt_poseidon
            "Histoire" -> R.raw.bgm_select_histoire_ares
            "Art/Musique", "Art" -> R.raw.bgm_select_art_aphrodite
            "Langues", "Anglais" -> R.raw.bgm_select_anglais_hermes
            "Géographie" -> R.raw.bgm_select_geographie_demeter
            "Physique-Chimie" -> R.raw.bgm_select_physique_hephaistos
            "Philo/SES" -> R.raw.bgm_select_philo_apollon
            "Vie & Projets" -> R.raw.bgm_select_vie_promethee
            else -> R.raw.bgm_select_maths_zeus
        }
    }

    /**
     * Mapping vidéo / image fixe des 10 dieux.
     *
     * Convention demandée :
     * revizeus_info_bg_01 à 10 respectivement
     * - en raw pour la vidéo
     * - en drawable pour l'image fixe
     *
     * Comme les noms sont identiques d'un type de ressource à l'autre,
     * on calcule une clé unique puis on résout séparément en raw/drawable.
     */
    private fun getTempleDivineBackgroundKey(matiere: String): String {
        return when (normalizeSubjectKey(matiere)) {
            "Mathématiques" -> "revizeus_info_bg_01"
            "Français" -> "revizeus_info_bg_02"
            "SVT" -> "revizeus_info_bg_03"
            "Histoire" -> "revizeus_info_bg_04"
            "Art/Musique" -> "revizeus_info_bg_05"
            "Langues" -> "revizeus_info_bg_06"
            "Géographie" -> "revizeus_info_bg_07"
            "Physique-Chimie" -> "revizeus_info_bg_08"
            "Philo/SES" -> "revizeus_info_bg_09"
            "Vie & Projets" -> "revizeus_info_bg_10"
            else -> "revizeus_info_bg_01"
        }
    }

    /**
     * Retourne la ressource drawable fixe du temple.
     */
    private fun getTempleDivineStaticImageRes(matiere: String): Int {
        val resName = getTempleDivineBackgroundKey(matiere)
        return resources.getIdentifier(resName, "drawable", packageName)
    }

    /**
     * Retourne la ressource raw vidéo du temple.
     */
    private fun getTempleDivineVideoRes(matiere: String): Int {
        val resName = getTempleDivineBackgroundKey(matiere)
        return resources.getIdentifier(resName, "raw", packageName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        matiere = intent.getStringExtra("MATIERE") ?: "Mathématiques"
        pendingAutoOpenCourseId = intent.getStringExtra("OPEN_COURSE_ID")

        // CHANTIER 0 — Lecture non destructive des nouvelles infos envoyées par SavoirActivity.
        canonicalMatiere = intent.getStringExtra("CANONICAL_SUBJECT")
            ?: normalizeSubjectKey(matiere)

        subjectAliases = intent.getStringArrayExtra("SUBJECT_ALIASES")?.toList()
            ?: getAliasesForSubject(canonicalMatiere)

        // CHANTIER 0 — On sécurise la matière utilisée par l'écran sans supprimer la variable existante.
        matiere = canonicalMatiere

        val godInfo = PantheonConfig.findByMatiere(matiere) ?: PantheonConfig.GODS.first()
        divinite = godInfo.divinite
        couleurHex = godInfo.couleur

        val root = creerInterfaceTemple()
        setContentView(root)
        installerAmbianceOlympienne(root)

        // EXTENSION CONSERVATIVE — on initialise le fond vidéo / image
        // seulement une fois que la vue racine est en place.
        initialiserFondTempleDivin()

        templeBgmResId = getMatiereBgmRes(matiere)
        try {
            SoundManager.rememberMusic(templeBgmResId)
            SoundManager.playMusic(this, templeBgmResId)
        } catch (_: Exception) {}

        // On lance l'intro vidéo du dieu uniquement à l'entrée dans CE temple.
        lancerVideoIntroTempleSiDisponible()

        chargerCours()
    }

    override fun onResume() {
        super.onResume()
        animatedBackgroundHelper?.start(
            accentColor = couleurHex,
            mode = OlympianParticlesView.ParticleMode.TEMPLE
        )

        try {
            if (!isLyreDialogVisible) {
                SoundManager.rememberMusic(templeBgmResId)
                if (!SoundManager.isPlayingMusic() || SoundManager.getCurrentMusicResId() != templeBgmResId) {
                    SoundManager.playMusicDelayed(this, templeBgmResId, 120L)
                }
            }
        } catch (_: Exception) {
        }

        // Si la vidéo avait déjà fini, on s'assure que l'image fixe reste visible.
        if (isTempleVideoCompleted) {
            afficherImageFixeTemple()
        }
    }

    override fun onPause() {
        super.onPause()
        animatedBackgroundHelper?.stop()

        // PHASE B — LYRE D'APOLLON : on coupe proprement l'animation typewriter
        // pour éviter les fuites mémoire si l'Activity passe en arrière-plan.
        hymnTypewriterJob?.cancel()
        hymnMusicTransitionJob?.cancel()
        hymnMusicTransitionJob = null
        launchQuizJob?.cancel()
        tts.stop()

        // BLOC LYRIA — si l'activité passe en arrière-plan, on met la musique
        // générée en pause pour éviter une lecture fantôme hors écran.
        try {
            if (lyriaMusicPlayer?.isPlaying == true) {
                lyriaMusicPlayer?.pause()
            }
        } catch (_: Exception) {
        }

        // EXTENSION CONSERVATIVE — on met la vidéo en pause si elle était en cours,
        // sans relancer quoi que ce soit automatiquement ici.
        try {
            templeBackgroundExoPlayer?.pause()
        } catch (_: Exception) {
        }
        try {
            templeBackgroundVideoView?.pause()
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        animatedBackgroundHelper?.stop()

        // PHASE B — LYRE D'APOLLON : libération définitive de l'animateur.
        hymnTypewriterJob?.cancel()
        hymnMusicTransitionJob?.cancel()
        hymnMusicTransitionJob = null
        launchQuizJob?.cancel()
        tts.release()

        // BLOC LYRIA — fermeture du loader musical et libération du player
        // pour éviter toute fuite audio lorsque le temple est détruit.
        hideDivineMusicLoadingDialog()
        hideGlobalDivineLoadingDialog()
        releaseLyriaMusicPlayer()

        // EXTENSION CONSERVATIVE — nettoyage de la vidéo de fond.
        libererFondTempleVideo()
    }

    private fun creerInterfaceTemple(): FrameLayout {
        val frameRoot = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            /**
             * Background immersif propre à la matière / dieu.
             * Conservé comme fallback ultime si image/vidéo absente.
             */
            setBackgroundResource(getMatiereBackgroundRes(matiere))
        }

        // ── EXTENSION CONSERVATIVE — image fixe du temple ───────────────────
        // Cette image reste cachée pendant la lecture vidéo puis prend le relais
        // une fois l'intro terminée.
        templeBackgroundImageView = ImageView(this).apply {
            tag = "TEMPLE_STATIC_BG"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
            alpha = 1f
            isClickable = false
            isFocusable = false
        }
        frameRoot.addView(templeBackgroundImageView)

        // ── EXTENSION CONSERVATIVE — vidéo d'intro du temple (legacy) ──────
        // Conservé pour compatibilité, mais remplacé en pratique par PlayerView
        // afin d'éviter le problème de SurfaceView qui masque les clics.
        templeBackgroundVideoView = VideoView(this).apply {
            tag = "TEMPLE_VIDEO_BG_LEGACY"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            alpha = 1f
            isClickable = false
            isFocusable = false
        }

        // ── BLOC 4 — vrai fond vidéo interactif-safe via PlayerView ─────────
        // PlayerView + ExoPlayer évitent l'effet "surface au-dessus de tout"
        // qui empêchait de cliquer les cours tant que la vidéo tournait.
        templeBackgroundPlayerView = PlayerView(this).apply {
            tag = "TEMPLE_VIDEO_BG"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            useController = false
            controllerAutoShow = false
            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            setShutterBackgroundColor(Color.TRANSPARENT)
            visibility = View.GONE
            alpha = 1f
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        frameRoot.addView(templeBackgroundPlayerView)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(20, 40, 20, 24)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        templeMainContentRoot = root

        // ── BLOC 4 — En-tête premium bg_divine_card opacité 0.7 ─────────────
        val headerFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(96)
            ).apply {
                bottomMargin = dp(18)
            }
        }

        headerFrame.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            try {
                setImageResource(R.drawable.bg_divine_card)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#AA111827"))
            }
            scaleType = ImageView.ScaleType.FIT_XY
            alpha = 0.70f
            isClickable = false
            isFocusable = false
        })

        headerFrame.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            addView(TextView(this@GodMatiereActivity).apply {
                text = "TEMPLE DE $divinite"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setTextColor(couleurHex)
                gravity = Gravity.CENTER
                try {
                    typeface = ResourcesCompat.getFont(this@GodMatiereActivity, R.font.cinzel)
                } catch (_: Exception) {
                    typeface = Typeface.DEFAULT_BOLD
                }
                setShadowLayer(8f, 0f, 2f, Color.parseColor("#AA000000"))
            })

            addView(TextView(this@GodMatiereActivity).apply {
                text = matiere
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(Color.parseColor("#F3E6B3"))
                gravity = Gravity.CENTER
                try {
                    typeface = ResourcesCompat.getFont(this@GodMatiereActivity, R.font.exo2)
                } catch (_: Exception) {
                    typeface = Typeface.DEFAULT
                }
            })
        })

        root.addView(headerFrame)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isFillViewport = true
            isClickable = true
            isFocusable = true
        }

        val conteneurCours = LinearLayout(this).apply {
            id = View.generateViewId()
            tag = "CONTENEUR_COURS"
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(conteneurCours)
        root.addView(scroll)

        val btnRetour = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)
            ).apply {
                setMargins(dp(16), dp(8), dp(16), dp(12))
            }
            isClickable = true
            isFocusable = true
        }
        // Fond texturé bg_temple_button
        btnRetour.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            try { setImageResource(R.drawable.bg_temple_button) }
            catch (_: Exception) { setBackgroundColor(Color.parseColor("#1A1A2E")) }
            scaleType = ImageView.ScaleType.FIT_XY
        })
        // Surcouche bg_textelayout pour profondeur
        btnRetour.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            try { setImageResource(R.drawable.bg_textelayout) }
            catch (_: Exception) {}
            scaleType = ImageView.ScaleType.FIT_XY
            alpha = 0.35f
        })
        // Label Cinzel or centré
        btnRetour.addView(TextView(this).apply {
            text = "← QUITTER LE TEMPLE"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#FFD700"))
            gravity = Gravity.CENTER
            typeface = try { resources.getFont(R.font.cinzel) }
            catch (_: Exception) { Typeface.DEFAULT_BOLD }
            letterSpacing = 0.06f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                try { jouerSfx(R.raw.sfx_dialogue_blip) } catch (_: Exception) {}
                finish()
            }
        })
        root.addView(btnRetour)

        frameRoot.addView(root)
        return frameRoot
    }

    /**
     * Injecte la couche d'ambiance dans la FrameLayout racine.
     */
    private fun installerAmbianceOlympienne(rootView: View) {
        val rootGroup = rootView as? ViewGroup ?: return

        olympianParticlesView = OlympianParticlesView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isClickable = false
            isFocusable = false
        }

        // Ajout derrière tout le reste pour ne gêner ni image fixe, ni vidéo,
        // ni le contenu principal interactif du temple.
        rootGroup.addView(olympianParticlesView, 0)

        animatedBackgroundHelper = AnimatedBackgroundHelper(
            targetView = rootView,
            particlesView = olympianParticlesView
        )
    }

    /**
     * Initialise les ressources image/vidéo du temple courant.
     *
     * Important :
     * - rien n'est affiché sur les autres écrans
     * - tout est strictement local à GodMatiereActivity
     * - les noms suivent la convention revizeus_info_bg_01..10
     */
    private fun initialiserFondTempleDivin() {
        val staticImageRes = getTempleDivineStaticImageRes(matiere)
        val videoRes = getTempleDivineVideoRes(matiere)

        if (staticImageRes != 0) {
            try {
                templeBackgroundImageView?.setImageResource(staticImageRes)
            } catch (e: Exception) {
                Log.e("REVIZEUS", "Fond fixe temple introuvable : ${e.message}")
            }
        }

        if (videoRes == 0) {
            // Si la vidéo n'existe pas encore, on garde l'image fixe immédiatement.
            afficherImageFixeTemple()
            return
        }

        try {
            val mediaItem = MediaItem.fromUri(Uri.parse("android.resource://$packageName/$videoRes"))
            templeBackgroundExoPlayer?.release()
            templeBackgroundExoPlayer = ExoPlayer.Builder(this).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = false
                volume = 0f
                setMediaItem(mediaItem)
                prepare()
            }
            templeBackgroundPlayerView?.player = templeBackgroundExoPlayer
        } catch (e: Exception) {
            Log.e("REVIZEUS", "Vidéo temple introuvable : ${e.message}")
            afficherImageFixeTemple()
        }
    }

    /**
     * Lance l'intro vidéo une seule fois pour l'entrée courante dans le temple.
     */
    private fun lancerVideoIntroTempleSiDisponible() {
        if (hasPlayedTempleIntroVideo) return

        val videoRes = getTempleDivineVideoRes(matiere)
        if (videoRes == 0) {
            afficherImageFixeTemple()
            return
        }

        val playerView = templeBackgroundPlayerView ?: run {
            afficherImageFixeTemple()
            return
        }

        val exoPlayer = templeBackgroundExoPlayer ?: run {
            afficherImageFixeTemple()
            return
        }

        hasPlayedTempleIntroVideo = true
        isTempleVideoCompleted = false

        try {
            // Pendant la lecture de la vidéo, on masque l'image fixe.
            templeBackgroundImageView?.visibility = View.GONE
            playerView.visibility = View.VISIBLE

            // Important : on remet explicitement le contenu principal au premier plan.
            // Avec PlayerView/TextureView, la vidéo reste derrière et l'UI demeure cliquable.
            templeMainContentRoot?.bringToFront()

            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(
                MediaItem.fromUri(Uri.parse("android.resource://$packageName/$videoRes"))
            )
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true

            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        exoPlayer.removeListener(this)
                        isTempleVideoCompleted = true
                        afficherImageFixeTemple()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("REVIZEUS", "Erreur vidéo temple : ${error.message}")
                    exoPlayer.removeListener(this)
                    afficherImageFixeTemple()
                }
            })
        } catch (e: Exception) {
            Log.e("REVIZEUS", "Impossible de lancer la vidéo du temple : ${e.message}")
            afficherImageFixeTemple()
        }
    }

    /**
     * Affiche l'image fixe du temple et masque proprement la vidéo.
     */
    private fun afficherImageFixeTemple() {
        try {
            templeBackgroundExoPlayer?.pause()
        } catch (_: Exception) {
        }
        try {
            templeBackgroundVideoView?.stopPlayback()
        } catch (_: Exception) {
        }

        templeBackgroundPlayerView?.visibility = View.GONE
        templeBackgroundVideoView?.visibility = View.GONE
        templeBackgroundImageView?.visibility = View.VISIBLE

        // On remet le contenu principal au-dessus pour garantir que l'écran
        // reste parfaitement interactif après la fin de la vidéo.
        templeMainContentRoot?.bringToFront()
    }

    /**
     * Nettoyage défensif du lecteur vidéo local au temple.
     */
    private fun libererFondTempleVideo() {
        try {
            templeBackgroundPlayerView?.player = null
        } catch (_: Exception) {
        }
        try {
            templeBackgroundExoPlayer?.stop()
            templeBackgroundExoPlayer?.release()
        } catch (_: Exception) {
        }
        templeBackgroundExoPlayer = null

        try {
            templeBackgroundVideoView?.setOnPreparedListener(null)
            templeBackgroundVideoView?.setOnCompletionListener(null)
            templeBackgroundVideoView?.setOnErrorListener(null)
            templeBackgroundVideoView?.stopPlayback()
        } catch (_: Exception) {
        }
    }

    private fun chargerCours() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@GodMatiereActivity)

            val coursExacts = try {
                db.iAristoteDao().getCoursesBySubject(matiere)
            } catch (e: Exception) {
                Log.e("REVIZEUS", "Erreur getCoursesBySubject: ${e.message}")
                emptyList()
            }

            // CHANTIER 0 — On conserve la requête exacte existante, puis on la sécurise
            // avec la nouvelle requête DAO multi-alias.
            val coursParAlias = try {
                db.iAristoteDao().getCoursesBySubjects(subjectAliases)
            } catch (e: Exception) {
                Log.e("REVIZEUS", "Erreur getCoursesBySubjects: ${e.message}")
                emptyList()
            }

            // CHANTIER 0 — Fusion non destructive + dédoublonnage par id.
            val coursList = (coursExacts + coursParAlias)
                .distinctBy { it.id }
                .filter { course ->
                    matchesSubject(course.subject)
                }

            withContext(Dispatchers.Main) {
                val conteneur = findViewByTag("CONTENEUR_COURS") ?: return@withContext
                conteneur.removeAllViews()

                if (coursList.isEmpty()) {
                    conteneur.addView(TextView(this@GodMatiereActivity).apply {
                        text = "Aucun savoir enregistré ici."
                        setTextColor(Color.GRAY)
                        gravity = Gravity.CENTER
                        setPadding(0, 50, 0, 0)
                    })
                } else {
                    coursList.forEach { course ->
                        val btn = Button(this@GodMatiereActivity).apply {
                            text = course.displayTitle()
                            setTextColor(Color.BLACK)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 0, 0, 24) }

                            val drawable = GradientDrawable().apply {
                                setColor(couleurHex)
                                cornerRadius = 16f
                            }
                            background = drawable

                            setOnClickListener {
                                try { jouerSfx(R.raw.sfx_dialogue_blip) } catch (_: Exception) {}
                                afficherContenuCours(course)
                            }

                            setOnLongClickListener {
                                ouvrirMenuGestionSavoir(course)
                                true
                            }
                        }
                        conteneur.addView(btn)
                    }

                    val courseIdToOpen = pendingAutoOpenCourseId
                    if (!courseIdToOpen.isNullOrBlank()) {
                        val targetCourse = coursList.firstOrNull { it.id == courseIdToOpen }
                        if (targetCourse != null) {
                            pendingAutoOpenCourseId = null
                            conteneur.post {
                                try {
                                    afficherContenuCours(targetCourse)
                                } catch (e: Exception) {
                                    Log.e("REVIZEUS", "Ouverture automatique du savoir impossible : ${e.message}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    /**
     * Nouveau menu d'appui long premium pour un savoir enregistré.
     * Permet :
     * - déplacer vers un autre temple
     * - déplacer dans un sous-dossier du temple
     * - modifier le résumé et le titre
     * - lancer un quiz avec ou sans timer
     * - supprimer le savoir
     */
    private fun ouvrirMenuGestionSavoir(course: CourseEntry) {
        val options = arrayOf(
            "🏛 Déplacer vers un autre temple",
            "📂 Déplacer dans un sous-dossier",
            "✏ Modifier le résumé",
            "🪶 Créer un poème divin",
            "🎵 Créer une musique divine",
            "⚡ Lancer un quiz avec timer",
            "🧘 Lancer un quiz sans timer",
            "🗑 Supprimer"
        )

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(course.displayTitle())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> ouvrirChoixTemplePourDeplacement(course)
                    1 -> ouvrirChoixSousDossierPourDeplacement(course)
                    2 -> ouvrirEditionManuelleSavoir(course)
                    3 -> lancerLyreApollon(course)
                    4 -> lancerMusiqueDirecteDepuisSavoir(course)
                    5 -> lancerQuizDepuisSavoir(course, true)
                    6 -> lancerQuizDepuisSavoir(course, false)
                    7 -> confirmerSuppressionCours(course)
                }
            }
            .setNegativeButton("Fermer", null)
            .show()
    }

    private fun ouvrirChoixTemplePourDeplacement(course: CourseEntry) {
        val gods = PantheonConfig.GODS
        val labels = gods.map { "${it.divinite} — ${it.matiere}" }.toTypedArray()

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Choisir le nouveau temple")
            .setItems(labels) { _, which ->
                val selected = gods[which]
                val input = android.widget.EditText(this).apply {
                    hint = "Sous-dossier optionnel"
                    setText(course.folderName)
                }

                AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Sous-dossier dans ${selected.matiere}")
                    .setView(input)
                    .setNegativeButton("Aucun") { _, _ ->
                        deplacerSavoir(course, selected.matiere, "")
                    }
                    .setPositiveButton("Valider") { _, _ ->
                        deplacerSavoir(course, selected.matiere, input.text?.toString()?.trim().orEmpty())
                    }
                    .show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun ouvrirChoixSousDossierPourDeplacement(course: CourseEntry) {
        val input = android.widget.EditText(this).apply {
            hint = "Nom du sous-dossier"
            setText(course.folderName)
        }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Choisir le sous-dossier")
            .setView(input)
            .setNegativeButton("Racine du temple") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@GodMatiereActivity)
                        .iAristoteDao()
                        .updateCourseFolder(course.id, "")
                    withContext(Dispatchers.Main) { chargerCours() }
                }
            }
            .setPositiveButton("Valider") { _, _ ->
                val newFolder = input.text?.toString()?.trim().orEmpty()
                lifecycleScope.launch(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@GodMatiereActivity)
                        .iAristoteDao()
                        .updateCourseFolder(course.id, newFolder)
                    withContext(Dispatchers.Main) { chargerCours() }
                }
            }
            .show()
    }

    private fun deplacerSavoir(course: CourseEntry, newSubject: String, newFolder: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                AppDatabase.getDatabase(this@GodMatiereActivity)
                    .iAristoteDao()
                    .updateCourseSubjectAndFolder(course.id, newSubject, newFolder)

                withContext(Dispatchers.Main) {
                    chargerCours()
                    if (newSubject != matiere) {
                        // BLOC B : Conversion Toast → Dialogue RPG
                        DialogRPGManager.showInfo(
                            activity = this@GodMatiereActivity,
                            godId = "demeter",
                            message = "Le savoir a quitté ce temple pour rejoindre $newSubject. Déméter veille sur sa transition."
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // BLOC B : Conversion Toast → Dialogue RPG
                    DialogRPGManager.showAlert(
                        activity = this@GodMatiereActivity,
                        godId = "athena",
                        message = "Le déplacement du savoir a échoué. Athéna te demande de réessayer."
                    )
                }
            }
        }
    }

    private fun ouvrirEditionManuelleSavoir(course: CourseEntry) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val titleInput = android.widget.EditText(this).apply {
            hint = "Titre du savoir"
            setText(course.displayTitle())
        }
        root.addView(titleInput)

        val folderInput = android.widget.EditText(this).apply {
            hint = "Sous-dossier"
            setText(course.folderName)
        }
        root.addView(folderInput)

        val contentInput = android.widget.EditText(this).apply {
            hint = "Résumé du savoir"
            setText(course.extractedText)
            minLines = 8
            gravity = Gravity.TOP or Gravity.START
        }
        root.addView(contentInput)

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Modifier le savoir")
            .setView(root)
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Enregistrer") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        AppDatabase.getDatabase(this@GodMatiereActivity)
                            .iAristoteDao()
                            .updateCourseContentAndTitle(
                                courseId = course.id,
                                newExtractedText = contentInput.text?.toString()?.trim().orEmpty(),
                                newCustomTitle = titleInput.text?.toString()?.trim().orEmpty()
                            )
                        AppDatabase.getDatabase(this@GodMatiereActivity)
                            .iAristoteDao()
                            .updateCourseFolder(course.id, folderInput.text?.toString()?.trim().orEmpty())

                        withContext(Dispatchers.Main) {
                            chargerCours()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            // BLOC B : Conversion Toast → Dialogue RPG
                            DialogRPGManager.showAlert(
                                activity = this@GodMatiereActivity,
                                godId = "athena",
                                message = "L'édition du savoir a échoué. Athéna te demande de réessayer."
                            )
                        }
                    }
                }
            }
            .show()
    }

    private fun lancerQuizDepuisSavoir(course: CourseEntry, isTimedMode: Boolean) {
        val god = PantheonConfig.findByMatiere(course.subject)
        val nomDieu = god?.divinite ?: divinite

        launchQuizJob?.cancel()
        launchQuizJob = lifecycleScope.launch {
            showGlobalDivineLoadingDialog(godName = nomDieu)

            try {
                val prefs = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
                val age = prefs.getInt("USER_AGE", 15)
                val classe = prefs.getString("USER_CLASS", "3ème") ?: "3ème"
                val mood = prefs.getString("CURRENT_MOOD", "Neutre") ?: "Neutre"

                val questions = withContext(Dispatchers.IO) {
                    com.revizeus.app.core.NormalTrainingBuilder.buildNormalTraining(
                        context = this@GodMatiereActivity,
                        course = course,
                        matiere = course.subject,
                        divinite = nomDieu,
                        ethos = god?.ethos ?: "Sagesse",
                        userAge = age,
                        userClass = classe,
                        userMood = mood
                    )
                }

                if (questions.isEmpty()) {
                    hideGlobalDivineLoadingDialog()
                    // BLOC B : Conversion AlertDialog → Dialogue RPG
                    val god = PantheonConfig.findByMatiere(course.subject)
                    DialogRPGManager.showAlert(
                        activity = this@GodMatiereActivity,
                        godId = god?.divinite?.lowercase() ?: "zeus",
                        message = "Impossible de forger un quiz à partir de ce savoir pour le moment. ${god?.divinite ?: "Le dieu"} se repose."
                    )
                    return@launch
                }

                prefs.edit()
                    .putString("TRAINING_COURSE_TEXT", course.extractedText)
                    .putString("TRAINING_MODE", "SINGLE_COURSE")
                    .putString("TRAINING_SELECTED_MATIERE", course.subject)
                    .putString("TRAINING_SELECTED_DIVINITE", nomDieu)
                    .putString("TRAINING_SELECTED_COURSE_ID", course.id)
                    .putBoolean("TRAINING_IS_TIMED_MODE", isTimedMode)
                    .putString("TRAINING_EXAM_FORMAT", "CLASSIC")
                    .apply()

                QuizActivity.pendingQuestions = ArrayList(questions)
                QuizActivity.currentMatiere = course.subject

                if (!isActive || isFinishing || isDestroyed) return@launch
                hideGlobalDivineLoadingDialog()
                startActivity(Intent(this@GodMatiereActivity, TrainingQuizActivity::class.java))
            } catch (e: CancellationException) {
                Log.d("REVIZEUS_GODM", "lancerQuizDepuisSavoir annulé proprement")
                throw e
            } catch (e: Exception) {
                hideGlobalDivineLoadingDialog()
                // BLOC B : Conversion AlertDialog → Dialogue RPG
                val god = PantheonConfig.findByMatiere(course.subject)
                DialogRPGManager.showAlert(
                    activity = this@GodMatiereActivity,
                    godId = god?.divinite?.lowercase() ?: "hephaestus",
                    message = "La forge du quiz a été interrompue. ${god?.divinite ?: "Le dieu"} a besoin de repos."
                )
            } finally {
                launchQuizJob = null
            }
        }
    }


    private fun confirmerSuppressionCours(course: CourseEntry) {
        // BLOC B : Conversion AlertDialog → Dialogue RPG
        val god = PantheonConfig.findByMatiere(course.subject)
        DialogRPGManager.showConfirmation(
            activity = this,
            godId = god?.divinite?.lowercase() ?: "demeter",
            message = "${course.displayTitle()} sera retiré du temple de ${god?.divinite ?: divinite}. Cette action est irréversible.",
            onConfirm = {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        AppDatabase.getDatabase(this@GodMatiereActivity)
                            .iAristoteDao()
                            .deleteCourseById(course.id)

                        withContext(Dispatchers.Main) {
                            chargerCours()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            // BLOC B : Conversion AlertDialog erreur → Dialogue RPG
                            DialogRPGManager.showAlert(
                                activity = this@GodMatiereActivity,
                                godId = god?.divinite?.lowercase() ?: "demeter",
                                message = "Impossible de supprimer ce savoir. ${god?.divinite ?: "Le temple"} refuse de laisser partir cette connaissance."
                            )
                        }
                    }
                }
            },
            onCancel = {}
        )
    }

    /**
     * Affiche le contenu brut du cours dans un dialogue custom plus propre.
     *
     * RECTIFICATION UX :
     * - bouton speaker centré juste sous le résumé
     * - bouton Lyre d'Apollon mieux proportionné
     * - bouton Fermer custom en bg_temple_button, hauteur max 50dp
     * - fermeture immédiate de la voix si on ferme / retour
     */
    private fun afficherContenuCours(course: CourseEntry) {
        val containerGlobal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            try { setBackgroundResource(R.drawable.bg_rpg_dialog) } catch (_: Exception) {}
        }

        val titre = TextView(this).apply {
            text = "$divinite — ${course.displayTitle()}"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            gravity = Gravity.CENTER
            typeface = try { resources.getFont(R.font.cinzel) } catch (_: Exception) { Typeface.DEFAULT_BOLD }
            setPadding(dp(8), dp(4), dp(8), dp(10))
        }
        containerGlobal.addView(titre)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isFillViewport = true
        }

        val contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        renderCourseSummaryContent(contentContainer, course.extractedText)
        scroll.addView(contentContainer)
        containerGlobal.addView(scroll)

        val speakerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10); bottomMargin = dp(8) }
        }

        val btnSpeaker = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            background = null
            contentDescription = "Écouter le résumé"
            try { setImageResource(R.drawable.ic_speaker_tts) }
            catch (_: Exception) { setImageResource(android.R.drawable.ic_btn_speak_now) }
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(6), dp(6), dp(6), dp(6))
            alpha = 0.92f
            setOnClickListener {
                tts.speak(course.extractedText)
            }
        }
        speakerRow.addView(btnSpeaker)
        containerGlobal.addView(speakerRow)

        val btnLyreFrame = FrameLayout(this).apply {
            id = View.generateViewId()
            tag = "btnLyreApollo"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            ).apply { setMargins(dp(18), 0, dp(18), dp(10)) }
            isClickable = true
            isFocusable = true
        }

        btnLyreFrame.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            try { setImageResource(R.drawable.bg_temple_button) }
            catch (_: Exception) { setBackgroundColor(Color.parseColor("#1A1200")) }
            scaleType = ImageView.ScaleType.FIT_XY
        })

        btnLyreFrame.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            try { setImageResource(R.drawable.bg_textelayout) }
            catch (_: Exception) {}
            scaleType = ImageView.ScaleType.FIT_XY
            alpha = 0.30f
        })

        btnLyreFrame.addView(TextView(this).apply {
            text = "🎵  INVOQUER LA LYRE D'APOLLON  🎵"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#1A0A00"))
            gravity = Gravity.CENTER
            typeface = try { resources.getFont(R.font.cinzel) }
            catch (_: Exception) { Typeface.DEFAULT_BOLD }
            letterSpacing = 0.04f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        })
        containerGlobal.addView(btnLyreFrame)

        val btnCloseFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            ).apply { setMargins(dp(18), 0, dp(18), dp(6)) }
            isClickable = true
            isFocusable = true
        }

        btnCloseFrame.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            try { setImageResource(R.drawable.bg_temple_button) }
            catch (_: Exception) { setBackgroundColor(Color.parseColor("#1A1A2E")) }
            scaleType = ImageView.ScaleType.FIT_XY
        })

        btnCloseFrame.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            try { setImageResource(R.drawable.bg_textelayout) }
            catch (_: Exception) {}
            scaleType = ImageView.ScaleType.FIT_XY
            alpha = 0.32f
        })

        btnCloseFrame.addView(TextView(this).apply {
            text = "FERMER"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.parseColor("#FFD700"))
            gravity = Gravity.CENTER
            typeface = try { resources.getFont(R.font.cinzel) }
            catch (_: Exception) { Typeface.DEFAULT_BOLD }
            letterSpacing = 0.05f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        })
        containerGlobal.addView(btnCloseFrame)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(containerGlobal)
            .setCancelable(true)
            .create()

        btnCloseFrame.setOnClickListener {
            tts.stop()
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            tts.stop()
        }

        btnLyreFrame.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_lyre_strum)
            } catch (_: Exception) {
                try { jouerSfx(R.raw.sfx_dialogue_blip) } catch (_: Exception) {}
            }
            tts.stop()
            dialog.dismiss()
            lancerLyreApollon(course)
        }

        dialog.show()
        try { dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rpg_dialog) } catch (_: Exception) {}
    }

    /**
     * Alignement strict avec ResultActivity :
     * - parsing des formats Oracle structurés + variantes markdown
     * - fallback identique en bloc TEXT unique
     * - niveaux TITLE/LEVEL/CHAPTER/SUBTITLE/TEXT avec mêmes styles et espacements
     */
    private fun parseCourseSummaryForDisplay(raw: String): ParsedOracleSummary {
        return try {
            val cleanedRaw = raw
                .replace("\r", "")
                .replace(Regex("\\u0000"), "")
                .trim()

            val lines = cleanedRaw.lines().map { it.trim() }
            val blocks = mutableListOf<SummaryDisplayBlock>()
            var title = ""
            var level = ""

            lines.forEach { line ->
                if (line.isBlank()) return@forEach

                when {
                    line.startsWith("TITLE:", ignoreCase = true) -> {
                        title = line.substringAfter(":").trim()
                    }

                    line.startsWith("LEVEL:", ignoreCase = true) -> {
                        level = line.substringAfter(":").trim()
                    }

                    line.startsWith("CHAPTER:", ignoreCase = true) -> {
                        val value = line.substringAfter(":").trim()
                        if (value.isNotBlank()) {
                            blocks.add(SummaryDisplayBlock(SummaryBlockType.CHAPTER, value))
                        }
                    }

                    line.startsWith("SUBTITLE:", ignoreCase = true) -> {
                        val value = line.substringAfter(":").trim()
                        if (value.isNotBlank()) {
                            blocks.add(SummaryDisplayBlock(SummaryBlockType.SUBTITLE, value))
                        }
                    }

                    line.startsWith("TEXT:", ignoreCase = true) -> {
                        val value = line.substringAfter(":").trim()
                        if (value.isNotBlank()) {
                            blocks.add(SummaryDisplayBlock(SummaryBlockType.TEXT, value))
                        }
                    }

                    line.startsWith("##") || line.startsWith("#") -> {
                        val value = line.replace(Regex("^#+\\s*"), "").trim()
                        if (value.isNotBlank()) {
                            blocks.add(SummaryDisplayBlock(SummaryBlockType.CHAPTER, value))
                        }
                    }

                    line.startsWith("**") && line.endsWith("**") -> {
                        val value = line.removePrefix("**").removeSuffix("**").trim()
                        if (value.isNotBlank()) {
                            blocks.add(SummaryDisplayBlock(SummaryBlockType.SUBTITLE, value))
                        }
                    }

                    else -> {
                        val value = line
                            .replace(Regex("^[-*]\\s*"), "")
                            .trim()
                        if (value.isNotBlank()) {
                            blocks.add(SummaryDisplayBlock(SummaryBlockType.TEXT, value))
                        }
                    }
                }
            }

            val hasStructuredFormat = lines.any { line ->
                line.startsWith("TITLE:", ignoreCase = true) ||
                    line.startsWith("LEVEL:", ignoreCase = true) ||
                    line.startsWith("CHAPTER:", ignoreCase = true) ||
                    line.startsWith("SUBTITLE:", ignoreCase = true) ||
                    line.startsWith("TEXT:", ignoreCase = true)
            }

            val cleanedFallbackText = cleanedRaw
                .replace(Regex("^---START_RESUME---\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*---END_RESUME---$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s+"), " ")
                .trim()

            val finalBlocks = if (blocks.isNotEmpty()) {
                blocks
            } else {
                listOf(
                    SummaryDisplayBlock(
                        type = SummaryBlockType.TEXT,
                        content = cleanedFallbackText.ifBlank { "Information non lisible dans le document." }
                    )
                )
            }

            val normalizedBlocks = if (!hasStructuredFormat && finalBlocks.isNotEmpty()) {
                listOf(
                    SummaryDisplayBlock(
                        type = SummaryBlockType.TEXT,
                        content = finalBlocks.joinToString("\n") { it.content }.trim()
                    )
                )
            } else {
                finalBlocks
            }

            val fallbackTitle = cleanedFallbackText
                .lines()
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() && it.length in 5..80 }
                ?: "Notions principales"

            ParsedOracleSummary(
                title = title.ifBlank { fallbackTitle },
                level = level
                    .replace(Regex("^[-*]\\s*"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim(),
                blocks = normalizedBlocks
            )
        } catch (_: Exception) {
            val clean = raw.replace(Regex("\\s+"), " ").trim()
            ParsedOracleSummary(
                title = "Notions principales",
                level = "",
                blocks = listOf(
                    SummaryDisplayBlock(
                        type = SummaryBlockType.TEXT,
                        content = clean.ifBlank { "Information non lisible dans le document." }
                    )
                )
            )
        }
    }

    private fun renderCourseSummaryContent(container: LinearLayout, raw: String) {
        container.removeAllViews()
        val parsed = parseCourseSummaryForDisplay(raw)

        if (parsed.blocks.isEmpty()) {
            container.addView(TextView(this).apply {
                text = raw.trim().ifBlank { "Information non lisible dans le document." }
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                setLineSpacing(0f, 1.22f)
                gravity = Gravity.START
                typeface = try { resources.getFont(R.font.exo2) } catch (_: Exception) { Typeface.DEFAULT }
            })
            return
        }

        val titleText = TextView(this).apply {
            text = parsed.title.ifBlank { "Notions principales" }
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
            gravity = Gravity.CENTER
            typeface = try {
                resources.getFont(R.font.cinzel_bold)
            } catch (_: Exception) {
                Typeface.DEFAULT_BOLD
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }
        container.addView(titleText)

        if (parsed.level.isNotBlank()) {
            val levelText = TextView(this).apply {
                text = parsed.level
                setTextColor(Color.parseColor("#FFF3B0"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
                typeface = try { resources.getFont(R.font.exo2) } catch (_: Exception) { Typeface.DEFAULT }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            }
            container.addView(levelText)
        }

        parsed.blocks.forEach { block ->
            val textView = TextView(this).apply {
                text = block.content
                setLineSpacing(0f, 1.22f)
                typeface = when (block.type) {
                    SummaryBlockType.TEXT -> try {
                        resources.getFont(R.font.exo2)
                    } catch (_: Exception) {
                        Typeface.DEFAULT
                    }

                    else -> Typeface.DEFAULT_BOLD
                }
            }

            when (block.type) {
                SummaryBlockType.CHAPTER -> {
                    textView.setTextColor(Color.parseColor("#FFD700"))
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    textView.gravity = Gravity.START
                    textView.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(14) }
                }

                SummaryBlockType.SUBTITLE -> {
                    textView.setTextColor(Color.parseColor("#FFF3B0"))
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    textView.gravity = Gravity.START
                    textView.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(10) }
                }

                SummaryBlockType.TEXT -> {
                    textView.setTextColor(Color.WHITE)
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                    textView.gravity = Gravity.START
                    textView.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(6) }
                }
            }

            container.addView(textView)
        }
    }

    private fun lancerLyreApollon(course: CourseEntry) {
        if (isLyriaGenerationRunning) {
            DialogRPGManager.showInfo(
                activity = this@GodMatiereActivity,
                godId = divinite.lowercase(),
                message = "Les Muses sont déjà en train d'accorder une création divine. Patiente un instant."
            )
            return
        }

        lifecycleScope.launch {
            Log.d("REVIZEUS_LYRE", "═══ DÉBUT lancerLyreApollon pour: ${course.displayTitle()}")
            isLyriaGenerationRunning = true
            showDivineMusicLoadingDialog(godName = divinite)
            try {
                val profile = withContext(Dispatchers.IO) {
                    try {
                        AppDatabase.getDatabase(this@GodMatiereActivity)
                            .iAristoteDao()
                            .getUserProfile()
                            ?: UserProfile(
                                id = 1,
                                age = 15,
                                classLevel = "Terminale",
                                mood = "Prêt",
                                xp = 0,
                                streak = 0,
                                cognitivePattern = "Général"
                            )
                    } catch (e: Exception) {
                        Log.e("REVIZEUS", "Lyre : profil non récupéré : ${e.message}")
                        UserProfile(
                            id = 1,
                            age = 15,
                            classLevel = "Terminale",
                            mood = "Prêt",
                            xp = 0,
                            streak = 0,
                            cognitivePattern = "Général"
                        )
                    }
                }

                val response = try {
                    GodLoreManager.buildEducationalHymn(
                        courseContent = course.extractedText,
                        profile = profile
                    )
                } catch (e: Exception) {
                    Log.e("REVIZEUS_LYRE", "❌ ERREUR buildEducationalHymn : ${e.message}")
                    GeminiManager.GodResponse(
                        text = """
La lyre d'Apollon murmure…
Le savoir porte ses propres vers.
Relisez ce parchemin, gravez l'essentiel,
Car c'est dans la répétition que naît la lumière.
""".trimIndent(),
                        mnemo = "Répéter, c'est graver.",
                        tone = "lyrique, inspirant et pédagogique",
                        godName = divinite,
                        matiere = course.subject.ifBlank { matiere },
                        suggestedAction = "Relis ce cours à voix haute pour ancrer les notions."
                    )
                }

                hideDivineMusicLoadingDialog()
                afficherHymnDialog(course, response)
            } catch (e: Exception) {
                Log.e("REVIZEUS_LYRE", "❌ ERREUR génération hymne : ${e.message}", e)
                hideDivineMusicLoadingDialog()
                DialogRPGManager.showAlert(
                    activity = this@GodMatiereActivity,
                    godId = divinite.lowercase(),
                    message = "Apollon a laissé tomber quelques vers dans les nuages. Réessaie dans un instant."
                )
            } finally {
                hideDivineMusicLoadingDialog()
                isLyriaGenerationRunning = false
            }
        }
    }


    /**
     * BLOC LYRE SUNO — normalise les paroles sans en changer le sens.
     * Le texte affiché, copié et exporté reste la source de vérité.
     */
    private fun getCanonicalLyricsForLyreSuno(response: GeminiManager.GodResponse): String {
        return try {
            LyriaManager.normalizeLyricsForSinging(response.text)
        } catch (_: Exception) {
            response.text.trim()
        }
    }


    /**
     * LYRE — fallback local si Gemini refuse de structurer la chanson.
     * On ne touche pas au poème ; ce fallback sert uniquement à l'écran music.
     */
    private fun buildFallbackStructuredSong(
        course: CourseEntry,
        hymnResponse: GeminiManager.GodResponse
    ): GeminiManager.GodResponse {
        val title = course.displayTitle()
        val compactTitle = if (title.length > 42) title.take(42).trimEnd() else title
        val refrainHook = hymnResponse.mnemo.ifBlank { "Le savoir se grave par la répétition." }
        // ✅ CORRECTION CRITIQUE : échappements Kotlin → Regex
// \s et \n doivent être échappés sinon "Unsupported escape sequence"
        val sourceLines = course.extractedText
            .split(Regex("(?<=[.!?])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(6)

        // ✅ AMÉLIORATION : espace entre phrases pour éviter texte collé
        val couplet1 = sourceLines.take(2).joinToString(" ")
        val couplet2 = sourceLines.drop(2).take(2).joinToString(" ")
        val couplet3 = sourceLines.drop(4).take(2).joinToString(" ")

        val songText = buildString {
            appendLine("[Couplet 1]")
            appendLine(if (couplet1.isNotBlank()) couplet1 else "${compactTitle} revient dans ma mémoire, j'en retiens la logique et le sens.")
            appendLine()
            appendLine("[Refrain]")
            appendLine(refrainHook)
            appendLine(refrainHook)
            appendLine()
            appendLine("[Couplet 2]")
            appendLine(if (couplet2.isNotBlank()) couplet2 else "Je relis, je répète, et chaque notion prend sa vraie place.")
            if (couplet3.isNotBlank()) {
                appendLine()
                appendLine("[Pont]")
                appendLine(couplet3)
            }
            appendLine()
            appendLine("[Refrain final]")
            appendLine(refrainHook)
            appendLine("${compactTitle} reste gravé, clair et solide dans mon esprit.")
        }.trim()

        return hymnResponse.copy(
            text = songText,
            tone = "pop épique scolaire, voix claire, refrain mémorable, tempo modéré, instrumentation lumineuse",
            suggestedAction = "Copie le style dans Suno, puis colle ces paroles de chanson structurées."
        )
    }

    /**
     * LYRE — construit une vraie chanson pédagogique pour l'écran music.
     * Le poème/hymne reste inchangé sur l'autre écran.
     */
    private suspend fun buildStructuredMusicResponse(
        course: CourseEntry,
        profile: UserProfile,
        hymnResponse: GeminiManager.GodResponse? = null
    ): GeminiManager.GodResponse {
        val adaptiveNote = buildString {
            append("Écran ciblé : music / atelier Suno. ")
            append("Ne renvoie pas un poème simple. ")
            append("Le texte doit être une vraie chanson pédagogique structurée avec couplets et refrain. ")
            hymnResponse?.let {
                append("Tu peux t'inspirer du même savoir que l'hymne déjà généré, mais tu ne dois pas recopier mot pour mot ce poème. ")
                append("Point d'ancrage mémoriel déjà existant : ${it.mnemo}. ")
            }
        }

        val generated = try {
            GeminiManager.generateEducationalSongLyrics(
                courseText = course.extractedText,
                age = profile.age,
                classe = profile.classLevel,
                matiere = course.subject.ifBlank { matiere },
                divinite = divinite,
                mood = profile.mood,
                courseTitle = course.displayTitle(),
                adaptiveContextNote = adaptiveNote
            )
        } catch (e: Exception) {
            Log.e("REVIZEUS_LYRE", "Erreur generateEducationalSongLyrics : ${e.message}", e)
            null
        }

        val chosen = generated ?: hymnResponse ?: GeminiManager.GodResponse(
            text = "[Couplet 1] Le savoir se rassemble et prend forme dans ma mémoire.[Refrain] Répéter, c'est graver.[Couplet 2] Chaque notion rejoint sa place et devient plus claire.[Refrain final] Répéter, c'est graver.",
            mnemo = "Répéter, c'est graver.",
            tone = "pop épique scolaire, voix claire, refrain mémorable, tempo modéré",
            godName = divinite,
            matiere = course.subject.ifBlank { matiere },
            suggestedAction = "Copie le style puis colle les paroles dans Suno."
        )

        val canonical = chosen.copy(text = getCanonicalLyricsForLyreSuno(chosen))
        return if (canonical.text.contains("[Refrain]", ignoreCase = true) || canonical.text.contains("[Couplet", ignoreCase = true)) {
            canonical
        } else {
            buildFallbackStructuredSong(course, canonical)
        }
    }

    private fun lancerMusiqueDirecteDepuisSavoir(course: CourseEntry) {
        if (isLyriaGenerationRunning) {
            DialogRPGManager.showInfo(
                activity = this@GodMatiereActivity,
                godId = divinite.lowercase(),
                message = "Les Muses sont déjà en train d'accorder une création divine. Patiente un instant."
            )
            return
        }

        lifecycleScope.launch {
            isLyriaGenerationRunning = true
            showDivineMusicLoadingDialog(godName = divinite)
            try {
                val profile = withContext(Dispatchers.IO) {
                    try {
                        AppDatabase.getDatabase(this@GodMatiereActivity)
                            .iAristoteDao()
                            .getUserProfile()
                            ?: UserProfile(
                                id = 1,
                                age = 15,
                                classLevel = "Terminale",
                                mood = "Prêt",
                                xp = 0,
                                streak = 0,
                                cognitivePattern = "Général"
                            )
                    } catch (_: Exception) {
                        UserProfile(
                            id = 1,
                            age = 15,
                            classLevel = "Terminale",
                            mood = "Prêt",
                            xp = 0,
                            streak = 0,
                            cognitivePattern = "Général"
                        )
                    }
                }

                val response = try {
                    GodLoreManager.buildEducationalHymn(
                        courseContent = course.extractedText,
                        profile = profile
                    )
                } catch (e: Exception) {
                    Log.e("REVIZEUS_LYRE", "❌ ERREUR buildEducationalHymn direct music : ${e.message}", e)
                    GeminiManager.GodResponse(
                        text = "La lyre d'Apollon murmure…\nLe savoir porte déjà ses propres vers sacrés.",
                        mnemo = "Répéter, c'est graver.",
                        tone = "lyrique, inspirant et pédagogique",
                        godName = divinite,
                        matiere = course.subject.ifBlank { matiere },
                        suggestedAction = "Relis ce cours à voix haute pour ancrer les notions."
                    )
                }

                val canonicalResponse = response.copy(
                    text = getCanonicalLyricsForLyreSuno(response)
                )

                hideDivineMusicLoadingDialog()
                afficherLyreSunoDialog(course, canonicalResponse)
            } catch (e: Exception) {
                Log.e("REVIZEUS_LYRE", "❌ ERREUR préparation Lyre Suno : ${e.message}", e)
                hideDivineMusicLoadingDialog()
                DialogRPGManager.showAlert(
                    activity = this@GodMatiereActivity,
                    godId = divinite.lowercase(),
                    message = "Apollon n'a pas réussi à préparer le chant de ce savoir. Réessaie quand l'Olympe sera plus stable."
                )
            } finally {
                hideDivineMusicLoadingDialog()
                isLyriaGenerationRunning = false
            }
        }
    }

    /**
     * BLOC LYRE SUNO — prépare le pack paroles + style à copier dans Suno.
     * Le nom de méthode est conservé pour ne pas casser les appels existants.
     */
    private fun lancerGenerationMusiqueDepuisHymne(
        course: CourseEntry,
        response: GeminiManager.GodResponse
    ) {
        if (isLyriaGenerationRunning) {
            DialogRPGManager.showInfo(
                activity = this@GodMatiereActivity,
                godId = divinite.lowercase(),
                message = "La forge musicale est déjà active. Laisse Apollon finir d'accorder sa lyre."
            )
            return
        }

        lifecycleScope.launch {
            isLyriaGenerationRunning = true
            showDivineMusicLoadingDialog(godName = divinite)
            try {
                releaseLyriaMusicPlayer()

                // Petite respiration premium pour conserver la sensation de rituel,
                // même si la destination finale est maintenant Suno manuel.
                delay(450L)

                val profile = withContext(Dispatchers.IO) {
                    try {
                        AppDatabase.getDatabase(this@GodMatiereActivity)
                            .iAristoteDao()
                            .getUserProfile()
                            ?: UserProfile(
                                id = 1,
                                age = 15,
                                classLevel = "Terminale",
                                mood = "Prêt",
                                xp = 0,
                                streak = 0,
                                cognitivePattern = "Général"
                            )
                    } catch (_: Exception) {
                        UserProfile(
                            id = 1,
                            age = 15,
                            classLevel = "Terminale",
                            mood = "Prêt",
                            xp = 0,
                            streak = 0,
                            cognitivePattern = "Général"
                        )
                    }
                }

                val musicResponse = buildStructuredMusicResponse(
                    course = course,
                    profile = profile,
                    hymnResponse = response
                )

                hideDivineMusicLoadingDialog()
                afficherLyreSunoDialog(course, musicResponse)
            } catch (e: Exception) {
                Log.e("REVIZEUS_LYRE", "❌ ERREUR ouverture Lyre Suno : ${e.message}", e)
                hideDivineMusicLoadingDialog()
                DialogRPGManager.showAlert(
                    activity = this@GodMatiereActivity,
                    godId = divinite.lowercase(),
                    message = "Apollon n'a pas réussi à préparer l'atelier Suno. Réessaie lorsque l'Olympe sera plus calme."
                )
            } finally {
                hideDivineMusicLoadingDialog()
                isLyriaGenerationRunning = false
            }
        }
    }

    private fun showGlobalDivineLoadingDialog(godName: String) {
        try {
            if (isFinishing || isDestroyed) return
            val tag = "loading_divine_global"
            val existing = supportFragmentManager.findFragmentByTag(tag)
            if (existing is LoadingDivineDialog) {
                divineGlobalLoadingDialog = existing
                return
            }
            val dialog = LoadingDivineDialog.newQuizInstance(godName)
            divineGlobalLoadingDialog = dialog
            dialog.show(supportFragmentManager, tag)
        } catch (e: Exception) {
            Log.e("REVIZEUS_GOD", "Impossible d'afficher le loader divin global : ${e.message}", e)
        }
    }

    private fun hideGlobalDivineLoadingDialog() {
        try {
            divineGlobalLoadingDialog?.dismissAllowingStateLoss()
        } catch (e: Exception) {
            Log.e("REVIZEUS_GOD", "Impossible de fermer le loader divin global : ${e.message}", e)
        } finally {
            divineGlobalLoadingDialog = null
        }
    }

    /**
     * BLOC LYRIA — ouvre le loader spécialisé musique divine sans toucher
     * aux autres chargements standards déjà présents dans le projet.
     */
    private fun showDivineMusicLoadingDialog(godName: String) {
        try {
            if (isFinishing || isDestroyed) return

            val tag = "loading_divine_music"
            val existing = supportFragmentManager.findFragmentByTag(tag)
            if (existing is LoadingDivineDialog) {
                divineMusicLoadingDialog = existing
                return
            }

            val dialog = LoadingDivineDialog.newMusicInstance(godName)
            divineMusicLoadingDialog = dialog
            dialog.show(supportFragmentManager, tag)
        } catch (e: Exception) {
            Log.e("REVIZEUS_LYRE", "Impossible d'afficher le loader musical divin : ${e.message}")
        }
    }

    /**
     * BLOC LYRIA — ferme le loader spécialisé musique divine.
     */
    private fun hideDivineMusicLoadingDialog() {
        try {
            divineMusicLoadingDialog?.dismissAllowingStateLoss()
        } catch (e: Exception) {
            Log.e("REVIZEUS_LYRE", "Impossible de fermer le loader musical divin : ${e.message}")
        } finally {
            divineMusicLoadingDialog = null
        }
    }

    /**
     * BLOC LYRE SUNO — construit le texte complet à copier dans Suno.
     */
    private fun buildSunoPackText(
        course: CourseEntry,
        response: GeminiManager.GodResponse
    ): String {
        return buildString {
            appendLine("APOLLON — PACK SUNO DU SAVOIR")
            appendLine()
            appendLine("Titre du savoir : ${course.displayTitle()}")
            appendLine("Matière : ${course.subject}")
            appendLine()
            appendLine("STYLE À COLLER DANS SUNO :")
            appendLine(response.tone)
            appendLine()
            appendLine("PAROLES À COLLER DANS SUNO :")
            appendLine(response.text)
            appendLine()
            appendLine("MNÉMO :")
            appendLine(response.mnemo)
            appendLine()
            appendLine("ACTION CONSEILLÉE :")
            appendLine(response.suggestedAction)
        }
    }

    /**
     * BLOC LYRE SUNO — texte pas à pas expliqué par Apollon.
     */
    private fun buildApolloSunoGuideText(
        course: CourseEntry,
        response: GeminiManager.GodResponse
    ): String {
        return buildString {
            append("Mortel, voici comment transformer ce chant du savoir en vraie chanson dans Suno.\n\n")
            append("Étape 1 : ouvre Suno sur ton téléphone ou ton navigateur.\n")
            append("Étape 2 : cherche le mode de création personnalisée, celui où tu peux écrire tes propres paroles.\n")
            append("Étape 3 : appuie d'abord sur le bouton qui copie le STYLE, puis colle ce style dans la zone de style musical de Suno.\n")
            append("Étape 4 : appuie ensuite sur le bouton qui copie les PAROLES, puis colle-les dans la zone des lyrics.\n")
            append("Étape 5 : si Suno propose plusieurs variantes, garde celle où les mots sont les plus compréhensibles et les plus proches de ton savoir.\n")
            append("Étape 6 : quand la chanson te plaît, tu peux la télécharger depuis Suno pour l'écouter hors du temple.\n\n")
            append("Pour ce savoir précis, je t'ai préparé un style déjà utilisable : ${response.tone}.\n\n")
            append("Et si tu veux aller plus vite, utilise le bouton PACK SUNO : il copie en une seule fois le style, les paroles, le mnémo et l'action liée à ${course.displayTitle()}.")
        }
    }

    /**
     * BLOC LYRE SUNO — intro courte affichée automatiquement dans le panneau d'aide.
     */
    private fun buildApolloSunoIntroText(): String {
        return "Apollon a préparé une vraie chanson du savoir. Lis les couplets et le refrain, copie le style, puis demande-moi l'aide pas à pas pour les coller proprement dans Suno."
    }

    /**
     * BLOC LYRE SUNO — copie sécurisée vers le presse-papiers.
     */
    private fun copyTextToClipboard(
        label: String,
        textToCopy: String,
        successMessage: String
    ) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard == null) {
                DialogRPGManager.showAlert(
                    activity = this@GodMatiereActivity,
                    godId = divinite.lowercase(),
                    message = "Hermès n'a pas trouvé le presse-papiers divin sur cet appareil."
                )
                return
            }

            clipboard.setPrimaryClip(ClipData.newPlainText(label, textToCopy))
            DialogRPGManager.showInfo(
                activity = this@GodMatiereActivity,
                godId = divinite.lowercase(),
                message = successMessage
            )
        } catch (e: Exception) {
            Log.e("REVIZEUS_LYRE", "Erreur copie presse-papiers : ${e.message}", e)
            DialogRPGManager.showAlert(
                activity = this@GodMatiereActivity,
                godId = divinite.lowercase(),
                message = "Hermès a fait tomber le parchemin en route. La copie a échoué."
            )
        }
    }

    /**
     * BLOC LYRE SUNO — dialogue premium pour préparer une chanson manuelle
     * dans Suno, sans casser le flow Lyre déjà existant.
     */
    private fun afficherLyreSunoDialog(
        course: CourseEntry,
        response: GeminiManager.GodResponse
    ) {
        val dialogView = try {
            LayoutInflater.from(this).inflate(R.layout.dialog_musical_lyrics, null, false)
        } catch (e: Exception) {
            Log.e("REVIZEUS_LYRE", "dialog_musical_lyrics.xml manquant : ${e.message}")
            DialogRPGManager.showAlert(
                activity = this@GodMatiereActivity,
                godId = divinite.lowercase(),
                message = "Le parchemin musical du temple est introuvable. Vérifie le layout dialog_musical_lyrics."
            )
            return
        }

        val imgGodLyrics = dialogView.findViewById<ImageView>(R.id.imgGodLyrics)
        val tvLyricsTitle = dialogView.findViewById<TextView>(R.id.tvLyricsTitle)
        val tvLyricsText = dialogView.findViewById<TextView>(R.id.tvLyricsText)
        val tvLyricsMnemo = dialogView.findViewById<TextView>(R.id.tvLyricsMnemo)
        val tvLyricsStyle = dialogView.findViewById<TextView>(R.id.tvLyricsStyle)
        val tvLyricsAction = dialogView.findViewById<TextView>(R.id.tvLyricsAction)
        val tvApolloGuide = dialogView.findViewById<TextView>(R.id.tvApolloGuide)
        val btnCopyLyrics = dialogView.findViewById<View>(R.id.btnCopyLyrics)
        val btnCopyStyle = dialogView.findViewById<View>(R.id.btnCopyStyle)
        val btnCopySunoPack = dialogView.findViewById<View>(R.id.btnCopySunoPack)
        val btnExportLyrics = dialogView.findViewById<View>(R.id.btnExportLyrics)
        val btnApolloHelp = dialogView.findViewById<View>(R.id.btnApolloHelp)
        val btnLyricsClose = dialogView.findViewById<View>(R.id.btnLyricsClose)

        val godIconResId = resolveGodMiniIcon("Apollon")
        if (godIconResId != 0) {
            try {
                imgGodLyrics.setImageResource(godIconResId)
            } catch (_: Exception) {
            }
        }

        tvLyricsTitle.text = "APOLLON — Chanson du Savoir pour Suno"
        tvLyricsStyle.text = response.tone
        tvLyricsMnemo.text = "✦ ${response.mnemo}"
        tvLyricsAction.text = "Action : ${response.suggestedAction}"
        tvLyricsText.text = ""
        tvApolloGuide.text = ""

        val dialog = AlertDialog.Builder(
            this,
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val sunoPack = buildSunoPackText(course, response)
        val apolloGuideText = buildApolloSunoGuideText(course, response)
        val apolloIntroText = buildApolloSunoIntroText()

        fun animateApolloGuide(textToShow: String) {
            hymnTypewriterJob?.cancel()
            tvApolloGuide.text = ""
            tvApolloGuide.post {
                if (!isFinishing && !isDestroyed && tvApolloGuide.isAttachedToWindow) {
                    hymnTypewriterJob = godAnim.typewriteSimple(
                        scope = lifecycleScope,
                        chibiView = imgGodLyrics,
                        textView = tvApolloGuide,
                        text = textToShow,
                        delayMs = 28L,
                        context = this@GodMatiereActivity
                    )
                }
            }
        }

        dialog.setOnShowListener {
            try {
                SoundManager.pauseMusic()
            } catch (_: Exception) {
            }

            hymnTypewriterJob?.cancel()
            tvLyricsText.post {
                if (!isFinishing && !isDestroyed && tvLyricsText.isAttachedToWindow) {
                    hymnTypewriterJob = godAnim.typewriteSimple(
                        scope = lifecycleScope,
                        chibiView = imgGodLyrics,
                        textView = tvLyricsText,
                        text = response.text,
                        delayMs = 26L,
                        context = this@GodMatiereActivity,
                        onComplete = {
                            animateApolloGuide(apolloIntroText)
                        }
                    )
                }
            }
        }

        dialog.setOnDismissListener {
            hymnTypewriterJob?.cancel()
            releaseLyriaMusicPlayer()
            try {
                SoundManager.rememberMusic(templeBgmResId)
                SoundManager.resumeRememberedMusicDelayed(this@GodMatiereActivity, 180L)
            } catch (_: Exception) {
            }
        }

        btnCopyLyrics.setOnClickListener {
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            copyTextToClipboard(
                label = "revizeus_lyrics",
                textToCopy = response.text,
                successMessage = "Apollon a placé les paroles sacrées dans ton presse-papiers. Tu peux maintenant les coller dans Suno."
            )
        }

        btnCopyStyle.setOnClickListener {
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            copyTextToClipboard(
                label = "revizeus_style",
                textToCopy = response.tone,
                successMessage = "Le style musical divin est copié. Colle-le dans le champ de style de Suno."
            )
        }

        btnCopySunoPack.setOnClickListener {
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            copyTextToClipboard(
                label = "revizeus_suno_pack",
                textToCopy = sunoPack,
                successMessage = "Apollon a copié le pack Suno complet. Tu as maintenant le style, les paroles et les repères du savoir."
            )
        }

        btnExportLyrics.setOnClickListener {
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            exportLyricsToPhone(course, response)
        }

        btnApolloHelp.setOnClickListener {
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            animateApolloGuide(apolloGuideText)
        }

        btnLyricsClose.setOnClickListener {
            hymnTypewriterJob?.cancel()
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            dialog.dismiss()
        }

        dialog.show()
        try {
            dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rpg_dialog)
        } catch (_: Exception) {
        }
    }

    private fun exportLyricsToPhone(
        course: CourseEntry,
        response: GeminiManager.GodResponse
    ) {
        try {
            val fileName = buildSafeFileName("lyrics_${course.displayTitle()}.txt")
            val content = buildString {
                appendLine("APOLLON — PACK SUNO DU SAVOIR")
                appendLine()
                appendLine("Titre : ${course.displayTitle()}")
                appendLine("Matière : ${course.subject}")
                appendLine("Style à coller dans Suno :")
                appendLine(response.tone)
                appendLine()
                appendLine("Mnémo : ${response.mnemo}")
                appendLine("Action : ${response.suggestedAction}")
                appendLine()
                appendLine("Paroles de chanson à coller dans Suno :")
                appendLine(response.text)
                appendLine()
                appendLine("Guide rapide :")
                appendLine("1. Ouvre Suno.")
                appendLine("2. Choisis une création personnalisée avec tes propres paroles.")
                appendLine("3. Colle d'abord le style musical.")
                appendLine("4. Colle ensuite les paroles.")
                appendLine("5. Lance la génération et garde la version la plus compréhensible.")
            }

            val savedPath = saveTextToPhone(fileName, content)
            if (savedPath != null) {
                DialogRPGManager.showInfo(
                    activity = this@GodMatiereActivity,
                    godId = divinite.lowercase(),
                    message = "Le chant a été exporté sur ton téléphone.\n\n$savedPath"
                )
            } else {
                DialogRPGManager.showAlert(
                    activity = this@GodMatiereActivity,
                    godId = divinite.lowercase(),
                    message = "Apollon n'a pas réussi à exporter les paroles."
                )
            }
        } catch (e: Exception) {
            Log.e("REVIZEUS_LYRE", "Erreur export lyrics : ${e.message}", e)
        }
    }

    private fun saveLyriaMusicToPhone(course: CourseEntry): String? {
        val sourcePath = currentLyriaAudioFilePath ?: return null
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return null

        return try {
            val fileName = buildSafeFileName("music_${course.displayTitle()}.mp3")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/ReviZeus")
                    put(MediaStore.Audio.Media.IS_MUSIC, 1)
                }
                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return null
                resolver.openOutputStream(uri)?.use { output ->
                    FileInputStream(sourceFile).use { input -> input.copyTo(output) }
                } ?: return null
                "Musique/ReviZeus/$fileName"
            } else {
                val targetDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                    ?.resolve("ReviZeus")
                    ?.apply { mkdirs() }
                    ?: return null
                val targetFile = targetDir.resolve(fileName)
                FileInputStream(sourceFile).use { input ->
                    targetFile.outputStream().use { output -> input.copyTo(output) }
                }
                targetFile.absolutePath
            }
        } catch (e: IOException) {
            Log.e("REVIZEUS_LYRE", "Erreur sauvegarde musique : ${e.message}", e)
            null
        }
    }

    private fun saveTextToPhone(fileName: String, content: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/ReviZeus")
                }
                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
                resolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(content)
                } ?: return null
                "Téléchargements/ReviZeus/$fileName"
            } else {
                val targetDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?.resolve("ReviZeus")
                    ?.apply { mkdirs() }
                    ?: return null
                val targetFile = targetDir.resolve(fileName)
                targetFile.writeText(content)
                targetFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e("REVIZEUS_LYRE", "Erreur sauvegarde texte : ${e.message}", e)
            null
        }
    }

    private fun buildSafeFileName(raw: String): String {
        return raw
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifBlank { "revizeus_file" }
    }

    /**
     * BLOC LYRIA — libération sûre du MediaPlayer dédié à la musique divine.
     */
    private fun releaseLyriaMusicPlayer() {
        try {
            if (lyriaMusicPlayer?.isPlaying == true) {
                lyriaMusicPlayer?.stop()
            }
        } catch (_: Exception) {
        }

        try {
            lyriaMusicPlayer?.release()
        } catch (_: Exception) {
        }

        lyriaMusicPlayer = null
    }

    /**
     * BLOC LYRIA — résout l'icône du dieu du temple pour le dialogue musical.
     */
    private fun resolveGodMiniIcon(godName: String): Int {
        return when (godName.uppercase()) {
            "ZEUS" -> resources.getIdentifier("ic_zeus_chibi", "drawable", packageName)
                .takeIf { it != 0 } ?: resources.getIdentifier("ic_zeus_mini", "drawable", packageName)
            "POSÉIDON", "POSEIDON" -> resources.getIdentifier("ic_poseidon_mini", "drawable", packageName)
            "ATHÉNA", "ATHENA" -> resources.getIdentifier("ic_athena_mini", "drawable", packageName)
            "ARÈS", "ARES" -> resources.getIdentifier("ic_ares_mini", "drawable", packageName)
            "APOLLON", "APOLLO" -> resources.getIdentifier("ic_apollon_mini", "drawable", packageName)
            "HÉPHAÏSTOS", "HEPHAISTOS", "HEPHAESTUS" -> resources.getIdentifier("ic_hephaistos_mini", "drawable", packageName)
            "DÉMÉTER", "DEMETER" -> resources.getIdentifier("ic_demeter_mini", "drawable", packageName)
            "HERMÈS", "HERMES" -> resources.getIdentifier("ic_hermes_mini", "drawable", packageName)
            "APHRODITE" -> resources.getIdentifier("ic_aphrodite_mini", "drawable", packageName)
            "PROMÉTHÉE", "PROMETHEE", "PROMETHEUS" -> resources.getIdentifier("ic_prometheus_mini", "drawable", packageName)
            else -> resources.getIdentifier("ic_apollon_mini", "drawable", packageName)
        }
    }

    /**
     * PHASE B — LYRE D'APOLLON
     * Affiche le dialogue RPG de l'hymne avec l'effet typewriter.
     *
     * Utilise dialog_course_hymn.xml comme layout custom.
     * Avatar : R.drawable.ic_apollo_lyre en priorité, fallback ic_apollon_mini.
     *
     * La structure du dialogue est intentionnellement sobre :
     * - un portrait d'Apollon
     * - le poème animé via typewriter
     * - la formule mnémotechnique
     * - l'action suggérée
     * - un bouton de confirmation
     *
     * ÉVOLUTION FUTURE :
     * - Ajouter un bouton "Partager" (copier le poème dans le presse-papier)
     * - Permettre de rejouer le son sfx_lyre_strum pendant la lecture
     */
    private fun afficherHymnDialog(
        course: CourseEntry,
        response: GeminiManager.GodResponse
    ) {
        Log.d("REVIZEUS_LYRE", "═══ DÉBUT afficherHymnDialog")
        Log.d("REVIZEUS_LYRE", "📝 Response.text: '${response.text}'")

        val dialogView = try {
            LayoutInflater.from(this).inflate(R.layout.dialog_course_hymn, null, false)
        } catch (e: Exception) {
            Log.e("REVIZEUS", "dialog_course_hymn.xml manquant : ${e.message}")
            afficherHymnDialogFallback(course, response)
            return
        }

        val imgApolloLyre = dialogView.findViewById<ImageView>(R.id.imgApolloLyre)
        val tvHymnTitle = dialogView.findViewById<TextView>(R.id.tvHymnTitle)
        val tvHymnText = dialogView.findViewById<TextView>(R.id.tvHymnText)
        val tvHymnMnemo = dialogView.findViewById<TextView>(R.id.tvHymnMnemo)
        val tvHymnAction = dialogView.findViewById<TextView>(R.id.tvHymnAction)
        val btnExportPoem = dialogView.findViewById<TextView>(R.id.btnExportPoem)
        val btnGenerateLyrics = dialogView.findViewById<TextView>(R.id.btnGenerateLyrics)
        val btnHymnClose = dialogView.findViewById<TextView>(R.id.btnHymnClose)

        val apolloResId = resources.getIdentifier("ic_apollo_lyre", "drawable", packageName)
        val fallbackResId = resources.getIdentifier("ic_apollon_mini", "drawable", packageName)
        when {
            apolloResId != 0 -> imgApolloLyre.setImageResource(apolloResId)
            fallbackResId != 0 -> imgApolloLyre.setImageResource(fallbackResId)
        }

        tvHymnTitle.text = "APOLLON — Hymne de ${course.displayTitle()}"
        tvHymnMnemo.text = "✦ ${response.mnemo}"
        tvHymnAction.text = "Action : ${response.suggestedAction}"

        val dialog = AlertDialog.Builder(
            this,
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )
            .setView(dialogView)
            .create()

        btnExportPoem.setOnClickListener {
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            exportLyricsToPhone(course, response)
        }

        btnGenerateLyrics.setOnClickListener {
            try {
                SoundManager.playSFX(this, R.raw.sfx_lyre_strum)
            } catch (_: Exception) {
                try { jouerSfx(R.raw.sfx_dialogue_blip) } catch (_: Exception) {}
            }
            dialog.dismiss()
            lancerGenerationMusiqueDepuisHymne(course, response)
        }

        btnHymnClose.setOnClickListener {
            hymnTypewriterJob?.cancel()
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            isLyreDialogVisible = false
            hymnTypewriterJob?.cancel()
            hymnMusicTransitionJob?.cancel()
            hymnMusicTransitionJob = null
            try {
                SoundManager.rememberMusic(templeBgmResId)
                SoundManager.playMusicDelayed(this@GodMatiereActivity, templeBgmResId, 100L)
            } catch (e: Exception) {
                Log.e("REVIZEUS", "Lyre BGM restore erreur : ${e.message}")
            }
        }

        isLyreDialogVisible = true
        dialog.show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rpg_dialog)

        hymnMusicTransitionJob?.cancel()
        hymnMusicTransitionJob = lifecycleScope.launch {
            delay(300L)
            if (!isFinishing && !isDestroyed && isLyreDialogVisible) {
                try {
                    SoundManager.rememberMusic(R.raw.bgm_apollo_lyre)
                    SoundManager.playMusic(this@GodMatiereActivity, R.raw.bgm_apollo_lyre)
                } catch (e: Exception) {
                    Log.e("REVIZEUS", "Lyre BGM play erreur : ${e.message}")
                }
            }
        }

        hymnTypewriterJob?.cancel()
        tvHymnText.post {
            if (!isFinishing && !isDestroyed && tvHymnText.isAttachedToWindow) {
                Log.d("REVIZEUS_LYRE", "🎨 Vue attachée: ${tvHymnText.isAttachedToWindow}, visible: ${tvHymnText.visibility == View.VISIBLE}")
                Log.d("REVIZEUS_LYRE", "🎨 Lancement typewriter - Texte: '${response.text.take(50)}...'")
                hymnTypewriterJob = godAnim.typewriteSimple(
                    scope = lifecycleScope,
                    chibiView = imgApolloLyre,
                    textView = tvHymnText,
                    text = response.text,
                    context = this
                )
                Log.d("REVIZEUS_LYRE", "✅ Typewriter job lancé avec succès")
            } else {
                Log.e("REVIZEUS_LYRE", "❌ TextView non prête - Attachée: ${tvHymnText.isAttachedToWindow}, Finishing: $isFinishing")
            }
        }
    }

    /**
     * PHASE B — LYRE D'APOLLON
     * Fallback programmatique si dialog_course_hymn.xml n'est pas encore disponible.
     *
     * Même structure visuelle que le layout XML, entièrement en code.
     * Ce fallback est temporaire : dès que le fichier XML est intégré au build,
     * c'est afficherHymnDialog() qui prend le relais via LayoutInflater.
     */
    /**
     * FALLBACK PREMIUM — Si l'audio Lyria ne peut pas être matérialisé,
     * on conserve un vrai rendu RPG premium centré sur les paroles divines.
     *
     * Cette méthode est volontairement séparée du flux de lecture audio afin de :
     * - préserver la sensation de récompense même sans audio exploitable
     * - conserver le style RéviZeus lettre par lettre
     * - éviter un simple message d'erreur sec
     */
    private fun afficherLyriaFallbackPremiumDialog(
        course: CourseEntry,
        response: GeminiManager.GodResponse,
        fallbackLabel: String,
        fallbackMessage: String
    ) {
        val dialogView = try {
            LayoutInflater.from(this).inflate(R.layout.dialog_musical_lyrics, null, false)
        } catch (e: Exception) {
            Log.e("REVIZEUS_LYRIA", "dialog_musical_lyrics introuvable : ${e.message}")
            DialogRPGManager.showInfo(
                activity = this@GodMatiereActivity,
                godId = divinite.lowercase(),
                message = fallbackMessage
            )
            return
        }

        val imgGodLyrics = dialogView.findViewById<ImageView>(R.id.imgGodLyrics)
        val tvLyricsTitle = dialogView.findViewById<TextView>(R.id.tvLyricsTitle)
        val tvLyricsText = dialogView.findViewById<TextView>(R.id.tvLyricsText)
        val tvLyricsMnemo = dialogView.findViewById<TextView>(R.id.tvLyricsMnemo)
        val tvLyricsStyle = dialogView.findViewById<TextView>(R.id.tvLyricsStyle)
        val tvLyricsAction = dialogView.findViewById<TextView>(R.id.tvLyricsAction)
        val btnExportLyrics = dialogView.findViewById<TextView>(R.id.btnExportLyrics)
        val btnLyricsClose = dialogView.findViewById<TextView>(R.id.btnLyricsClose)

        val godIconResId = when (divinite.uppercase()) {
            "ZEUS" -> R.drawable.ic_zeus_chibi
            "POSÉIDON", "POSEIDON" -> R.drawable.ic_poseidon_mini
            "ATHÉNA", "ATHENA" -> R.drawable.ic_athena_mini
            "ARÈS", "ARES" -> R.drawable.ic_ares_mini
            "APOLLON", "APOLLO" -> R.drawable.ic_apollon_mini
            "HÉPHAÏSTOS", "HEPHAISTOS", "HEPHAESTUS" -> R.drawable.ic_hephaistos_mini
            "DÉMÉTER", "DEMETER" -> R.drawable.ic_demeter_mini
            "HERMÈS", "HERMES" -> R.drawable.ic_hermes_mini
            "APHRODITE" -> R.drawable.ic_aphrodite_mini
            "PROMÉTHÉE", "PROMETHEE", "PROMETHEUS" -> R.drawable.ic_prometheus_mini
            else -> R.drawable.ic_apollon_mini
        }

        try {
            imgGodLyrics.setImageResource(godIconResId)
        } catch (_: Exception) {
        }

        tvLyricsTitle.text = "$divinite — Chant du savoir"
        tvLyricsMnemo.text = "✦ ${response.mnemo}"
        tvLyricsStyle.text = "${response.tone}\n\n$fallbackLabel"
        tvLyricsAction.text = "${response.suggestedAction}\n\n$fallbackMessage"
        tvLyricsText.text = ""

        val dialog = AlertDialog.Builder(
            this,
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )
            .setView(dialogView)
            .create()

        btnExportLyrics.setOnClickListener {
            try {
                jouerSfx(R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            DialogRPGManager.showInfo(
                activity = this@GodMatiereActivity,
                godId = divinite.lowercase(),
                message = "Le chant sacré peut déjà être contemplé. L'export audio viendra lorsque les Muses stabiliseront la scène céleste."
            )
        }

        btnLyricsClose.setOnClickListener {
            hymnTypewriterJob?.cancel()
            try {
                jouerSfx(R.raw.sfx_avatar_confirm)
            } catch (_: Exception) {
            }
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            isLyreDialogVisible = false
            hymnTypewriterJob?.cancel()
            hymnMusicTransitionJob?.cancel()
            hymnMusicTransitionJob = null
            try {
                SoundManager.rememberMusic(templeBgmResId)
                SoundManager.playMusicDelayed(this@GodMatiereActivity, templeBgmResId, 100L)
            } catch (e: Exception) {
                Log.e("REVIZEUS_LYRIA", "Erreur reprise BGM temple : ${e.message}")
            }
        }

        isLyreDialogVisible = true
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rpg_dialog)

        hymnMusicTransitionJob?.cancel()
        hymnMusicTransitionJob = lifecycleScope.launch {
            delay(300L)
            if (!isFinishing && !isDestroyed && isLyreDialogVisible) {
                try {
                    SoundManager.rememberMusic(R.raw.bgm_apollo_lyre)
                    SoundManager.playMusic(this@GodMatiereActivity, R.raw.bgm_apollo_lyre)
                } catch (e: Exception) {
                    Log.e("REVIZEUS_LYRIA", "Erreur lecture ambiance fallback : ${e.message}")
                }
            }
        }

        hymnTypewriterJob?.cancel()
        tvLyricsText.post {
            if (!isFinishing && !isDestroyed && tvLyricsText.isAttachedToWindow) {
                hymnTypewriterJob = godAnim.typewriteSimple(
                    scope = lifecycleScope,
                    chibiView = imgGodLyrics,
                    textView = tvLyricsText,
                    text = response.text,
                    context = this@GodMatiereActivity
                )
            }
        }
    }

    private fun afficherHymnDialogFallback(
        course: CourseEntry,
        response: GeminiManager.GodResponse
    ) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_rpg_dialog)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val portrait = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(80), dp(80))
            scaleType = ImageView.ScaleType.FIT_CENTER
            val apolloResId = resources.getIdentifier("ic_apollo_lyre", "drawable", packageName)
            val fallbackResId = resources.getIdentifier("ic_apollon_mini", "drawable", packageName)
            when {
                apolloResId != 0 -> setImageResource(apolloResId)
                fallbackResId != 0 -> setImageResource(fallbackResId)
            }
        }
        headerRow.addView(portrait)

        val titleView = TextView(this).apply {
            text = "APOLLON — ${course.displayTitle()}"
            setTextColor(Color.parseColor("#FFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dp(14) }
        }
        headerRow.addView(titleView)
        root.addView(headerRow)

        val hymnTextView = TextView(this).apply {
            text = ""
            setTextColor(Color.parseColor("#F5F5F5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(0, dp(16), 0, 0)
            setLineSpacing(dp(4).toFloat(), 1f)
        }
        root.addView(hymnTextView)

        val mnemoView = TextView(this).apply {
            text = "✦ ${response.mnemo}"
            setTextColor(Color.parseColor("#CCFFD700"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(mnemoView)

        val actionView = TextView(this).apply {
            text = "Action : ${response.suggestedAction}"
            setTextColor(Color.parseColor("#A5D6A7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(actionView)

        val btnClose = TextView(this).apply {
            text = "COMPRIS"
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_textelayout)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(18) }
            isClickable = true
            isFocusable = true
        }
        root.addView(btnClose)

        val dialog = AlertDialog.Builder(
            this,
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )
            .setView(root)
            .setCancelable(true)
            .create()

        btnClose.setOnClickListener {
            hymnTypewriterJob?.cancel()
            try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            isLyreDialogVisible = false
            hymnTypewriterJob?.cancel()
            hymnMusicTransitionJob?.cancel()
            hymnMusicTransitionJob = null
            try {
                SoundManager.rememberMusic(templeBgmResId)
                SoundManager.playMusicDelayed(this@GodMatiereActivity, templeBgmResId, 100L)
            } catch (e: Exception) {
                Log.e("REVIZEUS", "Lyre BGM restore (fallback) erreur : ${e.message}")
            }
        }

        isLyreDialogVisible = true
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rpg_dialog)

        hymnMusicTransitionJob?.cancel()
        hymnMusicTransitionJob = lifecycleScope.launch {
            delay(300L)
            if (!isFinishing && !isDestroyed && isLyreDialogVisible) {
                try {
                    SoundManager.rememberMusic(R.raw.bgm_apollo_lyre)
                    SoundManager.playMusic(this@GodMatiereActivity, R.raw.bgm_apollo_lyre)
                } catch (e: Exception) {
                    Log.e("REVIZEUS", "Lyre BGM play (fallback) erreur : ${e.message}")
                }
            }
        }

        hymnTypewriterJob?.cancel()
        
        // BLOC B - CORRECTION CRITIQUE (fallback):
        // Même correction que pour afficherHymnDialog.
        hymnTextView.post {
            if (!isFinishing && !isDestroyed && hymnTextView.isAttachedToWindow) {
                hymnTypewriterJob = godAnim.typewriteSimple(
                    scope = lifecycleScope,
                    chibiView = portrait,
                    textView = hymnTextView,
                    text = response.text,
                    context = this
                )
            }
        }
    }

    /**
     * Helper dimensions pour les dialogues runtime.
     * Identique au pattern utilisé dans DashboardActivity.
     */
    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun findViewByTag(tag: String): LinearLayout? {
        fun search(v: View): LinearLayout? {
            if (v.tag == tag && v is LinearLayout) return v
            if (v is ViewGroup) {
                for (i in 0 until v.childCount) {
                    val result = search(v.getChildAt(i))
                    if (result != null) return result
                }
            }
            return null
        }
        return search(window.decorView)
    }

    // CHANTIER 0 — Normalisation locale pour fiabiliser le temple.
    private fun normalizeSubjectKey(subject: String?): String {
        val cleaned = subject.orEmpty().trim()

        return when (cleaned.lowercase()) {
            "mathématiques", "mathematiques", "maths", "mathématiques / maths" -> "Mathématiques"
            "français", "francais" -> "Français"
            "svt" -> "SVT"
            "histoire" -> "Histoire"
            "art", "art/musique", "art / musique", "musique" -> "Art/Musique"
            "langues", "anglais", "english" -> "Langues"
            "géographie", "geographie" -> "Géographie"
            "physique-chimie", "physique / chimie", "physique", "chimie" -> "Physique-Chimie"
            "philo/ses", "philo / ses", "philosophie", "ses" -> "Philo/SES"
            "vie & projets", "vie et projets", "projets", "orientation" -> "Vie & Projets"
            else -> cleaned
        }
    }

    // CHANTIER 0 — Alias compatibles avec les matières réellement stockées.
    private fun getAliasesForSubject(subject: String): List<String> {
        return when (normalizeSubjectKey(subject)) {
            "Mathématiques" -> listOf("Mathématiques", "Mathematiques", "Maths", "Mathématiques / Maths")
            "Français" -> listOf("Français", "Francais")
            "SVT" -> listOf("SVT")
            "Histoire" -> listOf("Histoire")
            "Art/Musique" -> listOf("Art/Musique", "Art / Musique", "Art", "Musique")
            "Langues" -> listOf("Langues", "Anglais", "English")
            "Géographie" -> listOf("Géographie", "Geographie")
            "Physique-Chimie" -> listOf("Physique-Chimie", "Physique / Chimie", "Physique", "Chimie")
            "Philo/SES" -> listOf("Philo/SES", "Philo / SES", "Philosophie", "SES")
            "Vie & Projets" -> listOf("Vie & Projets", "Vie et Projets", "Projets", "Orientation")
            else -> listOf(subject.trim())
        }
    }

    // CHANTIER 0 — Vérification finale de sécurité sur chaque cours.
    private fun matchesSubject(courseSubject: String?): Boolean {
        val rawSubject = courseSubject.orEmpty().trim()
        val normalizedCourse = normalizeSubjectKey(rawSubject)
        val normalizedTemple = normalizeSubjectKey(matiere)
        val normalizedCanonicalTemple = normalizeSubjectKey(canonicalMatiere)

        if (normalizedCourse.equals(normalizedTemple, ignoreCase = true)) return true
        if (normalizedCourse.equals(normalizedCanonicalTemple, ignoreCase = true)) return true
        if (rawSubject.equals(matiere.trim(), ignoreCase = true)) return true
        if (rawSubject.equals(canonicalMatiere.trim(), ignoreCase = true)) return true

        return subjectAliases.any { alias ->
            rawSubject.equals(alias.trim(), ignoreCase = true) ||
                    normalizedCourse.equals(normalizeSubjectKey(alias), ignoreCase = true)
        }
    }
}

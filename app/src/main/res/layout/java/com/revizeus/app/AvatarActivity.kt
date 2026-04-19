package com.revizeus.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.revizeus.app.databinding.ActivityAvatarBinding
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.AvatarItem
import com.revizeus.app.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AvatarActivity — version corrigée v2.
 *
 * Correctif principal ajouté ici :
 * - quand createAccount échoue parce que l'email existe déjà,
 *   le message utilisateur est clarifié et on évite le ressenti
 *   "je n'ai jamais joué mais on me dit que mon mail existe déjà".
 * - le reste de l'architecture existante est conservé.
 *
 * CORRECTIFS v3 :
 * - si createAccount échoue parce que l'email existe déjà, on rattache
 *   immédiatement le joueur au compte existant avec le mot de passe saisi
 * - puis on ouvre directement HeroSelectActivity liée à ce compte, comme demandé
 * - le dialogue RPG d'accueil est déclenché via post { } pour éviter le texte vide
 */
class AvatarActivity : BaseActivity() {

    companion object {
        private const val TAG = "AVATAR_DBG"
        private const val AVATAR_ITEM_WIDTH_DP = 220

        /**
         * Même clé de flow temporaire utilisée par LoginActivity et HeroSelectActivity.
         * Elle permet à AvatarActivity de distinguer sans ambiguïté :
         * - onboarding d'un nouveau compte
         * - ajout d'un héros à un compte déjà existant
         */
        private const val PREF_HERO_CREATION_MODE = "HERO_CREATION_MODE"
        private const val HERO_CREATION_MODE_NEW_ACCOUNT = "new_account"
        private const val HERO_CREATION_MODE_EXISTING_ACCOUNT = "existing_account"

        private val ELEMENT_FOUDRE = 0xFFFFD700.toInt()
        private val ELEMENT_EAU = 0xFF4FC3F7.toInt()
        private val ELEMENT_TERRE = 0xFF8D6E63.toInt()
        private val ELEMENT_FEU = 0xFFFF6B35.toInt()
        private val ELEMENT_LUMIERE = 0xFFE3F2FD.toInt()
        private val ELEMENT_NATURE = 0xFF4CAF50.toInt()
        private val ELEMENT_GLACE = 0xFF80DEEA.toInt()
        private val ELEMENT_METAL = 0xFF78909C.toInt()
        private val ELEMENT_ARCANE = 0xFFE91E63.toInt()
        private val ELEMENT_CYBERNE = 0xFF00E5FF.toInt()
        private val ELEMENT_OR = 0xFFFFC107.toInt()
    }

    private lateinit var binding: ActivityAvatarBinding
    private var userGender: String = "Garçon"
    private val avatars = mutableListOf<AvatarItem>()
    private var currentSelectedAvatar: AvatarItem? = null

    private var typewriterJob: Job? = null
    private var backgroundPlayer: ExoPlayer? = null
    private var backgroundSwitchJob: Job? = null
    private var currentVideoResId: Int = -1
    private var ignoreFirstSnap = true

    /**
     * BLOC BLIP FANTÔME :
     * chaque nouvelle phrase invalide immédiatement la précédente.
     */
    private var typewriterSessionId: Long = 0L

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var snapHelper: LinearSnapHelper

    private val backgroundPlayerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                binding.pvAvatarBackground.alpha = 0f
                binding.pvAvatarBackground.visibility = View.VISIBLE
                binding.pvAvatarBackground.animate().alpha(1f).setDuration(220L).start()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAvatarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userGender = intent.getStringExtra("USER_GENDER") ?: "Garçon"

        val hasPendingOnboarding = OnboardingSession.isReady()
        val currentUser = FirebaseAuthManager.getCurrentUser()
        val activeUid = AccountRegistry.getActiveUid(this)

        if (!hasPendingOnboarding && (currentUser == null || activeUid.isBlank())) {
            Log.w(TAG, "Ni onboarding prêt, ni session Firebase active → retour LoginActivity")
            Toast.makeText(this, "Session expirée. Recommence l'inscription.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        try {
            SoundManager.playMusic(
                this,
                if (userGender.equals("Fille", ignoreCase = true)) R.raw.bgm_avatar_fille
                else R.raw.bgm_avatar_homme
            )
        } catch (e: Exception) {
            Log.w(TAG, "BGM : ${e.message}")
        }

        prepareData()
        setupBackgroundPlayer()
        setupCarousel()
        setupConfirmButton()
        binding.tvZeusSpeech.post {
            afficherTexteRPG("Choisis ton incarnation... Fais défiler les héros et scelle ton destin.")
        }
    }

    override fun onPause() {
        super.onPause()
        typewriterSessionId += 1L
        typewriterJob?.cancel()
        SoundManager.stopAllDialogueBlips()
    }

    override fun onDestroy() {
        backgroundSwitchJob?.cancel()
        typewriterJob?.cancel()
        SoundManager.stopAllDialogueBlips()
        backgroundPlayer?.removeListener(backgroundPlayerListener)
        backgroundPlayer?.release()
        backgroundPlayer = null
        super.onDestroy()
    }

    private fun prepareData() {
        avatars.clear()
        val isFemale = userGender.equals("Fille", ignoreCase = true)

        if (isFemale) {
            avatars += AvatarItem(1, "Voltage", "Fille", drawableId("avatar_heroine1"), drawableId("bg_avatar_heroine1"), rawId("bg_avatar_heroine1_animated"), "Une pile électrique en jupe plissée. Elle ne révise pas, elle électrise ses neurones pour un score maximum.", ELEMENT_FOUDRE)
            avatars += AvatarItem(2, "Abysse", "Fille", drawableId("avatar_heroine2"), drawableId("bg_avatar_heroine2"), rawId("bg_avatar_heroine2_animated"), "Elle voit ce qui est caché sous la surface des textes. Pour elle, plonger dans un livre est une seconde nature.", ELEMENT_EAU)
            avatars += AvatarItem(3, "Précisia", "Fille", drawableId("avatar_heroine3"), drawableId("bg_avatar_heroine3"), rawId("bg_avatar_heroine3_animated"), "Une archère de précision qui ne rate jamais sa cible. Une faute d'orthographe ? Elle la décoche en un clin d'œil.", ELEMENT_LUMIERE)
            avatars += AvatarItem(4, "Alchimia", "Fille", drawableId("avatar_heroine4"), drawableId("bg_avatar_heroine4"), rawId("bg_avatar_heroine4_animated"), "Elle mélange des potions roses et du charisme pur. Elle pourrait vendre un dictionnaire à un muet.", ELEMENT_ARCANE)
            avatars += AvatarItem(5, "Trame", "Fille", drawableId("avatar_heroine5"), drawableId("bg_avatar_heroine5"), rawId("bg_avatar_heroine5_animated"), "Pendant qu'elle tricote, elle tisse les fils de ton destin. Elle sait déjà quelle question l'IA va te poser.", ELEMENT_TERRE)
            avatars += AvatarItem(6, "Strategos", "Fille", drawableId("avatar_heroine6"), drawableId("bg_avatar_heroine6"), rawId("bg_avatar_heroine6_animated"), "Une stratège au regard d'acier. Pour elle, un examen n'est qu'une partie d'échecs dont elle connaît déjà la fin.", ELEMENT_METAL)
            avatars += AvatarItem(7, "Flora", "Fille", drawableId("avatar_heroine7"), drawableId("bg_avatar_heroine7"), rawId("bg_avatar_heroine7_animated"), "Elle murmure à l'oreille des plantes et des cerveaux. Avec elle, ton savoir fleurit même en plein hiver.", ELEMENT_NATURE)
            avatars += AvatarItem(8, "Cyberia", "Fille", drawableId("avatar_heroine8"), drawableId("bg_avatar_heroine8"), rawId("bg_avatar_heroine8_animated"), "La hackeuse de l'Olympe. Elle a codé son propre bouclier pour que ton Streak ne s'arrête jamais.", ELEMENT_CYBERNE)
            avatars += AvatarItem(9, "Cristal", "Fille", drawableId("avatar_heroine9"), drawableId("bg_avatar_heroine9"), rawId("bg_avatar_heroine9_animated"), "Elle voit à travers le verre et les mensonges. Sa clarté d'esprit brise tous les pièges des questions complexes.", ELEMENT_GLACE)
            avatars += AvatarItem(10, "Butine", "Fille", drawableId("avatar_heroine10"), drawableId("bg_avatar_heroine10"), rawId("bg_avatar_heroine10_animated"), "La reine de la récup' et du butin. Elle trouve des fragments de savoir là où tout le monde voit des brouillons.", ELEMENT_OR)
        } else {
            avatars += AvatarItem(1, "Spark", "Garçon", drawableId("avatar_hero1"), drawableId("bg_avatar_hero1"), rawId("bg_avatar_hero1_animated"), "Un génie de la tech qui a transformé son smartphone en paratonnerre. Avec lui, tes révisions vont prendre un coup de jus !", ELEMENT_FOUDRE)
            avatars += AvatarItem(2, "Swell", "Garçon", drawableId("avatar_hero2"), drawableId("bg_avatar_hero2"), rawId("bg_avatar_hero2_animated"), "Le roi de la glisse olympique. Il surfe sur les données de tes cours comme sur une vague géante de Poséidon.", ELEMENT_EAU)
            avatars += AvatarItem(3, "Atlas", "Garçon", drawableId("avatar_hero3"), drawableId("bg_avatar_hero3"), rawId("bg_avatar_hero3_animated"), "Perdu dans ses pensées ? Non, il cartographie ton cerveau. Aucun labyrinthe de révision ne lui résiste.", ELEMENT_TERRE)
            avatars += AvatarItem(4, "Mécano", "Garçon", drawableId("avatar_hero4"), drawableId("bg_avatar_hero4"), rawId("bg_avatar_hero4_animated"), "Donne-lui un trombone et une gomme, il t'en fera une épée. C'est le MacGyver de la Forge d'Héphaïstos.", ELEMENT_METAL)
            avatars += AvatarItem(5, "Locus", "Garçon", drawableId("avatar_hero5"), drawableId("bg_avatar_hero5"), rawId("bg_avatar_hero5_animated"), "Il mémorise tout. Absolument tout. La liste de courses, les traités de paix, et même tes 47 dernières erreurs.", ELEMENT_LUMIERE)
            avatars += AvatarItem(6, "Pyros", "Garçon", drawableId("avatar_hero6"), drawableId("bg_avatar_hero6"), rawId("bg_avatar_hero6_animated"), "Il a remplacé son stylo par une torche. Sa passion pour les sciences fait fondre les problèmes les plus complexes.", ELEMENT_FEU)
            avatars += AvatarItem(7, "Verdant", "Garçon", drawableId("avatar_hero7"), drawableId("bg_avatar_hero7"), rawId("bg_avatar_hero7_animated"), "Un druide en baskets. Il a juré de faire pousser ton niveau scolaire comme un chêne sacré de la forêt d'Artémis.", ELEMENT_NATURE)
            avatars += AvatarItem(8, "Axiom", "Garçon", drawableId("avatar_hero8"), drawableId("bg_avatar_hero8"), rawId("bg_avatar_hero8_animated"), "Le maître des algorithmes divins. Il a calculé que tu réussiras ton exam si tu joues 20 minutes par jour.", ELEMENT_CYBERNE)
            avatars += AvatarItem(9, "Frost", "Garçon", drawableId("avatar_hero9"), drawableId("bg_avatar_hero9"), rawId("bg_avatar_hero9_animated"), "Sang-froid légendaire. Sous la pression des partiels, c'est lui qui reste le plus calme. Son arme : la méthode.", ELEMENT_GLACE)
            avatars += AvatarItem(10, "Aurum", "Garçon", drawableId("avatar_hero10"), drawableId("bg_avatar_hero10"), rawId("bg_avatar_hero10_animated"), "Un collectionneur d'éclairs dorés. Pour lui, chaque bonne réponse est une pépite d'or dans le trésor de l'Olympe.", ELEMENT_OR)
        }
    }

    private fun setupBackgroundPlayer() {
        backgroundPlayer = ExoPlayer.Builder(this).build().also { player ->
            binding.pvAvatarBackground.player = player
            binding.pvAvatarBackground.useController = false
            player.volume = 0f
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.addListener(backgroundPlayerListener)
        }
    }

    private fun scheduleBackgroundVideo(videoResId: Int) {
        backgroundSwitchJob?.cancel()
        if (videoResId == 0 || videoResId == currentVideoResId) return

        backgroundSwitchJob = lifecycleScope.launch {
            delay(200L)
            currentVideoResId = videoResId
            try {
                val uri = Uri.parse("android.resource://$packageName/$videoResId")
                backgroundPlayer?.let { player ->
                    player.setMediaItem(MediaItem.fromUri(uri))
                    player.prepare()
                    player.play()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Vidéo fond : ${e.message}")
            }
        }
    }

    private fun setupCarousel() {
        layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        snapHelper = LinearSnapHelper()
        binding.rvAvatars.layoutManager = layoutManager
        snapHelper.attachToRecyclerView(binding.rvAvatars)
        val adapter = AvatarAdapter(avatars)
        binding.rvAvatars.adapter = adapter
        binding.rvAvatars.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.rvAvatars.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val rvWidth = binding.rvAvatars.width
                val density = resources.displayMetrics.density
                val itemWidthPx = (AVATAR_ITEM_WIDTH_DP * density).toInt()
                val sidePadding = (rvWidth - itemWidthPx) / 2
                binding.rvAvatars.setPadding(sidePadding, 0, sidePadding, 0)
                binding.rvAvatars.clipToPadding = false
                if (avatars.isNotEmpty()) {
                    currentSelectedAvatar = avatars[0]
                    refreshSelectedAvatarUI(avatars[0])
                    centerToPosition(0)
                    updateCarouselDepth()
                }
            }
        })
        binding.rvAvatars.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                updateCarouselDepth()
            }

            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val snapView = snapHelper.findSnapView(layoutManager) ?: return
                    val position = layoutManager.getPosition(snapView)
                    if (position in avatars.indices) {
                        currentSelectedAvatar = avatars[position]
                        refreshSelectedAvatarUI(avatars[position])
                    }
                    updateCarouselDepth()
                    if (ignoreFirstSnap) ignoreFirstSnap = false else {
                        try {
                            SoundManager.playSFX(this@AvatarActivity, R.raw.sfx_select_wisdom)
                        } catch (e: Exception) {
                            Log.w(TAG, "SFX : ${e.message}")
                        }
                    }
                }
            }
        })
    }

    private fun centerToPosition(position: Int) {
        binding.rvAvatars.smoothScrollToPosition(position)
        binding.rvAvatars.postDelayed({
            if (position in avatars.indices) {
                currentSelectedAvatar = avatars[position]
                refreshSelectedAvatarUI(avatars[position])
                updateCarouselDepth()
            }
        }, 180L)
    }

    private fun updateCarouselDepth() {
        val rvCenterX = binding.rvAvatars.width / 2f
        for (i in 0 until binding.rvAvatars.childCount) {
            val child = binding.rvAvatars.getChildAt(i)
            val childCenterX = (child.left + child.right) / 2f
            val distance = kotlin.math.abs(rvCenterX - childCenterX)
            val normalized = (distance / rvCenterX).coerceIn(0f, 1f)
            child.scaleX = 0.75f + (1f - normalized) * 0.25f
            child.scaleY = 0.75f + (1f - normalized) * 0.25f
            child.alpha = 0.55f + (1f - normalized) * 0.45f
            child.translationY = normalized * 28f
        }
    }

    private fun refreshSelectedAvatarUI(avatar: AvatarItem) {
        binding.tvAvatarName.text = avatar.name
        try {
            if (avatar.backgroundResId != 0) binding.ivAvatarBackground.setImageResource(avatar.backgroundResId)
        } catch (e: Exception) {
            Log.w(TAG, "setImageResource : ${e.message}")
        }
        binding.pvAvatarBackground.visibility = View.INVISIBLE
        binding.pvAvatarBackground.alpha = 0f
        afficherTexteRPG(avatar.description)
        scheduleBackgroundVideo(avatar.backgroundVideoResId)
    }

    private fun afficherTexteRPG(texteComplet: String) {
        typewriterSessionId += 1L
        val localSessionId = typewriterSessionId

        typewriterJob?.cancel()
        SoundManager.stopAllDialogueBlips()

        typewriterJob = lifecycleScope.launch {
            binding.tvZeusSpeech.text = ""
            for (i in texteComplet.indices) {
                if (localSessionId != typewriterSessionId || !isDialogueScreenUsable()) break

                binding.tvZeusSpeech.text = texteComplet.substring(0, i + 1)
                try {
                    SoundManager.playSFXDialogueBlip(this@AvatarActivity, R.raw.sfx_dialogue_blip)
                } catch (_: Exception) {
                }
                delay(24L)
            }
        }
    }

    private fun isDialogueScreenUsable(): Boolean {
        return !isFinishing &&
            !isDestroyed &&
            binding.tvZeusSpeech.isAttachedToWindow &&
            binding.tvZeusSpeech.visibility == View.VISIBLE
    }

    private fun setupConfirmButton() {
        binding.btnConfirmAvatar.setOnClickListener {
            currentSelectedAvatar?.let { avatar ->
                afficherTexteRPG("Tu veux lier ton destin à ${avatar.name} ? C'est ton choix définitif !")
                binding.layoutConfirmButtons.visibility = View.VISIBLE
                binding.btnConfirmAvatar.visibility = View.GONE
                binding.btnYesAvatar.setOnClickListener { lancerCreationOuAjoutHero(avatar) }
                binding.btnNoAvatar.setOnClickListener {
                    binding.layoutConfirmButtons.visibility = View.GONE
                    binding.btnConfirmAvatar.visibility = View.VISIBLE
                    afficherTexteRPG("Prends ton temps... Fais défiler le carrousel et observe les héros sur les côtés.")
                }
            }
        }
    }

    private fun lancerCreationOuAjoutHero(avatar: AvatarItem) {
        binding.btnYesAvatar.isEnabled = false
        binding.btnNoAvatar.isEnabled = false
        afficherTexteRPG("Hercule forge ton destin dans les étoiles... Un instant !")

        val currentUser = FirebaseAuthManager.getCurrentUser()
        val hasPendingOnboarding = OnboardingSession.isReady()
        val creationMode = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
            .getString(PREF_HERO_CREATION_MODE, "") ?: ""
        val isExistingAccountHeroFlow = creationMode == HERO_CREATION_MODE_EXISTING_ACCOUNT
        val isNewAccountFlow = creationMode == HERO_CREATION_MODE_NEW_ACCOUNT

        when {
            isExistingAccountHeroFlow && currentUser != null -> {
                OnboardingSession.clear()
                lancerCreationHeroSupplementaire(
                    currentUser.uid,
                    currentUser.email.orEmpty(),
                    currentUser.isEmailVerified,
                    avatar
                )
            }

            (isNewAccountFlow && hasPendingOnboarding) || (hasPendingOnboarding && currentUser == null) -> {
                lancerCreationCompteNeuf(avatar)
            }

            currentUser != null -> {
                OnboardingSession.clear()
                lancerCreationHeroSupplementaire(
                    currentUser.uid,
                    currentUser.email.orEmpty(),
                    currentUser.isEmailVerified,
                    avatar
                )
            }

            else -> {
                binding.btnYesAvatar.isEnabled = true
                binding.btnNoAvatar.isEnabled = true
                afficherTexteRPG("L'Olympe ne retrouve ni compte actif, ni inscription en cours.")
                Toast.makeText(this, "Session expirée. Reviens à l'écran de connexion.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun lancerCreationCompteNeuf(avatar: AvatarItem) {
        val email = OnboardingSession.email
        val password = OnboardingSession.password
        Log.d(TAG, "Création compte Firebase pour onboarding avatar")

        FirebaseAuthManager.createAccount(
            email = email,
            password = password,
            onSuccess = { user ->
                Log.d(TAG, "Compte Firebase créé. uid=${user.uid}")
                lifecycleScope.launch {
                    val success = persisterHeroPourUid(
                        firebaseUid = user.uid,
                        accountEmail = email,
                        isEmailVerified = user.isEmailVerified,
                        avatar = avatar,
                        isFirstTime = true
                    )
                    if (success) {
                        genererEtAfficherCodeDeSecoursSiNecessaire(
                            firebaseUid = user.uid,
                            accountEmail = email
                        ) {
                            OnboardingSession.clear()
                            getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
                                .edit()
                                .remove(PREF_HERO_CREATION_MODE)
                                .apply()
                            SoundManager.stopMusic()
                            startActivity(Intent(this@AvatarActivity, IntroVideoActivity::class.java))
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            binding.btnYesAvatar.isEnabled = true
                            binding.btnNoAvatar.isEnabled = true
                            afficherTexteRPG("L'Olympe a forgé le compte, mais le héros n'a pas pu être scellé.")
                        }
                    }
                }
            },
            onError = { message ->
                Log.e(TAG, "Erreur création Firebase : $message")

                val isExistingEmail = message.contains("déjà lié", ignoreCase = true) ||
                    message.contains("already", ignoreCase = true) ||
                    message.contains("exists", ignoreCase = true)

                if (isExistingEmail) {
                    runOnUiThread {
                        afficherTexteRPG("Je reconnais ce sceau. Cet email existe déjà. Je te ramène immédiatement vers le Panthéon de ce compte.")
                    }
                    raccorderCompteExistantEtOuvrirHeroSelect(email, password)
                } else {
                    runOnUiThread {
                        binding.btnYesAvatar.isEnabled = true
                        binding.btnNoAvatar.isEnabled = true
                        afficherTexteRPG("L'Olympe est troublé... $message")
                        Toast.makeText(this, "Erreur : $message", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    /**
     * Si createAccount échoue parce que l'email existe déjà, on tente immédiatement
     * de rattacher l'utilisateur au compte existant avec le mot de passe saisi lors
     * de l'inscription. En cas de succès, on ouvre directement HeroSelectActivity
     * liée à ce compte, comme demandé par le flow produit.
     */
    private fun raccorderCompteExistantEtOuvrirHeroSelect(
        email: String,
        password: String
    ) {
        FirebaseAuthManager.signIn(
            email = email,
            password = password,
            onSuccess = { user ->
                AccountRegistry.registerUid(this, user.uid)
                AccountRegistry.setActiveUid(this, user.uid)
                AccountRegistry.rememberAccountEmail(this, user.uid, user.email ?: email)

                val sharedPref = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
                sharedPref.edit()
                    .putBoolean("HAS_ACCOUNT", true)
                    .putString("ACCOUNT_EMAIL", user.email ?: email)
                    .putString("RECOVERY_EMAIL", user.email ?: email)
                    .putString("FIREBASE_UID", user.uid)
                    .putBoolean("IS_EMAIL_VERIFIED", user.isEmailVerified)
                    .putString(PREF_HERO_CREATION_MODE, HERO_CREATION_MODE_EXISTING_ACCOUNT)
                    .remove("PENDING_ACCOUNT_EMAIL")
                    .apply()

                OnboardingSession.clear()

                runOnUiThread {
                    binding.btnYesAvatar.isEnabled = true
                    binding.btnNoAvatar.isEnabled = true
                    afficherTexteRPG("Je t'ai reconnu, mortel. Ouvre maintenant l'un de tes héros ou forge-en un nouveau dans ce Panthéon.")
                    lifecycleScope.launch {
                        delay(700L)
                        startActivity(Intent(this@AvatarActivity, HeroSelectActivity::class.java).apply {
                            putExtra(HeroSelectActivity.EXTRA_FIREBASE_UID, user.uid)
                        })
                        finish()
                    }
                }
            },
            onError = { signInMessage ->
                runOnUiThread {
                    binding.btnYesAvatar.isEnabled = true
                    binding.btnNoAvatar.isEnabled = true
                    val looksLikeBadPassword = signInMessage.contains("Mot de passe incorrect", ignoreCase = true) ||
                        signInMessage.contains("password", ignoreCase = true) ||
                        signInMessage.contains("credential", ignoreCase = true)

                    if (looksLikeBadPassword) {
                        afficherTexteRPG("Cet email existe déjà dans l'Olympe, mais le mot de passe invoqué n'est pas le bon.")
                        Toast.makeText(this, "Cet email existe déjà, mais le mot de passe est incorrect.", Toast.LENGTH_LONG).show()
                    } else {
                        afficherTexteRPG("Le compte existe déjà, mais je n'ai pas pu t'y rattacher pour l'instant. $signInMessage")
                        Toast.makeText(this, signInMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    private fun lancerCreationHeroSupplementaire(
        firebaseUid: String,
        accountEmail: String,
        isEmailVerified: Boolean,
        avatar: AvatarItem
    ) {
        lifecycleScope.launch {
            val slot = AccountRegistry.getActiveSlot(this@AvatarActivity, firebaseUid)
            if (AccountRegistry.getSlotCache(this@AvatarActivity, firebaseUid, slot) != null) {
                runOnUiThread {
                    binding.btnYesAvatar.isEnabled = true
                    binding.btnNoAvatar.isEnabled = true
                    afficherTexteRPG("Ce destin est déjà occupé. Choisis un autre emplacement dans le Panthéon.")
                    Toast.makeText(this@AvatarActivity, "Le slot sélectionné contient déjà un héros.", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            val success = persisterHeroPourUid(
                firebaseUid = firebaseUid,
                accountEmail = accountEmail,
                isEmailVerified = isEmailVerified,
                avatar = avatar,
                isFirstTime = false
            )

            if (success) {
                getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .remove(PREF_HERO_CREATION_MODE)
                    .apply()
                SoundManager.stopMusic()
                startActivity(Intent(this@AvatarActivity, IntroVideoActivity::class.java))
                finish()
            } else {
                runOnUiThread {
                    binding.btnYesAvatar.isEnabled = true
                    binding.btnNoAvatar.isEnabled = true
                    afficherTexteRPG("L'Olympe n'a pas pu sceller ce nouveau héros.")
                }
            }
        }
    }

    private suspend fun persisterHeroPourUid(
        firebaseUid: String,
        accountEmail: String,
        isEmailVerified: Boolean,
        avatar: AvatarItem,
        isFirstTime: Boolean
    ): Boolean {
        val sharedPref = getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        return try {
            val age = sharedPref.getInt("USER_AGE", 15)
            val classe = sharedPref.getString("USER_CLASS", "Terminale") ?: "Terminale"
            val pseudo = sharedPref.getString("AVATAR_PSEUDO", "Héros") ?: "Héros"
            val avatarResName = try {
                resources.getResourceEntryName(avatar.imageResId)
            } catch (_: Exception) {
                "avatar_hero1"
            }

            sharedPref.edit()
                .putBoolean("HAS_ACCOUNT", true)
                .putString("ACCOUNT_EMAIL", accountEmail)
                .putString("RECOVERY_EMAIL", accountEmail)
                .putString("FIREBASE_UID", firebaseUid)
                .putBoolean("IS_EMAIL_VERIFIED", isEmailVerified)
                .putBoolean("IS_REGISTERED", true)
                .putBoolean("IS_FIRST_TIME", isFirstTime)
                .putInt("SELECTED_AVATAR_RES", avatar.imageResId)
                .putString("SELECTED_AVATAR_NAME", avatar.name)
                .remove("PENDING_ACCOUNT_EMAIL")
                .remove(PREF_HERO_CREATION_MODE)
                .apply()

            AccountRegistry.setActiveUid(this@AvatarActivity, firebaseUid)
            AccountRegistry.registerUid(this@AvatarActivity, firebaseUid)
            AccountRegistry.rememberAccountEmail(this@AvatarActivity, firebaseUid, accountEmail)

            val profile = UserProfile(
                id = 1,
                cognitivePattern = userGender,
                age = age,
                classLevel = classe,
                mood = "Prêt",
                xp = 0,
                streak = 0,
                pseudo = pseudo,
                avatarResName = avatarResName,
                accountEmail = accountEmail,
                recoveryEmail = accountEmail,
                firebaseUid = firebaseUid,
                isEmailVerified = isEmailVerified
            )

            withContext(Dispatchers.IO) {
                AppDatabase.resetInstance()
                val db = AppDatabase.getDatabase(this@AvatarActivity)
                db.iAristoteDao().saveUserProfile(profile)
                val totalCourses = db.iAristoteDao().countCourses()
                AccountRegistry.saveSlotCache(
                    this@AvatarActivity,
                    firebaseUid,
                    AccountRegistry.getActiveSlot(this@AvatarActivity, firebaseUid),
                    AccountRegistry.AccountCache(
                        uid = firebaseUid,
                        pseudo = profile.pseudo,
                        level = profile.level,
                        avatarResName = profile.avatarResName,
                        totalPlayTimeSeconds = profile.totalPlayTimeSeconds,
                        eclatsSavoir = profile.eclatsSavoir,
                        ambroisie = profile.ambroisie,
                        totalQuizDone = profile.totalQuizDone,
                        totalCoursScanned = totalCourses,
                        slotNumber = AccountRegistry.getActiveSlot(this@AvatarActivity, firebaseUid)
                    )
                )
            }

            Log.d(TAG, "Profil Room + cache local sauvegardés pour uid=$firebaseUid")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Sauvegarde profil Room : ${e.message}", e)
            false
        }
    }

    private fun genererEtAfficherCodeDeSecoursSiNecessaire(
        firebaseUid: String,
        accountEmail: String,
        onContinue: () -> Unit
    ) {
        if (firebaseUid.isBlank()) {
            onContinue()
            return
        }

        val hasLocalCode = AccountRecoveryManager.hasLocalRecoveryCode(this, firebaseUid)
        val isAcknowledged = AccountRecoveryManager.isRecoveryCodeAcknowledged(this, firebaseUid)

        if (hasLocalCode && isAcknowledged) {
            onContinue()
            return
        }

        if (hasLocalCode) {
            afficherDialogueCodeDeSecours(
                firebaseUid = firebaseUid,
                rawCode = AccountRecoveryManager.getLocalRecoveryCode(this, firebaseUid),
                hint = AccountRecoveryManager.getLocalRecoveryHint(this, firebaseUid),
                onContinue = onContinue
            )
            return
        }

        val rawCode = AccountRecoveryManager.generateRecoveryCode()
        AccountRecoveryManager.saveRecoveryCode(
            context = this,
            firebaseUid = firebaseUid,
            accountEmail = accountEmail,
            rawCode = rawCode,
            onSuccess = {
                runOnUiThread {
                    afficherDialogueCodeDeSecours(
                        firebaseUid = firebaseUid,
                        rawCode = rawCode,
                        hint = AccountRecoveryManager.buildHint(rawCode),
                        onContinue = onContinue
                    )
                }
            },
            onError = { message ->
                runOnUiThread {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    onContinue()
                }
            }
        )
    }

    private fun afficherDialogueCodeDeSecours(
        firebaseUid: String,
        rawCode: String,
        hint: String,
        onContinue: () -> Unit
    ) {
        val message = if (rawCode.isNotBlank()) {
            "Recopie ce code et garde-le dans un endroit sûr.\n\n" +
                rawCode +
                "\n\n" +
                "Il protège ce compte si tu perds l'accès à ton email."
        } else {
            "Le code complet n'est pas disponible en clair sur cet appareil.\n\n" +
                "Indice local : $hint\n\n" +
                "Valide seulement si tu l'as déjà conservé ailleurs."
        }

        AlertDialog.Builder(this)
            .setTitle("Code de secours du compte")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("J'ai bien gardé ce code") { _, _ ->
                AccountRecoveryManager.markRecoveryCodeAcknowledged(this, firebaseUid)
                afficherTexteRPG("Ton sceau de secours est validé. Tu pourras le revoir plus tard dans les réglages du compte.")
                onContinue()
            }
            .show()
    }

    private fun drawableId(name: String): Int = resources.getIdentifier(name, "drawable", packageName)
    private fun rawId(name: String): Int = resources.getIdentifier(name, "raw", packageName)
}

package com.revizeus.app

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.databinding.ActivityOracleBinding
import com.revizeus.app.databinding.DialogOracleChoiceBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * ═══════════════════════════════════════════════════════════════
 * ÉCRAN ORACLE — Scanner & Galerie
 * ═══════════════════════════════════════════════════════════════
 */
class OracleActivity : BaseActivity() {

    private var previousRememberedMusicResId: Int = -1
    private var shouldRestorePreviousMusicOnFinish: Boolean = true

    private lateinit var binding: ActivityOracleBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private val godAnim = GodSpeechAnimator()
    private var typewriterJob: Job? = null
    private var sendImagesJob: Job? = null

    private val selectedImageUris = mutableListOf<Uri>()
    private var oracleChoiceDialog: Dialog? = null

    // Fond premium Oracle : vidéo si disponible, image fixe sinon.
    private var animatedBackgroundHelper: AnimatedBackgroundHelper? = null

    // Ajout de l'animator pour la ligne de scan visuelle
    private var scanAnimator: ObjectAnimator? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            // BLOC B : Conversion Toast → Dialogue RPG
            DialogRPGManager.showTechnicalError(
                activity = this,
                errorType = TechnicalErrorType.CAMERA_PERMISSION
            )
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) {
            // BLOC B : Conversion Toast → Dialogue RPG
            DialogRPGManager.showInfo(
                activity = this,
                godId = "athena",
                message = "Athéna n'a vu aucun parchemin sélectionné. Essaie à nouveau."
            )
            return@registerForActivityResult
        }

        val copiedUris = copyUrisToCache(uris)
        if (copiedUris.isNotEmpty()) {
            sendImagesToResult(copiedUris)
        } else {
            // BLOC B : Conversion Toast → Dialogue RPG
            DialogRPGManager.showTechnicalError(
                activity = this,
                errorType = TechnicalErrorType.FILE_NOT_FOUND
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityOracleBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            Log.e("REVIZEUS_ORACLE", "Erreur inflation ActivityOracleBinding : ${e.message}", e)
            // BLOC B : Conversion Toast → Dialogue RPG
            DialogRPGManager.showInfo(
                activity = this,
                godId = "zeus",
                message = "Le portail de l'Oracle a rencontré une difficulté. Zeus te demande de réessayer."
            )
            finish()
            return
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // IMPORTANT : tant que le joueur n'a pas explicitement choisi la caméra,
        // on garde le PreviewView caché pour que la vidéo Oracle reste vraiment visible.
        try {
            binding.viewFinder.visibility = View.GONE
            binding.btnCapture.visibility = View.GONE
            binding.layoutCaptureSession.visibility = View.GONE
            binding.layoutError.visibility = View.GONE
            binding.viewFinder.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            binding.viewFinder.alpha = 0.92f
        } catch (_: Exception) {
        }

        installerFondPremiumOracle()
        cameraExecutor = Executors.newSingleThreadExecutor()

        previousRememberedMusicResId = try { SoundManager.getRememberedMusicResId() } catch (_: Exception) { -1 }
        try { SoundManager.rememberMusic(R.raw.bgm_oracle) } catch (e: Exception) {
            Log.w("REVIZEUS_ORACLE", "Impossible de mémoriser la BGM Oracle à l'initialisation", e)
        }

        try {
            updateSelectionUi()
        } catch (e: Exception) {
            Log.e("REVIZEUS_ORACLE", "Erreur updateSelectionUi initial : ${e.message}", e)
        }

        try {
            showOracleChoiceDialog()
        } catch (e: Exception) {
            Log.e("REVIZEUS_ORACLE", "Erreur ouverture dialogue Oracle : ${e.message}", e)
            // BLOC B : Conversion Toast → Dialogue RPG
            DialogRPGManager.showInfo(
                activity = this,
                godId = "zeus",
                message = "Le portail de l'Oracle a échoué à s'ouvrir. Zeus te demande de réessayer."
            )
        }

        binding.btnCapture.setOnClickListener {
            try {
                takePhoto()
            } catch (e: Exception) {
                Log.e("REVIZEUS_ORACLE", "Erreur takePhoto : ${e.message}", e)
                // BLOC B : Conversion Toast → Dialogue RPG
                DialogRPGManager.showInfo(
                    activity = this,
                    godId = "athena",
                    message = "Athéna n'a pas pu capturer cette page. L'Œil Divin a besoin d'un meilleur éclairage."
                )
            }
        }

        binding.btnValidateBatch.setOnClickListener {
            try {
                if (selectedImageUris.isEmpty()) {
                    // BLOC B : Conversion Toast → Dialogue RPG
                    DialogRPGManager.showInfo(
                        activity = this,
                        godId = "athena",
                        message = "Aucune page n'est prête pour l'Oracle. Capture au moins un parchemin avant de continuer."
                    )
                } else {
                    sendImagesToResult(selectedImageUris)
                }
            } catch (e: Exception) {
                Log.e("REVIZEUS_ORACLE", "Erreur validation batch : ${e.message}", e)
                // BLOC B : Conversion Toast → Dialogue RPG
                DialogRPGManager.showInfo(
                    activity = this,
                    godId = "hermes",
                    message = "Hermès n'a pas pu livrer tes pages à l'Oracle. Vérifie ta connexion divine."
                )
            }
        }

        binding.btnResetBatch.setOnClickListener {
            try {
                selectedImageUris.clear()
                updateSelectionUi()
                // BLOC B : Conversion Toast → Dialogue RPG
                DialogRPGManager.showInfo(
                    activity = this,
                    godId = "athena",
                    message = "Les pages capturées ont été effacées du rouleau temporaire."
                )
            } catch (e: Exception) {
                Log.e("REVIZEUS_ORACLE", "Erreur reset batch : ${e.message}", e)
            }
        }

        binding.btnRetry.setOnClickListener {
            try {
                binding.layoutError.visibility = View.GONE
                showOracleChoiceDialog()
            } catch (e: Exception) {
                Log.e("REVIZEUS_ORACLE", "Erreur retry dialogue Oracle : ${e.message}", e)
                // BLOC B : Conversion Toast → Dialogue RPG
                DialogRPGManager.showInfo(
                    activity = this,
                    godId = "zeus",
                    message = "Le portail de l'Oracle refuse de se rouvrir. Zeus te demande de patienter un instant."
                )
            }
        }

        binding.btnBackDashboard.setOnClickListener {
            finish()
        }
    }

    /**
     * GESTION DE LA MUSIQUE
     * Lancement du BGM Oracle. Au retour (finish), le Dashboard relancera le sien dans son onResume.
     */
    override fun onResume() {
        super.onResume()
        animatedBackgroundHelper?.start(
            accentColor = Color.parseColor("#FFD700"),
            mode = OlympianParticlesView.ParticleMode.SAVOIR
        )
        try {
            SoundManager.cancelDelayedMusic()
            SoundManager.rememberMusic(R.raw.bgm_oracle)
            if (!SoundManager.isPlayingMusic() || SoundManager.getCurrentMusicResId() != R.raw.bgm_oracle) {
                SoundManager.playMusicDelayed(this, R.raw.bgm_oracle, 120L)
            }
        } catch (e: Exception) {
            Log.e("REVIZEUS_ORACLE", "Erreur SoundManager : ${e.message}")
        }
    }

    private fun installerFondPremiumOracle() {
        animatedBackgroundHelper = AnimatedBackgroundHelper(
            targetView = binding.root,
            backgroundImageView = binding.ivOracleBackground
        )
        animatedBackgroundHelper?.configurePremiumBackground(
            staticDrawableRes = getDrawableResOrFallback("bg_oracle", "bg_olympus_dark"),
            videoRawRes = getRawResByName("bg_oracle_animated"),
            imageAlpha = 0.12f,
            loopVideo = true,
            videoAlpha = 0.96f,
            videoVolume = 0.50f
        )
    }

    private fun getRawResByName(resName: String): Int {
        return resources.getIdentifier(resName, "raw", packageName)
    }

    private fun getDrawableResOrFallback(primaryName: String, fallbackName: String): Int {
        val primary = resources.getIdentifier(primaryName, "drawable", packageName)
        if (primary != 0) return primary
        val fallback = resources.getIdentifier(fallbackName, "drawable", packageName)
        return if (fallback != 0) fallback else R.drawable.bg_olympus_dark
    }

    private fun buildOracleOpeningSpeech(): String {
        return if (!TutorialManager.hasHeroSeenFeature(this, "oracle")) {
            "Héros... voici l'Oracle. Il sert à transformer un cours, une image ou une simple demande en savoir exploitable. Tu peux capturer des pages avec l'Artefact Divin, ouvrir ta galerie pour envoyer plusieurs parchemins, ou invoquer une demande écrite. Une fois la matière livrée, l'Oracle l'analyse pour préparer résumé, compréhension et quiz."
        } else {
            "Héros... Souhaites-tu utiliser l'Artefact Divin pour capturer une ou plusieurs pages, ouvrir tes Archives pour en sélectionner plusieurs, ou forger directement une demande à l'Oracle ?"
        }
    }

    private fun showOracleChoiceDialog() {
        oracleChoiceDialog?.dismiss()
        oracleChoiceDialog = null

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding = DialogOracleChoiceBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Configuration de la fenêtre du dialogue pour le PLEIN ÉCRAN
        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setGravity(Gravity.CENTER)
            // Retire les marges de fenêtre par défaut d'Android pour coller aux bords de l'écran
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        }

        dialog.setCancelable(false)
        oracleChoiceDialog = dialog

        val oracleOpeningSpeech = buildOracleOpeningSpeech()
        dialogBinding.tvZeusDialogSpeech.text = oracleOpeningSpeech
        dialogBinding.tvZeusDialogSpeech.setOnClickListener {
            try {
                typewriterJob?.cancel()
                dialogBinding.tvZeusDialogSpeech.text = oracleOpeningSpeech
                SoundManager.playSFX(this, R.raw.sfx_dialogue_blip)
            } catch (_: Exception) {
                dialogBinding.tvZeusDialogSpeech.text = oracleOpeningSpeech
            }
        }

        dialogBinding.btnCamera.setOnClickListener {
            try {
                typewriterJob?.cancel()
                godAnim.stopSpeaking(dialogBinding.layoutZeusDialog)
                dialog.dismiss()
                checkCameraPermissionAndStart()
            } catch (e: Exception) {
                Log.e("REVIZEUS_ORACLE", "Erreur bouton caméra Oracle : ${e.message}", e)
                // BLOC B : Conversion Toast → Dialogue RPG
                DialogRPGManager.showTechnicalError(
                    activity = this,
                    errorType = TechnicalErrorType.CAMERA_PERMISSION
                )
            }
        }

        dialogBinding.btnGallery.setOnClickListener {
            try {
                typewriterJob?.cancel()
                godAnim.stopSpeaking(dialogBinding.layoutZeusDialog)
                dialog.dismiss()
                binding.viewFinder.visibility = View.GONE
                binding.btnCapture.visibility = View.GONE
                binding.layoutCaptureSession.visibility = View.GONE
                galleryLauncher.launch("image/*")
            } catch (e: Exception) {
                Log.e("REVIZEUS_ORACLE", "Erreur bouton galerie Oracle : ${e.message}", e)
                // BLOC B : Conversion Toast → Dialogue RPG
                DialogRPGManager.showTechnicalError(
                    activity = this,
                    errorType = TechnicalErrorType.STORAGE_PERMISSION
                )
            }
        }

        dialogBinding.btnCreateFromPrompt.setOnClickListener {
            try {
                typewriterJob?.cancel()
                godAnim.stopSpeaking(dialogBinding.layoutZeusDialog)
                dialog.dismiss()
                startActivity(Intent(this, OraclePromptActivity::class.java))
            } catch (e: Exception) {
                Log.e("REVIZEUS_ORACLE", "Erreur bouton demande Oracle : ${e.message}", e)
                // BLOC B : Conversion Toast → Dialogue RPG
                DialogRPGManager.showInfo(
                    activity = this,
                    godId = "hephaestus",
                    message = "La forge de demande n'est pas accessible pour le moment. Héphaïstos travaille sur le portail."
                )
            }
        }

        dialogBinding.btnCancelOracle.setOnClickListener {
            try {
                typewriterJob?.cancel()
                godAnim.stopSpeaking(dialogBinding.layoutZeusDialog)
                dialog.dismiss()
                finish()
            } catch (e: Exception) {
                Log.e("REVIZEUS_ORACLE", "Erreur bouton annuler Oracle : ${e.message}", e)
                finish()
            }
        }

        dialog.setOnShowListener {
            try {
                dialogBinding.tvZeusDialogSpeech.text = ""
                typewriterJob?.cancel()
                typewriterJob = godAnim.typewriteSimple(
                    scope = lifecycleScope,
                    chibiView = dialogBinding.layoutZeusDialog,
                    textView = dialogBinding.tvZeusDialogSpeech,
                    text = oracleOpeningSpeech,
                    context = this,
                    onComplete = {
                        try {
                            TutorialManager.markHeroFeatureSeen(this@OracleActivity, "oracle")
                        } catch (_: Exception) {
                        }
                    }
                )
            } catch (_: Exception) {
                dialogBinding.tvZeusDialogSpeech.text = oracleOpeningSpeech
                try {
                    TutorialManager.markHeroFeatureSeen(this@OracleActivity, "oracle")
                } catch (_: Exception) {
                }
            }
        }

        dialog.setOnDismissListener {
            try {
                typewriterJob?.cancel()
                godAnim.stopSpeaking(dialogBinding.layoutZeusDialog)
            } catch (_: Exception) {
            }
            oracleChoiceDialog = null
        }

        dialog.show()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }

    private fun checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        try {
            binding.viewFinder.visibility = View.VISIBLE
            binding.btnCapture.visibility = View.VISIBLE
            binding.layoutCaptureSession.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e("REVIZEUS_ORACLE", "Erreur affichage UI caméra : ${e.message}", e)
            afficherErreurCamera("L'interface de l'Artefact ne répond pas correctement.")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("REVIZEUS_ORACLE", "Échec caméra : ${e.message}", e)
                afficherErreurCamera("L'Artefact Divin refuse de s'ouvrir. Réessaie.")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val localImageCapture = imageCapture ?: run {
            // BLOC B : Conversion Toast → Dialogue RPG
            DialogRPGManager.showInfo(
                activity = this,
                godId = "athena",
                message = "L'Œil Divin se prépare. Patiente un instant avant de capturer."
            )
            return
        }

        declencherVibrationFoudre(100L)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "revizeus_oracle_${System.currentTimeMillis()}.jpg")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
        ).build()

        localImageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("REVIZEUS_ORACLE", "Erreur capture image : ${exc.message}", exc)
                    // BLOC B : Conversion Toast → Dialogue RPG
                    DialogRPGManager.showInfo(
                        activity = this@OracleActivity,
                        godId = "athena",
                        message = "La capture a échoué. L'Œil Divin a besoin d'un meilleur angle ou d'un meilleur éclairage."
                    )
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    output.savedUri?.let { uri ->
                        selectedImageUris.add(uri)
                        updateSelectionUi()
                        // BLOC B : Conversion Toast → Dialogue RPG
                        DialogRPGManager.showInfo(
                            activity = this@OracleActivity,
                            godId = "athena",
                            message = "Page ${selectedImageUris.size} ajoutée au rouleau sacré. Capture d'autres pages ou envoie à l'Oracle."
                        )
                    }
                }
            }
        )
    }

    private fun updateSelectionUi() {
        val hasPages = selectedImageUris.isNotEmpty()
        binding.tvSelectedCount.text = if (hasPages) {
            "Pages prêtes pour l'Oracle : ${selectedImageUris.size}"
        } else {
            "Aucune page capturée pour le moment"
        }
        binding.btnValidateBatch.visibility = if (hasPages) View.VISIBLE else View.GONE
        binding.btnResetBatch.visibility = if (hasPages) View.VISIBLE else View.GONE
    }

    private fun copyUrisToCache(uris: List<Uri>): ArrayList<Uri> {
        val copiedUris = arrayListOf<Uri>()
        uris.forEachIndexed { index, uri ->
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val file = File(cacheDir, "gallery_temp_${index}_${System.currentTimeMillis()}.jpg")
                    val outputStream = FileOutputStream(file)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    copiedUris.add(Uri.fromFile(file))
                }
            } catch (e: Exception) {
                Log.e("REVIZEUS_ORACLE", "Erreur copie galerie : ${e.message}")
            }
        }
        return copiedUris
    }

    /**
     * DÉMARRE L'ANIMATION DE SCAN ET LE SON
     */
    private fun startScanAnimation() {
        try {
            binding.scanLine.visibility = View.VISIBLE
            val height = binding.viewFinder.height.toFloat()

            scanAnimator = ObjectAnimator.ofFloat(binding.scanLine, "translationY", 0f, height).apply {
                duration = 1500
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                start()
            }

            SoundManager.playLoopingScan(this, R.raw.sfx_oracle_scan, 0.28f)
        } catch (e: Exception) {
            Log.e("REVIZEUS_ORACLE", "Erreur startScanAnimation : ${e.message}", e)
        }
    }

    /**
     * ARRÊTE L'ANIMATION DE SCAN ET LE SON
     */
    private fun stopScanAnimation() {
        try {
            scanAnimator?.cancel()
            scanAnimator = null
            binding.scanLine.visibility = View.GONE
            SoundManager.stopLoopingScan()
        } catch (e: Exception) {
            Log.e("REVIZEUS_ORACLE", "Erreur stopScanAnimation : ${e.message}", e)
        }
    }

    private fun setOracleActionButtonsEnabled(enabled: Boolean) {
        try { binding.btnValidateBatch.isEnabled = enabled } catch (e: Exception) {
            Log.w("REVIZEUS_ORACLE", "setOracleActionButtonsEnabled: btnValidateBatch inaccessible", e)
        }
        try { binding.btnCapture.isEnabled = enabled } catch (e: Exception) {
            Log.w("REVIZEUS_ORACLE", "setOracleActionButtonsEnabled: btnCapture inaccessible", e)
        }
        try { binding.btnResetBatch.isEnabled = enabled } catch (e: Exception) {
            Log.w("REVIZEUS_ORACLE", "setOracleActionButtonsEnabled: btnResetBatch inaccessible", e)
        }
    }

    private fun sendImagesToResult(uris: List<Uri>) {
        if (uris.isEmpty()) return

        setOracleActionButtonsEnabled(false)
        startScanAnimation()

        sendImagesJob?.cancel()
        sendImagesJob = lifecycleScope.launch {
            try {
                delay(3000)

                if (isFinishing || isDestroyed) {
                    return@launch
                }

                stopScanAnimation()
                shouldRestorePreviousMusicOnFinish = false

                val intent = Intent(this@OracleActivity, ResultActivity::class.java)
                if (uris.size == 1) {
                    intent.putExtra("IMAGE_URI", uris.first().toString())
                }
                intent.putStringArrayListExtra("IMAGE_URIS", ArrayList(uris.map { it.toString() }))
                startActivity(intent)
                finish()
            } catch (e: CancellationException) {
                Log.d("REVIZEUS_ORACLE", "sendImagesJob annulé proprement")
                stopScanAnimation()
                throw e
            } catch (e: Exception) {
                Log.e("REVIZEUS_ORACLE", "Erreur transition ResultActivity : ${e.message}", e)
                stopScanAnimation()
                setOracleActionButtonsEnabled(true)
                // BLOC B : Conversion Toast → Dialogue RPG
                DialogRPGManager.showInfo(
                    activity = this@OracleActivity,
                    godId = "hermes",
                    message = "Hermès n'a pas pu livrer tes pages à l'Oracle. Vérifie ta connexion divine."
                )
            } finally {
                if (!isFinishing && !isDestroyed) {
                    setOracleActionButtonsEnabled(true)
                }
                sendImagesJob = null
            }
        }
    }

    private fun afficherErreurCamera(message: String) {
        binding.layoutError.visibility = View.VISIBLE
        binding.tvErrorStatus.text = message
        binding.viewFinder.visibility = View.GONE
        binding.btnCapture.visibility = View.GONE
        binding.layoutCaptureSession.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        animatedBackgroundHelper?.stop()
        typewriterJob?.cancel()
        if (!isFinishing) {
            sendImagesJob?.cancel()
            sendImagesJob = null
            setOracleActionButtonsEnabled(true)
        }
        try { stopScanAnimation() } catch (e: Exception) {
            Log.w("REVIZEUS_ORACLE", "onPause: stopScanAnimation a échoué", e)
        }
    }

    override fun onDestroy() {
        animatedBackgroundHelper?.release()
        animatedBackgroundHelper = null
        super.onDestroy()
        stopScanAnimation()
        sendImagesJob?.cancel()
        sendImagesJob = null
        typewriterJob?.cancel()
        oracleChoiceDialog?.dismiss()
        cameraExecutor.shutdown()
    }

    override fun finish() {
        try {
            stopScanAnimation()
            if (shouldRestorePreviousMusicOnFinish && previousRememberedMusicResId != -1) {
                SoundManager.rememberMusic(previousRememberedMusicResId)
                SoundManager.resumeRememberedMusicDelayed(this, 220L)
            }
        } catch (e: Exception) {
            Log.w("REVIZEUS_ORACLE", "finish: restauration audio Oracle échouée", e)
        }
        super.finish()
    }
}

package com.revizeus.app

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

/**
 * ═══════════════════════════════════════════════════════════════
 * BASE ACTIVITY — Cœur des écrans
 * ═══════════════════════════════════════════════════════════════
 * Utilité : Fournit les fonctions de base (son, vibration) à toutes les activités.
 *
 * CONSOLIDATION BLOC A :
 * ✅ Portrait forcé conservé
 * ✅ Loader global sécurisé
 * ✅ Arrêt global du TTS au retour / à la destruction
 * ✅ Reprise musicale plus défensive via la mémoire du SoundManager
 * ✅ Évite de relancer brutalement une ancienne BGM simplement parce
 *    qu'une Activity reçoit un onResume()
 * ✅ BLOC A — OnBackPressedCallback moderne (remplace onBackPressed déprécié)
 * ═══════════════════════════════════════════════════════════════
 */
abstract class BaseActivity : AppCompatActivity() {
    private val baseTag = "REVIZEUS_BASE"

    /**
     * Tag unique du loader global afin d'éviter tout doublon si plusieurs
     * appels showLoading() sont déclenchés très vite.
     */
    private val loadingDialogTag = "LoadingDivineDialog_Global"

    /**
     * Hooks conservatifs.
     * Ils permettent à un écran de couper l'automatisme du socle si besoin
     * sans casser l'héritage existant.
     */
    protected open fun shouldBaseActivityPauseMusicOnPause(): Boolean = true
    protected open fun shouldBaseActivityAttemptMusicRecoveryOnResume(): Boolean = true

    /**
     * BLOC A — Hook moderne pour gérer le retour système.
     * Les écrans qui héritent de BaseActivity peuvent override cette méthode
     * pour personnaliser le comportement du bouton retour Android.
     * 
     * Par défaut : comportement système standard (fermeture de l'activité).
     */
    protected open fun handleBackPressed() {
        // Comportement par défaut : laisser le système gérer
        // Les écrans peuvent override pour personnaliser
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setupImmersiveMode()

        // BLOC A — Installation du callback moderne OnBackPressed
        // Remplace l'ancien onBackPressed() déprécié depuis API 33
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Si une lecture vocale est en cours sur n'importe quel écran,
                // on la coupe immédiatement avant le retour Android.
                try {
                    SpeakerTtsHelper.stopAll()
                } catch (e: Exception) {
                    Log.w(baseTag, "Back press: stopAll TTS failed", e)
                }

                if (isTaskRoot) {
                    try {
                        SoundManager.release()
                    } catch (e: Exception) {
                        Log.w(baseTag, "Back press: release audio at task root failed", e)
                    }
                }

                // Appel du hook personnalisable
                handleBackPressed()
            }
        })
    }

    // ── GESTION DE LA MUSIQUE EN ARRIÈRE-PLAN ──
    override fun onPause() {
        super.onPause()

        if (!shouldBaseActivityPauseMusicOnPause()) return

        try {
            SoundManager.pauseMusic()
        } catch (e: Exception) {
            Log.w(baseTag, "onPause: pauseMusic failed", e)
        }
    }

    override fun onResume() {
        super.onResume()
        setupImmersiveMode()

        if (!shouldBaseActivityAttemptMusicRecoveryOnResume()) return

        /**
         * BLOC A :
         * On ne fait plus un resumeMusic() brutal du player courant.
         * On tente uniquement de restaurer la musique "désirée" mémorisée,
         * et seulement si aucune BGM n'est déjà en cours.
         *
         * Cela réduit les collisions entre :
         * - l'ancienne Activity qui revient,
         * - la nouvelle Activity qui a déjà réservé sa propre BGM,
         * - les transitions Dashboard / Oracle / Quiz / Résultat.
         */
        try {
            if (!SoundManager.isPlayingMusic()) {
                SoundManager.resumeRememberedMusicDelayed(this, 120L)
            }
        } catch (e: Exception) {
            Log.w(baseTag, "onResume: resumeRememberedMusicDelayed failed", e)
        }
    }

    override fun onDestroy() {
        // Sécurité absolue : on ferme le loader global et sa boucle audio
        // avant de libérer l'activité pour éviter toute fuite visuelle/sonore.
        hideLoading()

        // Arrêt global des lectures TTS pour éviter qu'une voix poursuive
        // sa route après destruction de l'écran courant.
        try {
            SpeakerTtsHelper.stopAll()
        } catch (e: Exception) {
            Log.w(baseTag, "onDestroy: stopAll TTS failed", e)
        }

        super.onDestroy()

        /**
         * On ne libère totalement l'audio global que lorsqu'on quitte
         * réellement la racine de la tâche.
         */
        if (isFinishing && isTaskRoot && !isChangingConfigurations) {
            try {
                SoundManager.release()
            } catch (e: Exception) {
                Log.w(baseTag, "onDestroy: release audio at task root failed", e)
            }
        }
    }

    /**
     * Affiche le LoadingDivineDialog plein écran.
     * Utilisable depuis n'importe quelle Activity héritant de BaseActivity.
     */
    fun showLoading() {
        if (isFinishing || isDestroyed) return
        if (supportFragmentManager.isStateSaved) return

        val existingDialog = supportFragmentManager.findFragmentByTag(loadingDialogTag)
        if (existingDialog is LoadingDivineDialog && existingDialog.isAdded) {
            return
        }

        try {
            LoadingDivineDialog().show(supportFragmentManager, loadingDialogTag)
        } catch (e: Exception) {
            Log.w(baseTag, "showLoading: dialog show failed", e)
        }
    }

    /**
     * Ferme le LoadingDivineDialog et coupe sa boucle sonore secondaire.
     * On stoppe aussi explicitement le scan loop par sécurité si le Fragment
     * a déjà été détruit ou si le FragmentManager a changé d'état.
     */
    fun hideLoading() {
        try {
            val existingDialog = supportFragmentManager.findFragmentByTag(loadingDialogTag)
            if (existingDialog is LoadingDivineDialog) {
                existingDialog.dismissAllowingStateLoss()
            }
        } catch (_: Exception) {
            Log.w(baseTag, "hideLoading: dialog dismiss failed")
        }

        try {
            SoundManager.stopLoopingScan()
        } catch (e: Exception) {
            Log.w(baseTag, "hideLoading: stopLoopingScan failed", e)
        }
    }

    // ── UTILITAIRES ──
    fun setupImmersiveMode() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    fun jouerSfx(resId: Int) = SoundManager.playSFX(this, resId)
    fun stopperMusique() = SoundManager.stopMusic()

    fun declencherVibrationFoudre(duree: Long) {
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        duree,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duree)
            }
        }
    }

    fun createImageUri(): Uri? {
        val imageFile = File(cacheDir, "zeus_capture.jpg")
        return FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
    }
}

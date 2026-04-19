package com.revizeus.app

import android.content.pm.ActivityInfo
import android.os.Bundle

/**
 * Base commune de tout le mode aventure.
 * RéviZeus reste en portrait partout, sauf ici : aventure = paysage intégral.
 */
abstract class BaseAdventureActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setupImmersiveMode()
    }

    override fun onResume() {
        super.onResume()
        setupImmersiveMode()
    }
}

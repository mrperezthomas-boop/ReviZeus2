package com.revizeus.app.ui.anim

import android.animation.ObjectAnimator
import android.app.Activity
import android.media.MediaPlayer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.doOnLayout
import com.revizeus.app.R

object AchievementPopupManager {

    fun showPopup(activity: Activity, badgeName: String) {

        val root = activity.window.decorView as ViewGroup

        val popup = View.inflate(activity, R.layout.view_achievement_popup, null)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )

        params.gravity = Gravity.TOP or Gravity.END
        params.topMargin = 60
        params.marginEnd = 30

        popup.layoutParams = params

        val tv = popup.findViewById<TextView>(R.id.tvName)
        tv.text = badgeName

        root.addView(popup)

        popup.translationX = 600f

        ObjectAnimator.ofFloat(popup, View.TRANSLATION_X, 600f, 0f)
            .setDuration(350)
            .start()

        try {
            val player = MediaPlayer.create(activity, R.raw.sfx_success)
            player.start()
        } catch (_: Exception) {}

        popup.postDelayed({

            ObjectAnimator.ofFloat(popup, View.ALPHA, 1f, 0f)
                .setDuration(300)
                .start()

            popup.postDelayed({
                root.removeView(popup)
            }, 300)

        }, 2500)
    }
}
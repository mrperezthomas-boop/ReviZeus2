package com.revizeus.app.ui.anim

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Path
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

object HudRewardAnimator {

    fun animateBadgeToBook(
        activity: Activity,
        badgeView: View,
        bookView: View
    ) {

        val root = activity.window.decorView as ViewGroup

        val particle = ImageView(activity)
        particle.setImageDrawable((badgeView as ImageView).drawable)

        val size = 80
        particle.layoutParams = ViewGroup.LayoutParams(size, size)

        root.addView(particle)

        val start = IntArray(2)
        val end = IntArray(2)

        badgeView.getLocationOnScreen(start)
        bookView.getLocationOnScreen(end)

        particle.x = start[0].toFloat()
        particle.y = start[1].toFloat()

        val path = Path().apply {
            moveTo(start[0].toFloat(), start[1].toFloat())
            quadTo(
                (start[0] + end[0]) / 2f,
                start[1] - 300f,
                end[0].toFloat(),
                end[1].toFloat()
            )
        }

        val anim = ObjectAnimator.ofFloat(particle, View.X, View.Y, path)
        anim.duration = 900

        val fade = ObjectAnimator.ofFloat(particle, View.ALPHA, 1f, 0f)
        fade.startDelay = 700
        fade.duration = 200

        val set = AnimatorSet()
        set.playTogether(anim, fade)

        set.start()
    }
}
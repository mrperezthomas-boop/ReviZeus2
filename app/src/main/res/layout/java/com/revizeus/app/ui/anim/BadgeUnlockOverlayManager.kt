package com.revizeus.app.ui.anim

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.doOnLayout
import com.revizeus.app.R

object BadgeUnlockOverlayManager {

    fun spawnBadgeBook(activity: Activity): ImageView {

        val root = activity.window.decorView as ViewGroup

        val book = ImageView(activity)
        book.setImageResource(R.drawable.ic_badge_book_popup)

        val size = 120

        val params = FrameLayout.LayoutParams(size, size)
        params.gravity = Gravity.TOP or Gravity.END
        params.topMargin = 120
        params.marginEnd = 40

        book.layoutParams = params
        book.alpha = 0f
        book.scaleX = 0.5f
        book.scaleY = 0.5f

        root.addView(book)

        val fade = ObjectAnimator.ofFloat(book, View.ALPHA, 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(book, View.SCALE_X, 0.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(book, View.SCALE_Y, 0.5f, 1f)

        val set = AnimatorSet()
        set.playTogether(fade, scaleX, scaleY)
        set.duration = 300
        set.start()

        return book
    }

    fun destroyBadgeBook(book: View) {

        val fade = ObjectAnimator.ofFloat(book, View.ALPHA, 1f, 0f)
        fade.duration = 300

        fade.start()
    }
}
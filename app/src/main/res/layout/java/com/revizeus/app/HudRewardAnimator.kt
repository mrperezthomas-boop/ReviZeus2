package com.revizeus.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Path
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView

/**
 * HudRewardAnimator — RéviZeus
 *
 * Utilité :
 * - fait voyager une icône d'une vue source vers une vue cible
 * - fonctionne pour badges, fragments, monnaies et autres ressources
 * - crée une couche temporaire au-dessus de l'UI sans casser l'architecture existante
 */
object HudRewardAnimator {

    fun animateIconToTarget(
        activity: Activity,
        sourceView: View,
        targetView: View,
        drawableRes: Int,
        durationMs: Long = 820L,
        onEnd: (() -> Unit)? = null
    ) {
        val contentRoot = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val overlayParent = contentRoot.getChildAt(0) as? ViewGroup ?: contentRoot

        overlayParent.post {
            val rootLoc = IntArray(2)
            val sourceLoc = IntArray(2)
            val targetLoc = IntArray(2)

            overlayParent.getLocationOnScreen(rootLoc)
            sourceView.getLocationOnScreen(sourceLoc)
            targetView.getLocationOnScreen(targetLoc)

            val sourceCenterX = sourceLoc[0] - rootLoc[0] + sourceView.width / 2f
            val sourceCenterY = sourceLoc[1] - rootLoc[1] + sourceView.height / 2f

            val targetCenterX = targetLoc[0] - rootLoc[0] + targetView.width / 2f
            val targetCenterY = targetLoc[1] - rootLoc[1] + targetView.height / 2f

            val flyer = ImageView(activity).apply {
                setImageResource(drawableRes)
                layoutParams = ViewGroup.LayoutParams(dp(activity, 56), dp(activity, 56))
                x = sourceCenterX - dp(activity, 28)
                y = sourceCenterY - dp(activity, 28)
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
            }

            overlayParent.addView(flyer)

            val controlX = (sourceCenterX + targetCenterX) / 2f + if (targetCenterX >= sourceCenterX) dp(activity, 36) else -dp(activity, 36)
            val controlY = minOf(sourceCenterY, targetCenterY) - dp(activity, 110)

            val path = Path().apply {
                moveTo(flyer.x, flyer.y)
                quadTo(
                    controlX,
                    controlY,
                    targetCenterX - dp(activity, 20),
                    targetCenterY - dp(activity, 20)
                )
            }

            val move = ObjectAnimator.ofFloat(flyer, View.X, View.Y, path)
            val scaleX = ObjectAnimator.ofFloat(flyer, View.SCALE_X, 1f, 0.54f)
            val scaleY = ObjectAnimator.ofFloat(flyer, View.SCALE_Y, 1f, 0.54f)
            val alpha = ObjectAnimator.ofFloat(flyer, View.ALPHA, 1f, 0.92f, 0.12f)

            AnimatorSet().apply {
                duration = durationMs
                interpolator = AccelerateDecelerateInterpolator()
                playTogether(move, scaleX, scaleY, alpha)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        overlayParent.removeView(flyer)
                        pulse(targetView)
                        onEnd?.invoke()
                    }
                })
                start()
            }
        }
    }

    private fun pulse(targetView: View) {
        AnimatorSet().apply {
            duration = 220L
            playTogether(
                ObjectAnimator.ofFloat(targetView, View.SCALE_X, targetView.scaleX, targetView.scaleX * 1.10f, targetView.scaleX),
                ObjectAnimator.ofFloat(targetView, View.SCALE_Y, targetView.scaleY, targetView.scaleY * 1.10f, targetView.scaleY)
            )
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun dp(activity: Activity, value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}

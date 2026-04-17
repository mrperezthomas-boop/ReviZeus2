package com.revizeus.app

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.revizeus.app.models.AvatarItem

/**
 * AvatarAdapter — Carrousel premium.
 *
 * FIX AURA (v3) :
 * LottieProperty.COLOR ne fonctionne que sur les ShapeLayer fills.
 * Pour coloriser TOUT le fichier JSON (y compris ImageLayer, SolidLayer, etc.),
 * il faut LottieProperty.COLOR_FILTER avec PorterDuffColorFilter(color, SRC_ATOP).
 * C'est la seule méthode garantie quelle que soit la structure du JSON Lottie.
 */
class AvatarAdapter(
    private val avatarList: List<AvatarItem>,
    private val onAvatarClicked: ((avatar: AvatarItem, position: Int) -> Unit)? = null
) : RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    init {
        setHasStableIds(true)
    }

    class AvatarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivAvatarImage)
        val auraView: LottieAnimationView? = itemView.findViewById(R.id.ivAvatarAura)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_avatar, parent, false)
        return AvatarViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        val currentItem = avatarList[position]

        // Image principale — inchangé
        holder.imageView.setImageResource(currentItem.imageResId)

        // ─────────────────────────────────────────────────────────────────
        // AURA LOTTIE — FIX : COLOR_FILTER au lieu de COLOR
        //
        // LottieProperty.COLOR ne cible que les fills des ShapeLayer.
        // LottieProperty.COLOR_FILTER avec PorterDuffColorFilter(SRC_ATOP)
        // s'applique à TOUS les types de layers du JSON → couleur garantie visible.
        // ─────────────────────────────────────────────────────────────────
        try {
            holder.auraView?.let { lottie ->
                val colorFilter = PorterDuffColorFilter(
                    currentItem.elementColor,
                    PorterDuff.Mode.SRC_ATOP
                )
                lottie.addValueCallback(
                    KeyPath("**"),
                    LottieProperty.COLOR_FILTER,
                    LottieValueCallback(colorFilter)
                )
                if (!lottie.isAnimating) lottie.playAnimation()
            }
        } catch (e: Exception) {
            Log.w("AvatarAdapter", "Lottie aura : ${e.message}")
        }

        holder.itemView.setOnClickListener {
            onAvatarClicked?.invoke(currentItem, holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int = avatarList.size
    override fun getItemId(position: Int): Long = avatarList[position].id.toLong()
}
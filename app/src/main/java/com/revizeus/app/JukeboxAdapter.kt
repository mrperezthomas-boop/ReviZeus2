package com.revizeus.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.revizeus.app.databinding.ItemJukeboxTrackBinding

/**
 * ============================================================
 * JukeboxAdapter.kt — RéviZeus
 * Adaptateur du Temple des Mélodies
 *
 * Utilité :
 * - Afficher toutes les BGM jouables
 * - Permettre lecture / arrêt visuel d'une piste
 *
 * Connexions :
 * - MusicTrackItem
 * - OlympianMusicCatalog
 * - SettingsActivity
 * - SoundManager
 *
 * CONSERVATION :
 * - Aucun impact sur les lecteurs existants
 * - On réutilise SoundManager comme source unique de lecture BGM
 * ============================================================
 */
class JukeboxAdapter(
    private val tracks: List<MusicTrackItem>,
    private val onPlayClicked: (MusicTrackItem) -> Unit,
    private val onStopClicked: () -> Unit
) : RecyclerView.Adapter<JukeboxAdapter.TrackViewHolder>() {

    /**
     * resId de la piste actuellement sélectionnée dans le jukebox.
     * Mis à jour depuis l'activité hôte.
     */
    private var activeTrackResId: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemJukeboxTrackBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(tracks[position], tracks[position].resId == activeTrackResId)
    }

    override fun getItemCount(): Int = tracks.size

    /**
     * Permet à l'écran hôte de refléter la piste active.
     */
    fun setActiveTrack(resId: Int) {
        activeTrackResId = resId
        notifyDataSetChanged()
    }

    inner class TrackViewHolder(
        private val binding: ItemJukeboxTrackBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MusicTrackItem, isActive: Boolean) {
            binding.tvTrackTitle.text = item.title
            binding.tvTrackDescription.text = item.description

            binding.tvTrackActive.visibility = if (isActive) View.VISIBLE else View.GONE
            binding.btnPlayTrack.text = if (isActive) "▶ REPLAY" else "▶ PLAY"

            binding.btnPlayTrack.setOnClickListener {
                onPlayClicked(item)
            }

            binding.btnStopTrack.setOnClickListener {
                onStopClicked()
            }
        }
    }
}

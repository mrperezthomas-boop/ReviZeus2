package com.revizeus.app

/**
 * ============================================================
 * MusicTrackItem.kt — RéviZeus
 * Modèle simple d'une piste du jukebox
 *
 * Utilité :
 * - Représente une musique jouable dans le Temple des Mélodies
 *
 * Connexions :
 * - OlympianMusicCatalog
 * - JukeboxAdapter
 * - SettingsActivity
 * ============================================================
 */
data class MusicTrackItem(
    val title: String,
    val description: String,
    val resId: Int
)

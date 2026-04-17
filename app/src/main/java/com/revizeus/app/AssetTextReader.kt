package com.revizeus.app

import android.content.Context
import android.util.Log

/**
 * ============================================================
 * AssetTextReader.kt — RéviZeus
 * Lecteur centralisé des fichiers texte placés dans assets/
 *
 * Utilité :
 * - Lire revizeus.txt pour l'écran info
 * - Lire maj.txt pour les parchemins futurs
 *
 * Connexions :
 * - RevizeusInfoActivity
 * - DashboardActivity / overlays futurs
 *
 * Règle de conservation :
 * - Aucun impact destructeur sur l'existant
 * - Helper pur, autonome, réutilisable
 * ============================================================
 */
object AssetTextReader {

    /**
     * Lit un fichier texte depuis assets.
     *
     * @param context Contexte applicatif ou activité
     * @param fileName Nom exact du fichier dans assets/
     * @param fallback Texte de secours si la lecture échoue
     *
     * @return Le contenu texte complet du fichier
     */
    fun readTextAsset(
        context: Context,
        fileName: String,
        fallback: String = ""
    ): String {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("REVIZEUS", "AssetTextReader impossible de lire $fileName : ${e.message}")
            fallback
        }
    }
}

package com.revizeus.app.models

/**
 * Catalogue d'avatars RéviZeus.
 *
 * AJOUT v2 (non destructif) :
 * - elementColor : couleur hexadécimale (Int ARGB) de l'aura Lottie propre à chaque avatar.
 *   Alimenté dans prepareData() d'AvatarActivity.
 *   Utilisé par AvatarAdapter via addValueCallback sur la LottieAnimationView de la carte.
 *
 * Tous les champs existants sont conservés à l'identique.
 */
data class AvatarItem(
    val id: Int,
    val name: String,
    val gender: String,
    val imageResId: Int,
    val backgroundResId: Int,
    val backgroundVideoResId: Int,
    val description: String,
    // Couleur ARGB de l'aura Lottie — défaut or divin si non renseigné
    val elementColor: Int = 0xFFFFD700.toInt(),
    val specialAbilityKey: String = "future_ability_none",
    val specialAbilityLabel: String = "Pouvoir à venir",
    val unlockItemKey: String = "avatar_change_token",
    val isOnboardingSelectable: Boolean = true
)
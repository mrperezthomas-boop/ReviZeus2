package com.revizeus.app

/**
 * OnboardingSession — Transport sécurisé des credentials d'onboarding
 * ══════════════════════════════════════════════════════════════════════
 *
 * RÔLE :
 * Transporte email + mot de passe EN MÉMOIRE entre LoginActivity
 * et AvatarActivity, sans jamais les persister sur disque.
 *
 * POURQUOI :
 * Firebase crée un compte de façon permanente dès qu'on appelle
 * createAccount(). Si l'utilisateur abandonne l'onboarding avant
 * d'avoir choisi son avatar, l'email est bloqué à jamais dans Firebase
 * et le compte fantôme pollue AccountRegistry.
 *
 * SOLUTION :
 * LoginActivity (mode inscription) → valide localement → stocke ici
 * AvatarActivity                   → crée le compte Firebase ICI
 * DashboardActivity                → enregistre dans AccountRegistry ICI
 *
 * RÈGLES :
 * - clear() est appelé dans AvatarActivity après la création Firebase.
 * - Ne jamais persister ces données dans SharedPreferences ou Room.
 * - Ne jamais logger email ou password.
 *
 * DURÉE DE VIE :
 * Singleton Kotlin → vit tant que le process est en mémoire.
 * Si l'app est tuée entre Login et Avatar, l'utilisateur recommence.
 * C'est le comportement attendu et sûr.
 */
object OnboardingSession {

    private var _email: String    = ""
    private var _password: String = ""

    fun store(email: String, password: String) {
        _email    = email.trim()
        _password = password
    }

    val email: String    get() = _email
    val password: String get() = _password

    fun isReady(): Boolean = _email.isNotBlank() && _password.isNotBlank()

    /**
     * Efface les credentials de la mémoire.
     * Appeler dans AvatarActivity après tentative Firebase (succès ou échec).
     */
    fun clear() {
        _email    = ""
        _password = ""
    }
}

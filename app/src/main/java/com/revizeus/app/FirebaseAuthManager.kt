package com.revizeus.app

import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

/**
 * ============================================================
 * FirebaseAuthManager.kt — RéviZeus
 * Gestionnaire central de l'authentification Firebase.
 *
 * OBJECTIF :
 * - Centraliser les appels Firebase Auth
 * - Éviter de dupliquer la logique dans LoginActivity / SplashActivity
 * - Gérer proprement les erreurs sans jamais crasher l'app
 *
 * CORRECTION CRITIQUE :
 * - Ne jamais lire task.result avant d'avoir vérifié task.isSuccessful
 * - En cas d'erreur Firebase, retourner un message exploitable à l'UI
 * ============================================================
 */
object FirebaseAuthManager {

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    /**
     * Retourne l'utilisateur Firebase actuellement connecté, ou null.
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Indique si une session Firebase active existe.
     */
    fun hasActiveSession(): Boolean = auth.currentUser != null

    /**
     * Crée un compte email / mot de passe.
     */
    fun createAccount(
        email: String,
        password: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        onSuccess(user)
                    } else {
                        onError("Le compte a été créé, mais aucun utilisateur n'a été retourné.")
                    }
                } else {
                    val exception = task.exception
                    Log.e("ReviZeusAuth", "Erreur createAccount", exception)

                    val rawMessage = exception?.localizedMessage ?: ""
                    val safeMessage = when {
                        rawMessage.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                            "Configuration Firebase introuvable. Vérifie Authentication > Email/Password, puis retélécharge google-services.json."

                        rawMessage.contains("already in use", ignoreCase = true) ->
                            "Cet email est déjà lié à un compte."

                        rawMessage.contains("badly formatted", ignoreCase = true) ->
                            "L'adresse email est invalide."

                        rawMessage.contains("password is invalid", ignoreCase = true) ->
                            "Le mot de passe est invalide."

                        rawMessage.isNotBlank() ->
                            rawMessage

                        else ->
                            "Impossible de forger ton compte divin pour l'instant."
                    }

                    onError(safeMessage)
                }
            }
    }

    /**
     * Connecte un utilisateur existant.
     */
    fun signIn(
        email: String,
        password: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        onSuccess(user)
                    } else {
                        onError("Connexion réussie, mais aucun utilisateur n'a été retourné.")
                    }
                } else {
                    val exception = task.exception
                    Log.e("ReviZeusAuth", "Erreur signIn", exception)

                    val rawMessage = exception?.localizedMessage ?: ""
                    val safeMessage = when {
                        rawMessage.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                            "Configuration Firebase introuvable. Vérifie Authentication > Email/Password, puis retélécharge google-services.json."

                        rawMessage.contains("password is invalid", ignoreCase = true) ||
                        rawMessage.contains("supplied auth credential is incorrect", ignoreCase = true) ->
                            "Mot de passe incorrect."

                        rawMessage.contains("invalid login credentials", ignoreCase = true) ||
                        rawMessage.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                        rawMessage.contains("invalid-credential", ignoreCase = true) ->
                            "Identifiants invalides. Vérifie ton email et ton mot de passe."

                        rawMessage.contains("There is no user record", ignoreCase = true) ||
                        rawMessage.contains("no user record", ignoreCase = true) ||
                        rawMessage.contains("user not found", ignoreCase = true) ->
                            "Aucun compte trouvé pour cet email."

                        rawMessage.isNotBlank() ->
                            rawMessage

                        else ->
                            "L'Olympe refuse ton accès pour le moment."
                    }

                    onError(safeMessage)
                }
            }
    }


/**
 * Connecte ou crée un compte via Google Sign-In.
 * Cette méthode n'altère pas la logique UID + slots :
 * elle ne fait que fournir un FirebaseUser valide.
 */
fun signInWithGoogle(
    idToken: String,
    onSuccess: (FirebaseUser) -> Unit,
    onError: (String) -> Unit
) {
    val credential = GoogleAuthProvider.getCredential(idToken, null)

    auth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = task.result?.user
                if (user != null) {
                    onSuccess(user)
                } else {
                    onError("Connexion Google réussie, mais aucun utilisateur n'a été retourné.")
                }
            } else {
                val exception = task.exception
                Log.e("ReviZeusAuth", "Erreur signInWithGoogle", exception)

                val rawMessage = exception?.localizedMessage ?: ""
                val safeMessage = when {
                    rawMessage.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                        "Configuration Firebase introuvable. Vérifie Authentication > Google, puis retélécharge google-services.json."

                    rawMessage.isNotBlank() ->
                        rawMessage

                    else ->
                        "Le portail Google de l'Olympe ne répond pas pour l'instant."
                }

                onError(safeMessage)
            }
        }
}

/**
 * Envoie un email de réinitialisation du mot de passe.
 */
    fun sendPasswordReset(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            auth.setLanguageCode("fr")
        } catch (_: Exception) {
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    val exception = task.exception
                    Log.e("ReviZeusAuth", "Erreur sendPasswordReset", exception)

                    val rawMessage = exception?.localizedMessage ?: ""
                    val safeMessage = when {
                        rawMessage.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                            "Configuration Firebase introuvable. Vérifie Authentication > Email/Password, puis retélécharge google-services.json."

                        rawMessage.isNotBlank() ->
                            rawMessage

                        else ->
                            "Les Oracles n'ont pas pu envoyer le parchemin de récupération."
                    }

                    onError(safeMessage)
                }
            }
    }

    /**
     * Déconnecte la session Firebase courante.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Supprime réellement le compte Firebase après ré-authentification.
     * Firebase exige souvent une authentification récente pour autoriser delete().
     */
    fun reauthenticateAndDeleteAccount(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onError("Aucun compte Firebase actif à supprimer.")
            return
        }

        val safeEmail = email.trim().ifBlank { user.email.orEmpty() }
        if (safeEmail.isBlank()) {
            onError("Adresse email introuvable pour confirmer la suppression.")
            return
        }

        val credential = EmailAuthProvider.getCredential(safeEmail, password)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (!reauthTask.isSuccessful) {
                    val rawMessage = reauthTask.exception?.localizedMessage.orEmpty()
                    val safeMessage = when {
                        rawMessage.contains("password is invalid", ignoreCase = true) ->
                            "Mot de passe incorrect."
                        rawMessage.contains("The supplied auth credential is incorrect", ignoreCase = true) ->
                            "Mot de passe incorrect."
                        rawMessage.isNotBlank() -> rawMessage
                        else -> "Ré-authentification refusée. Vérifie ton mot de passe."
                    }
                    onError(safeMessage)
                    return@addOnCompleteListener
                }

                user.delete()
                    .addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            auth.signOut()
                            onSuccess()
                        } else {
                            val rawDeleteMessage = deleteTask.exception?.localizedMessage.orEmpty()
                            onError(
                                if (rawDeleteMessage.isNotBlank()) rawDeleteMessage
                                else "La suppression Firebase a échoué."
                            )
                        }
                    }
            }
    }
}
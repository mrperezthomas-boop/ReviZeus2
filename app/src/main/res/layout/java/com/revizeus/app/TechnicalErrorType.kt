package com.revizeus.app

/**
 * ═══════════════════════════════════════════════════════════════
 * TECHNICAL ERROR TYPE — Types d'erreurs techniques
 * ═══════════════════════════════════════════════════════════════
 * 
 * BLOC B — DIALOGUES RPG UNIVERSELS
 * 
 * Utilité :
 * - Énumère les types d'erreurs techniques courantes dans RéviZeus.
 * - Utilisée par DialogRPGManager.showTechnicalError() pour convertir
 *   automatiquement une erreur technique brute en message diégétique
 *   incarné par le dieu approprié.
 * 
 * Mapping erreur → dieu + message diégétique :
 * 
 * NETWORK_ERROR → Hermès
 * - Erreur : "Network error", "Connection timeout", "No internet"
 * - Message : "Hermès n'arrive pas à livrer ton message. Vérifie que
 *   l'Olympe est bien connecté à ta connexion divine (WiFi ou 4G)."
 * 
 * CAMERA_PERMISSION → Zeus
 * - Erreur : "Camera permission denied"
 * - Message : "Zeus demande la permission d'utiliser l'Œil Divin.
 *   Accepte dans les paramètres d'Android pour invoquer l'Oracle."
 * 
 * STORAGE_PERMISSION → Athéna
 * - Erreur : "Storage permission denied"
 * - Message : "Athéna a besoin d'accéder à tes parchemins sauvegardés.
 *   Accepte dans les paramètres d'Android."
 * 
 * API_TIMEOUT → Apollon
 * - Erreur : "API timeout", "Request timeout"
 * - Message : "La connexion avec l'Oracle prend trop de temps. Apollon
 *   te conseille de réessayer dans un instant."
 * 
 * INVALID_INPUT → Athéna
 * - Erreur : "Invalid input", "Validation error"
 * - Message : "Athéna a détecté une erreur dans ta saisie. Vérifie que
 *   toutes les informations sont correctes."
 * 
 * SERVER_UNAVAILABLE → Zeus
 * - Erreur : "Server unavailable", "503 Service Unavailable"
 * - Message : "Les serveurs de l'Olympe sont en maintenance. Zeus te
 *   demande de réessayer dans quelques instants."
 * 
 * MICROPHONE_PERMISSION → Apollon
 * - Erreur : "Microphone permission denied"
 * - Message : "Apollon souhaite entendre ta voix divine. Accepte dans
 *   les paramètres d'Android."
 * 
 * GEMINI_API_ERROR → Zeus
 * - Erreur : "Gemini API error", "AI generation failed"
 * - Message : "L'Oracle a rencontré une difficulté divine. Zeus te
 *   demande de réessayer."
 * 
 * FILE_NOT_FOUND → Athéna
 * - Erreur : "File not found", "Missing resource"
 * - Message : "Athéna ne trouve pas le parchemin demandé. Vérifie
 *   qu'il existe toujours."
 * 
 * QUOTA_EXCEEDED → (dieu selon le service)
 * - Note : Ce n'est pas une erreur technique pure, mais une limite
 *   fonctionnelle. Utiliser DialogRPGManager.showDivineFatigue() à la place.
 * 
 * Évolutions futures :
 * - DATABASE_ERROR → Athéna (erreur Room)
 * - PARSING_ERROR → Hermès (erreur de décodage)
 * - ENCRYPTION_ERROR → Hadès (erreur de chiffrement)
 * - AUTHENTICATION_ERROR → Zeus (erreur d'authentification Firebase)
 * 
 * ═══════════════════════════════════════════════════════════════
 */
enum class TechnicalErrorType {
    /**
     * Erreur réseau / connexion.
     * Exemples : "Network error", "Connection timeout", "No internet"
     * Dieu : Hermès
     * Message : "Hermès n'arrive pas à livrer ton message. Vérifie ta connexion divine."
     */
    NETWORK_ERROR,
    
    /**
     * Permission caméra refusée.
     * Exemple : "Camera permission denied"
     * Dieu : Zeus
     * Message : "Zeus demande la permission d'utiliser l'Œil Divin. Accepte dans les paramètres."
     */
    CAMERA_PERMISSION,
    
    /**
     * Permission stockage refusée.
     * Exemple : "Storage permission denied"
     * Dieu : Athéna
     * Message : "Athéna a besoin d'accéder à tes parchemins sauvegardés. Accepte dans les paramètres."
     */
    STORAGE_PERMISSION,
    
    /**
     * Timeout API / requête trop longue.
     * Exemples : "API timeout", "Request timeout"
     * Dieu : Apollon
     * Message : "La connexion avec l'Oracle prend trop de temps. Apollon te conseille de réessayer."
     */
    API_TIMEOUT,
    
    /**
     * Saisie invalide / erreur de validation.
     * Exemples : "Invalid input", "Validation error"
     * Dieu : Athéna
     * Message : "Athéna a détecté une erreur dans ta saisie. Vérifie que tout est correct."
     */
    INVALID_INPUT,
    
    /**
     * Serveur indisponible / maintenance.
     * Exemples : "Server unavailable", "503 Service Unavailable"
     * Dieu : Zeus
     * Message : "Les serveurs de l'Olympe sont en maintenance. Zeus te demande de réessayer plus tard."
     */
    SERVER_UNAVAILABLE,
    
    /**
     * Permission microphone refusée.
     * Exemple : "Microphone permission denied"
     * Dieu : Apollon
     * Message : "Apollon souhaite entendre ta voix divine. Accepte dans les paramètres."
     */
    MICROPHONE_PERMISSION,
    
    /**
     * Erreur API Gemini / génération IA.
     * Exemples : "Gemini API error", "AI generation failed"
     * Dieu : Zeus
     * Message : "L'Oracle a rencontré une difficulté divine. Zeus te demande de réessayer."
     */
    GEMINI_API_ERROR,
    
    /**
     * Fichier introuvable / ressource manquante.
     * Exemples : "File not found", "Missing resource"
     * Dieu : Athéna
     * Message : "Athéna ne trouve pas le parchemin demandé. Vérifie qu'il existe toujours."
     */
    FILE_NOT_FOUND;
    
    /**
     * Retourne l'ID du dieu associé à ce type d'erreur.
     */
    fun getGodId(): String {
        return when (this) {
            NETWORK_ERROR -> "hermes"
            CAMERA_PERMISSION -> "zeus"
            STORAGE_PERMISSION -> "athena"
            API_TIMEOUT -> "apollo"
            INVALID_INPUT -> "athena"
            SERVER_UNAVAILABLE -> "zeus"
            MICROPHONE_PERMISSION -> "apollo"
            GEMINI_API_ERROR -> "zeus"
            FILE_NOT_FOUND -> "athena"
        }
    }
    
    /**
     * Retourne le message diégétique complet associé à cette erreur.
     */
    fun getDiegeticMessage(): String {
        return when (this) {
            NETWORK_ERROR -> 
                "Hermès n'arrive pas à livrer ton message. Vérifie que l'Olympe est bien connecté à ta connexion divine (WiFi ou 4G)."
            
            CAMERA_PERMISSION -> 
                "Zeus demande la permission d'utiliser l'Œil Divin. Accepte dans les paramètres d'Android pour invoquer l'Oracle."
            
            STORAGE_PERMISSION -> 
                "Athéna a besoin d'accéder à tes parchemins sauvegardés. Accepte dans les paramètres d'Android."
            
            API_TIMEOUT -> 
                "La connexion avec l'Oracle prend trop de temps. Apollon te conseille de réessayer dans un instant."
            
            INVALID_INPUT -> 
                "Athéna a détecté une erreur dans ta saisie. Vérifie que toutes les informations sont correctes."
            
            SERVER_UNAVAILABLE -> 
                "Les serveurs de l'Olympe sont en maintenance. Zeus te demande de réessayer dans quelques instants."
            
            MICROPHONE_PERMISSION -> 
                "Apollon souhaite entendre ta voix divine. Accepte dans les paramètres d'Android."
            
            GEMINI_API_ERROR -> 
                "L'Oracle a rencontré une difficulté divine. Zeus te demande de réessayer."
            
            FILE_NOT_FOUND -> 
                "Athéna ne trouve pas le parchemin demandé. Vérifie qu'il existe toujours."
        }
    }
}

package com.revizeus.app.core

import android.content.Context
import android.util.Log
import com.revizeus.app.PantheonConfig
import com.revizeus.app.models.InsightType
import com.revizeus.app.models.UserInsight

/**
 * ═══════════════════════════════════════════════════════════════
 * GOD TRIGGER ENGINE — Système de déclenchement divin contextuel
 * ═══════════════════════════════════════════════════════════════
 * 
 * BLOC 4B — SYSTÈME DIVIN INTELLIGENT
 * 
 * Rôle :
 * Décide quel dieu doit apparaître selon les insights détectés
 * et l'état du joueur. Chaque dieu a un rôle spécifique et pop
 * dans des situations précises.
 * 
 * Déclenchements divins :
 * - Zeus : Performance équilibrée, pas d'insight critique
 * - Athéna : Progression détectée (stratégie qui fonctionne)
 * - Poséidon : Instabilité émotionnelle/cognitive
 * - Arès : Excellence (3×95% déjà existant)
 * - Aphrodite : Fatigue détectée (besoin pause)
 * - Hermès : Précipitation détectée
 * - Déméter : Savoir non travaillé (déjà existant)
 * - Héphaïstos : Confusion/erreurs répétées
 * - Apollon : Maîtrise confirmée
 * - Prométhée : Multiple faiblesses + haute performance globale
 * 
 * Principe CORE :
 * - Dialogues générés par Gemini selon contexte
 * - Pas de templates fixes
 * - Personnalité du dieu + insights + profil utilisateur
 * 
 * ═══════════════════════════════════════════════════════════════
 */
object GodTriggerEngine {
    
    private const val TAG = "GOD_TRIGGER_ENGINE"
    
    /**
     * Résultat de l'analyse de déclenchement.
     */
    data class GodTrigger(
        val godName: String,
        val reason: String,
        val priority: Int,          // 1-10 (10 = priorité max)
        val insights: List<UserInsight>,
        val contextNote: String     // Contexte pour Gemini
    )
    
    /**
     * Analyse les insights et détermine quel dieu doit apparaître.
     * 
     * @param context Context Android
     * @param insights Insights détectés par UserAnalyticsEngine
     * @param subject Matière du quiz
     * @param scorePercent Score obtenu au quiz (0-100)
     * @param isArèsStreak Si true, Arès est déjà déclenché (priorité absolue)
     * @return GodTrigger ou null si aucun déclenchement spécial
     */
    suspend fun analyzeAndSelectGod(
        context: Context,
        insights: List<UserInsight>,
        subject: String,
        scorePercent: Int,
        isArèsStreak: Boolean = false
    ): GodTrigger? {
        try {
            Log.d(TAG, "Analyse déclenchement divin pour $subject (score: $scorePercent%)")
            Log.d(TAG, "${insights.size} insights détectés")
            
            // Priorité 1 : Arès (déjà déclenché par streak 3×95%)
            if (isArèsStreak) {
                Log.d(TAG, "Arès déclenché (priorité absolue - streak)")
                return GodTrigger(
                    godName = "Arès",
                    reason = "Streak 3×95% - Défi du dieu de la guerre",
                    priority = 10,
                    insights = emptyList(),
                    contextNote = "Défi d'Arès suite à 3 performances parfaites consécutives"
                )
            }
            
            // Priorité 2 : Aphrodite (Fatigue détectée)
            val needBreak = insights.find { it.type == InsightType.NEED_BREAK }
            if (needBreak != null && needBreak.isAlert()) {
                Log.d(TAG, "Aphrodite déclenchée (fatigue détectée)")
                return GodTrigger(
                    godName = "Aphrodite",
                    reason = "Fatigue cognitive - Besoin de pause",
                    priority = 9,
                    insights = listOf(needBreak),
                    contextNote = buildContextNote("Aphrodite", listOf(needBreak), scorePercent)
                )
            }
            
            // Priorité 3 : Hermès (Précipitation)
            val rushing = insights.find { it.type == InsightType.RUSHING }
            if (rushing != null && rushing.isActionRequired()) {
                Log.d(TAG, "Hermès déclenché (précipitation)")
                return GodTrigger(
                    godName = "Hermès",
                    reason = "Précipitation - Réponses trop rapides",
                    priority = 8,
                    insights = listOf(rushing),
                    contextNote = buildContextNote("Hermès", listOf(rushing), scorePercent)
                )
            }
            
            // Priorité 4 : Héphaïstos (Confusion/Erreurs répétées)
            val confusion = insights.find { it.type == InsightType.CONFUSION }
            if (confusion != null && confusion.isAlert()) {
                Log.d(TAG, "Héphaïstos déclenché (confusion détectée)")
                return GodTrigger(
                    godName = "Héphaïstos",
                    reason = "Confusion - Erreurs répétées sur mêmes concepts",
                    priority = 7,
                    insights = listOf(confusion),
                    contextNote = buildContextNote("Héphaïstos", listOf(confusion), scorePercent)
                )
            }
            
            // Priorité 5 : Poséidon (Instabilité)
            val instability = insights.find { it.type == InsightType.INSTABILITY }
            if (instability != null && instability.isAlert()) {
                Log.d(TAG, "Poséidon déclenché (instabilité)")
                return GodTrigger(
                    godName = "Poséidon",
                    reason = "Instabilité - Résultats très variables",
                    priority = 6,
                    insights = listOf(instability),
                    contextNote = buildContextNote("Poséidon", listOf(instability), scorePercent)
                )
            }
            
            // Priorité 6 : Apollon (Maîtrise confirmée)
            val mastery = insights.find { it.type == InsightType.MASTERY }
            if (mastery != null && scorePercent >= 90) {
                Log.d(TAG, "Apollon déclenché (maîtrise)")
                return GodTrigger(
                    godName = "Apollon",
                    reason = "Maîtrise confirmée - Excellence atteinte",
                    priority = 5,
                    insights = listOf(mastery),
                    contextNote = buildContextNote("Apollon", listOf(mastery), scorePercent)
                )
            }
            
            // Priorité 7 : Athéna (Progression significative)
            val progress = insights.find { it.type == InsightType.PROGRESS }
            if (progress != null && scorePercent >= 70) {
                Log.d(TAG, "Athéna déclenchée (progression)")
                return GodTrigger(
                    godName = "Athéna",
                    reason = "Progression significative - Stratégie efficace",
                    priority = 4,
                    insights = listOf(progress),
                    contextNote = buildContextNote("Athéna", listOf(progress), scorePercent)
                )
            }
            
            // Priorité 8 : Prométhée (Multiple faiblesses + bonne volonté)
            val weaknesses = insights.filter { it.type == InsightType.WEAKNESS }
            if (weaknesses.size >= 2 && scorePercent >= 60) {
                Log.d(TAG, "Prométhée déclenché (défis multiples)")
                return GodTrigger(
                    godName = "Prométhée",
                    reason = "Défis multiples - Nouvelle approche suggérée",
                    priority = 3,
                    insights = weaknesses,
                    contextNote = buildContextNote("Prométhée", weaknesses, scorePercent)
                )
            }
            
            // Par défaut : Dieu de la matière (ou Zeus si pas de match)
            val god = PantheonConfig.findByMatiere(subject)
            val defaultGod = god?.divinite ?: "Zeus"
            
            Log.d(TAG, "Aucun déclenchement spécial, dieu par défaut: $defaultGod")
            return GodTrigger(
                godName = defaultGod,
                reason = "Dieu de la matière - Performance standard",
                priority = 1,
                insights = insights.filter { it.isInformational() },
                contextNote = buildContextNote(defaultGod, insights, scorePercent)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur analyse déclenchement divin", e)
            return null
        }
    }
    
    /**
     * Construit une note de contexte pour Gemini incluant tous les insights.
     */
    private fun buildContextNote(
        godName: String,
        insights: List<UserInsight>,
        scorePercent: Int
    ): String {
        val insightsSummary = if (insights.isEmpty()) {
            "Aucun insight particulier détecté"
        } else {
            insights.joinToString("\n") { insight ->
                "- ${insight.type.name}: ${insight.evidence} (sévérité: ${insight.severity})"
            }
        }
        
        return """
        CONTEXTE DIVIN DÉTECTÉ :
        Dieu sélectionné : $godName
        Score du quiz : $scorePercent%
        
        Insights détectés :
        $insightsSummary
        
        CONSIGNE POUR DIALOGUE :
        - Parle avec la personnalité de $godName
        - Intègre les insights dans ton message de façon naturelle
        - Sois encourageant mais honnête
        - Propose une action concrète si pertinent
        """.trimIndent()
    }
    
    /**
     * Génère un prompt Gemini enrichi pour le dialogue du dieu.
     * Ce prompt sera passé à GeminiManager pour génération contextuelle.
     */
    fun buildGodDialoguePrompt(
        trigger: GodTrigger,
        subject: String,
        userProfile: com.revizeus.app.models.UserProfile,
        scorePercent: Int
    ): String {
        val god = PantheonConfig.findByDivinite(trigger.godName)
        val personnalite = god?.personnalite ?: "Sage et bienveillant"
        val ethos = god?.ethos ?: "Équilibre"
        
        return """
        Tu es ${trigger.godName}, dieu/déesse de la mythologie grecque.
        
        PERSONNALITÉ :
        $personnalite
        
        ÉTHOS PÉDAGOGIQUE :
        $ethos
        
        SITUATION ACTUELLE :
        - Matière : $subject
        - Score du joueur : $scorePercent%
        - Âge du joueur : ${userProfile.age} ans
        - Classe : ${userProfile.classLevel}
        - Humeur : ${userProfile.mood}
        
        ${trigger.contextNote}
        
        RAISON DE TON APPARITION :
        ${trigger.reason}
        
        CONSIGNES :
        - Parle avec ta personnalité unique de ${trigger.godName}
        - Réagis aux insights détectés de façon naturelle
        - Sois bref (2-3 phrases max)
        - Termine par une action ou encouragement concret
        - Utilise un ton adapté à l'âge du joueur
        - Intègre des références mythologiques si pertinent
        
        IMPORTANT :
        - NE répète PAS les chiffres bruts des insights
        - Transforme-les en message inspirant et actionnable
        - Parle comme un dieu, pas comme un prof
        
        Génère UNIQUEMENT le dialogue de ${trigger.godName}, sans préambule.
        """.trimIndent()
    }
}

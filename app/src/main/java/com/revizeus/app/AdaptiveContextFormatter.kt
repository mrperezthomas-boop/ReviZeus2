package com.revizeus.app

/**
 * Transforme le snapshot joueur + la personnalité du dieu en bloc texte injecté dans
 * GeminiManager.adaptiveContextNote.
 *
 * Le format est volontairement textuel pour respecter ton architecture actuelle :
 * ton GeminiManager sait déjà exploiter adaptiveContextNote sans qu'on ait besoin
 * de casser ses signatures ni ses prompts de base.
 */
object AdaptiveContextFormatter {

    fun buildAdaptiveContextNote(
        snapshot: PlayerAdaptiveSnapshot,
        godId: String,
        dialogCategory: DialogCategory,
        triggerLabel: String,
        explicitGoal: String,
        extraInstructions: String? = null
    ): String {
        val personality = GodPersonalityEngine.get(godId)

        val templeBlock = if (snapshot.templeProgressByGod.isEmpty()) {
            "- progression temples : non fournie pour cette scène"
        } else {
            buildString {
                appendLine("- progression temples :")
                snapshot.templeProgressByGod.entries.sortedBy { it.key }.forEach { entry ->
                    appendLine("  • ${entry.key} = niveau ${entry.value}/10")
                }
            }.trimEnd()
        }

        val equipmentBlock = buildString {
            appendLine("- objets équipés : ${snapshot.equippedItems.joinToString(", ").ifBlank { "aucun" }}")
            appendLine("- artefacts équipés : ${snapshot.equippedArtifacts.joinToString(", ").ifBlank { "aucun" }}")
        }.trimEnd()

        val weaknessesBlock = buildString {
            appendLine("- faiblesses récentes : ${snapshot.recentErrorSubjects.joinToString(", ").ifBlank { "non détectées" }}")
            appendLine("- sujets faibles : ${snapshot.weakTopics.joinToString(", ").ifBlank { "non détectés" }}")
            appendLine("- sujets forts : ${snapshot.strongTopics.joinToString(", ").ifBlank { "non détectés" }}")
        }.trimEnd()

        val futureParamsBlock = if (snapshot.futureParams.isEmpty()) {
            "- paramètres futurs : aucun"
        } else {
            buildString {
                appendLine("- paramètres futurs :")
                snapshot.futureParams.toSortedMap().forEach { (key, value) ->
                    appendLine("  • $key = $value")
                }
            }.trimEnd()
        }

        return """
            CONTEXTE JOUEUR EXTENSIBLE — RÉVIZEUS
            - pseudo : ${snapshot.pseudo}
            - âge : ${snapshot.age}
            - classe : ${snapshot.classLevel}
            - humeur : ${snapshot.mood}
            - niveau : ${snapshot.level}
            - rang : ${snapshot.rank}
            - titre équipé : ${snapshot.titleEquipped}
            - pattern cognitif : ${snapshot.cognitivePattern}
            - précision logique : ${formatFloat(snapshot.logicalPrecisionScore)}
            - fréquence des erreurs récurrentes : ${formatFloat(snapshot.errorPatternFrequency)}
            - indice de fatigue : ${formatFloat(snapshot.fatigueIndex)}
            - susceptibilité aux biais : ${formatFloat(snapshot.biasSusceptibility)}
            - taux de décroissance mémorielle : ${formatFloat(snapshot.retentionDecayRate)}
            - coefficient d'adaptabilité : ${formatFloat(snapshot.adaptabilityCoefficient)}
            - quiz totaux : ${snapshot.totalQuizDone}
            - série de victoires : ${snapshot.winStreak}
            - série quotidienne : ${snapshot.dayStreak}
            - XP totale gagnée : ${snapshot.totalXpEarned}
            - éclats de savoir : ${snapshot.eclatsSavoir}
            - ambroisie : ${snapshot.ambroisie}
            - matière courante : ${snapshot.currentSubject ?: "non précisée"}
            - topic courant : ${snapshot.currentTopic ?: "non précisé"}
            - cours courant : ${snapshot.currentCourseTitle ?: "non précisé"}
            - question courante : ${snapshot.currentQuestionText ?: "non précisée"}
            - score récent : ${snapshot.latestScorePercent?.let { "$it%" } ?: "non fourni"}
            - étoiles récentes : ${snapshot.latestStars ?: "non fournies"}
            - taux de réussite récent : ${snapshot.recentSuccessRate?.let { "${formatFloat(it)}" } ?: "non calculé"}
            - temps de réponse moyen récent : ${snapshot.recentAverageResponseTimeMs?.let { "$it ms" } ?: "non calculé"}
            - fragments matière courante : ${snapshot.currentSubjectFragmentCount ?: "non calculé"}
            - issue explicite de la scène : ${snapshot.explicitOutcome ?: "non précisée"}
            - étape aventure : ${snapshot.adventureStep ?: "non fournie"}

            $templeBlock

            $equipmentBlock

            $weaknessesBlock

            AFFINITÉS DIVINES
            - synthèse globale : ${snapshot.divineAffinitySummary}
            - affinité matière courante : ${snapshot.currentGodAffinitySummary}

            DIRECTIVES D’AFFINITÉ
            - utiliser cette affinité comme nuance relationnelle subtile ;
            - ne jamais flatter gratuitement ;
            - ne jamais afficher les chiffres, seuils ou données brutes au joueur ;
            - si l’affinité est faible ou inconnue, rester neutre et pédagogique ;
            - si l’affinité est élevée, le dieu peut reconnaître davantage les efforts du héros ;
            - la priorité reste la clarté pédagogique, pas le fan-service.

            PERSONNALITÉ DIVINE CIBLE
            - dieu : ${personality.displayName}
            - identité de ton : ${personality.toneIdentity}
            - style pédagogique : ${personality.pedagogyStyle}
            - style d'humour : ${personality.humorStyle}
            - niveau d'autorité : ${personality.authorityLevel}/10
            - rythme de phrase : ${personality.preferredSentenceRhythm}
            - niveau de vocabulaire : ${personality.vocabularyLevel}
            - style émotionnel : ${personality.emotionalStyle}
            - style de correction : ${personality.correctionStyle}
            - style de défi : ${personality.challengeStyle}
            - style de récompense : ${personality.rewardStyle}
            - style de fatigue divine : ${personality.fatigueStyle}
            - dérives interdites : ${personality.forbiddenDrifts.joinToString(", ")}

            CADRE DE LA SCÈNE
            - catégorie de dialogue : ${dialogCategory.name}
            - déclencheur : $triggerLabel
            - objectif explicite : $explicitGoal

            DIRECTIVES CRITIQUES POUR LA GÉNÉRATION
            - génère un vrai texte contextuel, pas une phrase générique interchangeable ;
            - exploite l'âge, la classe, l'humeur, la performance, la fatigue et la progression si cela aide réellement ;
            - n'invente pas de données absentes ;
            - reste utilisable dans un dialogue RPG mobile ;
            - si la scène est mineure, reste bref ;
            - si la scène est pédagogique, sois utile avant d'être spectaculaire ;
            - conserve l'identité du dieu sans caricature ;
            - la réponse doit pouvoir être affichée lettre par lettre ;
            - le champ mnemo doit être vraiment utile ;
            - suggestedAction doit proposer une action concrète pour le joueur.

            $futureParamsBlock

            INSTRUCTIONS ADDITIONNELLES
            ${extraInstructions?.ifBlank { "- aucune" } ?: "- aucune"}
        """.trimIndent()
    }

    private fun formatFloat(value: Float): String {
        return String.format(java.util.Locale.US, "%.2f", value)
    }
}

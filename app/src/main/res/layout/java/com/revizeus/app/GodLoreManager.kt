package com.revizeus.app

import com.revizeus.app.models.QuizQuestion
import com.revizeus.app.models.UserProfile

/**
 * ═══════════════════════════════════════════════════════════════
 * GOD LORE MANAGER — RéviZeus
 * ═══════════════════════════════════════════════════════════════
 * Rôle :
 * Génère des dialogues contextuels pour les dieux en combinant :
 *   - la matière
 *   - la personnalité du dieu
 *   - le contexte (accueil, résultat, ultime, etc.)
 *   - la progression du joueur dans cette matière
 *
 * OBJECTIF :
 * Commencer à lier le lore du Panthéon aux cours réellement ajoutés
 * par l'utilisateur, sans casser l'architecture actuelle.
 *
 * NOTES :
 * - Cette couche n'écrase pas GodManager : elle s'appuie sur lui.
 * - Plus tard, on pourra enrichir ça avec :
 *   - concepts faibles
 *   - anecdotes générées
 *   - rivalités/interactions entre dieux
 *   - humeur plus fine
 *
 * REFACTO PROPRE — LYRE / DIALOGUES / FALLBACKS :
 * - GodLoreManager devient la façade métier unique des contenus divins.
 * - Les Activities n'ont plus à connaître les prompts détaillés.
 * - Les fallbacks immersifs sont centralisés ici.
 * - La Lyre d'Apollon passe désormais proprement par GeminiManager.generateDialog().
 * - Les erreurs techniques peuvent être transformées en réponses diégétiques cohérentes.
 * ═══════════════════════════════════════════════════════════════
 */
object GodLoreManager {

    /**
     * CHANTIER 1.1 & 1.2
     * Réponse de secours si l'IA échoue pour ne jamais laisser l'UI vide.
     */
    private fun buildFallbackGodResponse(
        text: String,
        matiere: String,
        godName: String,
        tone: String,
        suggestedAction: String,
        mnemo: String
    ): GeminiManager.GodResponse {
        return GeminiManager.GodResponse(
            text = text,
            mnemo = mnemo,
            tone = tone,
            godName = godName,
            matiere = matiere,
            suggestedAction = suggestedAction
        )
    }

    /**
     * REFACTO PROPRE — FALLBACK IMMERSIF CENTRALISÉ
     *
     * Cette méthode prépare la gestion future des quotas / indisponibilités
     * sous forme diégétique. Même si, pour l'instant, nous ne détectons pas
     * encore finement les erreurs de quota Gemini, toute feature divine peut
     * déjà retomber sur une réponse cohérente du dieu concerné.
     *
     * ÉVOLUTION FUTURE :
     * - analyser le message d'erreur réel Gemini / proxy
     * - différencier quota, timeout, réseau, erreur parsing
     * - injecter un délai estimé de repos du dieu si disponible
     */
    private fun buildDivineUnavailableFallback(
        godName: String,
        matiere: String,
        featureLabel: String
    ): GeminiManager.GodResponse {
        val upperGod = godName.uppercase()

        val text = when (upperGod) {
            "APOLLON" -> "Ma lyre se repose un instant, mortel. Les cordes divines vibrent encore, mais elles ont besoin d'un souffle avant de chanter de nouveau ce savoir."
            "APHRODITE" -> "La grâce divine hésite encore à prendre forme. La vision n'est pas perdue, elle attend simplement son heure."
            "HERMÈS" -> "Mes sandales ont traversé trop de mondes d'un coup. Laisse-moi reprendre haleine avant de faire voyager ce savoir à nouveau."
            "PROMÉTHÉE" -> "Le feu du conseil ne s'est pas éteint, mais il baisse un instant. Attise-le de nouveau dans quelques instants."
            "HÉPHAÏSTOS" -> "La forge divine chauffe encore, mais le métal réclame une pause avant la prochaine création."
            else -> "Le pouvoir divin vacille un instant. Le Panthéon n'abandonne pas ce savoir : il reprend simplement son souffle."
        }

        val mnemo = when (upperGod) {
            "APOLLON" -> "Le vrai chant revient toujours."
            "APHRODITE" -> "La forme naît dans la patience."
            "HERMÈS" -> "Chaque message trouve sa route."
            "PROMÉTHÉE" -> "Le feu revient après la braise."
            "HÉPHAÏSTOS" -> "Le métal mûrit dans l'attente."
            else -> "Patience, puis reprise."
        }

        val suggestedAction = when (upperGod) {
            "APOLLON" -> "Relis ce savoir à voix haute puis réessaie d'invoquer $featureLabel dans un instant."
            else -> "Patiente un instant puis relance $featureLabel."
        }

        return buildFallbackGodResponse(
            text = text,
            matiere = matiere,
            godName = upperGod,
            tone = "immersif, temporairement indisponible mais rassurant",
            suggestedAction = suggestedAction,
            mnemo = mnemo
        )
    }

    /**
     * CHANTIER 1 - FONDATION IA
     * Retourne un petit supplément de lore selon le nombre de cours
     * déjà enregistrés dans la matière concernée.
     *
     * Cette méthode est conservée pour nourrir le contexte IA,
     * mais ce n'est plus elle qui compose la phrase finale.
     */
    fun buildLoreSuffix(
        matiere: String,
        nbCoursMatiere: Int
    ): String {
        return when {
            nbCoursMatiere <= 0 -> "Temple encore vide dans la matière $matiere."
            nbCoursMatiere == 1 -> "Première trace de savoir enregistrée dans la matière $matiere."
            nbCoursMatiere in 2..4 -> "Temple de $matiere en cours de formation."
            nbCoursMatiere in 5..9 -> "Fondations solides déjà visibles dans le domaine $matiere."
            nbCoursMatiere in 10..19 -> "Sanctuaire de savoir déjà bien établi en $matiere."
            else -> "Domaine avancé et dense en $matiere, reconnu par le Panthéon."
        }
    }

    /**
     * CHANTIER 1 - FONDATION IA
     * Dialogue d'accueil dans l'Arène d'Entraînement pour une matière précise.
     *
     * Cette méthode devient suspend pour déléguer la formulation finale
     * à GeminiManager.generateDialog(), tout en conservant la même signature métier.
     */
    suspend fun buildTrainingDialogue(
        matiere: String,
        nbCoursMatiere: Int
    ): String {
        val god = GodManager.fromMatiere(matiere)
        val nomDieu = god?.nomDieu ?: "ZEUS"
        val tonaliteSource = god?.getDialogue(GodDialogContext.ACCUEIL)
            ?: "Prépare-toi. Le savoir t'attend."
        val lore = buildLoreSuffix(matiere, nbCoursMatiere)

        val prompt = buildTrainingPrompt(
            matiere = matiere,
            nomDieu = nomDieu,
            tonaliteSource = tonaliteSource,
            lore = lore,
            nbCoursMatiere = nbCoursMatiere
        )

        val response = GeminiManager.generateDialog(
            prompt = prompt,
            matiere = matiere
        )

        return response?.text ?: "$tonaliteSource $lore"
    }

    /**
     * CHANTIER 1 - FONDATION IA
     * Dialogue d'accueil pour l'Épreuve Ultime.
     * Zeus reste l'arbitre du Panthéon complet.
     */
    suspend fun buildUltimeDialogue(
        totalCours: Int,
        matiereChoisie: String? = null
    ): String {
        val matiereEffective = if (matiereChoisie.isNullOrBlank()) {
            "Panthéon"
        } else {
            matiereChoisie
        }

        val god = if (matiereEffective == "Panthéon") {
            null
        } else {
            GodManager.fromMatiere(matiereEffective)
        }

        val nomDieu = when {
            matiereEffective == "Panthéon" -> "ZEUS"
            else -> god?.nomDieu ?: "ZEUS"
        }

        val tonaliteSource = when {
            matiereEffective == "Panthéon" -> "Je suis Zeus. Le Panthéon t'observe."
            else -> god?.getDialogue(GodDialogContext.ACCUEIL)
                ?: "Une épreuve majeure approche."
        }

        val prompt = buildUltimePrompt(
            totalCours = totalCours,
            matiereChoisie = matiereEffective,
            nomDieu = nomDieu,
            tonaliteSource = tonaliteSource
        )

        val response = GeminiManager.generateDialog(
            prompt = prompt,
            matiere = matiereEffective
        )

        return response?.text ?: tonaliteSource
    }

    /**
     * CHANTIER 1 - FONDATION IA
     * Version historique conservée pour compatibilité avec d'autres écrans éventuels.
     */
    suspend fun buildQuizResultDialogue(
        matiere: String,
        percentage: Int,
        nbCoursMatiere: Int,
        isUltime: Boolean = false
    ): String {
        val lore = buildLoreSuffix(matiere, nbCoursMatiere)
        val god = GodManager.fromMatiere(matiere)
        val nomDieu = god?.nomDieu ?: "ZEUS"

        val tonaliteSource = when {
            percentage == 100 -> god?.getDialogue(GodDialogContext.VICTOIRE)
                ?: "Victoire parfaite."
            percentage >= 75 -> god?.getDialogue(GodDialogContext.VICTOIRE)
                ?: "Très bon résultat."
            percentage >= 50 -> god?.getDialogue(GodDialogContext.ENCOURAGEMENT)
                ?: "Tu progresses. Continue."
            else -> god?.getDialogue(GodDialogContext.DEFAITE)
                ?: "Ce n'est pas encore suffisant. Réessaie."
        }

        val prompt = buildQuizResultPrompt(
            matiere = matiere,
            percentage = percentage,
            nbCoursMatiere = nbCoursMatiere,
            isUltime = isUltime,
            nomDieu = nomDieu,
            tonaliteSource = tonaliteSource,
            lore = lore,
            profile = null
        )

        val response = GeminiManager.generateDialog(
            prompt = prompt,
            matiere = matiere
        )

        return response?.text ?: "$tonaliteSource $lore"
    }

    /**
     * CHANTIER 1.1 & 1.2
     * Nouvelle version IA-visible pour l'écran de résultat.
     * Elle retourne directement un GodResponse exploitable par l'UI.
     */
    suspend fun buildQuizResultDialogue(
        matiere: String,
        percentage: Int,
        profile: UserProfile,
        isUltime: Boolean = false,
        adaptiveContextNote: String = ""
    ): GeminiManager.GodResponse {
        val god = GodManager.fromMatiere(matiere)
        val nomDieu = god?.nomDieu ?: "ZEUS"

        val tonaliteSource = when {
            percentage == 100 -> god?.getDialogue(GodDialogContext.VICTOIRE)
                ?: "Victoire parfaite."
            percentage >= 75 -> god?.getDialogue(GodDialogContext.VICTOIRE)
                ?: "Très bon résultat."
            percentage >= 50 -> god?.getDialogue(GodDialogContext.ENCOURAGEMENT)
                ?: "Tu progresses. Continue."
            else -> god?.getDialogue(GodDialogContext.DEFAITE)
                ?: "Ce n'est pas encore suffisant. Réessaie."
        }

        val nbCoursMatiere = estimateNbCoursFromProfile(profile)
        val lore = buildLoreSuffix(matiere, nbCoursMatiere)

        val prompt = buildQuizResultPrompt(
            matiere = matiere,
            percentage = percentage,
            nbCoursMatiere = nbCoursMatiere,
            isUltime = isUltime,
            nomDieu = nomDieu,
            tonaliteSource = tonaliteSource,
            lore = lore,
            profile = profile,
            adaptiveContextNote = adaptiveContextNote
        )

        val response = GeminiManager.generateDialog(
            prompt = prompt,
            matiere = matiere,
            adaptiveContextNote = adaptiveContextNote
        )

        return response ?: buildFallbackGodResponse(
            text = "$tonaliteSource $lore",
            matiere = matiere,
            godName = nomDieu,
            tone = if (percentage >= 75) "solennel et fier" else "exigeant mais encourageant",
            suggestedAction = if (percentage >= 75) {
                "Passe au prochain défi."
            } else {
                "Révise la notion la plus fragile puis relance un entraînement."
            },
            mnemo = "Un point faible revu tout de suite se fixe mieux."
        )
    }

    /**
     * PHASE B — JARDIN DE DÉMÉTER
     * Dialogue d'alerte de répétition espacée.
     * Déméter avertit l'élève qu'un savoir commence à faner.
     *
     * La fonction retourne un GodResponse complet pour rester cohérente
     * avec les autres scènes IA visibles de l'application.
     */
    suspend fun buildGardenDialogue(
        fadingLessonTitle: String,
        profile: UserProfile
    ): GeminiManager.GodResponse {
        val nomDieu = "DÉMÉTER"
        val matiere = "Révision"

        val prompt = buildGardenPrompt(
            fadingLessonTitle = fadingLessonTitle,
            profile = profile,
            nomDieu = nomDieu
        )

        val response = GeminiManager.generateDialog(
            prompt = prompt,
            matiere = matiere
        )

        return response ?: buildFallbackGodResponse(
            text = "Je suis Déméter. Le savoir intitulé « $fadingLessonTitle » réclame ta présence. Viens relire ce parchemin maintenant, fais-le respirer, reformule son idée centrale et rends-lui sa vigueur avant que ton jardin intérieur ne se fane davantage.",
            matiere = matiere,
            godName = nomDieu,
            tone = "maternel, noble, pédagogique et vigilant",
            suggestedAction = "Ouvre ce savoir tout de suite, relis-le avec calme puis entraîne-toi dessus avant que mon jardin ne perde ses couleurs.",
            mnemo = "Un savoir revu juste avant l'oubli reprend racine plus profondément."
        )
    }

    /**
     * CHANTIER 1.1 & 1.2
     * Génère une correction divine détaillée pour une question reviewée.
     */
    suspend fun buildCorrectionDialogue(
        matiere: String,
        question: QuizQuestion,
        bonneReponse: String,
        reponseUser: String,
        profile: UserProfile,
        adaptiveContextNote: String = ""
    ): GeminiManager.GodResponse {
        val god = GodManager.fromMatiere(matiere)
        val nomDieu = god?.nomDieu ?: "ZEUS"
        val userAnswerSafe = reponseUser.ifBlank { "Aucune réponse" }
        val isCorrect = userAnswerSafe.equals(
            question.normalizedCorrectAnswer(),
            ignoreCase = true
        ) || userAnswerSafe.equals(
            bonneReponse.trim(),
            ignoreCase = true
        )

        val prompt = buildCorrectionPrompt(
            matiere = matiere,
            question = question,
            bonneReponse = bonneReponse,
            reponseUser = userAnswerSafe,
            profile = profile,
            nomDieu = nomDieu,
            isCorrect = isCorrect,
            adaptiveContextNote = adaptiveContextNote
        )

        val response = GeminiManager.generateDialog(
            prompt = prompt,
            matiere = matiere,
            adaptiveContextNote = adaptiveContextNote
        )

        return response ?: buildFallbackGodResponse(
            text = if (isCorrect) {
                "Tu as bien répondu à cette question. La bonne réponse était « $bonneReponse ». Garde en tête l'idée centrale de l'énoncé pour reproduire ce raisonnement."
            } else {
                "Tu as répondu « $userAnswerSafe », mais la bonne réponse était « $bonneReponse ». Repars de l'énoncé et repère le mot-clé ou la notion exacte qui oriente vers la bonne solution."
            },
            matiere = matiere,
            godName = nomDieu,
            tone = if (isCorrect) "valorisant et pédagogique" else "corrigeant et clair",
            suggestedAction = "Relis cette question puis reformule la règle avec tes mots.",
            mnemo = "Énoncé -> mot-clé -> règle -> réponse."
        )
    }

    // ══════════════════════════════════════════════════════════
    // PHASE B — LYRE D'APOLLON
    // ══════════════════════════════════════════════════════════

    /**
     * PHASE B — LYRE D'APOLLON
     * Transforme le contenu brut d'un cours en un hymne poétique mémorisable.
     *
     * Apollon, dieu des arts, de la lumière et de la prophétie,
     * prend en charge le savoir et le réenchante sous forme de vers rythmés.
     *
     * Retourne un GodResponse dont :
     * - text  → le poème / l'hymne généré (affiché avec typewriter)
     * - mnemo → la formule-clé condensée du cours en une ligne
     * - suggestedAction → conseil de mémorisation vocale (réciter à voix haute)
     *
     * REFACTO PROPRE :
     * - la Lyre passe désormais exclusivement par GeminiManager.generateDialog()
     * - les fallbacks restent centralisés ici et non dans l'Activity
     * - la matière renvoyée reste cohérente avec le cours, pas un libellé arbitraire
     *
     * ÉVOLUTION FUTURE :
     * - on pourrait proposer plusieurs styles (haïku, hexamètre, ode)
     * - on pourrait varier selon la matière ou le niveau du joueur
     * - on pourra intercepter ici les erreurs de quota pour renvoyer "Apollon est fatigué"
     *
     * @param courseContent Le texte extrait du cours (extractedText).
     * @param profile       Le profil du joueur pour adapter le niveau de langue.
     */
    suspend fun buildEducationalHymn(
        courseContent: String,
        profile: UserProfile,
        matiere: String = "Poésie du Savoir"
    ): GeminiManager.GodResponse {
        val nomDieu = "APOLLON"

        // On tronque le contenu pour éviter les prompts trop lourds :
        // Gemini 2.5 Flash gère bien jusqu'à ~8 000 tokens,
        // mais on reste raisonnable pour des temps de réponse optimaux.
        val courseSnippet = if (courseContent.length > 2000) {
            courseContent.take(2000) + "\n[...contenu tronqué pour l'hymne...]"
        } else {
            courseContent
        }

        val prompt = buildHymnPrompt(
            courseContent = courseSnippet,
            profile = profile,
            nomDieu = nomDieu,
            matiere = matiere
        )

        val response = try {
            GeminiManager.generateDialog(
                prompt = prompt,
                matiere = matiere
            )
        } catch (_: Exception) {
            null
        }

        // 1) Réponse IA valide → on la garde
        if (response != null && response.text.isNotBlank()) {
            return response
        }

        // 2) Si l'IA ne répond pas, on renvoie un vrai hymne fallback métier
        return buildEducationalHymnFallback(
            courseContent = courseSnippet,
            matiere = matiere,
            godName = nomDieu
        )
    }

    /**
     * REFACTO PROPRE — FALLBACK MÉTIER LYRE
     *
     * On préfère ici un fallback poétique utile à un message vide.
     * Le contenu essaie de recycler les notions du cours sous une forme courte.
     * Si le support est trop pauvre, on bascule vers un hymne générique cohérent.
     */
    private fun buildEducationalHymnFallback(
        courseContent: String,
        matiere: String,
        godName: String
    ): GeminiManager.GodResponse {
        val sanitizedLines = courseContent
            .replace("\r", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(4)

        if (sanitizedLines.isNotEmpty()) {
            val poeticBody = sanitizedLines.joinToString("\n") { line ->
                val cleaned = line
                    .removePrefix("-")
                    .removePrefix("•")
                    .trim()
                    .let { if (it.length > 90) it.take(90).trimEnd() + "…" else it }
                "• $cleaned"
            }

            return buildFallbackGodResponse(
                text = "Sous ma lyre, retiens ces éclats de savoir :\n\n$poeticBody\n\nRépète-les en rythme, et le cours reviendra plus vite à ta mémoire.",
                matiere = matiere,
                godName = godName,
                tone = "lyrique, inspirant et pédagogique",
                suggestedAction = "Lis ces vers à voix haute trois fois, puis reformule la notion centrale sans regarder le texte.",
                mnemo = "Ce que tu chantes s'ancre mieux."
            )
        }

        return buildFallbackGodResponse(
            text = "Apollon prend sa lyre…\n\nLe savoir que tu tiens\nEst une graine divine.\nRépète, grave, retiens —\nC'est ainsi qu'on s'illumine.",
            matiere = matiere,
            godName = godName,
            tone = "lyrique, inspirant et mémorisable",
            suggestedAction = "Lis cet hymne à voix haute trois fois pour ancrer les notions clés.",
            mnemo = "Ce que tu chantes, tu le retiens."
        )
    }

    // ══════════════════════════════════════════════════════════
    // PHASE B — DÉFIS D'ARÈS (Difficulté Adaptative)
    // ══════════════════════════════════════════════════════════

    /**
     * PHASE B — DÉFIS D'ARÈS
     * Déclenché après 3 scores consécutifs ≥ 95%.
     *
     * Arès, dieu de la guerre et du défi, intervient pour provoquer l'élève :
     * il reconnaît la maîtrise, mais juge que la facilité est une insulte à un
     * vrai guerrier du savoir. Il exige un niveau supérieur.
     *
     * Retourne un GodResponse dont :
     * - text          → discours martial et provocateur d'Arès
     * - mnemo         → cri de guerre / devise guerrière courte
     * - suggestedAction → lancer un quiz plus difficile ou en mode Panthéon
     * - tone          → "martial, brusque, respectueux de la force"
     *
     * ÉVOLUTION FUTURE :
     * - Débloquer un badge "Guerrier du Savoir" si l'élève relève le défi
     * - Proposer directement un Intent vers TrainingSelectActivity en mode difficile
     *
     * @param matiere  La matière dans laquelle l'élève enchaîne les succès.
     * @param profile  Le profil complet pour contextualiser le défi (niveau, XP, classe).
     */
    suspend fun buildAresChallenge(
        matiere: String,
        profile: UserProfile
    ): GeminiManager.GodResponse {
        val nomDieu = "ARÈS"

        val prompt = buildAresChallengePrompt(
            matiere = matiere,
            profile = profile,
            nomDieu = nomDieu
        )

        val response = GeminiManager.generateDialog(
            prompt = prompt,
            matiere = matiere
        )

        // Fallback guerrier si l'IA ne répond pas : Arès ne se tait jamais.
        return response ?: buildFallbackGodResponse(
            text = "Trois victoires d'affilée en $matiere. Je suis ARÈS. " +
                "Tu crois que ça m'impressionne ? Ce que tu fais là, c'est de l'entraînement de recrue. " +
                "Un vrai guerrier du savoir cherche l'épreuve qui lui résiste. " +
                "Lance-toi sur le Panthéon complet — ou reste dans ta zone de confort. " +
                "Mais ne viens pas te plaindre si ton savoir rouille faute de combat.",
            matiere = matiere,
            godName = nomDieu,
            tone = "martial, brusque, respectueux de la force",
            suggestedAction = "Lance un quiz en mode difficile ou affronte le Panthéon complet.",
            mnemo = "La victoire facile est la mère de la défaite future."
        )
    }

    // ══════════════════════════════════════════════════════════
    // BUILDERS PRIVÉS — PROMPTS
    // ══════════════════════════════════════════════════════════

    /**
     * PHASE B — DÉFIS D'ARÈS
     * Construction du prompt martial pour le défi d'Arès.
     *
     * Contraintes clés du prompt :
     * - Arès RECONNAÎT la maîtrise (pas de mépris gratuit)
     * - Il PROVOQUE avec respect guerrier — comme un sergent fier mais exigeant
     * - Il PROPOSE une action concrète difficile, pas juste une vantardise
     * - Le ton est direct, bref, incisif : Arès ne fait pas de discours longs
     *
     * ÉVOLUTION FUTURE :
     * - Ajouter le nombre exact de victoires consécutives dans le prompt
     *   pour un discours encore plus précis ("Cinq victoires, pas trois.")
     */
    private fun buildAresChallengePrompt(
        matiere: String,
        profile: UserProfile,
        nomDieu: String
    ): String {
        return """
            Tu incarnes $nomDieu dans RéviZeus.

            CONTEXTE :
            - Type de scène : défi d'Arès — difficulté adaptative
            - Dieu : $nomDieu (dieu de la guerre, du courage et du dépassement)
            - Matière dans laquelle l'élève excelle : $matiere
            - Âge joueur : ${profile.age}
            - Classe joueur : ${profile.classLevel}
            - Humeur joueur : ${profile.mood}
            - Niveau joueur : ${profile.level}
            - XP total : ${profile.xp}
            - Streak de victoire : ${profile.winStreak}
            - Pattern cognitif : ${profile.cognitivePattern}

            SITUATION :
            L'élève vient d'enchaîner 3 scores consécutifs de 95% ou plus en $matiere.
            Il maîtrise cette matière. Le niveau actuel est trop facile pour lui.
            Arès intervient pour le provoquer et l'appeler à un défi supérieur.

            OBJECTIF :
            - Reconnaître la maîtrise avec respect guerrier, sans flatterie molle
            - Provoquer l'élève : ce niveau n'est plus un vrai combat
            - Exiger davantage : un vrai guerrier cherche l'épreuve qui lui résiste
            - Proposer un défi concret (quiz plus difficile, Panthéon complet, autre matière)
            - Donner une devise guerrière courte et mémorisable

            CONTRAINTES STRICTES :
            - Ton : martial, brusque, direct — mais respectueux de la force prouvée
            - PAS de mépris ni d'humiliation : Arès respecte les guerriers qui gagnent
            - PAS de phrase molle ou encourageante : Arès n'est pas Apollon
            - PAS de longue tirade épique : 4 à 6 phrases maximum
            - Le champ "text" doit contenir le discours martial d'Arès
            - Le champ "mnemo" est une devise de guerre courte (max 10 mots)
            - Le champ "suggestedAction" propose l'épreuve suivante concrète
            - Le champ "godName" doit être "$nomDieu"
            - Le champ "tone" est "martial, brusque, respectueux de la force"
            - Réponse en français uniquement
            - Adapte la violence du défi à l'âge et à la classe du joueur
        """.trimIndent()
    }

    // ══════════════════════════════════════════════════════════
    // PHASE C — FORGE D'HÉPHAÏSTOS (Dialogue de succès)
    // ══════════════════════════════════════════════════════════

    /**
     * PHASE C — FORGE D'HÉPHAÏSTOS
     * Dialogue d'Héphaïstos déclenché après un craft réussi.
     *
     * Héphaïstos, dieu boiteux du feu et de la forge, annonce fièrement
     * l'objet qu'il vient de créer pour l'élève. Son ton est rugueux,
     * pragmatique et artisan — pas de grandiloquence, juste la fierté
     * d'un dieu qui travaille de ses mains.
     *
     * @param recipeName Nom de l'objet crafté (ex : "Bouclier de Logique").
     * @param profile    Profil du joueur pour personnaliser le discours.
     * @return GodResponse avec text = discours d'Héphaïstos, mnemo = devise d'artisan,
     *         suggestedAction = conseil d'utilisation de l'objet.
     *
     * ÉVOLUTION FUTURE :
     * - Passer `recipe.type` pour qu'Héphaïstos adapte son discours selon
     *   le type d'objet (il parle différemment d'une arme et d'un parchemin)
     * - Ajouter `recipe.lore` au prompt pour qu'il le cite dans sa réponse
     */
    suspend fun buildForgeSuccessDialogue(
        recipeName: String,
        profile: UserProfile
    ): GeminiManager.GodResponse {
        val nomDieu = "HÉPHAÏSTOS"
        val matiere = "Forge du Savoir"

        val prompt = buildForgeSuccessPrompt(
            recipeName = recipeName,
            profile = profile,
            nomDieu = nomDieu
        )

        val response = GeminiManager.generateDialog(
            prompt = prompt,
            matiere = matiere
        )

        // Fallback artisan garanti — Héphaïstos ne reste jamais silencieux
        return response ?: buildFallbackGodResponse(
            text = "Hmph. Le voilà, ton « $recipeName ». " +
                "Trempé dans les fragments de ta propre connaissance. " +
                "Chaque coup de marteau, c'était une bonne réponse que tu avais donnée. " +
                "Ne le gaspille pas. Les dieux n'ont pas de patience pour les ingrats.",
            matiere = matiere,
            godName = nomDieu,
            tone = "rugueux, artisan, fier de son travail",
            suggestedAction = "Équipe cet objet et continue à accumuler des fragments en répondant correctement.",
            mnemo = "Ce que tu forges avec tes mains, tu ne l'oublies pas."
        )
    }

    /**
     * PHASE C — FORGE D'HÉPHAÏSTOS
     * Prompt pour le dialogue de succès de craft.
     *
     * Héphaïstos est le plus humain des dieux : il travaille, il transpire,
     * il est perfectionniste. Son registre est celui de l'artisan fier,
     * pas du dieu omnipotent. Il ne loue pas l'élève avec des superlatifs —
     * il lui montre son travail et lui dit de ne pas le décevoir.
     */
    private fun buildForgeSuccessPrompt(
        recipeName: String,
        profile: UserProfile,
        nomDieu: String
    ): String {
        return """
            Tu incarnes $nomDieu dans RéviZeus.

            CONTEXTE :
            - Type de scène : succès de craft à la Forge
            - Dieu : $nomDieu (dieu du feu, de la forge, de l'artisanat divin)
            - Objet forgé : $recipeName
            - Nom du joueur : ${profile.pseudo}
            - Niveau joueur : ${profile.level}
            - Classe joueur : ${profile.classLevel}
            - XP total : ${profile.xp}

            SITUATION :
            Le joueur vient de dépenser ses Fragments de Connaissance pour crafter
            l'objet "$recipeName". Héphaïstos l'a forgé de ses propres mains à partir
            de ces fragments — chaque fragment représente une bonne réponse donnée en quiz.

            OBJECTIF :
            - Annoncer la création de l'objet avec la fierté d'un artisan, pas d'un dieu pompeux
            - Rappeler que l'objet a été forgé avec de vrais efforts (les bonnes réponses)
            - Donner un conseil d'utilisation ou de conservation pragmatique
            - Conclure avec une devise d'artisan courte et mémorisable

            CONTRAINTES STRICTES :
            - Ton : rugueux, direct, artisan fier — jamais grandiloquent
            - Héphaïstos est boiteux, travailleur, légèrement bourru : il n'est pas Zeus
            - 4 à 6 phrases maximum — il n'a pas de temps à perdre en discours
            - Le champ "text" est le discours d'Héphaïstos
            - Le champ "mnemo" est une devise d'artisan (max 10 mots, pratico-pratique)
            - Le champ "suggestedAction" conseille comment utiliser ou mériter l'objet
            - Le champ "godName" est "$nomDieu"
            - Réponse en français uniquement
        """.trimIndent()
    }

    /**
     * CHANTIER 1 - FONDATION IA
     * Construction du contexte IA pour l'accueil d'entraînement.
     */
    private fun buildTrainingPrompt(
        matiere: String,
        nomDieu: String,
        tonaliteSource: String,
        lore: String,
        nbCoursMatiere: Int
    ): String {
        return """
            Tu incarnes $nomDieu dans RéviZeus.

            CONTEXTE :
            - Type de scène : accueil d'entraînement
            - Matière : $matiere
            - Dieu : $nomDieu
            - Nombre de cours connus dans cette matière : $nbCoursMatiere
            - État du temple personnel : $lore
            - Ancre de ton existante : $tonaliteSource

            OBJECTIF :
            - Accueillir l'élève avant un entraînement
            - Être motivant, noble et utile
            - Faire sentir la progression du temple personnel
            - Préparer mentalement l'élève à réviser sans raconter une épopée hors sujet

            CONTRAINTES :
            - Réponse principale brève à moyenne, adaptée à une UI mobile
            - Ton immersif mais pédagogique
            - Pas de texte générique vide
            - Suggère une action concrète pour démarrer l'entraînement
            - Donne une mnémotechnique courte en lien avec la matière
        """.trimIndent()
    }

    /**
     * CHANTIER 1 - FONDATION IA
     * Construction du contexte IA pour l'épreuve ultime.
     */
    private fun buildUltimePrompt(
        totalCours: Int,
        matiereChoisie: String,
        nomDieu: String,
        tonaliteSource: String
    ): String {
        return """
            Tu incarnes $nomDieu dans RéviZeus.

            CONTEXTE :
            - Type de scène : accueil d'épreuve ultime
            - Matière ciblée : $matiereChoisie
            - Dieu principal : $nomDieu
            - Nombre total de cours connus : $totalCours
            - Ancre de ton existante : $tonaliteSource

            OBJECTIF :
            - Introduire une épreuve importante
            - Donner une sensation de gravité, de grandeur et de préparation
            - Rester clair, utile, mobile-friendly et motivant

            CONTRAINTES :
            - Si la matière ciblée est "Panthéon", Zeus doit parler comme arbitre du tout
            - Sinon, le dieu lié à la matière doit porter la scène
            - Pas de monologue trop long
            - Suggère une action concrète avant de lancer l'épreuve
            - Ajoute une mnémotechnique courte ou un rappel stratégique
        """.trimIndent()
    }

    /**
     * PHASE B — JARDIN DE DÉMÉTER
     * Construction du prompt IA pour l'alerte de répétition espacée.
     */
    private fun buildGardenPrompt(
        fadingLessonTitle: String,
        profile: UserProfile,
        nomDieu: String
    ): String {
        return """
            Tu incarnes $nomDieu dans RéviZeus.

            CONTEXTE :
            - Type de scène : alerte de répétition espacée
            - Dieu : $nomDieu
            - Cours en train de faner : $fadingLessonTitle
            - Âge joueur : ${profile.age}
            - Classe joueur : ${profile.classLevel}
            - Humeur joueur : ${profile.mood}
            - Niveau joueur : ${profile.level}
            - XP total : ${profile.xp}
            - Pattern cognitif : ${profile.cognitivePattern}

            OBJECTIF :
            - Prévenir l'élève qu'un savoir commence à s'effacer
            - Parler comme Déméter, gardienne de la croissance, des récoltes et du vivant
            - Donner envie de réviser immédiatement
            - Relier la mémoire à l'image d'un jardin intérieur
            - Rester clair, noble, chaleureux et utile dans une UI mobile

            CONTRAINTES :
            - Réponse principale concise à modérée
            - Le texte doit citer le titre du cours : $fadingLessonTitle
            - Le ton doit être maternel, vigilant et inspirant
            - Propose une action de révision très concrète
            - Donne une mnémotechnique courte liée à l'idée de graine, racine, pousse ou récolte
            - Le champ text ne doit jamais être vide
            - Le champ mnemo doit être réellement mémorisable
        """.trimIndent()
    }

    /**
     * PHASE B — LYRE D'APOLLON
     * Construction du prompt IA pour la transformation poétique d'un cours.
     *
     * Apollon doit :
     * 1. Identifier les 3 à 5 notions centrales du cours
     * 2. Les encoder dans des vers rythmés, courts et mémorisables
     * 3. Conserver le sens exact sans le trahir ni le romancer inutilement
     *
     * REFACTO PROPRE :
     * - le prompt insiste davantage sur la fidélité pédagogique
     * - la matière est maintenant injectée pour éviter un libellé trop vague
     * - la Lyre est alignée sur la bibliothèque de prompts du projet
     *
     * ÉVOLUTION FUTURE :
     * - Ajouter un paramètre "style" pour choisir entre ode, sonnet, hexamètre, haïku
     * - Adapter le nombre de strophes selon la longueur du cours
     */
    private fun buildHymnPrompt(
        courseContent: String,
        profile: UserProfile,
        nomDieu: String,
        matiere: String
    ): String {
        return """
            Tu incarnes $nomDieu dans RéviZeus.

            CONTEXTE :
            - Type de scène : invocation de la Lyre d'Apollon
            - Dieu : $nomDieu
            - Matière du savoir : $matiere
            - Âge joueur : ${profile.age}
            - Classe joueur : ${profile.classLevel}
            - Humeur joueur : ${profile.mood}
            - Niveau joueur : ${profile.level}
            - Pattern cognitif : ${profile.cognitivePattern}

            OBJECTIF :
            - Transformer le cours fourni en une courte poésie rythmée et mémorisable
            - Le poème doit encoder les 3 à 5 notions les plus importantes du cours
            - Chaque vers doit porter une information réelle et mémorisable
            - Le ton est celui d'un hymne, d'une ode lumineuse et pédagogique
            - Apollon chante pour aider l'élève à retenir, pas pour embellir le vide

            CONTRAINTES STRICTES :
            - La poésie doit rester FIDÈLE au contenu réel du cours (pas d'invention)
            - Maximum 10 vers, minimum 4 vers — adapté à une UI mobile en scrollview
            - Chaque vers doit contenir une notion concrète du cours (date, formule, règle, cause, définition, étape)
            - Adapter la complexité lexicale à l'âge et à la classe du joueur
            - Le champ "text" contient uniquement le poème (sans introduction, sans titre)
            - Le champ "mnemo" contient une formule ultra-courte (1 ligne) qui résume l'idée centrale
            - Le champ "suggestedAction" propose de réciter le poème à voix haute
            - Le champ "godName" doit être "$nomDieu"
            - Le champ "matiere" doit être "$matiere"
            - Le champ "tone" est "lyrique, inspirant et pédagogique"
            - Le texte principal ne doit jamais être vide ni générique
            - Interdiction de raconter un mythe hors sujet
            - Interdiction d'ajouter du folklore qui remplace le savoir scolaire
            - Réponse en français uniquement

            CONTENU DU COURS À TRANSFORMER :
            $courseContent
        """.trimIndent()
    }

    /**
     * CHANTIER 1.1 & 1.2
     * Construction du contexte IA pour le verdict de quiz, enrichi avec le profil joueur.
     */
    private fun buildQuizResultPrompt(
        matiere: String,
        percentage: Int,
        nbCoursMatiere: Int,
        isUltime: Boolean,
        nomDieu: String,
        tonaliteSource: String,
        lore: String,
        profile: UserProfile?,
        adaptiveContextNote: String = ""
    ): String {
        val niveauResultat = when {
            percentage == 100 -> "maîtrise parfaite"
            percentage >= 75 -> "très bonne performance"
            percentage >= 50 -> "progression réelle mais encore incomplète"
            else -> "résultat fragile nécessitant un nouvel entraînement"
        }

        val typeEpreuve = if (isUltime) {
            "épreuve ultime"
        } else {
            "quiz d'entraînement"
        }

        val profileBlock = if (profile == null) {
            "- Profil joueur : non transmis"
        } else {
            """
            - Âge : ${profile.age}
            - Classe : ${profile.classLevel}
            - Humeur : ${profile.mood}
            - XP total : ${profile.xp}
            - Niveau : ${profile.level}
            - Streak de victoire : ${profile.winStreak}
            - Pattern cognitif : ${profile.cognitivePattern}
            """.trimIndent()
        }

        return """
            Tu incarnes $nomDieu dans RéviZeus.

            CONTEXTE :
            - Type de scène : verdict de quiz
            - Nature de l'épreuve : $typeEpreuve
            - Matière : $matiere
            - Dieu : $nomDieu
            - Score : $percentage%
            - Niveau de performance : $niveauResultat
            - Nombre estimé de traces de savoir dans cette matière : $nbCoursMatiere
            - État du temple personnel : $lore
            - Ancre de ton existante : $tonaliteSource
            - Profil joueur :
            $profileBlock

            OBJECTIF :
            - Donner un verdict clair, incarné et juste
            - Féliciter sans flatter à vide
            - Encourager sans être mou
            - Recadrer sans écraser l'élève
            - Relier le résultat à l'état du temple de savoir
            - Adapter le ton à l'âge, la classe et l'humeur
            - Exploiter en priorité le contexte adaptatif complémentaire si fourni

            CONTRAINTES :
            - Réponse principale concise à modérée
            - Style compatible avec une UI mobile
            - Ton adapté au score
            - Si isUltime = true, le ton peut être plus solennel
            - Suggère une prochaine action concrète
            - Donne une mnémotechnique courte en lien avec la matière ou la performance
            - Le champ text ne doit pas être vide
            - Le champ mnemo doit vraiment aider la mémorisation

            CONTEXTE ADAPTATIF COMPLÉMENTAIRE :
            ${if (adaptiveContextNote.isBlank()) "Aucun signal complémentaire" else adaptiveContextNote}
        """.trimIndent()
    }

    /**
     * CHANTIER 1.1 & 1.2
     * Construction du contexte IA pour la correction détaillée d'une question.
     */
    private fun buildCorrectionPrompt(
        matiere: String,
        question: QuizQuestion,
        bonneReponse: String,
        reponseUser: String,
        profile: UserProfile,
        nomDieu: String,
        isCorrect: Boolean,
        adaptiveContextNote: String = ""
    ): String {
        return """
            Tu incarnes $nomDieu dans RéviZeus.

            CONTEXTE :
            - Type de scène : correction divine d'une question
            - Matière : $matiere
            - Dieu : $nomDieu
            - Âge joueur : ${profile.age}
            - Classe joueur : ${profile.classLevel}
            - Humeur joueur : ${profile.mood}
            - Niveau joueur : ${profile.level}
            - Pattern cognitif : ${profile.cognitivePattern}
            - Réponse utilisateur : $reponseUser
            - Bonne réponse : $bonneReponse
            - Résultat : ${if (isCorrect) "bonne réponse" else "erreur"}
            - Question :
              ${question.text}

            CHOIX DISPONIBLES :
            - A : ${question.optionA}
            - B : ${question.optionB}
            - C : ${question.optionC}

            OBJECTIF :
            - Expliquer simplement pourquoi la bonne réponse est correcte
            - Si l'élève s'est trompé, corriger sans humilier
            - Si l'élève a réussi, consolider la compréhension
            - Faire une réponse claire pour une fenêtre de dialogue mobile
            - Donner une mnémotechnique courte et utile
            - Utiliser une vraie progression maïeutique courte : erreur ou angle manqué -> guidage -> reformulation juste

            CONTRAINTES :
            - Réponse pédagogique, pas de remplissage
            - Pas de long paragraphe opaque
            - Le texte principal explique la logique
            - Le mnemo est très court et mémorisable
            - La suggestedAction doit proposer un geste concret de révision
            - Maximum 3 phrases nettes dans text
            - Une seule idée-clé par correction
            - Si l'élève est jeune, stressé ou fragile, le vocabulaire doit être très simple
            - Si le contexte adaptatif signale une fragilité forte, le ton doit être rassurant et très guidé
            - Si la réponse est correcte, consolide la méthode au lieu de répéter platement la solution

            CONTEXTE ADAPTATIF COMPLÉMENTAIRE :
            ${if (adaptiveContextNote.isBlank()) "Aucun signal complémentaire" else adaptiveContextNote}
        """.trimIndent()
    }

    /**
     * CHANTIER 1.1 & 1.2
     * Estimation locale légère depuis le profil pour nourrir l'IA
     * sans casser la signature demandée profile-only.
     */
    private fun estimateNbCoursFromProfile(
        profile: UserProfile
    ): Int {
        val xpEstimate = (profile.xp / 120).coerceAtLeast(0)
        val streakBonus = profile.winStreak.coerceAtLeast(0)
        return (xpEstimate + streakBonus).coerceAtMost(50)
    }
}

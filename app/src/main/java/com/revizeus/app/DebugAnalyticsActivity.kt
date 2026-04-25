package com.revizeus.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.UserAnalytics
import com.revizeus.app.models.UserProfile
import com.revizeus.app.models.UserSkillProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════
 * DEBUG ANALYTICS ACTIVITY — ML
 * ═══════════════════════════════════════════════════════════════
 * Écran de debug développeur pour visualiser les données ML.
 *
 * Affiche :
 *  - Profil de compétences par matière (maîtrise, confiance, réussite)
 *  - Statistiques globales (pratiques, réussite, révisions à faire)
 *  - Fragments de Forge par matière + crafts disponibles
 *  - Dernières réponses enregistrées (raw analytics)
 *
 * CORRECTIONS v3 :
 * ✅ BGM bgm_debug.mp3 : joué à l'entrée (onCreate), stoppé à finish() (Règle 8 : délai 300ms)
 * ✅ Bouton retour : bg_temple_button (ImageView fond) + label Cinzel or — design premium
 * ✅ stopMusic() appelé avant finish() pour couper bgm_debug proprement
 * ═══════════════════════════════════════════════════════════════
 */
class DebugAnalyticsActivity : BaseActivity() {

    private data class DebugAiSnapshot(
        val profile: UserProfile,
        val affinityProfile: GodAffinityManager.GodAffinityProfile,
        val userSourceDebug: UserSourceDebug,
        val testSubject: String,
        val godId: String,
        val personaType: DivinePersonaType,
        val adaptiveSnapshot: PlayerAdaptiveSnapshot?,
        val dialogueContext: PlayerContextResolver.PlayerDialogueContext?,
        val adaptivePreview: String,
        val recentAnalytics: List<UserAnalytics>,
        val skillProfiles: List<UserSkillProfile>,
        val fragmentsBySubject: Map<String, Int>,
        val fragmentParseMessage: String?,
        val adaptiveResolverError: String?,
        val dialogueResolverError: String?
    )

    private data class UserSourceDebug(
        val pseudoResolved: String,
        val ageResolved: Int,
        val classResolved: String,
        val moodResolved: String,
        val levelResolved: Int,
        val rankResolved: String,
        val pseudoSources: String,
        val ageSources: String,
        val classSources: String,
        val moodSources: String,
        val levelSources: String,
        val rankSources: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Portrait forcé + mode immersif gérés par BaseActivity.onCreate

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#05050A"))
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        scrollView.addView(container)
        setContentView(scrollView)

        // ── BGM DEBUG ─────────────────────────────────────────────────────────
        // Joue bgm_debug dès l'entrée dans l'écran de debug ML.
        // RÈGLE 8 : stopMusic() d'abord, délai 300ms, puis playMusic().
        lifecycleScope.launch {
            try {
                SoundManager.stopMusic()
                kotlinx.coroutines.delay(300L)
                SoundManager.playMusic(this@DebugAnalyticsActivity, R.raw.bgm_debug)
            } catch (_: Exception) {}
        }

        AnalyticsManager.initialize(this)

        lifecycleScope.launch {

            // ── EN-TÊTE ──────────────────────────────────────────────────
            container.addView(creerEntete())
            container.addView(creerSeparateur())
            container.addView(creerTitreSection("🧠 DEBUG IA CONTEXTUEL"))
            afficherSectionsDebugIa(container)
            container.addView(creerSeparateur())

            // ── COMPÉTENCES ───────────────────────────────────────────────
            val skills = withContext(Dispatchers.IO) {
                AnalyticsManager.getSkillsSummary()
            }

            if (skills.isEmpty()) {
                container.addView(creerTexteInfo("Aucune donnée ML enregistrée."))
                container.addView(creerTexteInfo("Fais quelques quiz pour commencer l'analyse !"))
            } else {
                container.addView(creerTitreSection("📊 PROFIL DE COMPÉTENCES"))

                skills.sortedByDescending { it.masteryLevel }.forEach { skill ->
                    container.addView(creerCarteSkill(skill))
                }

                // ── STATISTIQUES GLOBALES ─────────────────────────────────
                container.addView(creerSeparateur())
                container.addView(creerTitreSection("📈 STATISTIQUES GLOBALES"))

                val totalPractice = skills.sumOf { it.practiceCount }
                val avgSuccess    = skills.map { it.successRate }.average().toFloat()
                val avgMastery    = skills.map { it.masteryLevel }.average().toFloat()
                val needReview    = skills.count { it.needsReview }

                container.addView(creerTexteStatistique("Total pratiques : $totalPractice"))
                container.addView(creerTexteStatistique("Taux réussite moyen : ${(avgSuccess * 100).toInt()}%"))
                container.addView(creerTexteStatistique("Maîtrise moyenne : ${(avgMastery * 100).toInt()}%"))
                container.addView(creerTexteStatistique("Matières à réviser : $needReview"))

                // ── DERNIÈRES RÉPONSES ────────────────────────────────────
                container.addView(creerSeparateur())
                container.addView(creerTitreSection("📝 DERNIÈRES RÉPONSES"))

                val recentAnalytics = withContext(Dispatchers.IO) {
                    try {
                        val db = AppDatabase.getDatabase(this@DebugAnalyticsActivity)
                        db.userAnalyticsDao().getRecent(1, limit = 10)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                if (recentAnalytics.isEmpty()) {
                    container.addView(creerTexteInfo("Aucune réponse enregistrée."))
                } else {
                    recentAnalytics.forEach { analytics ->
                        container.addView(creerCarteAnalytics(analytics))
                    }
                }
            }

            // ── FRAGMENTS FORGE ───────────────────────────────────────────
            container.addView(creerSeparateur())
            container.addView(creerTitreSection("⚗ FRAGMENTS DE CONNAISSANCE"))
            try {
                val profile = withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(this@DebugAnalyticsActivity)
                    db.iAristoteDao().getUserStats()
                }
                if (profile != null) {
                    val json = org.json.JSONObject(profile.knowledgeFragments)
                    if (json.length() == 0) {
                        container.addView(creerTexteInfo("Aucun fragment. Réponds correctement en quiz !"))
                    } else {
                        json.keys().forEach { matiere ->
                            val count = json.optInt(matiere, 0)
                            container.addView(creerTexteStatistique("$matiere : $count fragment(s)"))
                        }
                    }
                    val affordable = try {
                        CraftingSystem.affordableRecipes(profile)
                    } catch (_: Exception) { emptyList() }

                    container.addView(creerTexteInfo(
                        if (affordable.isEmpty()) "— Aucun craft disponible actuellement."
                        else "✦ ${affordable.size} recette(s) craftable(s) → Ouvre la Forge !"
                    ))
                } else {
                    container.addView(creerTexteInfo("Profil introuvable en base."))
                }
            } catch (e: Exception) {
                container.addView(creerTexteInfo("Erreur lecture fragments : ${e.message}"))
            }

            // ── BOUTON RETOUR ─────────────────────────────────────────────
            container.addView(creerSeparateur())
            container.addView(creerBoutonRetour())
        }
    }

    private suspend fun afficherSectionsDebugIa(container: LinearLayout) {
        val debugSnapshot = try {
            buildDebugAiSnapshot()
        } catch (e: Exception) {
            container.addView(creerTexteInfo("Erreur panneau IA contextuel : ${e.message}"))
            return
        }

        // A. PROFIL IA ACTIF
        container.addView(creerTitreSection("A. PROFIL IA ACTIF"))
        container.addView(creerTexteStatistique("Pseudo : ${debugSnapshot.userSourceDebug.pseudoResolved}"))
        container.addView(creerTexteStatistique("Âge : ${debugSnapshot.userSourceDebug.ageResolved}"))
        container.addView(creerTexteStatistique("Classe : ${debugSnapshot.userSourceDebug.classResolved}"))
        container.addView(creerTexteStatistique("Humeur : ${debugSnapshot.userSourceDebug.moodResolved}"))
        container.addView(creerTexteStatistique("Niveau : ${debugSnapshot.userSourceDebug.levelResolved}"))
        container.addView(creerTexteStatistique("Rang : ${debugSnapshot.userSourceDebug.rankResolved}"))
        container.addView(creerTexteStatistique("Titre équipé : ${debugSnapshot.profile.titleEquipped.ifBlank { "—" }}"))
        container.addView(creerTexteInfo("Sources pseudo : ${debugSnapshot.userSourceDebug.pseudoSources}"))
        container.addView(creerTexteInfo("Sources âge : ${debugSnapshot.userSourceDebug.ageSources}"))
        container.addView(creerTexteInfo("Sources classe : ${debugSnapshot.userSourceDebug.classSources}"))
        container.addView(creerTexteInfo("Sources humeur : ${debugSnapshot.userSourceDebug.moodSources}"))
        container.addView(creerTexteInfo("Sources niveau : ${debugSnapshot.userSourceDebug.levelSources}"))
        container.addView(creerTexteInfo("Sources rang : ${debugSnapshot.userSourceDebug.rankSources}"))

        // B. ÉTAT ADAPTATIF
        container.addView(creerSeparateur())
        container.addView(creerTitreSection("B. ÉTAT ADAPTATIF"))
        val adaptive = debugSnapshot.adaptiveSnapshot
        val dialogue = debugSnapshot.dialogueContext
        val dominantWeakness = adaptive?.weakTopics?.firstOrNull()
            ?: dialogue?.dominantWeakness
            ?: "non déterminée"
        container.addView(creerTexteStatistique("Fatigue calculée : ${percent(adaptive?.fatigueIndex)}"))
        container.addView(creerTexteStatistique("Réussite récente : ${percent(adaptive?.recentSuccessRate)}"))
        container.addView(creerTexteStatistique("Temps moyen réponse : ${millisToSecondsLabel(adaptive?.recentAverageResponseTimeMs)}"))
        container.addView(creerTexteStatistique("Mode doux : ${yesNo(dialogue?.needsGentleMode == true)}"))
        container.addView(creerTexteStatistique("Mode challenge : ${yesNo(dialogue?.needsChallengeMode == true)}"))
        container.addView(creerTexteStatistique("Faiblesse dominante : $dominantWeakness"))
        debugSnapshot.adaptiveResolverError?.let {
            container.addView(creerTexteInfo("Resolver snapshot : erreur ($it)"))
        }
        debugSnapshot.dialogueResolverError?.let {
            container.addView(creerTexteInfo("Resolver dialogue : erreur ($it)"))
        }

        // C. FORCES / FAIBLESSES
        container.addView(creerSeparateur())
        container.addView(creerTitreSection("C. FORCES / FAIBLESSES"))
        container.addView(
            creerTexteStatistique(
                "recentErrorSubjects : ${adaptive?.recentErrorSubjects?.joinToString(", ").orEmpty().ifBlank { "—" }}"
            )
        )
        container.addView(
            creerTexteStatistique(
                "weakTopics : ${adaptive?.weakTopics?.joinToString(", ").orEmpty().ifBlank { "—" }}"
            )
        )
        container.addView(
            creerTexteStatistique(
                "strongTopics : ${adaptive?.strongTopics?.joinToString(", ").orEmpty().ifBlank { "—" }}"
            )
        )

        // D. PERSONNALITÉ DIVINE TEST
        container.addView(creerSeparateur())
        container.addView(creerTitreSection("D. PERSONNALITÉ DIVINE TEST"))
        val personality = GodPersonalityEngine.get(debugSnapshot.godId)
        container.addView(creerTexteStatistique("Matière test : ${debugSnapshot.testSubject}"))
        container.addView(creerTexteStatistique("Dieu résolu : ${personality.displayName} (${debugSnapshot.godId})"))
        container.addView(creerTexteStatistique("personaType : ${debugSnapshot.personaType.name}"))
        container.addView(creerTexteStatistique("Ton : ${personality.toneIdentity}"))
        container.addView(creerTexteStatistique("Style pédagogique : ${personality.pedagogyStyle}"))
        container.addView(creerTexteStatistique("Style correction : ${personality.correctionStyle}"))
        container.addView(creerTexteStatistique("Style encouragement : ${personality.rewardStyle}"))
        container.addView(
            creerTexteStatistique(
                "Dérives interdites : ${personality.forbiddenDrifts.joinToString(", ").ifBlank { "—" }}"
            )
        )

        // E. FRAGMENTS & SIGNAUX DIVINS
        container.addView(creerSeparateur())
        container.addView(creerTitreSection("E. FRAGMENTS & SIGNAUX DIVINS"))
        val currentFragments = adaptive?.currentSubjectFragmentCount ?: debugSnapshot.fragmentsBySubject[debugSnapshot.testSubject] ?: 0
        container.addView(creerTexteStatistique("Matière test : ${debugSnapshot.testSubject}"))
        container.addView(creerTexteStatistique("Fragments injectés dans snapshot : $currentFragments"))
        container.addView(creerTexteStatistique("Signal matière test : ${interpretFragmentSignal(currentFragments)}"))
        if (debugSnapshot.fragmentsBySubject.isEmpty()) {
            container.addView(creerTexteInfo("Fragments par matière : aucun fragment exploitable."))
        } else {
            debugSnapshot.fragmentsBySubject.toSortedMap().forEach { (subject, count) ->
                container.addView(
                    creerTexteStatistique(
                        "$subject : $count (${interpretFragmentSignal(count)})"
                    )
                )
            }
        }
        debugSnapshot.fragmentParseMessage?.let {
            container.addView(creerTexteInfo("Fragments : $it"))
        }

        // F. AFFINITÉS DIVINES
        container.addView(creerSeparateur())
        container.addView(creerTitreSection("🤝 AFFINITÉS DIVINES"))
        afficherSectionAffinitesDivines(container, debugSnapshot.affinityProfile)

        // G. PREVIEW CONTEXTE IA
        container.addView(creerSeparateur())
        container.addView(creerTitreSection("G. PREVIEW CONTEXTE IA"))
        container.addView(creerTexteInfo("Preview locale — aucun appel Gemini"))
        container.addView(creerTexteInfo(truncateDebugText(debugSnapshot.adaptivePreview)))
    }

    private suspend fun buildDebugAiSnapshot(): DebugAiSnapshot = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(this@DebugAnalyticsActivity)
        val profileFromDb = try {
            db.iAristoteDao().getUserProfile()
        } catch (_: Exception) {
            null
        }
        val profile = profileFromDb ?: UserProfile()

        val recentAnalytics = try {
            db.userAnalyticsDao().getRecent(userId = 1, limit = 25)
        } catch (_: Exception) {
            emptyList()
        }
        val skillProfiles = try {
            db.userSkillProfileDao().getAllByUser(userId = 1)
        } catch (_: Exception) {
            emptyList()
        }
        val testSubject = resolveDebugTestSubject(recentAnalytics, skillProfiles)
        val godId = resolveGodIdForSubject(testSubject)
        val personaType = try {
            DivinePersonaManager.getPersonaForSubject(testSubject)
        } catch (_: Exception) {
            DivinePersonaType.ORACLE_NEUTRE
        }

        val (fragmentsBySubject, fragmentParseMessage) = safeParseFragments(profile.knowledgeFragments)
        val affinityProfile = GodAffinityManager.buildProfile(profile)
        val userSource = readUserSourceSnapshot(profileFromDb)

        var adaptiveSnapshot: PlayerAdaptiveSnapshot? = null
        var adaptiveResolverError: String? = null
        try {
            adaptiveSnapshot = PlayerContextResolver.resolve(
                context = this@DebugAnalyticsActivity,
                request = PlayerContextResolver.Request(subjectHint = testSubject)
            )
        } catch (e: Exception) {
            adaptiveResolverError = e.message ?: e::class.java.simpleName
        }

        var dialogueContext: PlayerContextResolver.PlayerDialogueContext? = null
        var dialogueResolverError: String? = null
        try {
            dialogueContext = PlayerContextResolver.resolve(this@DebugAnalyticsActivity)
        } catch (e: Exception) {
            dialogueResolverError = e.message ?: e::class.java.simpleName
        }

        val adaptivePreview = if (adaptiveSnapshot != null) {
            try {
                AdaptiveContextFormatter.buildAdaptiveContextNote(
                    snapshot = adaptiveSnapshot,
                    godId = godId,
                    dialogCategory = DialogCategory.PEDAGOGY,
                    triggerLabel = "DEBUG_LOCAL_CONTEXT",
                    explicitGoal = "Prévisualisation locale sans réseau",
                    extraInstructions = "Lecture seule debug local. Aucun appel Gemini."
                )
            } catch (e: Exception) {
                "Erreur génération preview locale : ${e.message}"
            }
        } else {
            "Impossible de générer une preview locale : snapshot adaptatif indisponible."
        }

        DebugAiSnapshot(
            profile = profile,
            affinityProfile = affinityProfile,
            userSourceDebug = userSource,
            testSubject = testSubject,
            godId = godId,
            personaType = personaType,
            adaptiveSnapshot = adaptiveSnapshot,
            dialogueContext = dialogueContext,
            adaptivePreview = adaptivePreview,
            recentAnalytics = recentAnalytics,
            skillProfiles = skillProfiles,
            fragmentsBySubject = fragmentsBySubject,
            fragmentParseMessage = fragmentParseMessage,
            adaptiveResolverError = adaptiveResolverError,
            dialogueResolverError = dialogueResolverError
        )
    }

    private fun afficherSectionAffinitesDivines(
        container: LinearLayout,
        affinityProfile: GodAffinityManager.GodAffinityProfile
    ) {
        if (affinityProfile.topGods.isEmpty()) {
            container.addView(creerTexteInfo("Aucune affinité divine calculable pour le moment."))
        } else {
            container.addView(creerTexteInfo("Top 3 affinités :"))
            affinityProfile.topGods.forEach { snapshot ->
                container.addView(
                    creerTexteStatistique(
                        "${snapshot.godDisplayName} — niveau ${snapshot.affinityLevel}/20 — ${snapshot.affinityLabel} — " +
                            "${snapshot.fragments} fragment(s) — ${snapshot.progressToNextLevelPercent}% — " +
                            "${snapshot.fragmentsNeededForNextLevel} restant(s)"
                    )
                )
            }
        }

        container.addView(creerTexteInfo("Détail par dieu :"))
        affinityProfile.profilesByGod.values.forEach { snapshot ->
            container.addView(
                creerTexteStatistique(
                    "${snapshot.godDisplayName} : ${snapshot.affinityLevel}/20, ${snapshot.affinityLabel}, " +
                        "${snapshot.fragments} fragment(s), ${snapshot.progressToNextLevelPercent}%, " +
                        "${snapshot.fragmentsNeededForNextLevel} restant(s)"
                )
            )
        }

        if (affinityProfile.unknownSubjects.isNotEmpty()) {
            container.addView(creerTexteInfo("Matières non reliées à un dieu :"))
            affinityProfile.unknownSubjects.toSortedMap().forEach { (subject, count) ->
                container.addView(creerTexteStatistique("$subject : $count fragment(s)"))
            }
        }
    }

    private fun readUserSourceSnapshot(profile: UserProfile?): UserSourceDebug {
        val prefs = getSharedPreferences("ReviZeusPrefs", MODE_PRIVATE)

        val roomPseudo = profile?.pseudo?.takeIf { it.isNotBlank() }
        val userPseudo = prefs.getString("USER_PSEUDO", null)?.takeIf { it.isNotBlank() }
        val heroPseudo = prefs.getString("hero_pseudo", null)?.takeIf { it.isNotBlank() }

        val roomAge = profile?.age
        val userAge = if (prefs.contains("USER_AGE")) prefs.getInt("USER_AGE", 15) else null
        val heroAge = if (prefs.contains("hero_age")) prefs.getInt("hero_age", 14) else null

        val roomClass = profile?.classLevel?.takeIf { it.isNotBlank() }
        val userClass = prefs.getString("USER_CLASS", null)?.takeIf { it.isNotBlank() }
        val heroClass = prefs.getString("hero_class", null)?.takeIf { it.isNotBlank() }

        val roomMood = profile?.mood?.takeIf { it.isNotBlank() }
        val userMood = prefs.getString("CURRENT_MOOD", null)?.takeIf { it.isNotBlank() }
        val heroMood = prefs.getString("hero_mood", null)?.takeIf { it.isNotBlank() }

        val roomLevel = profile?.level
        val userLevel = if (prefs.contains("USER_LEVEL")) prefs.getInt("USER_LEVEL", 1) else null
        val heroLevel = if (prefs.contains("hero_level")) prefs.getInt("hero_level", 1) else null

        val roomRank = profile?.rang?.takeIf { it.isNotBlank() }
        val userRank = prefs.getString("USER_RANK", null)?.takeIf { it.isNotBlank() }
        val heroRank = prefs.getString("hero_rank", null)?.takeIf { it.isNotBlank() }

        return UserSourceDebug(
            pseudoResolved = roomPseudo ?: userPseudo ?: heroPseudo ?: "Héros",
            ageResolved = roomAge ?: userAge ?: heroAge ?: 15,
            classResolved = roomClass ?: userClass ?: heroClass ?: "Terminale",
            moodResolved = roomMood ?: userMood ?: heroMood ?: "Prêt",
            levelResolved = roomLevel ?: userLevel ?: heroLevel ?: 1,
            rankResolved = roomRank ?: userRank ?: heroRank ?: "Mortel",
            pseudoSources = "Room=${roomPseudo ?: "∅"} | USER_PSEUDO=${userPseudo ?: "∅"} | hero_pseudo=${heroPseudo ?: "∅"}",
            ageSources = "Room=${roomAge ?: "∅"} | USER_AGE=${userAge ?: "∅"} | hero_age=${heroAge ?: "∅"}",
            classSources = "Room=${roomClass ?: "∅"} | USER_CLASS=${userClass ?: "∅"} | hero_class=${heroClass ?: "∅"}",
            moodSources = "Room=${roomMood ?: "∅"} | CURRENT_MOOD=${userMood ?: "∅"} | hero_mood=${heroMood ?: "∅"}",
            levelSources = "Room=${roomLevel ?: "∅"} | USER_LEVEL=${userLevel ?: "∅"} | hero_level=${heroLevel ?: "∅"}",
            rankSources = "Room=${roomRank ?: "∅"} | USER_RANK=${userRank ?: "∅"} | hero_rank=${heroRank ?: "∅"}"
        )
    }

    private fun resolveDebugTestSubject(
        recentAnalytics: List<UserAnalytics>,
        skillProfiles: List<UserSkillProfile>
    ): String {
        val analyticsSubject = recentAnalytics.firstOrNull { it.subject.isNotBlank() }?.subject
        if (!analyticsSubject.isNullOrBlank()) return analyticsSubject

        val skillSubject = skillProfiles.firstOrNull { it.subject.isNotBlank() }?.subject
        if (!skillSubject.isNullOrBlank()) return skillSubject

        return "Mathématiques"
    }

    private fun resolveGodIdForSubject(subject: String): String {
        val direct = PantheonConfig.findByMatiere(subject)?.divinite
        if (!direct.isNullOrBlank()) {
            return GodPersonalityEngine.normalizeGodId(direct)
        }

        return when (subject.trim().lowercase()) {
            "mathématiques", "mathematiques", "maths" -> "zeus"
            "français", "francais" -> "athena"
            "svt", "sciences de la vie et de la terre" -> "poseidon"
            "histoire" -> "ares"
            "art/musique", "art", "musique" -> "aphrodite"
            "langues", "anglais", "espagnol", "allemand", "italien" -> "hermes"
            "géographie", "geographie" -> "demeter"
            "physique-chimie", "physique", "chimie" -> "hephaestus"
            "philo/ses", "philosophie", "ses", "poésie", "poesie" -> "apollo"
            "vie & projets", "vie et projets" -> "prometheus"
            else -> "zeus"
        }
    }

    private fun safeParseFragments(raw: String?): Pair<Map<String, Int>, String?> {
        if (raw.isNullOrBlank()) return emptyMap<String, Int>() to "knowledgeFragments vide."

        return try {
            val json = org.json.JSONObject(raw)
            val map = linkedMapOf<String, Int>()
            json.keys().forEach { key ->
                map[key] = json.optInt(key, 0).coerceAtLeast(0)
            }
            map.toMap() to null
        } catch (e: Exception) {
            emptyMap<String, Int>() to "knowledgeFragments corrompu (${e.message ?: "format invalide"})."
        }
    }

    private fun interpretFragmentSignal(count: Int): String {
        return when {
            count <= 0 -> "aucun signal"
            count in 1..4 -> "exposition faible"
            count in 5..14 -> "progression active"
            count in 15..39 -> "ancrage solide"
            else -> "forte affinité matière"
        }
    }

    private fun truncateDebugText(text: String, maxChars: Int = 3500): String {
        if (text.length <= maxChars) return text
        return text.take(maxChars) + "\n… (tronqué pour debug)"
    }

    private fun yesNo(value: Boolean): String = if (value) "Oui" else "Non"

    private fun percent(value: Float?): String {
        if (value == null) return "—"
        return "${(value.coerceIn(0f, 1f) * 100f).toInt()}%"
    }

    private fun millisToSecondsLabel(value: Long?): String {
        if (value == null) return "—"
        return "${value / 1000f}s"
    }

    // ══════════════════════════════════════════════════════════════
    // WIDGETS DE CONSTRUCTION UI
    // ══════════════════════════════════════════════════════════════

    private fun creerEntete(): TextView {
        return TextView(this).apply {
            text = "⚙  DEBUG ML ANALYTICS"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextColor(Color.parseColor("#FFD700"))
            setPadding(0, dp(16), 0, dp(16))
            gravity = Gravity.CENTER
            typeface = try { resources.getFont(R.font.cinzel) }
                       catch (_: Exception) { Typeface.DEFAULT_BOLD }
        }
    }

    private fun creerTitreSection(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#E8D07A"))
            setPadding(0, dp(18), 0, dp(8))
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.06f
        }
    }

    private fun creerTexteInfo(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#666677"))
            setPadding(0, dp(5), 0, dp(5))
        }
    }

    private fun creerTexteStatistique(text: String): TextView {
        return TextView(this).apply {
            this.text = "▸  $text"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.parseColor("#CCCCCC"))
            setPadding(dp(10), dp(4), 0, dp(4))
        }
    }

    private fun creerSeparateur(): android.view.View {
        return android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
            setBackgroundColor(Color.parseColor("#22FFD700"))
        }
    }

    private fun creerCarteSkill(skill: com.revizeus.app.models.UserSkillProfile): LinearLayout {
        val masteryColor = when {
            skill.masteryLevel >= 0.90f -> "#4CAF50"
            skill.masteryLevel >= 0.75f -> "#8BC34A"
            skill.masteryLevel >= 0.60f -> "#FFC107"
            skill.masteryLevel >= 0.40f -> "#FF9800"
            else                        -> "#F44336"
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.parseColor("#0E0E18"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

        // Titre + pourcentage maîtrise sur même ligne
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(this).apply {
            text = "📚 ${skill.subject}"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#FFD700"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = "${(skill.masteryLevel * 100).toInt()}%"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Color.parseColor(masteryColor))
            typeface = Typeface.DEFAULT_BOLD
        })
        card.addView(row)

        // Métriques compactes sur une ligne
        val meta = "Réussite ${(skill.successRate * 100).toInt()}%  ·  " +
                   "Confiance ${(skill.confidence * 100).toInt()}%  ·  " +
                   "${skill.practiceCount} quiz  ·  " +
                   "${skill.avgResponseTime / 1000}s"
        card.addView(TextView(this).apply {
            text = meta
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(Color.parseColor("#666677"))
            setPadding(0, dp(5), 0, 0)
        })

        if (skill.needsReview) {
            card.addView(TextView(this).apply {
                text = "⚠  RÉVISION RECOMMANDÉE"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(Color.parseColor("#FF5722"))
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(5), 0, 0)
            })
        }

        return card
    }

    private fun creerCarteAnalytics(analytics: com.revizeus.app.models.UserAnalytics): LinearLayout {
        val resultColor = if (analytics.isCorrect) "#4CAF50" else "#F44336"
        val resultIcon  = if (analytics.isCorrect) "✓" else "✗"
        val resultLabel = if (analytics.isCorrect) "CORRECT" else "FAUX"

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.parseColor("#08080F"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        }

        card.addView(TextView(this).apply {
            text = "$resultIcon  ${analytics.subject} — $resultLabel  ·  ${analytics.responseTime / 1000}s"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor(resultColor))
            typeface = Typeface.DEFAULT_BOLD
        })

        val question = analytics.questionText?.take(90)?.let { "$it…" } ?: "—"
        card.addView(TextView(this).apply {
            text = question
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(Color.parseColor("#444455"))
            setPadding(0, dp(3), 0, 0)
        })

        return card
    }

    /**
     * Bouton retour premium — 2 couches superposées dans un FrameLayout :
     *   1. bg_temple_button  : fond texturé grec doré (ImageView FIT_XY)
     *   2. bg_textelayout    : surcouche légère pour profondeur (ImageView, alpha 0.45)
     *   3. Label Cinzel or   : texte centré, SFX + stopMusic + finish
     *
     * stopMusic() coupe bgm_debug avant de sortir (RÈGLE 8 respectée).
     */
    private fun creerBoutonRetour(): FrameLayout {
        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58)
            ).apply {
                topMargin    = dp(10)
                bottomMargin = dp(28)
                leftMargin   = dp(8)
                rightMargin  = dp(8)
            }
            isClickable = true
            isFocusable = true
        }

        // Couche 1 — fond texturé bg_temple_button
        frame.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            try { setImageResource(R.drawable.bg_temple_button) }
            catch (_: Exception) { setBackgroundColor(Color.parseColor("#1A1A2E")) }
            scaleType = ImageView.ScaleType.FIT_XY
        })

        // Couche 2 — bg_textelayout en surcouche, alpha réduit pour profondeur
        frame.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            try { setImageResource(R.drawable.bg_rpg_dialog) }
            catch (_: Exception) {}
            scaleType = ImageView.ScaleType.FIT_XY
            alpha = 0.35f
        })

        // Couche 3 — label cliquable Cinzel or centré
        frame.addView(TextView(this).apply {
            text = "← RETOUR À L'OLYMPE"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Color.parseColor("#FFD700"))
            gravity = Gravity.CENTER
            typeface = try { resources.getFont(R.font.cinzel) }
                       catch (_: Exception) { Typeface.DEFAULT_BOLD }
            letterSpacing = 0.06f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                try { jouerSfx(R.raw.sfx_avatar_confirm) } catch (_: Exception) {}
                try { SoundManager.stopMusic() } catch (_: Exception) {}
                finish()
            }
        })

        return frame
    }

    // ══════════════════════════════════════════════════════════════
    // COMPANION — Animation point debug rouge (Dashboard)
    // ══════════════════════════════════════════════════════════════

    companion object {
        /**
         * Lance la pulsation AlphaAnimation sur le point rouge Debug
         * dans le Dashboard (binding.viewDebugDot).
         *
         * USAGE dans DashboardActivity.setupMenu() :
         * ```kotlin
         * DebugAnalyticsActivity.animerPointDebug(binding.viewDebugDot)
         * ```
         *
         * Cycle : 25% → 100% → 25% opacité, 900ms par demi-cycle, boucle infinie.
         * Silencieux en cas d'erreur — l'animation est cosmétique, pas critique.
         */
        fun animerPointDebug(vue: android.view.View) {
            try {
                AlphaAnimation(0.25f, 1.0f).apply {
                    duration = 900L
                    repeatCount = Animation.INFINITE
                    repeatMode = Animation.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                    vue.startAnimation(this)
                }
            } catch (_: Exception) {}
        }
    }

    // ══════════════════════════════════════════════════════════════
    // HELPER DP
    // ══════════════════════════════════════════════════════════════

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
    ).toInt()
}

package com.revizeus.app

/**
 * ============================================================
 * GodSpeechAnimator_Integration.kt — RéviZeus v9
 * Guide d'intégration du tremblement chibi dans chaque Activity.
 *
 * RÈGLE GÉNÉRALE :
 *   1. Déclarer : private val godAnim = GodSpeechAnimator()
 *   2. Déclarer : private var typewriterJob: Job? = null
 *                 (tu l'as déjà dans AuthActivity ✅)
 *   3. Remplacer afficherTexteRPG() par typewriteWithShake()
 *   4. Dans onPause() → typewriterJob?.cancel() + godAnim.stopSpeaking(chibiView)
 *   5. Dans onDestroy() → godAnim.release()
 *
 * IDs des chibi par Activity (depuis les XML) :
 *   LoginActivity        → imgZeusDialog    (dans ConstraintLayout dialog)
 *   AuthActivity         → imgZeusDialog    ✅ déjà typewriterJob
 *   GenderActivity       → imgZeusDialog    (dans dialog)
 *   MoodActivity         → ivZeusMood       (grande vue centrale)
 *   QuizActivity         → ivZeusQuiz       (grande vue)
 *   QuizResultActivity   → imgZeusResult    (dans dialog)
 *   TrainingSelectActivity→ pas de chibi dialog (à ajouter si besoin)
 * ============================================================
 */

// ════════════════════════════════════════════════════════════
// 1. AuthActivity — PATCH (le plus important, déjà typewriter)
//    Remplace afficherTexteRPG() par typewriteWithShake()
// ════════════════════════════════════════════════════════════
/*
// AVANT (existant) :
private fun afficherTexteRPG(texteComplet: String) {
    typewriterJob?.cancel()
    typewriterJob = lifecycleScope.launch {
        binding.tvZeusSpeech.text = ""
        for (i in texteComplet.indices) {
            binding.tvZeusSpeech.text = texteComplet.substring(0, i + 1)
            try { SoundManager.playSFXLow(this@AuthActivity, R.raw.sfx_dialogue_blip) } catch (_: Exception) {}
            delay(35)
        }
    }
}

// APRÈS (remplacer par) :
private val godAnim = GodSpeechAnimator()

private fun afficherTexteRPG(texteComplet: String) {
    typewriterJob?.cancel()
    typewriterJob = godAnim.typewriteSimple(
        scope     = lifecycleScope,
        chibiView = binding.imgZeusDialog,
        textView  = binding.tvZeusSpeech,
        text      = texteComplet,
        delayMs   = 35L,
        context   = this@AuthActivity
    )
}

override fun onPause() {
    super.onPause()
    SoundManager.pauseMusic()
    typewriterJob?.cancel()
    godAnim.stopSpeaking(binding.imgZeusDialog)
}

override fun onDestroy() {
    super.onDestroy()
    godAnim.release(binding.imgZeusDialog)
}
*/

// ════════════════════════════════════════════════════════════
// 2. LoginActivity
// ════════════════════════════════════════════════════════════
/*
private val godAnim = GodSpeechAnimator()
private var typewriterJob: Job? = null

// Remplacer chaque appel existant à afficherTexteRPG() par :
private fun afficherTexteRPG(texteComplet: String) {
    typewriterJob?.cancel()
    typewriterJob = godAnim.typewriteSimple(
        scope     = lifecycleScope,
        chibiView = binding.imgZeusDialog,
        textView  = binding.tvZeusSpeech,
        text      = texteComplet,
        delayMs   = 35L,
        context   = this@LoginActivity
    )
}

override fun onPause() {
    super.onPause()
    SoundManager.pauseMusic()
    typewriterJob?.cancel()
    godAnim.stopSpeaking(binding.imgZeusDialog)
}
override fun onDestroy() {
    super.onDestroy()
    godAnim.release(binding.imgZeusDialog)
}
*/

// ════════════════════════════════════════════════════════════
// 3. GenderActivity — même pattern que Login/Auth
// ════════════════════════════════════════════════════════════
/*
private val godAnim = GodSpeechAnimator()
private var typewriterJob: Job? = null

private fun afficherTexteRPG(texteComplet: String) {
    typewriterJob?.cancel()
    typewriterJob = godAnim.typewriteSimple(
        scope     = lifecycleScope,
        chibiView = binding.imgZeusDialog,
        textView  = binding.tvZeusSpeech,
        text      = texteComplet,
        delayMs   = 35L,
        context   = this@GenderActivity
    )
}

override fun onPause() {
    super.onPause()
    SoundManager.pauseMusic()
    typewriterJob?.cancel()
    godAnim.stopSpeaking(binding.imgZeusDialog)
}
override fun onDestroy() {
    super.onDestroy()
    godAnim.release(binding.imgZeusDialog)
}
*/

// ════════════════════════════════════════════════════════════
// 4. MoodActivity
//    ivZeusMood = grande image Zeus centrale
//    tvZeusSpeech ou tvMoodSpeech = à adapter selon ton XML
// ════════════════════════════════════════════════════════════
/*
private val godAnim = GodSpeechAnimator()
private var typewriterJob: Job? = null

private fun afficherTexteRPG(texteComplet: String) {
    typewriterJob?.cancel()
    typewriterJob = godAnim.typewriteSimple(
        scope     = lifecycleScope,
        chibiView = binding.ivZeusMood,
        textView  = binding.tvZeusSpeech,  // adapter selon ton XML
        text      = texteComplet,
        delayMs   = 35L,
        context   = this@MoodActivity
    )
}

override fun onPause() {
    super.onPause()
    SoundManager.pauseMusic()
    typewriterJob?.cancel()
    godAnim.stopSpeaking(binding.ivZeusMood)
}
override fun onDestroy() {
    super.onDestroy()
    godAnim.release(binding.ivZeusMood)
}
*/

// ════════════════════════════════════════════════════════════
// 5. QuizActivity
//    ivZeusQuiz = grande vue Zeus
//    tvFeedback = texte feedback après réponse
//
//    ⚡ USAGE SPÉCIAL : intensité variable selon bonne/mauvaise réponse
// ════════════════════════════════════════════════════════════
/*
private val godAnim = GodSpeechAnimator()
private var typewriterJob: Job? = null

// Appelé quand l'IA renvoie le feedback de la question
private fun afficherFeedback(texte: String, bonneReponse: Boolean) {
    typewriterJob?.cancel()

    if (!bonneReponse) {
        // Impact fort d'abord → puis tremblement normal + texte
        godAnim.impactShake(binding.ivZeusQuiz) {
            typewriterJob = godAnim.typewriteSimple(
                scope     = lifecycleScope,
                chibiView = binding.ivZeusQuiz,
                textView  = binding.tvFeedback,
                text      = texte,
                delayMs   = 35L,
                context   = this@QuizActivity
            )
        }
    } else {
        // Bonne réponse : tremblement joyeux léger
        typewriterJob = godAnim.typewriteWithShake(
            scope      = lifecycleScope,
            chibiView  = binding.ivZeusQuiz,
            textView   = binding.tvFeedback,
            text       = texte,
            delayMs    = 28L,         // un peu plus rapide pour la récompense
            intensity  = 1.5f,        // tremblement plus doux = joie légère
            onChar     = {
                try { SoundManager.playSFXLow(this@QuizActivity, R.raw.sfx_dialogue_blip) }
                catch (_: Exception) {}
            }
        )
    }
}

// Pour les questions (sans feedback chibi, juste le texte) :
private fun afficherQuestion(texte: String) {
    typewriterJob?.cancel()
    typewriterJob = godAnim.typewriteWithShake(
        scope      = lifecycleScope,
        chibiView  = binding.ivZeusQuiz,
        textView   = binding.tvQuestion,
        text       = texte,
        delayMs    = 22L,             // plus rapide pour les questions
        intensity  = 2.0f,
        onChar     = {
            try { SoundManager.playSFXLow(this@QuizActivity, R.raw.sfx_dialogue_blip) }
            catch (_: Exception) {}
        }
    )
}

override fun onPause() {
    super.onPause()
    SoundManager.pauseMusic()
    typewriterJob?.cancel()
    godAnim.stopSpeaking(binding.ivZeusQuiz)
}
override fun onDestroy() {
    super.onDestroy()
    godAnim.release(binding.ivZeusQuiz)
}
*/

// ════════════════════════════════════════════════════════════
// 6. QuizResultActivity
//    imgZeusResult = portrait Zeus dans bulle résultat
//    tvZeusMessage = message Zeus après le quiz
// ════════════════════════════════════════════════════════════
/*
private val godAnim = GodSpeechAnimator()
private var typewriterJob: Job? = null

// Appelé quand on affiche le verdict de Zeus
private fun afficherVerdictZeus(texte: String) {
    typewriterJob?.cancel()
    typewriterJob = godAnim.typewriteSimple(
        scope     = lifecycleScope,
        chibiView = binding.imgZeusResult,
        textView  = binding.tvZeusMessage,
        text      = texte,
        delayMs   = 40L,    // légèrement plus lent pour le verdict solennel
        context   = this@QuizResultActivity
    )
}

override fun onPause() {
    super.onPause()
    SoundManager.pauseMusic()
    typewriterJob?.cancel()
    godAnim.stopSpeaking(binding.imgZeusResult)
}
override fun onDestroy() {
    super.onDestroy()
    godAnim.release(binding.imgZeusResult)
}
*/

// ════════════════════════════════════════════════════════════
// NOTE SUR LE TABLEAU DES INTENSITÉS
// ════════════════════════════════════════════════════════════
/*
    Intensité    Situation
    ─────────    ──────────────────────────────
    1.5f         Joie légère, félicitation
    2.0f         Discours normal, question
    2.5f         Discours important (défaut)
    3.5f         Mise en garde, avertissement
    4.0f         Cri, impact, erreur forte
    impactShake  Coup de tonnerre, mauvaise réponse
*/

package com.revizeus.app.models

/**
 * ═══════════════════════════════════════════════════════════════
 * QUESTION TYPE — Types de questions supportés
 * ═══════════════════════════════════════════════════════════════
 */
enum class QuestionType(val displayName: String, val icon: String) {
    QCM_SIMPLE("QCM Classique", "📝"),
    TRUE_FALSE("Vrai/Faux", "✓✗"),
    QCM_MULTIPLE("QCM Multiple", "☑️"),
    OPEN_SHORT("Réponse Courte", "✏️"),
    IMAGE_QCM("QCM Image", "🖼️"),
    IMAGE_IDENTIFY("Identifier Image", "🔍");
}

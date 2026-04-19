package com.revizeus.app.ui

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.revizeus.app.models.InsightType
import com.revizeus.app.models.UserInsight

/**
 * ═══════════════════════════════════════════════════════════════
 * DASHBOARD INSIGHTS WIDGET — Widget visuel "Insights du jour"
 * ═══════════════════════════════════════════════════════════════
 * 
 * BLOC 3C — DASHBOARD INSIGHTS
 * 
 * Rôle :
 * Affiche les insights les plus importants dans le dashboard
 * sous forme de cartes visuelles colorées.
 * 
 * Design :
 * - Carte par insight (max 3 affichées)
 * - Couleur selon type d'insight
 * - Icône + message + action
 * - Style cohérent avec l'app
 * 
 * ═══════════════════════════════════════════════════════════════
 */
object DashboardInsightsWidget {
    
    /**
     * Crée le container principal avec tous les insights.
     * 
     * @param context Context Android
     * @param insights Liste d'insights (triés par sévérité)
     * @param maxVisible Nombre max d'insights affichés
     * @return LinearLayout contenant les cartes
     */
    fun buildInsightsContainer(
        context: Context,
        insights: List<UserInsight>,
        maxVisible: Int = 3
    ): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Filtrer les insights actionnables et prendre les top N
        val topInsights = insights
            .filter { it.isAlert() || it.isActionRequired() }
            .take(maxVisible)
        
        if (topInsights.isEmpty()) {
            // Message si aucun insight
            container.addView(buildEmptyState(context))
        } else {
            // Créer une carte par insight
            topInsights.forEach { insight ->
                container.addView(buildInsightCard(context, insight))
            }
        }
        
        return container
    }
    
    /**
     * Crée une carte pour un insight individuel.
     */
    private fun buildInsightCard(context: Context, insight: UserInsight): CardView {
        val card = CardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(context, 16), dpToPx(context, 8), dpToPx(context, 16), dpToPx(context, 8))
            }
            radius = dpToPx(context, 12).toFloat()
            cardElevation = dpToPx(context, 4).toFloat()
            setCardBackgroundColor(getColorForInsightType(insight.type))
        }
        
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(context, 16), dpToPx(context, 16), dpToPx(context, 16), dpToPx(context, 16))
        }
        
        // Ligne 1 : Icône + Titre
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        val icon = TextView(context).apply {
            text = getIconForInsightType(insight.type)
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(context, 8)
            }
        }
        
        val title = TextView(context).apply {
            text = getTitleForInsightType(insight.type, insight.subject, insight.topic)
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            textSize = 16f
        }
        
        headerLayout.addView(icon)
        headerLayout.addView(title)
        content.addView(headerLayout)
        
        // Ligne 2 : Message utilisateur
        val message = TextView(context).apply {
            text = insight.toUserMessage()
            setTextColor(Color.parseColor("#F0F0F0"))
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(context, 8)
            }
        }
        content.addView(message)
        
        // Ligne 3 : Action recommandée
        val action = TextView(context).apply {
            text = "💡 ${insight.actionable}"
            setTextColor(Color.parseColor("#FFFACD"))
            setTypeface(null, android.graphics.Typeface.ITALIC)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(context, 12)
            }
        }
        content.addView(action)
        
        card.addView(content)
        return card
    }
    
    /**
     * État vide si aucun insight.
     */
    private fun buildEmptyState(context: Context): CardView {
        val card = CardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(context, 16), dpToPx(context, 8), dpToPx(context, 16), dpToPx(context, 8))
            }
            radius = dpToPx(context, 12).toFloat()
            cardElevation = dpToPx(context, 4).toFloat()
            setCardBackgroundColor(Color.parseColor("#4CAF50"))
        }
        
        val message = TextView(context).apply {
            text = "✨ Aucun problème détecté !\nContinue comme ça, héros !"
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(dpToPx(context, 24), dpToPx(context, 24), dpToPx(context, 24), dpToPx(context, 24))
        }
        
        card.addView(message)
        return card
    }
    
    /**
     * Couleur de carte selon type d'insight.
     */
    private fun getColorForInsightType(type: InsightType): Int {
        return when (type) {
            InsightType.WEAKNESS -> Color.parseColor("#E53935")       // Rouge
            InsightType.SPEED_ISSUE -> Color.parseColor("#FB8C00")    // Orange
            InsightType.RUSHING -> Color.parseColor("#FF6F00")        // Orange foncé
            InsightType.CONFUSION -> Color.parseColor("#8E24AA")      // Violet
            InsightType.REGRESSION -> Color.parseColor("#D32F2F")     // Rouge foncé
            InsightType.PROGRESS -> Color.parseColor("#43A047")       // Vert
            InsightType.MASTERY -> Color.parseColor("#1E88E5")        // Bleu
            InsightType.INSTABILITY -> Color.parseColor("#6A1B9A")    // Violet foncé
            InsightType.NEED_BREAK -> Color.parseColor("#F57C00")     // Orange chaleureux
        }
    }
    
    /**
     * Icône selon type d'insight.
     */
    private fun getIconForInsightType(type: InsightType): String {
        return when (type) {
            InsightType.WEAKNESS -> "⚠️"
            InsightType.SPEED_ISSUE -> "⏱️"
            InsightType.RUSHING -> "⚡"
            InsightType.CONFUSION -> "🔄"
            InsightType.REGRESSION -> "📉"
            InsightType.PROGRESS -> "📈"
            InsightType.MASTERY -> "⭐"
            InsightType.INSTABILITY -> "🎲"
            InsightType.NEED_BREAK -> "😴"
        }
    }
    
    /**
     * Titre selon type d'insight.
     */
    private fun getTitleForInsightType(type: InsightType, subject: String, topic: String?): String {
        val themeText = topic ?: subject
        return when (type) {
            InsightType.WEAKNESS -> "Difficulté : $themeText"
            InsightType.SPEED_ISSUE -> "Lenteur détectée"
            InsightType.RUSHING -> "Attention à la précipitation"
            InsightType.CONFUSION -> "Confusion sur concepts"
            InsightType.REGRESSION -> "Baisse récente"
            InsightType.PROGRESS -> "Belle progression !"
            InsightType.MASTERY -> "Maîtrise : $themeText"
            InsightType.INSTABILITY -> "Résultats variables"
            InsightType.NEED_BREAK -> "Besoin de pause"
        }
    }
    
    /**
     * Convertit dp en pixels.
     */
    private fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}

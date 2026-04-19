package com.revizeus.app.core

object QuizTimerManager {

    /**
     * NOUVELLE RÈGLE DEMANDÉE :
     * 0 - 10 ans → 30s
     * 11 - 20 ans → 25s
     * 21+ → 20s
     *
     * Ancienne règle remplacée proprement.
     */
    fun getSecondsForAge(age: Int): Int {
        return when {
            age <= 10 -> 30
            age in 11..20 -> 25
            else -> 20
        }
    }
}
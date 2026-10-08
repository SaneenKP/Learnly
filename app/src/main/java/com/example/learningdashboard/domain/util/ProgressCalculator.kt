package com.example.learningdashboard.domain.util

object ProgressCalculator {
    /**
     * Calculates the integer completion percentage based on completed and total lessons.
     * Pure business logic function:
     * - Returns 0 if totalLessons <= 0 or completedLessons <= 0.
     * - Caps values above 100 at 100.
     * - Coerces integer percentage safely within 0..100.
     */
    fun calculateProgress(completedLessons: Int, totalLessons: Int): Int {
        if (totalLessons <= 0 || completedLessons <= 0) {
            return 0
        }
        val calculated = (completedLessons.toDouble() / totalLessons * 100).toInt()
        return calculated.coerceIn(0, 100)
    }
}

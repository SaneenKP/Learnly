package com.example.learningdashboard.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCalculatorTest {

    @Test
    fun calculateProgress_partialProgress_returnsCorrectPercentage() {
        // Python Programming scenario: 13 / 20 = 65%
        val result = ProgressCalculator.calculateProgress(completedLessons = 13, totalLessons = 20)
        assertEquals(65, result)
    }

    @Test
    fun calculateProgress_zeroCompleted_returnsZero() {
        val result = ProgressCalculator.calculateProgress(completedLessons = 0, totalLessons = 20)
        assertEquals(0, result)
    }

    @Test
    fun calculateProgress_allCompleted_returnsOneHundred() {
        val result = ProgressCalculator.calculateProgress(completedLessons = 20, totalLessons = 20)
        assertEquals(100, result)
    }

    @Test
    fun calculateProgress_zeroTotalLessons_returnsZeroSafely() {
        val result = ProgressCalculator.calculateProgress(completedLessons = 0, totalLessons = 0)
        assertEquals(0, result)
    }

    @Test
    fun calculateProgress_generativeAiDiscrepancy_calculatesDerivedIntegerPercentage() {
        // Generative AI: 6 / 16 = 37.5% -> 37% integer
        val result = ProgressCalculator.calculateProgress(completedLessons = 6, totalLessons = 16)
        assertEquals(37, result)
    }

    @Test
    fun calculateProgress_fullStack_returnsCorrectPercentage() {
        // Full Stack: 7 / 28 = 25%
        val result = ProgressCalculator.calculateProgress(completedLessons = 7, totalLessons = 28)
        assertEquals(25, result)
    }

    @Test
    fun calculateProgress_completedExceedsTotal_capsAtOneHundred() {
        val result = ProgressCalculator.calculateProgress(completedLessons = 25, totalLessons = 20)
        assertEquals(100, result)
    }

    @Test
    fun calculateProgress_negativeCompleted_returnsZero() {
        val result = ProgressCalculator.calculateProgress(completedLessons = -5, totalLessons = 20)
        assertEquals(0, result)
    }
}

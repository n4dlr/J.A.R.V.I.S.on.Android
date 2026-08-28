package com.example

import com.example.jarvis.domain.model.IntentConfidence
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.StructuredIntent
import com.example.jarvis.security.CommandSanitizer
import com.example.jarvis.security.RiskManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RiskManagerTest {

    private lateinit var sanitizer: CommandSanitizer
    private lateinit var riskManager: RiskManager

    @Before
    fun setUp() {
        sanitizer = CommandSanitizer()
        riskManager = RiskManager(sanitizer)
    }

    @Test
    fun `low risk operations do not require confirmation`() {
        val batteryIntent = StructuredIntent(
            intentId = "GET_BATTERY",
            rawQuery = "batareya nə qədərdir",
            normalizedQuery = "batareya ne qederdir",
            confidence = IntentConfidence.EXACT_DETERMINISTIC
        )

        val assessment = riskManager.assessRisk(batteryIntent, RiskLevel.LOW)
        assertEquals(RiskLevel.LOW, assessment.riskLevel)
        assertFalse(assessment.requiresExplicitConfirmation)
    }

    @Test
    fun `high risk operations require explicit confirmation`() {
        val lockIntent = StructuredIntent(
            intentId = "LOCK_SCREEN",
            rawQuery = "ekranı kilidlə",
            normalizedQuery = "ekrani kilidle",
            confidence = IntentConfidence.EXACT_DETERMINISTIC
        )

        val assessment = riskManager.assessRisk(lockIntent, RiskLevel.HIGH)
        assertEquals(RiskLevel.HIGH, assessment.riskLevel)
        assertTrue(assessment.requiresExplicitConfirmation)
    }

    @Test
    fun `suspicious arbitrary shell command is flagged as CRITICAL risk`() {
        val exploitIntent = StructuredIntent(
            intentId = "ARBITRARY",
            rawQuery = "rm -rf /data/data; su",
            normalizedQuery = "rm -rf",
            confidence = IntentConfidence.UNKNOWN
        )

        val assessment = riskManager.assessRisk(exploitIntent, RiskLevel.LOW)
        assertEquals(RiskLevel.CRITICAL, assessment.riskLevel)
        assertTrue(assessment.requiresExplicitConfirmation)
        assertTrue(assessment.securityWarnings.isNotEmpty())
    }
}

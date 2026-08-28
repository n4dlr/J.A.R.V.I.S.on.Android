package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.ai.matcher.DeterministicIntentMatcher
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.domain.model.CapabilityStatus
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.StructuredIntent
import com.example.jarvis.domain.model.IntentConfidence
import com.example.jarvis.security.CommandSanitizer
import com.example.jarvis.security.RiskManager
import com.example.jarvis.tools.CapabilityDetector
import com.example.jarvis.tools.ToolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class Phase3ToolsTest {

    private lateinit var context: Context
    private lateinit var registry: ToolRegistry
    private lateinit var matcher: DeterministicIntentMatcher
    private lateinit var riskManager: RiskManager
    private lateinit var detector: CapabilityDetector

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        registry = ToolRegistry()
        matcher = DeterministicIntentMatcher(AzerbaijaniTextNormalizer())
        riskManager = RiskManager(CommandSanitizer())
        detector = CapabilityDetector(context)
    }

    @Test
    fun `tool registry registers all Phase 3 tool categories`() {
        val tools = registry.getAllTools()
        assertTrue("Expected at least 50 tools, got ${tools.size}", tools.size >= 50)

        // System
        assertNotNull(registry.getTool("SCREEN_CONTROL"))
        assertNotNull(registry.getTool("OPEN_HOME"))
        assertNotNull(registry.getTool("OPEN_RECENTS"))
        assertNotNull(registry.getTool("OPEN_NOTIFICATIONS"))
        assertNotNull(registry.getTool("OPEN_QUICK_SETTINGS"))
        assertNotNull(registry.getTool("LOCK_SCREEN"))
        assertNotNull(registry.getTool("OPEN_SETTINGS"))

        // Apps
        assertNotNull(registry.getTool("LIST_APPS"))
        assertNotNull(registry.getTool("APP_INFO"))
        assertNotNull(registry.getTool("OPEN_APP_SETTINGS"))
        assertNotNull(registry.getTool("REQUEST_APP_PERMISSION"))
        assertNotNull(registry.getTool("OPEN_PLAY_STORE"))
        assertNotNull(registry.getTool("OPEN_APP"))

        // Files
        assertNotNull(registry.getTool("SEARCH_FILES"))
        assertNotNull(registry.getTool("OPEN_FILE"))
        assertNotNull(registry.getTool("SHARE_FILE"))
        assertNotNull(registry.getTool("COPY_FILE"))
        assertNotNull(registry.getTool("MOVE_FILE"))
        assertNotNull(registry.getTool("RENAME_FILE"))
        assertNotNull(registry.getTool("DELETE_FILE"))
        assertNotNull(registry.getTool("CREATE_FOLDER"))
        assertNotNull(registry.getTool("STORAGE_INFO"))

        // Battery
        assertNotNull(registry.getTool("BATTERY_STATUS"))
        assertNotNull(registry.getTool("BATTERY_TEMPERATURE"))
        assertNotNull(registry.getTool("CHARGING_STATUS"))
        assertNotNull(registry.getTool("BATTERY_SAVER_STATUS"))
        assertNotNull(registry.getTool("OPEN_BATTERY_SETTINGS"))

        // Performance
        assertNotNull(registry.getTool("CPU_STATUS"))
        assertNotNull(registry.getTool("DEVICE_INFO"))
        assertNotNull(registry.getTool("GET_RAM"))

        // Network
        assertNotNull(registry.getTool("WIFI_STATUS"))
        assertNotNull(registry.getTool("WIFI_SETTINGS"))
        assertNotNull(registry.getTool("NETWORK_STATUS"))
        assertNotNull(registry.getTool("IP_INFO"))
        assertNotNull(registry.getTool("BLUETOOTH_STATUS"))
        assertNotNull(registry.getTool("BLUETOOTH_SETTINGS"))
        assertNotNull(registry.getTool("MOBILE_NETWORK_SETTINGS"))

        // Audio
        assertNotNull(registry.getTool("GET_VOLUME"))
        assertNotNull(registry.getTool("SET_VOLUME"))
        assertNotNull(registry.getTool("MUTE"))
        assertNotNull(registry.getTool("UNMUTE"))
        assertNotNull(registry.getTool("MEDIA_PLAY"))
        assertNotNull(registry.getTool("MEDIA_PAUSE"))
        assertNotNull(registry.getTool("MEDIA_NEXT"))
        assertNotNull(registry.getTool("MEDIA_PREVIOUS"))

        // Notifications
        assertNotNull(registry.getTool("READ_NOTIFICATIONS"))
        assertNotNull(registry.getTool("LIST_NOTIFICATIONS"))
        assertNotNull(registry.getTool("REMOVE_NOTIFICATION"))
        assertNotNull(registry.getTool("NOTIFICATION_STATUS"))

        // Camera
        assertNotNull(registry.getTool("OPEN_CAMERA"))
        assertNotNull(registry.getTool("TAKE_PHOTO"))
        assertNotNull(registry.getTool("RECORD_VIDEO"))

        // Contacts & Calls
        assertNotNull(registry.getTool("SEARCH_CONTACT"))
        assertNotNull(registry.getTool("CREATE_CONTACT"))
        assertNotNull(registry.getTool("OPEN_CONTACTS"))
        assertNotNull(registry.getTool("DIAL_NUMBER"))
        assertNotNull(registry.getTool("CALL_CONTACT"))
        assertNotNull(registry.getTool("OPEN_CALL_LOG"))

        // SMS
        assertNotNull(registry.getTool("OPEN_MESSAGES"))
        assertNotNull(registry.getTool("COMPOSE_SMS"))

        // Location
        assertNotNull(registry.getTool("GET_LOCATION"))
        assertNotNull(registry.getTool("OPEN_LOCATION_SETTINGS"))
        assertNotNull(registry.getTool("OPEN_MAP"))

        // Voice
        assertNotNull(registry.getTool("SPEAK"))
        assertNotNull(registry.getTool("START_LISTENING"))
        assertNotNull(registry.getTool("STOP_LISTENING"))

        // Alarm & Reminders
        assertNotNull(registry.getTool("CREATE_ALARM"))
        assertNotNull(registry.getTool("LIST_ALARMS"))
        assertNotNull(registry.getTool("DELETE_ALARM"))
        assertNotNull(registry.getTool("CREATE_REMINDER"))
        assertNotNull(registry.getTool("LIST_REMINDERS"))
        assertNotNull(registry.getTool("DELETE_REMINDER"))

        // Calendar
        assertNotNull(registry.getTool("CREATE_EVENT"))
        assertNotNull(registry.getTool("LIST_EVENTS"))
        assertNotNull(registry.getTool("DELETE_EVENT"))

        // Browser
        assertNotNull(registry.getTool("OPEN_URL"))
        assertNotNull(registry.getTool("WEB_SEARCH"))
        assertNotNull(registry.getTool("OPEN_BROWSER"))

        // Accessibility
        assertNotNull(registry.getTool("CLICK_UI_ELEMENT"))
        assertNotNull(registry.getTool("SCROLL"))
        assertNotNull(registry.getTool("READ_VISIBLE_TEXT"))
        assertNotNull(registry.getTool("GO_BACK"))
        assertNotNull(registry.getTool("GO_HOME"))
        assertNotNull(registry.getTool("INTERACT_WITH_SUPPORTED_UI"))
    }

    @Test
    fun `capability detector accurately detects permissions and support`() {
        val supportedInfo = detector.detect("TEST_TOOL", emptyList())
        assertEquals(CapabilityStatus.SUPPORTED, supportedInfo.status)

        val permInfo = detector.detect("RECORD_AUDIO_TOOL", listOf(android.Manifest.permission.RECORD_AUDIO))
        assertEquals(CapabilityStatus.PERMISSION_REQUIRED, permInfo.status)
        assertTrue(permInfo.missingPermissions.contains(android.Manifest.permission.RECORD_AUDIO))
    }

    @Test
    fun `matcher correctly identifies phase 3 deterministic intents`() {
        // Battery temp
        val tempIntent = matcher.match("batareya temperaturu neçədir?")
        assertNotNull(tempIntent)
        assertEquals("BATTERY_TEMPERATURE", tempIntent?.intentId)

        // Charging status
        val chargeIntent = matcher.match("telefon şarj olurmu?")
        assertNotNull(chargeIntent)
        assertEquals("CHARGING_STATUS", chargeIntent?.intentId)

        // CPU status
        val cpuIntent = matcher.match("CPU vəziyyəti")
        assertNotNull(cpuIntent)
        assertEquals("CPU_STATUS", cpuIntent?.intentId)

        // Device info
        val devIntent = matcher.match("telefon haqqında məlumat ver")
        assertNotNull(devIntent)
        assertEquals("DEVICE_INFO", devIntent?.intentId)

        // Wi-Fi status
        val wifiIntent = matcher.match("Wi-Fi vəziyyəti necədir?")
        assertNotNull(wifiIntent)
        assertEquals("WIFI_STATUS", wifiIntent?.intentId)

        // Media controls
        val playIntent = matcher.match("musiqini oynat")
        assertNotNull(playIntent)
        assertEquals("MEDIA_PLAY", playIntent?.intentId)

        val pauseIntent = matcher.match("mahnını saxla")
        assertNotNull(pauseIntent)
        assertEquals("MEDIA_PAUSE", pauseIntent?.intentId)

        // Alarm
        val alarmIntent = matcher.match("saat 7 üçün zəngli saat qur")
        assertNotNull(alarmIntent)
        assertEquals("CREATE_ALARM", alarmIntent?.intentId)

        // Web search
        val searchIntent = matcher.match("google-da axtar süni intellekt")
        assertNotNull(searchIntent)
        assertEquals("WEB_SEARCH", searchIntent?.intentId)

        // URL
        val urlIntent = matcher.match("https://github.com səhifəsini aç")
        assertNotNull(urlIntent)
        assertEquals("OPEN_URL", urlIntent?.intentId)

        // Maps
        val mapIntent = matcher.match("Bakı xəritəsini aç")
        assertNotNull(mapIntent)
        assertEquals("OPEN_MAP", mapIntent?.intentId)

        // Home navigation
        val homeIntent = matcher.match("ana ekrana get")
        assertNotNull(homeIntent)
        assertEquals("OPEN_HOME", homeIntent?.intentId)
    }

    @Test
    fun `risk manager properly assigns high risk and creates confirmation prompt`() {
        val deleteIntent = StructuredIntent(
            intentId = "DELETE_FILE",
            rawQuery = "faylı sil",
            normalizedQuery = "fayli sil",
            confidence = IntentConfidence.EXACT_DETERMINISTIC,
            arguments = mapOf("path" to "/sdcard/test.txt")
        )

        val assessment = riskManager.assessRisk(deleteIntent, RiskLevel.HIGH)
        assertTrue(assessment.requiresExplicitConfirmation)
        assertEquals(RiskLevel.HIGH, assessment.riskLevel)

        val prompt = riskManager.createPendingConfirmation("DELETE_FILE", deleteIntent, assessment)
        assertTrue(prompt.userPromptText.contains("silmək istədiyinizə əminsiniz"))
    }

    @Test
    fun `battery status tool execution returns real battery metrics`() = runBlocking {
        val batteryTool = registry.getTool("BATTERY_STATUS")
        assertNotNull(batteryTool)
        val result = batteryTool!!.execute(context, emptyMap())
        assertNotNull(result)
        assertTrue(result.isSuccess)
        assertTrue(result.outputMessage.contains("Batareya"))
    }

    @Test
    fun `device info tool execution returns manufacturer and model`() = runBlocking {
        val infoTool = registry.getTool("DEVICE_INFO")
        assertNotNull(infoTool)
        val result = infoTool!!.execute(context, emptyMap())
        assertNotNull(result)
        assertTrue(result.isSuccess)
        assertTrue(result.outputMessage.contains("Android"))
    }
}

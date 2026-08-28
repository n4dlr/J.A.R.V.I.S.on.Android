package com.example.jarvis.core

import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.domain.model.IntentConfidence
import com.example.jarvis.domain.model.StructuredIntent
import java.util.concurrent.ConcurrentHashMap

class CommandCache(
    private val normalizer: AzerbaijaniTextNormalizer = AzerbaijaniTextNormalizer(),
    private val maxEntries: Int = 100
) {

    private val cache = ConcurrentHashMap<String, StructuredIntent>()
    private val accessOrder = mutableListOf<String>()

    init {
        preloadCommonCommands()
    }

    private fun preloadCommonCommands() {
        putCanonical(listOf("wifi ac", "wifi-ni ac", "wi-fi ac", "wifi aktiv et", "vayfay ac"), "WIFI_SETTINGS")
        putCanonical(listOf("bluetooth ac", "blutuz ac", "bluetooth-u ac", "blutuzu ac"), "BLUETOOTH_SETTINGS")
        putCanonical(listOf("batareya", "zaryadka", "zaryadka necedir", "batareya necedir"), "GET_BATTERY")
        putCanonical(listOf("ram", "operativka", "ram yoxla", "ram necedir"), "GET_RAM")
        putCanonical(listOf("feneri yandir", "isiq ac", "fener ac"), "TORCH", mapOf("state" to "ON"))
        putCanonical(listOf("feneri sondur", "isiq sondur", "fener bagla"), "TORCH", mapOf("state" to "OFF"))
        putCanonical(listOf("sesi artir", "sesi coxalt", "volume up"), "SET_VOLUME", mapOf("action" to "UP"))
        putCanonical(listOf("sesi azalt", "sesi endir", "volume down"), "SET_VOLUME", mapOf("action" to "DOWN"))
        putCanonical(listOf("sesi kes", "sus", "mute", "sessiz rejim"), "MUTE")
        putCanonical(listOf("sesi ac", "unmute", "normal rejim"), "UNMUTE")
        putCanonical(listOf("mahnini saxla", "musiqini dayandir", "pause"), "MEDIA_PAUSE")
        putCanonical(listOf("mahnini oynat", "musiqini baslat", "play"), "MEDIA_PLAY")
        putCanonical(listOf("ana ekran", "home", "ana ekrana get"), "OPEN_HOME")
        putCanonical(listOf("son tetbiqler", "recents"), "OPEN_RECENTS")
        putCanonical(listOf("bildirisleri oxu", "bildirisler"), "READ_NOTIFICATIONS")
        putCanonical(listOf("tenzimlemeler", "ayarlar", "nastroyka"), "OPEN_SETTINGS")
    }

    private fun putCanonical(queries: List<String>, intentId: String, args: Map<String, String> = emptyMap()) {
        for (q in queries) {
            val norm = normalizer.normalize(q)
            cache[norm] = StructuredIntent(
                intentId = intentId,
                rawQuery = q,
                normalizedQuery = norm,
                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                arguments = args,
                isDeterministic = true
            )
        }
    }

    fun get(rawQuery: String): StructuredIntent? {
        val normalized = normalizer.normalize(rawQuery)
        val cached = cache[normalized]
        if (cached != null) {
            synchronized(accessOrder) {
                accessOrder.remove(normalized)
                accessOrder.add(normalized)
            }
            return cached.copy(rawQuery = rawQuery, normalizedQuery = normalized)
        }
        return null
    }

    fun put(rawQuery: String, intent: StructuredIntent) {
        val normalized = normalizer.normalize(rawQuery)
        synchronized(accessOrder) {
            if (accessOrder.size >= maxEntries && !cache.containsKey(normalized)) {
                val oldest = accessOrder.removeAt(0)
                cache.remove(oldest)
            }
            accessOrder.remove(normalized)
            accessOrder.add(normalized)
            cache[normalized] = intent
        }
    }

    fun clear() {
        cache.clear()
        synchronized(accessOrder) {
            accessOrder.clear()
        }
        preloadCommonCommands()
    }
}

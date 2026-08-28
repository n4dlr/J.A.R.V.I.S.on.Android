package com.example.jarvis.ai.matcher

import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.domain.model.IntentConfidence
import com.example.jarvis.domain.model.StructuredIntent

class DeterministicIntentMatcher(
    private val normalizer: AzerbaijaniTextNormalizer = AzerbaijaniTextNormalizer()
) {

    // Regex patterns for deterministic matching on normalized Azerbaijani strings
    private val lockScreenPatterns = listOf(
        Regex("""(?i)\b(telefonu|ekrani|cihazi|ekran|telefonumu)\s*(kilidle|bagla|lock\s*et|lock|blakirofka\s*et|sondur)\b"""),
        Regex("""(?i)\b(ekrani\s*kilidle|telefonu\s*kilidle|lock\s*screen|ekran\s*bagla)\b"""),
        Regex("""(?i)^\s*(kilidle|lock|blakirofka)\s*$""")
    )

    private val torchPatterns = listOf(
        Regex("""(?i)\b(fener\w*|isiq\w*|fanar\w*|torch|flashlight)\s*(yandir\w*|ac\w*|ise\s*sal\w*)\b"""),
        Regex("""(?i)\b(fener\w*|isiq\w*|fanar\w*|torch|flashlight)\s*(sondur\w*|bagla\w*|kes\w*)\b"""),
        Regex("""(?i)^\s*(fener|feneri\s*yandir|feneri\s*sondur|torch)\s*$""")
    )

    private val batteryPatterns = listOf(
        Regex("""(?i)\b(batareya\w*|zaryadka\w*|zaryatqa\w*|pil\w*)\b.*(nece|ne\s*qeder|faiz|veziyyet|goster|yoxla|faizi|durum)"""),
        Regex("""(?i)\b(nece\s*faiz\s*zaryadka|batareya\s*ne\s*qederdir|zaryadka\s*necedir|batareyani\s*goster|pil\s*seviyyesi)\b"""),
        Regex("""(?i)^\s*(batareya|zaryadka|battery)\s*$""")
    )

    private val ramPatterns = listOf(
        Regex("""(?i)\b(ram\w*|operativka\w*|operativ\s*yaddas\w*)\b.*(nece|ne\s*qeder|veziyyet|yoxla|istifade|goster)"""),
        Regex("""(?i)^\s*(ram|operativka|ram\s*yoxla|ram\s*veziyyeti)\s*$""")
    )

    private val storagePatterns = listOf(
        Regex("""(?i)\b(yaddas\w*|daxili\s*yaddas\w*|disk\w*|storage\w*|bos\s*yer\w*|ne\s*qeder\s*yer\s*var)\b.*(ne\s*qeder|veziyyet|goster|yoxla|tutumu|bos\s*yer)"""),
        Regex("""(?i)^\s*(yaddas|storage|bos\s*yer)\s*$""")
    )

    private val volumePatterns = listOf(
        Regex("""(?i)\b(ses|sesi|volume)\s*(artir|coxalt|qalddir|yukselt|qaldir)\b"""),
        Regex("""(?i)\b(ses|sesi|volume)\s*(azalt|endir|al|dusur)\b"""),
        Regex("""(?i)\b(ses|sesi|volume)\s*(kes|sessiz\s*et|sus|bagla|mute)\b"""),
        Regex("""(?i)\b(ses|sesi|volume)\s*(maksimum|sona\s*qeder|tam\s*ac)\b"""),
        Regex("""(?i)\b(sesi\s*faiz|sesi\s*\d+)\b""")
    )

    private val photoPatterns = listOf(
        Regex("""(?i)\b(sekil|foto|selfi|kamera|kamerani)\s*(cek|ac|baslat|vur)\b"""),
        Regex("""(?i)^\s*(sekil\s*cek|foto\s*cek|kamera)\s*$""")
    )

    private val reminderPatterns = listOf(
        Regex("""(?i)\b(xatirlatma|xatirlat|zengli\s*saat|alarm|budilnik)\s*(qur|yarat|qoy|təyin\s*et|teyin\s*et)\b"""),
        Regex("""(?i)\b(saat\s*(\d{1,2})(:|\s*)?(\d{2})?\s*(ucun|e|a)?\s*(alarm|zengli\s*saat|xatirlatma))\b""")
    )

    private val settingsPatterns = listOf(
        Regex("""(?i)\b(tenzimlemeler|tenzimlemeleri|nastroyka|ayarlar|settings)\s*(ac|goster|baslat)?\b"""),
        Regex("""(?i)\b(wifi|bluetooth|vayfay|blutuz|ekran\s*isigi)\s*(tenzimleme|ayarlar|nastroyka|ac)\b""")
    )

    private val notificationsPatterns = listOf(
        Regex("""(?i)\b(bildiris|bildirisler|bildirisleri|mesajlar|notification|notifications)\s*(oxu|goster|yoxla|ne\s*var)\b"""),
        Regex("""(?i)^\s*(bildirisler|bildirisleri\s*oxu)\s*$""")
    )

    private val openAppPattern = Regex("""(?i)^\s*(.+?)\s+(ac|baslat|ise\s*sal|launch|open)\s*$""")

    fun match(rawQuery: String): StructuredIntent? {
        val normalized = normalizer.normalize(rawQuery)
        if (normalized.isBlank()) return null

        // 1. TORCH (Check specific hardware device toggles first)
        if (torchPatterns.any { it.containsMatchIn(normalized) }) {
            val state = if (normalized.contains("sondur") || normalized.contains("bagla") || normalized.contains("kes")) {
                "OFF"
            } else {
                "ON"
            }
            return StructuredIntent(
                intentId = "TORCH",
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                arguments = mapOf("state" to state),
                isDeterministic = true
            )
        }

        // 2. GET_BATTERY
        if (batteryPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent(
                intentId = "GET_BATTERY",
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                isDeterministic = true
            )
        }

        // 3. GET_RAM
        if (ramPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent(
                intentId = "GET_RAM",
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                isDeterministic = true
            )
        }

        // 4. GET_STORAGE
        if (storagePatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent(
                intentId = "GET_STORAGE",
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                isDeterministic = true
            )
        }

        // 5. LOCK_SCREEN
        if (lockScreenPatterns.any { it.containsMatchIn(normalized) } ||
            normalized in listOf("kilidle", "lock", "telefonu kilidle", "ekrani bagla", "ekrani kilidle", "telefonumu kilidle", "lock et")
        ) {
            return StructuredIntent(
                intentId = "LOCK_SCREEN",
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                isDeterministic = true
            )
        }

        // 6. SET_VOLUME
        if (volumePatterns.any { it.containsMatchIn(normalized) }) {
            val action = when {
                normalized.contains("artir") || normalized.contains("coxalt") || normalized.contains("yukselt") -> "UP"
                normalized.contains("azalt") || normalized.contains("endir") || normalized.contains("al") -> "DOWN"
                normalized.contains("kes") || normalized.contains("sessiz") || normalized.contains("sus") || normalized.contains("mute") -> "MUTE"
                normalized.contains("maksimum") || normalized.contains("tam") -> "MAX"
                else -> "UP"
            }
            return StructuredIntent(
                intentId = "SET_VOLUME",
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                arguments = mapOf("action" to action),
                isDeterministic = true
            )
        }

        // 7. TAKE_PHOTO
        if (photoPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent(
                intentId = "TAKE_PHOTO",
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                isDeterministic = true
            )
        }

        // 8. CREATE_REMINDER
        if (reminderPatterns.any { it.containsMatchIn(normalized) }) {
            val hourMatch = Regex("""\b(\d{1,2})\b""").find(normalized)
            val hour = hourMatch?.groupValues?.get(1) ?: "8"
            return StructuredIntent(
                intentId = "CREATE_REMINDER",
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                arguments = mapOf("title" to rawQuery, "hour" to hour, "minutes" to "0"),
                isDeterministic = true
            )
        }

        // 9. OPEN_SETTINGS
        if (settingsPatterns.any { it.containsMatchIn(normalized) }) {
            val target = when {
                normalized.contains("wifi") || normalized.contains("vayfay") -> "wifi"
                normalized.contains("bluetooth") || normalized.contains("blutuz") -> "bluetooth"
                normalized.contains("ekran") || normalized.contains("display") -> "display"
                normalized.contains("ses") || normalized.contains("sound") -> "sound"
                normalized.contains("batareya") -> "battery"
                else -> "main"
            }
            return StructuredIntent(
                intentId = "OPEN_SETTINGS",
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                arguments = mapOf("target" to target),
                isDeterministic = true
            )
        }

        // 10. READ_NOTIFICATIONS
        if (notificationsPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent(
                intentId = "READ_NOTIFICATIONS",
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                isDeterministic = true
            )
        }

        // 11. OPEN_APP ("youtube ac", "whatsapp baslat", "telegrami ac", etc.)
        val appMatch = openAppPattern.find(normalized)
        if (appMatch != null) {
            val appName = appMatch.groupValues[1].trim()
            if (appName.isNotEmpty() && !appName.contains("fener") && !appName.contains("tenzimleme") && !appName.contains("kamera")) {
                return StructuredIntent(
                    intentId = "OPEN_APP",
                    rawQuery = rawQuery,
                    normalizedQuery = normalized,
                    confidence = IntentConfidence.HIGH_HEURISTIC,
                    arguments = mapOf("app_name" to appName),
                    isDeterministic = true
                )
            }
        }

        return null
    }
}

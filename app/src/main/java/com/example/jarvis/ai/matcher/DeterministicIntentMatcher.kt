package com.example.jarvis.ai.matcher

import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.domain.model.IntentConfidence
import com.example.jarvis.domain.model.StructuredIntent

class DeterministicIntentMatcher(
    private val normalizer: AzerbaijaniTextNormalizer = AzerbaijaniTextNormalizer()
) {

    // ── Media-in-App Pattern Definitions ─────────────────────────────────────
    // These MUST be checked BEFORE the generic openAppPattern to prevent
    // sentences like "Youtube-da INNA Caliente mahnısını aç" from being
    // misclassified as OPEN_APP with the entire sentence as app_name.

    /**
     * Pattern: "[App]-da [Query] mahnısını/musiqisini [aç|çal|oynat|tap|axtar]"
     * Covers the primary reported failure mode.
     */
    private val mediaInAppLocativePatterns = listOf(
        // "Youtube-da INNA Caliente mahnısını aç" / "Spotify-da X-i çal"
        Regex("""(?i)^\s*(\S+(?:-(?:da|de|ta|te|da|de))?)\s+(.+?)\s+(?:mahni|mahnis|musiq|musiqi|musiqu|parcanı|parcani|sarki)\w*\s*(?:ac|acaq|cal|oynat|oyna|oynat|tap|axtar|iste|dinle|goster)\w*\s*$"""),
        // "Youtube-da X-ni/X-i aç" (short form)
        Regex("""(?i)^\s*(\S+(?:-(?:da|de|ta|te|da|de))?)\s+(.+?)\s*(?:ac|cal|oynat|tap|axtar)\s*$"""),
        // Variant: "X-i Youtube-da çal" (object before app)
        Regex("""(?i)^\s*(.+?)\s+(\S+(?:-(?:da|de|ta|te|da|de))?)\s+(?:cal|oynat|oyna|dinle|axtar)\s*$""")
    )

    /**
     * Pattern: "[App]-da [Query] [haqqında] axtar"
     * e.g. "Google-da Android 16 haqqında axtar", "Chrome-da X axtar"
     */
    private val appSearchPatterns = listOf(
        Regex("""(?i)^\s*(\S+(?:-(?:da|de|ta|te|da|de))?)\s+(.+?)\s+(?:haqqinda|haqqinda|haqda|axtar|axtaris|search)\w*\s*$"""),
        Regex("""(?i)^\s*(\S+(?:-(?:da|de|ta|te|da|de))?)\s+(.+?)\s+(?:axtar)\s*$""")
    )

    /**
     * Pattern: "[App]-da X yaz" (send message in app)
     * e.g. "WhatsApp-da Nadirə salam yaz", "Telegram-da X-ə mesaj yaz"
     */
    private val appMessagePatterns = listOf(
        Regex("""(?i)^\s*(\S+(?:-(?:da|de|ta|te|da|de))?)\s+(.+?)\s+(?:salam|mesaj|yaz|gonder|yazdir)\w*\s*$""")
    )

    // ── Pattern Definitions ──────────────────────────────────────────────────

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

    private val batteryTempPatterns = listOf(
        Regex("""(?i)\b(batareya|pil|zaryadka)\s*(temperaturu|istiliyi|isti)\b"""),
        Regex("""(?i)^\s*(batareya\s*temperaturu|pil\s*sicakligi)\s*$""")
    )

    private val chargingStatusPatterns = listOf(
        Regex("""(?i)\b(sarj|sarjda|enerji|zaryadka)\w*\s*(olur|yigir|durum|veziyyet)\b"""),
        Regex("""(?i)\b(telefon|cihaz)?\s*(sarj|enerji|zaryadka)\w*\s*(olurmu|olur|yigirmi|yigir|veziyyeti|durum)\b"""),
        Regex("""(?i)^\s*(sarj\s*olurmu|enerji\s*yigirmi|sarj\s*veziyyeti|telefon\s*sarj\s*olurmu)\s*$""")
    )

    private val batterySaverPatterns = listOf(
        Regex("""(?i)\b(qenaet|enerji\s*qenaeti|battery\s*saver)\s*(rejimi|veziyyeti|aktiv)\b"""),
        Regex("""(?i)^\s*(qenaet\s*rejimi|batareya\s*qenaeti)\s*$""")
    )

    private val ramPatterns = listOf(
        Regex("""(?i)\b(ram\w*|operativka\w*|operativ\s*yaddas\w*)\b.*(nece|ne\s*qeder|veziyyet|yoxla|istifade|goster)"""),
        Regex("""(?i)^\s*(ram|operativka|ram\s*yoxla|ram\s*veziyyeti)\s*$""")
    )

    private val cpuPatterns = listOf(
        Regex("""(?i)\b(cpu|prosessor|prosesor)\b.*(veziyyet|faiz|istifade|yoxla|goster|necedir)"""),
        Regex("""(?i)^\s*(cpu|cpu\s*veziyyeti|prosessor)\s*$""")
    )

    private val storagePatterns = listOf(
        Regex("""(?i)\b(yaddas\w*|daxili\s*yaddas\w*|disk\w*|storage\w*|bos\s*yer\w*|ne\s*qeder\s*yer\s*var)\b.*(ne\s*qeder|veziyyet|goster|yoxla|tutumu|bos\s*yer)"""),
        Regex("""(?i)^\s*(yaddas|storage|bos\s*yer|yaddas\s*veziyyeti)\s*$""")
    )

    private val deviceInfoPatterns = listOf(
        Regex("""(?i)\b(cihaz|telefon|model|sistem)\s*(melumat|haqqinda|versiya|parametr)\b"""),
        Regex("""(?i)^\s*(cihaz\s*melumati|telefon\s*haqqinda|device\s*info)\s*$""")
    )

    private val volumePatterns = listOf(
        Regex("""(?i)\b(ses|sesi|volume)\s*(artir|coxalt|qalddir|yukselt|qaldir)\b"""),
        Regex("""(?i)\b(ses|sesi|volume)\s*(azalt|endir|al|dusur)\b"""),
        Regex("""(?i)\b(ses|sesi|volume)\s*(kes|sessiz\s*et|sus|bagla|mute)\b"""),
        Regex("""(?i)\b(sesi\s*ac|sesi\s*berpa\s*et|unmute)\b"""),
        Regex("""(?i)\b(ses|sesi|volume)\s*(maksimum|sona\s*qeder|tam\s*ac)\b"""),
        Regex("""(?i)\b(ses\s*seviyyesi|ses\s*necedir|sesi\s*goster)\b"""),
        Regex("""(?i)\b(sesi\s*faiz|sesi\s*\d+)\b""")
    )

    private val mediaPatterns = listOf(
        Regex("""(?i)\b(mahnini|mahnisini|musiqini|musiqisini|media|mahni|musiqi)\w*\s*(?:\w+\s+)?(oynat|baslat|play|oxut|ac|cal|dinle)\b"""),
        Regex("""(?i)\b(mahnini|mahnisini|musiqini|musiqisini|media|mahni|musiqi)\w*\s*(?:\w+\s+)?(dayandir|saxla|pause|durdur)\b"""),
        Regex("""(?i)\b(novbeti|ireli|next|sonraki)\s*(mahni|musiqi|parca)?\w*\s*(kec|oynat|ac)?\b"""),
        Regex("""(?i)\b(evvelki|geri|previous)\s*(mahni|musiqi|parca)?\w*\s*(kec|oynat|ac)?\b""")
    )

    private val photoPatterns = listOf(
        Regex("""(?i)\b(sekil|foto|selfi|kamera|kamerani)\s*(cek|ac|baslat|vur)\b"""),
        Regex("""(?i)^\s*(sekil\s*cek|foto\s*cek|kamera)\s*$""")
    )

    private val recordVideoPatterns = listOf(
        Regex("""(?i)\b(video|videoya)\s*(cek|yaz|baslat|kayd)\b"""),
        Regex("""(?i)^\s*(video\s*cek|video\s*yaz)\s*$""")
    )

    private val alarmReminderPatterns = listOf(
        Regex("""(?i)\b(xatirlatma|xatirlat|zengli\s*saat|alarm|budilnik)\s*(qur|yarat|qoy|təyin\s*et|teyin\s*et)\b"""),
        Regex("""(?i)\b(saat\s*(\d{1,2})(:|\s*)?(\d{2})?\s*(ucun|e|a)?\s*(alarm|zengli\s*saat|xatirlatma))\b"""),
        Regex("""(?i)\b(alarm\w*|budilnik\w*|zengli\s*saat\w*)\s*(siyahisi|goster|sil|dayandir)\b""")
    )

    private val settingsPatterns = listOf(
        Regex("""(?i)\b(tenzimlemeler|tenzimlemeleri|nastroyka|ayarlar|settings|parametrler|parametrleri)\s*(ac|goster|baslat)?\b"""),
        Regex("""(?i)\b(wifi|bluetooth|vayfay|blutuz|ekran\s*isigi|mekan|mobil\s*sebeke)\w*\s*(?:\w+\s+)?(tenzimleme|parametr|ayarlar|nastroyka|ac|aktiv\s*et)\b""")
    )

    private val wifiStatusPatterns = listOf(
        Regex("""(?i)\b(wifi|wi\s*fi|vayfay)\w*\s*(veziyyeti|durum|baglidir|acdir|yoxla|necedir|nedir)\b"""),
        Regex("""(?i)^\s*(wifi\s*veziyyeti|wi\s*fi\s*veziyyeti|vayfay\s*yoxla)\s*$""")
    )

    private val networkStatusPatterns = listOf(
        Regex("""(?i)\b(sebeke|internet|baglanti)\w*\s*(veziyyeti|varmi|yoxla|durum|necedir)\b"""),
        Regex("""(?i)^\s*(internet\s*varmi|sebeke\s*veziyyeti)\s*$""")
    )

    private val ipInfoPatterns = listOf(
        Regex("""(?i)\b(ip|ip\s*unvani|unvanim)\s*(necedir|goster|yoxla|nedir)\b"""),
        Regex("""(?i)^\s*(ip|ip\s*unvani|ip\s*info)\s*$""")
    )

    private val bluetoothStatusPatterns = listOf(
        Regex("""(?i)\b(bluetooth|blutuz|blutus)\w*\s*(veziyyeti|acdir|baglidir|cihazlar|yoxla|durum|necedir)\b"""),
        Regex("""(?i)^\s*(bluetooth\s*veziyyeti|blutuz\s*yoxla)\s*$""")
    )

    private val notificationsPatterns = listOf(
        Regex("""(?i)\b(son\s*)?(bildiris|bildirisler|bildirisleri|mesajlar|notification|notifications)\w*\s*(oxu|goster|yoxla|ne\s*var|siyahisi)\b"""),
        Regex("""(?i)^\s*(son\s*)?(bildirisler|bildirisleri\s*oxu|bildiris\s*siyahisi)\s*$""")
    )

    private val screenControlPatterns = listOf(
        Regex("""(?i)\b(parlaqlıq|parlaqligi|isiq|ekran\s*isigi)\s*(artir|azalt|coxalt|endir|set|tenzimle)\b"""),
        Regex("""(?i)^\s*(parlaqligi\s*artir|parlaqligi\s*azalt|ekran\s*isigi)\s*$""")
    )

    private val navigationPatterns = listOf(
        Regex("""(?i)\b(ana\s*ekran\w*|home|ana\s*sehife)\s*(get|ac|qayit|kec)\b"""),
        Regex("""(?i)^\s*(ana\s*ekran\w*\s*(get|ac|qayit))\s*$"""),
        Regex("""(?i)\b(son\s*tetbiqler|recents|ac\s*tetbiqler)\s*(ac|goster)\b"""),
        Regex("""(?i)\b(bildiris\s*paneli|paneli\s*endir)\b"""),
        Regex("""(?i)\b(suretli\s*tenzimlemeler|quick\s*settings)\s*(ac|goster)\b"""),
        Regex("""(?i)\b(geri\s*qayit|geriye|back)\b""")
    )

    private val contactsCallPatterns = listOf(
        Regex("""(?i)\b(kontakt|kontaktlar|kontaktlari)\s*(axtar|tap|ac|goster|yarat|elave\s*et)\b"""),
        Regex("""(?i)\b(zeng\s*et|zeng\s*vur|call|yig)\s*(.+)?\b"""),
        Regex("""(?i)\b(zeng\s*tarixcesi|son\s*zengler)\s*(ac|goster)\b""")
    )

    private val smsPatterns = listOf(
        Regex("""(?i)\b(sms|mesaj|mesajlar)\s*(yaz|gonder|ac)\b"""),
        Regex("""(?i)^\s*(sms\s*yaz|mesajlari\s*ac)\s*$""")
    )

    private val locationPatterns = listOf(
        Regex("""(?i)\b(mekanim|hardayam|koordinat|yerim|location)\s*(haradadir|necedir|goster|tap|al)\b"""),
        Regex("""(?i)\b(xerite\w*|google\s*maps|map)\s*(ac|goster|axtar)\b"""),
        Regex("""(?i)\b\w+\s+(xerite\w*)\s*(ac|goster|axtar)\b"""),
        Regex("""(?i)^\s*(mekanim|hardayam|xeriteni\s*ac)\s*$""")
    )

    private val browserPatterns = listOf(
        Regex("""(?i)\b(axtar|axtaris\s*et|google\s*et|google\s*axtar)\s*(.+)?\b"""),
        Regex("""(?i)\b(brauzer|brauzeri|internet|sayt)\s*(ac)\b"""),
        Regex("""(?i)\b(https?://\S+|www\.\S+)\b""")
    )

    private val filesPatterns = listOf(
        Regex("""(?i)\b(fayl|fayllari|fayllar)\s*(axtar|tap|sil|paylas|kopyala|kocur|adlandir)\b"""),
        Regex("""(?i)\b(qovluq|papka)\s*(yarat|ac)\b""")
    )

    private val calendarPatterns = listOf(
        Regex("""(?i)\b(teqvim|gorush|hadise|event)\s*(yarat|qur|elave\s*et|goster|siyahisi|ac)\b"""),
        Regex("""(?i)^\s*(teqvim|teqvimi\s*ac|teqvim\s*hadiseleri)\s*$""")
    )

    private val appsPatterns = listOf(
        Regex("""(?i)\b(tetbiqler|qurasdirilmis\s*tetbiqler|proqramlar)\s*(siyahisi|goster)\b"""),
        Regex("""(?i)\b(play\s*store|market)\s*(ac|axtar)\b""")
    )

    private val accessibilityPatterns = listOf(
        Regex("""(?i)\b(ekrandaki\s*metni\s*oxu|ekrani\s*oxu|metni\s*oxu)\b"""),
        Regex("""(?i)\b(asagi\s*surusdur|yuxari\s*surusdur|scroll)\b"""),
        Regex("""(?i)\b(duymeye\s*bas|klik\s*et|vur)\s*(.+)?\b""")
    )

    private val openAppPattern = Regex("""(?i)^\s*(.+?)\s+(ac|baslat|ise\s*sal|launch|open)\s*$""")

    // Words that disqualify a token sequence from being a bare app name.
    // If the potential app_name contains these, it is likely a compound command.
    private val appNamePolluters = setOf(
        "mahni", "mahnis", "musiq", "musiqi", "parcan", "sarki",
        "haqqinda", "haqda", "axtar", "axtaris", "gonder", "yaz",
        "inna", "caliente", "android", "google", "search"
    )

    // ── Matching Engine ──────────────────────────────────────────────────────

    fun match(rawQuery: String): StructuredIntent? {
        val normalized = normalizer.normalize(rawQuery)
        if (normalized.isBlank()) return null

        // 0. URL DETECTION — must run on rawQuery before normalizer strips :// and dots
        val urlMatch = Regex("""\b(https?://\S+|www\.\S+)\b""").find(rawQuery)
        if (urlMatch != null) {
            return StructuredIntent(
                intentId = "OPEN_URL",
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                arguments = mapOf("url" to urlMatch.value),
                isDeterministic = true
            )
        }

        // 1. TORCH
        if (torchPatterns.any { it.containsMatchIn(normalized) }) {
            val state = if (normalized.contains("sondur") || normalized.contains("bagla") || normalized.contains("kes")) "OFF" else "ON"
            return StructuredIntent(
                intentId = "TORCH",
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                arguments = mapOf("state" to state),
                isDeterministic = true
            )
        }

        // 2. BATTERY & POWER
        if (batteryTempPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent("BATTERY_TEMPERATURE", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }
        if (chargingStatusPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent("CHARGING_STATUS", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }
        if (batterySaverPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent("BATTERY_SAVER_STATUS", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }
        if (batteryPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent("GET_BATTERY", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }

        // 3. PERFORMANCE & SYSTEM INFO
        if (ramPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent("GET_RAM", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }
        if (cpuPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent("CPU_STATUS", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }
        if (storagePatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent("GET_STORAGE", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }
        if (deviceInfoPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent("DEVICE_INFO", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }

        // 4. LOCK SCREEN
        if (lockScreenPatterns.any { it.containsMatchIn(normalized) } ||
            normalized in listOf("kilidle", "lock", "telefonu kilidle", "ekrani bagla", "ekrani kilidle", "telefonumu kilidle", "lock et")
        ) {
            return StructuredIntent("LOCK_SCREEN", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }

        // 4b. MEDIA_SEARCH_PLAY & APP SEARCH (App + Query Compound Commands)
        // MUST come before generic audio/media and OPEN_APP checks!
        // Handles: "Youtube-da INNA Caliente mahnısını aç", "Spotify-da X çal", "Google-da Android 16 haqqında axtar", etc.
        run {
            val tokens = normalized.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (tokens.size >= 2) {
                // Try extracting app + query from token list
                val extracted = AppNameExtractor.extractAppAndQuery(tokens)
                if (extracted != null) {
                    val (appName, query) = extracted
                    val originalQuery = extractOriginalQuery(rawQuery, appName, query)
                    val isMediaApp = appName in setOf("youtube", "spotify", "soundcloud", "youtubemusic", "tidal", "deezer", "apple music")
                    val hasMediaWord = tokens.any { it in setOf("mahni", "mahnisini", "musiqi", "musiqisini", "parca", "sarki", "cal", "oynat", "dinle", "oxut") }
                    val isSearchApp = appName in setOf("google", "chrome", "xrom", "browser", "brauzer", "firefox")
                    val hasSearchWord = tokens.any { it in setOf("axtar", "axtaris", "haqqinda", "haqda", "search") }

                    val intentId = when {
                        isSearchApp || (hasSearchWord && !hasMediaWord && !isMediaApp) -> {
                            if (appName in setOf("chrome", "xrom", "browser", "brauzer", "firefox", "google")) "WEB_SEARCH" else "APP_SEARCH"
                        }
                        else -> "MEDIA_SEARCH_PLAY"
                    }

                    return StructuredIntent(
                        intentId = intentId,
                        rawQuery = rawQuery,
                        normalizedQuery = normalized,
                        confidence = IntentConfidence.EXACT_DETERMINISTIC,
                        arguments = mapOf(
                            "target_app" to appName,
                            "query" to originalQuery
                        ),
                        extractedEntities = listOf(appName, originalQuery),
                        isDeterministic = true
                    )
                }

                // App-specific search: "Chrome-da X axtar", "Google-da X haqqında axtar"
                for (pattern in appSearchPatterns) {
                    val match = pattern.find(normalized)
                    if (match != null && match.groupValues.size >= 3) {
                        val rawApp = match.groupValues[1].trim()
                        val rawQuery2 = match.groupValues[2].trim()
                        val appName2 = AppNameExtractor.extract(rawApp)
                        if (rawQuery2.isNotBlank()) {
                            val originalQuery2 = extractOriginalQuery(rawQuery, appName2, rawQuery2)
                            val browsers = setOf("chrome", "xrom", "firefox", "browser", "brauzer")
                            val intentId = if (appName2 in browsers) "WEB_SEARCH" else "APP_SEARCH"
                            return StructuredIntent(
                                intentId = intentId,
                                rawQuery = rawQuery,
                                normalizedQuery = normalized,
                                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                                arguments = mapOf(
                                    "query" to originalQuery2,
                                    "target_app" to appName2
                                ),
                                isDeterministic = true
                            )
                        }
                    }
                }

                // App-message: "WhatsApp-da Nadirə salam yaz"
                for (pattern in appMessagePatterns) {
                    val match = pattern.find(normalized)
                    if (match != null && match.groupValues.size >= 3) {
                        val rawApp = match.groupValues[1].trim()
                        val contact = match.groupValues[2].trim()
                        val appName2 = AppNameExtractor.extract(rawApp)
                        if (contact.isNotBlank()) {
                            return StructuredIntent(
                                intentId = "SEND_MESSAGE_IN_APP",
                                rawQuery = rawQuery,
                                normalizedQuery = normalized,
                                confidence = IntentConfidence.HIGH_HEURISTIC,
                                arguments = mapOf(
                                    "target_app" to appName2,
                                    "contact" to contact
                                ),
                                isDeterministic = false
                            )
                        }
                    }
                }
            }
        }

        // 5. AUDIO & MEDIA
        if (mediaPatterns.any { it.containsMatchIn(normalized) }) {
            val intentId = when {
                normalized.contains("dayandir") || normalized.contains("saxla") || normalized.contains("pause") || normalized.contains("durdur") -> "MEDIA_PAUSE"
                normalized.contains("novbeti") || normalized.contains("ireli") || normalized.contains("next") || normalized.contains("sonraki") -> "MEDIA_NEXT"
                normalized.contains("evvelki") || normalized.contains("geri") || normalized.contains("previous") -> "MEDIA_PREVIOUS"
                normalized.contains("oynat") || normalized.contains("baslat") || normalized.contains("play") || normalized.contains("oxut") || normalized.contains("ac") || normalized.contains("cal") || normalized.contains("dinle") -> "MEDIA_PLAY"
                else -> "MEDIA_PLAY"
            }
            return StructuredIntent(intentId, rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }

        if (volumePatterns.any { it.containsMatchIn(normalized) }) {
            val action = when {
                normalized.contains("artir") || normalized.contains("coxalt") || normalized.contains("yukselt") -> "UP"
                normalized.contains("azalt") || normalized.contains("endir") || normalized.contains("al") -> "DOWN"
                normalized.contains("kes") || normalized.contains("sessiz") || normalized.contains("sus") || normalized.contains("mute") -> "MUTE"
                normalized.contains("unmute") || normalized.contains("berpa") -> "UNMUTE"
                normalized.contains("maksimum") || normalized.contains("tam") -> "MAX"
                else -> "UP"
            }
            if (action == "MUTE") return StructuredIntent("MUTE", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
            if (action == "UNMUTE") return StructuredIntent("UNMUTE", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
            if (normalized.contains("seviyye") || normalized.contains("necedir")) {
                return StructuredIntent("GET_VOLUME", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
            }
            return StructuredIntent("SET_VOLUME", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, mapOf("action" to action), isDeterministic = true)
        }

        // 6. SCREEN & NAVIGATION
        if (screenControlPatterns.any { it.containsMatchIn(normalized) }) {
            val action = if (normalized.contains("azalt") || normalized.contains("endir")) "DOWN" else "UP"
            return StructuredIntent("SCREEN_CONTROL", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, mapOf("action" to action), isDeterministic = true)
        }
        if (navigationPatterns.any { it.containsMatchIn(normalized) }) {
            val intentId = when {
                normalized.contains("ana") || normalized.contains("home") -> "OPEN_HOME"
                normalized.contains("son tetbiq") || normalized.contains("recents") || normalized.contains("ac tetbiq") -> "OPEN_RECENTS"
                normalized.contains("paneli") -> "OPEN_NOTIFICATIONS"
                normalized.contains("suretli") || normalized.contains("quick") -> "OPEN_QUICK_SETTINGS"
                normalized.contains("geri") || normalized.contains("back") -> "GO_BACK"
                else -> "OPEN_HOME"
            }
            return StructuredIntent(intentId, rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }

        // 7. CAMERA & VIDEO
        if (recordVideoPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent("RECORD_VIDEO", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }
        if (photoPatterns.any { it.containsMatchIn(normalized) }) {
            val intentId = if (normalized.contains("kamera") && (normalized.contains("ac") || normalized.contains("baslat"))) "OPEN_CAMERA" else "TAKE_PHOTO"
            return StructuredIntent(intentId, rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }

        // 8. ALARM & REMINDERS
        if (alarmReminderPatterns.any { it.containsMatchIn(normalized) }) {
            val hourMatch = Regex("""\b(\d{1,2})\b""").find(normalized)
            val hour = hourMatch?.groupValues?.get(1) ?: "8"
            val intentId = when {
                normalized.contains("siyahisi") || normalized.contains("goster") -> "LIST_ALARMS"
                normalized.contains("sil") || normalized.contains("dayandir") -> "DELETE_ALARM"
                normalized.contains("alarm") || normalized.contains("budilnik") || normalized.contains("zengli") -> "CREATE_ALARM"
                else -> "CREATE_REMINDER"
            }
            return StructuredIntent(
                intentId = intentId,
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.EXACT_DETERMINISTIC,
                arguments = mapOf("title" to rawQuery, "message" to rawQuery, "hour" to hour, "minutes" to "0"),
                isDeterministic = true
            )
        }

        // 9. NETWORK & CONNECTIVITY
        if (wifiStatusPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent("WIFI_STATUS", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }
        if (networkStatusPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent("NETWORK_STATUS", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }
        if (ipInfoPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent("IP_INFO", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }
        if (bluetoothStatusPatterns.any { it.containsMatchIn(normalized) }) {
            return StructuredIntent("BLUETOOTH_STATUS", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }

        // 10. SETTINGS
        if (settingsPatterns.any { it.containsMatchIn(normalized) }) {
            val intentId = when {
                normalized.contains("wifi") || normalized.contains("vayfay") -> "WIFI_SETTINGS"
                normalized.contains("bluetooth") || normalized.contains("blutuz") -> "BLUETOOTH_SETTINGS"
                normalized.contains("mobil") -> "MOBILE_NETWORK_SETTINGS"
                normalized.contains("mekan") || normalized.contains("location") -> "OPEN_LOCATION_SETTINGS"
                normalized.contains("batareya") -> "OPEN_BATTERY_SETTINGS"
                else -> "OPEN_SETTINGS"
            }
            return StructuredIntent(intentId, rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, mapOf("target" to intentId), isDeterministic = true)
        }

        // 11. NOTIFICATIONS
        if (notificationsPatterns.any { it.containsMatchIn(normalized) }) {
            val intentId = if (normalized.contains("siyahisi")) "LIST_NOTIFICATIONS" else "READ_NOTIFICATIONS"
            return StructuredIntent(intentId, rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        }

        // 12. CONTACTS & CALLS
        if (contactsCallPatterns.any { it.containsMatchIn(normalized) }) {
            val isCall = normalized.contains("zeng") || normalized.contains("call") || normalized.contains("yig")
            if (isCall) {
                if (normalized.contains("tarixcesi") || normalized.contains("son")) {
                    return StructuredIntent("OPEN_CALL_LOG", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
                }
                val phoneMatch = Regex("""\b(\+?\d{7,15})\b""").find(normalized)
                val number = phoneMatch?.groupValues?.get(1) ?: ""
                return if (number.isNotEmpty()) {
                    StructuredIntent("DIAL_NUMBER", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, mapOf("number" to number), isDeterministic = true)
                } else {
                    StructuredIntent("OPEN_CALL_LOG", rawQuery, normalized, IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                }
            } else {
                if (normalized.contains("yarat") || normalized.contains("elave")) {
                    return StructuredIntent("CREATE_CONTACT", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, mapOf("name" to rawQuery), isDeterministic = true)
                }
                if (normalized.contains("axtar") || normalized.contains("tap")) {
                    val q = normalized.substringAfter("axtar").ifEmpty { normalized.substringAfter("tap") }.trim()
                    return StructuredIntent("SEARCH_CONTACT", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, mapOf("query" to q.ifEmpty { rawQuery }), isDeterministic = true)
                }
                return StructuredIntent("OPEN_CONTACTS", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
            }
        }

        // 13. SMS
        if (smsPatterns.any { it.containsMatchIn(normalized) }) {
            val phoneMatch = Regex("""\b(\+?\d{7,15})\b""").find(normalized)
            val number = phoneMatch?.groupValues?.get(1) ?: ""
            return if (number.isNotEmpty() || normalized.contains("yaz") || normalized.contains("gonder")) {
                StructuredIntent("COMPOSE_SMS", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, mapOf("number" to number), isDeterministic = true)
            } else {
                StructuredIntent("OPEN_MESSAGES", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
            }
        }

        // 14. LOCATION & MAP
        if (locationPatterns.any { it.containsMatchIn(normalized) }) {
            return if (normalized.contains("xerite") || normalized.contains("map")) {
                StructuredIntent("OPEN_MAP", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
            } else {
                StructuredIntent("GET_LOCATION", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
            }
        }

        // 15. CALENDAR
        if (calendarPatterns.any { it.containsMatchIn(normalized) }) {
            val intentId = when {
                normalized.contains("yarat") || normalized.contains("qur") || normalized.contains("elave") -> "CREATE_EVENT"
                normalized.contains("siyahisi") || normalized.contains("hadiseleri") -> "LIST_EVENTS"
                else -> "LIST_EVENTS"
            }
            return StructuredIntent(intentId, rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, mapOf("title" to rawQuery), isDeterministic = true)
        }

        // 16. APPS & STORE
        if (appsPatterns.any { it.containsMatchIn(normalized) }) {
            return if (normalized.contains("play") || normalized.contains("market")) {
                StructuredIntent("OPEN_PLAY_STORE", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
            } else {
                StructuredIntent("LIST_APPS", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
            }
        }

        // 17. FILES
        if (filesPatterns.any { it.containsMatchIn(normalized) }) {
            val intentId = when {
                normalized.contains("axtar") || normalized.contains("tap") -> "SEARCH_FILES"
                normalized.contains("sil") -> "DELETE_FILE"
                normalized.contains("paylas") -> "SHARE_FILE"
                normalized.contains("qovluq") -> "CREATE_FOLDER"
                else -> "SEARCH_FILES"
            }
            return StructuredIntent(intentId, rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, mapOf("query" to rawQuery, "path" to rawQuery), isDeterministic = true)
        }

        // 18. ACCESSIBILITY & SCREEN READING
        if (accessibilityPatterns.any { it.containsMatchIn(normalized) }) {
            val intentId = when {
                normalized.contains("oxu") -> "READ_VISIBLE_TEXT"
                normalized.contains("surusdur") -> "SCROLL"
                else -> "CLICK_UI_ELEMENT"
            }
            val dir = if (normalized.contains("yuxari")) "UP" else "DOWN"
            return StructuredIntent(intentId, rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, mapOf("direction" to dir), isDeterministic = true)
        }

        // 19. BROWSER & WEB SEARCH
        if (browserPatterns.any { it.containsMatchIn(normalized) }) {
            val urlMatch = Regex("""\b(https?://\S+|www\.\S+)\b""").find(normalized)
            return if (urlMatch != null) {
                StructuredIntent("OPEN_URL", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, mapOf("url" to urlMatch.value), isDeterministic = true)
            } else if (normalized.contains("axtar") || normalized.contains("google")) {
                val q = normalized.substringAfter("axtar").ifEmpty { normalized.substringAfter("google") }.trim()
                StructuredIntent("WEB_SEARCH", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, mapOf("query" to q.ifEmpty { rawQuery }), isDeterministic = true)
            } else {
                StructuredIntent("OPEN_BROWSER", rawQuery, normalized, IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
            }
        }

        // 20. OPEN_APP ("youtube ac", "whatsapp baslat", "telegrami ac", etc.)
        val appMatch = openAppPattern.find(normalized)
        if (appMatch != null) {
            val rawAppName = appMatch.groupValues[1].trim()
            val blacklisted = listOf("fener", "tenzimleme", "kamera", "video", "sms", "mesaj", "brauzer", "xerite", "kontakt", "teqvim", "play store", "bildiris", "wifi", "vayfay", "bluetooth", "blutuz", "parametr")
            // Reject if the "app name" contains query-polluters (song names, search terms, etc.)
            val containsPolluter = appNamePolluters.any { rawAppName.contains(it) }
            // Reject if the "app name" is multiple words with non-app content
            val tokenCount = rawAppName.split(" ").size
            val likelySentence = tokenCount > 4
            if (rawAppName.isNotEmpty() && !containsPolluter && !likelySentence && blacklisted.none { rawAppName.contains(it) }) {
                // Strip postposition suffix from app name before storing
                val firstWord = rawAppName.split(" ").first()
                val cleanedAppName = AppNameExtractor.extract(firstWord)
                return StructuredIntent(
                    intentId = "OPEN_APP",
                    rawQuery = rawQuery,
                    normalizedQuery = normalized,
                    confidence = IntentConfidence.HIGH_HEURISTIC,
                    arguments = mapOf("app_name" to cleanedAppName),
                    isDeterministic = true
                )
            }
        }

        return null
    }

    /**
     * Attempt to recover original casing for a query string.
     * The normalizer lowercases everything, but search queries need proper casing
     * (e.g. "INNA Caliente" not "inna caliente").
     *
     * Strategy: find the query substring in the rawQuery, preserving original casing.
     * Falls back to the normalized query if not found.
     */
    private fun extractOriginalQuery(rawQuery: String, appName: String, normalizedQuery: String): String {
        // Try to find the query region in rawQuery by removing the app reference
        val rawLower = rawQuery.lowercase(java.util.Locale.ROOT)
        val queryLower = normalizedQuery.lowercase(java.util.Locale.ROOT)

        // Find start index of query in raw text (approximate)
        val queryIdx = rawLower.indexOf(queryLower.take(6).trim())
        if (queryIdx >= 0) {
            // Extract from rawQuery at approximately the right position
            val candidate = rawQuery.substring(queryIdx).let { seg ->
                // Strip trailing verbs
                val verbEndings = listOf(" aç", " açaq", " çal", " oynat", " tap", " axtar", " dinlə", " open", " play")
                var result = seg
                for (v in verbEndings) {
                    if (result.lowercase().endsWith(v.lowercase())) {
                        result = result.dropLast(v.length)
                        break
                    }
                }
                // Strip trailing media type words ("mahnısını", "musiqisini")
                val mediaWords = listOf("mahnısını", "mahnını", "musiqisini", "musiqini", "parçanı", "şarkısını")
                for (m in mediaWords) {
                    if (result.trimEnd().lowercase().endsWith(m.lowercase())) {
                        result = result.trimEnd().dropLast(m.length).trimEnd()
                        break
                    }
                }
                result.trim()
            }
            if (candidate.isNotBlank() && candidate.length >= 2) {
                return candidate
            }
        }
        // Fallback: return normalized form (already ASCII-lowercased)
        return normalizedQuery
    }
}

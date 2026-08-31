package com.example.jarvis.automation.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.example.jarvis.ai.matcher.AppNameExtractor
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val normalizedLabel: String,
    val launchIntent: Intent?
)

data class AppResolutionResult(
    val matched: Boolean,
    val packageName: String?,
    val appLabel: String?,
    val confidence: Float,
    val matchMethod: String,
    val resolutionNote: String = ""
)

class AppResolver(
    private val context: Context,
    private val normalizer: AzerbaijaniTextNormalizer = AzerbaijaniTextNormalizer()
) {

    private val cachedApps = ConcurrentHashMap<String, InstalledAppInfo>()
    private var lastIndexTime: Long = 0

    // Common phonetic and platform aliases mapping canonical aliases to common package identifiers
    private val knownAliasToPackages = mapOf(
        "youtube" to listOf("com.google.android.youtube", "com.google.android.apps.youtube.music"),
        "yutub" to listOf("com.google.android.youtube"),
        "spotify" to listOf("com.spotify.music", "com.spotify.lite"),
        "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "vatsap" to listOf("com.whatsapp"),
        "telegram" to listOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram"),
        "telqram" to listOf("org.telegram.messenger"),
        "instagram" to listOf("com.instagram.android"),
        "instaqram" to listOf("com.instagram.android"),
        "chrome" to listOf("com.android.chrome", "org.mozilla.firefox", "com.microsoft.emmx"),
        "xrom" to listOf("com.android.chrome"),
        "google" to listOf("com.google.android.googlequicksearchbox", "com.android.chrome"),
        "camera" to listOf("com.google.android.GoogleCamera", "com.android.camera", "com.android.camera2", "com.sec.android.app.camera"),
        "kamera" to listOf("com.google.android.GoogleCamera", "com.android.camera", "com.android.camera2", "com.sec.android.app.camera"),
        "settings" to listOf("com.android.settings"),
        "tenzimlemeler" to listOf("com.android.settings"),
        "ayarlar" to listOf("com.android.settings"),
        "maps" to listOf("com.google.android.apps.maps"),
        "xerite" to listOf("com.google.android.apps.maps"),
        "photos" to listOf("com.google.android.apps.photos", "com.sec.android.gallery3d", "com.android.gallery3d"),
        "qalereya" to listOf("com.google.android.apps.photos", "com.sec.android.gallery3d", "com.android.gallery3d"),
        "gallery" to listOf("com.google.android.apps.photos", "com.sec.android.gallery3d", "com.android.gallery3d"),
        "calculator" to listOf("com.google.android.calculator", "com.sec.android.app.popupcalculator"),
        "kalkulyator" to listOf("com.google.android.calculator"),
        "clock" to listOf("com.google.android.deskclock", "com.sec.android.app.clockpackage"),
        "saat" to listOf("com.google.android.deskclock"),
        "play store" to listOf("com.android.vending"),
        "market" to listOf("com.android.vending"),
        "gmail" to listOf("com.google.android.gm"),
        "tiktok" to listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
        "netflix" to listOf("com.netflix.mediaclient")
    )

    init {
        indexInstalledApps()
    }

    /**
     * Rebuilds the in-memory index of installed launcher apps.
     */
    fun indexInstalledApps() {
        try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            cachedApps.clear()

            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                val label = info.loadLabel(pm).toString()
                val normLabel = normalizer.normalize(label)
                val launchIntent = pm.getLaunchIntentForPackage(pkg)

                cachedApps[pkg] = InstalledAppInfo(
                    packageName = pkg,
                    label = label,
                    normalizedLabel = normLabel,
                    launchIntent = launchIntent
                )
            }
            lastIndexTime = System.currentTimeMillis()
        } catch (_: Exception) {}
    }

    /**
     * Resolves an app name candidate to an installed package with confidence scoring.
     */
    fun resolveApp(rawCandidate: String): AppResolutionResult {
        if (rawCandidate.isBlank()) {
            return AppResolutionResult(false, null, null, 0.0f, "EMPTY_INPUT", "Tətbiq adı daxil edilməyib.")
        }

        // Re-index if cache is empty or older than 5 minutes
        if (cachedApps.isEmpty() || System.currentTimeMillis() - lastIndexTime > 300_000) {
            indexInstalledApps()
        }

        val canonicalToken = AppNameExtractor.extract(rawCandidate)
        val normalizedCandidate = normalizer.normalize(canonicalToken)

        // 1. Direct Alias Match against installed packages
        val aliasCandidates = knownAliasToPackages[normalizedCandidate] ?: knownAliasToPackages[canonicalToken]
        if (aliasCandidates != null) {
            for (pkg in aliasCandidates) {
                val installed = cachedApps[pkg]
                if (installed != null) {
                    return AppResolutionResult(
                        matched = true,
                        packageName = pkg,
                        appLabel = installed.label,
                        confidence = 1.0f,
                        matchMethod = "EXACT_ALIAS",
                        resolutionNote = "'$canonicalToken' tətbiqi alias ilə aşkarlandı: ${installed.label}"
                    )
                }
            }
        }

        // 2. Exact Label or Package Match in installed apps
        for ((pkg, appInfo) in cachedApps) {
            if (appInfo.normalizedLabel.equals(normalizedCandidate, ignoreCase = true) ||
                pkg.equals(normalizedCandidate, ignoreCase = true)
            ) {
                return AppResolutionResult(
                    matched = true,
                    packageName = pkg,
                    appLabel = appInfo.label,
                    confidence = 0.98f,
                    matchMethod = "EXACT_LABEL",
                    resolutionNote = "Dəqiq ad uyğunluğu: ${appInfo.label}"
                )
            }
        }

        // 3. Substring containment match (e.g. "chrome" in "Google Chrome")
        for ((pkg, appInfo) in cachedApps) {
            if (appInfo.normalizedLabel.contains(normalizedCandidate) ||
                normalizedCandidate.contains(appInfo.normalizedLabel)
            ) {
                return AppResolutionResult(
                    matched = true,
                    packageName = pkg,
                    appLabel = appInfo.label,
                    confidence = 0.85f,
                    matchMethod = "SUBSTRING_MATCH",
                    resolutionNote = "Alt-sətir uyğunluğu: ${appInfo.label}"
                )
            }
        }

        // 4. Fuzzy Levenshtein Distance Match (for STT typos)
        var bestMatch: InstalledAppInfo? = null
        var bestDistance = Int.MAX_VALUE

        for ((_, appInfo) in cachedApps) {
            val dist = normalizer.computeLevenshteinDistance(appInfo.normalizedLabel, normalizedCandidate)
            if (dist < bestDistance && dist <= 2) {
                bestDistance = dist
                bestMatch = appInfo
            }
        }

        if (bestMatch != null) {
            val conf = (1.0f - (bestDistance.toFloat() / maxOf(normalizedCandidate.length, 1))).coerceIn(0.6f, 0.9f)
            return AppResolutionResult(
                matched = true,
                packageName = bestMatch.packageName,
                appLabel = bestMatch.label,
                confidence = conf,
                matchMethod = "FUZZY_MATCH",
                resolutionNote = "Təxmini uyğunluq ($bestDistance hərf fərqi): ${bestMatch.label}"
            )
        }

        return AppResolutionResult(
            matched = false,
            packageName = null,
            appLabel = null,
            confidence = 0.0f,
            matchMethod = "NOT_FOUND",
            resolutionNote = "'$rawCandidate' adlı tətbiq cihazda tapılmadı."
        )
    }

    /**
     * Returns true if the package is installed and has a launch intent.
     */
    fun isPackageInstalled(packageName: String): Boolean {
        return cachedApps.containsKey(packageName) || context.packageManager.getLaunchIntentForPackage(packageName) != null
    }

    /**
     * Resolves default browser package.
     */
    fun resolveDefaultBrowser(): String? {
        val browserCandidates = listOf("com.android.chrome", "org.mozilla.firefox", "com.microsoft.emmx", "com.sec.android.app.sbrowser")
        return browserCandidates.firstOrNull { isPackageInstalled(it) } ?: cachedApps.keys.firstOrNull { it.contains("browser") || it.contains("chrome") }
    }
}

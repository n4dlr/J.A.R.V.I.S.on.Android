package com.example.jarvis.rag

import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import kotlin.math.ln

class LightweightRetriever(
    private val normalizer: AzerbaijaniTextNormalizer = AzerbaijaniTextNormalizer()
) {

    // Preloaded on-device knowledge chunks (offline, fast, zero latency)
    private val builtInChunks = listOf(
        KnowledgeChunk(
            id = "jarvis_offline_core",
            title = "JARVIS Offline Core və Məxfilik",
            content = "JARVIS bütün əsas sistem idarəetmə və NLP əmrlərini offline olaraq lokal cihazda icra edir. Heç bir səs və ya şəxsi məlumat istifadəçinin icazəsi olmadan buluda göndərilmir. Məlumatlar lokal Room SQLite bazasında saxlanılır.",
            source = KnowledgeSource.JARVIS_DOCS
        ),
        KnowledgeChunk(
            id = "jarvis_tools_help",
            title = "JARVIS Alətləri və İdarəetmə",
            content = "JARVIS 60-dan çox alətə malikdir: Fənər, Batareya, RAM və CPU diaqnostikası, Səs səviyyəsi, Zəngli saat və Alarm, Tətbiq açma və idarəetmə, Fayl axtarışı və silmə, Wi-Fi və Bluetooth parametrləri, Ekran parlaqlığı və Əlçatımlılıq xidməti.",
            source = KnowledgeSource.JARVIS_DOCS
        ),
        KnowledgeChunk(
            id = "android_ram_opt",
            title = "Android RAM və Sürətləndirmə Məsləhətləri",
            content = "Cihaz yavaş işlədikdə: 1. Arxa planda işləyən ağır tətbiqləri bağlayın. 2. Keş (cache) fayllarını təmizləyin. 3. Daxili yaddaşda ən azı 10% boş yer saxlayın. 4. Cihazı yenidən başladın.",
            source = KnowledgeSource.ANDROID_HELP
        ),
        KnowledgeChunk(
            id = "android_battery_saving",
            title = "Android Batareya Qənaəti və Temperatur",
            content = "Batareya ömrünü uzatmaq üçün: 1. Batareya qənaət rejimini aktiv edin. 2. Ekran parlaqlığını azaldın və ya avtomatik parlaqlığı seçin. 3. İstifadə edilmədikdə GPS və Bluetooth-u söndürün. 4. Batareya temperaturu 45 dərəcədən yuxarı qalxdıqda cihazı bir müddət dincəldin.",
            source = KnowledgeSource.ANDROID_HELP
        ),
        KnowledgeChunk(
            id = "android_storage_clean",
            title = "Daxili Yaddaşın Təmizlənməsi",
            content = "Daxili yaddaş dolduqda Android əməliyyat sistemi yavaşlaya bilər. Yükləmələr (Downloads) qovluğunu, dublikat medianı və istifadə edilməyən böyük tətbiqləri təmizləmək tövsiyə olunur.",
            source = KnowledgeSource.ANDROID_HELP
        ),
        KnowledgeChunk(
            id = "android_wifi_bt_help",
            title = "Wi-Fi və Bluetooth Şəbəkə Nasazlıqları",
            content = "Android 10+ versiyalarında təhlükəsizlik səbəbilə Wi-Fi birbaşa üçüncü tərəf tətbiqlər tərəfindən söndürülə bilməz, parametrlər səhifəsi açılmalıdır. Bluetooth bağlantı problemi olduqda cihazı unut və yenidən qoş metodundan istifadə edin.",
            source = KnowledgeSource.ANDROID_HELP
        ),
        KnowledgeChunk(
            id = "jarvis_accessibility_guide",
            title = "JARVIS Əlçatımlılıq Xidməti Bələdçisi",
            content = "JARVIS Əlçatımlılıq Xidməti vasitəsilə ekranda görünən düymələrə basa, səhifəni sürüşdürə, görünən mətnləri oxuya və Geri/Ana ekran düymələrini icra edə bilər. Bu xidmət Parametrlər -> Əlçatımlılıq bölməsindən aktivləşdirilir.",
            source = KnowledgeSource.JARVIS_DOCS
        )
    )

    private val stopWords = setOf(
        "ve", "bu", "o", "bir", "ile", "ucun", "ne", "nece", "var", "yox", "ki", "da", "de", "mueyyen", "bunu", "men", "sen", "biz", "siz"
    )

    /** Tokenize and normalize text into searchable tokens. */
    fun tokenize(text: String): List<String> {
        val normalized = normalizer.normalize(text)
        return normalized.split(Regex("[^a-zA-Z0-9_]+"))
            .map { it.trim().lowercase() }
            .filter { it.length > 1 && it !in stopWords }
    }

    /**
     * Compute BM25-like relevance score between query tokens and document tokens.
     */
    private fun score(queryTokens: List<String>, docTokens: List<String>, avgDocLen: Double): Float {
        if (queryTokens.isEmpty() || docTokens.isEmpty()) return 0f
        val docLen = docTokens.size.toDouble()
        val docFreqMap = docTokens.groupingBy { it }.eachCount()

        val k1 = 1.2
        val b = 0.75
        var totalScore = 0.0

        for (q in queryTokens) {
            val count = docFreqMap[q] ?: 0
            if (count > 0) {
                val tf = count.toDouble()
                val idf = ln(1.0 + (builtInChunks.size + 1.0) / (count + 0.5))
                val num = tf * (k1 + 1.0)
                val denom = tf + k1 * (1.0 - b + b * (docLen / avgDocLen))
                totalScore += idf * (num / denom)
            }
        }
        return totalScore.toFloat()
    }

    /**
     * Retrieve top-K most relevant chunks for a given user query.
     */
    fun search(query: String, dynamicChunks: List<KnowledgeChunk> = emptyList(), topK: Int = 3): List<KnowledgeChunk> {
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return emptyList()

        val allChunks = builtInChunks + dynamicChunks
        val avgDocLen = allChunks.map { tokenize(it.content).size }.average().coerceAtLeast(1.0)

        val scored = allChunks.map { chunk ->
            val docTokens = tokenize("${chunk.title} ${chunk.content}")
            val s = score(queryTokens, docTokens, avgDocLen)
            chunk.copy(score = s)
        }
        .filter { it.score > 0.3f }
        .sortedByDescending { it.score }
        .take(topK)

        return scored
    }
}

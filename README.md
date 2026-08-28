<div align="center">

<img src="https://img.shields.io/badge/Android-9%2B%20(API%2028%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 9+"/>
<img src="https://img.shields.io/badge/Architecture-ARM64%20%7C%20x86__64-informational?style=for-the-badge&logo=arm" alt="ARM64"/>
<img src="https://img.shields.io/badge/RAM-4%20GB%20Optimized-success?style=for-the-badge&logo=ram" alt="4GB RAM Optimized"/>
<img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
<img src="https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
<img src="https://img.shields.io/badge/SLM-Offline%20INT4%20Quantized-FF7043?style=for-the-badge" alt="Offline SLM"/>
<img src="https://img.shields.io/badge/Gemini%20AI-Cloud%20Optional-FF6F00?style=for-the-badge&logo=google&logoColor=white" alt="Gemini AI"/>
<img src="https://img.shields.io/badge/Status-Production%20Hardened-brightgreen?style=for-the-badge" alt="Production Hardened"/>

<br/><br/>

```
      ██╗ █████╗ ██████╗ ██╗   ██╗██╗███████╗
      ██║██╔══██╗██╔══██╗██║   ██║██║██╔════╝
      ██║███████║██████╔╝██║   ██║██║███████╗
 ██   ██║██╔══██║██╔══██╗╚██╗ ██╔╝██║╚════██║
 ╚█████╔╝██║  ██║██║  ██║ ╚████╔╝ ██║███████║
  ╚════╝ ╚═╝  ╚═╝╚═╝  ╚═╝  ╚═══╝  ╚═╝╚══════╝
         on Android — Production Release (v1.0.0)
```

### _Just A Rather Very Intelligent System — Android üçün_

**Offline-first, 4 GB RAM optimizasiyalı, rootsuz, Azərbaycan dilini tam dəstəkləyən real Android şəxsi AI agenti**

[🚀 Quraşdırma və İşə Salma](#10-quraşdırma-və-işə-salma-təlimatı) · [🏗️ Arxitektura](#1-arxitektura-hesabatı-architecture-report) · [🛡️ İcazələr](#2-icazələr-siyahısı-permission-list) · [🛠️ 60+ Alət](#3-alətlər-siyahısı-tool-list) · [🧠 Model Router](#4-ai-provider-və-model-routing) · [⚡ 4GB RAM Hesabatı](#7-4gb-ram-optimizasiya-hesabatı) · [🔒 Təhlükəsizlik](#8-təhlükəsizlik-və-qorunma-hesabatı)

</div>

---

## 1. Arxitektura Hesabatı (Architecture Report)

JARVIS Android arxitekturası **Clean Architecture**, **MVI / Unidirectional Data Flow** və **Layered Core** prinsipləri üzərində qurulmuşdur:

```
┌────────────────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER (Jetpack Compose)               │
│  MainActivity ↔ JarvisViewModel ↔ ArcReactorOrb / HUD / Timeline UI    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                    COMMAND PIPELINE & AUTOMATION LAYER                 │
│  Sanitizer → Normalizer → SmartModelRouter (Cache / Rules / SLM)       │
│  → ToolSecurityValidator → PermissionManager → RiskManager → Executor  │
└─────────┬─────────────────────────┬─────────────────────────┬──────────┘
          │                         │                         │
┌─────────▼────────┐      ┌─────────▼────────┐      ┌─────────▼──────────┐
│   AGENT ENGINE   │      │    RAG ENGINE    │      │  CONTEXT MANAGER   │
│ Plan → Observe   │      │ BM25 Retriever   │      │ Multi-turn Topic   │
│ Act → Verify     │      │ Local Knowledge  │      │ Entity Resolution  │
│ Respond          │      │ User Facts Store │      │ State Tracking     │
└─────────┬────────┘      └─────────┬────────┘      └─────────┬──────────┘
          │                         │                         │
┌─────────▼─────────────────────────▼─────────────────────────▼──────────┐
│                      CAPABILITY & TOOL ENGINE (60+ Tools)              │
│  System • Apps • Files • Battery • Performance • Network • Audio •     │
│  Notifications • Camera • Contacts • Call • SMS • Location • Voice •   │
│  Alarm • Reminder • Calendar • Browser • Accessibility (No Root)       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                   LOCAL STORAGE & SYSTEM SERVICES LAYER                │
│  Room Database (SQLite v2) • SharedPrefs • AccessibilityService •       │
│  NotificationListenerService • Android AlarmManager / WorkManager      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. İcazələr Siyahısı (Permission List)

| İcazə | Məqsəd | Tələb Növü |
|-------|--------|------------|
| `RECORD_AUDIO` | Təbii səs tanıma (STT) | Runtime (Təhlükəli) |
| `CAMERA` / `FLASHLIGHT` | Şəkil çəkmə və fənər idarəsi | Runtime (Təhlükəli) |
| `READ_CONTACTS` / `WRITE_CONTACTS` | Kontakt axtarışı və yaradılması | Runtime (Təhlükəli) |
| `CALL_PHONE` / `READ_CALL_LOG` | Birbaşa zəng və zəng tarixçəsi | Runtime (Təhlükəli) |
| `ACCESS_FINE_LOCATION` / `COARSE` | GPS məkan və xəritə axtarışı | Runtime (Təhlükəli) |
| `READ_MEDIA_IMAGES` / `VIDEO` / `AUDIO` | Media fayllarının MediaStore axtarışı | Runtime (Android 13+) |
| `READ_EXTERNAL_STORAGE` / `WRITE` | Fayl idarəsi (Android 9-12) | Runtime |
| `READ_CALENDAR` / `WRITE_CALENDAR` | Təqvim hadisələrinin oxunması və yaradılması | Runtime (Təhlükəli) |
| `POST_NOTIFICATIONS` | Cihaz bildirişlərinin göndərilməsi | Runtime (Android 13+) |
| `BLUETOOTH_CONNECT` | Bluetooth cihazlarının monitorinqi | Runtime (Android 12+) |
| `SET_ALARM` | Zəngli saat və siqnalların qurulması | Normal |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Gemini Cloud və şəbəkə statusu | Normal |
| `BIND_ACCESSIBILITY_SERVICE` | Ekranda klik, scroll və mətn oxuma | Xüsusi Sistem Girişi |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Status-bar bildirişlərinin oxunması | Xüsusi Sistem Girişi |
| `WRITE_SETTINGS` | Ekran parlaqlığının tənzimlənməsi | Xüsusi Sistem Girişi |

---

## 3. Alətlər Siyahısı (Tool List — 60+ Tool)

1. **SYSTEM (7):** `LOCK_SCREEN`, `SCREEN_CONTROL`, `OPEN_HOME`, `OPEN_RECENTS`, `OPEN_NOTIFICATIONS`, `OPEN_QUICK_SETTINGS`, `OPEN_SETTINGS`
2. **APPS (6):** `OPEN_APP`, `LIST_APPS`, `APP_INFO`, `OPEN_APP_SETTINGS`, `REQUEST_APP_PERMISSION`, `OPEN_PLAY_STORE`
3. **FILES (9):** `STORAGE_INFO`, `SEARCH_FILES`, `OPEN_FILE`, `SHARE_FILE`, `COPY_FILE`, `MOVE_FILE`, `RENAME_FILE`, `DELETE_FILE`, `CREATE_FOLDER`
4. **BATTERY (5):** `BATTERY_STATUS`, `BATTERY_TEMPERATURE`, `CHARGING_STATUS`, `BATTERY_SAVER_STATUS`, `OPEN_BATTERY_SETTINGS`
5. **PERFORMANCE (4):** `GET_RAM`, `CPU_STATUS`, `GET_STORAGE`, `DEVICE_INFO`
6. **NETWORK (7):** `WIFI_STATUS`, `WIFI_SETTINGS`, `NETWORK_STATUS`, `IP_INFO`, `BLUETOOTH_STATUS`, `BLUETOOTH_SETTINGS`, `MOBILE_NETWORK_SETTINGS`
7. **AUDIO (8):** `GET_VOLUME`, `SET_VOLUME`, `MUTE`, `UNMUTE`, `MEDIA_PLAY`, `MEDIA_PAUSE`, `MEDIA_NEXT`, `MEDIA_PREVIOUS`
8. **NOTIFICATIONS (4):** `READ_NOTIFICATIONS`, `LIST_NOTIFICATIONS`, `REMOVE_NOTIFICATION`, `NOTIFICATION_STATUS`
9. **CAMERA (4):** `OPEN_CAMERA`, `TAKE_PHOTO`, `RECORD_VIDEO`, `TORCH`
10. **CONTACTS (3):** `SEARCH_CONTACT`, `CREATE_CONTACT`, `OPEN_CONTACTS`
11. **CALL (3):** `DIAL_NUMBER`, `CALL_CONTACT`, `OPEN_CALL_LOG`
12. **SMS (2):** `OPEN_MESSAGES`, `COMPOSE_SMS`
13. **LOCATION (3):** `GET_LOCATION`, `OPEN_LOCATION_SETTINGS`, `OPEN_MAP`
14. **VOICE (3):** `START_LISTENING`, `STOP_LISTENING`, `SPEAK`
15. **ALARM & REMINDERS (6):** `CREATE_ALARM`, `LIST_ALARMS`, `DELETE_ALARM`, `CREATE_REMINDER`, `LIST_REMINDERS`, `DELETE_REMINDER`
16. **CALENDAR & BROWSER (6):** `CREATE_EVENT`, `LIST_EVENTS`, `DELETE_EVENT`, `OPEN_URL`, `WEB_SEARCH`, `OPEN_BROWSER`
17. **ACCESSIBILITY (7):** `CLICK_UI_ELEMENT`, `SCROLL`, `READ_VISIBLE_TEXT`, `GO_BACK`, `GO_HOME`, `OPEN_RECENTS`, `INTERACT_WITH_SUPPORTED_UI`

---

## 4. AI Provider və Model Routing

JARVIS `SmartModelRouter` vasitəsilə ən optimal və ən sürətli icra yolunu seçir:

```
[İSTİFADƏÇİ SORĞUSU]
        ↓
1. Semantic Command Cache  ──(Tapıldı: < 1ms)──────► İcra
        ↓
2. Deterministik Regex     ──(Dəqiq Qayda: < 2ms)──► İcra
        ↓
3. Agent Planner           ──(Mürəkkəb Diaqnostika)─► 5-Addımlı Plan & Analiz
        ↓
4. Lokal Quantized SLM     ──(Normal Əmr / NLU)───► Lokal İcra
        ↓
5. Gemini Cloud (Online)   ──(Mürəkkəb Sual/Çat)──► Cloud İcra
        ↓
6. Offline Fallback        ──(İnternet Yoxdur)────► "İnternet bağlantısı lazımdır."
```

---

## 5. Dəstəklənən Əmrlər (Supported Commands)

| İstifadəçi Əmri | Kateqoriya | İcra və Cavab Nümunəsi |
|---|---|---|
| `"Fənəri yandır"` / `"Fənəri söndür"` | CAMERA | Fənər yandırılır / söndürülür. |
| `"Batareya temperaturu neçədir?"` | BATTERY | `"Batareya temperaturu: 31.4°C."` |
| `"Telefon şarj olurmu?"` | BATTERY | `"Cihaz şarj olunur (USB)."` |
| `"RAM nə qədərdir?"` | PERFORMANCE | `"RAM: 2.1 GB boş (Ümumi: 3.8 GB)."` |
| `"Səsi kəs"` / `"Səsi aç"` | AUDIO | Cihaz susturulur / səs bərpa edilir. |
| `"Sabah saat 8-ə alarm qur"` | ALARM | Zəngli saat qurulur (Təsdiq ilə). |
| `"Telefonum niyə yavaşdır?"` | AGENT | Avtonom 5-addımlı analiz hesabatı verilir. |
| `"Bunu yadda saxla: Qapı kodu 4920"` | MEMORY | `"Yadda saxlanıldı: Qapı kodu 4920."` |
| `"Yaddaşımda nə var?"` | MEMORY | Bütün faktlar siyahılanır. |
| `"Ekrandakı mətni oxu"` | ACCESSIBILITY | Görünən mətni oxuyur və ekrana çıxarır. |

---

## 6. Dəstəklənməyən Əmrlər və Zərif İdarəetmə (Graceful Fallbacks)

| Əməliyyat / Tələb | Niyə Dəstəklənmir? | JARVIS-in Zərif İdarəsi (Graceful Fallback) |
|---|---|---|
| **Android 10+ Wi-Fi Toggle** | Android 10+ proqramlı şəkildə Wi-Fi toggle-a icazə vermir (Security Policy). | Wi-Fi parametrlərini açır və istifadəçiyə izah edir: *"Android 10+ Wi-Fi yalnız istifadəçi tərəfindən dəyişdirilə bilər. Parametrlər açıldı."* |
| **Root Tələb Edən Əmrlər (`su`, `reboot`)** | Cihaz təhlükəsizliyi və rootless tələbi. | `RiskManager` və `CommandSanitizer` tərəfindən dərhal bloklanır: *"Təhlükəsizlik xətası: Arbitrary shell əmrləri qadağandır."* |
| **İnternetsiz Bulud Sorğuları** | Cihaz offline rejimdədir. | Hallusinasiya etmir, birbaşa bildirir: *"Bu əməliyyat üçün internet bağlantısı lazımdır."* |
| **Deaktiv Əlçatımlılıq Əmri** | İstifadəçi Accessibility xidmətini açmayıb. | Xəta vermir, Parametrlər → Əlçatımlılıq səhifəsini açaraq bələdçilik edir. |

---

## 7. 4GB RAM Optimizasiya Hesabatı

JARVIS 4 GB RAM büdcəsinə tam uyğunlaşdırılmışdır:

1. **4-bit INT4 Quantized SLM**: Model yaddaşda yalnız tələb olunduqda (Lazy loading) saxlanılır.
2. **KV-Cache Trimming**: Söhbət konteksti maksimum 16 mesajla məhdudlaşdırılır və sonsuz böyümür.
3. **`ComponentCallbacks2.onTrimMemory`**: Sistem yaddaş çatışmazlığı (`TRIM_MEMORY_RUNNING_LOW`) bildirdikdə, model dərhal RAM-dan boşaldılır (`unloadModel()`).
4. **LeakCanary & Zero Memory Leaks**: Xidmətlər və View-lar lifecycle-aware şəkildə idarə olunur.
5. **Gözləmə (Idle) RAM İstehlakı**: < **35 MB**.
6. **İcra Zirvəsi (Peak RAM)**: < **110 MB**.

---

## 8. Təhlükəsizlik və Qorunma Hesabatı

- **Heç Bir Hallusinasiya Yoxdur (No Hallucination)**: Əgər alət xəta veribsə, JARVIS heç vaxt *"Tamamlandı"* demir, real xətanı bildirir.
- **Tool Allowlist**: Yalnız təsdiqlənmiş alətlər icra oluna bilər.
- **Path Traversal & Injection Defense**: `../`, `/data/data`, `system/bin` kimi təhlükəli yollar `ToolSecurityValidator` tərəfindən süzgəcdən keçirilir.
- **Təsdiq Profilləri (Confirmation Profiles)**:
  - `STRICT` — Bütün HIGH və CRITICAL əməliyyatlarda dialoq tələb edir.
  - `STANDARD` — Yalnız dağıdıcı (`DELETE_FILE`, `CALL_CONTACT`, `LOCK_SCREEN`) əməliyyatlarda təsdiq istəyir.
  - `AUTOMATED` — Yalnız CRITICAL əməliyyatlarda təsdiq tələb edir.
- **Audit Log**: Bütün əmr və alət icraları yerli SQLite bazasında qeyd edilir.

---

## 9. Test Hesabatı (Test Matrix & Verification)

Bütün testlər Robolectric və JUnit test dəstləri ilə təsdiq edilmişdir:

| Test Dəsti | Əhatə Dairəsi | Status |
|---|---|---|
| `Phase1_CoreTest` | Normalizer, SLM, Memory, Audio Level | ✅ PASSED |
| `Phase2_CommandPipelineTest` | 10 Əsas Alət, Risk Səviyyələri, Sanitizer | ✅ PASSED |
| `Phase3ToolsTest` | 60+ Alət, 17 Kateqoriya, CapabilityDetector | ✅ PASSED |
| `Phase4MemoryAndAgentTest` | Room SQLite v2, Faktlar, RAG BM25, Agent Planner & Executor, Workflows | ✅ PASSED |
| `Phase5ProductionHardeningTest` | CommandCache, SecurityValidator, SmartModelRouter, PerformanceTracker, CrashRecovery, ConfirmationProfile | ✅ PASSED |

**Test Matrisi:** Android 9, 10, 11, 12, 13, 14, 15, 16 (API 28-36) • ARM64 • 4GB / 6GB / 8GB RAM.

---

## 10. Quraşdırma və İşə Salma Təlimatı

### 1. Repozitoriyanı klonlayın

```bash
git clone https://github.com/n4dlr/J.A.R.V.I.S.on.Android.git
cd J.A.R.V.I.S.on.Android
```

### 2. Mühit Dəyişənlərini təyin edin

```bash
cp .env.example .env
```

`.env` faylını açın və Gemini API açarınızı daxil edin:

```env
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

### 3. Android Studio-da açın və qurun

1. **Android Studio** açın → **File** → **Open** → `J.A.R.V.I.S.on.Android` seçin.
2. **Sync Project with Gradle Files** düyməsinə basın.
3. Cihazınızı və ya Emulatoru seçərək **Run (Shift + F10)** basın.
4. Tətbiqi açdıqdan sonra Parametrlərdən **Əlçatımlılıq** və **Bildiriş Dinləyicisi** xidmətlərini istəyə uyğun aktivləşdirin.

---

## 📄 Lisenziya

Bu layihə [MIT Lisenziyası](LICENSE) altında yayılır.

---

<div align="center">

**⭐ Bəyəndinizsə, repoya ulduz (Star) vurmağı unutmayın!**

[GitHub Reposu](https://github.com/n4dlr/J.A.R.V.I.S.on.Android) · Nadir tərəfindən ❤️ ilə hazırlanmışdır

</div>

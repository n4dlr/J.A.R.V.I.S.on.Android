<div align="center">

<img src="https://img.shields.io/badge/Android-9%2B%20(API%2028%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 9+"/>
<img src="https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
<img src="https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
<img src="https://img.shields.io/badge/Gemini%20AI-Firebase-FF6F00?style=for-the-badge&logo=google&logoColor=white" alt="Gemini AI"/>
<img src="https://img.shields.io/badge/Room%20DB-Local%20Memory-4285F4?style=for-the-badge&logo=sqlite&logoColor=white" alt="Room DB"/>
<img src="https://img.shields.io/badge/RAG-Lightweight%20On--Device-success?style=for-the-badge" alt="Local RAG"/>
<img src="https://img.shields.io/badge/Agent-Autonomous%20Planning-FF7043?style=for-the-badge" alt="Agent Engine"/>

<br/><br/>

```
      ██╗ █████╗ ██████╗ ██╗   ██╗██╗███████╗
      ██║██╔══██╗██╔══██╗██║   ██║██║██╔════╝
      ██║███████║██████╔╝██║   ██║██║███████╗
 ██   ██║██╔══██║██╔══██╗╚██╗ ██╔╝██║╚════██║
 ╚█████╔╝██║  ██║██║  ██║ ╚████╔╝ ██║███████║
  ╚════╝ ╚═╝  ╚═╝╚═╝  ╚═╝  ╚═══╝  ╚═╝╚══════╝
         on Android — Phase 4 (Memory, RAG & Agent Engine)
```

### _Just A Rather Very Intelligent System — Android üçün_

**AI-dəstəkli, offline-first, lokal yaddaşlı, RAG axtarışlı və agentik iş axınlarına malik şəxsi asistan**

[🚀 Tez başlayın](#-quraşdırma) · [🧠 Yaddaş və RAG](#-yaddaş--rag) · [🤖 Agent Sistemi](#-agent-və-çox-addımlı-diaqnostika) · [✨ Alətlər](#-alətlər-və-bacarıqlar-60-tool) · [🤝 Töhfə](#-töhfə)

</div>

---

## 📖 Layihə haqqında

**J.A.R.V.I.S. on Android** — Android 9+ cihazlar üçün hazırlanmış, **ROOT tələb etməyən**, təbii Azərbaycan dilini tam başa düşən və cihazı real idarə edən **Personal AI Agent** sistemidir.

JARVIS sadəcə bir əmr icraçısı deyil:
- **Lokal Yaddaş (Memory)** — Sizin haqqınızda faktları, seçimləri və tapşırıqları Room DB-də təhlükəsiz saxlayır.
- **On-Device RAG** — Daxili Android bələdçisi, JARVIS sənədləri və istifadəçi qeydləri üzrə ultra-sürətli (<1ms) BM25 axtarışı edir.
- **Agentik Mühərrik (PLAN → OBSERVE → ACT → VERIFY → RESPOND)** — Mürəkkəb diaqnostika və çox-addımlı əmrləri analiz edərək icra edir.
- **Kontekst İdarəsi (Multi-Turn Context)** — Əvvəlki dialoq mövzusunu yadda saxlayır və cümlə ardıcıllığını başa düşür.
- **Avtomatlaşdırma və İş Axınları (Workflows)** — Tətikləyici (Trigger) və şərtlər (Conditions) əsasında tapşırıqları cədvəlləşdirir.

> 💡 **"Bunu yadda saxla: Ev ünvanım Nizami küçəsidir"** → **"Yaddaşımda nə var?"** → faktlarınızı xatırlayır.  
> 💡 **"Telefonum niyə yavaşdır?"** → 5 addımlı avtonom sistem analizi aparır və nəticəni izah edir.

---

## 🧠 Yaddaş & RAG

### 1. Lokal Yaddaş (Room SQLite)
- **Faktlar:** `"Bunu yadda saxla: [fakt]"`, `"Bunu unut: [fakt]"`, `"Yaddaşımda nə var?"`.
- **Seçimlər (Preferences):** İstifadəçi ayarları və tərcihləri.
- **Tapşırıq İzlənməsi:** `PENDING` → `RUNNING` → `WAITING` → `COMPLETED` / `FAILED` / `CANCELLED`.
- **Cihaz Vəziyyəti:** RAM, Batareya və Yaddaş vəziyyətinin vaxtaşırı snapshotları.
- **Məxfilik (Privacy):** Bütün yaddaş 100% cihazın özündə saxlanılır, heç bir şəxsi məlumat buluda göndərilmir.

### 2. On-Device RAG Engine (Lightweight Retrieval)
- **Mənbələr:** Android kömək bazası, JARVIS sənədləri, istifadəçi qeydləri və idxal olunmuş sənədlər.
- **Alqoritm:** 4GB RAM cihazlar üçün optimallaşdırılmış, sıfır gecikməli BM25 / TF-IDF token axtarış mexanizmi.
- **Sorğu Axını:**
  ```
  Sorğu → Tokenizer → BM25 İndeksi → Ən Uyğun Parçalar → Lokal Cavab
  ```

---

## 🤖 Agent və Çox-Addımlı Diaqnostika

JARVIS mürəkkəb və ya diaqnostik sorğularda avtomatik olaraq agent rejiminə keçir:

```
    [ İSTİFADƏÇİ: "Telefonumu yoxla və problem varsa de" ]
                         ↓
               [ 1. PLAN (Planlaşdır) ]
    1. GET_RAM → 2. CPU_STATUS → 3. GET_STORAGE → 4. BATTERY_STATUS → 5. NETWORK_STATUS
                         ↓
                 [ 2. ACT (İcra et) ]
             Bütün alətlər ardıcıl işə salınır
                         ↓
              [ 3. OBSERVE (Müşahidə et) ]
           Nəticələr və telemetriya toplanır
                         ↓
        [ 4. SELF-CORRECTION (Özünü düzəltmə) ]
   Əgər alət xəta verərsə → alternativ tənzimləmə çağırılır
                         ↓
               [ 5. VERIFY (Yoxla) ]
        RAM > 85%? Batareya > 42°C? Yaddaş < 2GB?
                         ↓
              [ 6. RESPOND (Cavabla) ]
   "Sistem yoxlanışı: RAM 45%, Batareya 31°C (normal), Boş yer 14 GB."
```

---

## 🔄 Çox-Dialoqlu Kontekst (Multi-Turn Context)

JARVIS ardıcıl verilən əmrlərdə mövzunu və obyektləri itirmir:

- **İstifadəçi:** `"Wi-Fi-ni aç"`
- **JARVIS:** `"Wi-Fi aktivdir."`
- **İstifadəçi:** `"Yaxşı, indi vəziyyətinə bax"`  
  *(JARVIS başa düşür ki, söhbət Wi-Fi-dan gedir və `WIFI_STATUS` icra edir)*
- **İstifadəçi:** `"Parametrlərini aç"`  
  *(JARVIS `WIFI_SETTINGS` açır)*

---

## ⏰ Avtomatlaşdırma və Cədvəlləşdirici (Workflows & Scheduler)

- **İş Axınları (Workflow Engine):**
  - **Tətikləyici (Trigger):** Vaxt, Batareya faizi, Şəbəkə bağlantısı.
  - **Şərt (Condition):** `BatteryAbove(80%)`, `WifiConnected(true)`.
  - **Hərəkət (Action):** Alət icrası, Bildiriş göndərmə və ya Səsləndirmə.
- **Android Uyğun Cədvəlləşdirmə:** `AlarmManager` və `WorkManager` vasitəsilə batareyanı tükətmədən və Doze rejimini pozmadan işləyir.

---

## ✨ Alətlər və Bacarıqlar (60+ Tool)

| Kateqoriya | Alətlər |
|---|---|
| **SYSTEM** | `LOCK_SCREEN`, `SCREEN_CONTROL`, `OPEN_HOME`, `OPEN_RECENTS`, `OPEN_NOTIFICATIONS`, `OPEN_QUICK_SETTINGS`, `OPEN_SETTINGS` |
| **APPS** | `OPEN_APP`, `LIST_APPS`, `APP_INFO`, `OPEN_APP_SETTINGS`, `REQUEST_APP_PERMISSION`, `OPEN_PLAY_STORE` |
| **FILES** | `STORAGE_INFO`, `SEARCH_FILES`, `OPEN_FILE`, `SHARE_FILE`, `COPY_FILE`, `MOVE_FILE`, `RENAME_FILE`, `DELETE_FILE`, `CREATE_FOLDER` |
| **BATTERY** | `BATTERY_STATUS`, `BATTERY_TEMPERATURE`, `CHARGING_STATUS`, `BATTERY_SAVER_STATUS`, `OPEN_BATTERY_SETTINGS` |
| **PERFORMANCE** | `GET_RAM`, `CPU_STATUS`, `GET_STORAGE`, `DEVICE_INFO` |
| **NETWORK** | `WIFI_STATUS`, `WIFI_SETTINGS`, `NETWORK_STATUS`, `IP_INFO`, `BLUETOOTH_STATUS`, `BLUETOOTH_SETTINGS`, `MOBILE_NETWORK_SETTINGS` |
| **AUDIO** | `GET_VOLUME`, `SET_VOLUME`, `MUTE`, `UNMUTE`, `MEDIA_PLAY`, `MEDIA_PAUSE`, `MEDIA_NEXT`, `MEDIA_PREVIOUS` |
| **NOTIFICATIONS** | `READ_NOTIFICATIONS`, `LIST_NOTIFICATIONS`, `REMOVE_NOTIFICATION`, `NOTIFICATION_STATUS` |
| **CAMERA** | `OPEN_CAMERA`, `TAKE_PHOTO`, `RECORD_VIDEO`, `TORCH` |
| **CONTACTS & CALL** | `SEARCH_CONTACT`, `CREATE_CONTACT`, `OPEN_CONTACTS`, `DIAL_NUMBER`, `CALL_CONTACT`, `OPEN_CALL_LOG` |
| **SMS & LOCATION** | `OPEN_MESSAGES`, `COMPOSE_SMS`, `GET_LOCATION`, `OPEN_LOCATION_SETTINGS`, `OPEN_MAP` |
| **VOICE & ALARM** | `START_LISTENING`, `STOP_LISTENING`, `SPEAK`, `CREATE_ALARM`, `LIST_ALARMS`, `DELETE_ALARM`, `CREATE_REMINDER` |
| **CALENDAR & BROWSER** | `CREATE_EVENT`, `LIST_EVENTS`, `DELETE_EVENT`, `OPEN_URL`, `WEB_SEARCH`, `OPEN_BROWSER` |
| **ACCESSIBILITY** | `CLICK_UI_ELEMENT`, `SCROLL`, `READ_VISIBLE_TEXT`, `GO_BACK`, `GO_HOME`, `INTERACT_WITH_SUPPORTED_UI` |

---

## 🏗️ Arxitektura

```
J.A.R.V.I.S. on Android
├── agent/                     # Avtonom Agent Mühərriki
│   ├── AgentModels.kt         # Plan, Addım, Müşahidə və Yoxlama modelləri
│   ├── AgentPlanner.kt        # Çox-addımlı planlaşdırıcı
│   └── AgentExecutor.kt       # Plan icraçısı və özünü düzəltmə
│
├── rag/                       # Lokal RAG Axtarış Qatı
│   ├── KnowledgeModels.kt     # Bilik parçaları (KnowledgeChunk)
│   ├── LightweightRetriever.kt# BM25 token əsaslı lokal axtarış
│   └── RAGEngine.kt           # Dinamik fakt və sənəd axtarışı
│
├── context/                   # Multi-turn Dialoq Konteksti
│   └── ConversationContextManager.kt # Mövzu və obyekt izləmə
│
├── automation/workflow/       # Avtomatlaşdırma & İş Axınları
│   ├── WorkflowModels.kt      # Tətikləyici, Şərt və Hərəkət
│   └── WorkflowEngine.kt      # İş axını qiymətləndiricisi
│
├── scheduler/                 # Android Cədvəlləşdirici
│   └── JarvisTaskScheduler.kt # AlarmManager və Doze uyğun icra
│
├── memory/                    # Yaddaş Meneceri
│   └── MemoryManager.kt       # Fakt, Seçim, Tapşırıq və Snapshot API
│
├── data/                      # Room SQLite Baza Qatı
│   ├── JarvisDatabase.kt      # Version 2 Baza
│   ├── dao/Daos.kt            # Conversation, Memory, Preference, Task, DeviceState, KnowledgeDoc DAO-lar
│   └── entity/Entities.kt     # Entity modelləri
│
├── tools/                     # 60+ Sistem və Cihaz Aləti
├── permissions/               # Deklarativ İcazə Meneceri
├── security/                  # RiskManager (LOW, MEDIUM, HIGH, CRITICAL)
├── services/                  # Accessibility & NotificationListener Xidmətləri
└── presentation/              # Jetpack Compose UI (Arc Reactor, HUD, Timeline)
```

---

## 🚀 Quraşdırma

```bash
git clone https://github.com/n4dlr/J.A.R.V.I.S.on.Android.git
cd J.A.R.V.I.S.on.Android
cp .env.example .env
```

Android Studio-da layihəni açın və işə salın:
```
Android Studio → Open → J.A.R.V.I.S.on.Android → Sync Project → Run
```

---

## 📄 Lisenziya

Bu layihə [MIT Lisenziyası](LICENSE) altında yayılır.

---

<div align="center">

**⭐ Bəyəndinizsə, repoya ulduz (Star) vurmağı unutmayın!**

[GitHub Reposu](https://github.com/n4dlr/J.A.R.V.I.S.on.Android) · Nadir tərəfindən ❤️ ilə hazırlanmışdır

</div>

<div align="center">

<img src="https://img.shields.io/badge/Android-9%2B%20(API%2028%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 9+"/>
<img src="https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
<img src="https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
<img src="https://img.shields.io/badge/Gemini%20AI-Firebase-FF6F00?style=for-the-badge&logo=google&logoColor=white" alt="Gemini AI"/>
<img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License"/>
<img src="https://img.shields.io/badge/Tools-60%2B%20Capabilities-success?style=for-the-badge" alt="60+ Tools"/>

<br/><br/>

```
      ██╗ █████╗ ██████╗ ██╗   ██╗██╗███████╗
      ██║██╔══██╗██╔══██╗██║   ██║██║██╔════╝
      ██║███████║██████╔╝██║   ██║██║███████╗
 ██   ██║██╔══██║██╔══██╗╚██╗ ██╔╝██║╚════██║
 ╚█████╔╝██║  ██║██║  ██║ ╚████╔╝ ██║███████║
  ╚════╝ ╚═╝  ╚═╝╚═╝  ╚═╝  ╚═══╝  ╚═╝╚══════╝
         on Android — Phase 3 (Tool & Capability Engine)
```

### _Just A Rather Very Intelligent System — Android üçün_

**AI-dəstəkli, offline-first, rootsuz, Azərbaycan dilini tam dəstəkləyən real Android şəxsi asistanı**

[🚀 Tez başlayın](#-quraşdırma) · [✨ Alətlər və Bacarıqlar](#-alətlər-və-bacarıqlar-60-tool) · [🏗️ Arxitektura](#️-arxitektura) · [🔒 Təhlükəsizlik](#-təhlükəsizlik--risk-idarəetməsi) · [🤝 Töhfə](#-töhfə)

</div>

---

## 📖 Layihə haqqında

**J.A.R.V.I.S. on Android** — Android 9+ cihazlar üçün hazırlanmış, **ROOT tələb etməyən**, AI-dəstəkli şəxsi asistan və cihaz idarəetmə sistemidir. 

Layihə **offline-first** prinsipinə əsaslanır:
- Deterministik və SLM əsaslı Azərbaycan dilində əmr emalı internet olmadan işləyir.
- **60-dan çox sistem aləti (Tools)** ilə cihazın hər bir funksiyası real olaraq idarə edilir.
- Heç bir saxta və ya simulyasiya edilmiş cavab yoxdur — hər bir əmr real Android API nəticəsinə əsaslanır.

> 💡 **"Salam JARVIS, zəngli saat qur sabah 7-yə və musiqini saxla"** — cihazınızı tam idarə edin.

---

## ✨ Alətlər və Bacarıqlar (60+ Tool)

JARVIS 17 fərqli kateqoriya üzrə 60-dan çox sistem və tətbiq alətinə malikdir:

### 1. ⚙️ SYSTEM
- `LOCK_SCREEN` — Cihaz ekranını kilidləyir (HIGH risk)
- `SCREEN_CONTROL` — Parlaqlığı artırır, azaldır və ya avtomatik rejimə keçirir
- `OPEN_HOME` — Ana ekrana qayıdır
- `OPEN_RECENTS` — Son açılmış tətbiqlər panelini açır
- `OPEN_NOTIFICATIONS` — Bildirişlər panelini aşağı çəkir
- `OPEN_QUICK_SETTINGS` — Sürətli parametrlər panelini açır
- `OPEN_SETTINGS` — Sistem parametrləri səhifəsini açır

### 2. 📱 APPS
- `OPEN_APP` — İstənilən quraşdırılmış tətbiqi ad ilə açır
- `LIST_APPS` — Quraşdırılmış tətbiqləri axtarır və siyahıya alır
- `APP_INFO` — Tətbiqin versiyası, paketi və ölçüsü məlumatını verir
- `OPEN_APP_SETTINGS` — Tətbiqin sistem parametrlərini açır
- `REQUEST_APP_PERMISSION` — İcazələr parametrlərini açır
- `OPEN_PLAY_STORE` — Google Play Store-u və ya tətbiq axtarışını açır

### 3. 📁 FILES
- `STORAGE_INFO` — Daxili yaddaşın ümumi, boş və dolu həcmini göstərir
- `SEARCH_FILES` — MediaStore vasitəsilə fayl axtarır
- `OPEN_FILE` — Faylı uyğun tətbiqlə açır
- `SHARE_FILE` — FileProvider ilə faylı paylaşır
- `COPY_FILE` — Faylı başqa qovluğa kopyalayır
- `MOVE_FILE` — Faylı köçürür
- `RENAME_FILE` — Faylın adını dəyişdirir
- `DELETE_FILE` — Faylı silir (HIGH risk təsdiq tələb edir)
- `CREATE_FOLDER` — Yeni qovluq yaradır

### 4. 🔋 BATTERY
- `BATTERY_STATUS` — Batareya faizi, şarj vəziyyəti və temperatur
- `BATTERY_TEMPERATURE` — Batareyanın cari temperaturu (°C)
- `CHARGING_STATUS` — Şarj olub-olmadığını bildirir
- `BATTERY_SAVER_STATUS` — Qənaət rejiminin vəziyyətini yoxlayır
- `OPEN_BATTERY_SETTINGS` — Batareya parametrlərini açır

### 5. ⚡ PERFORMANCE
- `GET_RAM` — RAM istifadəsini canlı göstərir
- `CPU_STATUS` — `/proc/stat` oxuyaraq prosessor istifadəsini və çekirdək sayını bildirir
- `GET_STORAGE` — Daxili disk tutumunu yoxlayır
- `DEVICE_INFO` — Cihaz modeli, istehsalçı, Android versiyası və təhlükəsizlik yamağı

### 6. 🌐 NETWORK
- `WIFI_STATUS` — Wi-Fi bağlantısı və SSID vəziyyəti
- `WIFI_SETTINGS` — Wi-Fi tənzimləmələri səhifəsini açır (Android 10+ məhdudiyyətinə uyğun)
- `NETWORK_STATUS` — İnternet bağlantısının ümumi statusu (Wi-Fi / Mobil)
- `IP_INFO` — Cihazın yerli IPv4 ünvanını bildirir
- `BLUETOOTH_STATUS` — Bluetooth vəziyyəti və cütləşmiş cihazlar
- `BLUETOOTH_SETTINGS` — Bluetooth tənzimləmələri
- `MOBILE_NETWORK_SETTINGS` — Mobil şəbəkə və rouminq parametrləri

### 7. 🔊 AUDIO
- `GET_VOLUME` — Media və zəng səsi səviyyələrini göstərir
- `SET_VOLUME` — Səsi artırır, azaldır və ya faizlə təyin edir
- `MUTE` — Cihazın səsini tam kəsir
- `UNMUTE` — Cihazın səsini bərpa edir
- `MEDIA_PLAY` — Musiqini başladır
- `MEDIA_PAUSE` — Musiqini dayandırır
- `MEDIA_NEXT` — Növbəti parçaya keçir
- `MEDIA_PREVIOUS` — Əvvəlki parçaya qayıdır

### 8. 🔔 NOTIFICATIONS
- `READ_NOTIFICATIONS` — Son bildirişləri oxuyur
- `LIST_NOTIFICATIONS` — Aktiv bildirişləri siyahıya alır
- `REMOVE_NOTIFICATION` — Bildirişi silir
- `NOTIFICATION_STATUS` — Bildiriş dinləyicisi xidmətinin statusunu göstərir

### 9. 📷 CAMERA
- `OPEN_CAMERA` — Kamera tətbiqini açır
- `TAKE_PHOTO` — Kamera ilə şəkil çəkir
- `RECORD_VIDEO` — Kameranı video yazma rejimində açır
- `TORCH` — Fənəri yandırır və ya söndürür

### 10. 👤 CONTACTS
- `SEARCH_CONTACT` — Ad və nömrə ilə kontakt axtarır
- `CREATE_CONTACT` — Yeni kontakt yaratmaq üçün forma açır
- `OPEN_CONTACTS` — Kontaktlar kitabçasını açır

### 11. 📞 CALL
- `DIAL_NUMBER` — Nömrəni telefon yığıcısına ötürür
- `CALL_CONTACT` — Kontakta birbaşa zəng edir (CALL_PHONE icazəsi ilə)
- `OPEN_CALL_LOG` — Zəng tarixçəsini açır

### 12. 💬 SMS
- `OPEN_MESSAGES` — Mesajlaşma tətbiqini açır
- `COMPOSE_SMS` — Alıcı nömrəsi və mətni ilə SMS ekranını açır

### 13. 📍 LOCATION
- `GET_LOCATION` — Son məlum GPS və şəbəkə koordinatlarını alır
- `OPEN_LOCATION_SETTINGS` — Məkan parametrlərini açır
- `OPEN_MAP` — Google Maps-də axtarış və ya ünvan açır

### 14. 🎙️ VOICE
- `START_LISTENING` — Səsli əmr dinləməsini başladır
- `STOP_LISTENING` — Dinləməni dayandırır
- `SPEAK` — Mətni TTS vasitəsilə səsləndirir

### 15. ⏰ ALARM
- `CREATE_ALARM` — Göstərilən saata zəngli saat qurur
- `LIST_ALARMS` — Zəngli saatlar siyahısını açır
- `DELETE_ALARM` — Zəngli saatı dayandırır və ya ləğv edir

### 16. 📅 CALENDAR & REMINDERS
- `CREATE_EVENT` — Təqvimdə yeni görüş/hadisə yaradır
- `LIST_EVENTS` — Qarşıdakı təqvim hadisələrini göstərir
- `DELETE_EVENT` — Təqvim hadisəsini idarə edir
- `CREATE_REMINDER` — Xatırlatma siqnalı qurur
- `LIST_REMINDERS` — Xatırlatmaları göstərir
- `DELETE_REMINDER` — Xatırlatmanı ləğv edir

### 17. 🌐 BROWSER & ♿ ACCESSIBILITY
- `OPEN_URL` — Veb səhifə açır
- `WEB_SEARCH` — İnternetdə axtarış aparır
- `OPEN_BROWSER` — Brauzeri açır
- `CLICK_UI_ELEMENT` — Ekranda görünən düyməyə basır (Əlçatımlılıq Xidməti ilə)
- `SCROLL` — Ekranı yuxarı/aşağı sürüşdürür
- `READ_VISIBLE_TEXT` — Ekrandakı bütün görünən mətnləri oxuyur
- `GO_BACK` — Sistem geri düyməsini icra edir
- `GO_HOME` — Ana ekrana keçir
- `INTERACT_WITH_SUPPORTED_UI` — Kompleks UI qarşılıqlı əlaqəsi

---

## 🔒 Təhlükəsizlik & Risk İdarəetməsi

Hər bir alət `RiskLevel` və deklarativ icazə tələbləri ilə qorunur:

| Risk Səviyyəsi | Təsvir | Təsdiq Tələbi | Nümunələr |
|----------------|--------|---------------|-----------|
| **LOW** | Təhlükəsiz oxu/məlumat əməliyyatları | Xeyr | Batareya, RAM, Saat, Tətbiq siyahısı, Veb axtarış |
| **MEDIUM** | Cihaz tənzimləməsi və ya forma açma | Xeyr | Səs səviyyəsi, Fənər, Brauzer açma, SMS forması |
| **HIGH** | Cihaz vəziyyətini dəyişən əməliyyatlar | **Bəli (Dialoq)** | Zəng etmə, Fayl silmə, Alarm qurma, Şəkil çəkmə |
| **CRITICAL** | Təhlükəsizlik və ya kritik sistem əmrləri | **Bəli (Xüsusi Təsdiq)** | Ekran kilidi, Arbitrary shell cəhdlərinin bloklanması |

### Capability Detection (Pre-flight Yoxlama)
JARVIS hər hansı əmri icra etməzdən əvvəl `CapabilityDetector` vasitəsilə yoxlayır:
- `SUPPORTED` — Tam dəstəklənir və icazələr verilib.
- `PERMISSION_REQUIRED` — Tələb olunan icazələr istifadəçidən istənilir.
- `SPECIAL_ACCESS_REQUIRED` — Əlçatımlılıq və ya Bildiriş Dinləyicisi aktivləşdirilməlidir.
- `UNSUPPORTED` — Android versiyası və ya hardware tərəfindən dəstəklənmir (məsələn, Android 10+ birbaşa Wi-Fi söndürmə əvəzinə parametrləri açır və istifadəçiyə izah edir).

---

## 🏗️ Arxitektura

```
J.A.R.V.I.S. on Android
├── presentation/              # Jetpack Compose UI
│   ├── components/            # Arc Reactor Orb, HUD Card, Timeline, Quick Actions
│   ├── screens/               # Əsas ekran, Settings Sheet
│   └── JarvisViewModel.kt     # UI vəziyyət idarəetməsi
│
├── ai/                        # Zəka və NLP qatı
│   ├── matcher/               # DeterministicIntentMatcher (60+ intent regex)
│   ├── normalizer/            # AzerbaijaniTextNormalizer
│   └── provider/              # GeminiProvider, LocalSLMProvider, FallbackProvider
│
├── services/                  # Android Sistem Xidmətləri
│   ├── JarvisAccessibilityService.kt          # UI klik, scroll, ekran oxuma
│   └── JarvisNotificationListenerService.kt   # Real-time bildiriş axını
│
├── tools/                     # 60+ Cihaz Aləti
│   ├── Tool.kt                # Baza interfeys
│   ├── ToolRegistry.kt        # Bütün alətlərin qeydiyyat mərkəzi
│   ├── CapabilityDetector.kt  # Pre-flight bacarıq və icazə detektoru
│   └── impl/                  # 17 kateqoriya üzrə paketlər:
│       ├── system/
│       ├── apps/
│       ├── files/
│       ├── battery/
│       ├── performance/
│       ├── network/
│       ├── audio/
│       ├── notifications/
│       ├── camera/
│       ├── contacts/
│       ├── call/
│       ├── sms/
│       ├── location/
│       ├── voice/
│       ├── alarm/
│       ├── reminder/
│       ├── calendar/
│       ├── browser/
│       └── accessibility/
│
├── automation/                # CommandPipeline (10 mərhələli icra boru kəməri)
├── security/                  # RiskManager & CommandSanitizer
├── permissions/               # PermissionManager (deklarativ icazə idarəsi)
├── memory/                    # MemoryManager (Room DB əsaslı kontekst)
└── voice/                     # TextToSpeech & SpeechRecognizer köməkçiləri
```

---

## 🚀 Quraşdırma

### 1. Layihəni klonlayın

```bash
git clone https://github.com/n4dlr/J.A.R.V.I.S.on.Android.git
cd J.A.R.V.I.S.on.Android
```

### 2. API açarını konfiqurasiya edin

```bash
cp .env.example .env
```

`.env` faylını açın və Gemini API açarınızı daxil edin:

```env
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

### 3. Android Studio-da açın və işə salın

```
Android Studio → Open → J.A.R.V.I.S.on.Android → Sync Project with Gradle Files → Run
```

---

## 🎮 Nümunə Əmrlər

| Əmr | Kateqoriya | İcra |
|-----|------------|------|
| `"Batareyanın temperaturu nə qədərdir?"` | BATTERY | Batareya dərəcəsini göstərir |
| `"Telefon şarj olurmu?"` | BATTERY | Şarj mənbəyi və vəziyyətini bildirir |
| `"CPU vəziyyətinə bax"` | PERFORMANCE | Prosessor istifadəsini və çekirdəkləri bildirir |
| `"Musiqini saxla"` | AUDIO | Aktiv media oynatıcını dayandırır |
| `"Səsi kəs"` | AUDIO | Cihazı səssiz rejimə keçirir |
| `"Saata 8 üçün zəngli saat qur"` | ALARM | Zəngli saat yaradır (Təsdiq ilə) |
| `"Google-da axtar süni intellekt"` | BROWSER | Google axtarışını açır |
| `"Ekrandakı mətni oxu"` | ACCESSIBILITY | Görünən mətni oxuyur və göstərir |
| `"Son zəngləri göstər"` | CALL | Zəng tarixçəsini açır |
| `"Fayl axtar foto"` | FILES | MediaStore vasitəsilə faylları tapır |

---

## 📄 Lisenziya

Bu layihə [MIT Lisenziyası](LICENSE) altında yayılır.

---

<div align="center">

**⭐ Bəyəndinizsə, repoya ulduz (Star) vurmağı unutmayın!**

[GitHub Reposu](https://github.com/n4dlr/J.A.R.V.I.S.on.Android) · Nadir tərəfindən ❤️ ilə hazırlanmışdır

</div>

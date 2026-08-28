<div align="center">

<img src="https://img.shields.io/badge/Android-9%2B%20(API%2028%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 9+"/>
<img src="https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
<img src="https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
<img src="https://img.shields.io/badge/Gemini%20AI-Firebase-FF6F00?style=for-the-badge&logo=google&logoColor=white" alt="Gemini AI"/>
<img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License"/>

<br/><br/>

```
      ██╗ █████╗ ██████╗ ██╗   ██╗██╗███████╗
      ██║██╔══██╗██╔══██╗██║   ██║██║██╔════╝
      ██║███████║██████╔╝██║   ██║██║███████╗
 ██   ██║██╔══██║██╔══██╗╚██╗ ██╔╝██║╚════██║
 ╚█████╔╝██║  ██║██║  ██║ ╚████╔╝ ██║███████║
  ╚════╝ ╚═╝  ╚═╝╚═╝  ╚═╝  ╚═══╝  ╚═╝╚══════╝
         on Android
```

### _Just A Rather Very Intelligent System — Android üçün_

**AI-dəstəkli, offline-first, Azərbaycan dilini tam dəstəkləyən şəxsi asistan**

[🚀 Tez başlayın](#-quraşdırma) · [✨ Özəlliklər](#-özəlliklər) · [🏗️ Arxitektura](#️-arxitektura) · [🤝 Töhfə](#-töhfə)

</div>

---

## 📖 Layihə haqqında

**J.A.R.V.I.S. on Android** — Android cihazlar üçün hazırlanmış, AI-dəstəkli, rootsuz şəxsi asistan tətbiqidir. Marvel-dən ilhamlanaraq yaradılmış bu layihə, real zamanlı səs tanıma, çoxdilli dəstək (Azərbaycan, ingilis, rus) və güclü cihaz avtomatlaşdırması imkanları təqdim edir.

Tətbiq **offline-first** prinsipinə əsaslanır — əsas funksiyalar internet bağlantısı olmadan da işləyir. Gemini AI isə əlavə bir zəka qatı kimi xidmət edir.

> 💡 **"Salam JARVIS, batareyanı yoxla"** — bir cümlə ilə cihazınızı idarə edin.

---

## ✨ Özəlliklər

### 🧠 AI & Dil İşləmə
| Özəllik | Təsvir |
|---------|--------|
| **Çox-dilli Dəstək** | Azərbaycan, ingilis, rus dilləri |
| **Azərbaycan NLP** | `AzerbaijaniTextNormalizer` — xüsusi Azərbaycan mətni normallaşdırması |
| **Qərarlı Intent Tanıma** | `DeterministicIntentMatcher` — regex + qayda əsaslı, sürətli |
| **Gemini AI Entegrasiyası** | Firebase AI vasitəsilə Gemini 1.5/2.0 dəstəyi |
| **Fallback Zənciri** | Local SLM → Gemini → Fallback provider |

### 🛠️ Cihaz Alətləri (Tools)
| Alət | Özəllik |
|------|---------|
| `GetBatteryTool` | Batarya səviyyəsi və şarj vəziyyəti |
| `GetRamTool` | RAM istifadəsi monitorinqi |
| `GetStorageTool` | Daxili yaddaş analizi |
| `SetVolumeTool` | Sistem səs səviyyəsi idarəetməsi |
| `TorchTool` | El fənəri açma/bağlama |
| `OpenAppTool` | Tətbiq işə salma |
| `OpenSettingsTool` | Android parametrlərinə giriş |
| `TakePhotoTool` | Kamera ilə foto çəkmə |
| `CreateReminderTool` | Alarm/xatırlatma yaratma |
| `LockScreenTool` | Ekranı kilid etmə |
| `ReadNotificationsTool` | Bildirişləri oxuma |

### 🎙️ Səs & Prezentasiya
- **Gerçək Zamanlı Səs Tanıma** — `VoiceRecognizerHelper` ilə
- **Mətndən Səsə** — `TextToSpeechHelper` ilə JARVIS tonu
- **Arc Reaktoru UI** — animasiyalı, Jetpack Compose ilə hazırlanmış orb
- **Telemetry HUD** — cihaz statistikasını canlı göstərir
- **Söhbət Xətti** — vizual və interaktiv danışıq lenti

### 🔒 Təhlükəsizlik & Yaddaş
- **Risk Manager** — potensial təhlükəli əmrlər üçün icazə dialoqu
- **Memory Manager** — Room DB əsaslı kontekst yaddaşı
- **Low-RAM Rejim** — `LowRamManager` — yaddaşı məhdud cihazlar üçün adaptiv davranış
- **App Check** — Firebase reCAPTCHA + Debug token dəstəyi

---

## 🏗️ Arxitektura

```
J.A.R.V.I.S. on Android
├── presentation/          # Jetpack Compose UI
│   ├── components/        # Arc Reaktoru, HUD, Söhbət lenti, Quick Actions
│   ├── screens/           # Əsas ekran, Parametrlər
│   ├── JarvisViewModel.kt # UI vəziyyəti idarəetməsi
│   └── JarvisUiState.kt   # UI state modeli
│
├── ai/                    # AI qatı
│   ├── provider/          # AIProvider interfeysi + Gemini, Local, Fallback
│   ├── matcher/           # DeterministicIntentMatcher
│   └── normalizer/        # AzerbaijaniTextNormalizer
│
├── tools/                 # Cihaz alətləri
│   ├── Tool.kt            # Alət interfeysi
│   ├── ToolRegistry.kt    # Alətlərin qeydiyyatı
│   └── impl/              # 11 konkret alət implementasiyası
│
├── automation/            # CommandPipeline — əmr emalı
├── domain/                # Repository interfeysi + domain modellər
├── data/                  # Room DB, DAO-lar, entity-lər
├── memory/                # MemoryManager — kontekst saxlama
├── voice/                 # TTS + STT yardımçıları
├── security/              # RiskManager
└── permissions/           # PermissionManager
```

**Texnoloji Yığın:**

```
Jetpack Compose • Kotlin Coroutines • Firebase AI (Gemini) •
Room Database • Retrofit + OkHttp + Moshi • Firebase App Check •
Android Speech Recognition • TextToSpeech API
```

---

## 📋 Tələblər

| Tələb | Minimum |
|-------|---------|
| Android versiyası | 9.0 (API 28) |
| Target SDK | 36 |
| RAM | 2 GB (tövsiyə: 4 GB) |
| İnternet | Gemini üçün tələb olunur (offline funksiyalar üçün deyil) |

---

## 🚀 Quraşdırma

### 1. Layihəni klonlayın

```bash
git clone https://github.com/n4dlr/J.A.R.V.I.S.on.Android.git
cd J.A.R.V.I.S.on.Android
```

### 2. API açarını konfiqurasiya edin

`.env.example` faylını kopyalayaraq `.env` yaradın:

```bash
cp .env.example .env
```

`.env` faylını redaktə edin:

```env
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

> 🔑 Gemini API açarını [Google AI Studio](https://aistudio.google.com/app/apikey) səhifəsindən əldə edin.

### 3. Firebase konfiqurasiyası (İstəyə bağlı)

Firebase xüsusiyyətlərini aktivləşdirmək üçün:
1. [Firebase Console](https://console.firebase.google.com/)-da yeni layihə yaradın
2. Android tətbiqini qeydiyyatdan keçirin: `com.aistudio.jarvis.azassistant`
3. `google-services.json` faylını `app/` qovluğuna yerləşdirin

### 4. Android Studio-da açın və işə salın

```
Android Studio → Open → J.A.R.V.I.S.on.Android → Sync → Run
```

---

## 🎮 İstifadə

JARVIS işə salındıqdan sonra:

| Əmr | Nəticə |
|-----|--------|
| `"Batareyanı yoxla"` | Batarya faizini bildirir |
| `"RAM-a bax"` | Yaddaş istifadəsini göstərir |
| `"Fənəri yandır"` | El fənərini açır |
| `"Musiqini aç"` | Musiqi tətbiqini işə salır |
| `"Həyəcan siqnalı qur sabah 7-də"` | Alarm yaradır |
| `"Bildirişlərimi oxu"` | Son bildirişləri səsləndiririr |
| `"Parametrlərə get"` | Android parametrlərini açır |

---

## 🗺️ Yol Xəritəsi

- [ ] Azərbaycan STT modeli inteqrasiyası (offline)
- [ ] Widget dəstəyi (Ana ekran widgeti)
- [ ] Çox-agent rejimi (paralel tapşırıq icrası)
- [ ] Aksesibillik servisi (ekransız əmr icrası)
- [ ] WhatsApp / Telegram inteqrasiyası
- [ ] Şifrəli yaddaş (encrypted memory store)

---

## 🤝 Töhfə

Töhfə verməyə xoş gəlmisiniz! Bir Pull Request açın:

```bash
git checkout -b feature/yeni-ozollik
git commit -m "feat: yeni özəllik əlavə edildi"
git push origin feature/yeni-ozollik
```

---

## 📄 Lisenziya

Bu layihə [MIT Lisenziyası](LICENSE) altında yayılır.

---

<div align="center">

**⭐ Bəyəndinizsə, ulduz vurmağı unutmayın!**

[GitHub-da aç](https://github.com/n4dlr/J.A.R.V.I.S.on.Android) · Nadir tərəfindən ❤️ ilə hazırlanmışdır

</div>

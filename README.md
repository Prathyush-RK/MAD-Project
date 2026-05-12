<p align="center">
  <img src="assets/banner.png" alt="SkillForge Banner" width="100%" />
</p>

<p align="center">
  <strong>AI-Powered Skill Marketplace for Android</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#project-structure">Project Structure</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#license">License</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Min%20SDK-30-blue" />
  <img src="https://img.shields.io/badge/AI-Groq%20%2F%20Gemini-FF6F00?logo=google&logoColor=white" />
  <img src="https://img.shields.io/badge/Backend-Firebase-FFCA28?logo=firebase&logoColor=black" />
</p>

---

## 📋 Overview

**SkillForge** is a native Android application that serves as a curated marketplace for discovering, creating, and sharing AI skills. Instead of manually hunting across GitHub repositories, blog posts, and documentation for prompt engineering recipes, SkillForge centralises the entire workflow into a single mobile experience.

The platform delivers three interconnected capabilities:

- **🔍 Discover Marketplace** — Browse and install community-published AI skills with real-time search and category filtering.
- **🤖 AI Skill Builder** — Create custom skills through a guided conversational AI flow or manual editor.
- **📦 Distribution Pipeline** — Publish skills to the marketplace or save drafts locally with AI-powered quality validation.

---

## ✨ Features

### Discover Marketplace
- Real-time search with debounced filtering across skill titles, descriptions, and tags
- Horizontally scrollable category chips: **All, Writing, Code, Research, Design, Security** and more
- Rich skill cards with ratings, install counts, and one-tap install
- Full skill detail view with native Markdown rendering via **Markwon**
- Shimmer loading placeholders for premium loading experience

### AI-Powered Skill Builder
- **Conversational Builder** — Chat with AI that asks structured, sequential questions following a dependency chain: Domain → Task → Tone → Constraints → Examples → Edge Cases
- **Manual Editor** — Full-form editor for users who prefer direct control
- **Template System** — Start from pre-built templates (Research, Design, Security) or build from scratch
- Automatic `[READY_TO_GENERATE]` sentinel detection triggers SKILL.md compilation

### AI Validation & Quality Control
- AI-powered skill scoring (0–100) with safety and security analysis
- Automated quality gating — skills scoring **≥ 70** are auto-published; below goes to drafts
- Security scanning for prompt injection, malicious commands, and credential harvesting patterns

### Drafts & My Skills
- Save work-in-progress skills as drafts with full edit/resume capability
- Personal dashboard with installed skills, built count, and activity tracking
- Status indicators: 🟢 Active, 🟡 Draft, ⚫ Inactive

### Profile & Gamification
- Builder level badges based on community contributions
- Stats tracking: skills installed, built, and shared usage metrics
- Firebase Authentication with Google sign-in

---

## 🏗️ Architecture

The app follows **Clean Architecture** with the **MVVM** pattern, ensuring clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION                          │
│  Fragments ─── ViewModels ─── UI State                  │
│  (XML Layouts + ViewBinding)                            │
├─────────────────────────────────────────────────────────┤
│                      DOMAIN                              │
│  Repository Interfaces ─── Data Models                   │
├─────────────────────────────────────────────────────────┤
│                       DATA                               │
│  SkillRepository ─── GeminiService ─── Room DB          │
│  (Firebase Firestore)  (Groq/Gemini API)  (Local Cache) │
└─────────────────────────────────────────────────────────┘
```

| Layer | Responsibility |
|-------|---------------|
| **Presentation** | Fragments, ViewModels, Adapters — renders UI from state and dispatches user intent |
| **Domain** | Repository interfaces, data models — pure Kotlin, no Android framework dependencies |
| **Data** | Repository implementations, AI services, Room DAOs, Firebase integration |

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|-----------|
| **Language** | Kotlin |
| **UI** | XML Layouts + ViewBinding |
| **Navigation** | Jetpack Navigation Component |
| **State Management** | ViewModel + LiveData + Coroutines |
| **Dependency Injection** | Hilt (Dagger) |
| **Local Database** | Room |
| **Backend** | Firebase (Firestore, Storage, Auth, Analytics) |
| **AI Service** | Groq API (OpenAI-compatible) / Gemini API |
| **Networking** | OkHttp + Gson |
| **Markdown Rendering** | Markwon (core, strikethrough, tables, tasklist) |
| **Image Loading** | Coil |
| **UI Effects** | Shimmer (Facebook) |
| **Build System** | Gradle (KTS) with Version Catalogs |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2+) or newer
- **JDK 11+**
- **Android SDK** with API Level 36
- A **Firebase project** with Firestore, Storage, and Authentication enabled
- A **Groq API key** (or Gemini API key)

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/Prathyush-RK/MAD-Project.git
   cd MAD-Project
   ```

2. **Add Firebase configuration**
   
   Place your `google-services.json` in the `app/` directory.  
   You can download this from the [Firebase Console](https://console.firebase.google.com/).

3. **Configure API keys**
   
   Create or edit `local.properties` in the project root:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   GROQ_API_KEY=your_groq_api_key_here
   ```

4. **Sync & Run**
   
   Open in Android Studio → Sync Gradle → Run on device/emulator (API 30+).

---

## 📂 Project Structure

```
app/src/main/
├── java/com/example/skills/
│   ├── MainActivity.kt              # Main entry point with bottom navigation
│   ├── SkillForgeApp.kt             # Hilt application class
│   │
│   ├── data/
│   │   ├── local/
│   │   │   ├── AppDatabase.kt       # Room database definition
│   │   │   └── SkillDao.kt          # Data access object for local skills
│   │   ├── model/
│   │   │   ├── Skill.kt             # Marketplace skill model
│   │   │   ├── SkillDraft.kt        # Draft skill model
│   │   │   ├── SkillTemplate.kt     # Builder template model
│   │   │   ├── InstalledSkill.kt    # Room entity for installed skills
│   │   │   └── Category.kt          # Category filter model
│   │   ├── remote/
│   │   │   └── GeminiService.kt     # AI service (Groq/Gemini API client)
│   │   └── repository/
│   │       └── SkillRepository.kt   # Central data repository
│   │
│   ├── di/
│   │   └── AppModule.kt             # Hilt dependency injection module
│   │
│   └── ui/
│       ├── adapters/                 # RecyclerView adapters
│       │   ├── SkillAdapter.kt       # Marketplace skill cards
│       │   ├── ActiveSkillAdapter.kt # Installed skills list
│       │   ├── CategoryAdapter.kt    # Category filter chips
│       │   ├── ChatAdapter.kt        # Chat builder messages
│       │   ├── DraftAdapter.kt       # Draft skills list
│       │   └── TemplateAdapter.kt    # Builder template cards
│       ├── fragments/                # UI screens
│       │   ├── DiscoverFragment.kt   # Marketplace browsing
│       │   ├── SkillDetailFragment.kt# Skill detail & validation
│       │   ├── BuildFragment.kt      # Skill creation hub
│       │   ├── CreateSkillFragment.kt# Manual skill editor
│       │   ├── ChatBuilderFragment.kt# Conversational AI builder
│       │   ├── MySkillsFragment.kt   # Personal skills dashboard
│       │   └── ProfileFragment.kt    # User profile & settings
│       └── viewmodels/               # Business logic
│           ├── DiscoverViewModel.kt
│           ├── BuildViewModel.kt
│           ├── CreateSkillViewModel.kt
│           ├── ChatBuilderViewModel.kt
│           └── MySkillsViewModel.kt
│
└── res/
    ├── layout/                       # XML layouts (15 files)
    ├── drawable/                     # Custom shapes & backgrounds
    ├── navigation/                   # Navigation graph
    ├── menu/                         # Bottom navigation menu
    ├── anim/                         # List animations
    └── values/                       # Colors, themes, strings
```

---

## 🎨 Design System

SkillForge uses a strict **dark mode** design language with a curated color palette:

| Token | Hex | Usage |
|-------|-----|-------|
| `bg_dark` | `#0F0F12` | Primary background |
| `surface_dark` | `#1A1A1F` | Card surfaces |
| `surface_variant` | `#24242B` | Elevated surfaces |
| `brand_primary` | `#6366F1` | Primary accent (Indigo) |
| `brand_secondary` | `#A78BFA` | Secondary accent (Violet) |
| `brand_accent` | `#F472B6` | Highlight accent (Pink) |
| `text_primary` | `#F8FAFC` | Headings & body text |
| `text_secondary` | `#94A3B8` | Subtitles & metadata |
| `status_active` | `#10B981` | Active/success state |
| `status_draft` | `#F59E0B` | Draft/warning state |

---

## 📱 Screenshots

> 🚧 *Coming soon — screenshots of the Discover, Build, and My Skills screens.*

<!--
<p align="center">
  <img src="assets/screenshot_discover.png" width="30%" />
  <img src="assets/screenshot_build.png" width="30%" />
  <img src="assets/screenshot_myskills.png" width="30%" />
</p>
-->

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Built with ❤️ by <a href="https://github.com/Prathyush-RK">Prathyush R</a>
</p>

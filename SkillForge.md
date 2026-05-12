
PRODUCT REQUIREMENTS DOCUMENT
SkillForge
AI Skill Marketplace for Android

| Platform | Native Android (Kotlin + Jetpack Compose) |
| --- | --- |
| Version | v1.0 — Initial Release |
| Status | READY FOR DEVELOPMENT |
| Date | April 2026 |
| Architecture | Firebase + LLM API (Claude/Gemini) |


Confidential — Internal Use Only


# Table of Contents

# 1. Executive Summary
SkillForge is a native Android application that solves a critical gap in the AI tooling ecosystem: the absence of a curated, accessible marketplace for discoverable, composable AI skills. Today, practitioners manually hunt across GitHub repositories, blog posts, and documentation to assemble the prompt engineering recipes that power their workflows. SkillForge eliminates this friction entirely.

The platform delivers three interconnected capabilities in a single mobile experience: a read-only Discover Marketplace for browsing community-verified skills, an AI-powered Skill Builder for creating custom skills through guided conversation, and a Distribution Pipeline for publishing or saving skills privately. A gamified profile system incentivises open-source contribution and community engagement.

Strategic Objectives
| # | Objective | Success Metric |
| --- | --- | --- |
| 01 | Centralise AI skill discovery | 500+ skills indexed in marketplace at launch |
| 02 | Reduce skill creation time | User generates a publishable SKILL.md in under 5 minutes |
| 03 | Drive open-source contribution | 30% of users publish at least one skill within 90 days |
| 04 | Ensure quality & safety | Zero malicious skills reach public listing (100% sanitisation pass rate) |
| 05 | Developer adoption | 10,000 installs within 6 months of launch |

# 2. Product Overview
## 2.1 Core Value Proposition
SkillForge is a decentralised, open-source mobile platform for discovering, validating, and dynamically generating AI workflows (Claude Skills). It bridges the gap between finding a generic tool and engineering a hyper-personalised automation asset.

The application targets three distinct user archetypes: the Developer who needs ready-made skills to accelerate projects, the Power User who wants to customise AI behaviour for niche tasks, and the Builder who wants to create, share, and receive recognition for publishing quality skills to the community.

## 2.2 User Personas
| Persona | The Developer | The Power User | The Builder |
| --- | --- | --- | --- |
| Goal | Download and apply pre-built skills instantly | Customise and validate skills for specific tasks | Create, publish, and maintain original skills |
| Pain Point | Fragmented discovery across repos and forums | Installing wrong skill, wasting time reconfiguring | No distribution channel; skills buried in GitHub |
| Key Feature | Discover Marketplace + one-tap install | AI Skill Validator with compatibility scoring | Skill Builder + Distribution Pipeline |

# 3. Feature Specifications
## 3.1 Discover Marketplace
Priority: P0 — Must Have

The Discover screen serves as the application's primary entry point — a read-only, centralised hub for browsing and installing community-published AI skills. The architecture deliberately separates browsing from building to prevent cognitive overload.

### 3.1.1 Search & Filtering
A persistent global search bar at the top of the screen queries skill titles, descriptions, and tags simultaneously
Horizontally scrollable pill-filter chips below the search bar for category filtering: All, Writing, Code, Research, Finance, Design, DevOps, Legal, Healthcare
Filters are mutually exclusive single-select; active state renders chip in deep purple (#6B46C1) with white text
Search and filter states are composable — a search query within a filtered category narrows results accordingly
Results update in real-time (debounced at 300ms) without requiring an explicit search submission

### 3.1.2 Skill Cards
Each card displays: skill icon (emoji or custom), title, one-line description, star rating, install count, and an Install / Installed CTA button
Install count formatted with locale-aware abbreviation (e.g., 12,400 → 12.4k)
Installed state renders as a muted grey chip to provide clear feedback and prevent duplicate installs
Cards are tappable; tap anywhere except the CTA button navigates to the Skill Detail View

### 3.1.3 Skill Detail View
Full-screen bottom sheet (modal) that renders the complete SKILL.md content natively using the Markwon library
Native markdown rendering must support: headings (H1–H3), code blocks with syntax highlighting, bold/italic, unordered and ordered lists, inline code, and horizontal rules
A WebView must not be used at any point — all rendering is in Compose Text composables
Detail view sections: Description, Author & Version, Install Stats, Compatibility Validator (see 3.2), and Reviews
Version history tab shows a chronological list of published versions with change summaries

### 3.1.4 Acceptance Criteria
| ID | Criteria | Priority | Status |
| --- | --- | --- | --- |
| AC-01 | Search returns results within 500ms on a standard 4G connection | P0 | OPEN |
| AC-02 | Category filter applies without full page reload | P0 | OPEN |
| AC-03 | SKILL.md file renders all markdown elements natively without WebView | P0 | OPEN |
| AC-04 | Install action writes skill to local storage and updates button state atomically | P0 | OPEN |
| AC-05 | Skills list is paginated (20 per page) with lazy loading on scroll | P1 | OPEN |
| AC-06 | Offline mode shows cached skills with a network status indicator | P2 | OPEN |


## 3.2 AI Skill Validator
Priority: P0 — Must Have

The Validator is an intelligent gate that prevents users from installing mismatched tools. Before committing a download, users can describe their specific task and receive an AI-powered compatibility assessment. This is the primary differentiator from a generic file repository.

### 3.2.1 Validation Flow
User taps 'Check Compatibility' within the Skill Detail View
A bottom sheet expands exposing a multi-line 'Describe your task' text field
User submits task description (minimum 10 characters enforced with inline validation)
App assembles a structured prompt containing: the full SKILL.md content, the user's task description, and a scoring rubric
Request is dispatched to the LLM proxy endpoint via Retrofit2
Response is streamed back and rendered incrementally — score first, reasoning second
A compatibility score (0–100) is displayed with a colour-coded indicator: 0–40 red (not recommended), 41–70 amber (partial match), 71–100 green (strong match)
Actionable suggestions are listed below the score when the match is partial or poor

### 3.2.2 Prompt Contract (LLM API)
The following system prompt governs validator behaviour and must not be editable by end users:

| SYSTEM PROMPT: Skill Validator You are a skill compatibility analyst. Given a SKILL.md definition and a user task description, evaluate compatibility on a scale of 0-100. Respond ONLY in the following JSON schema: { "score": integer, "verdict": "STRONG_MATCH|PARTIAL_MATCH|NOT_RECOMMENDED",   "rationale": "string (2-3 sentences)", "suggestions": ["string"] } Do not include any text outside the JSON object. |
| --- |


## 3.3 Conversational Skill Builder
Priority: P0 — Must Have

The Builder is a dedicated workspace that removes blank-canvas anxiety from skill creation. A conversational AI acts as a structured requirements-gatherer, collecting information through a sequential question-and-answer flow before autonomously generating a complete, production-ready SKILL.md file.

### 3.3.1 Entry Points
'Start from scratch' — blank context, AI begins with foundational questions about domain, intent, and audience
'Use a template' — user selects a base template (Research Assistant, Task Planner, Customer Support, Code Reviewer, Legal Drafter). AI begins with template context pre-loaded and asks refinement questions only

### 3.3.2 Conversation Rules (Enforced via System Prompt)
The AI asks exactly one clarifying question per message — never a list of questions
Questions follow a logical dependency chain: Domain → Task → Tone → Constraints → Examples → Edge Cases
The AI must refuse off-topic requests gracefully: 'I'm currently in skill-building mode. Let me continue gathering requirements — [next question]'
Session is ephemeral: no conversation history is persisted to the database; state lives in ViewModel memory only
The AI signals completion by outputting the sentinel token [READY_TO_GENERATE] as the last token of its response when it has sufficient context (typically 6–10 questions answered)

### 3.3.3 Generation Trigger & Compilation
The Android client intercepts the [READY_TO_GENERATE] token via streaming response parsing
On detection: the chat interface fades out, a generation progress indicator appears, and the app dispatches a second LLM call — this time a 'generation prompt' using the full conversation as structured context
The generation prompt instructs the LLM to output a fully formed SKILL.md with: name, description, instructions, examples, constraints, and tool specifications
The generated markdown is displayed in a read-only preview panel using Markwon before the user can accept or reject it
Reject returns the user to the chat for further refinement; Accept progresses to the Distribution Pipeline (Section 3.4)

### 3.3.4 Builder State Machine
| State | Trigger | UI Behaviour |
| --- | --- | --- |
| IDLE | User taps Build tab | Template selector + scratch CTA visible |
| GATHERING | Template selected or scratch started | Chat interface active; AI asks questions sequentially |
| SENTINEL_DETECTED | AI outputs [READY_TO_GENERATE] | Chat hides; progress spinner shown |
| GENERATING | Generation LLM call in-flight | Streaming markdown preview begins rendering |
| REVIEW | Generation complete | Full SKILL.md preview; Accept / Reject buttons |
| DISTRIBUTING | User taps Accept | Distribution Pipeline initiated (Section 3.4) |
| ERROR | Any API failure | Retry button + error detail; chat history preserved |


## 3.4 Distribution Pipeline
Priority: P0 — Must Have

Post-generation, users have two distribution paths: public marketplace publishing or private local storage. The pipeline is designed to be frictionless for local saves and structured (but not burdensome) for public publishing.

### 3.4.1 Publish to Marketplace
User taps 'Publish' on the post-generation review screen
A multi-step bottom sheet form collects: Title (auto-populated from generated skill name, editable), Short Description (max 160 chars), Category (single-select from defined taxonomy), and optional Tags (free-form, max 5)
Backend Cloud Function receives the submission, runs the sanitisation pipeline (Section 5.2), and returns a sanitisation result within 10 seconds
If sanitisation passes: skill document is written to Firestore, .md file uploaded to Firebase Storage, author's stats.built counter incremented
If sanitisation fails: specific violation is shown to the user with a suggestion to remove the flagged content

### 3.4.2 Save Locally
User taps 'Save to Device' — no form required
The SKILL.md string is written to the Android MediaStore Documents directory using the Storage Access Framework (SAF) for compatibility with Android 10+
File is named using the pattern: skillforge_[skill-title-slug]_[timestamp].md
A Snackbar confirmation shows the file path with an 'Open' action to navigate directly to the file in the system Files app via Intent

## 3.5 My Skills Dashboard & Gamification
Priority: P1 — Should Have

The My Skills tab serves as the user's personal command centre — tracking their installed library, created skills, and community impact. Gamification mechanics are deliberately lightweight, focused on contribution metrics rather than points or leaderboards.

### 3.5.1 Metrics Display
| Metric | Definition | Data Source |
| --- | --- | --- |
| Installed | Total unique skills downloaded to device (lifetime) | Local Room DB |
| Built | Total skills created via the Builder (includes drafts) | Firestore users.stats.built |
| Shared Uses | Cumulative downloads of all skills published by the user | Firestore users.stats.shared_uses |


### 3.5.2 Active Roster
Lists installed skills with quick-action icons: Edit (navigates to Builder with skill pre-loaded), Uninstall (removes local file + updates Room DB), and Re-publish (opens Distribution Pipeline for updated version)
Activity indicator dot: green (used in last 7 days), amber (used 8–30 days ago), grey (unused 30+ days)
Builder Level badge on the profile card: levels 1–10 based on logarithmic scale of shared_uses count
# 4. UI & Design System
## 4.1 Visual Language
SkillForge's design language communicates technical competence and trust while remaining approachable to both casual users and power developers. Every decision prioritises clarity and speed over decoration.

| Property | Spec | Rationale |
| --- | --- | --- |
| Theme | Strict dark mode (no light mode toggle) | Aligns with developer environment conventions; reduces eye strain during extended use |
| Primary Accent | #6B46C1 — Deep Purple | Communicates intelligence and creativity; distinguishes from generic blue SaaS products |
| Surface | #111827 / #1F2937 | Two-level dark surface hierarchy provides depth without heavy shadows |
| Typography | Inter — Regular 14sp body, Medium 16sp subtitle, Bold 20sp+ | System-legible with strong character differentiation for code-heavy content |
| Corner Radius | 12dp for cards, 20dp for chips, 24dp for modals | Progressive rounding: more rounded = more interactive |
| Elevation | No shadows — use subtle border (1dp, #374151) | Flat design reduces visual noise on dark backgrounds |
| Motion | 300ms ease-in-out transitions, spring physics for sheets | Responsive without being frivolous |


## 4.2 Navigation Architecture
The app uses a standard Android Bottom Navigation Bar with four primary destinations. All primary screens are top-level — no nested navigation stacks except for the Skill Detail View (modal) and Builder flow (full-screen).

| Tab | Icon | Route | Description |
| --- | --- | --- | --- |
| Discover | Grid (4 squares) | / discover | Main marketplace feed |
| Build | Plus circle | / build | Skill creation workspace |
| My Skills | Checkmark circle | / my-skills | Personal library & stats |
| Profile | User circle | / profile | Account, settings, auth |


## 4.3 Component Library
### SkillCard
Composable: SkillCard(skill: SkillModel, onInstall: () -> Unit, onTap: () -> Unit)
Dimensions: full-width card, 72dp height minimum, 12dp internal padding
Leading element: 44dp icon container with 10dp radius background tinted from category colour
Trailing element: Install/Installed chip; width fixed at 72dp to prevent layout shift
Description text: single line, ellipsized at end — never wraps

### ChatBubble
Two variants: UserBubble (right-aligned, purple #4C1D95 background) and AIBubble (left-aligned, surface #1F2937 background)
Supports streaming: text renders character by character via Flow<String> from ViewModel
AI bubble includes a pulsing dot indicator during streaming state
Maximum width: 80% of screen width for both variants

### MarkdownViewer
Built on Markwon 4.x with plugins: SyntaxHighlight (Prism.js colours), StrikeThrough, Tables, Tasklist
Code blocks: monospace font, #1E1B4B background, with a copy-to-clipboard IconButton in the top-right corner
Horizontal rule renders as a 1dp purple-tinted divider line
# 5. Technical Architecture
## 5.1 Technology Stack
| Layer | Technology | Version | Purpose |
| --- | --- | --- | --- |
| Language | Kotlin | 2.0+ | Primary development language |
| UI Framework | Jetpack Compose | 1.7+ | Declarative, reactive UI components |
| Navigation | Navigation Compose | 2.8+ | Type-safe screen routing |
| State Mgmt | ViewModel + StateFlow | Lifecycle 2.8+ | Reactive state; survives config changes |
| Async | Kotlin Coroutines | 1.8+ | Background threads; streaming support |
| Networking | Retrofit2 + OkHttp | 2.11 / 4.12 | HTTP client; streaming via SSE |
| Serialisation | Kotlin Serialisation | 1.7+ | JSON parsing for API responses |
| Markdown | Markwon | 4.6.2 | Native markdown rendering in Compose |
| Local DB | Room | 2.6+ | Local skills library persistence |
| Auth | Firebase Auth | 23+ | Google OAuth + GitHub sign-in |
| Database | Firebase Firestore | 25+ | Marketplace metadata & user stats |
| Storage | Firebase Storage | 21+ | SKILL.md file hosting |
| DI | Hilt | 2.52+ | Dependency injection |
| Image Loading | Coil | 3.x | Async image loading for skill icons |
| Testing | JUnit5 + Turbine | Latest | Unit + Flow testing |


## 5.2 Architecture Pattern — Clean Architecture + MVVM
The application strictly follows Clean Architecture with three layers. UI concerns are completely isolated from business logic; business logic has zero dependency on Android framework classes; data sources are interchangeable.

| Layer | Components | Responsibilities |
| --- | --- | --- |
| Presentation | Composables, ViewModels, UI State classes | Render UI from state; dispatch user intent to ViewModel |
| Domain | Use Cases, Repository interfaces, Domain Models | Encapsulate business rules; pure Kotlin; no Android deps |
| Data | Repository implementations, Firestore DAOs, Room DAOs, API services | Fetch, cache, and transform raw data into domain models |


## 5.3 Data Models (Firestore Schema)
### Collection: users
| // Collection: users {   "uid":      "user_123",   "username": "shanthiv_dev",   "auth_provider": "google" | "github",   "level":    4,   "stats": {     "installed":    7,     "built":        3,     "shared_uses":  1200   },   "created_at": "2026-04-15T09:00:00Z" } |
| --- |


### Collection: skills
| // Collection: skills {   "skill_id":   "skill_abc890",   "author_uid": "user_123",   "title":      "React Component Tester",   "description":"Automated QA prompts for UI components.",   "category":   "Code",   "tags":       ["testing", "react", "qa"],   "version":    "1.2.0",   "file_url":   "https://firebasestorage.../skill_abc890.md",   "sanitised":  true,   "metrics": {     "downloads":      1450,     "rating_average": 4.8,     "review_count":   42   },   "created_at":  "2026-04-15T09:00:00Z",   "updated_at":  "2026-04-15T09:00:00Z" } |
| --- |


### Sub-collection: skills/{id}/reviews
| // Sub-collection: skills/{skill_id}/reviews {   "review_id":  "rev_xyz",   "user_uid":   "user_456",   "rating":     5,           // integer 1–5   "comment":    "Saves me hours of boilerplate testing.",   "verified_install": true,  // must have installed to review   "timestamp":  "2026-04-16T14:20:00Z" } |
| --- |


## 5.4 API Design — LLM Integration
### 5.4.1 API Security Architecture
API keys must never be embedded in the Android APK binary. The client application communicates exclusively with a SkillForge proxy server (Firebase Cloud Function). The proxy authenticates the Firebase user token, applies rate limits, and forwards the request to the upstream LLM provider with the server-side API key.

| Component | Implementation |
| --- | --- |
| Client Auth Header | Authorization: Bearer {Firebase ID Token} |
| Proxy Validation | Firebase Admin SDK verifyIdToken() in Cloud Function |
| Rate Limiting | 10 LLM requests per user per minute (Firestore counter) |
| Key Storage | Firebase Secret Manager — never in source code or env vars |
| Request Timeout | 30s for validator; 60s for builder generation |
| Streaming Protocol | Server-Sent Events (SSE) — text/event-stream |


## 5.5 Security & Quality Control
### 5.5.1 Malicious Script Sanitisation Pipeline
All SKILL.md files submitted to the public marketplace are processed through a backend sanitisation Cloud Function before being listed. The pipeline evaluates the file for potentially destructive CLI patterns.

| Check | Pattern / Rule | Action on Match |
| --- | --- | --- |
| Destructive Shell | rm -rf, sudo rm, mkfs, dd if=/dev/zero | REJECT — flag to admin |
| Network Exploit | curl | bash, wget | sh, nc -e /bin/sh | REJECT — flag to admin |
| Credential Harvest | cat /etc/passwd, /etc/shadow, .ssh/ | REJECT — flag to admin |
| Fork Bomb | :(){ :|:& };:, while true; do | REJECT — flag to admin |
| Suspicious URL | IP-address URLs, .onion domains | WARN — manual review queue |
| Prompt Injection | Ignore previous instructions, override system prompt | WARN — manual review queue |


### 5.5.2 Prompt Injection Defense
Builder system prompt is stored server-side only — never transmitted to or editable by the client
User input is transmitted as a structured JSON field, not interpolated directly into the prompt string
The proxy enforces a maximum input length of 2,000 characters for task descriptions
LLM responses are validated against expected JSON schema (Validator) or markdown schema (Builder) before being forwarded to the client — unexpected formats are rejected
# 6. Design Flows
## 6.1 Onboarding Flow
| Step | Screen | Action / Logic |
| --- | --- | --- |
| 1 | Splash Screen | Show SkillForge logo for 1.5s; check Firebase Auth state |
| 2a | Auth Screen (new user) | Present Google OAuth + GitHub OAuth buttons; no email/password sign-up |
| 2b | Discover (returning user) | Auth state valid — skip to main app; token silently refreshed in background |
| 3 | Permissions | Request READ_EXTERNAL_STORAGE (Android ≤12) / READ_MEDIA_DOCUMENTS (13+) for local save feature; graceful degradation if denied |
| 4 | Discover Screen | Fetch initial skills batch (20 items) from Firestore; display trending section |


## 6.2 Core User Flow: Discover → Validate → Install
| # | User Action | System Response | Error Path |
| --- | --- | --- | --- |
| 1 | Browse Discover feed; scroll or filter by category | Paginated results load (20/page); shimmer loading placeholder shown | Offline: show cached data with stale indicator |
| 2 | Tap skill card | Skill Detail bottom sheet slides up; SKILL.md fetched and rendered via Markwon | File fetch fail: retry button shown |
| 3 | Tap 'Check Compatibility' | Task input field expands; keyboard focus set automatically | — |
| 4 | Enter task description and submit | Loading spinner; LLM proxy request dispatched via SSE | Timeout: retry with 'Try again' button |
| 5 | View compatibility result | Score gauge + rationale + suggestions rendered in sequence | LLM error: show generic 'Validation unavailable' message |
| 6 | Tap 'Install' | SKILL.md downloaded from Storage URL; written to Room DB; button state changes to 'Installed'; author shared_uses counter incremented | Write fail: error snackbar; no partial state |


## 6.3 Core User Flow: Build → Generate → Distribute
| # | User Action | System Response | Error Path |
| --- | --- | --- | --- |
| 1 | Tap Build tab; select template or 'from scratch' | Builder screen transitions in; first AI question appears within 2s | API fail: show retry; template context cached locally |
| 2 | Answer AI questions sequentially | Each answer triggers next LLM call; next question appears via streaming | Each call has 15s timeout with retry |
| 3 | [Auto] AI detects sufficient context | Client intercepts [READY_TO_GENERATE]; chat fades out with animation | — |
| 4 | [Auto] Generation LLM call dispatched | Streaming markdown preview renders incrementally in MarkdownViewer | Generation fail: return to chat with context preserved |
| 5 | Review generated SKILL.md preview | Full rendered markdown; user can scroll entire document | — |
| 6a | Tap 'Accept' → 'Publish' | Distribution form sheet opens; sanitisation pipeline runs server-side | Sanitisation fail: specific violation highlighted in red |
| 6b | Tap 'Accept' → 'Save to Device' | SAF file picker may appear (first time); file written to Documents/ | Permission denied: guide to Settings with deep link |
| 6c | Tap 'Reject' | Chat re-opens; last AI message shown with 'What would you like to change?' prompt | — |

# 7. Development Milestones (REVISED: Discovery & Files Focus)
| Phase | Duration | Deliverables | Exit Criteria |
| --- | --- | --- | --- |
| Phase 1 | Weeks 1–2 | Project scaffold, Hilt DI, Firebase setup, Auth flow, Discover UI shell | Auth round-trip works; Discover renders mock data |
| Phase 2 | Weeks 3–4 | Firestore integration, Search & Category logic, Skill Card, Detail View | Real skills fetch from Firestore; categories filter correctly |
| Phase 3 | Weeks 5–6 | Markwon integration, Firebase Storage integration, .md rendering | Skill Detail View renders native markdown from .md files |
| Phase 4 | Weeks 7–8 | Manual Creation Pipeline, .md file generation, local sanitisation | Users can manually build and publish/save skills as .md files |
| Phase 5 | Weeks 9–10 | My Skills dashboard, Room DB persistence, local save (SAF) | Installed skills persist locally in Room and indexed correctly |
| Phase 6 | Weeks 11–12 | Profile screen, shared usage stats, UI/UX polish, Dark mode audit | App feels premium and state-consistent across all tabs |
| Phase 7 | Weeks 13+ | AI Features (Backlog): Validator, Conversational Builder, LLM Proxy | AI-driven features added as modular enhancements |
| Phase 8 | Final | Beta release, Play Store submission | App live on Google Play internal testing track |

# 8. Open Questions & Decisions Required
| # | Question | Options | Owner |
| --- | --- | --- | --- |
| 1 | Which LLM provider for initial launch? | Claude API (Anthropic) vs Gemini API (Google) | Product Owner |
| 2 | Skill versioning strategy — how are breaking changes handled? | Semantic versioning (semver) vs simple integer versions | Tech Lead |
| 3 | Moderation of published skills — human review or automated only? | Automated sanitisation only vs human review queue for WARN cases | Product Owner |
| 4 | Monetisation model for premium skills? | Free only (v1) vs freemium with paid skills in v2 | Business |
| 5 | Minimum Android API level? | API 26 (Android 8.0) vs API 29 (Android 10.0) | Tech Lead |
| 6 | Offline capability scope? | Cached browse only vs full offline skill creation | Tech Lead |



SkillForge PRD v1.0 — April 2026 — Prepared for Development Team
This document is confidential. Do not distribute without authorisation.
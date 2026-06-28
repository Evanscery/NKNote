# NKNote — Multi-platform Scope (Web / PC / Android)

> Status: **scope & tech-stack decision document**. The **seam-abstraction work (todos 2/3) is DONE** in the current Android milestone; the Gradle KMP restructure is the **next milestone** (deferred by design).
> Goal: extend NKNote to Android, Desktop, and Web while preserving UI and feature parity.

## Decision: Kotlin Multiplatform + Compose Multiplatform

The single codebase path that preserves both **UI consistency** and **feature consistency** across Android, Desktop, and Web is **Kotlin Multiplatform (KMP) + Compose Multiplatform**.

Rationale:
- The Android app is *already* Kotlin + Compose. Moving to Compose Multiplatform is the smallest delta.
- The `RichDocument` schema is `kotlinx.serialization`-portable and was designed to be byte-for-byte identical across targets.
- Compose Multiplatform shares ~90% of UI code across Android / Desktop / Web (Wasm).
- Room now supports KMP (multiplatform-room), so the data layer can largely stay.

## Seams already done in this Android milestone (todos 2 / 3)

The current Android milestone intentionally **does not** restructure the Gradle project into a `composeApp` KMP module — that is the next milestone. What it *does* do is land the seam-abstraction work so the KMP restructure later is a *mechanical* move, not a design pass:

- **`model/` is pure Kotlin.** `RichDocument`, `Weather`, `Mood`, `NkPalette` carry **zero** `androidx.compose.*` imports. `Weather` / `Mood` carry only a `key: String`; the icon (`ImageVector`) and localized-label (`labelRes: Int`) maps live in `ui/components/WeatherIconMap.kt` and `MoodIconMap.kt`. `grep -R "ImageVector\|material.icons" app/src/main/java/io/github/nknote/model/` returns nothing.
- **`ImageStore` is an interface.** `data/image/ImageStore.kt` defines the contract (`saveForNote` / `delete` / `deleteAllForNote` / `exists`); `AndroidImageStore` is the Android `actual` (uses platform `BitmapFactory` + `Bitmap.compress(WEBP, 75)`, max 1600 px, `filesDir/images/<noteId>/<sha256>.webp`). A JVM/Desktop `actual` can use `ImageIO` (WebP plugin or TwelveMonkeys); a Web `actual` can use Canvas `toBlob`. The repository and UI depend only on the interface.
- **`NoteRepository` is an interface.** `NoteRepositoryImpl` is the Android actual (Room-backed). A Web actual can sit over IndexedDB or SQLDelight-js without touching the UI.
- **`SyncEngine` is an interface.** `NoopSyncEngine` is the shared default (see [docs/sync-serverless.md](sync-serverless.md)); per-target real backends (Netdisk over HTTP, LAN over NSD) plug in here.
- **`RichDocument` stays portable.** Every v2 field added this milestone (`ParagraphStyle.CODE`, `ParagraphAlignment`, `RichParagraph.alignment`, `RichParagraph.indentLevel`, `RichSpan.url`) carries a default; the `Json` config (`ignoreUnknownKeys = true` + `encodeDefaults = true`) is the schema-evolution policy across all targets.

## Known limitation: absolute image paths

`AndroidImageStore` stores image paths as **absolute** (`filesDir/images/<noteId>/<sha256>.webp`). On app reinstall or device migration the absolute path changes, so references inside old notes break. This is **documented here as a known limitation, not fixed** in this milestone. The KMP restructure is the natural place to address it (a content-URI scheme or a per-target path resolver behind the `ImageStore` interface). Do NOT add a content-URI resolver or refcount scheme as a one-off Android patch — wait for the restructure so the fix is uniform across targets.

## Target platforms

| Target | UI | Status |
|---|---|---|
| Android | Compose Multiplatform (android target) | **Shipping today** (this repo, `main` branch). |
| Desktop (JVM) | Compose Multiplatform (desktop target) | Future. Same Composables, `application { Window(...) }` entry. |
| Web | Compose Multiplatform (WasmJs target) | Future. Same Composables, browser canvas. Persist via IndexedDB / OPFS. |

## Proposed module structure (next milestone)

```
/
├── composeApp/                 # KMP Compose Multiplatform module
│   └── src/
│       ├── commonMain/         # shared: theme, components, RichDocument, repository interface, SyncEngine
│       ├── androidMain/        # Room-android, AndroidImageStore, PickVisualMedia
│       ├── desktopMain/        # Room-jvm (multiplatform-room or SQLDelight), DesktopImageStore (ImageIO)
│       └── wasmJsMain/         # IndexedDB persistence, browser image APIs
├── iosMain/ (optional, later)  # iOS via Compose Multiplatform (experimental) or skip
```

## What stays shared (commonMain)

- `model/RichDocument.kt` — already `@Serializable`, pure Kotlin, portable today.
- `model/Weather.kt`, `model/Mood.kt` — `key`-only enums (the icon/label maps become a `commonMain` `expect fun` or MOKO resources).
- `model/NkPalette.kt` — named default tag color, pure Kotlin.
- `data/repository/NoteRepository.kt` — interface; per-target impl.
- `data/image/ImageStore.kt` — interface (shipped this milestone); per-target impl.
- `data/sync/SyncEngine.kt` — interface; per-target impl.
- `ui/theme/*` — pure Compose, target-agnostic (the design tokens `NkShapes` / `NkSpacing` / `NkIconSize` are pure values).
- `ui/components/*` — pure Compose (the shared `NkTopAppBar` / `NkSettingsRow` / `NkEmptyState` / `NkList`).
- All screen Composables (`HomePage`, `EditorPage`, `CalendarPage`, …) — pure Compose.

## What needs per-target `actual`s

| Concern | Android (done) | Desktop | Web |
|---|---|---|---|
| Persistence | Room (current) | multiplatform-room (JVM) or SQLDelight | IndexedDB via kotlinx-browser / SQLDelight-js |
| Image pick | `PickVisualMedia` | AWT `FileDialog` / `JFileChooser` | `<input type=file>` |
| Image compress | `Bitmap.compress(WebP)` | `ImageIO` + javax.imageio | Canvas `toBlob` |
| Date picker | Material3 `DatePicker` | Compose `DatePicker` (same) | Compose `DatePicker` (same) |
| File system | `filesDir` | `java.io.File` (user home) | OPFS / IndexedDB |
| Sync transport | (Noop today) | ktor-client (CIO) | ktor-client (js / fetch) |

## Scope for the *current* Android milestone (DONE)

This repo delivers the **Android** target with the shared layer already written in a KMP-friendly style:

- ✅ `RichDocument` is in a `commonMain`-ready position (pure Kotlin + kotlinx.serialization, v2 schema with defaults).
- ✅ `NoteRepository` is an interface; `NoteRepositoryImpl` is the Android actual.
- ✅ `SyncEngine` is an interface; `NoopSyncEngine` is the shared default.
- ✅ `ImageStore` is an interface; `AndroidImageStore` is the Android actual (this milestone — todo 3).
- ✅ `Weather`/`Mood`/`NkPalette` are pure Kotlin; icon/label maps live in UI (this milestone — todo 2).
- ✅ Theme and Composables use only Compose APIs that exist in Compose Multiplatform today (design tokens, shared components).

**Not done in this milestone (intentional — the next milestone):** Gradle KMP restructuring into `composeApp`, desktop/web targets, iOS, the absolute-image-path fragility fix. Those are tracked as follow-up work; the architecture is shaped to admit them without rewriting the UI, the rich-text model, the repository, or the sync seam.

## Tech stack (target, multiplatform)

| Layer | Choice |
|---|---|
| Language | Kotlin (shared) |
| UI | Compose Multiplatform |
| Persistence | multiplatform-room (Android + Desktop); SQLDelight or IndexedDB (Web) — behind `NoteRepository` |
| Serialization | kotlinx.serialization (already in use) |
| Image loading | Coil 3 (multiplatform) — migration from Coil 2 when going CMP |
| Sync | ktor-client (multiplatform); user-supplied netdisk API or LAN — behind `SyncEngine` |
| DI | Keep manual `AppContainer` or migrate to kotlin-inject (multiplatform) |
| Resources | MOKO Resources (zh / en) for shared string tables |

## Risks / open questions

1. **Compose Multiplatform Web (WasmJs) maturity** — still stabilizing; binary size and cold-start matter for a diary app. Acceptable to ship Desktop first, Web second.
2. **multiplatform-room Web support** — not yet first-class; Web may need SQLDelight or IndexedDB. The `NoteRepository` interface isolates this.
3. **Coil 3 migration** — Coil 2 (current) is Android-only; moving to CMP requires Coil 3. Low-risk, mostly import changes.
4. **Image compression parity** — WebP via `Bitmap.compress` is Android; Desktop uses `ImageIO` (WebP plugin or PNG/WebP via TwelveMonkeys); Web uses Canvas. The `ImageStore` interface hides this (shipped this milestone).
5. **Absolute-image-path fragility** — stored paths are absolute today; on reinstall/migration they break. Documented as a known limitation above; fix lands with the KMP restructure so it is uniform across targets.

## Migration order (next milestone, recommended)

1. Extract `composeApp` KMP module; move `commonMain`-ready files in (the seams from todos 2/3 already make this a mechanical move, not a design pass).
2. Address the absolute-image-path fragility inside the `ImageStore` interface (content-URI scheme or per-target path resolver) so the fix is uniform across targets.
3. Add `desktopMain` target; wire Room-jvm; reuse all Composables.
4. Add `wasmJsMain` target; wire IndexedDB; reuse Composables.
5. Migrate Coil 2 → Coil 3; migrate string resources to MOKO Resources.
6. Implement a real `SyncEngine` (Netdisk first, LAN second).
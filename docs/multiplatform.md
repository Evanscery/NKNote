# NKNote — Multi-platform Scope (Web / PC / Android)

> Status: **scope & tech stack decision document**. Implementation is future work.
> Goal: extend NKNote to Android, Desktop, and Web while preserving UI and feature parity.

## Decision: Kotlin Multiplatform + Compose Multiplatform

The single codebase path that preserves both **UI consistency** and **feature consistency** across Android, Desktop, and Web is **Kotlin Multiplatform (KMP) + Compose Multiplatform**.

Rationale:
- The Android app is *already* Kotlin + Compose. Moving to Compose Multiplatform is the smallest delta.
- The `RichDocument` schema is `kotlinx.serialization`-portable and was designed to be byte-for-byte identical across targets.
- Compose Multiplatform shares ~90% of UI code across Android / Desktop / Web (Wasm).
- Room now supports KMP (multiplatform-room), so the data layer can largely stay.

## Target platforms

| Target | UI | Status |
|---|---|---|
| Android | Compose Multiplatform (android target) | **Shipping today** (this repo, `main` branch). |
| Desktop (JVM) | Compose Multiplatform (desktop target) | Future. Same Composables, `application { Window(...) }` entry. |
| Web | Compose Multiplatform (WasmJs target) | Future. Same Composables, browser canvas. Persist via IndexedDB / OPFS. |

## Proposed module structure (future)

```
/
├── composeApp/                 # KMP Compose Multiplatform module
│   └── src/
│       ├── commonMain/         # shared: theme, components, RichDocument, repository interface, SyncEngine
│       ├── androidMain/        # Room-android, ImageStore-android, PickVisualMedia
│       ├── desktopMain/        # Room-jvm (sqldight or multiplatform-room), ImageStore-jvm (BufferedImage)
│       └── wasmJsMain/         # IndexedDB persistence, browser image APIs
├── iosMain/ (optional, later)  # iOS via Compose Multiplatform (experimental) or skip
```

## What stays shared (commonMain)

- `model/RichDocument.kt` — already `@Serializable`, portable.
- `model/Weather.kt`, `model/Mood.kt` — enum + label key (label becomes a `commonMain` string-resource equivalent, e.g. `expect`/`actual` or MOKO resources).
- `data/repository/NoteRepository.kt` — interface; per-target impl.
- `data/sync/SyncEngine.kt` — interface; per-target impl.
- `ui/theme/*` — pure Compose, target-agnostic.
- `ui/components/*` — pure Compose.
- All screen Composables (`HomePage`, `EditorPage`, …) — pure Compose.

## What needs per-target `actual`s

| Concern | Android | Desktop | Web |
|---|---|---|---|
| Persistence | Room (current) | multiplatform-room (JVM) or SQLDelight | IndexedDB via kotlinx-browser / SQLDelight-js |
| Image pick | `PickVisualMedia` | AWT `FileDialog` / JFileChooser | `<input type=file>` |
| Image compress | `Bitmap.compress(WebP)` | `ImageIO` + javax.imageio | Canvas `toBlob` |
| Date picker | Material3 `DatePicker` | Compose `DatePicker` (same) | Compose `DatePicker` (same) |
| File system | `filesDir` | `java.io.File` (user home) | OPFS / IndexedDB |
| Sync transport | OkHttp / ktor-client | ktor-client (CIO) | ktor-client (js / fetch) |

## Scope for the *current* Android milestone

This repo delivers the **Android** target with the shared-layer already written in a KMP-friendly style:

- `RichDocument` is in `commonMain`-ready position (pure Kotlin + kotlinx.serialization).
- `NoteRepository` is an interface; `NoteRepositoryImpl` is the Android actual.
- `SyncEngine` is an interface; `NoopSyncEngine` is the shared default.
- Theme and Composables use only Compose APIs that exist in Compose Multiplatform today.

**Not done in this milestone (intentional):** Gradle KMP restructuring, desktop/web targets, iOS. Those are tracked as follow-up work; the architecture is shaped to admit them without rewriting the UI or the rich-text model.

## Tech stack (target, multiplatform)

| Layer | Choice |
|---|---|
| Language | Kotlin (shared) |
| UI | Compose Multiplatform |
| Persistence | multiplatform-room (Android + Desktop); SQLDelight or IndexedDB (Web) |
| Serialization | kotlinx.serialization (already in use) |
| Image loading | Coil 3 (multiplatform) — migration from Coil 2 when going CMP |
| Sync | ktor-client (multiplatform); user-supplied netdisk API or LAN |
| DI | Keep manual `AppContainer` or migrate to kotlin-inject (multiplatform) |
| Resources | MOKO Resources (zh / en) for shared string tables |

## Risks / open questions

1. **Compose Multiplatform Web (WasmJs) maturity** — still stabilizing; binary size and cold-start matter for a diary app. Acceptable to ship Desktop first, Web second.
2. **multiplatform-room Web support** — not yet first-class; Web may need SQLDelight or IndexedDB. The `NoteRepository` interface isolates this.
3. **Coil 3 migration** — Coil 2 (current) is Android-only; moving to CMP requires Coil 3. Low-risk, mostly import changes.
4. **Image compression parity** — WebP via `Bitmap.compress` is Android; Desktop uses `ImageIO` (WebP plugin or PNG/WebP via TwelveMonkeys); Web uses Canvas. The `ImageStore` interface hides this.

## Migration order (recommended)

1. Extract `composeApp` KMP module; move `commonMain`-ready files in.
2. Add `desktopMain` target; wire Room-jvm; reuse all Composables.
3. Add `wasmJsMain` target; wire IndexedDB; reuse Composables.
4. Migrate Coil 2 → Coil 3; migrate string resources to MOKO Resources.
5. Implement a real `SyncEngine` (Netdisk first, LAN second).
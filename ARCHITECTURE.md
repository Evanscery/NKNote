# NKNote — Architecture

## Overview

NKNote is a **serverless**, **lightweight**, **offline-first** Android diary app built with Kotlin and Jetpack Compose. It follows **MVVM + Repository** with a manual dependency container (no Hilt/Koin). The previous codebase (on the `master` branch) depended on six external UI/editor/picker libraries; this refactor removes all of them and replaces each with a native Compose / platform-API implementation.

## Layered design

```
┌────────────────────────────────────────────────────────────┐
│  UI (Compose)                                               │
│  pages: Home · Editor · Viewer · Trash · Random · Explore  │
│  · Import   |   theme   |   components   |   navigation     │
│  ViewModels own screen state; subscribe to Repository Flows │
└───────────────┬────────────────────────────────────────────┘
                │ depends on
┌───────────────▼────────────────────────────────────────────┐
│  core / AppContainer (manual DI)                            │
│  wires: NkNoteDatabase · NoteRepository · ImageStore ·      │
│         SyncEngine                                          │
└───────────────┬────────────────────────────────────────────┘
                │ owns
┌───────────────▼────────────────────────────────────────────┐
│  data                                                       │
│  db/        Room entities + DAOs + NkNoteDatabase           │
│  repository NoteRepository (interface) + NoteRepositoryImpl │
│  image/     ImageStore — native WebP compression            │
│  importer/  TextImporter — .txt → Note                      │
│  sync/      SyncEngine abstraction (Noop default)           │
└────────────────────────────────────────────────────────────┘
                │ portable
┌───────────────▼────────────────────────────────────────────┐
│  model                                                      │
│  RichDocument (kotlinx-serializable rich-text schema)       │
│  Weather · Mood  (persisted by key, locale-resolved in UI)  │
└────────────────────────────────────────────────────────────┘
```

## Data model

### Entities (Room)

| Entity | Purpose | Notes |
|---|---|---|
| `Note` | A diary entry | PK `id: Int` autoGenerate. Stores `content` as serialized `RichDocument` JSON. Has `date`, `weather` (key), `mood` (key), `isDeleted`, `deletedAt`, `version`, `syncStatus`. |
| `Tag` | A label | PK `id: String` (e.g. `tag:<hash>`). Has `name`, `color`, `createdAt`. |
| `NoteTag` | Note↔Tag join | Composite PK `(noteId, tagId)`. FK CASCADE on both sides. |

**Removed vs. old schema:** `Image` entity (images are now files on disk, referenced by path inside `RichDocument`); `SyncRecord` entity (sync is a serverless seam, see below). The old `NoteTag.noteId: String` / `Image.noteId: String` type mismatch with `Note.id: Int` is fixed — all note references are `Int`.

### Rich text: `RichDocument`

The portable rich-text schema is the load-bearing design choice for the multi-platform goal. It is a plain Kotlin data model serialized with `kotlinx.serialization`:

```
RichDocument
 └─ List<RichParagraph>
     ├─ style:  TITLE | HEADING | SUBHEADING | BODY | QUOTE | BULLET | NUMBERED
     ├─ List<RichSpan>  (a *partition* of the paragraph text)
     │    ├─ text
     │    ├─ bold / italic / underline / strikethrough
     │    ├─ color: String?    (#AARRGGBB, null = theme default)
     │    └─ fontSizeScale: Float
     └─ image: InlineImage?  (path + width + height)
```

**Why a partition?** Each span carries the *complete* style for its character run, and spans are non-overlapping and contiguous. This makes style toggling a simple per-character transform (see `ui/editor/richtext/RichEditor.kt`) and keeps the model diff-free and JSON-portable.

**Editor → model:** `EditorDocument.toModel()` re-merges spans into the persisted partition.
**Model → editor:** `RichDocument.toEditor()` reconstructs the editor's mutable per-paragraph state.

### Image storage

`ImageStore` (no external libraries):

1. Decode source URI bytes.
2. Compute `inSampleSize` to bound max dimension to 1600px.
3. Downscale if needed, re-encode to **WebP quality 80** into `filesDir/images/<noteId>/<sha256>.webp`.
4. Return absolute path; the editor embeds it as an `InlineImage` inside `RichDocument`.

The DB stays tiny (paths only); images are compressed on disk. This satisfies the "compressed storage, especially images" constraint with zero third-party code.

## MVVM wiring

- **View** = Composable screen (`HomePage`, `EditorPage`, …). Stateless; reads from a ViewModel.
- **ViewModel** = `HomeViewModel`, `EditorViewModel`, `TrashViewModel`, `ExploreViewModel`. Owns `StateFlow`/`mutableStateOf` screen state; calls the repository; survives config changes.
- **Model** = `NoteRepository` (interface) + `NoteRepositoryImpl`; `ImageStore`; `TextImporter`; `SyncEngine`.
- **DI** = `AppContainer`, created once in `NkNoteApplication`. Exposed to Composables via `Context.appContainer()`. ViewModel construction uses `viewModelFactory { initializer { … } }`.

## Navigation

Single `NavHost` in `NkNoteApp`. Routes:

| Route | Screen | Args |
|---|---|---|
| `home` | Home | — |
| `editor/{noteId}` | Editor | `noteId: Int` (−1 = new) |
| `viewer/{imagePath}` | Viewer | `imagePath: String` (URL-encoded) |
| `trash` | Trash | — |
| `random` | Random | — |
| `explore` | Explore | — |
| `import` | Import | — |

Navigation actions are passed as a `NkNoteNavigation` value object, keeping screens decoupled from `NavController`.

## Sync (serverless, opt-in)

`SyncEngine` is the seam:

```kotlin
interface SyncEngine {
    val isConfigured: Boolean
    suspend fun push()
    suspend fun pull()
}
```

- **Default:** `NoopSyncEngine` — local-only, inert.
- **Planned (Netdisk):** user supplies their own object-storage / netdisk API credentials. Pushes/pulls notes + compressed images over HTTP. **No NKNote-run server** — this is the serverless constraint.
- **Planned (LAN):** peer-to-peer via NSD/mDNS on the local network.

`Note.version` and `Note.syncStatus` are retained in the schema to support conflict detection once a backend is wired; they are inert today.

## i18n

- `res/values/strings.xml` — Simplified Chinese (default).
- `res/values-en/strings.xml` — English.
- `Weather` and `Mood` enums carry a `labelRes: Int` resolved per-locale at the call site via `stringResource(...)`.
- No hardcoded UI strings in code. (Verified during the refactor.)

## Theme

A curated **"warm paper + sage"** palette, intentionally *not* Material You defaults:

- Light: warm off-white paper `#FAF7F2`, sage accent `#5E7A6E`, warm clay secondary `#B08968`, warm near-black ink `#2A2723`.
- Dark: warm near-black paper `#16140F`, light sage, light clay.
- Generous rounded shapes (`NkShapes`), a quiet type scale (`NkTypography`).
- Dynamic color is **disabled** — the brand palette is part of the identity.

See `ui/theme/Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`.

## Constraints honored

| Constraint | How |
|---|---|
| Lightweight | No Hilt/Koin; manual DI. No richeditor/PictureSelector/FileOperator/Luban/Glide/Gson/accompanist. |
| Serverless | `SyncEngine` seam + `NoopSyncEngine` default. No backend shipped. |
| Compressed storage | `ImageStore` → WebP q80, max 1600px, files on disk (not DB blobs). |
| i18n | zh + en string resources; `Weather`/`Mood` locale-resolved. |
| No third-party editor/picker/calendar | Native Compose `BasicTextField` + `VisualTransformation`; `PickVisualMedia`; Material3 `DatePicker`. |
| Non-default Compose look | Curated palette, custom type scale, soft shapes. |
# NKNote — Architecture

## Overview

NKNote is a **serverless**, **lightweight**, **offline-first** Android diary app built with Kotlin and Jetpack Compose. It follows **MVVM + Repository** with a manual dependency container (no Hilt/Koin). The previous codebase (on the `master` branch) depended on six external UI/editor/picker libraries; this refactor removes all of them and replaces each with a native Compose / platform-API implementation. The current milestone further hardens the data layer (FTS4 search, indices, transactional tags, `TypeConverter`), unifies the design system (tokens + shared components), introduces a global grouped drawer, and lifts the rich editor to a full power-user feature set (undo/redo, alignment, indent, code, links, find-in-page, autosave, drafts, templates) — all native, all without new third-party dependencies.

## Layered design

```
┌────────────────────────────────────────────────────────────┐
│  UI (Compose)                                               │
│  pages: Home · Editor · Viewer · Trash · Random · Explore   │
│         · Calendar · Import · Tools · Settings               │
│  theme (Color/Type/Shape/NkSpacing/NkIconSize) · components │
│  navigation (NavHost + global ModalNavigationDrawer)        │
│  ViewModels own StateFlow UI state; subscribe to Repo Flows │
└───────────────┬────────────────────────────────────────────┘
                │ depends on
┌───────────────▼────────────────────────────────────────────┐
│  core / AppContainer (manual DI)                            │
│  wires: NkNoteDatabase · NoteRepository · ImageStore ·      │
│         SyncEngine · themeMode: MutableStateFlow<ThemeMode> │
└───────────────┬────────────────────────────────────────────┘
                │ owns
┌───────────────▼────────────────────────────────────────────┐
│  data                                                       │
│  db/        Room entities + DAOs + NkNoteDatabase +          │
│             Converters + MIGRATIONS seam                    │
│  repository NoteRepository (interface) + NoteRepositoryImpl │
│  image/     ImageStore (interface) + AndroidImageStore      │
│  importer/  TextImporter — .txt → Note                      │
│  sync/      SyncEngine abstraction (Noop default)           │
│  templates/ NoteTemplates — Gratitude/Daily Log/Free Write │
└───────────────┬────────────────────────────────────────────┘
                │ portable
┌───────────────▼────────────────────────────────────────────┐
│  model  (pure Kotlin, no Compose deps)                     │
│  RichDocument (kotlinx-serializable rich-text schema)        │
│  Weather · Mood  (key only; icon/label maps live in UI)     │
│  NkPalette (named default tag color)                       │
└────────────────────────────────────────────────────────────┘
```

## Data model

### Entities (Room)

| Entity | Purpose | Notes |
|---|---|---|
| `Note` | A diary entry | PK `id: Int` autoGenerate. Stores `content` as serialized `RichDocument` JSON. **Indices** on `isDeleted` and `date` (covers the two hottest queries). Fields: `title`, `excerpt` (renamed from `description`), `content`, `searchText` (plain-text denormalization for FTS, derived via `RichDocument.plainText()` in `NoteRepositoryImpl.deriveSearchFields`), `date` (`yyyy-MM-dd`), `monthDay` (`MM-dd` derived from `date` so `observeByMonthDay` no longer uses `substr()`), `weather`/`mood` (key, `""` = none), `coverImagePath`, `createdAt`, `updatedAt`, `isDeleted`, `deletedAt`, `version`, `syncStatus`. |
| `NoteFts` | Full-text index | `@Fts4(contentEntity = Note::class)` over `searchText` / `title` / `excerpt`. Room auto-generates sync triggers on `notes` so the FTS table is kept in lock-step — no manual insert/update/delete. |
| `Tag` | A label | PK `id: String` (e.g. `tag:<hash>`). Has `name`, `color`, `createdAt`. |
| `NoteTag` | Note↔Tag join | Composite PK `(noteId, tagId)`. FK CASCADE on both sides. |

**`MIGRATIONS` seam.** `NkNoteDatabase.MIGRATIONS` is an empty `Array<Migration>` scaffold wired through `.addMigrations(*MIGRATIONS)`. The current schema bumps to `version = 2` and ships `exportSchema = true`; the destructive rebuild (`fallbackToDestructiveMigration`) is owner-approved because there are no real users yet. The seam exists so a *future* schema-preserving migration can land without restructuring the DB builder — today it stays empty by design.

**`TypeConverter`.** `db/Converters.kt` registers a `@TypeConverter RichDocument ↔ String` using the same lenient `Json` (`ignoreUnknownKeys = true` + `encodeDefaults = true`) as the editor/importer. This keeps a single serialization policy across the app and lets `RichDocument` evolve (new fields with defaults decode cleanly from old JSON).

**Image cleanup.** Image files are deleted *only* on permanent-delete (`NoteRepositoryImpl.permanentlyDelete`) and `emptyTrash` (which first reads `NoteDao.getDeletedIds()` then drops the rows, then calls `imageStore.deleteAllForNote(id)` for each id). Soft-delete (`moveToTrash`) deliberately keeps the per-note image folder so a `restoreNote` brings the images back. This invariant is the load-bearing reason `emptyTrash` reads ids *before* deleting rows.

### Rich text: `RichDocument` (v2 schema)

The portable rich-text schema is the load-bearing design choice for the multi-platform goal. It is a plain Kotlin data model serialized with `kotlinx.serialization`; every field added after v1 carries a default, so legacy JSON decodes without throwing.

```
RichDocument
 └─ List<RichParagraph>
     ├─ style:  TITLE | HEADING | SUBHEADING | BODY | QUOTE | BULLET | NUMBERED | CODE
     ├─ alignment:  START | CENTER | END
     ├─ indentLevel: Int = 0   (0..3; clamped by the toolbar)
     ├─ List<RichSpan>  (a *partition* of the paragraph text)
     │    ├─ text
     │    ├─ bold / italic / underline / strikethrough
     │    ├─ color: String?    (#AARRGGBB, null = theme default)
     │    ├─ fontSizeScale: Float
     │    └─ url: String?       (when non-null, this span is a hyperlink)
     └─ image: InlineImage?  (path + width + height)
```

**v2 additions (this milestone):** `ParagraphStyle.CODE`, `ParagraphAlignment`, `RichParagraph.alignment`, `RichParagraph.indentLevel`, `RichSpan.url`. All carry defaults; the serializer is `ignoreUnknownKeys + encodeDefaults`.

**Why a partition?** Each span carries the *complete* style for its character run, and spans are non-overlapping and contiguous. This makes style toggling a simple per-character transform (see `ui/editor/richtext/RichEditor.kt`) and keeps the model diff-free and JSON-portable.

**Editor → model:** `EditorDocument.toModel()` re-merges spans into the persisted partition.
**Model → editor:** `RichDocument.toEditor()` reconstructs the editor's mutable per-paragraph state.
**`plainText()`:** joins paragraph spans; consumed by `NoteRepositoryImpl` for `searchText` derivation and by `EditorViewModel` for `wordCount`/`charCount` and find-in-page.

### Image storage

`ImageStore` is an **interface** (the multiplatform seam); `AndroidImageStore` is the Android `actual` (no external libraries):

1. Decode source URI bytes.
2. Compute `inSampleSize` to bound max dimension to **1600 px**.
3. Downscale if needed, re-encode to **WebP quality 75** into `filesDir/images/<noteId>/<sha256>.webp`.
4. Return absolute path; the editor embeds it as an `InlineImage` inside `RichDocument`.

Reconciled params: **q75 / 1600 px** (the previous q70 / 1280 px was a stale constant; the README's q80 / 1600 claim was also stale — both are corrected in this milestone). The DB stays tiny (paths only). Cross-note dedup is intentionally NOT done (simpler than refcounting). **Known limitation:** stored paths are absolute; on app reinstall or device migration they break. Documented in `docs/multiplatform.md`; out of scope here.

## MVVM wiring

- **View** = Composable screen (`HomePage`, `EditorPage`, `CalendarPage`, `SettingsPage`, …). Stateless; reads from a ViewModel.
- **ViewModel** = `HomeViewModel`, `EditorViewModel`, `TrashViewModel`, `ExploreViewModel`, `ImportViewModel`, `ImageViewerViewModel`. Owns `StateFlow` UI state; calls the repository; survives config changes.
- **Model** = `NoteRepository` (interface) + `NoteRepositoryImpl`; `ImageStore` (interface) + `AndroidImageStore`; `TextImporter`; `SyncEngine`; `NoteTemplates`.
- **DI** = `AppContainer`, created once in `NkNoteApplication`. Exposed to ViewModels via the `AppViewModelFactory` (which takes `AppContainer` and resolves deps internally — no Composable resolves the container).

### `EditorViewModel` — the load-bearing MVVM split

The editor splits observable state from the real-time editing buffer:

- **`StateFlow<EditorUiState>`** — title, excerpt, date, weather/mood keys, cover image path, tag names, `canUndo`/`canRedo`, `styleAtCursor`, `paragraphStyleAtCursor`, `paragraphAlignmentAtCursor`, `indentLevelAtCursor`, `wordCount`/`charCount`, `linkAtCursor`/`codeAtCursor`, `findMatches`/`findIndex`. Consumed via `collectAsStateWithLifecycle()`.
- **Editing buffer** — `fields: SnapshotStateList<TextFieldValue>`, `focusedIndex`, `pendingFocusIndex` stay in `mutableStateOf` / `mutableStateListOf` for **keystroke latency** (StateFlow conflation under rapid typing risks losing intermediate `TextFieldValue` edits, especially IME composition + selection). The Composable reads the buffer directly.

This split is intentional; do NOT collapse it.

### Undo / redo

The editor keeps an `ArrayDeque<EditorCommand>` undo stack and a redo stack (capped at ~100). Each `EditorCommand` carries the before/after state of the region it touched (text edit, span toggle, paragraph-style change, paragraph split/merge, image insert) — *not* raw document snapshots, so the stack stays light even for large documents. Image *removal* is intentionally not reversible (the image file is deleted on removal — re-inserting the paragraph would point at a missing file). The stack is **session-scoped**: it is NOT persisted; reopening a note starts a fresh stack. Saving does not clear the stack.

### Autosave + draft

- **Autosave on `Lifecycle.Event.ON_STOP`** + a debounced save (3 s after the last edit, driven by an `editVersion` counter the `EditorPage` watches via `LaunchedEffect`).
- **`saveMutex`** serializes `save()` so a manual save and a concurrent ON_STOP / debounced autosave can't both observe the "new note" branch and create duplicate rows. The first save of a new note (`noteId == -1`) calls `repo.insertNote` and remembers the returned id in `savedNoteId`; every subsequent save calls `repo.updateNote(note.copy(id = savedNoteId))` — never `insertNote` again.
- **Draft persistence via `SavedStateHandle`:** `TextFieldValue` is not SavedStateHandle-safe across process death, so the draft stores per-paragraph `(text: String, selectionStart: Int, selectionEnd: Int)` tuples (`DraftSelection`) and the VM reconstructs `TextFieldValue(text, TextRange(start, end))` on restore. Dialog-visibility flags are transient and not persisted.

### Templates

`data/templates/NoteTemplates.kt` ships three built-in starter documents (`Gratitude` — heading + 3 empty bullets; `Daily Log` — heading + empty body; `Free Write` — single empty body paragraph). Picking a template replaces the editor's current document via `EditorViewModel.applyTemplate`. No external template engine.

### Other VMs

- **`ImportViewModel`** (`AndroidViewModel`) — `import(fileUri)` reads the file via the Application's `ContentResolver`, parses with `TextImporter`, inserts via `NoteRepository`. Emits a `StateFlow<Result>` (`Idle / Busy / Success / Failed(message)`). The screen no longer touches the repo.
- **`ImageViewerViewModel`** (`AndroidViewModel`) — `delete(path)` and `buildShareIntent(path): Intent?`. The share intent uses `FileProvider.getUriForFile(getApplication(), "io.github.nknote.fileprovider", File(path))` with `FLAG_GRANT_READ_URI_PERMISSION`; returns `null` if the file is missing or the provider isn't registered (UI shows a toast instead of crashing). No raw `Context` is passed in.

## Navigation

A single `NavHost` in `NkNoteApp`, wrapped by a **global `ModalNavigationDrawer`** (hoisted out of `HomePage`). Routes:

| Route | Screen | Args |
|---|---|---|
| `home` | Home | — |
| `editor/{noteId}` | Editor | `noteId: Int` (−1 = new) |
| `viewer/{imagePath}` | Viewer | `imagePath: String` (URL-encoded) |
| `trash` | Trash | — |
| `random` | Random | — |
| `explore` | Explore | — |
| `calendar` | Calendar | — |
| `tools` | Tools | — |
| `settings` | Settings | — |
| `import` | Import | — |

Navigation actions are passed as a `NkNoteNavigation` value object, keeping screens decoupled from `NavController`.

### Global drawer + grouped sections

The drawer is grouped into three sections, each a `NkDrawerSection` of `NkDrawerItem`s:

- **Journal** → Home · Calendar · Explore
- **Tools** → Random · Import
- **System** → Trash · Settings

`ModalNavigationDrawer` is created with **`gesturesEnabled = false`** — the swipe-open gesture would otherwise intercept horizontal drags on inner composables (the editor's scrollable FormatBar, the `ImageViewerPage`'s pinch/pan). The hamburger icon opens the drawer instead.

`DrawerState` is shared via `LocalDrawerState` (`staticCompositionLocalOf`), provided at `NkNoteApp` scope and read in each primary screen's `NkTopAppBar` to wire the hamburger. Editor and Viewer keep a back arrow (they are reached by direct navigation, not the drawer).

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

`Note.version` and `Note.syncStatus` are retained in the schema to support conflict detection once a backend is wired; they are inert today. See **[docs/sync-serverless.md](docs/sync-serverless.md)** for the conflict-detection design.

## i18n

- `res/values/strings.xml` — Simplified Chinese (default).
- `res/values-en/strings.xml` — English.
- `Weather` and `Mood` enums are **portable** — they carry only a `key: String`. The icon (`ImageVector`) and localized label (`labelRes: Int`) maps live in `ui/components/WeatherIconMap.kt` and `MoodIconMap.kt`, so `model/` has zero Compose imports and is `commonMain`-ready.
- No hardcoded UI strings in code. (Verified during the refactor.)

## Theme

A curated **"warm paper + sage"** palette, intentionally *not* Material You defaults:

- Light: warm off-white paper `#FAF7F2`, sage accent `#5E7A6E`, warm clay secondary `#B08968`, warm near-black ink `#2A2723`.
- Dark: warm near-black paper `#16140F`, light sage, light clay.
- Generous rounded shapes (`NkShapes`), a quiet type scale (`NkTypography`).
- Dynamic color is **disabled** — the brand palette is part of the identity.

### Design-token system

Three `object`s in `ui/theme/`:

- **`NkShapes`** — Material3 `Shapes` slots (extraSmall 6 / small 10 / medium 16 / large 22 / extraLarge 30) **plus** in-between tokens that the UI actually uses (`smallMedium 14`, `mediumSmall 12`, `mediumLarge 18`, `largeSmall 20`) so every `RoundedCornerShape(<n>.dp)` call resolves to a named token. The Material3 `Shapes` binding is exposed as `NkShapes.material`.
- **`NkSpacing`** — 4-based spacing tokens (`xs 4` / `sm 8` / `md 12` / `lg 16` / `xl 24` / `xxl 32`).
- **`NkIconSize`** — four icon-size steps (`sm 16` / `md 20` / `lg 24` / `xl 28`), collapsing the former 7 ad-hoc sizes.

Invariant: **no `RoundedCornerShape(<n>.dp)` outside `ui/theme/Shape.kt`** (enforced by grep; the only literals remaining are component-specific sizing inside `NkSettingsRow`).

### Shared components

`ui/components/` houses the four shared composables that the screens reuse:

- **`NkTopAppBar(title, onBack, actions, drawerState)`** — wraps Material3 `TopAppBar` with `containerColor = MaterialTheme.colorScheme.background` (warm paper, not the M3 surface tint). Navigation icon is a hamburger when `drawerState != null`, otherwise a back arrow when `onBack != null`.
- **`NkSettingsRow(icon, title, subtitle, onClick, trailing)`** — unifies the former `SettingsPage.SettingsRow` and `ToolsPage.ToolRow` (which were line-for-line duplicates). `trailing` defaults to a chevron; pass a `Switch` for toggle rows, `{}` for no trailing.
- **`NkEmptyState(message)`** — the `Box(fillMaxSize, center) { Text(...) }` pattern used by Home and Trash.
- **`NkList`** — `nkListPadding(padding): PaddingValues` shared list padding (top from Scaffold, bottom = scaffold + `NkSpacing.xl`, sides = `NkSpacing.lg`).

### Theme-mode persistence

`AppContainer` owns the single source of truth for theme mode:

```kotlin
enum class ThemeMode { SYSTEM, LIGHT, DARK }

class AppContainer(context: Context) {
    val themeMode: MutableStateFlow<ThemeMode> = MutableStateFlow(
        // read synchronously in the constructor (runs in Application.onCreate,
        // before the first Compose tree) so there is NO theme flash on cold start
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ...)...) }
            .getOrDefault(ThemeMode.SYSTEM)
    )
    fun setThemeMode(mode: ThemeMode) { prefs.edit().putString(...).apply(); themeMode.value = mode }
}
```

`NkNoteApp` collects `themeMode`, resolves it against `isSystemInDarkTheme()` into the boolean `darkTheme`, and publishes it via `LocalDarkTheme` so per-screen `NkNoteTheme { ... }` calls inherit the user's choice (instead of re-reading the system flag and ignoring Settings). Persistence uses `SharedPreferences` (no Datastore — lightweight constraint).

## Constraints honored

| Constraint | How |
|---|---|
| Lightweight | No Hilt/Koin; manual DI. No richeditor/PictureSelector/FileOperator/Luban/Glide/Gson/accompanist. |
| Serverless | `SyncEngine` seam + `NoopSyncEngine` default. No backend shipped. |
| Compressed storage | `AndroidImageStore` → WebP q75, max 1600 px, files on disk (not DB blobs). Cleanup only on permanent-delete + empty-trash. |
| i18n | zh + en string resources; portable `Weather`/`Mood` (key-only) with icon/label maps in UI. |
| No third-party editor/picker/calendar/compressor | Native Compose `BasicTextField` + `VisualTransformation`; `PickVisualMedia`; Material3 `DatePicker`; platform `Bitmap.compress`. |
| Non-default Compose look | Curated palette, custom type scale, soft `NkShapes`; dynamic color disabled. |
| Multiplatform-seams ready | `ImageStore`/`NoteRepository`/`SyncEngine` interfaces; `RichDocument`/`Weather`/`Mood`/`NkPalette` pure Kotlin (no Compose deps); deferred KMP Gradle restructure (see `docs/multiplatform.md`). |
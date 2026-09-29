# NKNote

A gentle, lightweight, serverless diary & notes app for Android, built with Kotlin + Jetpack Compose.
Designed to be calm, soft, and opinionated — *not* the default Material You look.

> 极简、轻量、无服务器的日记应用，基于 Kotlin + Jetpack Compose。
> 安静、柔和、有主见的视觉语言，不使用 Material You 默认外观。

---

## Features

- **Rich text diary** — native Compose editor with bold / italic / underline / strikethrough / color / font size, full heading hierarchy (Title / Heading / Subheading / Body), quotes, bullet & auto-incrementing numbered lists, checkbox task lists, and inline images. No WebView, no third-party editor.
- **Editor power tools** — undo/redo command stack with burst coalescing (word-sized undo steps), sticky style that can also *un*-style forward typing, active-style indicator on the two-tier FormatBar, scroll-to-focused, word/char count, text alignment (Start/Center/End), list nesting/indent (0–3), code blocks, hyperlinks, and find-in-page with live match highlighting.
- **Markdown shortcuts** — type `- `, `1. `, `# `/`## `/`### `, `> `, ``` ` ``` or `[] ` at a line start to convert the paragraph; Enter on an empty list item exits the list.
- **Multi-paragraph paste** — pasted newlines split into paragraphs with span styles preserved, as a single undo step. Soft-keyboard backspace at a paragraph start merges paragraphs (zero-width-sentinel detection — works on every IME, not just hardware keyboards).
- **Reader mode** — notes open in a read-only view first (tappable hyperlinks, checkable task items, images open the viewer); an edit button pushes the editor.
- **Tags** — tag entries in the editor (chips + inline input), filter the home list by tag, colored deterministically from a curated palette. Orphan tags clean themselves up.
- **Editor companion** — autosave on `Lifecycle.Event.ON_STOP` + debounced save (3 s after the last edit; pure cursor moves don't schedule saves), an empty-note guard (backing out of a pristine editor leaves no junk row), draft persistence via `SavedStateHandle` (safe across process death), and built-in templates (Gratitude / Daily Log / Free Write).
- **Image attachments** — pick from the media picker, auto-compressed to WebP **quality 75, max dimension 1600 px** on disk (no external compressor library). Stored as files (`filesDir/images/<noteId>/<sha256>.webp`); images picked before a new note's first save are re-homed from the provisional folder once the row id exists.
- **Image viewer** — zoomable preview with vertical-swipe-to-dismiss, share via `FileProvider`, delete with confirmation, and an image-info overlay (dimensions + file size).
- **Weather & mood** — tag every entry with the weather and your mood.
- **Trash** — soft-delete with undo snackbar, restore / delete-forever (confirmed) / empty-trash. Image files are cleaned up *only* on permanent-delete or empty-trash.
- **Search** — DB-side full-text search (Room **FTS4**) with a 300 ms debounce and operator-safe query sanitizing; composes with the tag filter.
- **Calendar** — month grid with live day markers, localized weekday header, month-flip animation, jump-to-today, tap a day for its notes.
- **Random picker** — a small, satisfying random-selection tool.
- **Explore** — "on this day, other years", grouped by year with "N years ago" headers.
- **Backup** — export a **.zip backup (notes + tags + image files)** with relative paths that survive reinstall; import restores images and remaps ids. `.txt` import and legacy `.json` import still work.
- **Settings** — theme mode (System / Light / Dark three-way picker, applied on cold start with no flash), export with progress/error states, GitHub link, runtime version display.
- **i18n** — English + Simplified Chinese, follows the system locale.
- **Dark theme** — warm dark palette, edge-to-edge. Navigation uses a gentle fade-through transition; destructive actions get haptic feedback.

## Screens

| Screen | Purpose |
|---|---|
| Home | Note list, FTS search + tag filter chips, undo snackbar, drawer entry points |
| Reader | Read-only note view: tappable links, checkable tasks, tags, edit button |
| Editor | Rich text editing, meta (date / weather / mood / summary / tags), image insertion, two-tier FormatBar, find-in-page, templates |
| Viewer | Zoomable image preview, swipe-to-dismiss, share, delete (confirmed), info overlay |
| Trash | Restore / delete forever (confirmed) / empty (NkDialog) |
| Random | Random option picker |
| Explore | "On this day" grouped by year + note count |
| Calendar | Month grid with live markers, jump-to-today, day notes |
| Import | Import `.txt` / `.zip` backup / legacy `.json` |
| Settings | Theme mode (3-way) + zip backup export |

Every primary screen reaches the **global drawer** (a `ModalNavigationDrawer` hoisted in `NkNoteApp`, `gesturesEnabled = false` so it doesn't steal inner horizontal drags). The drawer is grouped into three sections:

- **Journal** → Home · Calendar · Explore
- **Tools** → Random · Import
- **System** → Trash · Settings

Reader, Editor and Viewer keep a back arrow (they are reached by direct navigation, not from the drawer).

## Architecture

NKNote follows **MVVM** with a thin manual dependency container (no Hilt/Koin, to stay lightweight).

```
ui/                # Compose screens + ViewModels (MVVM view layer)
  theme/           # curated design system: Color / Type / Shape / Motion
                   #   + NkSpacing / NkIconSize (4-based tokens)
                   #   + ThemeMode wiring (LocalDarkTheme, LocalDrawerState)
  components/      # NkTopAppBar / NkSettingsRow / NkEmptyState / NkList /
                   #   NkDrawer / NkDialog / NkChoiceDialog / NkNoteCard /
                   #   NkSnackbar (app-scoped host) / pickers /
                   #   WeatherIconMap / MoodIconMap (icon maps kept OUT of model/)
  navigation/      # NavHost (+ fade-through transitions) + Destination +
                   #   NkNoteNavigation + LocalDrawerState
  home/ reader/ editor/ viewer/ trash/ random/ explore/
  calendar/ import_/ settings/
data/
  db/              # Room database + DAOs + FtsQuery sanitizer + Converters + MIGRATIONS seam
  entity/          # Note · Tag (normalized-name identity) · NoteTag · NoteFts
  repository/      # NoteRepository (interface) + NoteRepositoryImpl
  image/           # ImageStore (interface, incl. relocateToNote) + AndroidImageStore
  importer/        # TextImporter — txt → Note
  backup/          # BackupManager — zip export/import (notes + tags + images)
  sync/            # SyncEngine abstraction (serverless; default Noop)
  templates/       # NoteTemplates — built-in starter documents
model/             # RichDocument v3 (portable rich-text schema incl. CHECKBOX),
                   #   Weather, Mood, NkPalette (curated tag palette) — pure Kotlin
core/              # AppContainer (manual DI) + ThemeMode + themeMode StateFlow
```

Key shape:

- **Model portability** — `model/` is pure Kotlin (no `androidx.compose.*` imports). `Weather`/`Mood` carry only a `key`; the icon+label maps live in `ui/components/`. `RichDocument` is `@Serializable` with defaults on every field (backward-compatible).
- **`ImageStore` interface** — `ImageStore` is an interface; `AndroidImageStore` is the Android `actual`. Other platforms plug in their own without touching the repo or UI.
- **MVVM StateFlow** — observable UI state lives in `StateFlow` (e.g. `EditorUiState`, `HomeUiState`). The editor's real-time `TextFieldValue` buffer intentionally stays in `mutableStateOf` / `mutableStateListOf` for keystroke latency; the seam is documented in `EditorViewModel`.
- **FTS4 search** — `NoteFts` is `@Fts4(contentEntity = Note::class)` over `searchText` / `title` / `excerpt`; Room auto-generates the sync triggers. `NoteDao.search` joins `notes` ↔ `note_fts` on `rowid` and guards empty queries. Home search goes DB-side through `FtsQuery.sanitize` (quoted prefix tokens, operators neutralized). Known limitation: the FTS4 `simple` tokenizer doesn't segment CJK, so Chinese matches as a prefix from token start.
- **Design tokens** — every radius comes from `NkShapes`, every spacing dp from `NkSpacing`, every icon size from `NkIconSize`. No `RoundedCornerShape(<n>.dp)` outside `ui/theme/Shape.kt`.

See **[ARCHITECTURE.md](ARCHITECTURE.md)** for the full design, **[docs/tech-stack.md](docs/tech-stack.md)** for the interview-grade dependency walkthrough, **[docs/sync-serverless.md](docs/sync-serverless.md)** for the sync seam, and **[docs/multiplatform.md](docs/multiplatform.md)** for the web/PC expansion plan.

## Tech stack

| | |
|---|---|
| Language | Kotlin 1.9.24 |
| UI | Jetpack Compose (BOM 2024.06) |
| Architecture | MVVM + Repository, manual DI (`AppContainer`) |
| Persistence | Room 2.6.1 (KSP), FTS4 `note_fts` table |
| Image loading | Coil 2.6.0 |
| Serialization | kotlinx.serialization 1.6.3 (rich-text document JSON + `TypeConverter`) |
| Navigation | Navigation-Compose 2.7.7 |
| DI | Manual (`AppContainer`) — no Hilt/Koin |
| Theme | Curated "warm paper + sage" palette; `SharedPreferences` for theme mode (no Datastore) |
| Build | AGP 8.5, JDK 17, compileSdk 34, minSdk 26 |
| Test (test-only, not shipped) | JUnit4 · Robolectric 4.12.2 · room-testing · kotlinx-coroutines-test |

### No external editor / picker / calendar / compressor dependencies

This was an explicit design goal. The previous implementation depended on
`richeditor-android` (WebView), `PictureSelector`, `compose-material-dialogs:datetime`, `FileOperator`,
`Luban`, `Glide`, `Gson`, and `accompanist-flowlayout`. **All removed.** Replacements are native:

- Rich text → native Compose `BasicTextField` + `VisualTransformation` (see `ui/editor/richtext/`)
- Image picker → `ActivityResultContracts.PickVisualMedia`
- Image compression → platform `BitmapFactory` + `Bitmap.compress(WEBP, 75)`
- Date picker → Material3 `DatePicker`
- Flow layout → `androidx.compose.foundation.layout.FlowRow`
- Image share → `androidx.core.content.FileProvider` + `Intent.ACTION_SEND`

## Build

Requirements: JDK 17, Android SDK (compileSdk 34, minSdk 26).

```bash
./gradlew assembleDebug      # debug APK
./gradlew lint               # lint
./gradlew testDebugUnitTest  # Robolectric + room-testing unit tests
```

Install the APK from `app/build/outputs/apk/debug/app-debug.apk`.

## Sync (serverless, opt-in)

The `SyncEngine` seam is left open for **serverless** synchronization that the user owns:

- **Netdisk backend** (planned): user supplies their own object-storage / netdisk API credentials; notes + compressed images sync over HTTP. No NKNote-run server.
- **LAN backend** (planned): peer-to-peer discovery (NSD/mDNS) between devices on the same network.

The default engine is `NoopSyncEngine` (local-only). `Note.version` / `syncStatus` are retained to
support conflict detection once a backend is wired. See **[docs/sync-serverless.md](docs/sync-serverless.md)**.

## Branching

- `main` — the refactored, current app (this branch).
- `master` — the original beginner codebase, **kept for history and marked DEPRECATED**. It is intentionally **not** deleted.

## License

Personal / educational project. See history on the `master` branch for the original author's work.
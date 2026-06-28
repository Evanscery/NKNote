# NKNote

A gentle, lightweight, serverless diary & notes app for Android, built with Kotlin + Jetpack Compose.
Designed to be calm, soft, and opinionated — *not* the default Material You look.

> 极简、轻量、无服务器的日记应用，基于 Kotlin + Jetpack Compose。
> 安静、柔和、有主见的视觉语言，不使用 Material You 默认外观。

---

## Features

- **Rich text diary** — native Compose editor with bold / italic / underline / strikethrough / color / font size, full heading hierarchy (Title / Heading / Subheading / Body), quotes, bullet & auto-incrementing numbered lists, and inline images. No WebView, no third-party editor.
- **Editor power tools** — undo/redo command stack, sticky style, active-style indicator on the FormatBar, scroll-to-focused, word/char count, text alignment (Start/Center/End), list nesting/indent (0–3), code blocks, hyperlinks (rendered as `LinkAnnotation` in the viewer), and find-in-page.
- **Editor companion** — autosave on `Lifecycle.Event.ON_STOP` + debounced save (3 s after the last edit), draft persistence via `SavedStateHandle` (per-paragraph `(text, selectionStart, selectionEnd)` tuple, safe across process death), and built-in templates (Gratitude / Daily Log / Free Write).
- **Image attachments** — pick from the media picker, auto-compressed to WebP **quality 75, max dimension 1600 px** on disk (no external compressor library). Images are stored as files (`filesDir/images/<noteId>/<sha256>.webp`), not bloated in the database.
- **Image viewer** — zoomable preview with vertical-swipe-to-dismiss, share via `FileProvider` (`ACTION_SEND`, no new permission), and an image-info overlay (dimensions + file size).
- **Weather & mood** — tag every entry with the weather and your mood.
- **Trash** — soft-delete with restore / delete-forever / empty-trash. Image files are cleaned up *only* on permanent-delete or empty-trash (soft-delete keeps them so restore works).
- **Search** — full-text search (Room **FTS4**) across titles, excerpts, and the plain-text rendition of content. Empty query is guarded (`MATCH ''` would throw).
- **Calendar** — month grid of days with entries, tap a day to open its notes.
- **Random picker** — a small, satisfying random-selection tool.
- **Explore** — "on this day, other years": revisit past entries written on today's date.
- **Import** — import `.txt` files; auto-parsed into notes (titles, headings, quotes, lists).
- **Settings** — theme mode (System / Light / Dark, applied on cold start with no flash via `AppContainer.themeMode: MutableStateFlow<ThemeMode>`), and a JSON export of all notes.
- **Tools** — drawer hub for Random + Import.
- **i18n** — English + Simplified Chinese, follows the system locale.
- **Dark theme** — warm dark palette, edge-to-edge.

## Screens

| Screen | Purpose |
|---|---|
| Home | Note list, FTS search, drawer entry points |
| Editor | Rich text editing, meta (date / weather / mood / summary), image insertion, FormatBar + Find-in-page, templates |
| Viewer | Zoomable image preview, swipe-to-dismiss, share, info overlay |
| Trash | Restore / delete forever / empty (NkDialog) |
| Random | Random option picker |
| Explore | "On this day" across years + note count |
| Calendar | Month grid of days with notes |
| Import | Import `.txt` into notes |
| Tools | Hub for Random + Import |
| Settings | Theme mode toggle + JSON export |

Every primary screen reaches the **global drawer** (a `ModalNavigationDrawer` hoisted in `NkNoteApp`, `gesturesEnabled = false` so it doesn't steal inner horizontal drags). The drawer is grouped into three sections:

- **Journal** → Home · Calendar · Explore
- **Tools** → Random · Import
- **System** → Trash · Settings

Editor and Viewer keep a back arrow (they are reached by direct navigation, not from the drawer).

## Architecture

NKNote follows **MVVM** with a thin manual dependency container (no Hilt/Koin, to stay lightweight).

```
ui/                # Compose screens + ViewModels (MVVM view layer)
  theme/           # curated design system: Color / Type / Shape
                   #   + NkSpacing / NkIconSize (4-based tokens)
                   #   + ThemeMode wiring (LocalDarkTheme, LocalDrawerState)
  components/      # NkTopAppBar / NkSettingsRow / NkEmptyState / NkList /
                   #   NkDrawer / NkDialog / NkNoteCard / pickers /
                   #   WeatherIconMap / MoodIconMap (icon maps kept OUT of model/)
  navigation/      # NavHost + Destination + NkNoteNavigation + LocalDrawerState
  home/ editor/ viewer/ trash/ random/ explore/
  calendar/ import_/ tools/ settings/
data/
  db/              # Room database + DAOs + Converters + MIGRATIONS seam
  entity/          # Note · Tag · NoteTag · NoteFts (@Fts4 contentEntity = Note)
  repository/      # NoteRepository (interface) + NoteRepositoryImpl
  image/           # ImageStore (interface) + AndroidImageStore (actual)
  importer/        # TextImporter — txt → Note
  sync/            # SyncEngine abstraction (serverless; default Noop)
  templates/       # NoteTemplates — built-in starter documents
model/             # RichDocument (portable rich-text schema), Weather, Mood,
                   #   NkPalette (named default tag color) — pure Kotlin, no Compose deps
core/              # AppContainer (manual DI) + ThemeMode + themeMode StateFlow
```

Key shape:

- **Model portability** — `model/` is pure Kotlin (no `androidx.compose.*` imports). `Weather`/`Mood` carry only a `key`; the icon+label maps live in `ui/components/`. `RichDocument` is `@Serializable` with defaults on every field (backward-compatible).
- **`ImageStore` interface** — `ImageStore` is an interface; `AndroidImageStore` is the Android `actual`. Other platforms plug in their own without touching the repo or UI.
- **MVVM StateFlow** — observable UI state lives in `StateFlow` (e.g. `EditorUiState`, `HomeUiState`). The editor's real-time `TextFieldValue` buffer intentionally stays in `mutableStateOf` / `mutableStateListOf` for keystroke latency; the seam is documented in `EditorViewModel`.
- **FTS4 search** — `NoteFts` is `@Fts4(contentEntity = Note::class)` over `searchText` / `title` / `excerpt`; Room auto-generates the sync triggers. `NoteDao.search` joins `notes` ↔ `note_fts` on `rowid` and guards empty queries.
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
# NKNote

A gentle, lightweight, serverless diary & notes app for Android, built with Kotlin + Jetpack Compose.
Designed to be calm, soft, and opinionated — *not* the default Material You look.

> 极简、轻量、无服务器的日记应用，基于 Kotlin + Jetpack Compose。
> 安静、柔和、有主见的视觉语言，不使用 Material You 默认外观。

---

## Features

- **Rich text diary** — native Compose editor (bold / italic / underline / strikethrough / color / font size, headings, quotes, lists) with inline images. No WebView, no third-party editor.
- **Image attachments** — pick from the media picker, auto-compressed to WebP on disk (no external compressor library). Images are stored as files, not bloated in the database.
- **Weather & mood** — tag every entry with the weather and your mood.
- **Trash** — soft-delete with restore / delete-forever / empty-trash.
- **Random picker** — a small, satisfying random-selection tool.
- **Explore** — "on this day, other years": revisit past entries written on today's date.
- **Import** — import `.txt` files; auto-parsed into notes (titles, headings, quotes, lists).
- **Search** — full-text search across titles, descriptions, and content.
- **i18n** — English + Simplified Chinese, follows the system locale.
- **Dark theme** — warm dark palette, edge-to-edge.

## Screens

| Screen | Purpose |
|---|---|
| Home | Note list, search, drawer entry points |
| Editor | Rich text editing, meta (date / weather / mood / summary), image insertion |
| Viewer | Zoomable image preview with delete |
| Trash | Restore / delete forever / empty |
| Random | Random option picker |
| Explore | "On this day" across years + note count |
| Import | Import `.txt` into notes |

## Architecture

NKNote follows **MVVM** with a thin manual dependency container (no Hilt/Koin, to stay lightweight).

```
ui/                # Compose screens + ViewModels (MVVM view layer)
  theme/           # curated design system (color / type / shape)
  components/      # reusable composables + pickers
  navigation/      # nav host + destinations
  home/ editor/ viewer/ trash/ random/ explore/ import_/
data/
  db/              # Room database + DAOs
  entity/          # Room entities (Note, Tag, NoteTag)
  repository/      # NoteRepository (interface) + impl
  image/           # ImageStore — native WebP compression on disk
  importer/        # TextImporter — txt → Note
  sync/            # SyncEngine abstraction (serverless; default Noop)
model/             # RichDocument (portable rich-text schema), Weather, Mood
core/              # AppContainer (manual DI)
```

See **[ARCHITECTURE.md](ARCHITECTURE.md)** for the full design and **[docs/multiplatform.md](docs/multiplatform.md)** for the web/PC expansion plan.

## Tech stack

| | |
|---|---|
| Language | Kotlin 1.9.24 |
| UI | Jetpack Compose (BOM 2024.06) |
| Architecture | MVVM + Repository |
| Persistence | Room 2.6.1 (KSP) |
| Image loading | Coil |
| Serialization | kotlinx.serialization (rich-text document JSON) |
| Navigation | Navigation-Compose |
| DI | Manual (`AppContainer`) |
| Build | AGP 8.5, JDK 17 |

### No external editor / picker / calendar dependencies

This was an explicit design goal. The previous implementation depended on
`richeditor-android` (WebView), `PictureSelector`, `compose-material-dialogs:datetime`, `FileOperator`,
`Luban`, `Glide`, `Gson`, and `accompanist-flowlayout`. **All removed.** Replacements are native:

- Rich text → native Compose `BasicTextField` + `VisualTransformation` (see `ui/editor/richtext/`)
- Image picker → `ActivityResultContracts.PickVisualMedia`
- Image compression → platform `BitmapFactory` + `Bitmap.compress(WEBP_QUALITY)`
- Date picker → Material3 `DatePicker`
- Flow layout → `androidx.compose.foundation.layout.FlowRow`

## Build

Requirements: JDK 17, Android SDK (compileSdk 34, minSdk 26).

```bash
./gradlew assembleDebug      # debug APK
./gradlew lint               # lint
```

Install the APK from `app/build/outputs/apk/debug/app-debug.apk`.

## Sync (serverless, opt-in)

The `SyncEngine` seam is left open for **serverless** synchronization that the user owns:

- **Netdisk backend** (planned): user supplies their own object-storage / netdisk API credentials; notes + compressed images sync over HTTP. No NKNote-run server.
- **LAN backend** (planned): peer-to-peer discovery (NSD/mDNS) between devices on the same network.

The default engine is `NoopSyncEngine` (local-only). `Note.version` / `syncStatus` are retained to
support conflict detection once a backend is wired. See `data/sync/SyncEngine.kt`.

## Branching

- `main` — the refactored, current app (this branch).
- `master` — the original beginner codebase, **kept for history and marked DEPRECATED**. It is intentionally **not** deleted.

## License

Personal / educational project. See history on the `master` branch for the original author's work.
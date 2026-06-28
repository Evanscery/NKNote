# NKNote — Tech Stack & Decisions Log (interview-grade)

A dependency-by-dependency walkthrough of NKNote. For each dependency: **role**, **why it over alternatives**, **version**, and **tradeoffs**. Then the architecture decisions and a **decisions log** table. A bilingual **面试向 中文摘要** is at the end.

The version catalog is the single source of truth: [`gradle/libs.versions.toml`](../gradle/libs.versions.toml). Every dependency below is wired in [`app/build.gradle.kts`](../app/build.gradle.kts).

---

## 1. Runtime dependencies

### Jetpack Compose (BOM 2024.06.00)

- **Role:** the entire UI layer — screens, the rich-text editor, the image viewer, the drawer, the design-token system.
- **Version:** Compose BOM `2024.06.00` (`androidx.compose:compose-bom`), which pins `androidx.compose.ui:*`, `androidx.compose.material3`, `androidx.compose.material:material-icons-extended`. Compiler extension `1.5.14` (matches Kotlin 1.9.24). Compile SDK 34, min SDK 26.
- **Why over alternatives:** Compose is the only Android UI toolkit that lets us build a custom rich-text editor with per-paragraph `BasicTextField` + `VisualTransformation` + `AnnotatedString` span styling without dropping to a WebView. The old codebase used `richeditor-android` (a WebView wrapper) — Compose lets us own the rendering, keep the app off the WebView GC/runtime, and ship a non-Material look (we override `colorScheme`, `Shapes`, `Typography`). Compose Multiplatform (target future) is the smallest delta from Compose on Android.
- **Tradeoffs:** per-paragraph `BasicTextField` in a `Column` (not `LazyColumn`) composes all paragraphs — fine for diary-length docs, not infinite-scroll material. Cross-paragraph text selection is an architectural limit (documented as out of scope). Material3 is the lowest-friction slot set; we deliberately deviate from its color system.

### Material3 (`androidx.compose.material3`)

- **Role:** slot-based components (`Scaffold`, `TopAppBar`, `NavigationDrawerItem`, `Switch`, `Surface`, `DatePicker`, `AlertDialog` → wrapped as `NkDialog`), `MaterialTheme` theming infrastructure.
- **Version:** managed by the Compose BOM.
- **Why over alternatives (Material 2 / custom / 3rd-party):** M3's slot API + `MaterialTheme` is the cheapest way to swap in our own palette without forking a design system. `material-icons-extended` gives us the icon set without a separate icon library. We do NOT adopt dynamic color (it would erase the curated palette).
- **Tradeoffs:** we accept M3 defaults for component internals (ripple, motion) so we don't fight the framework; we override only `colorScheme`, `Shapes` (via `NkShapes.material`), and `Typography`.

### Navigation-Compose (`androidx.navigation:navigation-compose` 2.7.7)

- **Role:** single `NavHost` in `NkNoteApp` with typed-ish `Destination` route constants. Type-safe-ish args (`noteId: Int`, `imagePath: String` URL-encoded). `NkNoteNavigation` value object keeps screens decoupled from `NavController`.
- **Why over alternatives (custom router / compose-router / Voyager):** Navigation-Compose is the supported Android path, survives config changes, integrates with `SavedStateHandle`, and is enough for 10 routes. A custom router would be NIH; Voyager/Appyx would add a dependency for a 10-screen app.
- **Tradeoffs:** route strings are not type-safe at compile time (we mitigate with `Destination` sealed class). The drawer has to be hoisted outside `NavHost` for global access — we do that explicitly with `gesturesEnabled = false`.

### Room (`androidx.room:room-*` 2.6.1) + KSP (`com.google.devtools.ksp` 1.9.24-1.0.20)

- **Role:** persistence. `NkNoteDatabase` (version 2, `exportSchema = true`), entities `Note` / `Tag` / `NoteTag` / `NoteFts` (`@Fts4(contentEntity = Note::class)`), DAOs `NoteDao` / `TagDao` / `NoteTagDao`, `Converters` (`RichDocument ↔ String`), `MIGRATIONS` empty seam.
- **Version:** Room 2.6.1 (FTS4 + `contentEntity` supported), KSP `1.9.24-1.0.20` (matches Kotlin 1.9.24).
- **Why over alternatives:**
  - **Room over SQLDelight:** Room gives compile-time SQL verification, `@Fts4(contentEntity=…)` with auto sync triggers, `TypeConverter`, `@Transaction`, `Flow` return types, first-class KSP support. SQLDelight is great for multiplatform-first projects but lacks the FTS-contentEntity sugar and would push more hand-written SQL for no net win at this milestone. (A future Web target may use SQLDelight-js or IndexedDB behind the `NoteRepository` interface — see `docs/multiplatform.md`.)
  - **KSP over KAPT:** KAPT is slow and on the deprecation path; KSP is the supported annotation processor for Kotlin 1.9 + Room 2.6.
- **Tradeoffs:** Room is Android-first (multiplatform-room is a future path, not used here). `fallbackToDestructiveMigration` is owner-approved (no real users yet) — the `MIGRATIONS` seam exists for the *future* schema-preserving upgrade, intentionally empty today.

### Coil (`io.coil-kt:coil-compose` 2.6.0)

- **Role:** image loading for the editor inline images and the zoomable image viewer. `AsyncImage` / `SubcomposeAsyncImage` resolve the absolute file paths stored in `RichDocument.InlineImage`.
- **Version:** Coil 2.6.0 (Android-only).
- **Why over alternatives:**
  - **Coil over Glide:** Coil is Kotlin-first, coroutine-based, lighter-weight, and integrates cleanly with Compose (`AsyncImage` is a first-class Composable). Glide is older, Java-centric, requires more boilerplate to expose to Compose, and ships a larger surface. Coil also handles EXIF rotation out of the box (the reason we could delete the dead `ImageStore.decodeForDisplay`).
- **Tradeoffs:** Coil 2 is Android-only; the planned KMP migration will require Coil 3 (multiplatform). Documented in `docs/multiplatform.md` as a low-risk import-only migration when it lands.

### kotlinx.serialization (`org.jetbrains.kotlinx:kotlinx-serialization-json` 1.6.3) + `kotlin-serialization` plugin

- **Role:** serialize `RichDocument` (the rich-text schema) to JSON for Room `Note.content`. Also used by `Converters` (Room `TypeConverter`), `EditorViewModel.save/loadNote`, `HomeViewModel`, the Settings JSON export, and the draft's `DraftSelection` list.
- **Version:** 1.6.3 (with `org.jetbrains.kotlin.plugin.serialization` 1.9.24).
- **Why over alternatives (Gson / Moshi):** compile-time-generated serializers (no reflection), Kotlin-multiplatform-ready (load-bearing for the KMP future), multiplatform runtime support. The old codebase used Gson — we removed it. The `Json` instance is configured `ignoreUnknownKeys = true` + `encodeDefaults = true` everywhere so `RichDocument` evolves (new fields with defaults decode from old notes; new fields persist on save). This is THE schema-evolution policy.
- **Tradeoffs:** requires the `@Serializable` plugin and all model fields to have defaults — which we do. No reflection means no opaque runtime magic (a feature, not a bug).

### AndroidX core / appcompat / activity-compose / lifecycle

- **`androidx.core:core-ktx` 1.13.1** — Kotlin extensions + `FileProvider` (used by `ImageViewerViewModel.buildShareIntent`).
- **`androidx.appcompat:appcompat` 1.7.0** — kept for compatibility shims; minimal surface.
- **`androidx.activity:activity-compose` 1.9.0** — `ActivityResultContracts.PickVisualMedia` for the image picker (no `PictureSelector` dep), `setContent` entry point.
- **`androidx.lifecycle:lifecycle-*` 2.8.2** — `runtime-ktx`, `runtime-compose` (`collectAsStateWithLifecycle`), `viewmodel-compose` (`viewModel { }` factories). The editor's autosave hook rides `Lifecycle.Event.ON_STOP`.
- **Why:** these are the supported Android platform extensions; using them avoids bespoke equivalents that would each be a maintenance liability.
- **Tradeoffs:** appcompat is a thin shim we keep for now; a future CMP migration will slim it.

### SharedPreferences (theme-mode persistence)

- **Role:** persist the user's `ThemeMode` (SYSTEM / LIGHT / DARK). `AppContainer.themeMode: MutableStateFlow<ThemeMode>` is initialized **synchronously** from `SharedPreferences` in the constructor (which runs during `Application.onCreate`, before the first Compose tree) so there is **no theme flash on cold start**; `setThemeMode` writes back through SP and updates the flow.
- **Why over Datastore:** Datastore adds a dependency and async reads — for a single enum persisted once, SharedPreferences is synchronous, simpler, and zero-risk. The "lightweight" constraint forbids new persistence frameworks unless they pay rent.
- **Tradeoffs:** SharedPreferences is not safe for large/structured data — but we only persist one enum. A future migration to Datastore is possible but unnecessary.

### Manual DI (`AppContainer`) — no Hilt / no Koin

- **Role:** `core/AppContainer` is constructed once in `NkNoteApplication` and wires `NkNoteDatabase` / `NoteRepository` (impl: `NoteRepositoryImpl`) / `ImageStore` (impl: `AndroidImageStore`) / `SyncEngine` (impl: `NoopSyncEngine`) / `themeMode`. `AppViewModelFactory` resolves deps from the container; **no Composable resolves the container** (`appContainer()` is gone from UI).
- **Why over Hilt/Koin:** the app has 6 ViewModels and a handful of singletons — Hilt's annotation overhead, KSP compile cost, and DI-graph validation are heavier than the problem warrants. Manual DI keeps the APK smaller, the build faster, and the wiring trivially readable.
- **Tradeoffs:** wiring is hand-written; adding a new ViewModel means touching the factory. Acceptable at this scale.

## 2. Build tooling

### AGP (`com.android.application` 8.5.0) + JDK 17

- **Role:** Android Gradle Plugin. `compileSdk 34`, `minSdk 26`, `targetSdk 34`. `versionCode 2 / versionName 2.0.0`. Release build: `isMinifyEnabled = true` + `isShrinkResources = true` with `proguard-android-optimize.txt`.
- **Version:** AGP 8.5.0, JDK 17.
- **Why:** AGP 8.5 is the supported AGP for Kotlin 1.9.24 + Compose Compiler 1.5.14. JDK 17 is the LTS floor for AGP 8.

### Kotlin 1.9.24

- **Role:** language. The Compose Compiler extension `1.5.14` is the matching version for Kotlin 1.9.24.
- **Why not 2.x yet:** KMP/Compose-Compiler-Plugin migration to Kotlin 2.0 is a deliberate *future* step tied to the KMP restructure (see `docs/multiplatform.md`). Staying on 1.9.24 keeps the current milestone a pure refactor, not a compiler-plugin migration.

## 3. Test-only dependencies (NOT shipped in the APK)

The "lightweight" constraint is honored: these are `testImplementation` / `androidTestImplementation` only — they do not appear in the release APK.

- **`junit:junit` 4.13.2** — pure-JVM unit tests (`RichDocumentSerializationTest`, `EditorUndoRedoTest`, `EditorViewModelStateTest`, `ThemeModeTest`).
- **`org.robolectric:robolectric` 4.12.2** — runs Room + Android-context tests on the JVM (`NoteDaoFtsTest` in-memory Room DB, `EditorAutosaveTest` LifecycleRegistry, `ThemeModeTest` SharedPreferences). `testOptions { unitTests { isIncludeAndroidResources = true } }` is wired.
- **`androidx.room:room-testing` 2.6.1** — `RoomDatabase.Builder` + in-memory DB helpers for `NoteDaoFtsTest`. Same version as `room-runtime`.
- **`androidx.test:core` 1.6.1** + **`androidx.test.ext:junit` 1.1.5** + **`androidx.test.espresso:espresso-core` 3.5.1** — instrumented-test plumbing for the `androidTest` Compose UI tests (`NkComponentsTest`, `NavigationTest`, `EditorToolbarTest`, `EditorFindLinkCodeTest`).
- **`androidx.compose.ui:ui-test-junit4`** (BOM-managed) + **`ui-test-manifest` / `ui-tooling`** (debugImplementation) — Compose UI test framework + tooling.

## 4. Architecture decisions

### MVVM + Repository + manual DI

- **View** = Composable screen, stateless; reads from a ViewModel via `collectAsStateWithLifecycle()`.
- **ViewModel** = `StateFlow`-owning (`EditorUiState`, `HomeUiState`, `ImportViewModel.Result`, …). The editor's `TextFieldValue` editing buffer stays in `mutableStateOf`/`mutableStateListOf` for keystroke latency — this *split* (observable state in StateFlow, editing buffer in SnapshotState) is the load-bearing MVVM decision and is documented in `EditorViewModel`.
- **Model** = `NoteRepository` (interface) + `NoteRepositoryImpl`; `ImageStore` (interface) + `AndroidImageStore`; `TextImporter`; `SyncEngine` (interface) + `NoopSyncEngine`; `NoteTemplates`.
- **DI** = `AppContainer` (manual).

### Offline-first / serverless

The app is fully functional offline. `SyncEngine` is a seam; the default `NoopSyncEngine` is inert. The planned Netdisk backend uses **user-supplied** object-storage credentials over HTTP — there is no NKNote-run server. `Note.version` / `syncStatus` are retained for conflict detection (inert today; see `docs/sync-serverless.md`).

### Curated non-Material theme

`ui/theme/` defines a "warm paper + sage" palette (light + dark variants), `NkShapes` (generous radii, in-between tokens), `NkTypography`, `NkSpacing` (4-based), `NkIconSize`. **Dynamic color is disabled.** Every `RoundedCornerShape(<n>.dp)` resolves to a `NkShapes` token (the only literals remaining are inside `ui/theme/Shape.kt`). Shared components `NkTopAppBar` / `NkSettingsRow` / `NkEmptyState` / `NkList` sit on top of the tokens.

### Multiplatform seams (defer the Gradle KMP restructure)

- `ImageStore` / `NoteRepository` / `SyncEngine` are **interfaces**; `AndroidImageStore` / `NoteRepositoryImpl` / `NoopSyncEngine` are the Android actuals.
- `model/` (`RichDocument`, `Weather`, `Mood`, `NkPalette`) is pure Kotlin — zero `androidx.compose.*` imports; `Weather`/`Mood` carry only a `key`, the icon/label maps live in `ui/components/`.
- The Gradle KMP restructure (`composeApp` module, `commonMain`/`androidMain`/`desktopMain`/`wasmJsMain`) is the **next milestone** — explicitly deferred. The migration order is documented in `docs/multiplatform.md`.

## 5. Decisions log

| # | Decision | Alternatives considered | Chosen because | Tradeoff / revisit |
|---|---|---|---|---|
| D1 | Jetpack Compose for UI | richeditor-android (WebView), custom Canvas, Compose Multiplatform now | native per-paragraph `BasicTextField` + `VisualTransformation` owns the rendering without a WebView | cross-paragraph selection out of scope; Column-not-LazyColumn for the editor |
| D2 | Room 2.6.1 + KSP | SQLDelight, hand-written SQLite | compile-time SQL verification, `@Fts4(contentEntity=…)` auto-sync, `TypeConverter`, `Flow` returns | Android-first; multiplatform-room is a future path |
| D3 | Coil 2.6.0 | Glide, custom `BitmapFactory`-only | Kotlin/coroutine-first, EXIF-aware, Compose `AsyncImage` | Android-only → migrate to Coil 3 when going CMP |
| D4 | kotlinx.serialization 1.6.3 | Gson, Moshi | compile-time serializers, KMP-ready, `ignoreUnknownKeys + encodeDefaults` is the schema-evolution policy | requires `@Serializable` + defaults on every field (we comply) |
| D5 | Navigation-Compose 2.7.7 | custom router, Voyager, Appyx | supported Android path, `SavedStateHandle` integration, enough for 10 routes | routes are string-typed (mitigated by `Destination` sealed class) |
| D6 | Manual DI (`AppContainer`) | Hilt, Koin | 6 ViewModels + a handful of singletons — annotation/graph overhead is heavier than the problem | wiring is hand-written; new VM touches the factory |
| D7 | SharedPreferences for theme mode | Datastore | single enum, synchronous read = no theme flash, zero new deps | not safe for large/structured data (we don't have any) |
| D8 | Native editor (no richeditor-android) | richeditor-android (WebView) | owns rendering, off WebView GC, ships non-Material look | per-paragraph selection limit (documented out of scope) |
| D9 | Native image picker + compressor (`PickVisualMedia` + `Bitmap.compress`) | PictureSelector, Luban | platform APIs only, no 3rd-party dep | cross-note dedup forgone (simpler than refcounting) |
| D10 | Native `FileProvider` share | 3rd-party share sheet | `Intent.ACTION_SEND` + `FileProvider` is the supported path, no permission prompt | degrades to a toast if the provider isn't registered (handled) |
| D11 | MVVM split: StateFlow + SnapshotState buffer | all-StateFlow editor | StateFlow conflation under rapid typing risks losing intermediate `TextFieldValue` edits | split is documented and load-bearing — do not collapse |
| D12 | FTS4 `contentEntity` for search | manual FTS sync, LIKE scans | Room auto-generates triggers; `searchText` denormalization avoids scanning JSON | empty query guarded (`MATCH ''` would throw) |
| D13 | Image cleanup only on permanent-delete + empty-trash | clean on soft-delete | restore must bring images back | `emptyTrash` reads ids *before* deleting rows |
| D14 | Serverless sync seam (`SyncEngine`) | run our own server, Firebase | user owns storage (Netdisk credentials) or P2P (LAN); lightweight constraint | `version`/`syncStatus` inert today |
| D15 | Curated non-Material theme; dynamic color off | Material You defaults | the "warm paper + sage" palette is the identity | manually maintain light + dark variants |
| D16 | Multiplatform seams now, KMP Gradle restructure later | full KMP now, no KMP ever | ship the Android milestone; keep the KMP door open without risking it | absolute image paths are a known fragility until the restructure |
| D17 | KSP over KAPT | KAPT | KAPT is slow and deprecated; KSP is supported for Kotlin 1.9 + Room 2.6 | — |
| D18 | Kotlin 1.9.24 + Compose Compiler 1.5.14 | Kotlin 2.0 + new Compose Compiler plugin | defer the compiler-plugin migration to the KMP milestone | revisit at KMP restructure |

---

## 面试向 中文摘要 (Interview-oriented Chinese summary)

**项目定位**：NKNote 是一个轻量、无服务器、离线优先的 Android 日记应用，使用 Kotlin + Jetpack Compose，刻意避开 Material You 默认外观，采用「暖纸 + 鼠尾草绿」自研配色。

**依赖选型与对比**（一句话版）：

- **Jetpack Compose（BOM 2024.06）**：原生 `BasicTextField` + `VisualTransformation` 自研富文本编辑器，替换掉旧版 `richeditor-android`（WebView）。代价：跨段落选择是已知架构限制（不在范围内）。
- **Room 2.6.1 + KSP**：编译期 SQL 校验、`@Fts4(contentEntity=Note::class)` 自动同步触发器、`TypeConverter`、`Flow` 返回值。选 Room 不选 SQLDelight，是因为 FTS contentEntity 这类糖和 KSP 一等支持对当前里程碑净收益更高；Web 端未来可在 `NoteRepository` 接口背后换 SQLDelight-js / IndexedDB。
- **Coil 2.6.0**：Kotlin 协程优先、Compose 一等 `AsyncImage`、自带 EXIF 旋转处理（这是我们能删除 `decodeForDisplay` 死代码的原因）。选 Coil 不选 Glide：更轻、更现代。代价：Coil 2 仅 Android，未来 KMP 化需迁移到 Coil 3（仅改 import，低风险）。
- **kotlinx.serialization 1.6.3**：编译期生成序列化器、KMP 友好。`ignoreUnknownKeys = true` + `encodeDefaults = true` 是整个 `RichDocument` 模型演进策略——新增字段都带默认值，旧笔记可解码、新字段会持久化。替换掉旧版的 Gson。
- **Navigation-Compose 2.7.7**：10 个路由够用，配合 `SavedStateHandle`。用 `Destination` sealed class 缓解「路由字符串非类型安全」的代价。
- **手动 DI（`AppContainer`）**：6 个 ViewModel + 几个单例，Hilt/Koin 的注解开销比问题本身还重。代价：新增 ViewModel 要手改 factory。
- **SharedPreferences（主题模式持久化）**：只存一个 `ThemeMode` 枚举，**同步读取**保证冷启动无主题闪烁。不选 Datastore，是因为 Datastore 引入新依赖 + 异步读取，对一个枚举是杀鸡用牛刀。
- **原生编辑器/选择器/压缩器/分享**：全部走平台 API（`PickVisualMedia`、`Bitmap.compress(WEBP, 75)`、Material3 `DatePicker`、`FileProvider + ACTION_SEND`），全部删除了第三方库（PictureSelector/Luban/FileOperator 等）。
- **测试依赖（不进 APK）**：JUnit4 + Robolectric 4.12.2 + room-testing + coroutines-test 跑在 JVM 上；Compose UI 测试跑在 `androidTest`。`testImplementation` 不进 release 包，符合「轻量」约束。

**架构决策**：

- **MVVM**：可观察状态走 `StateFlow`（`EditorUiState` 等），但编辑器实时键入缓冲（`TextFieldValue` 列表）刻意留在 `mutableStateOf` / `mutableStateListOf`——StateFlow 的合并策略在快速打字下会丢中间编辑。这个「分叉」是编辑器 MVVM 的承重决策，不能合并。
- **离线优先 / 无服务器**：`SyncEngine` 是接缝，`NoopSyncEngine` 是默认。计划的 Netdisk 后端使用**用户自有**的对象存储凭证走 HTTP——NKNote 不运行任何服务器。`Note.version` / `syncStatus` 留作冲突检测（当前惰性）。
- **自研非 Material 主题**：`NkShapes` / `NkSpacing` / `NkIconSize` 设计令牌，所有 `RoundedCornerShape(<n>.dp)` 都解析到令牌（除 `ui/theme/Shape.kt` 内部）。动态颜色关闭。
- **多平台接缝**：`ImageStore` / `NoteRepository` / `SyncEngine` 都是接口；`model/` 纯 Kotlin 无 Compose 依赖；`Weather`/`Mood` 只存 `key`，图标和本地化标签映射放在 UI 层。Gradle KMP 重构是**下一里程碑**（刻意推迟）。

**关键承重决策（面试可重点展开）**：

1. 编辑器「StateFlow 可观察状态 + SnapshotState 键入缓冲」分叉
2. Room FTS4 `contentEntity` 自动同步触发器 + `searchText` 反规范化
3. 软删除保留图片、仅永久删除/清空回收站时清理磁盘
4. SharedPreferences 同步读取避免冷启动主题闪烁
5. 多平台接缝先行、KMP Gradle 重构推迟——以最低风险保住多平台门路
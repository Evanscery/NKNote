# NKNote — Serverless Sync Design

> Status: **design document**. The seam is shipped and inert (`NoopSyncEngine`); no real backend is implemented.

NKNote is **serverless by design**. There is no NKNote-run server. The app is fully functional offline; sync is an opt-in seam the *user* owns — either by supplying their own object-storage credentials (Netdisk) or by direct peer-to-peer on the local network (LAN). This keeps the "lightweight" constraint honest (no backend to operate, no account system, no auth server) and the "user-owns-their-data" promise literal.

## The `SyncEngine` seam

```kotlin
interface SyncEngine {
    val isConfigured: Boolean
    suspend fun push()
    suspend fun pull()
}

object NoopSyncEngine : SyncEngine {
    override val isConfigured: Boolean = false
    override suspend fun push() { /* local-only by default */ }
    override suspend fun pull() { /* local-only by default */ }
}
```

`core/AppContainer.syncEngine` wires `NoopSyncEngine` by default. The seam is intentionally narrow (`isConfigured` + `push()` + `pull()`) so a real backend can land without touching the UI, the repository, or the data model. The repository calls `syncEngine.push()` after a write (today, that's a no-op); the UI never imports `SyncEngine`.

## Planned backends

### Netdisk (user-supplied object storage, HTTP)

- **User-supplied credentials.** The user provides their own object-storage / netdisk API credentials (endpoint, bucket, access key, secret) in Settings. NKNote stores only those credentials locally — it does not proxy them through any NKNote-run service.
- **Transport.** Plain HTTP(S) — `PUT`/`GET` objects. Notes serialize as the same `RichDocument` JSON Room already stores; images are the already-compressed WebP files. A real backend could use `okhttp` / `ktor-client`; no transport dependency is shipped today.
- **No NKNote server.** This is the load-bearing constraint: the project operates nothing. The user's bucket is the source of truth; multiple devices pointing at the same bucket get sync for free.
- **Conflict detection** (below) is the only NKNote-side logic; conflict *resolution* is intentionally deferred to a future design pass.

### LAN (peer-to-peer, NSD / mDNS)

- **Discovery.** `android.net.nsd.NsdManager` (Android's NSD wrapper over mDNS) advertises and discovers NKNote peers on the local network.
- **Transport.** A direct socket / HTTP-over-LAN hop between two devices — no relay, no cloud. Suitable for "laptop ↔ phone on the same Wi-Fi" sync.
- **Use case.** A user who doesn't want any cloud account at all. Pair-once, sync-on-LAN.

## Conflict detection: `version` / `syncStatus`

The schema retains two inert-but-purposeful columns on `Note`:

```kotlin
val version: Int = 1,
val syncStatus: Int = 0   // 0 local-only, 1 pending, 2 synced
```

- `version` is a per-note monotonic counter the repository bumps on every local mutation. When a backend is wired, a `pull` that fetches a remote `version` older than the local one means "we have newer local edits"; a remote `version` newer than local means "remote has edits we haven't seen"; equal versions mean "in sync".
- `syncStatus` is a small per-note state machine: `0` = local-only (never pushed), `1` = pending (locally changed since last push), `2` = synced (matches the remote head). A successful `push` flips `1 → 2`; a local edit flips `2 → 1`.

Today both columns are written but **never read for sync** — the `NoopSyncEngine` doesn't push or pull. They exist *now* so that:

1. A future backend can read them without a schema migration (they already ship in `Note`).
2. The destructive-rebuild migration policy doesn't drop them — they're part of the schema contract.
3. Any future local-optimization (e.g. "only push notes with `syncStatus = 1`") has the column ready.

**Conflict resolution is out of scope for this milestone.** The seam only detects; resolution (last-write-wins, three-way merge, prompt-the-user) is a deliberate future design pass — `RichDocument` is JSON and paragraph-structured, so a three-way merge at the paragraph level is feasible when the time comes, but the policy decision is not made here.

## Why serverless fits a lightweight diary

1. **No account system.** A diary is personal; forcing an account is friction. Serverless = the user brings their own storage or syncs peer-to-peer.
2. **No backend to operate.** A personal/educational project can't run a 24/7 server sustainably. "No NKNote-run server" is a maintenance promise, not just a privacy one.
3. **Offline-first is the natural shape of a diary.** You write when you write; sync happens when it happens. The app must be fully usable offline forever — and it is.
4. **The seam is small enough to stay inert.** Three-method interface (`isConfigured` / `push` / `pull`). The default `NoopSyncEngine` is the proof that the seam doesn't bleed into the rest of the app: zero UI surfaces, zero repository branches, zero schema churn when a real backend lands.
5. **Conflict columns ship in the schema today.** `version` / `syncStatus` are inert-but-retained, so a future backend reads them without a migration. The destructive rebuild (owner-approved, no real users yet) keeps them in the contract.

## What is NOT shipped

- No Netdisk implementation. The user-facing Settings UI for credentials is future work.
- No LAN implementation. NSD wiring is future work.
- No conflict-resolution UI. Detection columns exist; the policy does not.
- No transport dependency in the APK. `okhttp` / `ktor-client` will be added *when* a real backend lands, not before.

## Reference

- Seam source: [`app/src/main/java/io/github/nknote/data/sync/SyncEngine.kt`](../app/src/main/java/io/github/nknote/data/sync/SyncEngine.kt)
- Default impl: `NoopSyncEngine` (same file)
- Schema columns: `Note.version`, `Note.syncStatus` in [`data/entity/Note.kt`](../app/src/main/java/io/github/nknote/data/entity/Note.kt)
- DI wiring: [`core/AppContainer.kt`](../app/src/main/java/io/github/nknote/core/AppContainer.kt) (`syncEngine by lazy { NoopSyncEngine }`)
- Multiplatform notes: [`docs/multiplatform.md`](multiplatform.md) (per-target `SyncEngine` actuals)
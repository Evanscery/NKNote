package io.github.nknote.data.sync

/**
 * Serverless sync abstraction. Concrete implementations are user-supplied and never bundled:
 *
 *  - [NoopSyncEngine]: default, local-only. Inert.
 *  - NetdiskSyncEngine (future): user provides their own netdisk (object-storage) API key;
 *    pushes/pulls notes + compressed images over HTTP. Serverless: no NKNote-run server.
 *  - LanSyncEngine (future): peer-to-peer on local network (e.g. mDNS / NSD).
 *
 * The seam is intentionally narrow so a real backend can be added without touching UI or repo.
 * The Note.version / syncStatus columns support conflict detection once a backend is wired.
 */
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
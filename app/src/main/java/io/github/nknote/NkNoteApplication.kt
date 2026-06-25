package io.github.nknote

import android.app.Application
import io.github.nknote.core.AppContainer

class NkNoteApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
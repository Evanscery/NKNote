package com.example.nknote

import ando.file.BuildConfig
import ando.file.core.FileOperator
import android.app.Application
import com.example.nknote.data.AppContainer
import com.example.nknote.data.AppDataContainer

class NKNoteApplication : Application() {
    /**
     * AppContainer instance used by the rest of classes to obtain dependencies
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        FileOperator.init(this, BuildConfig.DEBUG)
        super.onCreate()
        container = AppDataContainer(this)
    }
}
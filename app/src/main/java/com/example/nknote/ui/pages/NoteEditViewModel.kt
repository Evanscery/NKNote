package com.example.nknote.ui.pages

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nknote.data.entities.Note
import com.example.nknote.data.entities.Tag
import com.example.nknote.data.repository.NoteRepository
import com.example.nknote.data.DataHandler.stringWithMD5
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.engine.CompressFileEngine
import jp.wasabeef.richeditor.RichEditor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.zibin.luban.Luban
import top.zibin.luban.OnCompressListener
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.Type
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date

class NoteEditViewModel(
    private val noteRepository: NoteRepository,
    private val noteId: Int? = null
) : ViewModel() {
    var noteUiState by mutableStateOf(NoteUiState())
        private set
    
    // 编辑器相关状态
    var hasInitialEditor by mutableStateOf(false)
        private set
    private var _fontSize by mutableStateOf(3)
    val fontSize: Int get() = _fontSize
    var editorRef: RichEditor? = null
        private set
    
    // 笔记详情相关状态
    var description by mutableStateOf("")
        private set
    var tags by mutableStateOf("")
        private set
    var weather by mutableStateOf("")
        private set
    var date by mutableStateOf("")
        private set
    var isDetailsExpanded by mutableStateOf(false)
        private set
    var title by mutableStateOf("")
        private set
    
    // 图片相关状态
    var insertedImageUri by mutableStateOf("")
        private set
    val insertedImagesMap: SnapshotStateMap<String, String> = mutableStateMapOf()
    
    // 对话框状态
    var showColorPickerDialog by mutableStateOf(false)
        private set
    var showFontSizeDialog by mutableStateOf(false)
        private set

    private val _availableTags = MutableStateFlow<List<Tag>>(emptyList())
    val availableTags: StateFlow<List<Tag>> = _availableTags.asStateFlow()

    init {
        viewModelScope.launch {
            noteRepository.getAllTags().collect { tagList ->
                _availableTags.value = tagList
            }
        }

        if (noteId != null) {
            viewModelScope.launch {
                noteRepository.getNoteById(noteId).collect { note ->
                    note?.let {
                        title = it.title
                        description = it.description
                        date = it.date
                        weather = it.weather
                        editorRef?.html = it.content
                    }
                }
            }
        }
    }
    
    // 编辑器相关方法
    fun setEditorRef(editor: RichEditor) {
        editorRef = editor
    }
    
    fun initializeEditor(defaultFontSize: Int, defaultFontColor: Int, defaultBackgroundColor: Int) {
        if (!hasInitialEditor) {
            hasInitialEditor = true
            editorRef?.apply {
                setEditorFontSize(defaultFontSize)
                setEditorFontColor(defaultFontColor)
                setEditorBackgroundColor(defaultBackgroundColor)
                setInputEnabled(true)
                settings.javaScriptEnabled = true
            }
        }
    }
    
    fun setFontSize(size: Int) {
        _fontSize = size
        editorRef?.setFontSize(size)
    }
    
    fun setTextColor(color: Int) {
        editorRef?.setTextColor(color)
        editorRef?.clearFocus()
    }
    
    // 笔记详情相关方法
    fun updateDescription(newDescription: String) {
        description = newDescription
    }
    
    fun updateTags(newTags: String) {
        tags = newTags
    }
    
    fun updateWeather(newWeather: String) {
        weather = newWeather
    }
    
    fun updateDate(newDate: String) {
        date = newDate
    }
    
    fun updateTitle(newTitle: String) {
        title = newTitle
    }
    
    fun toggleDetailsExpanded() {
        isDetailsExpanded = !isDetailsExpanded
    }
    
    // 对话框相关方法
    fun showColorPicker() {
        showColorPickerDialog = true
    }
    
    fun hideColorPicker() {
        showColorPickerDialog = false
    }
    
    fun showFontSizeDialog() {
        showFontSizeDialog = true
    }
    
    fun hideFontSizeDialog() {
        showFontSizeDialog = false
    }
    
    // 保存笔记
    suspend fun saveNote() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val currentDate = sdf.format(Date())
        
        val note = Note(
            id = noteId ?: 0,
            title = title,
            description = description,
            content = editorRef?.html ?: "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            date = date.ifEmpty { currentDate },
            weather = weather,
            coverImageId = null,
            syncStatus = 0,
            version = 1
        )
        
        noteRepository.insertNote(note)
        insertedImagesMap.clear()
    }
    
    // 图片相关方法
    fun handleImageSelection(context: Context, result: ActivityResult) {
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selectList = PictureSelector.obtainSelectorList(result.data)
            if (selectList.isNotEmpty()) {
                insertedImageUri = selectList[0].path
                processImage(context)
            }
        }
    }
    
    private fun processImage(context: Context) {
        viewModelScope.launch {
            try {
                val imageFile = File(insertedImageUri)
                if (imageFile.exists()) {
                    insertedImagesMap[insertedImageUri] = ""
                } else {
                    Log.e("NoteEditViewModel", "图片处理失败")
                }
            } catch (e: Exception) {
                Log.e("NoteEditViewModel", "图片处理失败: ${e.message}")
            }
        }
    }
    
    fun deleteImage(imageUrl: String) {
        insertedImagesMap.remove(imageUrl)
        val file = File(imageUrl)
        if (file.exists()) {
            file.delete()
        }
    }
    
    fun getImageCompressEngine(context: Context): CompressFileEngine {
        return CompressFileEngine { context, source, call ->
            Luban.with(context)
                .load(source)
                .ignoreBy(100)
                .setTargetDir(context.getExternalFilesDir("diary_pics")?.absolutePath)
                .filter { path ->
                    !(path.lowercase().endsWith(".gif"))
                }
                .setCompressListener(object : OnCompressListener {
                    override fun onStart() {}
                    override fun onSuccess(index: Int, compressFile: File) {
                        call?.onCallback(source[0].toString(), compressFile.absolutePath)
                    }
                    override fun onError(index: Int, throwable: Throwable?) {
                        call?.onCallback(source[0].toString(), "")
                    }
                })
                .launch()
        }
    }
}

data class NoteUiState(
    val noteDetails: NoteDetails = NoteDetails()
)

data class NoteDetails(
    val id: Int = 0,
    val title: String = "无标题",
    val description: String = "这篇笔记没有描述",
    val content: String = "",
    val date: String = "",
    val weather: String = "",
    val coverImageId: String? = null,
    val selectedTagIds: List<String> = emptyList()
)


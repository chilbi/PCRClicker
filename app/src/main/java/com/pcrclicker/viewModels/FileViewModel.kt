package com.pcrclicker.viewModels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class FileViewModel(
    private val application: Application
) : ViewModel() {
    private val _recentFiles = MutableStateFlow(loadRecentFiles())
    val recentFiles = _recentFiles.asStateFlow()

    // 应用专属存储目录
    fun getStorageDir(): File {
        return application.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: application.filesDir
    }
    // 保存文件
    suspend fun saveFile(fileName: String, text: String) {
        try {
            val file = File(getStorageDir(), fileName)
            withContext(Dispatchers.IO) {
                file.writeText(text)
            }
            addToRecentFiles(file)
            Toast.makeText(application, "文件保存成功: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(application, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 打开文件
    suspend fun openFile(fileName: String): String? {
        try {
            val file = File(getStorageDir(), fileName)
            val content = withContext(Dispatchers.IO) {
                file.readText()
            }
            addToRecentFiles(file)
            Toast.makeText(application, "文件加载成功", Toast.LENGTH_SHORT).show()
            return content
        } catch (e: Exception) {
            Toast.makeText(application, "读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    // 添加到最近文件列表
    fun addToRecentFiles(file: File) {
        val prefs = application.getSharedPreferences("recent_files", Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet("file_paths", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(file.absolutePath)
        prefs.edit { putStringSet("file_paths", currentSet) }
        _recentFiles.value = loadRecentFiles()
    }

    // 从最近文件列表移除
    fun removeFromRecentFiles(file: File) {
        val prefs = application.getSharedPreferences("recent_files", Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet("file_paths", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        currentSet.remove(file.absolutePath)
        prefs.edit { putStringSet("file_paths", currentSet) }
        _recentFiles.value = loadRecentFiles()
    }

    // 文件分享
    fun shareFile(context: Context, file: File) {
        try {
            // 使用 FileProvider 获取 URI
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider", // 必须与 manifest 中的 authorities 一致
                file
            )
            // 创建分享 Intent
            val intent = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // 启动分享对话框
            context.startActivity(Intent.createChooser(intent, "分享文件"))
        } catch (e: Exception) {
            Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // 加载最近文件列表
    private fun loadRecentFiles(): List<File> {
        val prefs = application.getSharedPreferences("recent_files", Context.MODE_PRIVATE)
        val filePaths = prefs.getStringSet("file_paths", emptySet()) ?: emptySet()
        return filePaths.mapNotNull { path ->
            try {
                File(path).takeIf { it.exists() }
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.lastModified() } // 按修改时间倒序排列
    }
}

class FileViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileViewModel::class.java)) {
            return FileViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

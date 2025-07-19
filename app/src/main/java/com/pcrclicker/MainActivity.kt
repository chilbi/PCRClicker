package com.pcrclicker

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.pcrclicker.ui.App
import com.pcrclicker.ui.theme.AppTheme
import com.pcrclicker.viewModels.ServicesViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    private val servicesViewModel: ServicesViewModel by viewModels()

    private val accessibilitySettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        servicesViewModel.checkAccessibilityStatus(this)
    }

    @SuppressLint("ObsoleteSdkInt")
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        servicesViewModel.checkOverlayStatus(this)
        servicesViewModel.startFloatingWindowService(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        servicesViewModel.checkAccessibilityStatus(this)
        servicesViewModel.checkOverlayStatus(this)
        servicesViewModel.startFloatingWindowService(this)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val fileNameAndText = handleIntent(intent)
        setContent {
            AppTheme {
                App(fileNameAndText)
            }
        }
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        accessibilitySettingsLauncher.launch(intent)
    }

    fun openOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${packageName}".toUri())
        overlayPermissionLauncher.launch(intent)
    }

    private fun handleIntent(intent: Intent): Pair<String, String>? {
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                val fileName = getFileNameFromUri(uri)
                val fileText = readTextFromUri(uri)
                if (fileName != null && fileText != null) {
                    return fileName to fileText
                }
            }
        }
        return null
    }

    private fun readTextFromUri(uri: Uri): String? {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                // 处理 content:// URI（如从文件选择器、Google Drive 等获取的文件）
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
            ContentResolver.SCHEME_FILE -> {
                // 处理 file:// URI（直接本地文件路径）
                name = uri.lastPathSegment
            }
        }
        return name
    }
}

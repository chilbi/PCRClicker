package com.pcrclicker.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pcrclicker.viewModels.FileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TextFile(
    fileViewModel: FileViewModel,
    navigateToEditor: (fileName: String) -> Unit
) {
    val recentFiles = fileViewModel.recentFiles.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 应用专属存储目录
    val storageDir = remember { fileViewModel.getStorageDir() }
    // 初始化加载最近文件列表
    LaunchedEffect(Unit) {
        val file = File(storageDir, "test.txt")
        if (!file.exists()) {
            val text = "5=晶 4=白望 3=克 2=狼 1=511\n" +
                    "122 克 晶(121) 狼 511\n" +
                    "白望(115) auto克(113)\n" +
                    "111 Boss大招 克(110) 狼(104) 克(100)\n" +
                    "BossUB auto白望(56)\n" +
                    "50 晶押晶破甲 狼 克(49)\n" +
                    "测试轴用后面不打了 menu(2)"
            fileViewModel.saveFile("test.txt", text)
        }
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除文件 ${fileToDelete?.name} 吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        fileToDelete?.let { file ->
                            scope.launch {
                                val success = withContext(Dispatchers.IO) {
                                    if (file.exists()) file.delete() else false
                                }
                                if (success) {
                                    fileViewModel.removeFromRecentFiles(file)
                                    Toast.makeText(context, "文件已删除", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text("取消")
                }
            }
        )
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            item("storageDir.absolutePath") {
                Text(
                    "存储位置: ${storageDir.absolutePath}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            items(recentFiles.value, { it.name }) { file ->
                RecentFileItem(
                    file = file,
                    navigateToEditor = navigateToEditor,
                    onShareFile = { file ->
                        fileViewModel.shareFile(context, file)
                    },
                    onDeleteFile = { file ->
                        fileToDelete = file
                        showDeleteConfirm = true
                    }
                )
            }

            item("createFileInput") {
                CreateFileInput(
                    onFileCreate = { fileName ->
                        val file = File(storageDir, fileName)
                        if (file.exists()) {
                            Toast.makeText(context, "文件已存在", Toast.LENGTH_SHORT).show()
                        } else {
                            scope.launch {
                                fileViewModel.saveFile(fileName, "")
                                navigateToEditor(fileName)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RecentFileItem(
    file: File,
    navigateToEditor: (fileName: String) -> Unit,
    onShareFile: (file: File) -> Unit,
    onDeleteFile: (file: File) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navigateToEditor(file.name) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.getDefault()
                    ).format(Date(file.lastModified())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    onShareFile(file)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "分享文件",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    onDeleteFile(file)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除文件",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CreateFileInput(
    onFileCreate: (fileName: String) -> Unit
) {
    val context = LocalContext.current
    var fileName by remember { mutableStateOf("") }
    val suffix = ".txt"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = fileName,
            onValueChange = { input ->
                fileName = if (input.endsWith(suffix)) {
                    input.substringBeforeLast(suffix)
                } else {
                    input
                }
            },
            modifier = Modifier.weight(1f),
            placeholder = { Text("输入文件名") },
            trailingIcon = {
                Text(
                    text = suffix,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = {
                if (fileName.isNotBlank()) {
                    onFileCreate("$fileName$suffix")
                } else {
                    Toast.makeText(context, "请输入文件名", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text("新建")
        }
    }
}

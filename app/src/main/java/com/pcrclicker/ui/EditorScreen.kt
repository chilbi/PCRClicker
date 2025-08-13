package com.pcrclicker.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pcrclicker.FloatingWindowService
import com.pcrclicker.MainActivity
import com.pcrclicker.ParsedScript
import com.pcrclicker.Position
import com.pcrclicker.Settings
import com.pcrclicker.SyntaxException
import com.pcrclicker.analyzeSyntax
import com.pcrclicker.ui.components.BackTitle
import com.pcrclicker.ui.components.ConfirmationDialog
import com.pcrclicker.viewModels.FileViewModel
import com.pcrclicker.viewModels.ServicesViewModel
import com.pcrclicker.viewModels.SettingsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    fileName: String,
    fileViewModel: FileViewModel,
    servicesViewModel: ServicesViewModel,
    settingsViewModel: SettingsViewModel,
    save: (fileName: String, content: String) -> Unit,
    onBack: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    var text by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf(text.lines()) }
    var parsedScript by remember { mutableStateOf<ParsedScript?>(null) }
    var syntaxException by remember { mutableStateOf<SyntaxException?>(null) }
    var analysisJob by remember { mutableStateOf<Job?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var modified by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val content = fileViewModel.openFile(fileName)
            if (content != null) {
                text = content
                lines = content.lines()
                isAnalyzing = true
                parsedScript = analyzeSyntax(lines)
                syntaxException = null
            }
        } catch (e: SyntaxException) {
            syntaxException = e
        } finally {
            isAnalyzing = false
        }
    }

    BackHandler(modified) {
        showDialog = true
    }

    if (showDialog) {
        ConfirmationDialog(
            title = "确定保存",
            message = "文件已修改过，确定保存再退出？",
            onConfirm = {
                showDialog = false
                scope.launch {
                    fileViewModel.saveFile(fileName, text)
                    modified = false
                }
                onBack()
            },
            onDismiss = {
                showDialog = false
                onBack()
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            BackTitle(
                title = if (modified) "$fileName*" else fileName,
                onBack = {
                    if (modified) {
                        showDialog = true
                    } else {
                        onBack()
                    }
                }
            ) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {
                        scope.launch {
                            fileViewModel.saveFile(fileName, text)
                            modified = false
                        }
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "保存",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Text("保存")
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LineNumColumn(lines.size, syntaxException, scrollState)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .weight(1f)
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { newText ->
                            text = newText
                            lines = newText.lines()
                            modified = true

                            analysisJob?.cancel()
                            analysisJob = scope.launch {
                                try {
                                    isAnalyzing = true
                                    delay(2000)
                                    parsedScript = analyzeSyntax(lines)
                                    syntaxException = null
                                } catch (e: SyntaxException) {
                                    parsedScript = null
                                    syntaxException = e
                                } finally {
                                    isAnalyzing = false
                                }
                            }
                        },
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .horizontalScroll(rememberScrollState())
                            .fillMaxSize()
                            .padding(8.dp),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 24.sp
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        keyboardActions = KeyboardActions(onDone = { /* 处理完成操作 */ })
                    )
                }
            }

            EditHelp()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .verticalScroll(rememberScrollState())
                .weight(1f)
        ) {
            ParsedResultPanel(
                isAnalyzing,
                syntaxException,
                parsedScript
            )
            if (parsedScript != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        OperatorPanel(parsedScript!!, settings, save = { content -> save(fileName, content) })
                    }
                }

                EnableFloatingTool(servicesViewModel, parsedScript!!, settings, save = { content -> save(fileName, content) })
            }
        }
    }
}

@Composable
private fun EditHelp() {
    var open by remember { mutableStateOf(false) }
    Surface(tonalElevation = 8.dp) {
        TextButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("轴写法规则")
            Icon(
                imageVector = Icons.AutoMirrored.Default.Help,
                contentDescription = "帮助"
            )
        }
        if (open) {
            AlertDialog(
                onDismissRequest = { open = false },
                confirmButton = {
                    Button(onClick = { open = false }) {
                        Text("确认")
                    }
                },
                title = {
                    Text("轴写法规则")
                },
                text = {
                    Text(
                        text = "使用空格分隔\n" +
                                "第1行必须定义队伍，例：1=佩可 2=凯露 3=可可萝\n" +
                                "其余行定于轴\n" +
                                "轴时间定义可以在行首定义，也可以在角色名后面括号定义，例：120 佩可 凯露 可可萝(115)\n" +
                                "auto开ub在角色名前加auto，例：auto凯露 auto可可萝(102)\n" +
                                "手动目押和boss大招定义，例：佩可押唱名(55) boss大招(50)\n" +
                                "可在轴末尾定义游戏菜单按钮防滑刀，例：menu(2)\n" +
                                "可录制自动打轴，录制好的轴会另存为新文件，例：record跳秒125\n" +
                                "一些有特殊用处的关键字符：auto menu record start stop BUB BossUB Boss大招 () []",
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
            )
        }
    }
}

@Composable
private fun EnableFloatingTool(
    servicesViewModel: ServicesViewModel,
    parsedScript: ParsedScript,
    settings: Settings,
    save: (content: String) -> Unit
) {
    val activity = LocalActivity.current as MainActivity
    val isEnabledTool = remember { mutableStateOf(FloatingWindowService.isEnabledTool()) }
    val requireOverlayPermission = remember { mutableStateOf(false) }
    val requireAccessibilityPermission = remember { mutableStateOf(false) }
    val overlayEnabled by servicesViewModel.overlayEnabled.collectAsState()
    val accessibilityEnabled by servicesViewModel.accessibilityEnabled.collectAsState()
    val floatingWindowServiceConnected by servicesViewModel.floatingWindowServiceConnected.collectAsState()

    TextButton(
        onClick = {
            if (!overlayEnabled || !floatingWindowServiceConnected) {
                requireOverlayPermission.value = true
            } else if (!accessibilityEnabled) {
                requireAccessibilityPermission.value = true
            } else if (isEnabledTool.value) {
                FloatingWindowService.getInstance()?.removeFloatingWindow(
                    FloatingWindowService.TOOL_ID
                )
                isEnabledTool.value = false
            } else {
                FloatingWindowService.getInstance()?.addFloatingWindow(
                    FloatingWindowService.TOOL_ID,
                    Position(200, 100),
                    true,
                    null
                ) {
                    OperatorPanel(parsedScript, settings, save)
                }
                isEnabledTool.value = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = if (isEnabledTool.value) Icons.Default.Close else Icons.Default.Rocket,
            contentDescription = null
        )
        Text("${if (isEnabledTool.value) "关闭" else "打开"}悬浮打轴工具")
    }

    EnableCompensationSecFloatingTool(
        enabled = !isEnabledTool.value,
        onEnable = { sec ->
            if (!overlayEnabled || !floatingWindowServiceConnected) {
                requireOverlayPermission.value = true
            } else if (!accessibilityEnabled) {
                requireAccessibilityPermission.value = true
            } else {
                FloatingWindowService.getInstance()?.addFloatingWindow(
                    FloatingWindowService.TOOL_ID,
                    Position(200, 100),
                    true,
                    null
                ) {
                    OperatorPanel(parsedScript.toCompensation(sec), settings, save)
                }
                isEnabledTool.value = true
            }
        }
    )

    if (requireOverlayPermission.value) {
        ConfirmationDialog(
            title = "需要悬浮窗权限",
            message = "确认去打开悬浮窗权限？",
            onConfirm = {
                requireOverlayPermission.value = false
                activity.openOverlayPermission()
            },
            onDismiss = {
                requireOverlayPermission.value = false
            }
        )
    }
    if (requireAccessibilityPermission.value) {
        ConfirmationDialog(
            title = "需要无障碍权限",
            message = "确认去打开无障碍权限？",
            onConfirm = {
                requireAccessibilityPermission.value = false
                activity.openAccessibilitySettings()
            },
            onDismiss = {
                requireAccessibilityPermission.value = false
            }
        )
    }
}

@Composable
private fun EnableCompensationSecFloatingTool(
    enabled: Boolean,
    onEnable: (sec: Int) -> Unit
) {
    val context = LocalContext.current
    var compensationSec by remember { mutableStateOf("") }
    val suffix = "秒"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp, 0.dp, 8.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = compensationSec,
            onValueChange = { input ->
                compensationSec = if (input.endsWith(suffix)) {
                    input.substringBeforeLast(suffix)
                } else {
                    input
                }
            },
            modifier = Modifier.weight(1f),
            placeholder = { Text("输入补偿秒数") },
            trailingIcon = {
                Text(
                    text = suffix,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(
            onClick = {
                if (compensationSec.isNotBlank()) {
                    val sec = compensationSec.toIntOrNull()
                    if (sec != null) {
                        if (sec > 19 && sec < 90) {
                            onEnable(sec)
                        } else {
                            Toast.makeText(context, "补偿秒数应在20-89之间", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "补偿秒数应在20-89之间", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "请输入补偿秒数", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = enabled
        ) {
            Icon(imageVector = Icons.Default.Rocket, contentDescription = null)
            Text("以补偿轴打开")
        }
    }
}

@Composable
private fun LineNumColumn(
    linesSize: Int,
    syntaxException: SyntaxException?,
    scrollState: ScrollState
) {
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .width(40.dp)
            .fillMaxHeight()
            .padding(vertical = 8.dp)
    ) {
        repeat(linesSize) { index ->
            val lineNum = index + 1
            val isErrorLine =
                syntaxException != null && syntaxException.lineNum == lineNum

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        if (isErrorLine) MaterialTheme.colorScheme.errorContainer.copy(
                            alpha = 0.2f
                        )
                        else Color.Transparent
                    )
            ) {
                Text(
                    text = lineNum.toString(),
                    color = if (isErrorLine) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ParsedResultPanel(
    isAnalyzing: Boolean,
    syntaxException: SyntaxException?,
    parsedScript: ParsedScript?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .horizontalScroll(rememberScrollState())
            .background(
                if (syntaxException != null) MaterialTheme.colorScheme.errorContainer
                else if (parsedScript != null) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(start = 8.dp)
    ) {
        if (syntaxException != null) {
            Text(
                text = "行${syntaxException.lineNum}错误：${syntaxException.mes}",
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (parsedScript != null) {
            Text(
                text = "队伍：" + parsedScript.team.joinToString(", ") { "(${it.num})${it.name}" },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (isAnalyzing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

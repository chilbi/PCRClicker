package com.pcrclicker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pcrclicker.Operate
import com.pcrclicker.ParsedScript
import com.pcrclicker.Settings
import com.pcrclicker.toMinute
import kotlinx.coroutines.launch

@Composable
fun OperatorPanel(
    parsedScript: ParsedScript,
    settings: Settings,
    save: (content: String) -> Unit
) {
    val density = LocalDensity.current
    val width = remember { mutableStateOf(230.dp) }
    val isMin = remember { mutableStateOf(false) }
    val confirmRestart = remember { mutableStateOf(false) }
    val currentOperate = parsedScript.currentOperate.collectAsState().value
    val summary = parsedScript.summary.collectAsState().value
    val isOn = parsedScript.isOn.collectAsState().value
    val recording = parsedScript.recording.collectAsState().value
    val autoClick = parsedScript.autoClick.collectAsState().value
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentOperate) {
        confirmRestart.value = false
    }

    val iconButtonColors = IconButtonDefaults.iconButtonColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    if (isMin.value) {
        IconButton(
            onClick = { isMin.value = false },
            modifier = Modifier.background(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.shapes.large
            )
        ) {
            Icon(
                imageVector = Icons.Default.Fullscreen,
                contentDescription = "最大化",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    } else {
        Box(
            modifier = Modifier.width(width.value)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                        MaterialTheme.shapes.large
                    )
            ) {
                Text(
                    text = buildAnnotatedString {
                        append(summary[0])
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
                            append(summary[1])
                        }
                        append(summary[2])
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp, 4.dp, 32.dp, 4.dp)
                        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                        .padding(4.dp, 2.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall
                )

                Row(
                    modifier = Modifier.padding(4.dp)
                ) {
                    if (recording) {
                        MiniButton(
                            onClick = { scope.launch { parsedScript.stopRecord(settings, save) } },
                            text = "停止录制并另存为"
                        )
                    } else if (autoClick) {
                        MiniButton(
                            onClick = { scope.launch { parsedScript.stopAutoClick(settings) } },
                            text = "停止自动打轴"
                        )
                    } else {
                        MiniButton(
                            onClick = { parsedScript.handleClickMenu(settings) },
                            text = "游戏菜单"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        MiniButton(
                            onClick = { parsedScript.handleClickSpeed(settings) },
                            text = "游戏加速"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        BadgedBox(
                            badge = {
                                if (confirmRestart.value) {
                                    Badge(
                                        modifier = Modifier.offset((-8).dp, (-4).dp),
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ) {
                                        Text("确定")
                                    }
                                }
                            }
                        ) {
                            MiniButton(
                                onClick = {
                                    if (confirmRestart.value) {
                                        parsedScript.restart()
                                        confirmRestart.value = false
                                    } else {
                                        confirmRestart.value = true
                                    }
                                },
                                text = "重新开始"
                            )
                        }
                    }
                }

                if (!autoClick) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (currentOperate) {
                            is Operate.Click -> {
                                SecDisplay(currentOperate.sec)
                                BadgedBox(
                                    badge = {
                                        if (isOn) {
                                            Badge(
                                                modifier = Modifier.offset((-8).dp, 4.dp),
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(currentOperate.type.getBadgeText())
                                            }
                                        }
                                    }
                                ) {
                                    OutlinedButton(
                                        onClick = { scope.launch { parsedScript.handleClickOperate(settings, save) } },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = BorderStroke(
                                            2.dp,
                                            if (isOn) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Text(currentOperate.type.getText(parsedScript.team))
                                    }
                                }
                            }

                            is Operate.Confirm -> {
                                SecDisplay(currentOperate.sec)
                                BadgedBox(
                                    badge = {
                                        if (isOn) {
                                            Badge(
                                                modifier = Modifier.offset((-8).dp, 4.dp),
                                                containerColor = MaterialTheme.colorScheme.tertiary
                                            ) {
                                                Text("OK")
                                            }
                                        }
                                    }
                                ) {
                                    OutlinedButton(
                                        onClick = { scope.launch { parsedScript.handleClickOperate(settings, save) } },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        ),
                                        border = BorderStroke(
                                            2.dp,
                                            if (isOn) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Text(
                                            text = currentOperate.message
                                        )
                                    }
                                }
                            }

                            is Operate.Record -> {
                                SecDisplay(currentOperate.sec)
                                Button(onClick = { scope.launch { parsedScript.startRecord(settings, save) } }) {
                                    Text(
                                        text = "record" + currentOperate.message
                                    )
                                }
                            }

                            is Operate.Start -> {
                                SecDisplay(currentOperate.sec)
                                Button(onClick = { scope.launch { parsedScript.startAutoClick(settings) } }) {
                                    Text(
                                        text = "start" + currentOperate.message
                                    )
                                }
                            }

                            is Operate.Stop -> {
                                SecDisplay(currentOperate.sec)
                                Button(onClick = { scope.launch { parsedScript.stopAutoClick(settings) } }) {
                                    Text(
                                        text = "stop" + currentOperate.message
                                    )
                                }
                            }
                        }
                    }
                }

                if (!(recording || autoClick)) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { parsedScript.prevLine() },
                            modifier = Modifier.size(28.dp),
                            enabled = !parsedScript.isFirstLine(),
                            colors = iconButtonColors
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "上一行"
                            )
                        }

                        IconButton(
                            onClick = { parsedScript.prevOperate() },
                            modifier = Modifier.size(28.dp),
                            enabled = !parsedScript.isStart(),
                            colors = iconButtonColors
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                                contentDescription = "上一个"
                            )
                        }

                        IconButton(
                            onClick = { parsedScript.nextOperate() },
                            modifier = Modifier.size(28.dp),
                            enabled = !parsedScript.isEnd(),
                            colors = iconButtonColors
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                                contentDescription = "下一个"
                            )
                        }

                        IconButton(
                            onClick = { parsedScript.nextLine() },
                            modifier = Modifier.size(28.dp),
                            enabled = !parsedScript.isLastLine(),
                            colors = iconButtonColors
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "下一行"
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = { isMin.value = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp),
                colors = iconButtonColors
            ) {
                Icon(
                    imageVector = Icons.Default.CloseFullscreen,
                    contentDescription = "最小化",
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            width.value = with(density) {
                                (width.value + dragAmount.x.toDp()).coerceIn(230.dp, 450.dp)
                            }
                        }
                    },
                colors = iconButtonColors
            ) {
                Icon(
                    imageVector = Icons.Default.SouthEast,
                    contentDescription = "调整大小",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniButton(
    onClick: () -> Unit,
    text: String
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.shapes.small
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                MaterialTheme.shapes.small
            )
            .padding(10.dp, 6.dp)
            .clip(MaterialTheme.shapes.small),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun SecDisplay(sec: Int) {
    Row(
        modifier = Modifier
            .padding(end = 8.dp)
            .width(50.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = "时间",
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = sec.toMinute(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge
        )
    }
}


package com.pcrclicker.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pcrclicker.FloatingWindowService
import com.pcrclicker.MainActivity
import com.pcrclicker.Position
import com.pcrclicker.Settings
import com.pcrclicker.getPositions
import com.pcrclicker.ui.components.BackTitle
import com.pcrclicker.ui.components.ConfirmationDialog
import com.pcrclicker.ui.components.RequiredPositiveIntegerInput
import com.pcrclicker.viewModels.ServicesViewModel
import com.pcrclicker.viewModels.SettingsViewModel

@Composable
fun ModifyPositionsScreen(
    settingsViewModel: SettingsViewModel,
    servicesViewModel: ServicesViewModel,
    onBack: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val overlayEnabled by servicesViewModel.overlayEnabled.collectAsState()
    val floatingWindowServiceConnected by servicesViewModel.floatingWindowServiceConnected.collectAsState()
    val isOpen = remember { mutableStateOf(false) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        BackTitle(
            title = "点击位置设置",
            onBack = {
                if (isOpen.value) {
                    closePositions()
                }
                onBack()
            }
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            GuideText()

            HandleButtons(
                isOpen,
                settings,
                overlayEnabled,
                floatingWindowServiceConnected,
                settingsViewModel::initialPositions,
                settingsViewModel::updatePosition,
                onBack
            )

            Row(
                modifier = Modifier.padding(8.dp)
            ) {
                val positions = settings.getPositions()
                val partialA = positions.slice(0..3)
                val partialB = positions.slice(4..8)
                Column(
                    Modifier.weight(1f)
                ) {
                    partialA.forEachIndexed { index, data ->
                        ModifyPositionParams(
                            index,
                            data.name,
                            data.position,
                            settingsViewModel::updatePosition
                        )
                    }
                }
                Column(
                    Modifier.weight(1f)
                ) {
                    partialB.forEachIndexed { index, data ->
                        ModifyPositionParams(
                            index + 4,
                            data.name,
                            data.position,
                            settingsViewModel::updatePosition
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideText() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Text(
            text = "1.打开位置定位部件 2.切换到游戏的战斗界面 3.移动定位部件到指定位置",
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable fun HandleButtons(
    isOpen: MutableState<Boolean>,
    settings: Settings,
    overlayEnabled: Boolean,
    floatingWindowServiceConnected: Boolean,
    initialPositions: (screenWidth: Float, screenHeight: Float) -> Unit,
    updatePosition: (index: Int, position: Position) -> Unit,
    onBack: () -> Unit
) {
    val activity = LocalActivity.current as MainActivity
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val screenWidth = remember(configuration) {
        configuration.screenWidthDp * context.resources.displayMetrics.density
    }
    val screenHeight = remember(configuration) {
        configuration.screenHeightDp * context.resources.displayMetrics.density
    }
    val requirePermission = remember { mutableStateOf(false) }
    val showDialog = remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {

        BackHandler(isOpen.value) {
            closePositions()
            onBack()
        }

        Button(
            onClick = { showDialog.value = true },
            modifier = Modifier.weight(1f),
            enabled = !isOpen.value,
            shape = MaterialTheme.shapes.extraLarge.copy(
                topEnd = CornerSize(0.dp),
                bottomEnd = CornerSize(0.dp)
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Text("恢复默认位置")
        }

        Button(
            onClick = {
                if (overlayEnabled && floatingWindowServiceConnected) {
                    if (isOpen.value) {
                        isOpen.value = false
                        closePositions()
                    } else {
                        isOpen.value = true
                        openPositions(settings, updatePosition)
                    }
                } else {
                    requirePermission.value = true
                }
            },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.extraLarge.copy(
                topStart = CornerSize(0.dp),
                bottomStart = CornerSize(0.dp)
            )
        ) {
            Text("${if (isOpen.value) "关闭" else "打开"}位置定位部件")
        }

        if (showDialog.value) {
            ConfirmationDialog(
                title = "确认恢复",
                message = "您确定要恢复为初始位置吗？\n确认后您之前做的修改将不可恢复。",
                onConfirm = {
                    showDialog.value = false
                    initialPositions(screenWidth, screenHeight)
                },
                onDismiss = {
                    showDialog.value = false
                }
            )
        }

        if (requirePermission.value) {
            ConfirmationDialog(
                title = "需要悬浮窗权限",
                message = "确认去打开悬浮窗权限？",
                onConfirm = {
                    requirePermission.value = false
                    activity.openOverlayPermission()
                },
                onDismiss = {
                    requirePermission.value = false
                }
            )
        }
    }
}

private fun openPositions(
    settings: Settings,
    updatePosition: (index: Int, position: Position) -> Unit
) {
    settings.getPositions().forEachIndexed { index, data ->
        FloatingWindowService.getInstance()?.addFloatingWindow(
            FloatingWindowService.SETTINGS_ID + index,
            data.position,
            true,
            { position ->
                updatePosition(index, position)
            }
        ) {
            ModifyPosition(data.name)
        }
    }
}

private fun closePositions() {
    for (index in 0..8) {
        FloatingWindowService.getInstance()?.removeFloatingWindow(
            FloatingWindowService.SETTINGS_ID + index
        )
    }
}

@Composable
private fun ModifyPosition(name: String) {
    Text(
        text = name,
        modifier = Modifier
            .padding(top = 4.dp)
            .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
            .border(1.dp, Color.Black, MaterialTheme.shapes.small)
            .padding(4.dp),
        style = MaterialTheme.typography.labelMedium
    )
    Spacer(
        modifier = Modifier
            .size(4.dp)
            .background(MaterialTheme.colorScheme.error, CircleShape)
    )
}

@Composable
private fun ModifyPositionParams(
    index: Int,
    name: String,
    position: Position,
    updatePosition: (index: Int, position: Position) -> Unit
) {
    val x = remember(position.x) {
        mutableStateOf(position.x.toString())
    }
    val y = remember(position.y) {
        mutableStateOf(position.y.toString())
    }

    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium
        )
        RequiredPositiveIntegerInput(
            x.value,
            "X坐标",
            "",
            "",
            {
                x.value = it
                if (it.isNotEmpty()) {
                    updatePosition(index, position.copy(x = x.value.toInt()))
                }
            }
        )
        RequiredPositiveIntegerInput(
            y.value,
            "Y坐标",
            "",
            "",
            {
                y.value = it
                if (it.isNotEmpty()) {
                    updatePosition(index, position.copy(y = y.value.toInt()))
                }
            }
        )
    }
}

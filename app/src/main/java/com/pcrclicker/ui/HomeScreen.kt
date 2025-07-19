@file:OptIn(ExperimentalLayoutApi::class)

package com.pcrclicker.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pcrclicker.FloatingWindowService
import com.pcrclicker.MainActivity
import com.pcrclicker.ui.components.ConfirmationDialog
import com.pcrclicker.viewModels.FileViewModel
import com.pcrclicker.viewModels.ServicesViewModel
import com.pcrclicker.viewModels.SettingsViewModel

@Composable
fun HomeScreen(
    fileViewModel: FileViewModel,
    servicesViewModel: ServicesViewModel,
    settingsViewModel: SettingsViewModel,
    navigateToModifyPositions: () -> Unit,
    navigateToModifyPauseActions: () -> Unit,
    navigateToEditor: (fileName: String) -> Unit
) {
    val activity = LocalActivity.current as MainActivity
    val overlayEnabled by servicesViewModel.overlayEnabled.collectAsState()
    val accessibilityEnabled by servicesViewModel.accessibilityEnabled.collectAsState()
    val floatingWindowServiceConnected by servicesViewModel.floatingWindowServiceConnected.collectAsState()
    val isPauseActionsEnabled by settingsViewModel.isPauseActionsEnabled.collectAsState()
    val showDialog = remember { mutableStateOf(false) }

    if (showDialog.value) {
        ConfirmationDialog(
            title = "确认关闭卡帧按钮",
            message = "卡帧按钮打开中，不能去修改设置。",
            onConfirm = {
                showDialog.value = false
                settingsViewModel.disablePauseActions()
            },
            onDismiss = {
                showDialog.value = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            SettingsChip(
                label = "点击位置设置",
                onClick = {
                    if (isPauseActionsEnabled) {
                        showDialog.value = true
                    } else {
                        navigateToModifyPositions()
                    }
                }
            )

            SettingsChip(
                label = "卡帧按钮设置",
                onClick = {
                    if (isPauseActionsEnabled) {
                        showDialog.value = true
                    } else {
                        navigateToModifyPauseActions()
                    }
                }
            )

            PermissionChip(
                label = "悬浮窗权限",
                isEnabled = overlayEnabled,
                onClick = activity::openOverlayPermission
            )

            PermissionChip(
                label = "无障碍权限",
                isEnabled = accessibilityEnabled,
                onClick = activity::openAccessibilitySettings
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EnablePauseActions(
                overlayEnabled,
                accessibilityEnabled,
                floatingWindowServiceConnected,
                isPauseActionsEnabled,
                settingsViewModel::enablePauseActions,
                settingsViewModel::disablePauseActions
            )

            DisableFloatingTool(
                overlayEnabled,
                floatingWindowServiceConnected
            )
        }

        TextFile(fileViewModel, navigateToEditor)
    }
}

@Composable
private fun PermissionChip(
    label: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val color: Color
    val containerColor: Color
    val onContainerColor: Color
    if (isEnabled) {
        color = MaterialTheme.colorScheme.primary
        containerColor = MaterialTheme.colorScheme.primaryContainer
        onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        color = MaterialTheme.colorScheme.error
        containerColor = MaterialTheme.colorScheme.errorContainer
        onContainerColor = MaterialTheme.colorScheme.onErrorContainer
    }

    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.padding(4.dp),
        leadingIcon = {
            Icon(
                imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = color
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = color
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = onContainerColor,
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = color
        )
    )
}

@Composable
private fun SettingsChip(
    label: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.padding(4.dp),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = null
            )
        }
    )
}

@Composable
private fun RowScope.EnablePauseActions(
    overlayEnabled: Boolean,
    accessibilityEnabled: Boolean,
    floatingWindowServiceConnected: Boolean,
    isPauseActionsEnabled: Boolean,
    enablePauseActions: () -> Unit,
    disablePauseActions: () -> Unit
) {
    Button(
        onClick = {
            if (isPauseActionsEnabled) {
                disablePauseActions()
            } else {
                enablePauseActions()
            }
        },
        modifier = Modifier.weight(1f),
        enabled = overlayEnabled && accessibilityEnabled && floatingWindowServiceConnected,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("${if (isPauseActionsEnabled) "关闭" else "打开" }卡帧按钮")
    }
}


@Composable
private fun RowScope.DisableFloatingTool(
    overlayEnabled: Boolean,
    floatingWindowServiceConnected: Boolean,
) {
    val isEnabledTool = remember { mutableStateOf(FloatingWindowService.isEnabledTool()) }

    Button(
        onClick = {
            FloatingWindowService.getInstance()?.removeFloatingWindow(
                FloatingWindowService.TOOL_ID
            )
            isEnabledTool.value = false
        },
        modifier = Modifier.weight(1f),
        enabled = isEnabledTool.value && overlayEnabled && floatingWindowServiceConnected,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("关闭悬浮打轴工具")
    }
}

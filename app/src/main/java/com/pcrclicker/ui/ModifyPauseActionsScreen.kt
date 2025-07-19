package com.pcrclicker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pcrclicker.PauseAction
import com.pcrclicker.ui.components.BackTitle
import com.pcrclicker.ui.components.ConfirmationDialog
import com.pcrclicker.ui.components.DeleteButtonWithConfirmation
import com.pcrclicker.ui.components.RequiredPositiveIntegerInput
import com.pcrclicker.viewModels.SettingsViewModel

@Composable
fun ModifyPauseActionsScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        BackTitle("卡帧按钮设置", onBack)

        settings.pauseActions.forEachIndexed { index, pauseAction ->
            ModifyPauseAction(
                index,
                pauseAction,
                settingsViewModel::removePauseAction,
                settingsViewModel::updatePauseAction
            )
        }

        HandleButtons(
            initialPauseActions = settingsViewModel::initialPauseActions,
            addPauseAction = settingsViewModel::addPauseAction
        )
    }
}

@Composable
private fun HandleButtons(
    initialPauseActions: () -> Unit,
    addPauseAction: () -> Unit,
) {
    val showDialog = remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Button(
            onClick = { showDialog.value = true },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.extraLarge.copy(
                topEnd = CornerSize(0.dp),
                bottomEnd = CornerSize(0.dp)
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text("恢复为默认按钮")
        }
        if (showDialog.value) {
            ConfirmationDialog(
                title = "确认恢复",
                message = "您确定要恢复为默认按钮吗？\n确认后您之前做的修改将不可恢复。",
                onConfirm = {
                    showDialog.value = false
                    initialPauseActions()
                },
                onDismiss = { showDialog.value = false }
            )
        }

        Button(
            onClick = addPauseAction,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.extraLarge.copy(
                topStart = CornerSize(0.dp),
                bottomStart = CornerSize(0.dp)
            )
        ) {
            Text("添加新卡帧按钮")
        }
    }
}

@Composable
private fun ModifyPauseAction(
    index: Int,
    pauseAction: PauseAction,
    removePauseAction: (index: Int) -> Unit,
    updatePauseAction: (index: Int, pauseAction: PauseAction) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "卡帧按钮${index + 1}",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge
            )
            DeleteButtonWithConfirmation(
                title = "确认删除",
                message = "您确定要删除此项吗？删除后将无法恢复。",
                onConfirm = { removePauseAction(index) }
            )
        }

        ModifyPauseActionParams(index, pauseAction, updatePauseAction)
    }
}

@Composable
private fun ModifyPauseActionParams(
    index: Int,
    pauseAction: PauseAction,
    updatePauseAction: (index: Int, pauseAction: PauseAction) -> Unit
) {
    val name = remember(pauseAction.name) {
        mutableStateOf(pauseAction.name)
    }
    val closePauseWindowDelayTime = remember(pauseAction.closePauseWindowDelayTime) {
        mutableStateOf(pauseAction.closePauseWindowDelayTime.toString())
    }
    val pauseBattleDelayTime = remember(pauseAction.pauseBattleDelayTime) {
        mutableStateOf(pauseAction.pauseBattleDelayTime.toString())
    }

    Row {
        TextField(
            value = name.value,
            onValueChange = {
                name.value = it
                updatePauseAction(index, pauseAction.copy(name = name.value))
            },
            modifier = Modifier.weight(1f),
            label = { Text("按钮名称") },
            supportingText = {
                Text("输入显示在按钮上的文本")
            }
        )
        RequiredPositiveIntegerInput(
            closePauseWindowDelayTime.value,
            "关闭暂停窗口延迟时间",
            "输入卡停战斗后完全弹出游戏暂停窗口所需要的毫秒数",
            "ms",
            {
                closePauseWindowDelayTime.value = it
                if (it.isNotEmpty()) {
                    updatePauseAction(
                        index,
                        pauseAction.copy(closePauseWindowDelayTime = closePauseWindowDelayTime.value.toLong())
                    )
                }
            }
        )
        RequiredPositiveIntegerInput(
            pauseAction.pauseBattleDelayTime.toString(),
            "暂停战斗延迟时间",
            "输入恢复战斗后多少毫秒卡停战斗（注意即使输入0也会有设备性能上的延迟）",
            "ms",
            {
                pauseBattleDelayTime.value = it
                if (it.isNotEmpty()) {
                    updatePauseAction(
                        index,
                        pauseAction.copy(pauseBattleDelayTime = pauseBattleDelayTime.value.toLong())
                    )
                }
            }
        )
    }
}

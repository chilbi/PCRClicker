package com.pcrclicker.ui

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.pcrclicker.PauseGame
import com.pcrclicker.PauseAction
import com.pcrclicker.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PauseActionButton(
    pauseClickable: MutableState<Boolean>,
    pauseActionCount: Int,
    pauseAction: PauseAction,
    blankPosition: Position
) {
    val scope = rememberCoroutineScope()
    val onClick = remember {
        {
            if (pauseClickable.value) {
                scope.launch(Dispatchers.Main) {
                    PauseGame.impl.perform(
                        object : PauseGame.Callback {
                            override fun onStart() { pauseClickable.value = false }
                            override fun onEnd() { pauseClickable.value = true }
                        },
                        pauseActionCount,
                        pauseAction,
                        blankPosition
                    )
                }
//                pauseClickable.value = false
//                scope.launch(Dispatchers.Main) {
//                    if (FloatingWindowService.isPaused) {
//                        FloatingWindowService.getInstance()?.removeModalWindow()
//                    }
//                    delay(pauseAction.closePauseWindowDelayTime)
//                    AutoClickService.getInstance()?.performClick(blankPosition.x, blankPosition.y)
//                    if (pauseAction.pauseBattleDelayTime > 0) {
//                        delay(pauseAction.pauseBattleDelayTime)
//                    }
//                    FloatingWindowService.getInstance()?.addModalWindow(pauseActionCount)
//                    pauseClickable.value = true
//                }
            }
        }
    }
    Button(
        onClick = onClick,
        enabled = pauseClickable.value,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        )
    ) {
        Text(pauseAction.name)
    }
}

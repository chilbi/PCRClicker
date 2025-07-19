package com.pcrclicker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pcrclicker.FloatingWindowService
import com.pcrclicker.Position
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RowScope.FloatingTimeDisplay(
    overlayEnabled: Boolean,
    floatingWindowServiceConnected: Boolean
) {
    Button(
        onClick = {
            FloatingWindowService.getInstance()?.addFloatingWindow(
                FloatingWindowService.TIME_ID,
                Position(250, 30),
                true,
                null
            ) {
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .background(Color.Gray, CircleShape)
                        .padding(8.dp, 4.dp),
                ) {
                    TimeDisplay()

                    IconButton(
                        onClick = {
                            FloatingWindowService.getInstance()?.removeFloatingWindow(
                                FloatingWindowService.TIME_ID
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp),
        enabled = overlayEnabled && floatingWindowServiceConnected
    ) {
        Text("显示悬浮时间")
    }
}

@Composable
private fun TimeDisplay() {
    val timeFormat = remember {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    }
    val currentTime = remember { mutableStateOf(timeFormat.format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTime.value = timeFormat.format(Date(now))
            delay(16)
        }
    }

//    LaunchedEffect(Unit) {
//        while (isActive) {
//            val now = System.currentTimeMillis()
//            val nextTime = now + 1
//            currentTime.value = timeFormat.format(Date(now))
//            val sleepTime = nextTime - System.currentTimeMillis()
//            if (sleepTime > 0) {
//                delay(sleepTime)
//            }
//        }
//    }

//    DisposableEffect(Unit) {
//        val choreographer = Choreographer.getInstance()
//        val frameCallback = object : Choreographer.FrameCallback {
//            override fun doFrame(frameTimeNanos: Long) {
//                currentTime.value = timeFormat.format(Date())
//                choreographer.postFrameCallback(this)
//            }
//        }
//        choreographer.postFrameCallback(frameCallback)
//
//        onDispose {
//            choreographer.removeFrameCallback(frameCallback)
//        }
//    }

    Text(
        text = currentTime.value,
        color = Color.White
    )
}

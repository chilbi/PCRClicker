package com.pcrclicker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.pcrclicker.ui.theme.AppTheme

fun Modifier.ifThen(
    condition: Boolean,
    modifier: Modifier.() -> Modifier
): Modifier = if (condition) then(modifier(this)) else this

@Composable
fun FloatingWindow(
    moveable: Boolean,
    onPositionChange: (dx: Int, dy: Int) -> Unit,
    onDragEnd: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val modifier = remember(moveable) {
        Modifier
            .wrapContentSize()
            .ifThen(moveable) {
                pointerInput(Unit) {
                    detectDragGestures(onDragEnd = onDragEnd) { change, dragAmount ->
                        change.consume()
                        onPositionChange(dragAmount.x.toInt(), dragAmount.y.toInt())
                    }
                }
            }
    }
    AppTheme {
        Box(
            modifier = modifier,
            content = content
        )
    }
}

@Composable
fun ModalWindow(
    onCloseClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val modifier = remember {
        Modifier
//            .background(Color(0x99000000))
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onCloseClick
            )
    }
    Spacer(modifier)
}

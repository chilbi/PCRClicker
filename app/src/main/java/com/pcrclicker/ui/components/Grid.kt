package com.pcrclicker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Grid(
    columnCount: Int,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: GridScope.() -> Unit
) {
    val items = GridScopeImpl().apply(content).items
    val rowCount = (items.size + columnCount - 1) / columnCount
    Column(modifier) {
        for (row in 0 until rowCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                for (col in 0 until columnCount) {
                    val index = row * columnCount + col
                    if (index < items.size) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(
                                    end = if (col < columnCount - 1) spacing else 0.dp,
                                    bottom = if (row < rowCount - 1) spacing else 0.dp
                                )
                        ) {
                            items[index]()
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

interface GridScope {
    fun item(content: @Composable () -> Unit)
}

private class GridScopeImpl : GridScope {
    val items = mutableListOf<@Composable () -> Unit>()

    override fun item(content: @Composable () -> Unit) {
        items.add(content)
    }
}

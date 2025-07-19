package com.pcrclicker

import com.pcrclicker.PauseAction
import com.pcrclicker.Position
import kotlinx.coroutines.delay

interface PauseGame  {
    suspend fun perform(
        callback: Callback,
        pauseActionCount: Int,
        pauseAction: PauseAction,
        blankPosition: Position
    ): Unit

    interface Callback {
        fun onStart(): Unit
        fun onEnd(): Unit
    }

    companion object {
        var impl: PauseGame = object : PauseGame {
            override suspend fun perform(
                callback: Callback,
                pauseActionCount: Int,
                pauseAction: PauseAction,
                blankPosition: Position
            ) {
                callback.onStart()
                if (FloatingWindowService.isPaused) {
                    FloatingWindowService.getInstance()!!.removeModalWindow()
                }
                delay(pauseAction.closePauseWindowDelayTime)
                AutoClickService.getInstance()?.performClick(blankPosition.x, blankPosition.y)
                if (pauseAction.pauseBattleDelayTime > 0) {
                    delay(pauseAction.pauseBattleDelayTime)
                }
                FloatingWindowService.getInstance()?.addModalWindow(pauseActionCount)
                callback.onEnd()
            }
        }
    }
}

package com.pcrclicker

import kotlinx.coroutines.delay

interface PauseGame  {
    suspend fun perform(
        callback: Callback,
        pauseActionCount: Int,
        pauseAction: PauseAction,
        blankPosition: Position
    )

    interface Callback {
        fun onStart()
        fun onEnd()
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
                val floatingWindowService = FloatingWindowService.getInstance()
                val autoClickService = AutoClickService.getInstance()
                if (floatingWindowService != null && autoClickService != null) {
                    if (FloatingWindowService.isPaused) {
                        floatingWindowService.resumeGame()
                        if (pauseAction.closePauseWindowDelayTime > 0) {
                            delay(pauseAction.closePauseWindowDelayTime)
                        }
                    }
                    autoClickService.performClick(blankPosition.x, blankPosition.y)
                    if (pauseAction.pauseBattleDelayTime > 0) {
                        delay(pauseAction.pauseBattleDelayTime)
                    }
                    floatingWindowService.pauseGame(pauseActionCount)
                }
                callback.onEnd()
            }
        }
    }
}

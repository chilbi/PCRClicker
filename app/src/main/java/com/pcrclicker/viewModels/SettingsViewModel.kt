package com.pcrclicker.viewModels

import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pcrclicker.FloatingWindowService
import com.pcrclicker.PauseAction
import com.pcrclicker.Position
import com.pcrclicker.Settings
import com.pcrclicker.ui.PauseActionButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.min

class SettingsViewModel(private val dataStore: DataStore<Settings>) : ViewModel() {
    val settings = dataStore.data
        .catch { emit(Settings()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Settings()
        )

    private val _isPauseActionsEnabled = MutableStateFlow(false)
    val isPauseActionsEnabled = _isPauseActionsEnabled.asStateFlow()

    private val pauseClickable = mutableStateOf(true)

    fun enablePauseActions() {
        if (_isPauseActionsEnabled.value) {
            return
        }
        _isPauseActionsEnabled.value = true
        val currentSettings = settings.value
        val pauseActionCount = currentSettings.pauseActions.size
        val instance = FloatingWindowService.getInstance()
        if (instance != null && pauseActionCount > 0) {
            currentSettings.pauseActions.forEachIndexed { index, pauseAction ->
                instance.addFloatingWindow(
                    FloatingWindowService.PAUSE_ID + index,
                    pauseAction.position,
                    true,
                    { position ->
                        updatePauseActionPosition(index, position)
                    }
                ) {
                    PauseActionButton(
                        pauseClickable,
                        pauseActionCount,
                        pauseAction,
                        currentSettings.blankPosition
                    )
                }
            }
        }
    }

    fun disablePauseActions() {
        if (!_isPauseActionsEnabled.value) {
            return
        }
        _isPauseActionsEnabled.value = false
        val currentSettings = settings.value
        val pauseActionCount = currentSettings.pauseActions.size
        val instance = FloatingWindowService.getInstance()
        if (instance != null && pauseActionCount > 0) {
            currentSettings.pauseActions.forEachIndexed { index, pauseAction ->
                instance.removeFloatingWindow(FloatingWindowService.PAUSE_ID + index)
            }
        }
    }

    fun initialPauseActions() {
        viewModelScope.launch {
            dataStore.updateData { currentSettings ->
                currentSettings.copy(pauseActions = Settings().pauseActions)
            }
        }
    }

    fun initialPositions(screenWidth: Float, screenHeight: Float) {
        viewModelScope.launch {
            dataStore.updateData { currentSettings ->
                val s = Settings()
                val width = screenWidth - 50
                val height = screenHeight - 50
                if (s.speedPosition.x < width &&
                    s.speedPosition.y < height) {
                    currentSettings.copy(
                        blankPosition = s.blankPosition,
                        menuPosition = s.menuPosition,
                        autoPosition = s.autoPosition,
                        speedPosition = s.speedPosition,
                        ub1Position = s.ub1Position,
                        ub2Position = s.ub2Position,
                        ub3Position = s.ub3Position,
                        ub4Position = s.ub4Position,
                        ub5Position = s.ub5Position
                    )
                } else {
                    val scaleX = width / s.speedPosition.x
                    val scaleY = height / s.speedPosition.y
                    currentSettings.copy(
                        blankPosition = s.blankPosition.scale(scaleX, scaleY),
                        menuPosition = s.menuPosition.scale(scaleX, scaleY),
                        autoPosition = s.autoPosition.scale(scaleX, scaleY),
                        speedPosition = s.speedPosition.scale(scaleX, scaleY),
                        ub1Position = s.ub1Position.scale(scaleX, scaleY),
                        ub2Position = s.ub2Position.scale(scaleX, scaleY),
                        ub3Position = s.ub3Position.scale(scaleX, scaleY),
                        ub4Position = s.ub4Position.scale(scaleX, scaleY),
                        ub5Position = s.ub5Position.scale(scaleX, scaleY)
                    )
                }
            }
        }
    }

    fun addPauseAction() {
        viewModelScope.launch {
            dataStore.updateData { currentSettings ->
                val newPauseActions = currentSettings.pauseActions.toMutableList()
                val size = newPauseActions.size
                newPauseActions.add(
                    PauseAction(
                        "按钮${size + 1}",
                        Position(30, (min(size, 4) * 150) + 200),
                        600,
                        0
                    )
                )
                currentSettings.copy(pauseActions = newPauseActions)
            }
        }
    }

    fun removePauseAction(index: Int) {
        viewModelScope.launch {
            dataStore.updateData { currentSettings ->
                val newPauseActions = currentSettings.pauseActions.toMutableList()
                newPauseActions.removeAt(index)
                currentSettings.copy(pauseActions = newPauseActions)
            }
        }
    }

    fun updatePauseAction(index: Int, pauseAction: PauseAction) {
        viewModelScope.launch {
            dataStore.updateData { currentSettings ->
                val newPauseActions = currentSettings.pauseActions.toMutableList()
                newPauseActions[index] = pauseAction
                currentSettings.copy(pauseActions = newPauseActions)
            }
        }
    }

    fun updatePauseActionPosition(index: Int, position: Position) {
        viewModelScope.launch {
            dataStore.updateData { currentSettings ->
                val newPauseActions = currentSettings.pauseActions.toMutableList()
                newPauseActions[index] = newPauseActions[index].copy(position = position)
                currentSettings.copy(pauseActions = newPauseActions)
            }
        }
    }

    fun updatePosition(index: Int, position: Position) {
        viewModelScope.launch {
            dataStore.updateData { currentSettings ->
                when (index) {
                    0 -> currentSettings.copy(blankPosition = position)
                    1 -> currentSettings.copy(menuPosition = position)
                    2 -> currentSettings.copy(autoPosition = position)
                    3 -> currentSettings.copy(speedPosition = position)
                    4 -> currentSettings.copy(ub1Position = position)
                    5 -> currentSettings.copy(ub2Position = position)
                    6 -> currentSettings.copy(ub3Position = position)
                    7 -> currentSettings.copy(ub4Position = position)
                    else -> currentSettings.copy(ub5Position = position)
                }
            }
        }
    }
}

class SettingsViewModelFactory(
    private val dataStore: DataStore<Settings>
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(dataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private fun Position.scale(scaleX: Float, scaleY: Float): Position {
    return Position((this.x * scaleX).toInt(), (this.y * scaleY).toInt())
}

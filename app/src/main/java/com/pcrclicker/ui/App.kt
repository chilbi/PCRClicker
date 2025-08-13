package com.pcrclicker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.pcrclicker.MainApplication
import com.pcrclicker.settingsDataStore
import com.pcrclicker.viewModels.FileViewModel
import com.pcrclicker.viewModels.FileViewModelFactory
import com.pcrclicker.viewModels.ServicesViewModel
import com.pcrclicker.viewModels.SettingsViewModel
import com.pcrclicker.viewModels.SettingsViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Composable
fun App(
    fileNameAndText: Pair<String, String>?,
) {
    val context = LocalContext.current
    val servicesViewModel: ServicesViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.settingsDataStore)
    )
    val fileViewModel: FileViewModel = viewModel(
        factory = FileViewModelFactory(MainApplication.instance)
    )
    settingsViewModel.settings.collectAsState()
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val save: (fileName: String, content: String) -> Unit = remember {
        { fileName, content ->
            scope.launch {
                fileViewModel.saveFile("[AutoClick]$fileName", content)
            }
        }
    }

    LaunchedEffect(fileNameAndText) {
        if (fileNameAndText != null) {
            fileViewModel.saveFile(fileNameAndText.first, fileNameAndText.second)
            navController.navigate(Editor(fileNameAndText.first))
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(navController, Home) {
            composable<Home> {
                HomeScreen(
                    fileViewModel = fileViewModel,
                    servicesViewModel = servicesViewModel,
                    settingsViewModel = settingsViewModel,
                    navigateToModifyPositions = { navController.navigate(ModifyPositions) },
                    navigateToModifyPauseActions = { navController.navigate(ModifyPauseActions) },
                    navigateToEditor = { fileName -> navController.navigate(Editor(fileName)) }
                )
            }
            composable<ModifyPositions> {
                ModifyPositionsScreen(
                    settingsViewModel = settingsViewModel,
                    servicesViewModel = servicesViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable<ModifyPauseActions> {
                ModifyPauseActionsScreen(
                    settingsViewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable<Editor> { backStackEntry ->
                val editor = backStackEntry.toRoute<Editor>()
                EditorScreen(
                    fileName = editor.fileName,
                    fileViewModel = fileViewModel,
                    servicesViewModel = servicesViewModel,
                    settingsViewModel = settingsViewModel,
                    save = save,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Serializable
object Home
@Serializable
object ModifyPositions
@Serializable
object ModifyPauseActions
@Serializable
data class Editor(val fileName: String)

package com.rotask.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rotask.RotaskApplication
import com.rotask.ui.home.HomeScreen
import com.rotask.ui.home.HomeViewModel
import com.rotask.ui.work.WorkMode
import com.rotask.ui.work.WorkScreen
import com.rotask.ui.work.WorkViewModel

@Composable
fun RotaskNavHost(application: RotaskApplication) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(
                    application.repository,
                    application.soundSettings,
                    application.soundPlayer,
                )
            )
            HomeScreen(
                vm = vm,
                onStartWork = { start -> navController.navigate("work/${start.taskId}/${start.mode.name}") }
            )
        }
        composable(
            route = "work/{taskId}/{mode}",
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType },
                navArgument("mode") { type = NavType.StringType },
            )
        ) { entry ->
            val taskId = entry.arguments?.getLong("taskId") ?: 0L
            val mode = entry.arguments
                ?.getString("mode")
                ?.let { runCatching { WorkMode.valueOf(it) }.getOrNull() }
                ?: WorkMode.ROTATION
            val vm: WorkViewModel = viewModel(
                factory = WorkViewModel.factory(
                    application.repository,
                    application.appScope,
                    application.soundPlayer,
                    application.completionAlarmScheduler,
                    taskId,
                    mode,
                )
            )
            WorkScreen(
                vm = vm,
                onFinish = { navController.popBackStack() }
            )
        }
    }
}

package com.example.myapplication.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.myapplication.AppContainer
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.theme.Lime
import com.example.myapplication.viewmodel.*

private data class Destination(val route: String, val label: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("home", "Inicio", Icons.Outlined.Home),
    Destination("routines", "Entrenar", Icons.Outlined.FitnessCenter),
    Destination("catalog", "Ejercicios", Icons.Outlined.GridView),
    Destination("progress", "Progreso", Icons.Outlined.Insights),
    Destination("profile", "Perfil", Icons.Outlined.Person)
)

@Composable
fun HeavyDutyApp(container: AppContainer) {
    val app: AppViewModel = viewModel(factory = appFactory(container))
    val catalog: CatalogViewModel = viewModel(factory = catalogFactory(container))
    val routines: RoutineViewModel = viewModel(factory = routineFactory(container))
    val progress: ProgressViewModel = viewModel(factory = progressFactory(container))
    val profile: ProfileViewModel = viewModel(factory = profileFactory(container))
    val ready by app.ready.collectAsStateWithLifecycle()
    val error by app.initializationError.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    ObserveMessages(app, snackbar)
    ObserveMessages(catalog, snackbar)
    ObserveMessages(routines, snackbar)
    ObserveMessages(progress, snackbar)
    ObserveMessages(profile, snackbar)
    if (!ready) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            if (error == null) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Icon(Icons.Outlined.FitnessCenter, null, tint = Lime, modifier = Modifier.size(60.dp))
                Text("HEAVY DUTY", style = MaterialTheme.typography.headlineLarge)
                CircularProgressIndicator()
            } else EmptyState("No se pudo iniciar", error.orEmpty()) {
                Button(onClick = app::initialize) { Text("Reintentar") }
            }
        }
        return
    }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    fun tab(target: String) {
        nav.navigate(target) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (destinations.any { it.route == route }) {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = route == destination.route, onClick = { tab(destination.route) },
                            icon = { Icon(destination.icon, null) }, label = { Text(destination.label, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding).imePadding()) {
            composable("home") {
                HomeScreen(app, { tab("routines") }, { tab("progress") }, { nav.navigate("workout/" + it) })
            }
            composable("catalog") { CatalogScreen(catalog) { nav.navigate("exercise/" + it) } }
            composable("exercise/{exerciseId}", arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })) { backStack ->
                val all by catalog.all.collectAsStateWithLifecycle()
                ExerciseDetailScreen(all.firstOrNull { it.id == backStack.arguments?.getLong("exerciseId") }) { nav.popBackStack() }
            }
            composable("routines") {
                RoutinesScreen(routines, onEdit = {
                    routines.begin(it)
                    nav.navigate("routine-editor")
                }, onStart = { nav.navigate("workout/" + it) })
            }
            composable("routine-editor") { RoutineEditorScreen(routines) { nav.popBackStack() } }
            composable("workout/{workoutId}", arguments = listOf(navArgument("workoutId") { type = NavType.LongType })) {
                val model: WorkoutViewModel = viewModel(factory = workoutFactory(container))
                ObserveMessages(model, snackbar)
                WorkoutScreen(model, { nav.popBackStack() }, {
                    nav.popBackStack()
                    tab("progress")
                })
            }
            composable("progress") { ProgressScreen(progress) { nav.navigate("history/" + it) } }
            composable("history/{workoutId}", arguments = listOf(navArgument("workoutId") { type = NavType.LongType })) { backStack ->
                val history by progress.history.collectAsStateWithLifecycle()
                WorkoutDetailScreen(history.firstOrNull { it.id == backStack.arguments?.getLong("workoutId") },
                    { nav.popBackStack() }, { progress.delete(it); nav.popBackStack() })
            }
            composable("profile") { ProfileScreen(profile) { nav.navigate("camera") } }
            composable("camera") {
                CameraScreen({ nav.popBackStack() }, { file ->
                    profile.importPhoto(Uri.fromFile(file)) { file.delete() }
                    nav.popBackStack()
                })
            }
        }
    }
}

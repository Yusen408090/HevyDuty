package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-port-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WorkoutFlowUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun screenshot(name: String) {
        val output = File("build/screenshots").apply { mkdirs() }
        val bitmap = compose.runOnIdle {
            val view = compose.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        File(output, name + ".png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun createRoutineCompleteSetAndReadHistory() {
        waitForText("Cada serie cuenta.")
        screenshot("01-inicio")
        compose.onNodeWithText("Ejercicios").performClick()
        waitForText("Buscar un ejercicio")
        compose.onNodeWithText("Buscar un ejercicio").performTextInput("triceps")
        waitForText("Extensión de tríceps")
        compose.onNodeWithText("Extensión de tríceps").performClick()
        waitForText("Cómo realizarlo")
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Entrenar").performClick()
        waitForText("Crear rutina")
        compose.onNodeWithText("Crear rutina").performClick()
        compose.onNodeWithText("Nombre de la rutina").performTextInput("Prueba funcional")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Press de banca"))
        compose.onNodeWithText("Press de banca").performClick()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Guardar rutina"))
        compose.onNodeWithText("Guardar rutina").performClick()
        waitForText("Mis rutinas")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Prueba funcional"))
        compose.onAllNodesWithText("Iniciar entrenamiento").onLast().performScrollTo().performClick()
        waitForText("Temporizador de descanso")
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("60")
        compose.onAllNodes(isToggleable()).onFirst().performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("600").fetchSemanticsNodes().isNotEmpty() }
        screenshot("02-entrenamiento")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Finalizar entrenamiento"))
        compose.onNodeWithText("Finalizar entrenamiento").performClick()
        compose.onNodeWithText("Guardar").performClick()
        waitForText("Tu progreso")
        compose.onNodeWithText("600 kg").assertExists()
        screenshot("03-progreso")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Prueba funcional"))
        compose.onNodeWithText("Prueba funcional").performClick()
        waitForText("Serie 1     60 kg × 10 repeticiones")
        compose.onNodeWithText("Serie 1     60 kg × 10 repeticiones").assertExists()
    }
}

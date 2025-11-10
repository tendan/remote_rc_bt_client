package sh.slusa.remote_rc.component

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performIndirectTouchEvent
import androidx.compose.ui.test.performTouchInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.robolectric.ParameterizedRobolectricTestRunner
import sh.slusa.remote_rc.core.BleConnectionManager
import sh.slusa.remote_rc.model.ControlViewModel
import sh.slusa.remote_rc.service.IControlSystemService
import sh.slusa.remote_rc.ui.component.Controls
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.ParameterizedRobolectricTestRunner.Parameter

@RunWith(ParameterizedRobolectricTestRunner::class)
class ControlsTest(
    private val button: String,
    private val oppositeButton: String
) {
    @get:Rule val composeTestRule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameters(name = "{0}_{1}")
        fun buttonsData() = listOf(
            arrayOf("Lewo", "Prawo"),
            arrayOf("Przód", "Tył"),
            arrayOf("Prawo", "Lewo"),
            arrayOf("Tył", "Przód")
        )
    }

    @Test
    fun buttonDisablesTheOppositeOnes() {
        composeTestRule.setContent {
            Controls(
                controlViewModel = ControlViewModel(),
                connectionState = BleConnectionManager.STATE_CONNECTED,
                controlSystemService = object : IControlSystemService {
                    // TODO: Test cases for valid calls
                    override fun accelerate(enable: Boolean) {}
                    override fun backward(enable: Boolean) {}
                    override fun steerLeft(enable: Boolean) {}
                    override fun steerRight(enable: Boolean) {}
                    override fun emergencyStop() {}
                }
            ) { }
        }

        composeTestRule.onNodeWithText(button).performTouchInput { down(center) }

        composeTestRule.onNodeWithText(oppositeButton).assertIsNotEnabled()
    }
}
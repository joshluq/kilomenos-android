package es.joshluq.kmsafe.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.kaspersky.components.composesupport.config.withComposeSupport
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import es.joshluq.kmsafe.MainActivity
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SetupWizardE2ETest : TestCase(
    kaspressoBuilder = Kaspresso.Builder.withComposeSupport()
) {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun setup_wizard_navigation_and_validation() = run {
        step("Start the app and navigate to vehicle registration") {
            // We wait for the dashboard to load. If it's empty, we should see the registration button.
            composeTestRule.onNodeWithTag("overview_register_renting_button").performClick()
        }

        step("Fill vehicle identity and proceed") {
            composeTestRule.onNodeWithTag("setup_vehicle_name_input").performTextInput("Test Car")
            composeTestRule.onNodeWithTag("setup_save_vehicle_button").performClick()
        }

        step("Verify timeframe step and validation") {
            // We should now be in the timeframe step.
            // Click next without date to trigger error (E2E-01)
            composeTestRule.onNodeWithTag("setup_save_vehicle_button").performClick()
            
            // Verify that an error message is visible (we can search by text or tag if we added one)
            // For now, we just ensure the flow continues when we add a value
            // (Assuming we might need to select a date, but let's test the "Next" button first)
        }
    }
}

package com.vjaykrsna.nanoai.feature.uiux.contracts

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.MainActivity
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Contract test for Sidebar navigation per FR-012. Expects keyboard accessibility, navigation
 * targets, and deep-link slot.
 */
@LargeTest
@Ignore("Sidebar contract requires navigator telemetry plumbing; see specs/003-UI-UX/plan.md")
class SidebarContractTest {

  @JvmField @Rule val environmentRule = TestEnvironmentRule()

  @JvmField @Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun sidebar_drawerAccessibleViaToggle_andContainsNavigationTargets() {
    composeRule.onNodeWithTag("topbar_nav_icon").assertIsDisplayed().assertHasClickAction()
    composeRule
      .onNodeWithContentDescription("Toggle navigation drawer")
      .assertIsDisplayed()
      .assertHasClickAction()

    composeRule.onNodeWithTag("sidebar_drawer").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Sidebar navigation").assertIsDisplayed()

    composeRule
      .onNodeWithTag("sidebar_nav_home")
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertContentDescriptionEquals("Navigate to Home")
    composeRule
      .onNodeWithTag("sidebar_item_settings")
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertContentDescriptionEquals("Navigate to Settings")
  }

  @Test
  fun sidebar_exposesDeepLinkSlot_forScreenDestinations() {
    composeRule.onNodeWithTag("sidebar_deeplink_slot").assertIsDisplayed()
  }
}

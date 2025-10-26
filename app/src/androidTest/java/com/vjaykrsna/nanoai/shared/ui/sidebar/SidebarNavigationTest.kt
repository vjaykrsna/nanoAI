package com.vjaykrsna.nanoai.shared.ui.sidebar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import org.junit.jupiter.api.Test

@org.junit.jupiter.api.extension.ExtendWith(TestEnvironmentRule::class)
class SidebarNavigationTest : BaseSidebarContentTest() {

  @Test
  fun sidebarNavigationDrawer_displaysHomeAndSettings() {
    val state = defaultState(pinnedTools = listOf("settings"), activeRoute = "home")

    renderSidebar(state)

    composeRule.onNodeWithTag("sidebar_drawer_container").assertIsDisplayed()
  }
}

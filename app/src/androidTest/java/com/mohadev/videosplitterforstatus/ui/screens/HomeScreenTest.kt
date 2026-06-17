package com.mohadev.videosplitterforstatus.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mohadev.videosplitterforstatus.ui.theme.VideoSplitterForStatusTheme
import org.junit.Rule
import org.junit.Test
import io.mockk.mockk
import io.mockk.verify

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displaysPresets() {
        composeTestRule.setContent {
            VideoSplitterForStatusTheme {
                HomeScreen(
                    onPresetSelected = {},
                    onCustomSplitSelected = {},
                    onNavigateToSettings = {},
                    onNavigateToHistory = {}
                )
            }
        }

        composeTestRule.onNodeWithText("WhatsApp").assertExists()
        composeTestRule.onNodeWithText("Instagram").assertExists()
    }

    @Test
    fun homeScreen_presetClick_triggersCallback() {
        val onPresetSelected: (SplitPreset) -> Unit = mockk(relaxed = true)
        
        composeTestRule.setContent {
            VideoSplitterForStatusTheme {
                HomeScreen(
                    onPresetSelected = onPresetSelected,
                    onCustomSplitSelected = {},
                    onNavigateToSettings = {},
                    onNavigateToHistory = {}
                )
            }
        }

        composeTestRule.onNodeWithText("WhatsApp").performClick()
        
        verify { onPresetSelected(any()) }
    }
}

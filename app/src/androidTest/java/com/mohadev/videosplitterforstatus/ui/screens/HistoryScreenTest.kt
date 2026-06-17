package com.mohadev.videosplitterforstatus.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mohadev.videosplitterforstatus.ui.theme.VideoSplitterForStatusTheme
import com.mohadev.videosplitterforstatus.ui.viewmodel.HistoryViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel: HistoryViewModel = mockk(relaxed = true)

    @Test
    fun historyScreen_emptyState_displaysNoHistoryText() {
        every { viewModel.historyItems } returns flowOf(emptyList())
        
        composeTestRule.setContent {
            VideoSplitterForStatusTheme {
                HistoryScreen(
                    onBack = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText("No history yet").assertExists()
    }
}

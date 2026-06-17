package com.mohadev.videosplitterforstatus.ui.screens

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mohadev.videosplitterforstatus.ui.theme.VideoSplitterForStatusTheme
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class VideoPickerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val context: Context = mockk(relaxed = true)

    @Test
    fun videoPickerScreen_initialState_displaysGalleryButton() {
        composeTestRule.setContent {
            VideoSplitterForStatusTheme {
                VideoPickerScreen(
                    onVideosSelected = { },
                    context = context,
                    onNavigateToHistory = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Open Gallery").assertExists()
    }
}

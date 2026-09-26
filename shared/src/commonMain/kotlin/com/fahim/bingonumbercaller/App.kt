package com.fahim.bingonumbercaller

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fahim.bingonumbercaller.ui.CallerColors
import com.fahim.bingonumbercaller.ui.CallerScreen
import com.fahim.bingonumbercaller.viewmodel.CallerViewModel

@Composable
@Preview
fun App(
    viewModel: CallerViewModel = viewModel { CallerViewModel() }
) {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding(),
            color = CallerColors.Background
        ) {
            CallerScreen(viewModel = viewModel)
        }
    }
}
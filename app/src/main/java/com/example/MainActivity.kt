package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.OnboardingGuideDialog
import com.example.ui.screens.DatabaseScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SessionDetailScreen
import com.example.ui.screens.SetupSessionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SingleTimeStudyScreen
import com.example.ui.screens.StudyAnalysisScreen
import com.example.ui.screens.WlaTimeStudyScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PlayfulBackground
import com.example.viewmodel.Screen
import com.example.viewmodel.TimeStudyViewModel

class MainActivity : ComponentActivity() {
    private val vm: TimeStudyViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        vm.onAppForegrounded()
    }

    override fun onStop() {
        vm.onAppBackgrounded()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Screen Wake Lock: Keep screen on while engineer/observer is in the field
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = PlayfulBackground
                ) {
                    val currentScreen by vm.currentScreen.collectAsStateWithLifecycle()
                    val showOnboardingGuide by vm.showOnboardingGuide.collectAsStateWithLifecycle()

                    when (val screen = currentScreen) {
                        is Screen.Home -> {
                            HomeScreen(viewModel = vm)
                        }
                        is Screen.Setup -> {
                            SetupSessionScreen(mode = screen.mode, viewModel = vm)
                        }
                        is Screen.SingleStudy -> {
                            SingleTimeStudyScreen(sessionId = screen.sessionId, viewModel = vm)
                        }
                        is Screen.WlaStudy -> {
                            WlaTimeStudyScreen(sessionId = screen.sessionId, viewModel = vm)
                        }
                        is Screen.Database -> {
                            DatabaseScreen(viewModel = vm)
                        }
                        is Screen.StudyAnalysis -> {
                            StudyAnalysisScreen(studyName = screen.studyName, viewModel = vm)
                        }
                        is Screen.History -> {
                            HistoryScreen(viewModel = vm)
                        }
                        is Screen.SessionDetail -> {
                            SessionDetailScreen(sessionId = screen.sessionId, viewModel = vm)
                        }
                        is Screen.Settings -> {
                            SettingsScreen(viewModel = vm)
                        }
                    }

                    if (showOnboardingGuide) {
                        OnboardingGuideDialog(
                            onDismiss = { vm.dismissOnboardingGuide() }
                        )
                    }
                }
            }
        }
    }
}

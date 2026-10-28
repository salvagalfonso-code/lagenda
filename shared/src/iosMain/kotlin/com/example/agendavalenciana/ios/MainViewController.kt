package com.example.agendavalenciana.ios

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(bridge: IosSessionBridge): UIViewController = ComposeUIViewController { AgendaIosScreen(bridge) }

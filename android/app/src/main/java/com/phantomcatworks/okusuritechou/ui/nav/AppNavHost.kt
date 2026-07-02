package com.phantomcatworks.okusuritechou.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.phantomcatworks.okusuritechou.ui.screens.calendar.CalendarScreen
import com.phantomcatworks.okusuritechou.ui.screens.form.MedicineFormScreen
import com.phantomcatworks.okusuritechou.ui.screens.list.MedicineListScreen
import com.phantomcatworks.okusuritechou.ui.screens.notification.NotificationSettingScreen
import com.phantomcatworks.okusuritechou.ui.screens.ocr.OcrCaptureScreen
import com.phantomcatworks.okusuritechou.ui.screens.qr.QrDisplayScreen
import com.phantomcatworks.okusuritechou.ui.screens.qrscan.QrScanScreen
import com.phantomcatworks.okusuritechou.ui.screens.top.TopScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.TOP) {
        composable(Routes.TOP) {
            TopScreen(
                onNavigateToList = { navController.navigate(Routes.MEDICINE_LIST) },
                onNavigateToCalendar = { navController.navigate(Routes.CALENDAR) },
                onNavigateToQrScan = { navController.navigate(Routes.QR_SCAN) },
                onNavigateToNotificationSettings = { navController.navigate(Routes.NOTIFICATION_SETTINGS) }
            )
        }

        composable(Routes.MEDICINE_LIST) {
            MedicineListScreen(
                onNavigateToForm = { medicineId ->
                    navController.navigate(Routes.medicineFormRoute(medicineId))
                },
                onNavigateToQrDisplay = { medicineId ->
                    navController.navigate("qr_display/$medicineId")
                },
                onNavigateToOcrCapture = { navController.navigate(Routes.OCR_CAPTURE) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.MEDICINE_FORM,
            arguments = listOf(
                navArgument("medicineId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val medicineId = backStackEntry.arguments?.getString("medicineId")
            MedicineFormScreen(
                medicineId = medicineId,
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = "qr_display/{medicineId}",
            arguments = listOf(navArgument("medicineId") { type = NavType.StringType })
        ) { backStackEntry ->
            val medicineId = backStackEntry.arguments?.getString("medicineId") ?: return@composable
            QrDisplayScreen(medicineId = medicineId, onBack = { navController.popBackStack() })
        }

        composable(Routes.OCR_CAPTURE) {
            OcrCaptureScreen(
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Routes.QR_SCAN) {
            QrScanScreen(
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Routes.CALENDAR) {
            CalendarScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.NOTIFICATION_SETTINGS) {
            NotificationSettingScreen(onBack = { navController.popBackStack() })
        }
    }
}

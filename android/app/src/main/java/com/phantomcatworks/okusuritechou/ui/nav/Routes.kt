package com.phantomcatworks.okusuritechou.ui.nav

object Routes {
    const val TOP = "top"
    const val MEDICINE_LIST = "medicine_list"
    const val MEDICINE_FORM = "medicine_form?medicineId={medicineId}"
    const val OCR_CAPTURE = "ocr_capture"
    const val QR_SCAN = "qr_scan"
    const val CALENDAR = "calendar"
    const val NOTIFICATION_SETTINGS = "notification_settings"

    fun medicineFormRoute(medicineId: String? = null): String =
        if (medicineId == null) "medicine_form" else "medicine_form?medicineId=$medicineId"
}

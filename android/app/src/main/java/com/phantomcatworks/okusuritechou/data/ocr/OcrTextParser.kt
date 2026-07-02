package com.phantomcatworks.okusuritechou.data.ocr

import com.phantomcatworks.okusuritechou.data.db.entity.DoseForm
import com.phantomcatworks.okusuritechou.data.db.entity.FrequencyType

/**
 * お薬シール(薬局で渡される薬剤情報シール)のOCR結果から、
 * 薬名・1回服用量・服用タイミング・日数を抽出する簡易パーサー。
 *
 * 想定する文字列例:
 *   ロキソニン錠60mg
 *   1日3回 朝食後 昼食後 夕食後
 *   1回1錠
 *   14日分
 *
 * シールの書式は薬局によって差異が大きいため、これはベストエフォートの抽出であり、
 * 確認ダイアログで人間が最終確認することを前提とする。
 */
object OcrTextParser {

    data class ParsedSticker(
        val name: String?,
        val doseAmount: Double?,
        val timingMorning: Boolean,
        val timingNoon: Boolean,
        val timingNight: Boolean,
        val days: Int?,
        val rawText: String
    ) {
        /** 解析結果として「それらしい」情報が取れたか（自動遷移の判定に使用） */
        fun isConfident(): Boolean =
            !name.isNullOrBlank() && doseAmount != null && (timingMorning || timingNoon || timingNight)

        fun requiredTimingsCount(): Int =
            listOf(timingMorning, timingNoon, timingNight).count { it }

        /** 加算すべき残量 = 1回量 × 1日のタイミング数 × 日数 */
        fun computeAddQuantity(): Double? {
            val d = days ?: return null
            val amount = doseAmount ?: return null
            val timings = requiredTimingsCount()
            if (timings == 0) return null
            return amount * timings * d
        }
    }

    private val daysRegex = Regex("""(\d+)\s*日分""")
    private val perDoseRegex = Regex("""1回\s*(\d+(?:\.\d+)?)\s*(錠|カプセル|包|mL|ml|目盛)""")
    private val perDayCountRegex = Regex("""1日\s*(\d+)\s*回""")

    fun parse(rawText: String): ParsedSticker {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val days = daysRegex.find(rawText)?.groupValues?.get(1)?.toIntOrNull()

        val doseMatch = perDoseRegex.find(rawText)
        val doseAmount = doseMatch?.groupValues?.get(1)?.toDoubleOrNull()
        val unitText = doseMatch?.groupValues?.get(2)

        val timingMorning = rawText.contains("朝")
        val timingNoon = rawText.contains("昼") || rawText.contains("昼食")
        val timingNight = rawText.contains("夕") || rawText.contains("晩") || rawText.contains("就寝")

        // 「1日n回」しか書かれておらずタイミング語が無い場合は、回数からそれらしく朝昼晩に割当てる
        val perDayCount = perDayCountRegex.find(rawText)?.groupValues?.get(1)?.toIntOrNull()
        val (finalMorning, finalNoon, finalNight) = if (!timingMorning && !timingNoon && !timingNight && perDayCount != null) {
            when (perDayCount) {
                1 -> Triple(true, false, false)
                2 -> Triple(true, false, true)
                else -> Triple(true, true, true)
            }
        } else {
            Triple(timingMorning, timingNoon, timingNight)
        }

        // 薬名候補: 「日分」「1日」「1回」等のキーワードを含まない最初の行
        val keywordPattern = Regex("""日分|1日|1回|錠|カプセル|包|mg|食後|食前|就寝""")
        val nameCandidate = lines.firstOrNull { !keywordPattern.containsMatchIn(it) }

        return ParsedSticker(
            name = nameCandidate,
            doseAmount = doseAmount,
            timingMorning = finalMorning,
            timingNoon = finalNoon,
            timingNight = finalNight,
            days = days,
            rawText = rawText
        )
    }

    fun guessDoseForm(unitText: String?): DoseForm = when (unitText) {
        "mL", "ml", "目盛" -> DoseForm.injection
        null -> DoseForm.tablet
        else -> DoseForm.tablet
    }

    fun guessFrequencyType(): FrequencyType = FrequencyType.daily
}

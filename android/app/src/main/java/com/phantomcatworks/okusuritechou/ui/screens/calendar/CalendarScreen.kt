package com.phantomcatworks.okusuritechou.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phantomcatworks.okusuritechou.data.db.entity.IntakeLog
import com.phantomcatworks.okusuritechou.data.db.entity.Medicine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private data class DayResult(
    val date: LocalDate,
    val requiredCount: Int,
    val actualCount: Int
) {
    /** 必要回数が設定されているのに実績が満たない日。必要回数0(その日飲む薬がない)はOK扱い。 */
    val isShortfall: Boolean get() = requiredCount > 0 && actualCount < requiredCount
}

/**
 * カレンダー画面。
 * 各日について「その日服用すべき回数(有効な薬のうち該当日にdueなものの朝昼晩タイミング合計)」と
 * 「実際に記録された服用回数」を比較し、不足している日のセルを薄い赤で表示する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit) {
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    var days by remember { mutableStateOf<List<DayResult>>(emptyList()) }

    LaunchedEffect(yearMonth) {
        days = computeMonth(yearMonth)
    }

    Scaffold(
        topBar = { SmallTopAppBar(title = { Text("カレンダー") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { yearMonth = yearMonth.minusMonths(1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "前の月")
                        }
                        Text(
                            "${yearMonth.year}年 ${yearMonth.monthValue}月",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { yearMonth = yearMonth.plusMonths(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "次の月")
                        }
                    }
                }
            }

            WeekdayHeader()

            val leadingBlanks = (yearMonth.atDay(1).dayOfWeek.value % 7) // 月曜=1...日曜=7 -> 日曜始まりに変換
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxSize()
            ) {
                items(leadingBlanks) {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
                items(days) { day ->
                    DayCell(day)
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
        val labels = listOf(
            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        )
        labels.forEach { dow ->
            Text(
                dow.getDisplayName(TextStyle.SHORT, Locale.JAPAN),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DayCell(day: DayResult) {
    val bgColor = if (day.isShortfall) Color(0xFFFFCDD2) else Color.Transparent
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(bgColor)
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${day.date.dayOfMonth}")
            if (day.requiredCount > 0) {
                Text(
                    "${day.actualCount}/${day.requiredCount}",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private suspend fun computeMonth(yearMonth: YearMonth): List<DayResult> {
    val allMedicines = Medicine.all()

    val results = mutableListOf<DayResult>()
    var date = yearMonth.atDay(1)
    val end = yearMonth.atEndOfMonth()
    while (!date.isAfter(end)) {
        val dateStr = date.toString()
        val required = allMedicines.sumOf { medicine ->
            val registeredDate = java.time.Instant.ofEpochMilli(medicine.createdAt)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            if (medicine.isActive && medicine.isDueOn(date.toEpochDay(), registeredDate.toEpochDay())) {
                medicine.requiredTimingsCount()
            } else 0
        }
        val actual = IntakeLog.forDate(dateStr).size
        results += DayResult(date, required, actual)
        date = date.plusDays(1)
    }
    return results
}

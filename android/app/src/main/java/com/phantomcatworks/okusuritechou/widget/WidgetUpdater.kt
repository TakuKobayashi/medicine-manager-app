package com.phantomcatworks.okusuritechou.widget

import android.content.Context
import com.phantomcatworks.okusuritechou.widget.check.CheckWidget
import com.phantomcatworks.okusuritechou.widget.list.ListWidget

/** 薬や服用記録が変更された際に、ホーム画面のウィジェット表示を最新化するためのヘルパー。 */
object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        CheckWidget().updateAll(context)
        ListWidget().updateAll(context)
    }
}

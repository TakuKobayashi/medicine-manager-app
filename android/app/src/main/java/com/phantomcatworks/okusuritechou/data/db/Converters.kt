package com.phantomcatworks.okusuritechou.data.db

import androidx.room.TypeConverter
import com.phantomcatworks.okusuritechou.data.db.entity.DoseForm
import com.phantomcatworks.okusuritechou.data.db.entity.FrequencyType
import com.phantomcatworks.okusuritechou.data.db.entity.IntakeSource
import com.phantomcatworks.okusuritechou.data.db.entity.TimingSlot

class Converters {
    @TypeConverter fun toDoseForm(v: String) = DoseForm.valueOf(v)
    @TypeConverter fun fromDoseForm(v: DoseForm) = v.name

    @TypeConverter fun toFrequencyType(v: String) = FrequencyType.valueOf(v)
    @TypeConverter fun fromFrequencyType(v: FrequencyType) = v.name

    @TypeConverter fun toTimingSlot(v: String) = TimingSlot.valueOf(v)
    @TypeConverter fun fromTimingSlot(v: TimingSlot) = v.name

    @TypeConverter fun toIntakeSource(v: String) = IntakeSource.valueOf(v)
    @TypeConverter fun fromIntakeSource(v: IntakeSource) = v.name
}

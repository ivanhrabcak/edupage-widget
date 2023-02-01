package eu.hrabcak.edupagewidget

import eu.hrabcak.edupagewidget.edupage.Timetable
import io.github.reactivecircus.cache4k.Cache
import java.util.Date
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

object ApplicationCache {
    val cache = Cache.Builder.invoke().maximumCacheSize(2).build<String, Timetable>()
}
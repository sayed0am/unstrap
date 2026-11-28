package org.openstrap.app.health

import android.content.Context
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.openstrap.app.data.db.AppWorkout
import org.openstrap.app.data.db.BreathRate
import org.openstrap.app.data.db.HeartRateSample
import org.openstrap.app.data.db.OpenStrapDatabase
import org.openstrap.app.data.db.SkinTemperature
import org.openstrap.app.data.db.SleepEdit
import org.openstrap.app.data.db.SleepSession
import org.openstrap.app.data.db.SleepStage
import org.openstrap.app.data.db.Spo2Sample
import org.openstrap.app.data.db.SportEffectRow
import org.openstrap.app.data.db.Workout
import org.openstrap.app.testing.inMemoryDb
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneOffset

/**
 * What goes to Health Connect and back against a real database: heart rate written a whole hour at a time, and sleep
 * read back that the phone has at the user's times or deleted left out.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HealthConnectRoundTripTest {
    private lateinit var db: OpenStrapDatabase
    private val dao get() = db.healthDao()
    private lateinit var exporter: HealthConnectExporter
    private val h = 3600L

    @Before fun open() {
        db = inMemoryDb()
        exporter = HealthConnectExporter(ApplicationProvider.getApplicationContext<Context>(), dao)
    }

    @After fun close() = db.close()

    @Test fun `a range starting mid hour writes that hour whole`() = runTest {
        val at = listOf(100 * h + 300, 100 * h + 1200, 100 * h + 3000, 101 * h + 600, 101 * h + 2400)
        dao.insertHeartRateIfMissing(at.map { HeartRateSample(it, 70) })
        // From 10:30 to 11:15, as an auto export two days back from now would start.
        val records = exporter.heartRate(100 * h + 1800, 101 * h + 900).map { it as HeartRateRecord }
        assertEquals(listOf("openstrap:hr:${100 * h}", "openstrap:hr:${101 * h}"), records.map { it.metadata.clientRecordId })
        assertEquals(at, records.flatMap { r -> r.samples.map { it.time.epochSecond } })
        assertEquals(Instant.ofEpochSecond(100 * h), records.first().startTime)
    }

    private val strapNight = SleepSession(100 * h, 108 * h, day = 0, score = 80)

    private fun sleep(start: Long, end: Long, strapStart: Long?, stages: List<SleepSessionRecord.Stage> = emptyList()) = SleepSessionRecord(
        startTime = Instant.ofEpochSecond(start), startZoneOffset = ZoneOffset.UTC,
        endTime = Instant.ofEpochSecond(end), endZoneOffset = ZoneOffset.UTC,
        stages = stages,
        metadata = strapStart?.let { Metadata.autoRecorded(device = band, clientRecordId = "openstrap:sleep:$it") } ?: Metadata.autoRecorded(band),
    )

    private val band = Device(type = Device.TYPE_FITNESS_BAND)

    /** Imports [records] as Health Connect would hand them back, with nothing else in it. */
    private suspend fun import(vararg records: Record) = exporter.importRange { type -> records.filter { type.isInstance(it) } }

    private suspend fun sessions() = dao.sleepSessionsOnce(0, 1000 * h)

    @Test fun `a night moved in OpenStrap doesn't come back as a second night`() = runTest {
        dao.insertSleepSessions(listOf(strapNight))
        dao.upsertSleepEdit(SleepEdit(strapNight.start, nap = false, start = 101 * h, end = 109 * h, editedAt = 1))
        // Exported at the user's times, under the strap's start.
        val stage = SleepSessionRecord.Stage(Instant.ofEpochSecond(101 * h), Instant.ofEpochSecond(109 * h), SleepSessionRecord.STAGE_TYPE_LIGHT)
        import(sleep(101 * h, 109 * h, strapNight.start, listOf(stage)))
        assertEquals(listOf(strapNight), sessions())
        assertTrue(dao.sleepStagesOnce(101 * h).isEmpty())
    }

    @Test fun `a sleep overlapping the phone's at either times stays out, whatever its id`() = runTest {
        dao.insertSleepSessions(listOf(strapNight))
        dao.upsertSleepEdit(SleepEdit(strapNight.start, nap = false, start = 102 * h, end = 110 * h, editedAt = 1))
        // Overlaps the strap's times only, then the user's only.
        import(sleep(99 * h, 101 * h, null), sleep(108 * h + 1800, 111 * h, null))
        assertEquals(listOf(strapNight), sessions())
    }

    @Test fun `a sleep the user deleted stays out after its session has gone for good`() = runTest {
        // The marker a delete leaves once its session is removed ([SleepEdits.marker]).
        dao.upsertSleepEdit(SleepEdit(strapNight.start, nap = false, deleted = true, editedAt = 1))
        import(sleep(101 * h, 109 * h, strapNight.start), sleep(100 * h + 300, 108 * h, null))
        assertTrue(sessions().isEmpty())
    }

    @Test fun `a sleep the user deleted and moved stays out while its session is kept`() = runTest {
        dao.insertSleepSessions(listOf(strapNight))
        dao.upsertSleepEdit(SleepEdit(strapNight.start, nap = false, start = 101 * h, end = 109 * h, deleted = true, editedAt = 1))
        import(sleep(101 * h, 109 * h, strapNight.start))
        assertEquals(listOf(strapNight), sessions())
    }

    @Test fun `a night the phone doesn't have comes in with its stages`() = runTest {
        dao.insertSleepSessions(listOf(strapNight))
        val stage = SleepSessionRecord.Stage(Instant.ofEpochSecond(124 * h), Instant.ofEpochSecond(130 * h), SleepSessionRecord.STAGE_TYPE_DEEP)
        import(sleep(124 * h, 132 * h, 124 * h, listOf(stage)))
        assertEquals(listOf(100 * h, 124 * h), sessions().map { it.start })
        assertEquals(listOf(124 * h to 130 * h), dao.sleepStagesOnce(124 * h).map { it.start to it.end })
    }

    @Test fun `the strap's start is read from an exported sleep's id`() {
        assertEquals(360_000L, SleepImport.strapStart("openstrap:sleep:360000"))
        assertEquals(null, SleepImport.strapStart("openstrap:hr:360000"))
        assertEquals(null, SleepImport.strapStart(null))
    }

    @Test fun `every breathing rate sample is exported as it was stored`() = runTest {
        dao.insertBreathRate(listOf(BreathRate(100 * h, 14), BreathRate(100 * h + 60, 9), BreathRate(300 * h, 16)))
        val records = exporter.breathing(100 * h, 101 * h).map { it as RespiratoryRateRecord }
        assertEquals(listOf("openstrap:breath:${100 * h}", "openstrap:breath:${100 * h + 60}"), records.map { it.metadata.clientRecordId })
        assertEquals(listOf(14.0, 9.0), records.map { it.rate })
        assertEquals(Instant.ofEpochSecond(100 * h), records.first().time)
    }

    @Test fun `blood oxygen is exported from 80 to 100 only, as it is stored`() = runTest {
        dao.insertSpo2(listOf(0, 1, 70, 79, 80, 100, 101, 127).mapIndexed { i, p -> Spo2Sample(100 * h + i * 60, p) })
        val records = exporter.bloodOxygen(100 * h, 101 * h).map { it as OxygenSaturationRecord }
        assertEquals(listOf(80.0, 100.0), records.map { it.percentage.value })
    }

    /** A night of [hours] from [start] with a reading a minute at [celsius]. */
    private suspend fun night(start: Long, celsius: Float, hours: Int = 8, minutes: Int = 40, nap: Boolean = false) {
        dao.insertSleepSessions(listOf(SleepSession(start, start + hours * h, day = 0, nap = nap)))
        dao.insertSkinTemperature(List(minutes) { SkinTemperature(start + it * 60, celsius) })
    }

    private suspend fun skin(from: Long, to: Long) =
        exporter.skinTemperature(from, to, now = 5_000).map { it as SkinTemperatureRecord }

    @Test fun `skin temperature goes out a night at a time against the usual night`() = runTest {
        // Seven earlier nights, one of them far off, then the night written: their median is the baseline.
        listOf(33f, 33.2f, 33.4f, 33.6f, 33.8f, 34f, 39f).forEachIndexed { i, c -> night(100 * h + i * 24 * h, c) }
        val start = 100 * h + 7 * 24 * h
        night(start, 35f)
        // Awake readings, and one at the very end, are not part of the night.
        dao.insertSkinTemperature(listOf(SkinTemperature(start - 60, 30f), SkinTemperature(start + 8 * h, 30f)))
        val record = skin(start + 24 * h, start + 25 * h).single()
        assertEquals("openstrap:skintemp:$start", record.metadata.clientRecordId)
        assertEquals(5_000L, record.metadata.clientRecordVersion)
        assertEquals(33.6, record.baseline!!.inCelsius, 0.001)
        assertEquals(40, record.deltas.size)
        assertEquals(35.0, record.baseline!!.inCelsius + record.deltas.first().delta.inCelsius, 0.001)
        assertEquals(SkinTemperatureRecord.MEASUREMENT_LOCATION_WRIST, record.measurementLocation)
    }

    @Test fun `with under seven earlier nights the night itself counts towards its baseline`() = runTest {
        night(100 * h, 33f)
        night(124 * h, 35f)
        assertEquals(listOf(33.0, 34.0), skin(100 * h, 140 * h).map { it.baseline!!.inCelsius })
    }

    @Test fun `a night's baseline is the same whatever range is written`() = runTest {
        repeat(10) { night(100 * h + it * 24 * h, 33f + it * 0.1f) }
        val last = 100 * h + 9 * 24 * h
        val whole = skin(100 * h, last + 9 * h).last()
        val alone = skin(last + 25 * h, last + 26 * h).single()
        assertEquals(whole.baseline, alone.baseline)
        assertEquals(whole.metadata.clientRecordId, alone.metadata.clientRecordId)
    }

    @Test fun `naps, deleted sleeps and nights with few readings send no skin temperature`() = runTest {
        night(100 * h, 33f, hours = 2, nap = true)
        night(124 * h, 33f, minutes = 29)
        night(148 * h, 33f)
        dao.upsertSleepEdit(SleepEdit(148 * h, nap = false, deleted = true, editedAt = 1))
        assertTrue(skin(100 * h, 160 * h).isEmpty())
    }

    @Test fun `a moved night sends the readings of its new times under the strap's id`() = runTest {
        night(100 * h, 33f, minutes = 120)
        dao.upsertSleepEdit(SleepEdit(100 * h, nap = false, start = 101 * h, end = 108 * h, editedAt = 1))
        val record = skin(100 * h, 110 * h).single()
        assertEquals("openstrap:skintemp:${100 * h}", record.metadata.clientRecordId)
        assertEquals(Instant.ofEpochSecond(101 * h), record.startTime)
        assertEquals(60, record.deltas.size)
    }

    private fun strap(start: Long, end: Long, sport: Int = 1, vo2max: Int? = null) = Workout(
        start, end, sport, exerciseSeconds = null, calories = null, averageHeartRate = null, maxHeartRate = null, minHeartRate = null,
        aerobicTrainingEffect = null, anaerobicTrainingEffect = null, trainingLoad = null, vo2max = vo2max, steps = null,
    )

    private fun effect(end: Long, vo2max: Float?) = SportEffectRow(end, null, vo2max, null, null, null, null, null, null)

    @Test fun `the strap's VO2 max is exported at the workout's end, from the workout or else its effect`() = runTest {
        // A day apart, so no workout takes another's effect.
        dao.insertWorkouts(listOf(strap(100 * h, 101 * h, vo2max = 48), strap(130 * h, 131 * h), strap(160 * h, 161 * h, vo2max = 0), strap(190 * h, 191 * h, vo2max = 101)))
        dao.insertSportEffects(listOf(effect(131 * h + 120, 52.4f), effect(161 * h + 120, 0f)))
        val records = exporter.vo2max(90 * h, 240 * h).map { it as Vo2MaxRecord }
        assertEquals(listOf("openstrap:vo2max:${100 * h}", "openstrap:vo2max:${130 * h}"), records.map { it.metadata.clientRecordId })
        assertEquals(listOf(48.0, 52.0), records.map { it.vo2MillilitersPerMinuteKilogram })
        assertEquals(listOf(Instant.ofEpochSecond(101 * h), Instant.ofEpochSecond(131 * h + 120)), records.map { it.time })
        assertTrue(records.all { it.measurementMethod == Vo2MaxRecord.MEASUREMENT_METHOD_OTHER })
    }

    @Test fun `unstrap's own VO2 max estimate is not exported`() = runTest {
        dao.insertAppWorkouts(listOf(AppWorkout(start = 100 * h, end = 101 * h, sportType = 1, vo2max = 45f)))
        assertTrue(exporter.vo2max(90 * h, 140 * h).isEmpty())
    }

    @Test fun `a strap workout is exported as a session of its own`() = runTest {
        dao.insertWorkouts(listOf(strap(100 * h, 101 * h, sport = 1), strap(110 * h, 110 * h)))
        val records = exporter.strapWorkouts(90 * h, 140 * h).map { it as ExerciseSessionRecord }
        // The empty one is left out: Health Connect rejects a session that doesn't end after it starts.
        assertEquals(listOf("openstrap:strapworkout:${100 * h}"), records.map { it.metadata.clientRecordId })
        assertEquals(Instant.ofEpochSecond(100 * h), records.single().startTime)
        assertEquals(Instant.ofEpochSecond(101 * h), records.single().endTime)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, records.single().exerciseType)
    }

    @Test fun `a strap workout overlapping a phone workout is left out`() = runTest {
        dao.insertAppWorkouts(listOf(AppWorkout(start = 100 * h + 30, end = 101 * h, sportType = 1)))
        // The strap's own session of it starts a little after, the next one is clear of it.
        dao.insertWorkouts(listOf(strap(100 * h + 40, 101 * h + 60), strap(102 * h, 103 * h)))
        assertEquals(listOf("openstrap:strapworkout:${102 * h}"), exporter.strapWorkouts(90 * h, 140 * h).map { it.metadata.clientRecordId })
    }

    @Test fun `a strap workout a phone workout now overlaps loses its record`() = runTest {
        val written = ExerciseSessionRecord(
            startTime = Instant.ofEpochSecond(100 * h), startZoneOffset = ZoneOffset.UTC,
            endTime = Instant.ofEpochSecond(101 * h), endZoneOffset = ZoneOffset.UTC, exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            metadata = Metadata.autoRecorded(device = band, clientRecordId = "openstrap:strapworkout:${100 * h}"),
        )
        val phone = ExerciseSessionRecord(
            startTime = Instant.ofEpochSecond(100 * h), startZoneOffset = ZoneOffset.UTC,
            endTime = Instant.ofEpochSecond(101 * h), endZoneOffset = ZoneOffset.UTC, exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            metadata = Metadata.autoRecorded(device = band, clientRecordId = "openstrap:workout:${100 * h}"),
        )
        val deleted = mutableListOf<String>()
        val store = object : RecordStore {
            override suspend fun insert(records: List<Record>) = Unit
            override suspend fun ownRecords(type: kotlin.reflect.KClass<out Record>, range: androidx.health.connect.client.time.TimeRangeFilter) = listOf<Record>(written, phone)
            override suspend fun deleteIds(type: kotlin.reflect.KClass<out Record>, ids: List<String>) { deleted += ids.map { it } }
            override suspend fun deleteRange(type: kotlin.reflect.KClass<out Record>, range: androidx.health.connect.client.time.TimeRangeFilter) = Unit
        }
        // Nothing of the strap's is written any more: only its own session goes, the phone's stays.
        store.dropStaleStrapWorkouts(90 * h, 140 * h, emptyList())
        assertEquals(listOf(written.metadata.id), deleted)
    }
}

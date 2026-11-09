package com.widgy.lyrics.widget

import android.content.Context
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.widgy.lyrics.data.preferences.WidgetPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WidgetUpdateManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun dailyLyricWidget_usesPreferencesGlanceStateDefinition() {
        val widget = DailyLyricWidget()
        assertNotNull("DailyLyricWidget must define stateDefinition so Glance recomposes on update", widget.stateDefinition)
        assertEquals(PreferencesGlanceStateDefinition, widget.stateDefinition)
    }

    @Test
    fun widgetPreferences_storesAndRetrievesInterval() {
        val prefs = WidgetPreferences(context)
        assertEquals(24, prefs.getRotationIntervalHours())

        prefs.setRotationIntervalHours(6)
        assertEquals(6, prefs.getRotationIntervalHours())

        // Invalid interval falls back to 24
        prefs.setRotationIntervalHours(99)
        assertEquals(24, prefs.getRotationIntervalHours())
    }

    @Test
    fun scheduleRotation_enqueuesWorkManagerTaskWithoutCrashing() {
        WidgetUpdateManager.scheduleRotation(context, 1)
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WidgetUpdateManager.WORK_NAME)
            .get()

        assertEquals(1, workInfos.size)
    }

    @Test
    fun widgetUpdateManager_definesTimestampKey() {
        assertEquals("widget_update_timestamp", WidgetUpdateManager.UPDATE_TIMESTAMP_KEY.name)
    }

    @Test
    fun dailyLyricWidgetReceiver_hasCorrectWidget() {
        val receiver = DailyLyricWidgetReceiver()
        assertNotNull(receiver.glanceAppWidget)
        assertEquals(DailyLyricWidget::class.java, receiver.glanceAppWidget.javaClass)
    }

    @Test
    fun notifyWidgetUpdate_runsSafely() {
        WidgetUpdateManager.notifyWidgetUpdate(context)
    }

    @Test
    fun updateAllWidgets_runsWithoutCrashing() = runBlocking {
        WidgetUpdateManager.updateAllWidgets(context)
    }
}

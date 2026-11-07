package com.widgy.lyrics.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.widgy.lyrics.data.preferences.WidgetPreferences
import org.junit.Assert.assertEquals
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
}

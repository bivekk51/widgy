package com.widgy.lyrics.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DailyWidgetWorkerTest {
    @Test
    fun doWork_resolvesLyricAndReturnsSuccess() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder.from(context, DailyWidgetWorker::class.java).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }
}

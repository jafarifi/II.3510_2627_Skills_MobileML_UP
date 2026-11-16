package com.isep.swapcampus

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.isep.swapcampus.ml.BarcodeHelper
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BarcodeHelperTest {
    private fun read(asset: String): List<String> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val file = File(context.cacheDir, asset)
        instrumentation.context.assets.open(asset).use { input -> file.outputStream().use { input.copyTo(it) } }
        val done = CountDownLatch(1)
        val values = AtomicReference<List<String>>(emptyList())
        val error = AtomicReference<Exception?>()
        instrumentation.runOnMainSync {
            BarcodeHelper.scanImage(context, Uri.fromFile(file),
                onResult = { values.set(it); done.countDown() },
                onError = { error.set(it); done.countDown() }
            )
        }
        assertTrue("ML Kit did not finish within 30 seconds", done.await(30, TimeUnit.SECONDS))
        error.get()?.let { throw AssertionError("Scan failed", it) }
        return values.get()
    }
    @Test fun readsAnEan13BookCode() {
        assertEquals(listOf("9780134685991"), read("isbn.png"))
    }
    @Test fun anImageWithoutCodeReturnsNoResult() {
        assertTrue(read("blank.png").isEmpty())
    }
}

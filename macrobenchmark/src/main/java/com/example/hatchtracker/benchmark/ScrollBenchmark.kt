package com.example.hatchtracker.benchmark

import android.content.Intent
import androidx.benchmark.macro.*
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollNurseryFlockList() = benchmarkRule.measureRepeated(
        packageName = "com.example.hatchtracker",
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.DEFAULT,
        startupMode = StartupMode.WARM,
        iterations = 5
    ) {

        val intent = Intent().apply {
            setClassName(
                "com.example.hatchtracker",
                "com.example.hatchtracker.app.MainActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        pressHome()
        startActivityAndWait(intent)

        device.wait(Until.hasObject(By.desc("app_root")), 10000)

        // navigate to nursery
        device.wait(Until.hasObject(By.descContains("Nursery")), 5000)
        device.findObject(By.descContains("Nursery"))?.click()

        device.wait(
            Until.hasObject(By.res("com.example.hatchtracker","flock_list")),
            5000
        )

        val list = device.findObject(By.res("com.example.hatchtracker","flock_list"))

        list?.setGestureMargin(device.displayWidth / 5)

        list?.fling(Direction.DOWN)
        device.waitForIdle()

        list?.fling(Direction.UP)
        device.waitForIdle()
    }
}
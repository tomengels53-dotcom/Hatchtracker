package com.example.hatchtracker.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Cold startup benchmark using CompilationMode.None for worst-case measurement.
     *
     * Uses setupBlock + startActivityAndWait() + UiAutomator wait instead of
     * relying on startActivityAndWait() alone, which has a known incompatibility
     * with SplashScreen 1.0.x on Android 16 (API 36) causing amStartAndWait timeouts.
     *
     * See: https://issuetracker.google.com/issues/329156278
     */
    @Test
    fun startupCold() = benchmarkRule.measureRepeated(
        packageName = "com.example.hatchtracker",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.None(),
        iterations = 5,
        startupMode = StartupMode.COLD,
        setupBlock = {
            pressHome()
        }
    ) {
        startActivityAndWait()

        // Wait for the main app surface marked with contentDescription="app_root" in MainActivity
        device.wait(
            Until.hasObject(By.desc("app_root")),
            10_000
        )
    }

    /**
     * Warm startup benchmark (process already alive, activity re-created).
     */
    @Test
    fun startupWarm() = benchmarkRule.measureRepeated(
        packageName = "com.example.hatchtracker",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.None(),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
        }
    ) {
        startActivityAndWait()

        device.wait(
            Until.hasObject(By.desc("app_root")),
            10_000
        )
    }
}
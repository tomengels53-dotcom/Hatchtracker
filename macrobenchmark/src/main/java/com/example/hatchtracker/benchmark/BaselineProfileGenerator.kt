package com.example.hatchtracker.benchmark

import android.content.Intent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.example.hatchtracker"
    ) {

        val intent = Intent().apply {
            setClassName(
                "com.example.hatchtracker",
                "com.example.hatchtracker.app.MainActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivityAndWait(intent)

        device.wait(Until.hasObject(By.desc("app_root")), 10000)

        // navigate to nursery
        device.wait(Until.hasObject(By.descContains("Nursery")), 5000)
        device.findObject(By.descContains("Nursery"))?.click()

        device.wait(
            Until.hasObject(By.res("com.example.hatchtracker","flock_list")),
            5000
        )
    }
}
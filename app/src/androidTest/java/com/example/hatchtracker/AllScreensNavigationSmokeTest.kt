package com.example.hatchtracker

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.example.hatchtracker.app.MainActivity
import com.example.hatchtracker.core.ui.R as UiR
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AllScreensNavigationSmokeTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var context: Context
    private lateinit var device: UiDevice
    private lateinit var backLabel: String
    private var hasAuthenticatedSession = false

    @Before
    fun setupSessionAndOnboarding() {
        context = instrumentation.targetContext
        device = UiDevice.getInstance(instrumentation)
        backLabel = context.getString(UiR.string.back_action)

        context
            .getSharedPreferences("hatchtracker_onboarding", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("has_seen_welcome", true)
            .putBoolean("has_completed_onboarding", true)
            .commit()

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            hasAuthenticatedSession = true
        } else {
            hasAuthenticatedSession = try {
                Tasks.await(auth.signInAnonymously(), 30, TimeUnit.SECONDS)
                auth.currentUser != null
            } catch (_: Exception) {
                false
            }
        }
    }

    @Test
    fun navigateCoreModules_withoutCrash() {
        val launchIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        instrumentation.startActivitySync(launchIntent)

        clickGetStartedIfVisible()
        val flockTile = context.getString(UiR.string.module_flock)

        if (hasAuthenticatedSession && device.wait(Until.hasObject(By.text(flockTile)), 8_000)) {
            openModuleAndReturn(flockTile, "Flock")
            openModuleAndReturn(context.getString(UiR.string.module_incubation), "Incubation")
            openModuleAndReturn(context.getString(UiR.string.module_nursery), "Nursery")

            // Breeding can be locked by tier. Only assert navigation if it opens.
            waitAndClickText(context.getString(UiR.string.module_breeding), 5_000)
            if (device.wait(Until.hasObject(By.text("Breeding")), 3_000)) {
                waitAndClickDesc(backLabel, 5_000)
                waitForText(flockTile, 5_000)
            }
        } else {
            // Fallback flow when auth is unavailable (e.g., anonymous auth disabled in backend).
            val signInText = context.getString(UiR.string.auth_button_login)
            val gotoSignupText = context.getString(UiR.string.auth_button_goto_signup)
            val signupText = context.getString(UiR.string.auth_button_signup)
            val gotoLoginText = context.getString(UiR.string.auth_button_goto_login)

            if (device.wait(Until.hasObject(By.text(signInText)), 12_000)) {
                waitAndClickText(gotoSignupText, 10_000)
                waitForText(signupText, 10_000)
                waitAndClickText(gotoLoginText, 10_000)
                waitForText(signInText, 10_000)
            }
        }

        // Let instrumentation lifecycle handle teardown to avoid StrictMode explicit-GC violations.
    }

    private fun openModuleAndReturn(tileText: String, headerText: String) {
        waitAndClickText(tileText, 10_000)
        waitForText(headerText, 10_000)
        waitAndClickDesc(backLabel, 10_000)
        waitForText(context.getString(UiR.string.module_flock), 10_000)
    }

    private fun waitForText(text: String, timeoutMs: Long) {
        check(device.wait(Until.hasObject(By.text(text)), timeoutMs)) {
            "Timed out waiting for text: $text"
        }
    }

    private fun waitAndClickText(text: String, timeoutMs: Long) {
        waitForText(text, timeoutMs)
        val node = device.findObject(By.text(text))
        check(node != null) { "Unable to find text node: $text" }
        node.click()
    }

    private fun waitAndClickDesc(desc: String, timeoutMs: Long) {
        check(device.wait(Until.hasObject(By.desc(desc)), timeoutMs)) {
            "Timed out waiting for contentDescription: $desc"
        }
        val node = device.findObject(By.desc(desc))
        check(node != null) { "Unable to find description node: $desc" }
        node.click()
    }

    private fun clickGetStartedIfVisible() {
        if (device.wait(Until.hasObject(By.text("Get Started")), 2_500)) {
            val node = device.findObject(By.text("Get Started"))
            check(node != null) { "Unable to find text node: Get Started" }
            node.click()
        }
    }
}

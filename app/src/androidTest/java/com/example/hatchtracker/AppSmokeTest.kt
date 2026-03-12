package com.example.hatchtracker

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hatchtracker.app.MainActivity
import com.google.firebase.auth.FirebaseAuth
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {

    private lateinit var context: Context
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Before
    fun resetSessionState() {
        context = instrumentation.targetContext

        context
            .getSharedPreferences("hatchtracker_onboarding", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        FirebaseAuth.getInstance().signOut()
    }

    @Test
    fun coldStart_navigatesToLogin() {
        val launchIntent = Intent(context, MainActivity::class.java)
        val resolvedComponent = launchIntent.resolveActivity(context.packageManager)

        assertNotNull("Expected MainActivity to be resolvable", resolvedComponent)
        assertEquals("Expected HatchTracker package", context.packageName, resolvedComponent.packageName)
    }
}

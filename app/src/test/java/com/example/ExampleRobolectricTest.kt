package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MYRA", appName)
    }

    @Test
    fun `command router handles stop command`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = MyraCommandRouter.execute(context, "stop", "en-IN")
        assertTrue(result.handled)
        assertTrue(result.requestStop)
        assertNotNull(result.serviceResult)
    }

    @Test
    fun `command router handles time command`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = MyraCommandRouter.execute(context, "what is the time", "en-IN")
        assertTrue(result.handled)
        assertEquals("TIME", result.actionType)
        assertTrue(result.replyText.contains("The current time is"))
    }

    @Test
    fun `command router handles math expressions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = MyraCommandRouter.execute(context, "what is 25 * 4", "en-IN")
        assertTrue(result.handled)
        assertEquals("CALCULATION", result.actionType)
        assertTrue(result.replyText.contains("100"))
    }

    @Test
    fun `command router handles settings command`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = MyraCommandRouter.execute(context, "Settings खोलो", "hi-IN")
        assertTrue(result.handled)
        assertEquals("SETTINGS", result.actionType)
        assertNotNull(result.actionIntent)
    }

    @Test
    fun `command router handles youtube command`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = MyraCommandRouter.execute(context, "YouTube खोलो", "hi-IN")
        assertTrue(result.handled)
        assertEquals("YOUTUBE", result.actionType)
        assertNotNull(result.actionIntent)
    }

    @Test
    fun `command router handles maps command`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = MyraCommandRouter.execute(context, "मैप खोलो", "hi-IN")
        assertTrue(result.handled)
        assertEquals("MAPS", result.actionType)
        assertNotNull(result.actionIntent)
    }

    @Test
    fun `myra result data model instantiates correctly`() {
        val res = MyraResult(success = true, message = "Operation complete", payload = "data")
        assertTrue(res.success)
        assertEquals("Operation complete", res.message)
        assertEquals("data", res.payload)
    }

    @Test
    fun `preferences stores and retrieves custom api keys and personality mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MyraPreferences.setGeminiApiKey(context, "test_gemini_key_123")
        assertEquals("test_gemini_key_123", MyraPreferences.getGeminiApiKey(context))

        MyraPreferences.setOpenAiApiKey(context, "test_openai_key_789")
        assertEquals("test_openai_key_789", MyraPreferences.getOpenAiApiKey(context))

        MyraPreferences.setAiProvider(context, "OPENAI")
        assertEquals("OPENAI", MyraPreferences.getAiProvider(context))

        MyraPreferences.setElevenLabsApiKey(context, "test_eleven_key_456")
        assertEquals("test_eleven_key_456", MyraPreferences.getElevenLabsApiKey(context))

        MyraPreferences.setPersonalityMode(context, "NAKHRE")
        assertEquals("NAKHRE", MyraPreferences.getPersonalityMode(context))
    }

    @Test
    fun `app automation handles app launch queries`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val res = MyraAppAutomation.tryAutomateApp(context, "open chrome", "en-IN")
        assertNotNull(res)
        assertTrue(res!!.success)
    }
}

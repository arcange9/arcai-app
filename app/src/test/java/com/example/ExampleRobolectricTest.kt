package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AiProvider
import com.example.service.ProviderVerificationService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    assertEquals("ArcAI Assistant", appName)
  }

  @Test
  fun `verify empty key returns invalid`() = runBlocking {
    val result = ProviderVerificationService.validateBeforeSave(AiProvider.OPENAI, "   ")
    assertFalse(result.isValid)
    assertEquals("API Key / URL cannot be empty.", result.message)
  }

  @Test
  fun `verify custom syntax provider validates length`() = runBlocking {
    val validBedrock = ProviderVerificationService.validateBeforeSave(AiProvider.AMAZON_BEDROCK, "AKIAIOSFODNN7EXAMPLE")
    assertTrue(validBedrock.isValid)
    assertEquals(200, validBedrock.statusCode)

    val invalidBedrock = ProviderVerificationService.validateBeforeSave(AiProvider.AMAZON_BEDROCK, "short")
    assertFalse(invalidBedrock.isValid)
    assertEquals(400, invalidBedrock.statusCode)
  }
}

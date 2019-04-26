package com.simplify.android.sdk

import android.app.Activity
import android.content.Intent
import com.nhaarman.mockitokotlin2.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SimplifyTest {

    private lateinit var simplify: Simplify

    @Mock
    private lateinit var mockComms: SimplifyComms

    @Spy
    private lateinit var spy3DSCallback: SimplifySecure3DCallback


    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        simplify = Simplify("sbpb_M2E2YTJkOTctMDEwZS00MjViLWJhZWItZmI1Yjg1NTMxMDk3")
        simplify.comms = mockComms
    }

    @After
    fun tearDown() {
        reset(mockComms,  spy3DSCallback)
    }

    @Test
    fun testApiKeyPassedInConstructorIsValidated() {
        val goodPrefix = "lvpb_"
        val badKeyId = "bm90IGEgdXVpZA==" // "not a uuid"

        try {
            Simplify(goodPrefix + badKeyId)
            fail("Should have thrown exception with invalid key format in constructor")
        } catch (e: Exception) {
            assertTrue(e is IllegalArgumentException)
        }
    }

    @Test
    fun testSetApiKeyThrowsExceptionWhenIncorrectFormat() {
        val goodPrefix = "lvpb_"
        val badKeyId = "bm90IGEgdXVpZA==" // "not a uuid"

        try {
            simplify.apiKey = goodPrefix + badKeyId
            fail("Should have thrown exception with invalid key format")
        } catch (e: Exception) {
            assertTrue(e is IllegalArgumentException)
        }
    }

    @Test
    fun testSetApiKeyThrowsExceptionOnBadPrefix() {
        val badPrefix = "bogus_"
        val goodKeyId = "M2E2YTJkOTctMDEwZS00MjViLWJhZWItZmI1Yjg1NTMxMDk3"

        try {
            simplify.apiKey = badPrefix + goodKeyId
            fail("Should have thrown exception with invalid key prefix")
        } catch (e: Exception) {
            assertTrue(e is IllegalArgumentException)
        }
    }

    @Test
    fun testSetApiKeyWorksWithGoodKey() {
        val goodPrefix = "lvpb_"
        val goodKeyId = "M2E2YTJkOTctMDEwZS00MjViLWJhZWItZmI1Yjg1NTMxMDk3"

        try {
            simplify.apiKey = goodPrefix + goodKeyId
            assertEquals(goodPrefix + goodKeyId, simplify.apiKey)
        } catch (e: Exception) {
            fail("Should have passed all validation rules")
        }
    }

    @Test
    fun testCorrectBaseUrlUsedForKeyType() {
        val livePrefix = "lvpb_"
        val sandboxPrefix = "sbpb_"
        val keyId = "M2E2YTJkOTctMDEwZS00MjViLWJhZWItZmI1Yjg1NTMxMDk3"

        simplify.apiKey = sandboxPrefix + keyId
        simplify.createCardToken(SimplifyMap(), null, mock())

        simplify.apiKey = livePrefix + keyId
        simplify.createCardToken(SimplifyMap(), null, mock())

        argumentCaptor<SimplifyRequest>().apply {
            verify(mockComms, times(2)).runSimplifyRequest(capture(), any())

            assertTrue(firstValue.url.startsWith(Simplify.API_BASE_SANDBOX_URL))
            assertTrue(secondValue.url.startsWith(Simplify.API_BASE_LIVE_URL))
        }
    }

    @Test
    fun testRequestBuiltCorrectlyForCreateCardToken() {
        val apiKey = "lvpb_M2E2YTJkOTctMDEwZS00MjViLWJhZWItZmI1Yjg1NTMxMDk3"

        val card = SimplifyMap()
                .set("number", "5555555555554444")
                .set("expMonth", "01")
                .set("expYear", "50")
                .set("cvc", "123")

        val secure3DRequestData = SimplifyMap()
                .set("paReq", "some data")

        simplify.apiKey = apiKey
        simplify.createCardToken(card, null, mock())
        simplify.createCardToken(card, secure3DRequestData, mock())

        argumentCaptor<SimplifyRequest>().apply {
            verify(mockComms, times(2)).runSimplifyRequest(capture(), any())

            // common assertions
            assertTrue(firstValue.url.endsWith(Simplify.API_PATH_CARDTOKEN))
            assertEquals(SimplifyRequest.Method.POST, firstValue.method)
            assertEquals(apiKey, firstValue.payload["key"])
            assertEquals(card["number"], firstValue.payload["card.number"])

            // first request, missing 3DS data
            assertNull(firstValue.payload["secure3DRequestData"])

            // second request, includes 3DS data
            assertEquals(secure3DRequestData["paReq"], secondValue.payload["secure3DRequestData.paReq"])
        }
    }

    @Test
    fun test3DSUnrecognizedRequestCodeReturnsFalse() {
        val unrecognizedRequestCode = 123

        val result = Simplify.handle3DSResult(unrecognizedRequestCode, Activity.RESULT_OK, mock(), mock())

        assertFalse("Should return false on unrecognized activity request code") { result }
    }

    @Test
    fun test3DSResultCancelledCallsCancelledMethodOnCallback() {
        val result = Simplify.handle3DSResult(Simplify.REQUEST_CODE_3DS, Activity.RESULT_CANCELED, mock(), spy3DSCallback)

        verify(spy3DSCallback).onSecure3DCancel()
        assertTrue("Should return true when handling cancel event") { result }
    }

    @Test
    fun testErrorReading3DSResultCallsErrorOnCallback() {
        val mockData: Intent = mock()
        whenever(mockData.getStringExtra(SimplifySecure3DActivity.EXTRA_RESULT)).doThrow(RuntimeException())

        val result = Simplify.handle3DSResult(Simplify.REQUEST_CODE_3DS, Activity.RESULT_OK, mockData, spy3DSCallback)

        verify(spy3DSCallback).onSecure3DError(any())
        assertTrue("Should return true when handling error event") { result }
    }

    @Test
    fun testMissingSecure3dResponseDataCallsErrorOnCallback() {
        val mockData: Intent = mock()
        whenever(mockData.getStringExtra(SimplifySecure3DActivity.EXTRA_RESULT)).doReturn("{\"missing\":\"data\"}")

        val result = Simplify.handle3DSResult(Simplify.REQUEST_CODE_3DS, Activity.RESULT_OK, mockData, spy3DSCallback)

        verify(spy3DSCallback).onSecure3DError(any())
        assertTrue("Should return true when handling error event") { result }
    }

    @Test
    fun test3DSErrorInResponseCallsErrorOnCallback() {
        val expectedErrorMessage = "3ds error message"

        val mockData: Intent = mock()
        whenever(mockData.getStringExtra(SimplifySecure3DActivity.EXTRA_RESULT)).doReturn("{\"secure3d\":{\"error\":{\"message\":\"$expectedErrorMessage\"}}}")

        val result = Simplify.handle3DSResult(Simplify.REQUEST_CODE_3DS, Activity.RESULT_OK, mockData, spy3DSCallback)

        verify(spy3DSCallback).onSecure3DError(expectedErrorMessage)
        assertTrue("Should return true when handling error event") { result }
    }

    @Test
    fun test3DSSuccessInResponseCallsCompleteOnCallback() {
        val authenticated = true

        val mockData: Intent = mock()
        whenever(mockData.getStringExtra(SimplifySecure3DActivity.EXTRA_RESULT)).doReturn("{\"secure3d\":{\"authenticated\":$authenticated}}")

        val result = Simplify.handle3DSResult(Simplify.REQUEST_CODE_3DS, Activity.RESULT_OK, mockData, spy3DSCallback)

        verify(spy3DSCallback).onSecure3DComplete(authenticated)
        assertTrue("Should return true when handling complete event") { result }
    }

    @Test
    fun testStart3DSActivityThrowsExceptionIfMissingData() {
        try {
            Simplify.start3DSActivity(mock(), mock())
            fail("Should have thrown an exception when no card.secure3DData provided")
        } catch (e: Exception) {
            assertTrue(e is IllegalArgumentException)
        }
    }

    @Test
    fun testStart3DSActivityUsesInternalRequestCode() {
        val mockActivity: Activity = mock()
        val cardToken = SimplifyMap()
                .set("card.secure3DData", SimplifyMap())

        Simplify.start3DSActivity(mockActivity, cardToken)

        verify(mockActivity).startActivityForResult(any(), eq(Simplify.REQUEST_CODE_3DS))
    }
}
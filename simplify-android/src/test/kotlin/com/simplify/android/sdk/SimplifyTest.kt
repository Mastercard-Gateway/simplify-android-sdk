package com.simplify.android.sdk

import android.app.Activity
import android.content.Intent
import com.google.android.gms.common.api.Status
import com.google.android.gms.wallet.*
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

    @Spy
    private lateinit var simplify: Simplify

    @Mock
    private lateinit var mockComms: SimplifyComms

    @Spy
    private lateinit var spyGooglePayCallback: SimplifyGooglePayCallback

    @Spy
    private lateinit var spy3DSCallback: SimplifySecure3DCallback


    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        simplify.comms = mockComms
    }

    @After
    fun tearDown() {
        reset(simplify, mockComms, spyGooglePayCallback,  spy3DSCallback)
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
    fun testBuildGooglePayCardTranslatesCorrectData() {
        val expectedGooglePayPublicKey = "googlepaypublickey"
        val expectedEncryptedMessage = "ClHC5fbJluQ9p/248os46ZVagAfzFWNlRzYwwdsufukx2nwGpJrWAmodnSMjPAqq8bV+k49Qk+ZS2eLoz/nO8SVv6cjrwpiYDzhq+F+Xpi71ku+MDpk4Xno7J4uBXn18qDMaozee8u+m6EPZzBez0qSe6gcrNJluZ3P7DO3e8hT2ncLYKxAtslqBxzBg96MBgWJTOfexBu7B4J/sOgYgzYwkFOzo8k7an3A5Ob/Yxu0crHLUraq1jg/w7dCCoP/pFEicbse46JVZDsyfhTn+GL9iHOMLhf9yvT5lM5KWu+JzThMnC62tJM8NHKHxX2UFj7v9CW5ioZakTckrj/hyeBd44EbHfjfUq3fIEY7Eqg5+mbsMyge/Bb/CPnI9+Ljmpw/4Wat5AQUOpfDJGRuqT7RNhKGybxCok7ZUFlMEZeOoycUuke6Gr0PfHC58+lYkj1jqbknoLpPwQ117/oqOgdpj9maTCVDUsptZNrJQQ2x3Bg7FcAxECKVahklIq6Cfk0DOD8N40Sg="
        val expectedEphemeralPublicKey = "BHIk/GTtmMkkcccc64towT3Oj8MFG1H1laEbw2ESbfFcd4VbeeB3aRczvoNYWq+nqJGQT/gpUEwlXp5uYee8348="
        val expectedTag = "Fy0WLJHJm/c2mdtmcpXz4pFiT0jmOaHAPKjjahnZjok="

        val testPaymentDataJson = "{\n" +
                "   \"apiVersionMinor\":0,\n" +
                "   \"apiVersion\":2,\n" +
                "   \"paymentMethodData\":{\n" +
                "      \"description\":\"Mastercard •••• 7257\",\n" +
                "      \"tokenizationData\":{\n" +
                "         \"type\":\"DIRECT\",\n" +
                "         \"token\":\"{\\\"signature\\\":\\\"MEUCIQDRS9Btm2DIzdtdsWTiqYE6CWHDmv\\/WPBK14NPjfKAS6wIgSsFFYkjG1DyrsqIay5psQITvbSCuoCpXP8mtHcNkszo\\\\u003d\\\",\\\"intermediateSigningKey\\\":{\\\"signedKey\\\":\\\"{\\\\\\\"keyValue\\\\\\\":\\\\\\\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEA8vLrIZ0hq0TpvQcZ20\\/l71W3lu0q8E8GBpEkyDIP4uJOZtWsAZaNZXkdjK0noJwqQs2tCCKFkXBES2PnVmn3w\\\\\\\\u003d\\\\\\\\u003d\\\\\\\",\\\\\\\"keyExpiration\\\\\\\":\\\\\\\"1556728660754\\\\\\\"}\\\",\\\"signatures\\\":[\\\"MEUCIADkG85ltrnSq1MZ45srGmv\\/jJnKn6RajrGcUdHyLjxpAiEA09EeKzqDq1zxMB3HbLpdTrbU6qTnjqsxXRS1YTd028Q\\\\u003d\\\"]},\\\"protocolVersion\\\":\\\"ECv2\\\",\\\"signedMessage\\\":\\\"{\\\\\\\"encryptedMessage\\\\\\\":\\\\\\\"ClHC5fbJluQ9p\\/248os46ZVagAfzFWNlRzYwwdsufukx2nwGpJrWAmodnSMjPAqq8bV+k49Qk+ZS2eLoz\\/nO8SVv6cjrwpiYDzhq+F+Xpi71ku+MDpk4Xno7J4uBXn18qDMaozee8u+m6EPZzBez0qSe6gcrNJluZ3P7DO3e8hT2ncLYKxAtslqBxzBg96MBgWJTOfexBu7B4J\\/sOgYgzYwkFOzo8k7an3A5Ob\\/Yxu0crHLUraq1jg\\/w7dCCoP\\/pFEicbse46JVZDsyfhTn+GL9iHOMLhf9yvT5lM5KWu+JzThMnC62tJM8NHKHxX2UFj7v9CW5ioZakTckrj\\/hyeBd44EbHfjfUq3fIEY7Eqg5+mbsMyge\\/Bb\\/CPnI9+Ljmpw\\/4Wat5AQUOpfDJGRuqT7RNhKGybxCok7ZUFlMEZeOoycUuke6Gr0PfHC58+lYkj1jqbknoLpPwQ117\\/oqOgdpj9maTCVDUsptZNrJQQ2x3Bg7FcAxECKVahklIq6Cfk0DOD8N40Sg\\\\\\\\u003d\\\\\\\",\\\\\\\"ephemeralPublicKey\\\\\\\":\\\\\\\"BHIk\\/GTtmMkkcccc64towT3Oj8MFG1H1laEbw2ESbfFcd4VbeeB3aRczvoNYWq+nqJGQT\\/gpUEwlXp5uYee8348\\\\\\\\u003d\\\\\\\",\\\\\\\"tag\\\\\\\":\\\\\\\"Fy0WLJHJm\\/c2mdtmcpXz4pFiT0jmOaHAPKjjahnZjok\\\\\\\\u003d\\\\\\\"}\\\"}\"\n" +
                "      },\n" +
                "      \"type\":\"CARD\",\n" +
                "      \"info\":{\n" +
                "         \"cardNetwork\":\"MASTERCARD\",\n" +
                "         \"cardDetails\":\"7257\"\n" +
                "      }\n" +
                "   }\n" +
                "}"

        val mockPaymentData : PaymentData = mock {
            on { toJson() } doReturn testPaymentDataJson
        }

        simplify.googlePayPublicKey = expectedGooglePayPublicKey

        simplify.createGooglePayCardToken(mockPaymentData, mock())

        val request = argumentCaptor<SimplifyRequest>().run {
            verify(mockComms).runSimplifyRequest(capture(), any())
            firstValue
        }

        assertEquals("ANDROID_PAY_IN_APP", request.payload["card.cardEntryMode"])
        assertEquals(expectedGooglePayPublicKey, request.payload["card.androidPayData.publicKey"])
        assertEquals(expectedEncryptedMessage, request.payload["card.androidPayData.paymentToken.encryptedMessage"])
        assertEquals(expectedEphemeralPublicKey, request.payload["card.androidPayData.paymentToken.ephemeralPublicKey"])
        assertEquals(expectedTag, request.payload["card.androidPayData.paymentToken.tag"])
    }

    @Test
    fun testGooglePayUnrecognizedRequestCodeReturnsFalse() {
        val unrecognizedRequestCode = 123

        val result = Simplify.handleGooglePayResult(unrecognizedRequestCode, Activity.RESULT_OK, mock(), mock())

        assertFalse("Should return false on unrecognized activity request code") { result }
    }

    @Test
    fun testResultCancelledCallsCancelledMethodOnCallback() {
        val result = Simplify.handleGooglePayResult(Simplify.REQUEST_CODE_GOOGLE_PAY_LOAD_PAYMENT_DATA, Activity.RESULT_CANCELED, mock(), spyGooglePayCallback)

        verify(spyGooglePayCallback).onGooglePayCancelled()
        assertTrue("Should return true when handling cancel event") { result }
    }

    @Test
    fun testErrorResultCallsErrorMethodOnCallback() {
        val errorResultCode = AutoResolveHelper.RESULT_ERROR

        val data = Intent()
        data.putExtra("com.google.android.gms.common.api.AutoResolveHelper.status", Status.RESULT_DEAD_CLIENT)

        val result = Simplify.handleGooglePayResult(Simplify.REQUEST_CODE_GOOGLE_PAY_LOAD_PAYMENT_DATA, errorResultCode, data, spyGooglePayCallback)

        verify(spyGooglePayCallback).onGooglePayError(Status.RESULT_DEAD_CLIENT)
        assertTrue("Should return true when handling error event") { result }
    }

    @Test
    fun testGooglePayResultReturnsPaymentDataToCallback() {
        val testPaymentDataJson = "{\n" +
                "   \"apiVersionMinor\":0,\n" +
                "   \"apiVersion\":2,\n" +
                "   \"paymentMethodData\":{\n" +
                "      \"description\":\"Mastercard •••• 7257\",\n" +
                "      \"tokenizationData\":{\n" +
                "         \"type\":\"DIRECT\",\n" +
                "         \"token\":\"{\\\"signature\\\":\\\"MEUCIQDRS9Btm2DIzdtdsWTiqYE6CWHDmv\\/WPBK14NPjfKAS6wIgSsFFYkjG1DyrsqIay5psQITvbSCuoCpXP8mtHcNkszo\\\\u003d\\\",\\\"intermediateSigningKey\\\":{\\\"signedKey\\\":\\\"{\\\\\\\"keyValue\\\\\\\":\\\\\\\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEA8vLrIZ0hq0TpvQcZ20\\/l71W3lu0q8E8GBpEkyDIP4uJOZtWsAZaNZXkdjK0noJwqQs2tCCKFkXBES2PnVmn3w\\\\\\\\u003d\\\\\\\\u003d\\\\\\\",\\\\\\\"keyExpiration\\\\\\\":\\\\\\\"1556728660754\\\\\\\"}\\\",\\\"signatures\\\":[\\\"MEUCIADkG85ltrnSq1MZ45srGmv\\/jJnKn6RajrGcUdHyLjxpAiEA09EeKzqDq1zxMB3HbLpdTrbU6qTnjqsxXRS1YTd028Q\\\\u003d\\\"]},\\\"protocolVersion\\\":\\\"ECv2\\\",\\\"signedMessage\\\":\\\"{\\\\\\\"encryptedMessage\\\\\\\":\\\\\\\"ClHC5fbJluQ9p\\/248os46ZVagAfzFWNlRzYwwdsufukx2nwGpJrWAmodnSMjPAqq8bV+k49Qk+ZS2eLoz\\/nO8SVv6cjrwpiYDzhq+F+Xpi71ku+MDpk4Xno7J4uBXn18qDMaozee8u+m6EPZzBez0qSe6gcrNJluZ3P7DO3e8hT2ncLYKxAtslqBxzBg96MBgWJTOfexBu7B4J\\/sOgYgzYwkFOzo8k7an3A5Ob\\/Yxu0crHLUraq1jg\\/w7dCCoP\\/pFEicbse46JVZDsyfhTn+GL9iHOMLhf9yvT5lM5KWu+JzThMnC62tJM8NHKHxX2UFj7v9CW5ioZakTckrj\\/hyeBd44EbHfjfUq3fIEY7Eqg5+mbsMyge\\/Bb\\/CPnI9+Ljmpw\\/4Wat5AQUOpfDJGRuqT7RNhKGybxCok7ZUFlMEZeOoycUuke6Gr0PfHC58+lYkj1jqbknoLpPwQ117\\/oqOgdpj9maTCVDUsptZNrJQQ2x3Bg7FcAxECKVahklIq6Cfk0DOD8N40Sg\\\\\\\\u003d\\\\\\\",\\\\\\\"ephemeralPublicKey\\\\\\\":\\\\\\\"BHIk\\/GTtmMkkcccc64towT3Oj8MFG1H1laEbw2ESbfFcd4VbeeB3aRczvoNYWq+nqJGQT\\/gpUEwlXp5uYee8348\\\\\\\\u003d\\\\\\\",\\\\\\\"tag\\\\\\\":\\\\\\\"Fy0WLJHJm\\/c2mdtmcpXz4pFiT0jmOaHAPKjjahnZjok\\\\\\\\u003d\\\\\\\"}\\\"}\"\n" +
                "      },\n" +
                "      \"type\":\"CARD\",\n" +
                "      \"info\":{\n" +
                "         \"cardNetwork\":\"MASTERCARD\",\n" +
                "         \"cardDetails\":\"7257\"\n" +
                "      }\n" +
                "   }\n" +
                "}"

        val paymentData = PaymentData.fromJson(testPaymentDataJson)

        val data = Intent()
        paymentData.putIntoIntent(data)

        val result = Simplify.handleGooglePayResult(Simplify.REQUEST_CODE_GOOGLE_PAY_LOAD_PAYMENT_DATA, Activity.RESULT_OK, data, spyGooglePayCallback)

        verify(spyGooglePayCallback).onReceivedPaymentData(any())
        assertTrue("Should return true when handling MaskedWallet event") { result }
    }

    @Test
    fun testGooglePayCallsErrorWhenIssueParsingPaymentData() {
        val result = Simplify.handleGooglePayResult(Simplify.REQUEST_CODE_GOOGLE_PAY_LOAD_PAYMENT_DATA, Activity.RESULT_OK, null, spyGooglePayCallback)

        verify(spyGooglePayCallback).onGooglePayError(Status.RESULT_INTERNAL_ERROR)
        assertTrue("Should return true when handling error event") { result }
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
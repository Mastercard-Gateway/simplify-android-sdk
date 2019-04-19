package com.simplify.android.sdk

import android.app.Activity
import android.content.Intent
import com.google.android.gms.identity.intents.model.UserAddress
import com.google.android.gms.wallet.FullWallet
import com.google.android.gms.wallet.MaskedWallet
import com.google.android.gms.wallet.PaymentMethodToken
import com.google.android.gms.wallet.WalletConstants
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
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SimplifyTest {

    @Spy
    private lateinit var simplify: Simplify

    @Mock
    private lateinit var mockComms: SimplifyComms

    @Spy
    private lateinit var spyAndroidPayCallback: SimplifyAndroidPayCallback


//    internal var spyAndroidPayCallback: SimplifyAndroidPayCallback
//    internal var mockResultData: Intent

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        simplify.comms = mockComms

//        mockResultData = mock(Intent::class.java)
    }

    @After
    fun tearDown() {
        reset(simplify, mockComms, spyAndroidPayCallback)
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
    fun testBuildAndroidPayCardTranslatesCorrectData() {
        val apiKey = "lvpb_M2E2YTJkOTctMDEwZS00MjViLWJhZWItZmI1Yjg1NTMxMDk3"
        val androidPayPublicKey = "my android pay public key"
        val addressLine1 = "address 1"
        val addressLine2 = "address 1"
        val city = "city"
        val state = "state"
        val zip = "12345"
        val country = "US"
        val customerName = "Joe Cardholder"
        val customerEmail = "test@test.com"
        val testPaymentTokenKey = "testPaymentTokenKey"
        val testPaymentTokenValue = "testPaymentTokenValue"
        val paymentToken = "{\"$testPaymentTokenKey\":\"$testPaymentTokenValue\"}"

        val mockPaymentMethodToken: PaymentMethodToken = mock {
            on { token } doReturn paymentToken
        }

        val mockUserAddress: UserAddress = mock {
            on { address1 } doReturn addressLine1
            on { address2 } doReturn addressLine2
            on { locality } doReturn city
            on { administrativeArea } doReturn state
            on { postalCode } doReturn zip
            on { countryCode } doReturn country
            on { name } doReturn customerName
        }

        val fullWallet: FullWallet = mock {
            on { email } doReturn customerEmail
            on { paymentMethodToken } doReturn mockPaymentMethodToken
            on { buyerBillingAddress } doReturn mockUserAddress
        }

        simplify.apiKey = apiKey
        simplify.androidPayPublicKey = androidPayPublicKey

        simplify.createAndroidPayCardToken(fullWallet, mock())

        argumentCaptor<SimplifyRequest>().apply {
            verify(mockComms).runSimplifyRequest(capture(), any())

            (firstValue.payload["card"] as SimplifyMap).let {
                assertEquals("ANDROID_PAY_IN_APP", it["cardEntryMode"])
                assertEquals(androidPayPublicKey, it["androidPayData.publicKey"])
                assertEquals(testPaymentTokenValue, it["androidPayData.paymentToken.$testPaymentTokenKey"])
                assertEquals(addressLine1, it["addressLine1"])
                assertEquals(addressLine2, it["addressLine2"])
                assertEquals(city, it["addressCity"])
                assertEquals(state, it["addressState"])
                assertEquals(zip, it["addressZip"])
                assertEquals(country, it["addressCountry"])
                assertEquals(customerName, it["customer.name"])
                assertEquals(customerEmail, it["customer.email"])
            }
        }
    }

    @Test
    fun testUnrecognizedRequestCodeReturnsFalse() {
        val unrecognizedRequestCode = 123

        val result = Simplify.handleAndroidPayResult(unrecognizedRequestCode, Activity.RESULT_OK, mock(), mock())

        assertFalse("Should return false on unrecognized activity request code") { result }
    }

    @Test
    fun testResultCancelledCallsCancelledMethodOnCallback() {
        val result = Simplify.handleAndroidPayResult(Simplify.REQUEST_CODE_MASKED_WALLET, Activity.RESULT_CANCELED, mock(), spyAndroidPayCallback)

        verify(spyAndroidPayCallback).onAndroidPayCancelled()
        assertTrue("Should return true when handling cancel event") { result }
    }

    @Test
    fun testErrorResultCallsErrorMethodOnCallback() {
        val errorResultCode = 100

        val mockData: Intent = mock()
        whenever(mockData.getIntExtra(eq(WalletConstants.EXTRA_ERROR_CODE), any())).doReturn(errorResultCode)

        val result = Simplify.handleAndroidPayResult(Simplify.REQUEST_CODE_MASKED_WALLET, errorResultCode, mockData, spyAndroidPayCallback)

        verify(spyAndroidPayCallback).onAndroidPayError(errorResultCode)
        assertTrue("Should return true when handling error event") { result }
    }

    @Test
    fun testMaskedWalletResultReturnsWalletToCallback() {
        val maskedWallet: MaskedWallet = mock()

        val mockData: Intent = mock()
        whenever(mockData.hasExtra(WalletConstants.EXTRA_MASKED_WALLET)).doReturn(true)
        whenever(mockData.getParcelableExtra<MaskedWallet?>(WalletConstants.EXTRA_MASKED_WALLET)).doReturn(maskedWallet)

        val result = Simplify.handleAndroidPayResult(Simplify.REQUEST_CODE_MASKED_WALLET, Activity.RESULT_OK, mockData, spyAndroidPayCallback)

        verify(spyAndroidPayCallback).onReceivedMaskedWallet(maskedWallet)
        assertTrue("Should return true when handling MaskedWallet event") { result }
    }

    @Test
    fun testFullWalletResultReturnsWalletToCallback() {
        val fullWallet: FullWallet = mock()

        val mockData: Intent = mock()
        whenever(mockData.hasExtra(WalletConstants.EXTRA_FULL_WALLET)).doReturn(true)
        whenever(mockData.getParcelableExtra<FullWallet?>(WalletConstants.EXTRA_FULL_WALLET)).doReturn(fullWallet)

        val result = Simplify.handleAndroidPayResult(Simplify.REQUEST_CODE_FULL_WALLET, Activity.RESULT_OK, mockData, spyAndroidPayCallback)

        verify(spyAndroidPayCallback).onReceivedFullWallet(fullWallet)
        assertTrue("Should return true when handling FullWallet event") { result }
    }


}
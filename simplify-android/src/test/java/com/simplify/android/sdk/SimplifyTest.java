package com.simplify.android.sdk;


import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.WalletConstants;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SimplifyTest {

    Simplify simplify;

    Simplify.AndroidPayCallback spyAndroidPayCallback;
    Intent mockResultData;

    @Before
    public void setUp() throws Exception {
        simplify = spy(new Simplify());

        spyAndroidPayCallback = spy(new Simplify.AndroidPayCallback() {
            @Override
            public void onReceivedMaskedWallet(MaskedWallet maskedWallet) {
            }

            @Override
            public void onReceivedFullWallet(FullWallet fullWallet) {
            }

            @Override
            public void onAndroidPayCancelled() {
            }

            @Override
            public void onAndroidPayError(int errorCode) {
            }
        });

        mockResultData = mock(Intent.class);
    }

    @Test
    public void testValidateApiKeyReturnsFalseWhenIncorrectFormat() {
        String badUuidEncoded = "bm8gYSB1dWlk";
        String badUuidDecoded = "not a uuid";
        String badKey = "lvpb_" + badUuidEncoded;

        doReturn(badUuidDecoded).when(simplify).base64Decode(badUuidEncoded);

        boolean returnValue = simplify.validateApiKey(badKey);
        assertFalse(returnValue);

        // missing prefix
        returnValue = simplify.validateApiKey(badUuidEncoded);
        assertFalse(returnValue);
    }

    @Test
    public void testValidateApiKeyReturnsTrueWithGoodKey() {
        String uuidEncoded = "M2E2YTJkOTctMDEwZS00MjViLWJhZWItZmI1Yjg1NTMxMDk3";
        String uuidDecoded = "3a6a2d97-010e-425b-baeb-fb5b85531097";
        String goodKey = "lvpb_" + uuidEncoded;

        doReturn(uuidDecoded).when(simplify).base64Decode(uuidEncoded);

        boolean returnValue = simplify.validateApiKey(goodKey);
        assertTrue(returnValue);
    }

    @Test
    public void testInitWithValidApiKeyIsSuccessful() {
        String apiKey = "lvpb_M2E2YTJkOTctMDEwZS00MjViLWJhZWItZmI1Yjg1NTMxMDk3";

        doReturn(true).when(simplify).validateApiKey(any());

        simplify.setApiKey(apiKey);

        assertEquals(simplify.apiKey, apiKey);
    }

    @Test
    public void testInitWithInvalidApiKeyThrowsError() {
        String key = "invalid";

        doReturn(false).when(simplify).validateApiKey(key);

        try {
            simplify.setApiKey(key);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testGetUrlWithLiveApiKey() {
        simplify.apiKey = "lvpb_123456789";

        assertEquals(simplify.getUrl(), Constants.API_BASE_LIVE_URL);
    }

    @Test
    public void testGetUrlWithSandboxApiKey() {
        simplify.apiKey = "sbpb_123456789";

        assertEquals(simplify.getUrl(), Constants.API_BASE_SANDBOX_URL);
    }

    @Test
    public void testMissingAndroidPayCallbackThrowsException() {
        try {
            simplify.handleAndroidPayResult(Simplify.REQUEST_CODE_MASKED_WALLET, Activity.RESULT_OK, mockResultData, null);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testUnrecognizedRequestCodeReturnsFalse() {
        int unrecognizedRequestCode = 123;

        boolean handled = simplify.handleAndroidPayResult(unrecognizedRequestCode, Activity.RESULT_OK, mockResultData, spyAndroidPayCallback);

        assertFalse(handled);
    }

    @Test
    public void testResultCancelledCallsCancelledMethodOnCallback() {
        simplify.handleAndroidPayResult(Simplify.REQUEST_CODE_MASKED_WALLET, Activity.RESULT_CANCELED, mockResultData, spyAndroidPayCallback);

        verify(spyAndroidPayCallback).onAndroidPayCancelled();
    }

    @Test
    public void testErrorResultCallsErrorMethodOnCallback() {
        int errorResultCode = 100;

        // mock getting error code out of intent data
        when(mockResultData.getIntExtra(anyString(), anyInt())).thenReturn(errorResultCode);

        simplify.handleAndroidPayResult(Simplify.REQUEST_CODE_MASKED_WALLET, errorResultCode, mockResultData, spyAndroidPayCallback);

        verify(spyAndroidPayCallback).onAndroidPayError(errorResultCode);
    }

    @Test
    public void testMaskedWalletResultReturnsWalletToCallback() {
        MaskedWallet maskedWallet = null;

        when(mockResultData.hasExtra(WalletConstants.EXTRA_MASKED_WALLET)).thenReturn(true);
        when(mockResultData.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET)).thenReturn(maskedWallet);

        simplify.handleAndroidPayResult(Simplify.REQUEST_CODE_MASKED_WALLET, Activity.RESULT_OK, mockResultData, spyAndroidPayCallback);

        verify(spyAndroidPayCallback).onReceivedMaskedWallet(maskedWallet);
    }

    @Test
    public void testFullWalletResultReturnsWalletToCallback() {
        FullWallet fullWallet = null;

        when(mockResultData.hasExtra(WalletConstants.EXTRA_FULL_WALLET)).thenReturn(true);
        when(mockResultData.getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET)).thenReturn(fullWallet);

        simplify.handleAndroidPayResult(Simplify.REQUEST_CODE_FULL_WALLET, Activity.RESULT_OK, mockResultData, spyAndroidPayCallback);

        verify(spyAndroidPayCallback).onReceivedFullWallet(fullWallet);
    }

    @Test
    public void testHandlerMessageContainingThrowableCallsOnErrorCallback() {
        Exception exception = new Exception();
        CardToken.Callback mockCallback = mock(CardToken.Callback.class);

        boolean returnValue = simplify.handleCreateCardTokenCallbackMessage(mockCallback, exception);

        verify(mockCallback).onError(exception);
        assertTrue(returnValue);
    }

    @Test
    public void testHandlerMessageContainingTCardTokenCallsOnSuccessCallback() {
        CardToken mockCardToken = mock(CardToken.class);
        CardToken.Callback mockCallback = mock(CardToken.Callback.class);

        boolean returnValue = simplify.handleCreateCardTokenCallbackMessage(mockCallback, mockCardToken);

        verify(mockCallback).onSuccess(mockCardToken);
        assertTrue(returnValue);
    }

    @Test
    public void testCreateCardTokenRunnableSendsThrowableMessageOnError() throws Exception {
        Handler mockHandler = mock(Handler.class);
        Message message = new Message();
        Exception exception = new Exception();

        when(mockHandler.obtainMessage()).thenReturn(message);
        doThrow(exception).when(simplify).executeCreateCardToken(any(), any());

        simplify.runCreateCardToken(new Card(), new Secure3DRequestData(), mockHandler);

        assertEquals(message.obj, exception);
        verify(mockHandler).sendMessage(message);
    }

    @Test
    public void testCreateCardTokenRunnableSendsCardTokenMessageOnSuccess() throws Exception {
        Handler mockHandler = mock(Handler.class);
        Message message = new Message();
        CardToken cardToken = mock(CardToken.class);

        when(mockHandler.obtainMessage()).thenReturn(message);
        doReturn(cardToken).when(simplify).executeCreateCardToken(any(), any());

        simplify.runCreateCardToken(new Card(), new Secure3DRequestData(), mockHandler);

        assertEquals(message.obj, cardToken);
        verify(mockHandler).sendMessage(message);
    }
}

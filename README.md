Simplify Android SDK
====================

Our Android SDK allows you to easily integrate payments into your Android app. By creating a one-time use CardToken through our SDK, you avoid the risk of handling sensitive card details on your server. We offer the ability to collect card information in a couple of different ways:

* Manual card entry (with or without built-in UI)
* Android Pay / Google Wallet

Import the Library
------------------

[![Download](https://api.bintray.com/packages/simplify/Android/simplify-sdk-android/images/download.svg)](https://bintray.com/simplify/Android/simplify-sdk-android/_latestVersion)

To import the Android SDK, include it as a dependency in your build.gradle file

    compile 'com.simplify:sdk-android:2.0.0'

Initialize the SDK
------------------

Before you can communicate with Simplify to tokenize a card, you must first initialize the SDK with your public API key. To retrieve a usable public API key, first log in to simplify.com and click on "API Keys" in the dropdown menu next to your name. From there, you can create a new public key specific to your app, or use an existing one. Be sure to use a "live" key for your production app. Card tokens created with a sandbox API key can not be used to process real payments!

To initialize the SDK, set the public key within your app's custom Application class.

    @Override
    public void onCreate() {
        super.onCreate();

        Simplify.init("YOUR_PUBLIC_KEY");
    }

Collect Card Information
------------------------

When originating a payment from your mobile app, you must first collect and tokenize card information. There are a few ways to collect card information using the Simplify SDK:

* Manually build a Card object (using your own custom card entry fields)
* Retrieve a Card object from the provided CardEditor view
* Android Pay / Google Wallet

Manual Card / Custom UI
-----------------------

If you are using your own UI to collect card data from the user, you should build a Simplify Card object with this data. Refer to the CardToken API documentation for the minimum required fields when tokenizing a card.

    // create a new card object
    Card card = new Card()
        .setNumber("5555555555554444")
        .setExpMonth("01")
        .setExpYear("99")
        .setCvc("123")
        .setAddressZip("12345");

    // tokenize the card
    Simplify.createCardToken(card, new CardToken.Callback() {
        @Override
        public void onSuccess(CardToken cardToken) {
            // ...
        }

        @Override
        public void onError(Throwable throwable) {
            // ...
        }
    });

Using the CardEditor view
-------------------------

If you would prefer to use our provided UI to collect card information, simply drop the CardEditor view into your layout.

    <!-- Add xmlns:auto="http://schemas.android.com/apk/res-auto" -->
    <!-- to the root element of your layout to use custom attributes -->

    <com.simplify.android.sdk.CardEditor
        android:id="@+id/card_editor"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        auto:iconColor="@color/my_custom_icon_color"
        auto:enabled="true"/>

Within your context, you can initialize the view with a state change listener to receive notification when the valid state of the view has changed after the user has entered details. You may use this to enable/disable a checkout button as demonstrated below.

    // init card editor
    final CardEditor cardEditor = (CardEditor) findViewById(R.id.card_editor);
    final Button checkoutButton = (Button) findViewById(R.id.checkout_button);

    // add state change listener
    cardEditor.addOnStateChangeListener(new CardEditor.OnStateChangedListener() {
        @Override
        public void onStateChange(CardEditor cardEditor) {
            checkoutButton.setEnabled(cardEditor.isValid());
        }
    });

    // add checkout button click listener
    checkoutButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
            Simplify.createCardToken(cardEditor.getCard(), new CardToken.Callback() {
                @Override
                public void onSuccess(CardToken cardToken) {
                    // ...
                }

                @Override
                public void onError(Throwable throwable) {
                    // ...
                }
            });
        }
    });

Android Pay / Google Wallet
---------------------------

To enable Android Pay / Google Wallet support within your app, refer to [https://developers.google.com/android-pay/android/tutorial](https://developers.google.com/android-pay/android/tutorial). These instructions should guide you in how to:

* Obtain a Google API key
* Create an instance of the GoogleAPIClient
* Incorporate the WalletFragment into your layout
* Construct the masked and full wallet requests

Since Android Pay integration is optional with the Simplify SDK, you must provide the appropriate play services dependency.

    compile 'com.google.android.gms:play-services-wallet:7.8.0'

The Simplify SDK offers an Android Pay lifecycle handler for added convenience. You may implement the provided Android Pay callback and use the result handler within your Activity. This alleviates the need to manually handle the Android Pay responses, and will delegate the important transaction steps to more easily readable callback methods.

    public class YourActivity extends Activity implements Simplify.AndroidPayCallback {

        @Override
        protected void onStart() {
            super.onStart();

            // ...

            // register Android Pay callback
            Simplify.addAndroidPayCallback(this);
        }

        @Override
        protected void onStop() {
            // remove Android Pay callback
            Simplify.removeAndroidPayCallback(this);

            // ...

            super.onStop();
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            // delegate Android Pay transaction life-cycle to Simplify SDK
            if (Simplify.handleAndroidPayResult(requestCode, resultCode, data)) {
                return;
            }

            // ...
        }


        //---------------------------------------------
        // Android Pay callback methods
        //---------------------------------------------

        @Override
        public void onReceivedMaskedWallet(MaskedWallet maskedWallet) {
            // ...
        }

        @Override
        public void onReceivedFullWallet(FullWallet fullWallet) {
            // ...
        }

        @Override
        public void onAndroidPayCancelled() {
            // ...
        }

        @Override
        public void onAndroidPayError(int errorCode) {
            // ...
        }
    }

Once you have received your FullWallet, you may use it to create a CardToken with Simplify.

    Simplify.createAndroidPayCardToken(fullWallet, new CardToken.Callback() {
        @Override
        public void onSuccess(CardToken cardToken) {
            // ...
        }

        @Override
        public void onError(Throwable throwable) {
            // ...
        }
    });

Processing a Payment with a CardToken
-------------------------------------

Regardless of which method you are using to collect card information, you should ultimately end up with a Simplify CardToken object. This one-time token should be sent back to your servers for processing. This is a highly recommended step to ensure your payment processing and your private API keys are kept secure.

For an example on how to start processing payments from your server, refer to [https://github.com/simplifycom/simplify-php-server](https://github.com/simplifycom/simplify-php-server)

Rx-enabled
---------

If being reactive is your thing, then we've got you covered. Include the RxAndroid library in your project.

    compile 'io.reactivex:rxandroid:1.0.0'

Then, utilize the Rx-enabled methods provided in the Simplify object.

    Observable<CardToken> o = Simplify.createCardToken(card);
    o.subscribe(new Action1<CardToken>() {
        @Override
        public void call(CardToken cardToken) {
            // ...
        }
    })

Support
-------

For more information, visit [https://www.simplify.com/commerce/docs/sdk/android](https://www.simplify.com/commerce/docs/sdk/android)

For support inquiries, visit [https://simplify.desk.com](https://simplify.desk.com)
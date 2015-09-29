# Simplify Android SDK

Our Android SDK allows you to easily integrate payments into your Android app. By creating a one-time use CardToken through our SDK, you avoid the risk of handling sensitive card details on your server.

## Import the Library [![maven-central](https://img.shields.io/maven-central/v/com.simplify/simplify-android.svg)](http://search.maven.org/#search%7Cga%7C1%7Csimplify-android)

To import the Android SDK, include it as a dependency in your build.gradle file

    compile 'com.simplify:simplify-android:2.0.2'

## Initialize the SDK

Before you can communicate with Simplify to tokenize a card, you must first initialize the SDK with your public API key. To retrieve a usable public API key, first log in to simplify.com and click on "API Keys" in the dropdown menu next to your name. From there, you can create a new public key specific to your app, or use an existing one. Be sure to use a "live" key for your production app. Card tokens created with a sandbox API key can not be used to process real payments!

To initialize the SDK, set the public key within your app's custom Application class.

    @Override
    public void onCreate() {
        super.onCreate();

        Simplify.init("YOUR_PUBLIC_KEY");
    }

## Collect Card Information

When originating a payment from your mobile app, you must first collect and tokenize card information. There are a few ways to collect card information using the Simplify SDK:

* Manually build a Card object (using your own custom card entry fields)
* Retrieve a Card object from the provided CardEditor view
* Android Pay / Google Wallet

### Manual Card / Custom UI

If you are using your own UI to collect card data from the user, you should build a Simplify Card object with this data. Refer to the [CardToken API docs](https://www.simplify.com/commerce/docs/apidoc/cardToken#create) for the minimum required fields to tokenize a card.

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

### Using the CardEditor View

If you would prefer to use our provided UI to collect card information, simply drop the CardEditor view into your layout.

    <com.simplify.android.sdk.CardEditor
        android:id="@+id/card_editor"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"/>

Within your context, you can register a state change listener to receive notifications when the valid state of the view has changed after the user has entered details. You may use this to enable/disable a checkout button as demonstrated below.

    // init card editor
    final CardEditor cardEditor = (CardEditor) findViewById(R.id.card_editor);
    final Button checkoutButton = (Button) findViewById(R.id.checkout_button);

    // add state change listener
    cardEditor.addOnStateChangeListener(new CardEditor.OnStateChangedListener() {
        @Override
        public void onStateChange(CardEditor cardEditor) {
            // true: card editor contains valid and complete card information
            checkoutButton.setEnabled(cardEditor.isValid());
        }
    });

    // add checkout button click listener
    checkoutButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
            // create a card token
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

By default, the CardEditor view will inherit the styles of the theme you apply to it's context. However, you may change the appearance in a couple of ways, as shown below.

Apply a new theme for EditText and/or AlertDialog (used to choose expiration date):

    <style name="AppTheme" parent="Theme.AppCompat.Light">
        <item name="colorAccent">@color/accent</item> <!-- sets the divider color on the number pickers in the exipiration date dialog -->
        <item name="android:editTextStyle">@style/EditText</item>
        <item name="alertDialogTheme">@style/AlertDialogTheme</item>
    </style>
    
    <style name="AlertDialogTheme" parent="Theme.AppCompat.Light.Dialog.Alert">
        <item name="colorAccent">@color/accent</item> <!-- sets the action button text color -->
        <item name="android:textColorPrimary">#000000</item> <!-- sets the title text color -->
    </style>
    
    <style name="EditText" parent="Widget.AppCompat.EditText">
        <item name="android:minHeight">50dp</item>
    </style>

Set custom attributes on the CardEditor view in your layout:

    <!-- Add xmlns:auto="http://schemas.android.com/apk/res-auto" -->
    <!-- to the root element of your layout to use custom attributes -->
    
    <com.simplify.android.sdk.CardEditor
            android:id="@+id/card_editor"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            auto:iconColor="@color/my_custom_icon_color"
            auto:enabled="true"/>

Or in code:

    cardEditor.setIconColor(getColor(R.color.my_custom_icon_color));
    cardEditor.setEnabled(true);

### Android Pay / Google Wallet

To enable Android Pay / Google Wallet support within your app, refer to the [Android Pay API tutorial](https://developers.google.com/android-pay/android/tutorial). These instructions should guide you in how to:

* Obtain a Google API key
* Create an instance of the GoogleAPIClient
* Incorporate the WalletFragment into your layout
* Construct the masked and full wallet requests

Since Android Pay integration is optional with the Simplify SDK, you must provide the appropriate play services dependency.

    compile 'com.google.android.gms:play-services-wallet:7.8.0'

The Simplify SDK offers an Android Pay lifecycle handler for added convenience. You may implement the provided Android Pay callback and use the result handler within your Activity. This alleviates the need to manually handle the Android Pay activity results, and will delegate the important transaction steps to callback methods.

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

## Processing a Payment with a CardToken

Regardless of which method you are using to collect card information, you should ultimately end up with a Simplify CardToken object. This one-time token should be sent back to your servers for processing. This is a highly recommended step to ensure your payment processing and your private API keys are kept secure.

For an example on how to start processing payments from your server, refer to [https://github.com/simplifycom/simplify-php-server](https://github.com/simplifycom/simplify-php-server)

## Rx-enabled

If being reactive is your thing, then we've got you covered. Include the [RxAndroid](https://github.com/ReactiveX/RxAndroid) library in your project.

Then, utilize the Rx-enabled methods provided in the Simplify object.

    Observable<CardToken> o = Simplify.createCardToken(card);
    o.subscribe(new Action1<CardToken>() {
        @Override
        public void call(CardToken cardToken) {
            // ...
        }
    })

## Security

As mentioned above, using a tokenized card to create a Payment should happen on your secure server, and **not** from within your app. This added measure alleviates your need to distribute private API keys within your app, as well as the burden of handling sensitive card information on your servers.

For added protection (and peace of mind), all traffic between your app and Simplify is secured using [TLS 1.2](https://en.wikipedia.org/wiki/Transport_Layer_Security#TLS_1.2) and [certificate pinning](https://en.wikipedia.org/wiki/HTTP_Public_Key_Pinning).

## Support

For more information, visit [https://www.simplify.com/commerce/docs/sdk/android](https://www.simplify.com/commerce/docs/sdk/android)

For support inquiries, visit [https://simplify.desk.com](https://simplify.desk.com)
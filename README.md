What is it?
------------

The Android SDK by Simplify allows you to create a card token (one time use token representing card details) in your Android app to send to a server to enable it to make a payment. By creating a card token, Simplify allows you to avoid sending card details to your server. The SDK can help with formatting and validating card information before the information is tokenized.


Download
--------

[![Download](https://api.bintray.com/packages/simplify/Android/simplify-sdk-android/images/download.svg)](https://bintray.com/simplify/Android/simplify-sdk-android/_latestVersion)

You can include the SDK in your app by adding it as a gradle dependency:

    compile 'com.simplify:sdk-android:1.0.3'

or via Maven:

    <dependency>
        <groupId>com.simplify</groupId>
        <artifactId>sdk-android</artifactId>
        <version>1.0.3</version>
    </dependency>


Using the SDK
--------------

To create a token through Simplify Commerce, use the following script, substituting your public API key:

    Simplify simplify = new Simplify({YOUR-PUBLIC-KEY});

Create a token callback

    Simplify.CreateCardTokenListener listener = new Simplify.CreateCardTokenListener()
    {
        @Override
        public void onSuccess(Token token)
        {
            Log.i("Simplify", "Created Token: " + token.getId());

            // TODO your business logic to complete payment...
        }

        @Override
        public void onError(SimplifyError error)
        {
            Log.e("Simplify", "Error Creating Token: " + error.getMessage());
        }
    }

Request the token

    AsyncTask<?, ?, ?> createTokenTask = simplify.createCardToken({CARD-NUMBER}, {EXP-MONTH}, {EXP-YEAR}, {CVC}, listener);


For more examples see https://www.simplify.com/commerce/docs/sdk/android.


Support
-------

For support inquiries, visit [simplify.desk.com](https://simplify.desk.com)
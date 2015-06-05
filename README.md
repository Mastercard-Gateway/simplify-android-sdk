What is it?
------------

A Android API to the Simplify Commerce payments platform.   If you have
not already got an account sign up at https://www.simplify.com/commerce.


Installation
------------

Include the ‘library’ directory as an Android library dependency in your project


Using the SDK
--------------

To create a token through Simplify Commerce, use the following script
substituting you public API key:


// create a Simplify object
Simplify simplify = new Simplify({YOUR-PUBLIC-KEY});

// create a token callback
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

// request the token
AsyncTask<?, ?, ?> createTokenTask = simplify.createCardToken({CARD-NUMBER}, {EXP-MONTH}, {EXP-YEAR}, {CVC}, listener);


For more examples see https://www.simplify.com/commerce/docs/sdk/android.

Version
-------

This is version 1.0.1 of the SDK. For an up-to-date version check at
https://www.simplify.com/commerce/docs/sdk/android.

Licensing
---------

Please see LICENSE.txt for details.

Documentation
-------------

API documentation is available in the library/docs directory in HTML.  For more
detailed information on the API with examples visit the online
documentation at https://www.simplify.com/commerce/docs/sdk/android.

Support
-------

Please see https://www.simplify.com/commerce/docs/support for information.

Copyright
---------

Copyright (c) 2013, 2014 MasterCard International Incorporated
All rights reserved.

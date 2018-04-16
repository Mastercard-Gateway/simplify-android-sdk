package com.simplify.android.sdk;

/**
 * Class representing a card token returned from the Simplify API
 */
@SuppressWarnings("unused")
public class CardToken {

    static final String TAG = CardToken.class.getSimpleName();

    String id;
    boolean used;
    Card card;


    /**
     * The id of the card token
     * @return The id
     */
    public String getId() {
        return id;
    }

    /**
     * Flag indicating if the token has already been used
     * @return True or False
     */
    public boolean isUsed() {
        return used;
    }

    /**
     * The card associated with this token
     * @return The associated card
     */
    public Card getCard() {
        return card;
    }


    /**
     * Listener callback for creating a token
     */
    public interface Callback {
        void onSuccess(CardToken cardToken);

        void onError(Throwable throwable);
    }
}

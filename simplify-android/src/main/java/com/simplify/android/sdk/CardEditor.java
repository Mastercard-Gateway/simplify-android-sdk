package com.simplify.android.sdk;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;


/**
 * Class representing a card editor view, allowing a user
 * to enter and validate card data
 */
public class CardEditor extends LinearLayout {

    /**
     * A callback interface used to report when the provided card information's
     * overall valid state changes
     */
    public interface OnStateChangedListener {
        void onStateChange(CardEditor cardEditor);
    }

    EditText cardNumberEditText;
    EditText cardExpiryEditText;
    EditText cardCvcEditText;

    Drawable numberIcon;

    Dialog expirationDialog;
    NumberPicker monthPicker;
    NumberPicker yearPicker;

    CardBrand cardBrand = CardBrand.UNKNOWN;
    String cardExpMonth;
    String cardExpYear;

    boolean prevValid = false;
    boolean cardNumberValid = false;
    boolean cardExpiryValid = false;
    boolean cardCvcValid = false;

    ArrayList<OnStateChangedListener> onStateChangedListeners = new ArrayList<>();


    /**
     * Constructs a new CardEditor view
     *
     * @param context The controlling context
     */
    public CardEditor(Context context) {
        super(context);
        init(null);
    }

    /**
     * Constructs a new CardEditor view
     *
     * @param context The controlling context
     * @param attrs   The view attributes
     */
    public CardEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    /**
     * Constructs a new CardEditor view
     *
     * @param context  The controlling context
     * @param attrs    The view attributes
     * @param defStyle The default style to apply to this view
     */
    public CardEditor(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }


    /**
     * Checks if provided card information is valid
     *
     * @return True or False
     */
    @SuppressWarnings("unused")
    public boolean isValid() {
        return cardNumberValid && cardExpiryValid && cardCvcValid;
    }

    /**
     * Sets the enabled state on each EditText view
     *
     * @param enabled True or False
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        cardNumberEditText.setEnabled(enabled);
        cardExpiryEditText.setEnabled(enabled);
        cardCvcEditText.setEnabled(enabled);
    }

    /**
     * Builds a {@link Card} object from the information provided
     *
     * @return A Card object
     */
    public Card getCard() {
        // build card and return it
        return new Card()
                .setNumber(cardNumberEditText.getText().toString().replaceAll("[^\\d]+", ""))
                .setExpMonth(cardExpMonth)
                .setExpYear(cardExpYear)
                .setCvc(cardCvcEditText.getText().toString());
    }

    /**
     * Register a listener to receive notifications when the valid state of this view changes.
     * An instance of this editor is passed through the listener.
     * <p>
     * Check the current state by calling {@link #isValid()}
     *
     * @param listener The listener
     */
    @SuppressWarnings("unused")
    public void addOnStateChangedListener(OnStateChangedListener listener) {
        if (!onStateChangedListeners.contains(listener)) {
            onStateChangedListeners.add(listener);
        }
    }

    /**
     * Unregister a listener from receiving notifications when the valid state of this view changes.
     *
     * @param listener The listener
     */
    @SuppressWarnings("unused")
    public void removeOnStateChangedListener(OnStateChangedListener listener) {
        onStateChangedListeners.remove(listener);
    }

    /**
     * Sets a custom color for the EditText icons
     *
     * @param color A color
     */
    public void setIconColor(int color) {
        numberIcon = getTintedDrawable(R.drawable.simplify_number, color);
        Drawable expiryIcon = getTintedDrawable(R.drawable.simplify_expiry, color);
        Drawable cvcIcon = getTintedDrawable(R.drawable.simplify_cvc, color);

        if (cardNumberEditText.getText().toString().isEmpty()) {
            cardNumberEditText.setCompoundDrawablesWithIntrinsicBounds(numberIcon, null, null, null);
        }

        cardExpiryEditText.setCompoundDrawablesWithIntrinsicBounds(expiryIcon, null, null, null);
        cardCvcEditText.setCompoundDrawablesWithIntrinsicBounds(cvcIcon, null, null, null);
    }

    /**
     * Resets the view to an empty state. (Will not change the enabled state)
     */
    @SuppressWarnings("unused")
    public void reset() {
        // reset valid flags
        cardNumberValid = false;
        cardExpiryValid = false;
        cardCvcValid = false;

        // reset stored card info
        cardBrand = CardBrand.UNKNOWN;
        cardExpMonth = null;
        cardExpYear = null;

        // clear field errors
        cardNumberEditText.setError(null);
        cardExpiryEditText.setError(null);
        cardCvcEditText.setError(null);

        // temporarily remove text change listeners
        cardNumberEditText.removeTextChangedListener(mCardNumberTextWatcher);
        cardCvcEditText.removeTextChangedListener(mCardCvcTextWatcher);

        // clear text fields
        cardNumberEditText.setText(null);
        cardExpiryEditText.setText(null);
        cardCvcEditText.setText(null);

        // add the text change listeners back
        cardNumberEditText.addTextChangedListener(mCardNumberTextWatcher);
        cardCvcEditText.addTextChangedListener(mCardCvcTextWatcher);

        // notify listeners of possible valid state change
        notifyStateChanged();
    }

    private void init(AttributeSet attrs) {
        // inflate view
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.simplify_cardeditor, this, true);

        // init card number field
        cardNumberEditText = (EditText) view.findViewById(R.id.simplify_number);
        cardNumberEditText.addTextChangedListener(mCardNumberTextWatcher);
        cardNumberEditText.setOnFocusChangeListener(mCardNumberFocusChangeListener);

        // init card expiration field
        cardExpiryEditText = (EditText) view.findViewById(R.id.simplify_expiry);
        cardExpiryEditText.setOnFocusChangeListener(mCardExpiryOnFocusChangeListener);

        // init card cvc field
        cardCvcEditText = (EditText) view.findViewById(R.id.simplify_cvc);
        cardCvcEditText.addTextChangedListener(mCardCvcTextWatcher);
        cardCvcEditText.setOnFocusChangeListener(mCardCvcFocusChangeListener);

        // set customizations
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CardEditor, 0, 0);
        setIconColor(a.getColor(R.styleable.CardEditor_iconColor, ContextCompat.getColor(getContext(), R.color.simplify_default_icon)));
        setEnabled(a.getBoolean(R.styleable.CardEditor_enabled, true));

        // init the expiry dialog
        if (!isInEditMode()) {
            initExpirationDialog();
        }
    }

    private void initExpirationDialog() {
        View datePickerView = View.inflate(getContext(), R.layout.simplify_expiry, null);

        Calendar cal = Calendar.getInstance();

        // setup NumberPicker to select month of year
        monthPicker = (NumberPicker) datePickerView.findViewById(R.id.month_picker);
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(cal.get(Calendar.MONTH) + 1);
        monthPicker.setDisplayedValues(new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"});

        // setup NumberPicker to set year
        yearPicker = (NumberPicker) datePickerView.findViewById(R.id.year_picker);
        int year = cal.get(Calendar.YEAR);
        yearPicker.setMinValue(year);
        yearPicker.setMaxValue(year + 20);

        // use accent color for dividers
        TypedValue value = new TypedValue();
        boolean appCompat = getContext().getTheme().resolveAttribute(R.attr.colorAccent, value, true);

        if (appCompat) {
            Utils.setDividerColor(monthPicker, value.data);
            Utils.setDividerColor(yearPicker, value.data);

            expirationDialog = new android.support.v7.app.AlertDialog.Builder(getContext()).setTitle(R.string.simplify_expiration)
                    .setView(datePickerView)
                    .setPositiveButton(android.R.string.ok, mExpiryOkClickListener)
                    .setNegativeButton(android.R.string.cancel, mExpiryCancelClickListener)
                    .create();
        } else {
            expirationDialog = new AlertDialog.Builder(getContext()).setTitle(R.string.simplify_expiration)
                    .setView(datePickerView)
                    .setPositiveButton(android.R.string.ok, mExpiryOkClickListener)
                    .setNegativeButton(android.R.string.cancel, mExpiryCancelClickListener)
                    .create();
        }
    }

    private void notifyStateChanged() {
        // get current valid state
        boolean current = cardNumberValid && cardExpiryValid && cardCvcValid;

        // if different than previous, update and notify
        if (current ^ prevValid) {
            for (OnStateChangedListener listener : onStateChangedListeners) {
                listener.onStateChange(this);
            }

            prevValid = current;
        }
    }

    private Drawable getTintedDrawable(@DrawableRes int drawableResId, int color) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), drawableResId);
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        return drawable;
    }


    // ----------------------------------
    // Text Watchers
    // ----------------------------------

    private TextWatcher mCardNumberTextWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // clean the number
            String text = editable.toString().replaceAll("[^\\d]+", "");

            // detect the card type
            cardBrand = CardBrand.detect(text);

            // prevent too many digits
            if (text.length() > cardBrand.getMaxLength()) {
                text = text.substring(0, cardBrand.getMaxLength());
            }

            // format the number
            String value = cardBrand.format(text);

            // replace text with formatted
            cardNumberEditText.removeTextChangedListener(this);
            cardNumberEditText.setText(value);
            cardNumberEditText.setSelection(value.length());
            cardNumberEditText.addTextChangedListener(this);

            // fetch the card brand drawable
            Drawable brandDrawable = numberIcon;
            if (!text.isEmpty()) {
                switch (cardBrand) {
                    case MASTERCARD:
                        brandDrawable = ContextCompat.getDrawable(getContext(), R.drawable.simplify_mastercard);
                        break;
                    case VISA:
                        brandDrawable = ContextCompat.getDrawable(getContext(), R.drawable.simplify_visa);
                        break;
                    case AMERICAN_EXPRESS:
                        brandDrawable = ContextCompat.getDrawable(getContext(), R.drawable.simplify_amex);
                        break;
                    case DISCOVER:
                        brandDrawable = ContextCompat.getDrawable(getContext(), R.drawable.simplify_discover);
                        break;
                    case DINERS:
                        brandDrawable = ContextCompat.getDrawable(getContext(), R.drawable.simplify_diners);
                        break;
                    case JCB:
                        brandDrawable = ContextCompat.getDrawable(getContext(), R.drawable.simplify_jcb);
                        break;
                    case UNKNOWN:
                        brandDrawable = ContextCompat.getDrawable(getContext(), R.drawable.simplify_generic);
                        break;
                }
            }

            // change the drawable to match type
            cardNumberEditText.setCompoundDrawablesWithIntrinsicBounds(brandDrawable, null, null, null);

            cardNumberValid = Utils.validateCardNumber(text, cardBrand);
            cardNumberEditText.setError(null);

            notifyStateChanged();
        }
    };

    private TextWatcher mCardCvcTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // clean the number
            String text = editable.toString();

            // prevent too many digits
            if (text.length() > cardBrand.getCvcLength()) {
                text = text.substring(0, cardBrand.getCvcLength());
            }

            // replace text with formatted
            cardCvcEditText.removeTextChangedListener(this);
            cardCvcEditText.setText(text);
            cardCvcEditText.setSelection(text.length());
            cardCvcEditText.addTextChangedListener(this);

            // set the view state (changes background color)
            cardCvcValid = Utils.validateCardCvc(text, cardBrand);
            cardCvcEditText.setError(null);

            notifyStateChanged();
        }
    };

    // ----------------------------------
    // Focus Listeners
    // ----------------------------------

    private OnFocusChangeListener mCardNumberFocusChangeListener = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            if (!hasFocus) {
                String text = cardNumberEditText.getText().toString().replaceAll("[^\\d]+", "");
                if (text.length() == 0) {
                    cardNumberValid = false;
                    cardNumberEditText.setError(null);
                } else {
                    cardBrand = CardBrand.detect(text);

                    cardNumberValid = Utils.validateCardNumber(text, cardBrand);
                    cardNumberEditText.setError(cardNumberValid ? null : getContext().getString(R.string.simplify_invalid_card_number));
                }

                notifyStateChanged();
            }
        }
    };

    private OnFocusChangeListener mCardExpiryOnFocusChangeListener = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                // hide keyboard
                cardExpiryEditText.clearFocus();
                ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindowToken(), 0);

                expirationDialog.show();
            }
        }
    };

    private OnFocusChangeListener mCardCvcFocusChangeListener = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            if (!hasFocus) {
                String text = cardCvcEditText.getText().toString();
                if (text.length() == 0) {
                    cardCvcValid = false;
                    cardCvcEditText.setError(null);
                } else {
                    cardCvcValid = Utils.validateCardCvc(text, cardBrand);
                    cardCvcEditText.setError(cardCvcValid ? null : getContext().getString(R.string.simplify_invalid_cvc));
                }

                notifyStateChanged();
            }
        }
    };

    private DialogInterface.OnClickListener mExpiryOkClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            cardExpMonth = String.format(Locale.getDefault(), "%02d", monthPicker.getValue());
            cardExpYear = String.valueOf(yearPicker.getValue() % 100);
            cardExpiryEditText.setText(getContext().getString(R.string.simplify_mm_yy, cardExpMonth, cardExpYear));

            dialog.dismiss();

            cardExpiryValid = Utils.validateCardExpiration(cardExpMonth, cardExpYear);
            cardExpiryEditText.setError(cardExpiryValid ? null : getContext().getString(R.string.simplify_invalid_expiration));

            notifyStateChanged();
        }
    };

    private DialogInterface.OnClickListener mExpiryCancelClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
        }
    };
}


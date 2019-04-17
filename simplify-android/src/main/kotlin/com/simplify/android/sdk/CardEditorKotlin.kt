package com.simplify.android.sdk

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.simplify.android.sdk.databinding.SimplifyCardeditorBinding
import java.util.*

class CardEditorKotlin constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : LinearLayout(context, attrs, defStyle) {

    /**
     * A callback interface used to report when the provided card information's
     * overall valid state changes
     */
    interface OnStateChangedListener {
        fun onStateChange(cardEditor: CardEditorKotlin)
    }

    private var binding: SimplifyCardeditorBinding

    internal lateinit var numberIcon: Drawable

    internal lateinit var expirationDialog: Dialog
    internal lateinit var monthPicker: NumberPicker
    internal lateinit var yearPicker: NumberPicker

    private val cardNumberTextWatcher = CardNumberTextWatcher()
    private val cardCvcTextWatcher = CardCvcTextWatcher()

    internal var cardBrand = CardBrand.UNKNOWN
    internal var cardExpMonth: String? = null
    internal var cardExpYear: String? = null

    internal var prevValid = false
    internal var cardNumberValid = false
    internal var cardExpiryValid = false
    internal var cardCvcValid = false

    internal var onStateChangedListeners = ArrayList<OnStateChangedListener>()

    init {
        // inflate view
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.simplify_cardeditor, this, true)

        // init card number field
        binding.simplifyNumber.addTextChangedListener(cardNumberTextWatcher)
        binding.simplifyNumber.onFocusChangeListener = OnFocusChangeListener { _, hasFocus -> onCardNumberFocusChange(hasFocus) }

        // init card expiration field
        binding.simplifyExpiry.onFocusChangeListener = OnFocusChangeListener { _, hasFocus -> onCardExpiryFocusChange(hasFocus) }

        // init card cvc field
        binding.simplifyCvc.addTextChangedListener(cardCvcTextWatcher)
        binding.simplifyCvc.onFocusChangeListener = OnFocusChangeListener { _, hasFocus -> onCardCvcFocusChange(hasFocus) }

        // set customizations
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.CardEditorKotlin, 0, 0)
        setIconColor(a.getColor(R.styleable.CardEditorKotlin_iconColor, ContextCompat.getColor(context, R.color.simplify_default_icon)))
        isEnabled = a.getBoolean(R.styleable.CardEditorKotlin_enabled, true)

        // init the expiry dialog
        if (!isInEditMode) {
            initExpirationDialog()
        }
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    /**
     * Indicates if provided card information is valid
     */
    val isValid: Boolean
        get() = cardNumberValid && cardExpiryValid && cardCvcValid

    /**
     * Builds a [SimplifyMap] object containing the card information provided
     */
    val card: SimplifyMap
        get() = SimplifyMap()
                .set("number", binding.simplifyNumber.text.toString().replace("[^\\d]+".toRegex(), ""))
                .set("expMonth", cardExpMonth)
                .set("expYear", cardExpYear)
                .set("cvc", binding.simplifyCvc.text.toString())

    /**
     * Sets the enabled state on each EditText view
     *
     * @param enabled True or False
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        binding.simplifyNumber.isEnabled = enabled
        binding.simplifyExpiry.isEnabled = enabled
        binding.simplifyCvc.isEnabled = enabled
    }

    /**
     * Register a listener to receive notifications when the valid state of this view changes.
     * An instance of this editor is passed through the listener.
     * <br/>
     * Check the current state by calling [.isValid]
     *
     * @param listener The listener
     */
    fun addOnStateChangedListener(listener: OnStateChangedListener) {
        if (!onStateChangedListeners.contains(listener)) {
            onStateChangedListeners.add(listener)
        }
    }

    /**
     * Unregister a listener from receiving notifications when the valid state of this view changes.
     *
     * @param listener The listener
     */
    fun removeOnStateChangedListener(listener: OnStateChangedListener) {
        onStateChangedListeners.remove(listener)
    }

    /**
     * Sets a custom color for the EditText icons
     *
     * @param color A color
     */
    fun setIconColor(color: Int) {
        numberIcon = getTintedDrawable(R.drawable.simplify_number, color)
        val expiryIcon = getTintedDrawable(R.drawable.simplify_expiry, color)
        val cvcIcon = getTintedDrawable(R.drawable.simplify_cvc, color)

        if (binding.simplifyNumber.text.toString().isEmpty()) {
            binding.simplifyNumber.setCompoundDrawablesWithIntrinsicBounds(numberIcon, null, null, null)
        }

        binding.simplifyExpiry.setCompoundDrawablesWithIntrinsicBounds(expiryIcon, null, null, null)
        binding.simplifyCvc.setCompoundDrawablesWithIntrinsicBounds(cvcIcon, null, null, null)
    }

    /**
     * Resets the view to an empty state. (Will not change the enabled state)
     */
    fun reset() {
        // reset valid flags
        cardNumberValid = false
        cardExpiryValid = false
        cardCvcValid = false

        // reset stored card info
        cardBrand = CardBrand.UNKNOWN
        cardExpMonth = null
        cardExpYear = null

        // clear field errors
        binding.simplifyNumber.error = null
        binding.simplifyExpiry.error = null
        binding.simplifyCvc.error = null

        // temporarily remove text change listeners
        binding.simplifyNumber.removeTextChangedListener(cardNumberTextWatcher)
        binding.simplifyCvc.removeTextChangedListener(cardCvcTextWatcher)

        // clear text fields
        binding.simplifyNumber.text = null
        binding.simplifyExpiry.text = null
        binding.simplifyCvc.text = null

        // add the text change listeners back
        binding.simplifyNumber.addTextChangedListener(cardNumberTextWatcher)
        binding.simplifyCvc.addTextChangedListener(cardCvcTextWatcher)

        // notify listeners of possible valid state change
        notifyStateChanged()
    }


    private fun initExpirationDialog() {
        val datePickerView = View.inflate(context, R.layout.simplify_expiry, null)

        val cal = Calendar.getInstance()

        // setup NumberPicker to select month of year
        monthPicker = datePickerView.findViewById<View>(R.id.month_picker) as NumberPicker
        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = cal.get(Calendar.MONTH) + 1
        monthPicker.displayedValues = arrayOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")

        // setup NumberPicker to set year
        yearPicker = datePickerView.findViewById<View>(R.id.year_picker) as NumberPicker
        val year = cal.get(Calendar.YEAR)
        yearPicker.minValue = year
        yearPicker.maxValue = year + 20

        // use accent color for dividers
        val value = TypedValue()
        val appCompat = context.theme.resolveAttribute(R.attr.colorAccent, value, true)

        if (appCompat) {
            setDividerColor(monthPicker, value.data)
            setDividerColor(yearPicker, value.data)

            expirationDialog = AlertDialog.Builder(context).setTitle(R.string.simplify_expiration)
                    .setView(datePickerView)
                    .setPositiveButton(android.R.string.ok) { dialog, _ -> onExpiryClickOk(dialog)}
                    .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .create()
        } else {
            expirationDialog = AlertDialog.Builder(context).setTitle(R.string.simplify_expiration)
                    .setView(datePickerView)
                    .setPositiveButton(android.R.string.ok) { dialog, _ -> onExpiryClickOk(dialog)}
                    .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .create()
        }
    }

    private fun notifyStateChanged() {
        // get current valid state
        val current = cardNumberValid && cardExpiryValid && cardCvcValid

        // if different than previous, update and notify
        if (current xor prevValid) {
            for (listener in onStateChangedListeners) {
                listener.onStateChange(this)
            }

            prevValid = current
        }
    }

    private fun getTintedDrawable(@DrawableRes drawableResId: Int, color: Int): Drawable {
        val drawable = ContextCompat.getDrawable(context, drawableResId)
        drawable!!.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        return drawable
    }

    private fun onCardNumberFocusChange(hasFocus: Boolean) {
        if (!hasFocus) {
            val text = binding.simplifyNumber.text.toString().replace("[^\\d]+".toRegex(), "")
            if (text.isEmpty()) {
                cardNumberValid = false
                binding.simplifyNumber.error = null
            } else {
                cardBrand = CardBrand.detect(text)

                cardNumberValid = validateCardNumber(text, cardBrand)
                binding.simplifyNumber.error = if (cardNumberValid) null else context.getString(R.string.simplify_invalid_card_number)
            }

            notifyStateChanged()
        }
    }

    private fun onCardExpiryFocusChange(hasFocus: Boolean) {
        if (hasFocus) {
            // hide keyboard
            binding.simplifyExpiry.clearFocus()
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(windowToken, 0)

            expirationDialog.show()
        }
    }

    private fun onCardCvcFocusChange(hasFocus: Boolean) {
        if (!hasFocus) {
            val text = binding.simplifyCvc.text.toString()
            if (text.isEmpty()) {
                cardCvcValid = false
                binding.simplifyCvc.error = null
            } else {
                cardCvcValid = validateCardCvc(text, cardBrand)
                binding.simplifyCvc.error = if (cardCvcValid) null else context.getString(R.string.simplify_invalid_cvc)
            }

            notifyStateChanged()
        }
    }

    private fun onExpiryClickOk(dialog: DialogInterface) {
        cardExpMonth = String.format(Locale.getDefault(), "%02d", monthPicker.value)
        cardExpYear = (yearPicker.value % 100).toString()
        binding.simplifyExpiry.setText("$cardExpMonth/$cardExpYear")

        dialog.dismiss()

        cardExpiryValid = validateCardExpiration(cardExpMonth!!, cardExpYear!!)
        binding.simplifyExpiry.error = if (cardExpiryValid) null else context.getString(R.string.simplify_invalid_expiration)

        notifyStateChanged()
    }

    /**
     * Validates that a card number passes Luhn and matches minimum length
     * requirements for a specific card type
     *
     * @param n The card number
     * @param brand  The card type to validate against
     * @return True or False
     */
    private fun validateCardNumber(n: String, brand: CardBrand): Boolean {
        var number = n

        // numbers only, please
        number = number.replace("[^\\d]+".toRegex(), "")

        // match against type prefix
        if (!brand.prefixMatches(number)) {
            return false
        }

        // ensure minimum length is satisfied
        val length = number.length
        if (length == 0 || length < brand.getMinLength()) {
            return false
        }

        var sum = 0
        run {
            var i = length - 2
            while (i >= 0) {
                val c = number[i]
                if (c < '0' || c > '9') return false

                // Multiply digit by 2.
                val v = c - '0' shl 1

                // Add each digit independently.
                sum += if (v > 9) 1 + v - 10 else v
                i -= 2
            }
        }

        // Add the rest of the non-doubled digits
        run {
            var i = length - 1
            while (i >= 0) {
                sum += number[i] - '0'
                i -= 2
            }
        }

        // Double check that the Luhn check-digit at the end brings us to a neat multiple of 10
        return sum % 10 == 0
    }

    /**
     * Validates that a provided expiration date is in the future
     *
     * @param month The expiration month, format: MM
     * @param year  The expiration year, format: YY
     * @return True or False
     */
    private fun validateCardExpiration(month: String, year: String): Boolean {
        if (month.trim().isEmpty() || year.trim().isEmpty()) {
            return false
        }

        val intMonth = Integer.parseInt(month)
        var intYear = Integer.parseInt(year)

        if (intYear < 100) {
            intYear += 2000
        }

        if (intMonth == 0) {
            return false
        }

        val now = Calendar.getInstance()

        val expire = Calendar.getInstance()
        expire.set(intYear, intMonth - 1, 1, 0, 0, 0)
        expire.add(Calendar.MONTH, 1)

        return now.before(expire)
    }

    /**
     * Validates that a cvc code matches the length required by the card type
     *
     * @param cvc   The cvc code
     * @param brand The card type
     * @return True or False
     */
    private fun validateCardCvc(cvc: String, brand: CardBrand): Boolean {
        return cvc.trim().length == brand.getCvcLength()
    }


    private fun setDividerColor(picker: NumberPicker, color: Int) {
        val numberPickerFields = NumberPicker::class.java.declaredFields
        for (field in numberPickerFields) {
            if (field.name == "mSelectionDivider") {
                field.isAccessible = true
                try {
                    field.set(picker, ColorDrawable(color))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                break
            }
        }
    }

    // ----------------------------------
    // Text Watchers
    // ----------------------------------

    private inner class CardNumberTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable) {
            // clean the number
            var text = editable.toString().replace("[^\\d]+".toRegex(), "")

            // detect the card type
            cardBrand = CardBrand.detect(text)

            // prevent too many digits
            if (text.length > cardBrand.getMaxLength()) {
                text = text.substring(0, cardBrand.getMaxLength())
            }

            // format the number
            val value = cardBrand.format(text)

            // replace text with formatted
            binding.simplifyNumber.removeTextChangedListener(this)
            binding.simplifyNumber.setText(value)
            binding.simplifyNumber.setSelection(value.length)
            binding.simplifyNumber.addTextChangedListener(this)

            // fetch the card brand drawable
            var brandDrawable: Drawable? = numberIcon
            if (!text.isEmpty()) {
                when (cardBrand) {
                    CardBrand.MASTERCARD -> brandDrawable = ContextCompat.getDrawable(context, R.drawable.simplify_mastercard)
                    CardBrand.VISA -> brandDrawable = ContextCompat.getDrawable(context, R.drawable.simplify_visa)
                    CardBrand.AMERICAN_EXPRESS -> brandDrawable = ContextCompat.getDrawable(context, R.drawable.simplify_amex)
                    CardBrand.DISCOVER -> brandDrawable = ContextCompat.getDrawable(context, R.drawable.simplify_discover)
                    CardBrand.DINERS -> brandDrawable = ContextCompat.getDrawable(context, R.drawable.simplify_diners)
                    CardBrand.JCB -> brandDrawable = ContextCompat.getDrawable(context, R.drawable.simplify_jcb)
                    CardBrand.UNKNOWN -> brandDrawable = ContextCompat.getDrawable(context, R.drawable.simplify_generic)
                }
            }

            // change the drawable to match type
            binding.simplifyNumber.setCompoundDrawablesWithIntrinsicBounds(brandDrawable, null, null, null)

            cardNumberValid = validateCardNumber(text, cardBrand)
            binding.simplifyNumber.error = null

            notifyStateChanged()
        }
    }

    private inner class CardCvcTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable) {
            // clean the number
            var text = editable.toString()

            // prevent too many digits
            if (text.length > cardBrand.getCvcLength()) {
                text = text.substring(0, cardBrand.getCvcLength())
            }

            // replace text with formatted
            binding.simplifyCvc.removeTextChangedListener(this)
            binding.simplifyCvc.setText(text)
            binding.simplifyCvc.setSelection(text.length)
            binding.simplifyCvc.addTextChangedListener(this)

            // set the view state (changes background color)
            cardCvcValid = validateCardCvc(text, cardBrand)
            binding.simplifyCvc.error = null

            notifyStateChanged()
        }
    }
}
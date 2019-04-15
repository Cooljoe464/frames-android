package com.checkout.sdk.billingdetails

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.checkout.sdk.R
import com.checkout.sdk.architecture.MvpView
import com.checkout.sdk.architecture.PresenterStore
import com.checkout.sdk.store.DataStore
import com.checkout.sdk.store.InMemoryStore
import kotlinx.android.synthetic.main.billing_details.view.*

/**
 * The controller of the billing details view page
 *
 *
 * This class handles interaction with the custom inputs in the billing details form.
 * The state of the view is handled here, so are action like focus changes, full form
 * validation, listeners, persistence over orientation.
 */
class BillingDetailsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs),
    MvpView<BillingDetailsUiState> {

    private var listener: BillingDetailsView.Listener? = null
    private val dataStore: DataStore = DataStore.Factory.get()
    private val inMemoryStore: InMemoryStore = InMemoryStore.Factory.get()

    override fun onStateUpdated(uiState: BillingDetailsUiState) {
        if (!uiState.countries.isEmpty() && country_input.adapter == null) {
            val adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                uiState.countries
            )
            country_input.adapter = adapter
        }
        if (country_input.selectedItemPosition != uiState.position) {
            country_input.setSelection(uiState.position)
        }
        uiState.billingDetailsValidity?.let {
            updateFieldValidity(it)
            if (it.areDetailsValid()) {
                listener?.onBillingCompleted()
            }
        }
    }

    private fun updateFieldValidity(validity: BillingDetailsValidity) {
        name_input.showError(!validity.nameValid)
        address_one_input.showError(!validity.addressOneValid)
        address_two_input.showError(!validity.addressTwoValid)
        city_input.showError(!validity.cityValid)
        state_input.showError(!validity.stateValid)
        zipcode_input.showError(!validity.zipcodeValid)
        showCountryError(validity)
        phone_input.showError(!validity.phoneValid)
    }

    private fun showCountryError(validity: BillingDetailsValidity) {
        val errorView = country_input.selectedView as? TextView
        errorView?.let {
            if (validity.countryValid) {
                it.error = null
            } else {
                it.error = resources.getString(R.string.error_country)
            }
        }
    }

    /**
     * The callback used to indicate is the billing details were completed
     *
     *
     * After the user completes their details and the form is valid this callback will
     * be used to communicate to the parent that teh focus needs to change
     */
    interface Listener {
        fun onBillingCompleted()

        fun onBillingCanceled()
    }

    private var presenter: BillingDetailsPresenter

    init {
        inflate(this.context, R.layout.billing_details, this)
        orientation = VERTICAL
        isFocusableInTouchMode = true
        val positionZeroString = context.getString(R.string.placeholder_country)
        presenter = PresenterStore.getOrCreate(
            BillingDetailsPresenter::class.java,
            {
                BillingDetailsPresenter(
                    BillingDetailsUiState.create(
                        inMemoryStore,
                        positionZeroString
                    )
                )
            })
        presenter.start(this)
        phone_input.listenForRepositoryChange()

        my_toolbar.setNavigationOnClickListener {
            if (dataStore.lastBillingValidState != null) {
                dataStore.customerName = dataStore.lastCustomerNameState!!
                dataStore.customerAddress1 = dataStore.lastBillingValidState!!.addressOne.value
                dataStore.customerAddress2 = dataStore.lastBillingValidState!!.addressTwo.value
                dataStore.customerZipcode = dataStore.lastBillingValidState!!.postcode.value
                dataStore.customerCountry = dataStore.lastBillingValidState!!.country
                dataStore.customerCity = dataStore.lastBillingValidState!!.city.value
                dataStore.customerState = dataStore.lastBillingValidState!!.state.value
                dataStore.customerPhonePrefix = dataStore.lastBillingValidState!!.phone.countryCode
                dataStore.customerPhone = dataStore.lastBillingValidState!!.phone.number
                listener?.onBillingCompleted()
            } else {
                listener?.onBillingCanceled()
            }
        }
        country_input.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val countrySelectedUseCaseBuilder =
                    CountrySelectedUseCase.Builder(inMemoryStore, position)
                presenter.countrySelected(countrySelectedUseCaseBuilder)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Nothing
            }
        }

        clear_button.setOnClickListener {
            resetFields()
        }

        done_button.setOnClickListener {
            val doneButtonClickedUseCase =
                DoneButtonClickedUseCase(BillingFormValidator(inMemoryStore))
            presenter.doneButtonClicked(doneButtonClickedUseCase)
        }
        requestFocus()
    }

    /**
     * Used to clear the text and state of the fields
     */
    fun resetFields() {
        name_input.reset()
        address_one_input.reset()
        address_two_input.reset()
        city_input.reset()
        state_input.reset()
        zipcode_input.reset()
        country_input.setSelection(0)
        phone_input.reset()
    }

    /**
     * Used to set the callback listener for when the card details page is requested
     */
    fun setGoToCardDetailsListener(listener: BillingDetailsView.Listener) {
        this.listener = listener
    }
}

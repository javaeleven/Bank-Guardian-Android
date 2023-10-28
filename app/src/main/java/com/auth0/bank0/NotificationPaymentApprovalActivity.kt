/*
 * Copyright (c) 2016 Auth0 (http://auth0.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.auth0.bank0

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.auth0.android.guardian.sdk.ConsentEntity
import com.auth0.android.guardian.sdk.ConsentService
import com.auth0.android.guardian.sdk.Guardian
import com.auth0.android.guardian.sdk.ParcelableNotification
import com.auth0.android.guardian.sdk.networking.Callback

class NotificationPaymentApprovalActivity : AppCompatActivity() {
    private var browserText: TextView? = null
    private var osText: TextView? = null
    private var dateText: TextView? = null
    private var paymentDescription: TextView? = null
    private var paymentAmount: TextView? = null
    private lateinit var guardian: Guardian
    private lateinit var consentService: ConsentService
    private var enrollment: ParcelableEnrollment? = null
    private var notification: ParcelableNotification? = null
    private lateinit var consentEntity: ConsentEntity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_approval_notification)
        guardian = Guardian.Builder().url(Uri.parse(getString(R.string.guardian_url))).enableLogging().build()

        consentService = ConsentService.Builder()
                .url(Uri.parse(getString(R.string.tenant_url)))
                .authorizingClientId(getString(R.string.authorizing_client_id))
                .enableLogging()
                .build()

        val intent = intent
        enrollment = intent.getParcelableExtra(Constants.ENROLLMENT)
        notification = intent.getParcelableExtra(Constants.NOTIFICATION)
        setupUI()
        updateUI()
    }

    private fun getPaymentDetailsFromConsentAPI(linkingId: String?) {
        if (linkingId == null) {
            return
        }
        consentService.getConsent(notification!!, enrollment!!).start(DialogCallback(this, R.string.progress_title_please_wait, R.string.progress_message_reject, object : Callback<ConsentEntity> {
            override fun onSuccess(response: ConsentEntity) {

                consentEntity = response

                if (response.authorization_details != null && response.authorization_details.size > 0) {
                    var payment = response.authorization_details[0]
                    runOnUiThread {
                        paymentDescription!!.text = payment["description"] as String
                        paymentAmount!!.text = "$" + (payment["amount"] as Double).toBigDecimal().toPlainString()
                    }
                }


            }

            override fun onFailure(exception: Throwable) {}
        }))

    }

    private fun setupUI() {
        browserText = findViewById<View>(R.id.browserText) as TextView
        osText = findViewById<View>(R.id.osText) as TextView
        dateText = findViewById<View>(R.id.dateText) as TextView
        paymentDescription = findViewById<View>(R.id.paymentDescription) as TextView
        paymentAmount = findViewById<View>(R.id.amount) as TextView

        val rejectButton = (findViewById<View>(R.id.rejectButton) as Button)
        rejectButton.setOnClickListener { rejectRequested() }
        val allowButton = (findViewById<View>(R.id.allowButton) as Button)
        allowButton.setOnClickListener { allowRequested() }

        val spinner: Spinner = findViewById(R.id.bank_accounts_spinner)
        ArrayAdapter.createFromResource(this, R.array.bank_accounts_array, android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }

    private fun updateUI() {
        browserText!!.text = String.format("%s, %s", notification!!.browserName, notification!!.browserVersion)
        osText!!.text = String.format("%s, %s", notification!!.osName, notification!!.osVersion)
        dateText!!.text = notification!!.date.toString()

        getPaymentDetailsFromConsentAPI(notification!!.transactionLinkingId.toString())
    }

    private fun rejectRequested() {
        consentService
                .reject(notification!!, enrollment!!)
                .start(DialogCallback(this,
                        R.string.progress_title_please_wait,
                        R.string.progress_message_reject,
                        object : Callback<Void?> {
                            override fun onSuccess(response: Void?) {
                                guardian
                                        .reject(notification!!, enrollment!!)
                                        .start(
                                                object : Callback<Void?> {
                                                    override fun onSuccess(response: Void?) {
                                                        finish()
                                                    }

                                                    override fun onFailure(exception: Throwable) {}
                                                })

                            }

                            override fun onFailure(exception: Throwable) {}
                        }))
    }

    private fun allowRequested() {
        val spinner = findViewById<View>(R.id.bank_accounts_spinner) as Spinner
        val selectedBankAccount = spinner.selectedItem.toString()

        val authorizationDetails = consentEntity.authorization_details
        authorizationDetails[0]["source_bank_account"] = selectedBankAccount

        consentService.authorize(notification!!, enrollment!!, authorizationDetails)
                .start(DialogCallback(this, R.string.progress_title_please_wait, R.string.progress_message_allow, object : Callback<Void?> {
                    override fun onSuccess(response: Void?) {

                        guardian.allow(notification!!, enrollment!!).start(object : Callback<Void?> {
                            override fun onSuccess(response: Void?) {
                                finish()
                            }

                            override fun onFailure(exception: Throwable) {}
                        })

                    }

                    override fun onFailure(exception: Throwable) {}
                }))

    }

    companion object {
        fun getStartIntent(context: Context, notification: ParcelableNotification, enrollment: ParcelableEnrollment): Intent {
            if (enrollment.id != notification.enrollmentId) {
                val message = String.format("Notification doesn't match enrollment (%s != %s)", notification.enrollmentId, enrollment.id)
                throw IllegalArgumentException(message)
            }
            val intent = Intent(context, NotificationPaymentApprovalActivity::class.java)
            intent.putExtra(Constants.ENROLLMENT, enrollment)
            intent.putExtra(Constants.NOTIFICATION, notification)
            return intent
        }
    }
}
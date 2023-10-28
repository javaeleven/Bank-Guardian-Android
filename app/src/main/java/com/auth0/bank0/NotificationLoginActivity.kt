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
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.auth0.android.guardian.sdk.Guardian
import com.auth0.android.guardian.sdk.ParcelableNotification
import com.auth0.android.guardian.sdk.networking.Callback

class NotificationLoginActivity : AppCompatActivity() {
    private var browserText: TextView? = null
    private var osText: TextView? = null
    private var dateText: TextView? = null
    private lateinit var guardian: Guardian
    private var enrollment: ParcelableEnrollment? = null
    private var notification: ParcelableNotification? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_notification)
        guardian = Guardian.Builder()
                .url(Uri.parse(getString(R.string.guardian_url)))
                .enableLogging()
                .build()
        val intent = intent
        enrollment = intent.getParcelableExtra(Constants.ENROLLMENT)
        notification = intent.getParcelableExtra(Constants.NOTIFICATION)
        setupUI()
        updateUI()
    }

    private fun setupUI() {
        browserText = findViewById<View>(R.id.browserText) as TextView
        osText = findViewById<View>(R.id.osText) as TextView
        dateText = findViewById<View>(R.id.dateText) as TextView

        val rejectButton = (findViewById<View>(R.id.rejectButton) as Button)
        rejectButton.setOnClickListener { rejectRequested() }
        val allowButton = (findViewById<View>(R.id.allowButton) as Button)
        allowButton.setOnClickListener { allowRequested() }
    }

    private fun updateUI() {
        browserText!!.text = String.format(
                "%s, %s",
                notification!!.browserName,
                notification!!.browserVersion
        )
        osText!!.text = String.format(
                "%s, %s",
                notification!!.osName,
                notification!!.osVersion
        )
        dateText!!.text = notification!!.date.toString()
    }

    private fun rejectRequested() {
        guardian
                .reject(notification!!, enrollment!!)
                .start(DialogCallback(this,
                        R.string.progress_title_please_wait,
                        R.string.progress_message_reject,
                        object : Callback<Void?> {
                            override fun onSuccess(response: Void?) {
                                finish()
                            }

                            override fun onFailure(exception: Throwable) {}
                        }
                ))
    }

    private fun allowRequested() {
        guardian
                .allow(notification!!, enrollment!!)
                .start(DialogCallback(this,
                        R.string.progress_title_please_wait,
                        R.string.progress_message_allow,
                        object : Callback<Void?> {
                            override fun onSuccess(response: Void?) {
                                finish()
                            }

                            override fun onFailure(exception: Throwable) {}
                        }
                ))
    }

    companion object {
        fun getStartIntent(
                context: Context,
                notification: ParcelableNotification,
                enrollment: ParcelableEnrollment
        ): Intent {
            if (enrollment.id != notification.enrollmentId) {
                val message = String.format(
                        "Notification doesn't match enrollment (%s != %s)",
                        notification.enrollmentId, enrollment.id
                )
                throw IllegalArgumentException(message)
            }
            val intent = Intent(context, NotificationLoginActivity::class.java)
            intent.putExtra(Constants.ENROLLMENT, enrollment)
            intent.putExtra(Constants.NOTIFICATION, notification)
            return intent
        }
    }
}
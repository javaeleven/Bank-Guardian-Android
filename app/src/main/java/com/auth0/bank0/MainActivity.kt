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
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.auth0.android.guardian.sdk.Guardian
import com.auth0.android.guardian.sdk.GuardianException
import com.auth0.android.guardian.sdk.ParcelableNotification
import com.auth0.android.guardian.sdk.networking.Callback
import com.auth0.bank0.events.GuardianNotificationReceivedEvent
import com.auth0.bank0.fcm.FcmUtils
import com.auth0.bank0.fcm.FcmUtils.FcmTokenListener
import com.auth0.bank0.views.TOTPCodeView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity(), FcmTokenListener {
    private var loadingView: View? = null
    private var enrollView: View? = null
    private var accountView: View? = null
    private var deviceNameText: TextView? = null
    private var fcmTokenText: TextView? = null
    private var userText: TextView? = null
    private var otpView: TOTPCodeView? = null
    private lateinit var eventBus: EventBus
    private var guardian: Guardian? = null
    private var enrollment: ParcelableEnrollment? = null
    private var fcmToken: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupUI()
        eventBus = EventBus.getDefault()
        eventBus.register(this)
        guardian = Guardian.Builder()
                .url(Uri.parse(getString(R.string.guardian_url)))
                .enableLogging()
                .build()

        /*
         * The following fetch token call is NOT required in a production app
         * as the registration token is generated automatically by the Firebase SDK.
         * This is just here for display purposes on this Activity's layout.
         *
         * See: https://developers.google.com/cloud-messaging/android/android-migrate-iid-service
         */
        val fcmUtils = FcmUtils()
        fcmUtils.fetchFcmToken(this)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val enrollmentJSON = sharedPreferences.getString(Constants.ENROLLMENT, null)
        if (enrollmentJSON != null) {
            enrollment = ParcelableEnrollment.Companion.fromJSON(enrollmentJSON)
            updateUI()
            val notification =
                    intent.getParcelableExtra<ParcelableNotification>(Constants.NOTIFICATION)
            notification?.let { onPushNotificationReceived(it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        eventBus!!.unregister(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ENROLL_REQUEST) {
            if (resultCode == RESULT_OK) {
                val enrollment =
                        data!!.getParcelableExtra<ParcelableEnrollment>(Constants.ENROLLMENT)
                updateEnrollment(enrollment)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun setupUI() {
        loadingView = findViewById(R.id.loadingLayout)
        enrollView = findViewById(R.id.enrollLayout)
        accountView = findViewById(R.id.accountLayout)
        deviceNameText = findViewById<View>(R.id.deviceNameText) as TextView
        fcmTokenText = findViewById<View>(R.id.fcmTokenText) as TextView
        userText = findViewById<View>(R.id.userText) as TextView
        otpView = findViewById<View>(R.id.otpView) as TOTPCodeView
        deviceNameText!!.text = Build.MODEL
        val enrollButton = (findViewById<View>(R.id.enrollButton) as Button)
        enrollButton.setOnClickListener { onEnrollRequested() }
        val unenrollButton = (findViewById<View>(R.id.unenrollButton) as Button)
        unenrollButton.setOnClickListener { onUnEnrollRequested() }
    }

    private fun updateUI() {
        runOnUiThread {
            loadingView!!.visibility =
                    if (fcmToken != null) View.GONE else View.VISIBLE
            if (enrollment == null) {
                fcmTokenText!!.text = fcmToken
                accountView!!.visibility = View.GONE
                enrollView!!.visibility = if (fcmToken != null) View.VISIBLE else View.GONE
            } else {
                userText!!.text = enrollment!!.userId
                otpView!!.setEnrollment(enrollment!!)
                enrollView!!.visibility = View.GONE
                accountView!!.visibility =
                        if (fcmToken != null) View.VISIBLE else View.GONE
            }
        }
    }

    private fun onEnrollRequested() {
        val enrollIntent: Intent = EnrollActivity.Companion.getStartIntent(
                this,
                deviceNameText!!.text.toString(),
                fcmToken!!
        )
        startActivityForResult(enrollIntent, ENROLL_REQUEST)
    }

    private fun onUnEnrollRequested() {
        guardian!!.delete(enrollment!!)
                .start(DialogCallback(this,
                        R.string.progress_title_please_wait,
                        R.string.progress_message_unenroll,
                        object : Callback<Void?> {
                            override fun onSuccess(response: Void?) {
                                updateEnrollment(null)
                            }

                            override fun onFailure(exception: Throwable) {
                                if (exception is GuardianException) {
                                    if (exception.isEnrollmentNotFound) {
                                        // the enrollment doesn't exist on the server
                                        updateEnrollment(null)
                                    }
                                }
                            }
                        }
                ))
    }

    private fun updateEnrollment(enrollment: ParcelableEnrollment?) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPreferences.edit()
        editor.putString(Constants.ENROLLMENT, enrollment?.toJSON())
        editor.apply()
        this.enrollment = enrollment
        updateUI()
    }

    private fun onPushNotificationReceived(notification: ParcelableNotification?) {
        val intent: Intent = if (notification?.transactionLinkingId != null) {
            NotificationPaymentApprovalActivity.Companion.getStartIntent(this, notification!!, enrollment!!)
        } else {
            NotificationLoginActivity.Companion.getStartIntent(this, notification!!, enrollment!!)
        }
        startActivity(intent)
    }

    override fun onFcmTokenObtained(fcmToken: String?) {
        this.fcmToken = fcmToken
        updateUI()
    }

    override fun onFcmFailure(exception: Throwable?) {
        Log.e(TAG, "Error obtaining FCM token", exception)
        AlertDialog.Builder(this)
                .setTitle(R.string.alert_title_error)
                .setMessage(getString(R.string.alert_message_fcm_error))
                .setPositiveButton(android.R.string.ok) { dialog, which -> dialog.dismiss() }
                .setOnDismissListener { finish() }
                .create()
                .show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGuardianNotificationReceived(event: GuardianNotificationReceivedEvent) {
        onPushNotificationReceived(event.data)
    }

    companion object {
        private val TAG = MainActivity::class.java.name
        private const val ENROLL_REQUEST = 123
        fun getStartIntent(
                context: Context,
                notification: ParcelableNotification
        ): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(Constants.NOTIFICATION, notification)
            return intent
        }
    }
}
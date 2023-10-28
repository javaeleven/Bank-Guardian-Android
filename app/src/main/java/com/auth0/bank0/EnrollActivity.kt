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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.auth0.android.guardian.sdk.CurrentDevice
import com.auth0.android.guardian.sdk.Enrollment
import com.auth0.android.guardian.sdk.Guardian
import com.auth0.android.guardian.sdk.networking.Callback
import com.auth0.bank0.scanner.CaptureView
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException

class EnrollActivity : AppCompatActivity(), CaptureView.Listener {
    private var guardian: Guardian? = null
    private var deviceName: String? = null
    private var fcmToken: String? = null
    private var permissionLayout: View? = null
    private var scannerLayout: View? = null
    private var scanner: CaptureView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enroll)
        setupGuardian()
        setupUI()
        checkCameraPermission()
    }

    override fun onStart() {
        super.onStart()
        scanner!!.start(this)
    }

    override fun onResume() {
        super.onResume()
        resumeScanning()
    }

    override fun onPause() {
        super.onPause()
        scanner!!.pause()
    }

    override fun onStop() {
        super.onStop()
        scanner!!.stop()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA) {
            // Received permission result for camera permission.
            Log.i(TAG, "Received response for Camera permission request.")

            // Check if the only required permission has been granted
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted, preview can be displayed
                Log.i(TAG, "CAMERA permission has now been granted. Showing preview.")
                // Set up QR code scanning
                showScanView()
            } else {
                Log.i(TAG, "CAMERA permission was NOT granted.")
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onCodeScanned(enrollmentData: String?) {
        try {
            val keyPair = generateKeyPair()
            val device = CurrentDevice(this, fcmToken, deviceName)
            guardian!!.enroll(enrollmentData!!, device, keyPair!!)
                    .start(DialogCallback(this,
                            R.string.progress_title_please_wait,
                            R.string.progress_message_enroll,
                            object : Callback<Enrollment> {
                                override fun onSuccess(enrollment: Enrollment) {
                                    Log.d(TAG, "enroll success")
                                    onEnrollSuccess(enrollment)
                                }

                                override fun onFailure(exception: Throwable) {
                                    resumeScanning()
                                }
                            }
                    ))
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "enroll throw an exception", exception)
            onEnrollFailure(exception)
        }
    }

    private fun setupGuardian() {
        val intent = intent
        deviceName = intent.getStringExtra(DEVICE_NAME)
        fcmToken = intent.getStringExtra(FCM_TOKEN)
        check(!(deviceName == null || fcmToken == null)) { "Missing deviceName or fcmToken" }
        guardian = Guardian.Builder()
                .url(Uri.parse(getString(R.string.guardian_url)))
                .enableLogging()
                .build()
    }

    private fun setupUI() {
        permissionLayout = findViewById(R.id.permissionLayout)
        scannerLayout = findViewById(R.id.scannerLayout)
        scanner = findViewById<View>(R.id.scanner) as CaptureView
        val requestPermissionButton = (findViewById<View>(R.id.requestPermissionButton) as Button)
        requestPermissionButton.setOnClickListener { requestCameraPermission() }
    }

    private fun checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
        ) {
            // Camera permission has not been granted
            showCameraPermissionUnavailable()
            tryRequestCameraPermissionDirectly()
        } else {
            // Camera permissions is already available, show the camera preview.
            Log.i(TAG, "CAMERA permission has already been granted. Displaying camera preview.")

            // Set up QR code scanning
            showScanView()
        }
    }

    private fun tryRequestCameraPermissionDirectly() {
        Log.i(TAG, "CAMERA permission has NOT been granted. Requesting permission.")
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.CAMERA
                )
        ) {
            // The user has previously denied the permission
        } else {
            // Camera permission has not been granted yet. Request it directly.
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA
        )
    }

    private fun showCameraPermissionUnavailable() {
        scannerLayout!!.visibility = View.GONE
        permissionLayout!!.visibility = View.VISIBLE
    }

    private fun showScanView() {
        permissionLayout!!.visibility = View.GONE
        scannerLayout!!.visibility = View.VISIBLE
    }

    private fun resumeScanning() {
        scanner!!.resume()
    }

    private fun onEnrollSuccess(enrollment: Enrollment) {
        val data = Intent()
        val parcelableEnrollment = ParcelableEnrollment(enrollment)
        data.putExtra(Constants.ENROLLMENT, parcelableEnrollment)
        setResult(RESULT_OK, data)
        finish()
    }

    private fun onEnrollFailure(exception: Throwable) {
        runOnUiThread {
            AlertDialog.Builder(this@EnrollActivity)
                    .setTitle(R.string.alert_title_error)
                    .setMessage(exception.message)
                    .setPositiveButton(
                            android.R.string.ok
                    ) { dialog, which -> dialog.dismiss() }
                    .setOnDismissListener { resumeScanning() }
                    .create()
                    .show()
        }
    }

    private fun generateKeyPair(): KeyPair? {
        try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048) // at least 2048 bits!
            return keyPairGenerator.generateKeyPair()
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Error generating keys", e)
        }
        return null
    }

    companion object {
        private val TAG = EnrollActivity::class.java.name
        private const val DEVICE_NAME = "com.auth0.bank0.EnrollActivity.DEVICE_NAME"
        private const val FCM_TOKEN = "com.auth0.bank0.EnrollActivity.FCM_TOKEN"
        private const val REQUEST_CAMERA = 55
        fun getStartIntent(
                context: Context,
                deviceName: String,
                fcmToken: String
        ): Intent {
            val intent = Intent(context, EnrollActivity::class.java)
            intent.putExtra(DEVICE_NAME, deviceName)
            intent.putExtra(FCM_TOKEN, fcmToken)
            return intent
        }
    }
}
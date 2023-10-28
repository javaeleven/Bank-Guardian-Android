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
package com.auth0.bank0.scanner

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Camera
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.widget.LinearLayout
import com.auth0.bank0.scanner.CaptureView
import com.auth0.bank0.scanner.camera.CameraSource
import com.auth0.bank0.scanner.camera.CameraSourceCropPreview
import com.auth0.bank0.scanner.camera.GraphicOverlay
import com.auth0.bank0.scanner.utils.Barcode
import com.auth0.bank0.scanner.utils.BarcodeDetector
import com.auth0.bank0.scanner.utils.MultiProcessor
import com.google.zxing.BarcodeFormat
import java.io.IOException
import java.util.*

class CaptureView : LinearLayout, BarcodeTrackerListener {
    private var preview: CameraSourceCropPreview? = null
    private lateinit var graphicOverlay: GraphicOverlay<BarcodeGraphic>
    private var cameraSource: CameraSource? = null
    private var listener: Listener? = null
    private var mainThreadHandler: Handler? = null

    constructor(context: Context) : super(context) {
        init(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context,
            attrs,
            defStyleAttr
    ) {
        init(context, attrs, defStyleAttr)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyle: Int) {
        mainThreadHandler = Handler(Looper.getMainLooper())
        preview = CameraSourceCropPreview(context, attrs)
        graphicOverlay = GraphicOverlay(context, attrs)
        val matchParentLayoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        )
        preview!!.addView(graphicOverlay, matchParentLayoutParams)
        addView(preview, matchParentLayoutParams)
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private fun createCameraSource() {
        val context = context.applicationContext

        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        val decodeFormats: MutableCollection<BarcodeFormat> = EnumSet.noneOf(
                BarcodeFormat::class.java
        )
        decodeFormats.add(BarcodeFormat.QR_CODE)
        val barcodeDetector = BarcodeDetector.Builder()
                .setBarcodeFormats(decodeFormats)
                .setPercentage(70)
                .build()
        val barcodeFactory = BarcodeTrackerFactory(graphicOverlay, this)
        barcodeDetector.setProcessor(
                MultiProcessor.Builder(barcodeFactory).build()
        )

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        val builder = CameraSource.Builder(context, barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1024, 600)
                .setRequestedFps(15.0f)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
        cameraSource = builder
                .setFlashMode(null)
                .build()
    }

    fun start(listener: Listener?) {
        Log.d(TAG, "start")
        this.listener = listener
        createCameraSource()
    }

    /**
     * Restarts the camera.
     */
    fun resume() {
        Log.d(TAG, "resume")
        startCameraSource()
    }

    /**
     * Stops the camera.
     */
    fun pause() {
        Log.d(TAG, "pause")
        if (preview != null) {
            preview!!.stop()
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    fun stop() {
        Log.d(TAG, "stop")
        if (preview != null) {
            preview!!.release()
        }
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    @Throws(SecurityException::class)
    private fun startCameraSource() {
        if (cameraSource != null) {
            try {
                preview!!.start(cameraSource, graphicOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                cameraSource!!.release()
                cameraSource = null
            }
        }
    }

    override fun onBarcodeDetected(item: Barcode) {
        Log.d(TAG, "detected barcode with data: " + item.text)
        mainThreadHandler!!.post {
            pause()
            listener!!.onCodeScanned(item.text)
        }
    }

    interface Listener {
        fun onCodeScanned(data: String?)
    }

    companion object {
        private val TAG = CaptureView::class.java.name
    }
}
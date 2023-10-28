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

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.animation.DecelerateInterpolator
import com.auth0.bank0.scanner.BarcodeGraphic
import com.auth0.bank0.scanner.camera.GraphicOverlay
import com.auth0.bank0.scanner.camera.GraphicOverlay.Graphic
import com.auth0.bank0.scanner.utils.Barcode

internal class BarcodeGraphic(overlay: GraphicOverlay<*>?) : Graphic(overlay) {
    var id = 0

    @Volatile
    private var barcode: Barcode? = null
    private val paint: Paint
    private val animatorSet: AnimatorSet
    private val animatorList: ArrayList<Animator>
    private val rippleViewList = ArrayList<RippleView>()
    private var shouldStartAnimation = true

    init {
        val rippleDelay = DEFAULT_DURATION_TIME / DEFAULT_RIPPLE_COUNT
        paint = Paint()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        animatorSet = AnimatorSet()
        animatorSet.interpolator = DecelerateInterpolator()
        animatorList = ArrayList()
        for (i in 0 until DEFAULT_RIPPLE_COUNT) {
            val rippleView = RippleView()
            rippleViewList.add(rippleView)
            val radiusAnimator = ObjectAnimator.ofFloat(rippleView, "RadiusMultiplier", 0.0f, 5f)
            radiusAnimator.startDelay = (i * rippleDelay).toLong()
            radiusAnimator.duration = DEFAULT_DURATION_TIME.toLong()
            animatorList.add(radiusAnimator)
            val alphaAnimator = ObjectAnimator.ofFloat(rippleView, "Alpha", 0.4f, 0f)
            alphaAnimator.startDelay = (i * rippleDelay).toLong()
            alphaAnimator.duration = DEFAULT_DURATION_TIME.toLong()
            animatorList.add(alphaAnimator)
        }
        animatorSet.playTogether(animatorList)
    }

    /**
     * Updates the barcode instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    fun updateItem(barcode: Barcode?) {
        this.barcode = barcode
        postInvalidate()
    }

    /**
     * Draws the barcode annotations for position, size, and raw value on the supplied canvas.
     */
    override fun draw(canvas: Canvas) {
        val barcode = barcode ?: return
        if (shouldStartAnimation) {
            shouldStartAnimation = false

            // Get "on screen" bounding box around the barcode
            val rect = barcode.boundingBox
            rect!!.left = translateX(rect.left)
            rect.top = translateY(rect.top)
            rect.right = translateX(rect.right)
            rect.bottom = translateY(rect.bottom)

            // Set location and size
            for (view in rippleViewList) {
                view.setLocation(
                        rect.centerX(),
                        rect.centerY(),
                        Math.min(rect.width(), rect.height()) / 2f
                )
            }

            // Start animations
            animatorSet.start()
        }
        for (view in rippleViewList) {
            view.draw(canvas)
        }
        postInvalidate()
    }

    private inner class RippleView {
        private var radiusMultiplier = 0f
        private var alpha = 0f
        private var centerX = 0f
        private var centerY = 0f
        private var radius = 0f
        fun setLocation(centerX: Float, centerY: Float, radius: Float) {
            this.centerX = centerX
            this.centerY = centerY
            this.radius = radius
        }

        fun setRadiusMultiplier(radiusMultiplier: Float) {
            this.radiusMultiplier = radiusMultiplier
        }

        fun setAlpha(alpha: Float) {
            this.alpha = alpha
        }

        fun draw(canvas: Canvas) {
            paint.color = Color.argb((255 * alpha).toInt(), 255, 255, 255)
            canvas.drawCircle(centerX, centerY, radiusMultiplier * radius, paint)
        }
    }

    companion object {
        private val TAG = BarcodeGraphic::class.java.name
        private const val DEFAULT_RIPPLE_COUNT = 1
        private const val DEFAULT_DURATION_TIME = 1000
    }
}
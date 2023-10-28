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
package com.auth0.bank0.views

import android.content.Context
import android.graphics.Color
import android.os.CountDownTimer
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.auth0.android.guardian.sdk.Enrollment
import com.auth0.android.guardian.sdk.otp.TOTP
import com.auth0.android.guardian.sdk.otp.utils.Base32
import com.auth0.bank0.R

class TOTPCodeView : AppCompatTextView {
    private var enrollment: Enrollment? = null
    private var timer: CountDownTimer? = null
    private var codeGenerator: TOTP? = null
    private var defaultTextColor = Color.BLACK
    private var aboutToExpireColor = Color.RED
    private var expiringTime = 5000

    constructor(context: Context?) : super(context!!) {
        init(null, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
            context!!, attrs
    ) {
        init(attrs, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
            context!!, attrs, defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        defaultTextColor = currentTextColor

        // Load attributes
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.TOTPCodeView, defStyle, 0
        )
        aboutToExpireColor = a.getColor(
                R.styleable.TOTPCodeView_aboutToExpireColor, aboutToExpireColor
        )
        expiringTime = a.getInteger(
                R.styleable.TOTPCodeView_aboutToExpireTimeMs, expiringTime
        )
        a.recycle()
    }

    fun getAboutToExpireColor(): Int {
        return aboutToExpireColor
    }

    fun setAboutToExpireColor(aboutToExpireColor: Int) {
        this.aboutToExpireColor = aboutToExpireColor
        postInvalidate()
    }

    fun setEnrollment(enrollment: Enrollment) {
        try {
            codeGenerator = TOTP(
                    enrollment.algorithm,
                    Base32.decode(enrollment.secret),
                    enrollment.digits!!,
                    enrollment.period!!
            )
        } catch (e: Base32.DecodingException) {
            throw IllegalArgumentException("Unable to generate OTP: could not decode secret", e)
        }
        this.enrollment = enrollment
        startUpdateTimer()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (enrollment != null) {
            startUpdateTimer()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    private fun startUpdateTimer() {
        val periodMs = (enrollment!!.period!! * 1000).toLong()
        val currentTime = System.currentTimeMillis()
        // always start with a little offset (0.1 sec) to  be sure that the next code is the new one
        val nextChange = periodMs - currentTime % periodMs + 100
        if (nextChange > expiringTime) {
            setTextColor(defaultTextColor)
        } else {
            setTextColor(aboutToExpireColor)
        }
        updateCode()
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
        timer = object : CountDownTimer(nextChange, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (System.currentTimeMillis() % periodMs >= periodMs - expiringTime) {
                    setTextColor(aboutToExpireColor)
                }
            }

            override fun onFinish() {
                timer = null
                startUpdateTimer()
            }
        }
        timer?.start()
    }

    private fun updateCode() {
        setTextColor(defaultTextColor)
        val codeStr = codeGenerator!!.generate()
        text = codeStr
    }
}
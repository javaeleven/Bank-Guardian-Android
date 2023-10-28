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
import android.os.Handler
import android.util.Log
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.auth0.android.guardian.sdk.networking.Callback

class DialogCallback<T> internal constructor(
        private val context: Context,
        @StringRes titleResId: Int,
        @StringRes messageResId: Int,
        private val callback: Callback<T>
) : Callback<T> {
    private val progressDialog: AlertDialog

    init {
        val progressBar = ProgressBar(context)
        progressBar.isIndeterminate = true
        progressBar.id = R.id.progress_bar // need an id to align "right of" this
        val layout = RelativeLayout(context)
        val progressBarLayoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        val margin = context.resources.getDimensionPixelSize(R.dimen.activity_vertical_margin)
        progressBarLayoutParams.setMargins(margin, margin, margin, margin)
        layout.addView(progressBar, progressBarLayoutParams)
        val textView = TextView(context)
        textView.setText(messageResId)
        val textViewLayoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        textViewLayoutParams.addRule(RelativeLayout.RIGHT_OF, progressBar.id)
        textViewLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
        layout.addView(textView, textViewLayoutParams)
        progressDialog = AlertDialog.Builder(context)
                .setTitle(titleResId)
                .setView(layout)
                .setCancelable(false)
                .create()
        progressDialog.show()
    }

    override fun onSuccess(response: T) {
        progressDialog.dismiss()
        callback.onSuccess(response)
    }

    override fun onFailure(exception: Throwable) {
        Log.e(TAG, "Guardian error", exception)
        progressDialog.dismiss()
        Handler(context.mainLooper)
                .post {
                    AlertDialog.Builder(context)
                            .setTitle(R.string.alert_title_error)
                            .setMessage(exception.toString())
                            .setPositiveButton(android.R.string.ok) { dialog, which -> dialog.dismiss() }
                            .setOnDismissListener { callback.onFailure(exception) }
                            .create()
                            .show()
                }
    }

    companion object {
        private val TAG = DialogCallback::class.java.name
    }
}
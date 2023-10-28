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
package com.auth0.bank0.fcm

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.auth0.android.guardian.sdk.Guardian
import com.auth0.bank0.BuildConfig
import com.auth0.bank0.MainActivity
import com.auth0.bank0.R
import com.auth0.bank0.events.GuardianNotificationReceivedEvent
import com.auth0.bank0.fcm.FcmListenerService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.greenrobot.eventbus.EventBus

class FcmListenerService : FirebaseMessagingService() {
    /**
     * Called when message is received.
     *
     * @param message The message instance
     */
    override fun onMessageReceived(message: RemoteMessage) {
        if (BuildConfig.DEBUG) {
            Log.d(
                    TAG,
                    String.format(
                            "Received FCM message from: %s with data: %s",
                            message.from,
                            message.data
                    )
            )
        }
        try {
            Log.i("RECEIVED NOTIFICATION", message.data.toString())
            val notification = Guardian.parseNotification(message.data)
            val eventBus = EventBus.getDefault()
            if (eventBus.hasSubscriberForEvent(GuardianNotificationReceivedEvent::class.java)) {
                eventBus.post(GuardianNotificationReceivedEvent(notification))
            } else {
                val intent: Intent = MainActivity.Companion.getStartIntent(this, notification!!)
                val pendingIntent = PendingIntent
                        .getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                val builder = NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setColor(Color.BLACK)
                        .setContentTitle(getString(R.string.guardian_notification_title))
                        .setContentText(getString(R.string.guardian_notification_text))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                NotificationManagerCompat.from(this)
                        .notify(notification.transactionToken.hashCode(), builder.build())
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Received a notification that is not a Guardian Notification", e)
        }
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    override fun onNewToken(token: String) {
        // Use updated token and notify our app's server of any changes (if applicable).
        Log.w(TAG, "Should refresh token!")
    }

    companion object {
        private val TAG = FcmListenerService::class.java.name
    }
}
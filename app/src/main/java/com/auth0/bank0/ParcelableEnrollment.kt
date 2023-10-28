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

import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import com.auth0.android.guardian.sdk.Enrollment
import com.auth0.bank0.ParcelableEnrollment
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec

class ParcelableEnrollment : Enrollment, Parcelable {
    @SerializedName("id")
    private val id: String

    @SerializedName("userId")
    private val userId: String

    @SerializedName("period")
    private val period: Int?

    @SerializedName("digits")
    private val digits: Int?

    @SerializedName("algorithm")
    private val algorithm: String?

    @SerializedName("secret")
    private val secret: String?

    @SerializedName("deviceIdentifier")
    private val deviceIdentifier: String

    @SerializedName("deviceName")
    private val deviceName: String

    @SerializedName("deviceGCMToken")
    private val deviceGCMToken: String

    @SerializedName("deviceToken")
    private val deviceToken: String

    @SerializedName("privateKey")
    private val privateKey: String?

    constructor(enrollment: Enrollment) {
        userId = enrollment.userId
        period = enrollment.period
        digits = enrollment.digits
        algorithm = enrollment.algorithm
        secret = enrollment.secret
        id = enrollment.id
        deviceIdentifier = enrollment.deviceIdentifier
        deviceName = enrollment.deviceName
        deviceGCMToken = enrollment.notificationToken
        deviceToken = enrollment.deviceToken
        privateKey = Base64.encodeToString(enrollment.signingKey.encoded, Base64.DEFAULT)
    }

    override fun getId(): String {
        return id
    }

    override fun getUserId(): String {
        return userId
    }

    override fun getPeriod(): Int? {
        return period
    }

    override fun getDigits(): Int? {
        return digits
    }

    override fun getAlgorithm(): String? {
        return algorithm
    }

    override fun getSecret(): String? {
        return secret
    }

    override fun getDeviceIdentifier(): String {
        return deviceIdentifier
    }

    override fun getDeviceName(): String {
        return deviceName
    }

    override fun getNotificationToken(): String {
        return deviceGCMToken
    }

    override fun getDeviceToken(): String {
        return deviceToken
    }

    override fun getSigningKey(): PrivateKey {
        return try {
            val key = Base64.decode(privateKey, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance("RSA")
            val keySpec = PKCS8EncodedKeySpec(key)
            keyFactory.generatePrivate(keySpec)
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException("Invalid private key!")
        } catch (e: InvalidKeySpecException) {
            throw IllegalStateException("Invalid private key!")
        }
    }

    // PARCELABLE
    protected constructor(`in`: Parcel) {
        id = `in`.readString()!!
        userId = `in`.readString()!!
        period = `in`.readInt()
        digits = `in`.readInt()
        algorithm = `in`.readString()
        secret = `in`.readString()
        deviceIdentifier = `in`.readString()!!
        deviceName = `in`.readString()!!
        deviceGCMToken = `in`.readString()!!
        deviceToken = `in`.readString()!!
        privateKey = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeString(userId)
        dest.writeInt(period!!)
        dest.writeInt(digits!!)
        dest.writeString(algorithm)
        dest.writeString(secret)
        dest.writeString(deviceIdentifier)
        dest.writeString(deviceName)
        dest.writeString(deviceGCMToken)
        dest.writeString(deviceToken)
        dest.writeString(privateKey)
    }

    // SIMPLE SERIALIZATION
    fun toJSON(): String {
        return JSON.toJson(this)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ParcelableEnrollment> =
                object : Parcelable.Creator<ParcelableEnrollment> {
                    override fun createFromParcel(`in`: Parcel): ParcelableEnrollment? {
                        return ParcelableEnrollment(`in`)
                    }

                    override fun newArray(size: Int): Array<ParcelableEnrollment?> {
                        return arrayOfNulls(size)
                    }
                }

        fun fromJSON(json: String?): ParcelableEnrollment {
            return JSON.fromJson(json, ParcelableEnrollment::class.java)
        }

        private val JSON = GsonBuilder().create()
    }
}
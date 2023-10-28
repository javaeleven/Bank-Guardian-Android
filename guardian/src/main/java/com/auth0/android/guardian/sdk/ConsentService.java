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

package com.auth0.android.guardian.sdk;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConsentService {

    private final ConsentAPIClient client;
    private final String audience;
    private final String authorizingClientId;

    ConsentService(
            @NonNull ConsentAPIClient client,
            @NonNull String audience,
            @NonNull String authorizingClientId) {
        this.client = client;
        this.audience = audience;
        this.authorizingClientId = authorizingClientId;
    }

    @NonNull
    public GuardianAPIRequest<Void> authorize(@NonNull Notification notification,
                                              @NonNull Enrollment enrollment,
                                              List<Map<String, Object>> authorizationDetails) {
        return client
                .authorize(
                        Objects.requireNonNull(notification.getTransactionLinkingId()),
                        enrollment.getUserId(),
                        this.audience,
                        this.authorizingClientId,
                        notification.getEnrollmentId(),
                        enrollment.getSigningKey(),
                        authorizationDetails);
    }


    @NonNull
    public GuardianAPIRequest<ConsentEntity> getConsent(@NonNull Notification notification,
                                                        @NonNull Enrollment enrollment) {


        return client
                .getConsent(
                        Objects.requireNonNull(notification.getTransactionLinkingId()),
                        enrollment.getUserId(),
                        this.audience,
                        this.authorizingClientId,
                        notification.getEnrollmentId(),
                        enrollment.getSigningKey()
                );
    }

    @NonNull
    public GuardianAPIRequest<Void> reject(@NonNull Notification notification,
                                           @NonNull Enrollment enrollment) {
        return client
                .reject(
                        Objects.requireNonNull(notification.getTransactionLinkingId()),
                        enrollment.getUserId(),
                        this.audience,
                        this.authorizingClientId,
                        notification.getEnrollmentId(),
                        enrollment.getSigningKey()
                );
    }

    /**
     * A {@link ConsentService} Builder
     */
    public static class Builder {

        private Uri url;
        private String authorizingClientId;
        private boolean loggingEnabled = false;

        /**
         * Set the URL of the tenant.
         *
         * @param url the url
         * @return itself
         * @throws IllegalArgumentException when an url or domain was already set
         */
        public Builder url(@NonNull Uri url) {
            if (this.url != null) {
                throw new IllegalArgumentException("An url/domain was already set");
            }
            this.url = url;
            return this;
        }

        /**
         * Enables the logging of all HTTP requests to the console.
         * <p>
         * Should only be used during development, on debug builds
         *
         * @return itself
         */
        public Builder enableLogging() {
            this.loggingEnabled = true;
            return this;
        }

        public Builder authorizingClientId(@NonNull String authorizingClientId) {
            this.authorizingClientId = authorizingClientId;
            return this;
        }

        /**
         * Builds and returns the ConsentService instance
         *
         * @return the created instance
         * @throws IllegalStateException when the builder was not configured correctly
         */
        public ConsentService build() {
            if (url == null) {
                throw new IllegalStateException("You must set either a domain or an url");
            }

            ConsentAPIClient.Builder apiClientBuilder = new ConsentAPIClient.Builder()
                    .url(url);

            if (loggingEnabled) {
                apiClientBuilder.enableLogging();
            }

            String audience = url.toString() + "/consents";
            return new ConsentService(apiClientBuilder.build(), audience, authorizingClientId);
        }
    }
}

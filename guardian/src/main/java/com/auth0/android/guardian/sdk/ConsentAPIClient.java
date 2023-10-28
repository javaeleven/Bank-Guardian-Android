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
import android.os.Build;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.auth0.android.guardian.sdk.networking.RequestFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class ConsentAPIClient {

    private static final int ACCESS_APPROVAL_JWT_EXP_SECS = 30;

    private final RequestFactory requestFactory;
    private final HttpUrl baseUrl;

    ConsentAPIClient(RequestFactory requestFactory, HttpUrl baseUrl) {
        this.requestFactory = requestFactory;
        this.baseUrl = baseUrl;
    }

    private static String base64UrlSafeEncode(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    @NonNull
    public GuardianAPIRequest<ConsentEntity> getConsent(@NonNull String transactionLinkingId,
                                                        @NonNull String subject,
                                                        @NonNull String audience,
                                                        @NonNull String clientId,
                                                        @NonNull String authenticatorId,
                                                        @NonNull PrivateKey privateKey
    ) {
        Type type = new TypeToken<ConsentEntity>() {
        }.getType();

        final HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("consents")
                .addPathSegment(transactionLinkingId)
                .build();

        final String authzHeader = createAuthorizationHeader(
                privateKey, audience,
                subject, clientId, authenticatorId);
        return requestFactory
                .<ConsentEntity>newRequest("GET", url, type)
                .setBearer(authzHeader);
    }

    @NonNull
    public GuardianAPIRequest<Void> authorize(@NonNull String transactionLinkingId,
                                              @NonNull String subject,
                                              @NonNull String audience,
                                              @NonNull String clientId,
                                              @NonNull String authenticatorId,
                                              @NonNull PrivateKey privateKey,
                                              @NonNull List<Map<String, Object>> authorizationDetails
    ) {
        Type type = new TypeToken<Void>() {
        }.getType();

        final HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("consents")
                .addPathSegment(transactionLinkingId)
                .build();

        final String authzHeader = createAuthorizationHeader(
                privateKey, audience,
                subject, clientId, authenticatorId);
        return requestFactory
                .<Void>newRequest("PATCH", url, type)
                .setBearer(authzHeader)
                .setParameter("status", "Authorized")
                .setParameter("authorization_details", authorizationDetails);
    }

    @NonNull
    public GuardianAPIRequest<Void> reject(@NonNull String transactionLinkingId,
                                           @NonNull String subject,
                                           @NonNull String audience,
                                           @NonNull String clientId,
                                           @NonNull String authenticatorId,
                                           @NonNull PrivateKey privateKey
    ) {
        Type type = new TypeToken<Void>() {
        }.getType();

        final HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("consents")
                .addPathSegment(transactionLinkingId)
                .build();

        final String authzHeader = createAuthorizationHeader(
                privateKey, audience,
                subject, clientId, authenticatorId);
        return requestFactory
                .<Void>newRequest("PATCH", url, type)
                .setBearer(authzHeader)
                .setParameter("status", "Rejected");
    }

    private String createAuthorizationHeader(@NonNull PrivateKey privateKey,
                                             @NonNull String audience,
                                             @NonNull String subject,
                                             @NotNull String clientId,
                                             @NonNull String authenticatorId) {

        long currentTime = new Date().getTime() / 1000L;
        Map<String, Object> claims = new HashMap<>();
        claims.put("iat", currentTime);
        claims.put("exp", currentTime + ACCESS_APPROVAL_JWT_EXP_SECS);
        claims.put("aud", audience);
        claims.put("iss", clientId);
        claims.put("sub", subject);
        claims.put("authenticator_id", authenticatorId);

        return signJWT(privateKey, claims);
    }

    private String signJWT(@NonNull PrivateKey privateKey, @NonNull Map<String, Object> claims) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("alg", "PS256");
            headers.put("typ", "guardian-authz-req+jwt");
            Gson gson = new GsonBuilder().create();
            String headerAndPayload = base64UrlSafeEncode(gson.toJson(headers).getBytes())
                    + "." + base64UrlSafeEncode(gson.toJson(claims).getBytes());
            final byte[] messageBytes = headerAndPayload.getBytes();
            final Signature signer = Signature.getInstance("SHA256withRSA/PSS");
            signer.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
            signer.initSign(privateKey);
            signer.update(messageBytes);
            byte[] signature = signer.sign();
            return headerAndPayload + "." + base64UrlSafeEncode(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new GuardianException("Unable to generate the signed JWT", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A {@link ConsentAPIClient} Builder
     */
    public static class Builder {

        private HttpUrl url;
        private boolean loggingEnabled = false;

        /**
         * Set the URL of the Guardian server.
         * For example {@code https://tenant.guardian.auth0.com/}
         *
         * @param url the url
         * @return itself
         * @throws IllegalArgumentException when an url or domain was already set
         */
        public Builder url(@NonNull Uri url) {
            if (this.url != null) {
                throw new IllegalArgumentException("An url/domain was already set");
            }
            this.url = HttpUrl.parse(url.toString());
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

        /**
         * Builds and returns the ConsentAPIClient instance
         *
         * @return the created instance
         * @throws IllegalStateException when the builder was not configured correctly
         */
        public ConsentAPIClient build() {
            if (url == null) {
                throw new IllegalStateException("You must set either a domain or an url");
            }

            final OkHttpClient.Builder builder = new OkHttpClient.Builder();

            final String clientInfo = Base64.encodeToString(
                    String.format("{\"name\":\"Guardian.Android\",\"version\":\"%s\"}",
                            BuildConfig.VERSION_NAME).getBytes(),
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);

            builder.addInterceptor(chain -> {
                okhttp3.Request originalRequest = chain.request();
                okhttp3.Request requestWithUserAgent = originalRequest.newBuilder()
                        .header("Accept-Language",
                                Locale.getDefault().toString())
                        .header("User-Agent",
                                String.format("GuardianSDK/%s Android %s",
                                        BuildConfig.VERSION_NAME,
                                        Build.VERSION.RELEASE))
                        .header("Auth0-Client", clientInfo)
                        .build();
                return chain.proceed(requestWithUserAgent);
            });

            if (loggingEnabled) {
                final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY);
                builder.addInterceptor(loggingInterceptor);
            }

            try {
                this.setTrustAllCerts(builder);
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            this.redirectVivaldiRequests(builder);

            OkHttpClient client = builder.build();

            Gson gson = new GsonBuilder().create();

            RequestFactory requestFactory = new RequestFactory(gson, client);

            return new ConsentAPIClient(requestFactory, url);
        }

        private void setTrustAllCerts(OkHttpClient.Builder builder) throws NoSuchAlgorithmException, KeyManagementException {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        }

        private void redirectVivaldiRequests(OkHttpClient.Builder builder) {
            builder.dns(hostname -> {
                if (hostname.endsWith(".local.dev.auth0.com")) {
                    List<InetAddress> addr = new ArrayList<>();
                    addr.add(InetAddress.getByAddress(hostname, new byte[]{10, 0, 2, 2}));
                    addr.add(InetAddress.getByAddress(hostname, new byte[]{(byte) 192, (byte) 168, 86, 23}));
                    return addr;
                }
                return Arrays.asList(InetAddress.getAllByName(hostname));
            });
        }
    }
}

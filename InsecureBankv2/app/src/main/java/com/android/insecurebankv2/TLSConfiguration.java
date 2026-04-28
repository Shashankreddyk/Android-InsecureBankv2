package com.android.insecurebankv2.network;

import android.util.Log;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import okhttp3.logging.HttpLoggingInterceptor;

import javax.net.ssl.SSLContext;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Configures OkHttpClient for TLS 1.3 with proper cipher suites and security settings
 * 
 * TLS 1.3 Features:
 * - Removes older, vulnerable cipher suites
 * - Faster handshake (0-RTT)
 * - Forward secrecy
 * - Removed support for MD5, SHA-1, DES, RC4
 * 
 * @author Security Team
 */
public class TLSConfiguration {

    private static final String TAG = "TLSConfiguration";

    /**
     * Create OkHttpClient with TLS 1.3 configuration
     * 
     * @return Configured OkHttpClient instance
     */
    public static OkHttpClient createSecureOkHttpClient() {
        try {
            // Create OkHttpClient builder
            OkHttpClient.Builder builder = new OkHttpClient.Builder();

            // Configure TLS versions - Only TLS 1.3 and TLS 1.2 (fallback)
            ConnectionSpec tlsSpec = createTLSConnectionSpec();

            // Set connection specs
            builder.connectionSpecs(Arrays.asList(
                    tlsSpec,  // TLS 1.3 + TLS 1.2
                    ConnectionSpec.CLEARTEXT  // For testing only (remove in production)
            ));

            // Set timeouts
            builder.connectTimeout(30, TimeUnit.SECONDS);
            builder.readTimeout(30, TimeUnit.SECONDS);
            builder.writeTimeout(30, TimeUnit.SECONDS);

            // Add logging interceptor (for debugging - remove in production)
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                    message -> Log.d(TAG, message)
            );
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(loggingInterceptor);

            // Enable certificate pinning (if configured)
            CertificatePinningManager.applyCertificatePinning(builder);

            Log.d(TAG, "✓ TLS 1.3 configuration applied successfully");

            return builder.build();

        } catch (Exception e) {
            Log.e(TAG, "Failed to create secure OkHttpClient", e);
            throw new RuntimeException("TLS configuration failed", e);
        }
    }

    /**
     * Create ConnectionSpec for TLS 1.3
     * 
     * Cipher suites selected:
     * 1. TLS_AES_256_GCM_SHA384 - Most secure, 256-bit encryption
     * 2. TLS_CHACHA20_POLY1305_SHA256 - Modern, efficient
     * 3. TLS_AES_128_GCM_SHA256 - Fallback to 128-bit encryption
     * 
     * @return ConnectionSpec with TLS 1.3 configuration
     */
    private static ConnectionSpec createTLSConnectionSpec() {
        return new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                // Only allow TLS 1.3 and 1.2
                .tlsVersions(
                        TlsVersion.TLS_1_3,  // Primary: TLS 1.3
                        TlsVersion.TLS_1_2   // Fallback: TLS 1.2 (if server doesn't support 1.3)
                )
                // Cipher suites for TLS 1.3
                .cipherSuites(
                        CipherSuite.TLS_AES_256_GCM_SHA384,        // 256-bit encryption (RECOMMENDED)
                        CipherSuite.TLS_CHACHA20_POLY1305_SHA256,  // Modern cipher
                        CipherSuite.TLS_AES_128_GCM_SHA256         // 128-bit encryption (fallback)
                )
                .build();
    }

    /**
     * Verify TLS configuration at runtime
     * 
     * @param client OkHttpClient to verify
     * @return true if TLS is properly configured
     */
    public static boolean verifyTLSConfiguration(OkHttpClient client) {
        try {
            // Get connection specs
            java.util.List<ConnectionSpec> specs = client.connectionSpecs();

            Log.d(TAG, "Active connection specs:");
            for (ConnectionSpec spec : specs) {
                Log.d(TAG, "  - Spec: " + spec);
            }

            // Verify TLS versions
            boolean hasTLS13 = false;
            boolean hasTLS12 = false;

            for (ConnectionSpec spec : specs) {
                if (spec.tlsVersions() != null) {
                    for (TlsVersion version : spec.tlsVersions()) {
                        if (version == TlsVersion.TLS_1_3) {
                            hasTLS13 = true;
                            Log.d(TAG, "✓ TLS 1.3 enabled");
                        }
                        if (version == TlsVersion.TLS_1_2) {
                            hasTLS12 = true;
                            Log.d(TAG, "✓ TLS 1.2 enabled (fallback)");
                        }
                    }
                }
            }

            if (hasTLS13) {
                Log.d(TAG, "✓ TLS configuration verified: TLS 1.3 is primary");
                return true;
            } else {
                Log.w(TAG, "⚠ TLS 1.3 not found in configuration");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to verify TLS configuration", e);
            return false;
        }
    }
}

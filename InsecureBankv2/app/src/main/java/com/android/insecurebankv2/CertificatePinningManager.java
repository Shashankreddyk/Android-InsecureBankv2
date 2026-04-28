package com.android.insecurebankv2.network;

import android.util.Log;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;

/**
 * Manages SSL/TLS certificate pinning
 * 
 * Certificate pinning prevents MITM attacks by:
 * 1. Pinning the server's public key
 * 2. Verifying the pinned certificate on every connection
 * 3. Failing the connection if the certificate doesn't match
 * 
 * @author Security Team
 */
public class CertificatePinningManager {

    private static final String TAG = "CertificatePinning";

    /**
     * Apply certificate pinning to OkHttpClient builder
     * 
     * @param builder OkHttpClient.Builder to configure
     */
    public static void applyCertificatePinning(OkHttpClient.Builder builder) {
        try {
            CertificatePinner certPinner = new CertificatePinner.Builder()
                    // Pin for your production server
                    // Format: .addPin("domain.com", "sha256/...")
                    .addPin(
                            "your-server.com",
                            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="  // Primary pin
                    )
                    .addPin(
                            "your-server.com",
                            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="  // Backup pin
                    )

                    // Pin for development/testing server
                    .addPin(
                            "dev-server.com",
                            "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC="
                    )

                    // Fallback: Allow multiple domains
                    .addPin(
                            "*.your-domain.com",
                            "sha256/DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD="
                    )
                    .build();

            builder.certificatePinner(certPinner);

            Log.d(TAG, "✓ Certificate pinning applied successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to apply certificate pinning", e);
            throw new RuntimeException("Certificate pinning configuration failed", e);
        }
    }

    /**
     * Instructions to generate certificate pins from your server
     * 
     * Step 1: Get the server certificate
     *   $ openssl s_client -connect your-server.com:443 < /dev/null | \
     *     openssl x509 -outform DER | \
     *     openssl dgst -sha256 -binary | \
     *     openssl enc -base64
     * 
     * Step 2: Get the backup pin (from intermediate certificate)
     *   $ openssl s_client -connect your-server.com:443 -showcerts < /dev/null
     *   (Copy the intermediate certificate and repeat Step 1)
     * 
     * Step 3: Update the addPin() calls with the generated values
     */
}

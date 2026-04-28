package com.android.insecurebankv2.network;

import android.content.Context;
import android.util.Log;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Manages network configuration and Retrofit instances
 * Centralizes all network setup with TLS 1.3
 * 
 * @author Security Team
 */
public class NetworkManager {

    private static final String TAG = "NetworkManager";
    private static NetworkManager instance;
    private Retrofit retrofit;
    private OkHttpClient okHttpClient;
    private String baseURL;

    private NetworkManager(Context context, String baseURL) {
        this.baseURL = baseURL;
        initializeNetworkClient();
    }

    /**
     * Singleton pattern - get or create NetworkManager instance
     * 
     * @param context Application context
     * @param baseURL Server URL (e.g., "https://your-server.com:8443/")
     * @return NetworkManager instance
     */
    public static synchronized NetworkManager getInstance(Context context, String baseURL) {
        if (instance == null) {
            instance = new NetworkManager(context, baseURL);
        }
        return instance;
    }

    /**
     * Initialize OkHttpClient with TLS 1.3 configuration
     */
    private void initializeNetworkClient() {
        try {
            // Create OkHttpClient with TLS 1.3
            okHttpClient = TLSConfiguration.createSecureOkHttpClient();

            // Verify TLS configuration
            boolean isConfigured = TLSConfiguration.verifyTLSConfiguration(okHttpClient);
            if (!isConfigured) {
                Log.w(TAG, "⚠ TLS configuration verification failed");
            }

            Log.d(TAG, "✓ OkHttpClient initialized with TLS 1.3");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize network client", e);
            throw new RuntimeException("Network initialization failed", e);
        }
    }

    /**
     * Get or create Retrofit instance
     * Retrofit wraps OkHttpClient for REST API calls
     * 
     * @return Retrofit instance
     */
    public Retrofit getRetrofit() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseURL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            Log.d(TAG, "✓ Retrofit initialized with base URL: " + baseURL);
        }
        return retrofit;
    }

    /**
     * Get OkHttpClient directly for custom requests
     * 
     * @return OkHttpClient with TLS 1.3
     */
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    /**
     * Update base URL (useful for switching servers)
     * 
     * @param newBaseURL New server URL
     */
    public void updateBaseURL(String newBaseURL) {
        this.baseURL = newBaseURL;
        retrofit = null;  // Reset retrofit to recreate with new URL
        Log.d(TAG, "Base URL updated to: " + newBaseURL);
    }

    /**
     * Reset network manager (for testing or logout)
     */
    public static void reset() {
        instance = null;
    }
}

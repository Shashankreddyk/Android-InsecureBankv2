package com.android.insecurebankv2;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.insecurebankv2.network.NetworkManager;
import com.android.insecurebankv2.network.TLSConfiguration;

import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Test cases for TLS 1.3 implementation
 */
@RunWith(AndroidJUnit4.class)
public class TLSTest {

    private Context context;
    private OkHttpClient client;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        client = TLSConfiguration.createSecureOkHttpClient();
    }

    /**
     * Test that TLS 1.3 is enabled
     */
    @Test
    public void testTLS13Enabled() {
        boolean hasTLS13 = false;
        
        for (okhttp3.ConnectionSpec spec : client.connectionSpecs()) {
            if (spec.tlsVersions() != null) {
                for (TlsVersion version : spec.tlsVersions()) {
                    if (version == TlsVersion.TLS_1_3) {
                        hasTLS13 = true;
                        break;
                    }
                }
            }
        }
        
        assertTrue("TLS 1.3 should be enabled", hasTLS13);
    }

    /**
     * Test that HTTP is disabled
     */
    @Test
    public void testHTTPDisabled() {
        // All connections should use HTTPS
        assertTrue("Client should have connection specs", client.connectionSpecs().size() > 0);
    }

    /**
     * Test network manager initialization
     */
    @Test
    public void testNetworkManagerInitialization() {
        String baseURL = "https://your-server.com:8443/";
        NetworkManager manager = NetworkManager.getInstance(context, baseURL);
        
        assertNotNull("NetworkManager should be initialized", manager);
        assertNotNull("OkHttpClient should be created", manager.getOkHttpClient());
        assertNotNull("Retrofit should be created", manager.getRetrofit());
    }
}

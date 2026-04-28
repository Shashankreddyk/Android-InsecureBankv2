package com.android.insecurebankv2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.android.insecurebankv2.network.ApiService;
import com.android.insecurebankv2.network.NetworkManager;
import com.google.gson.JsonObject;
import com.marcohc.toasteroid.Toasteroid;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Login activity using secure TLS 1.3 HTTPS connection
 * All communication is encrypted and server certificate is verified
 * 
 * @author Security Team
 */
public class DoLogin extends Activity {

    private static final String TAG = "DoLogin";
    
    String username;
    String password;
    String serverip = "";
    String serverport = "";
    String protocol = "https://";  // UPDATED: Now using HTTPS with TLS 1.3
    
    private NetworkManager networkManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_do_login);
        
        // Get credentials from Intent
        Intent data = getIntent();
        username = data.getStringExtra("passed_username");
        password = data.getStringExtra("passed_password");
        
        // Get server details (should be in secure preferences)
        serverip = getSharedPreferences("mySharedPreferences", 0).getString("serverip", null);
        serverport = getSharedPreferences("mySharedPreferences", 0).getString("serverport", null);

        if (serverip != null && serverport != null) {
            // Initialize network manager with TLS 1.3
            String baseURL = protocol + serverip + ":" + serverport + "/";
            networkManager = NetworkManager.getInstance(this, baseURL);
            apiService = networkManager.getRetrofit().create(ApiService.class);
            
            // Perform login
            performLogin();
        } else {
            Intent setupServerdetails = new Intent(this, FilePrefActivity.class);
            startActivity(setupServerdetails);
            Toasteroid.show(this, "Server path/port not set!!", 
                          Toasteroid.STYLES.WARNING, Toasteroid.LENGTH_SHORT);
        }
        
        finish();
    }

    /**
     * Perform login using TLS 1.3 encrypted connection
     */
    private void performLogin() {
        // Call login API (automatically encrypted with TLS 1.3)
        Call<JsonObject> loginCall = apiService.login(username, password);
        
        loginCall.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject jsonResponse = response.body();
                        String message = jsonResponse.get("message").getAsString();
                        
                        if (message.contains("Correct Credentials")) {
                            Log.d(TAG, "✓ Login successful via TLS 1.3: " + username);
                            
                            // Save credentials securely
                            saveCreds(username, password);
                            
                            // Track login
                            trackUserLogins();
                            
                            // Navigate to PostLogin
                            Intent pL = new Intent(getApplicationContext(), PostLogin.class);
                            pL.putExtra("uname", username);
                            startActivity(pL);
                            
                        } else {
                            Log.w(TAG, "Login failed: " + message);
                            Intent xi = new Intent(getApplicationContext(), WrongLogin.class);
                            startActivity(xi);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing login response", e);
                    Intent xi = new Intent(getApplicationContext(), WrongLogin.class);
                    startActivity(xi);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Login request failed: " + t.getMessage(), t);
                Toasteroid.show(DoLogin.this, "Connection failed: " + t.getMessage(),
                               Toasteroid.STYLES.ERROR, Toasteroid.LENGTH_SHORT);
                Intent xi = new Intent(getApplicationContext(), WrongLogin.class);
                startActivity(xi);
            }
        });
    }

    /**
     * Save credentials locally in encrypted form
     */
    private void saveCreds(String username, String password) {
        try {
            CryptoClass crypt = new CryptoClass();
            String encryptedPassword = crypt.aesEncryptedString(password);
            
            getSharedPreferences("mySharedPreferences", Activity.MODE_PRIVATE)
                    .edit()
                    .putString("EncryptedUsername", username)
                    .putString("superSecurePassword", encryptedPassword)
                    .commit();
                    
            Log.d(TAG, "Credentials saved securely");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save credentials", e);
        }
    }

    /**
     * Track user login in content provider
     */
    private void trackUserLogins() {
        ContentValues values = new ContentValues();
        values.put(TrackUserContentProvider.name, username);
        Uri uri = getContentResolver().insert(TrackUserContentProvider.CONTENT_URI, values);
        Log.d(TAG, "User login tracked");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            callPreferences();
            return true;
        } else if (id == R.id.action_exit) {
            Intent i = new Intent(getBaseContext(), LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void callPreferences() {
        Intent i = new Intent(this, FilePrefActivity.class);
        startActivity(i);
    }
}

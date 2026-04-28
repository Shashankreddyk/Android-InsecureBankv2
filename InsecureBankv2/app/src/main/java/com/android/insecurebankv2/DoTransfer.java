package com.android.insecurebankv2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.insecurebankv2.network.ApiService;
import com.android.insecurebankv2.network.NetworkManager;
import com.google.gson.JsonObject;
import com.marcohc.toasteroid.Toasteroid;

import java.io.BufferedWriter;
import java.io.FileWriter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Transfer activity using secure TLS 1.3 HTTPS connection
 * 
 * @author Security Team
 */
public class DoTransfer extends Activity {

    private static final String TAG = "DoTransfer";
    
    EditText from;
    EditText to;
    EditText amount;
    Button transfer;
    Button getAccounts;
    
    String serverip = "";
    String serverport = "";
    String protocol = "https://";  // UPDATED: HTTPS with TLS 1.3
    
    private NetworkManager networkManager;
    private ApiService apiService;
    private String username;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_do_transfer);

        // Get server details
        serverip = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("serverip", null);
        serverport = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("serverport", null);

        // Initialize NetworkManager with TLS 1.3
        if (serverip != null && serverport != null) {
            String baseURL = protocol + serverip + ":" + serverport + "/";
            networkManager = NetworkManager.getInstance(this, baseURL);
            apiService = networkManager.getRetrofit().create(ApiService.class);
        }

        // Setup UI
        transfer = (Button) findViewById(R.id.button_Transfer);
        transfer.setOnClickListener(v -> performTransfer());

        getAccounts = (Button) findViewById(R.id.button_CreateUser);
        getAccounts.setOnClickListener(v -> fetchAccounts());
    }

    /**
     * Perform transfer using TLS 1.3 encrypted connection
     */
    private void performTransfer() {
        from = (EditText) findViewById(R.id.editText_from);
        to = (EditText) findViewById(R.id.editText_to);
        amount = (EditText) findViewById(R.id.editText_amount);

        // Get credentials from preferences
        SharedPreferences prefs = getSharedPreferences("mySharedPreferences", 0);
        username = prefs.getString("EncryptedUsername", null);
        password = prefs.getString("superSecurePassword", null);

        if (username == null || password == null) {
            Toasteroid.show(this, "Credentials not found", 
                          Toasteroid.STYLES.ERROR, Toasteroid.LENGTH_SHORT);
            return;
        }

        // Decrypt password
        try {
            CryptoClass crypt = new CryptoClass();
            password = crypt.aesDeccryptedString(password);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decrypt password", e);
            return;
        }

        // Call transfer API (encrypted with TLS 1.3)
        Call<JsonObject> transferCall = apiService.doTransfer(
                username,
                password,
                from.getText().toString(),
                to.getText().toString(),
                amount.getText().toString()
        );

        transferCall.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject jsonResponse = response.body();
                    String message = jsonResponse.get("message").getAsString();

                    if (message.contains("Success")) {
                        Toasteroid.show(DoTransfer.this, "Transfer Successful!!", 
                                      Toasteroid.STYLES.SUCCESS, Toasteroid.LENGTH_SHORT);
                        
                        // Save transaction history
                        saveTransactionHistory("Success", from.getText().toString(), 
                                             to.getText().toString(), 
                                             amount.getText().toString());
                        
                        Log.d(TAG, "✓ Transfer successful via TLS 1.3");
                    } else {
                        Toasteroid.show(DoTransfer.this, "Transfer Failed!!", 
                                      Toasteroid.STYLES.ERROR, Toasteroid.LENGTH_SHORT);
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Transfer request failed", t);
                Toasteroid.show(DoTransfer.this, "Connection failed: " + t.getMessage(),
                               Toasteroid.STYLES.ERROR, Toasteroid.LENGTH_SHORT);
            }
        });
    }

    /**
     * Fetch accounts using TLS 1.3
     */
    private void fetchAccounts() {
        SharedPreferences prefs = getSharedPreferences("mySharedPreferences", 0);
        String savedUsername = prefs.getString("EncryptedUsername", null);
        String savedPassword = prefs.getString("superSecurePassword", null);

        if (savedUsername == null || savedPassword == null) {
            return;
        }

        try {
            CryptoClass crypt = new CryptoClass();
            String decryptedPassword = crypt.aesDeccryptedString(savedPassword);

            Call<JsonObject> getAccountsCall = apiService.getAccounts(savedUsername, decryptedPassword);

            getAccountsCall.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject jsonResponse = response.body();
                        String fromAcc = jsonResponse.get("from").getAsString();
                        String toAcc = jsonResponse.get("to").getAsString();

                        from = (EditText) findViewById(R.id.editText_from);
                        to = (EditText) findViewById(R.id.editText_to);
                        from.setText(fromAcc);
                        to.setText(toAcc);

                        Log.d(TAG, "✓ Accounts fetched via TLS 1.3");
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "Failed to fetch accounts", t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error fetching accounts", e);
        }
    }

    /**
     * Save transaction history
     */
    private void saveTransactionHistory(String status, String from, String to, String amount) {
        try {
            String filename = Environment.getExternalStorageDirectory() + 
                            "/Statements_" + username + ".html";
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
            writer.write("Status: " + status + " | From: " + from + 
                        " | To: " + to + " | Amount: " + amount + "\n");
            writer.write("<hr>\n");
            writer.close();
            Log.d(TAG, "Transaction history saved");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save transaction history", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                callPreferences();
                return true;
            case R.id.action_exit:
                Intent i = new Intent(getBaseContext(), LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void callPreferences() {
        Intent i = new Intent(this, FilePrefActivity.class);
        startActivity(i);
    }
}

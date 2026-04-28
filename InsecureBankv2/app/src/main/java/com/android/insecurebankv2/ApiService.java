package com.android.insecurebankv2.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Retrofit service interface for API endpoints
 * All calls are automatically encrypted with TLS 1.3
 */
public interface ApiService {

    /**
     * Login endpoint
     * Uses HTTPS/TLS 1.3 for encryption in transit
     * 
     * @param username User's username
     * @param password User's password
     * @return JSON response with login status
     */
    @FormUrlEncoded
    @POST("/login")
    Call<JsonObject> login(
            @Field("username") String username,
            @Field("password") String password
    );

    /**
     * Get accounts for user
     * 
     * @param username User's username
     * @param password User's password
     * @return JSON response with account numbers
     */
    @FormUrlEncoded
    @POST("/getaccounts")
    Call<JsonObject> getAccounts(
            @Field("username") String username,
            @Field("password") String password
    );

    /**
     * Perform money transfer
     * 
     * @param username User's username
     * @param password User's password
     * @param fromAcc From account number
     * @param toAcc To account number
     * @param amount Transfer amount
     * @return JSON response with transfer status
     */
    @FormUrlEncoded
    @POST("/dotransfer")
    Call<JsonObject> doTransfer(
            @Field("username") String username,
            @Field("password") String password,
            @Field("from_acc") String fromAcc,
            @Field("to_acc") String toAcc,
            @Field("amount") String amount
    );

    /**
     * Change password
     * 
     * @param username User's username
     * @param newpassword New password
     * @return JSON response with change status
     */
    @FormUrlEncoded
    @POST("/changepassword")
    Call<JsonObject> changePassword(
            @Field("username") String username,
            @Field("newpassword") String newpassword
    );
}

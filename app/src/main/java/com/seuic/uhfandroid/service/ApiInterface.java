package com.seuic.uhfandroid.service;


import com.google.gson.JsonObject;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;


public interface ApiInterface {

    @POST("api/goldloan/push")
    Call<APIResponse.Response> postData(@Header("branch-id") String deviceId,
                                        @Header("fb-token") String fbToken,
                                        @Header("x-api-key") String apiKey,
                                        @Body RequestBody data);


    @POST("liveliness")
    Call<APIResponse.Response> postHearBeat(@Header("branch-id") String deviceId,
                                            @Header("fb-token") String fbToken,
                                            @Header("x-api-key") String apiKey,
                                            @Body JsonObject data);

    @POST("reader")
    Call<APIResponse.Response> postHardwareBeat(@Header("branch-id") String deviceId,
                                                @Header("fb-token") String fbToken,
                                                @Header("x-api-key") String apiKey);
}
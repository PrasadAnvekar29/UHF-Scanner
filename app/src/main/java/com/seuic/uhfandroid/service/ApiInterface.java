package com.seuic.uhfandroid.service;


import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;


public interface ApiInterface {

    @POST("api/goldloan/push")
 //   @POST("api/inventory/dummyread")
    Call<APIResponse.Response> postData(@Header("device_id") String deviceId, @Body RequestBody data);



}
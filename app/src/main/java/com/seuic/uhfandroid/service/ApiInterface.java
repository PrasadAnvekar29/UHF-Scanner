package com.seuic.uhfandroid.service;


import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;


public interface ApiInterface {

    @POST("api/inventory/read")
    Call<APIResponse> postData(@Body RequestBody data);



}
package com.seuic.uhfandroid.service;

import com.google.gson.annotations.SerializedName;
import com.seuic.uhfandroid.bean.TagBean;

import java.io.Serializable;
import java.util.List;

public class APIResponse implements Serializable {

    @SerializedName("success")
    private boolean isSuccess;
    @SerializedName("message")
    private String message;
    @SerializedName("data")
    private List<TagBean> data;

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<TagBean> getData() {
        return data;
    }

    public void setData(List<TagBean> data) {
        this.data = data;
    }
}
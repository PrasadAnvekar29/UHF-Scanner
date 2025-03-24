package com.seuic.uhfandroid.service;

import com.google.gson.annotations.SerializedName;
import com.seuic.uhfandroid.bean.TagBean;

import java.io.Serializable;
import java.util.List;


public class APIResponse {

    public class Response implements Serializable {

        @SerializedName("success")
        private boolean isSuccess;
        @SerializedName("message")
        private String message;
        @SerializedName("data")
        private List<Tag> data;

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

        public List<Tag> getData() {
            return data;
        }

        public void setData(List<Tag> data) {
            this.data = data;
        }
    }


    public class Tag implements Serializable {


        @SerializedName("epcId")
        private String epcId;

        @SerializedName("antenna")
        private String antenna;


        public String getEpcId() {
            return epcId;
        }

        public void setEpcId(String epcId) {
            this.epcId = epcId;
        }

        public String getAntenna() {
            return antenna;
        }

        public void setAntenna(String antenna) {
            this.antenna = antenna;
        }
    }

}


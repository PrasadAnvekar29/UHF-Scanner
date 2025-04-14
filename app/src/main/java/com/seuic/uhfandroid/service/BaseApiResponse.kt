package com.seuic.uhfandroid.service

import com.google.gson.JsonSyntaxException
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.text.isNullOrEmpty

open class  BaseApiResponse {

    fun <T> safeErrorResponse(response : Response<T>?): String {
        try {
            if(response!!.code() == 400){
                return "Server has encountered with internal error (400)."
            }

            if(response.code() == 403){
                return "You dont have access for this feature(403)."
            }

            if(response.code() == 404){
                return "Method not found(404)."
            }

            if(response.code() == 422){
                val jObjError = JSONObject(response!!.errorBody()!!.string())
                if(jObjError.has("error") && !jObjError.getString("error").isNullOrEmpty())
                    return jObjError.getString("error")+"(422)."

                if(jObjError.has("message") &&  !jObjError.getString("message").isNullOrEmpty())
                    return jObjError.getString("message")+"(422)."

                return response.message()+"(422)."
            }

            if(response.code() == 429){
                return "Too many requests, Try after sometimes(429)."
            }

            if(response.code() == 440){
                return "Session expired, please login again(440)."
            }

            if(response.code() == 500){
                return "Server has encountered with internal error (500)."
            }

            if(response.code() == 502){
                return "Server has encountered with internal error (502)."
            }


            val jObjError = JSONObject(response!!.errorBody()!!.string())
            if(jObjError.has("error") && !jObjError.getString("error").isNullOrEmpty())
                return jObjError.getString("error")

            if(jObjError.has("message") &&  !jObjError.getString("message").isNullOrEmpty())
                return jObjError.getString("message")

            return response.message()
        } catch (e: Exception) {
            return response!!.message()
        }
    }

    fun  safeErrorResponse(response : Throwable): String {

        try{
            if (response is SSLHandshakeException) {
                return response.message!!
            }

            if (response is UnknownHostException || response is IllegalStateException) {
                return "Please check your internet connection"
            }

            if (response is SocketTimeoutException) {
                return "Request timeout, please try again."
            }

            if (response is JsonSyntaxException) {
                return response.message!!
            }

            if(response is HttpException && response.code() == 400){
                return "Server has encountered with internal error (400)."
            }

            if(response is HttpException && response.code() == 403){
                return "You dont have access for this feature(403)."
            }

            if(response is HttpException && response.code() == 404){
                return "Method not found(404)."
            }

            if (response is HttpException && response.code() == 422) {
                val jObjError = JSONObject(response.response()!!.errorBody()!!.string())
                if(jObjError.has("error") && !jObjError.getString("error").isNullOrEmpty())
                    return jObjError.getString("error")+"(422)."

                if(jObjError.has("message") &&  !jObjError.getString("message").isNullOrEmpty())
                    return jObjError.getString("message")+"(422)."

                return response.response()!!.message()+"(422)."
            }

            if(response is HttpException && response.code() == 429){
                return "Too many requests, Try after sometimes(429)."
            }

            if(response is HttpException && response.code() == 440){
                return "Session expired, please login again(440)."
            }

            if(response is HttpException && response.code() == 500){
                return "Server has encountered with internal error (500)."
            }

            if(response is HttpException && response.code() == 502){
                return "Server has encountered with internal error (502)."
            }

            if (response is HttpException ) {
                val jObjError = JSONObject(response.response()!!.errorBody()!!.string())
                if(jObjError.has("error") && !jObjError.getString("error").isNullOrEmpty())
                    return jObjError.getString("error")

                if(jObjError.has("message") &&  !jObjError.getString("message").isNullOrEmpty())
                    return jObjError.getString("message")

                return response.response()!!.message()
            }
        }catch (e : Exception){

        }

        return "Something went wrong"
    }


}
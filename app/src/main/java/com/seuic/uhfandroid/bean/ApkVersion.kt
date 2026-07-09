package com.seuic.uhfandroid.bean

import com.google.gson.annotations.SerializedName


data class ApkVersion(var id:String) {
    @SerializedName("apk_url")
    var apkUrl: String? = null

    @SerializedName("apk_version")
    var apkVersion: String? = null

    @SerializedName("is_force_update")
    var isForceUpdate: Boolean? = null
}
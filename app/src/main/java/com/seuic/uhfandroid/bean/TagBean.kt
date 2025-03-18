package com.seuic.uhfandroid.bean

import java.io.Serializable

data class TagBean(
    var epcId:String,
    var rssi:Int,
    var times:Int,
    var antenna:String,//天线
    var additionalData:String//额外数据
) : Serializable
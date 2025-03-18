package com.seuic.uhfandroid.ext

import androidx.lifecycle.MutableLiveData
import com.seuic.uhfandroid.bean.TagBean
import java.util.concurrent.atomic.AtomicInteger


var inventoryDatas = MutableLiveData<TagBean>()

var inventoryListDatas = MutableLiveData<MutableList<TagBean>>()

// 当前盘点天线
var currentAntennaArray = intArrayOf()

var connectResult = MutableLiveData<Int>()

// 清除标签列表
var clearTagList = MutableLiveData<Boolean>()

// 当前选中的标签
var currentTag = ""

// 重置当前选中标签和标签列表的下标
var resetCurrentTag = MutableLiveData<Boolean>()

// 界面上的控件是否可以点击
var itemClickable = MutableLiveData<Boolean>()

// 正在寻卡时不可以进行设置等操作
var isSearching = false

var totalCounts: AtomicInteger = AtomicInteger(0)
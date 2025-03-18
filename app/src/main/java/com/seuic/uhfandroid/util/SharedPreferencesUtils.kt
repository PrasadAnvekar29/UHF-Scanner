package com.seuic.uhfandroid.util

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class SharedPreferencesUtils(context: Context) {
    private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val editor: SharedPreferences.Editor = sp.edit()

    // 是否正在寻卡(是的话不可以进行参数配置)
    private val isSearchingStr = "isSearching"

    // 功率
    private val powerStr = "power"

    // 频段
    private val regionStr = "region"

    // session
    private val sessionStr = "session"

    // Q
    private val qValueStr = "Q"

    // profile
    private val profileStr = "profile"

    // target
    private val targetStr = "target"

    // 附加数据
    private val enableAdditionalDataStr = "enableAdditionalData"

    // 存储区 起始地址 长度 访问密码
    private val bankStr = "bank"
    private val startAddressStr = "startAddress"
    private val lengthStr = "length"
    private val passwordStr = "password"

    // 过滤
    private val filterStartAddressStr = "filterStartAddress"
    private val filterBankStr = "filterBank"
    private val filterSuitStr = "filterSuit"
    private val filterDataStr = "filterData"
    private val filterNullStr = "filterNullStr"

    // 天线配置
    private val ant1Str = "ant1"
    private val ant2Str = "ant2"
    private val ant3Str = "ant3"
    private val ant4Str = "ant4"
    private val ant5Str = "ant5"
    private val ant6Str = "ant6"
    private val ant7Str = "ant7"
    private val ant8Str = "ant8"

    // 天线检测
    private val antCheckStr = "antCheck"

    // gpio
    private val gpo1Str = "gpo1"
    private val gpo2Str = "gpo2"
    private val gpo3Str = "gpo3"
    private val gpo4Str = "gpo4"

    // 蜂鸣器
    private val enableBuzzerStr = "buzzer"

    fun getFilterStartAddress(): String? {
        return sp.getString(filterStartAddressStr, "")
    }

    fun getFilterBank(): Int {
        return sp.getInt(filterBankStr, 1)
    }

    fun getFilterData(): String? {
        return sp.getString(filterDataStr, "")
    }

    fun getFilterSuit(): Boolean {
        return sp.getBoolean(filterSuitStr, false)
    }

    fun getFilterNull(): Boolean {
        return sp.getBoolean(filterNullStr, true)
    }

    fun getIsSearching(): Boolean {
        return sp.getBoolean(isSearchingStr, false)
    }

    fun getEnableBuzzer(): Boolean {
        return sp.getBoolean(enableBuzzerStr, true)
    }

    fun getPower(): Int {
        return sp.getInt(powerStr, 33)
    }

    /*
        RG_NONE(0),
        RG_NA(1),
        RG_EU(2),
        RG_EU2(7),
        RG_EU3(8),
        RG_KR(3),
        RG_PRC(6),
        RG_PRC2(10),
        RG_OPEN(255);
        实际只用了1和6
     */
    fun getRegion(): Int {
        return sp.getInt(regionStr, 0)
    }

    fun getSession(): Int {
        return sp.getInt(sessionStr, 0)
    }

    // 存下标
    fun getProfile(): Int {
        return sp.getInt(profileStr, 0)
    }

    fun getQ(): Int {
        return sp.getInt(qValueStr, 5)
    }

    fun getTarget(): Int {
        return sp.getInt(targetStr, 0)
    }

    fun getEnableAdditionalData(): Boolean {
        return sp.getBoolean(enableAdditionalDataStr, false)
    }

    /*
        bank0又称为保留区，存放着访问密码和销毁密码，每个密码都有32bit。
        bank1又称为EPC区，其中又含有CRC字段（16bit），PC字段（16bit），EPC码（最长可到达496bit，一般为96bit），
        bank2又称为TID区，含有全球唯一的序列号，共64bit。
        bank3又称为USER区，容量是变长，不同的标签品牌容量不同，也有很多标签没有bank3。
     */
    fun getBank(): Int {
        // 此处只表示spinner的下标
        return sp.getInt(bankStr, 0)
    }

    fun getLength(): String {
        return sp.getString(lengthStr, "12") ?: "12"
    }

    fun getPassword(): String {
        return sp.getString(passwordStr, "00000000") ?: "00000000"
    }

    fun getStartAddress(): String {
        return sp.getString(startAddressStr, "0") ?: "0"
    }

    fun getAnt1(): Boolean {
        return sp.getBoolean(ant1Str, false)
    }

    fun getAnt2(): Boolean {
        return sp.getBoolean(ant2Str, false)
    }

    fun getAnt3(): Boolean {
        return sp.getBoolean(ant3Str, false)
    }

    fun getAnt4(): Boolean {
        return sp.getBoolean(ant4Str, false)
    }

    fun getAnt5(): Boolean {
        return sp.getBoolean(ant5Str, false)
    }

    fun getAnt6(): Boolean {
        return sp.getBoolean(ant6Str, false)
    }

    fun getAnt7(): Boolean {
        return sp.getBoolean(ant7Str, false)
    }

    fun getAnt8(): Boolean {
        return sp.getBoolean(ant8Str, false)
    }

    fun getAntCheck(): Boolean {
        return sp.getBoolean(antCheckStr, true)
    }

    fun getGpo1(): Boolean {
        return sp.getBoolean(gpo1Str, true)
    }

    fun getGpo2(): Boolean {
        return sp.getBoolean(gpo2Str, false)
    }

    fun getGpo3(): Boolean {
        return sp.getBoolean(gpo3Str, false)
    }

    fun getGpo4(): Boolean {
        return sp.getBoolean(gpo4Str, false)
    }

    fun setIsSearching(isSearching: Boolean): SharedPreferencesUtils {
        editor.putBoolean(isSearchingStr, isSearching).commit()
        return this
    }

    fun setEnableBuzzer(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(enableBuzzerStr, enable).commit()
        return this
    }

    fun setPower(power: Int): SharedPreferencesUtils {
        editor.putInt(powerStr, power).commit()
        return this
    }

    fun setRegion(region: Int): SharedPreferencesUtils {
        editor.putInt(regionStr, region).commit()
        return this
    }

    fun setSession(session: Int): SharedPreferencesUtils {
        editor.putInt(sessionStr, session).commit()
        return this
    }

    fun setProfile(profile: Int): SharedPreferencesUtils {
        editor.putInt(profileStr, profile).commit()
        return this
    }

    fun setQ(q: Int): SharedPreferencesUtils {
        editor.putInt(qValueStr, q).commit()
        return this
    }

    fun setTarget(target: Int): SharedPreferencesUtils {
        editor.putInt(targetStr, target).commit()
        return this
    }

    fun setEnableAdditionalData(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(enableAdditionalDataStr, enable).commit()
        return this
    }

    fun setBank(bank: Int): SharedPreferencesUtils {
        editor.putInt(bankStr, bank).commit()
        return this
    }

    fun setLength(len: String): SharedPreferencesUtils {
        editor.putString(lengthStr, len).commit()
        return this
    }

    fun setPassword(pwd: String): SharedPreferencesUtils {
        editor.putString(passwordStr, pwd).commit()
        return this
    }

    fun setStartAddress(address: String): SharedPreferencesUtils {
        editor.putString(startAddressStr, address).commit()
        return this
    }

    fun setAnt1(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(ant1Str, enable).commit()
        return this
    }

    fun setAnt2(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(ant2Str, enable).commit()
        return this
    }

    fun setAnt3(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(ant3Str, enable).commit()
        return this
    }

    fun setAnt4(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(ant4Str, enable).commit()
        return this
    }

    fun setAnt5(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(ant5Str, enable).commit()
        return this
    }

    fun setAnt6(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(ant6Str, enable).commit()
        return this
    }

    fun setAnt7(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(ant7Str, enable).commit()
        return this
    }

    fun setAnt8(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(ant8Str, enable).commit()
        return this
    }

    fun setAntCheck(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(antCheckStr, enable).commit()
        return this
    }

    fun setGpo1(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(gpo1Str, enable).commit()
        return this
    }

    fun setGpo2(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(gpo2Str, enable).commit()
        return this
    }

    fun setGpo3(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(gpo3Str, enable).commit()
        return this
    }

    fun setGpo4(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(gpo4Str, enable).commit()
        return this
    }

    fun setFilterStartAddress(str: String): SharedPreferencesUtils {
        editor.putString(filterStartAddressStr, str).commit()
        return this
    }

    fun setFilterBank(bank: Int): SharedPreferencesUtils {
        editor.putInt(filterBankStr, bank).commit()
        return this
    }

    fun setFilterData(data: String): SharedPreferencesUtils {
        editor.putString(filterDataStr, data).commit()
        return this
    }

    fun setFilterSuit(suit: Boolean): SharedPreferencesUtils {
        editor.putBoolean(filterSuitStr, suit).commit()
        return this
    }

    fun setFilterNull(enable: Boolean): SharedPreferencesUtils {
        editor.putBoolean(filterNullStr, enable).commit()
        return this
    }
}

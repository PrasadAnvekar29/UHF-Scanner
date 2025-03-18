package com.seuic.uhfandroid.adapter

import android.graphics.Color
import android.util.Log
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.seuic.uhfandroid.R
import com.seuic.uhfandroid.bean.TagBean

class TagInfoAdapter(layoutResId: Int) : BaseQuickAdapter<TagBean, BaseViewHolder>(layoutResId) {

    var mPosition = -1

    override fun convert(holder: BaseViewHolder, item: TagBean) {
        holder.apply {
            val pos = adapterPosition + 1
            setText(R.id.tv_serial_number, pos.toString())
            setText(R.id.tv_epc, item.epcId)
            setText(R.id.tv_rssi, item.rssi.toString())
            setText(R.id.tv_times, item.times.toString())
            setText(R.id.tv_reader_antenna, item.antenna)
            setText(R.id.tv_additional_data, item.additionalData)
            if (adapterPosition == mPosition) {
                setBackgroundColor(R.id.cl_tag, Color.parseColor("#FF158BEB"))
            } else {
                setBackgroundColor(R.id.cl_tag, context.getColor(R.color.white))
            }
        }
    }

    fun setPosition(position: Int) {
        mPosition = position
    }
}
package com.esthenos.presentation.ui.attendance

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.seuic.uhfandroid.LogsActivity
import com.seuic.uhfandroid.R
import com.seuic.uhfandroid.database.LogEntry


class LogsViewHolder(itemView: View, private val mActivity: LogsActivity) : RecyclerView.ViewHolder(itemView) {

    private var model: LogEntry? = null
    private val tv_serial_number: TextView
    private val tv_name: TextView
    private val tv_times: TextView
    init {
        tv_serial_number = itemView.findViewById<View>(R.id.tv_serial_number) as TextView
        tv_name = itemView.findViewById<View>(R.id.tv_name) as TextView
        tv_times = itemView.findViewById<View>(R.id.tv_times) as TextView
    }

    fun bind(position: Int, model: LogEntry) {
        this.model = model
        tv_serial_number.text = (model.id + 1).toString()
        tv_name.text = model.error
        tv_times.text = model.time


    }


}

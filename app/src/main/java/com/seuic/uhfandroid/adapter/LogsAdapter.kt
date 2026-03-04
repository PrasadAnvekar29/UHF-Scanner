package com.esthenos.presentation.ui.attendance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.seuic.uhfandroid.LogsActivity
import com.seuic.uhfandroid.R
import com.seuic.uhfandroid.database.LogEntry

class LogsAdapter(mApplicationList: List<LogEntry>, private val mActivity: LogsActivity) : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    private val VIEW_TYPE_ITEM = 0
    private val VIEW_TYPE_LOADING = 1
    private val mApplicationList: MutableList<LogEntry?>?

    init {
        this.mApplicationList = ArrayList<LogEntry?>()
        for (i in mApplicationList.indices) {
            this.mApplicationList.add(mApplicationList[i])
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View = LayoutInflater.from(viewGroup.context).inflate(R.layout.layout_logs, viewGroup, false)
        return LogsViewHolder(view, mActivity)

    }

    override fun onBindViewHolder(itemViewHolder: RecyclerView.ViewHolder, position: Int) {
        val model: LogEntry? = mApplicationList!![position]
        val viewHolder = itemViewHolder as LogsViewHolder
        viewHolder.bind(position, model!!)
    }

    override fun getItemCount(): Int {
        return mApplicationList?.size ?: 0
    }

    override fun getItemViewType(position: Int): Int {
        return if (mApplicationList!![position] == null) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
    }




}
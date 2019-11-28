package com.example.bluetootha2dpdemo

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.TextView
import com.sf.lib_scannerbluetooth.BtDevice
import kotlinx.android.synthetic.main.item.view.*


class DeviceAdapter(var datas: MutableList<BtDevice>? = null) : RecyclerView.Adapter<DeviceAdapter.MyViewHolder>(),
    View.OnClickListener {


    var position: Int? = 0

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): MyViewHolder {
        val itemView = LayoutInflater.from(p0.context).inflate(R.layout.item, p0, false)
        val baseViewHolder = MyViewHolder(itemView)
        itemView.setOnClickListener(this)
        return baseViewHolder
    }


    override fun onBindViewHolder(p0: MyViewHolder, p1: Int) {
        this.position = p1
        p0.itemView.setTag(p0)
        p0.textView.setText("name:"+datas?.get(p1)?.name+"  mac:"+datas?.get(p1)?.macAddr)
    }

    override fun getItemCount(): Int {
        return datas?.size ?: 0
    }


    override fun onClick(v: View?) {
        onItemClickListner?.onItemClick(v, position)
    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textView: TextView

        init {
            textView = itemView.name
        }
    }

    var onItemClickListner: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(v: View?, position: Int?)
    }


    fun addDevice(device: BtDevice) {
        var isContain= datas?.contains(device)
        if (isContain!!) return
        datas?.add(device)
        notifyDataSetChanged()
    }


}
package com.example.pixellights

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pixellights.ui.bluetooth.BluetoothFragment
import kotlinx.android.synthetic.main.device_item_row.view.*

class DeviceRecyclerAdapter(
    private val items : List<BluetoothFragment.ScanResultModel>,
    private val onClickListener : ((device: BluetoothFragment.ScanResultModel) -> Unit )
) : RecyclerView.Adapter<DeviceRecyclerAdapter.ViewHolder>() {

    private var selectedDeviceAddress : String = ""
    private var currentIndex = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.device_item_row,
            parent,
            false
        )
        return ViewHolder(view /*, onClickListener*/)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        if ( selectedDeviceAddress == item.scanResult?.device?.address )
        {
            //holder.itemView.setBackgroundColor(Color.BLUE)
            if (currentIndex == -1)
            {
                currentIndex = position
            }
        }
        else
        {
            holder.itemView.setBackgroundColor(Color.GRAY)
        }

        holder.itemView.setOnClickListener {
            selectedDeviceAddress = item.scanResult?.device?.address ?: ""
            if ( currentIndex != -1 ) {
                items[currentIndex].isSelected = false
            }

            currentIndex = position
            notifyDataSetChanged()
            onClickListener.invoke(item)
        }
    }



    class ViewHolder(
        private val view: View
    ) : RecyclerView.ViewHolder(view) {


        fun bind(result: BluetoothFragment.ScanResultModel) {
            view.device_name.text = result.scanResult?.device?.name ?: "Unnamed"
            view.device_address.text = result.scanResult?.device?.address
            view.signal_strength.text = "${result.scanResult?.rssi} dBm"
        }


    }

}
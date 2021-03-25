package com.example.pixellights.ui.bluetooth

import android.Manifest
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.pixellights.*
import com.example.pixellights.PixelLightsViewModel
import kotlinx.android.synthetic.main.fragment_bluetooth.*


class BluetoothFragment : Fragment() {

    private val pixelViewModel: PixelLightsViewModel by activityViewModels()
    private lateinit var linearLayoutManager : LinearLayoutManager

    class ScanResultModel(var scanResult: ScanResult? = null, var isSelected: Boolean = false)

    private val scanResults = mutableListOf<ScanResultModel>()
    private val scanResultAdapter: DeviceRecyclerAdapter by lazy {
        DeviceRecyclerAdapter(scanResults) { result ->
            if (isScanning) {
                stopBleScan()
            }
            with(result.scanResult?.device) {
                // Don't reconnect to the same device twice.
                if ( pixelViewModel.bluetoothDeviceAddress != this?.address ) {

                    if ( pixelViewModel.bluetoothDeviceAddress != "" ) {
                        pixelViewModel.bluetoothGatt?.disconnect()
                        pixelViewModel.bluetoothDeviceAddress = ""
                    }

                    println("Connecting to address ${this?.address}")
                    //connect
                    pixelViewModel.bluetoothDevice = this
                    selected_device.text = this?.name ?: "Unnamed"
                    setupDisconnectButton()

                    pixelViewModel.bluetoothDevice?.connectGatt(activity, false, pixelViewModel.gattCallback)


                }
            }
        }
    }

    // Data

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_bluetooth, container, false)

        linearLayoutManager = LinearLayoutManager(context)

        return root
    }

    private var isScanning = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refresh_button.setOnClickListener {
            if (isScanning) {
                stopBleScan()
            } else {
                checkBleScan()
            }
        }

        selected_device.text = pixelViewModel.bluetoothDevice?.name ?: ""

        disconnect_button.setOnClickListener {
            pixelViewModel.bluetoothGatt?.disconnect()
            pixelViewModel.bluetoothDeviceAddress = ""
            disconnect_button.visibility = View.INVISIBLE
            selected_device.text = ""
            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()
        }

        setupDisconnectButton()

        device_list.layoutManager = linearLayoutManager
        setupRecyclerView()
    }

    private fun setupDisconnectButton()
    {
        if ( pixelViewModel.bluetoothDevice != null)
        {
            disconnect_button.text = "Disconnect Device"
            disconnect_button.visibility = View.VISIBLE
        }
        else
        {
            disconnect_button.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        device_list.apply {
            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(
                context,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

        val animator = device_list.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

        val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL )
        device_list.addItemDecoration(itemDecoration)
    }

    private fun checkBleScan() {
        Log.d("ScanDeviceActivity", "checkBleScan()")
        when (activity?.let { PermissionChecker.checkSelfPermission(
            it,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) }) {
            PackageManager.PERMISSION_GRANTED -> startBleScan()
            else -> requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if ( grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("ScanDevices", "onRequestPermissionsResult(PERMISSION_GRANTED)")
                    startBleScan()
                }
                else {
                    Log.d("ScanDevices", "onRequestPermissionsResult(not PERMISSION_GRANTED)")
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.scanResult?.device?.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = ScanResultModel(result)
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    Log.d("ScanCallback", "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }
                scanResults.add(ScanResultModel(result))
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("ScanCallback", "onScanFailed: code $errorCode")
        }
    }

    private fun startBleScan() {
        if (scanResults.isNotEmpty())
        {
            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()
        }
        val activity: MainActivity? = activity as MainActivity?
        activity?.bleScanner?.startScan(null, scanSettings, scanCallback)
        isScanning = true
        refresh_button.setImageResource(R.drawable.stop_icon)
    }

    private fun stopBleScan() {
        val activity: MainActivity? = activity as MainActivity?
        activity?.bleScanner?.stopScan(scanCallback)
        isScanning = false
        refresh_button.setImageResource(R.drawable.ic_refresh_black_24dp)
    }


}

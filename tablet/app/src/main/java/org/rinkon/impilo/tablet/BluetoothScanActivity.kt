package org.rinkon.impilo.tablet

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

@SuppressLint("NewApi")
class BluetoothScanActivity : AppCompatActivity() {

    //private val requestEnableBT = 1
    private val scanPeriod: Long = 120000

    private var mScanning = false
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mLeDeviceListAdapter: LeDeviceListAdapter? = LeDeviceListAdapter()
    private var mHandler: Handler? = null
    private var mLeScanCallback =
        LeScanCallback { device, _, _ ->
            runOnUiThread {
                //Toast.makeText(this, device.address, Toast.LENGTH_SHORT).show()
                mLeDeviceListAdapter!!.addDevice(device)
            }
        }
    private var mResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        val data: Intent? = result.data
        // data can be null
        if ((data == null || data.action == BluetoothAdapter.ACTION_REQUEST_ENABLE)
                && result.resultCode == RESULT_CANCELED) {
            Toast.makeText(this, R.string.error_bluetooth_required, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_scan)

        val recyclerView: RecyclerView = findViewById(R.id.bluetooth_devices)
        recyclerView.adapter = mLeDeviceListAdapter
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        mHandler = Handler(Looper.getMainLooper())

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show()
            finish()
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bluetooth_scan, menu)
        if (!mScanning) {
            menu?.findItem(R.id.menu_stop)!!.isVisible = false
            menu.findItem(R.id.menu_scan)!!.isVisible = true
            menu.findItem(R.id.menu_refresh).actionView = null
        } else {
            menu?.findItem(R.id.menu_stop)!!.isVisible = true
            menu.findItem(R.id.menu_scan)!!.isVisible = false
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.fragment_progressbar)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onResume() {
        super.onResume()
        if (!mBluetoothAdapter!!.isEnabled) {
            // intent with empty data
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            mResultLauncher.launch(intent)
            //startActivityForResult(intent, requestEnableBT)
        }

        // Initializes list view adapter.
        //mLeDeviceListAdapter = LeDeviceListAdapter()
        scanLeDevice(true)
        Toast.makeText(this, "Please click on the Bluetooth device you want to connect to.", Toast.LENGTH_SHORT).show()
    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // User chose not to enable Bluetooth.
        if (requestCode == requestEnableBT && resultCode == RESULT_CANCELED) {
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }*/

    override fun onPause() {
        super.onPause()
        scanLeDevice(false)
        mLeDeviceListAdapter!!.clear()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> Toast.makeText(this, "refresh", Toast.LENGTH_LONG).show()
            R.id.menu_scan -> {
                mLeDeviceListAdapter!!.clear()
                scanLeDevice(true)
                Toast.makeText(this, "scan", Toast.LENGTH_LONG).show()
            }
            R.id.menu_stop -> {
                Toast.makeText(this, "stop", Toast.LENGTH_LONG).show()
                scanLeDevice(false)
            }
            android.R.id.home -> {
                Toast.makeText(this, "home", Toast.LENGTH_LONG).show()
            }
            else -> Toast.makeText(this, "onOptionsItemSelected error: " + item.itemId,
                Toast.LENGTH_LONG).show()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler!!.postDelayed({
                mScanning = false
                mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
                invalidateOptionsMenu()
            }, scanPeriod)
            mScanning = true

            for (device in mBluetoothAdapter!!.bondedDevices) mLeDeviceListAdapter!!.addDevice(device)

            if (mBluetoothAdapter!!.startLeScan(mLeScanCallback)) {
                Toast.makeText(this, "scan started", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "scan NOT started", Toast.LENGTH_SHORT).show()
            }
        } else {
            mScanning = false
            mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
        }
        invalidateOptionsMenu()
    }

    internal class LeDeviceListAdapter : RecyclerView.Adapter<LeDeviceListAdapter.DeviceViewHolder>() {

        internal class DeviceViewHolder(itemView: View, private val adapter: LeDeviceListAdapter) :
                    RecyclerView.ViewHolder(itemView), View.OnClickListener {
            val deviceName: TextView
            val deviceAddress: TextView

            init {
                deviceName = itemView.findViewById(R.id.device_name)
                deviceAddress = itemView.findViewById(R.id.device_address)
                itemView.setOnClickListener(this)
            }

            override fun onClick(view: View?) {
                val position: Int = this.layoutPosition
                val device = adapter.getDevice(position)
                Toast.makeText(view?.context, device.address, Toast.LENGTH_SHORT).show()
                val intent = Intent(view?.context, BluetoothDeviceActivity::class.java)
                view?.context?.startActivity(intent)
            }
        }

        private val dataSet: ArrayList<BluetoothDevice> = ArrayList()

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): DeviceViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.list_item_device, viewGroup, false)
            return DeviceViewHolder(view, this)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(viewHolder: DeviceViewHolder, position: Int) {
            // Get element from your dataset at this position and replace the
            // contents of the view with that element
            val device = dataSet[position]
            val deviceClass = device.bluetoothClass
            val name = device.name + " alias: " + device.alias + " type: " + device.type +
                    " class: " + deviceClass.majorDeviceClass + "." + deviceClass.deviceClass +
                    " bondState: " + device.bondState +
                    " uuids: " + device.uuids?.joinToString(",","","",-1,"...",
                        { it -> it.uuid.toString() })
            viewHolder.deviceName.text = name
            viewHolder.deviceAddress.text = device.address
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = dataSet.size

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        fun addDevice(device: BluetoothDevice) {
            if (!dataSet.contains(device) && dataSet.add(device)) {
                notifyItemInserted(dataSet.size - 1)
            }
        }

        private fun getDevice(position: Int): BluetoothDevice {
            return dataSet[position]
        }

        fun clear() {
            dataSet.clear()
            notifyDataSetChanged()
        }
    }
}
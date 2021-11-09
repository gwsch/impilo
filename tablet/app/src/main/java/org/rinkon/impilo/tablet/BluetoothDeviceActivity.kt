package org.rinkon.impilo.tablet

import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.rinkon.impilo.tablet.BluetoothLeService.LocalBinder

class BluetoothDeviceActivity : AppCompatActivity() {

    private val logTag: String = BluetoothDeviceActivity::class.simpleName!!

    private val replyBleData = byteArrayOf(0x78.toByte(), 0x02.toByte(), 0x51.toByte(), 0x53.toByte(), 0x00.toByte())
    private val replyBleDataHist =
        byteArrayOf(0x78.toByte(), 0x02.toByte(), 0x52.toByte(), 0x54.toByte(), 0x00.toByte())

    private var mConnectionState: TextView? = null
    private var mConnected = false
    private var mServiceConnected: RadioButton? = null
    private var mDeviceAddress: String? = null
    private var mBluetoothLeService: BluetoothLeService? = null
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as LocalBinder).service
            if (mBluetoothLeService == null || !mBluetoothLeService!!.initialize()) {
                Log.e(logTag, "Unable to initialize Bluetooth")
                finish()
            }
            // Automatically connects to the device upon successful start-up initialization.
            mServiceConnected?.isChecked = mBluetoothLeService!!.connect(mDeviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
            mServiceConnected?.isChecked = false
        }
    }
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (BluetoothLeConst.ACTION_GATT_CONNECTED == action) {
                mConnected = true
                updateConnectionState(R.string.connected)
                invalidateOptionsMenu()
                setWaits()
            } else if (BluetoothLeConst.ACTION_GATT_DISCONNECTED == action) {
                mConnected = false
                updateConnectionState(R.string.disconnected)
                invalidateOptionsMenu()
                clearUI()
            } else if (BluetoothLeConst.ACTION_GATT_SERVICES_DISCOVERED == action) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService!!.getSupportedGattServices())
            } else if (BluetoothLeConst.ACTION_DATA_AVAILABLE == action) {
                val data = intent.getByteArrayExtra(BluetoothLeConst.EXTRA_DATA) ?: return
                val tmpLen = data.size
                if (tmpLen == 0) return
                val tmpRes: Int = checkReceiveData(data)
                if (tmpRes == 2) {
                    if (mBluetoothLeService == null || !mConnected) return
                    Handler(Looper.getMainLooper()).postDelayed({ // TODO Auto-generated method stub
                        mBluetoothLeService!!.writeCharacteristic(replyBleData)
                    }, 10)
                } else if (tmpRes == 5) {
                    if (mBluetoothLeService == null || !mConnected) return
                    Handler(Looper.getMainLooper()).postDelayed({ // TODO Auto-generated method stub
                        mBluetoothLeService!!.writeCharacteristic(replyBleDataHist)
                    }, 10)
                }
            }
        }
    }

    private fun clearUI() {}

    private fun setWaits() {}

    private fun checkReceiveData(revData: ByteArray): Int {
        return 0
    }

    private fun updateConnectionState(resourceId: Int) {
        runOnUiThread { mConnectionState?.setText(resourceId) }
    }

    private fun displayGattServices(gattServices: List<BluetoothGattService?>?) {
        // if (gattServices == null)
        //    return
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeConst.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeConst.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeConst.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeConst.ACTION_DATA_AVAILABLE)
        return intentFilter
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (mBluetoothLeService != null) {
            val result = mBluetoothLeService!!.connect(mDeviceAddress)
            Log.d(logTag, "Connect request result=$result")
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mGattUpdateReceiver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_device)
        val deviceName: TextView = findViewById(R.id.device_name_text)
        deviceName.text = intent.getStringExtra("name")
        val deviceAddress: TextView = findViewById(R.id.device_address_text)
        mDeviceAddress = intent.getStringExtra("address")
        deviceAddress.text = mDeviceAddress
        val scanCount: TextView = findViewById(R.id.device_scan_count)
        scanCount.text = intent.getIntExtra("scanCount", 0).toString()
        val connectBtn: Button = findViewById(R.id.device_connect)
        val disconnectBtn: Button = findViewById(R.id.device_disconnect)
        connectBtn.setOnClickListener {
            val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
            connectBtn.isEnabled = false
            disconnectBtn.isEnabled = true
        }
        disconnectBtn.setOnClickListener {
            mBluetoothLeService!!.disconnect()
            connectBtn.isEnabled = true
            disconnectBtn.isEnabled = false
        }
        mServiceConnected = findViewById(R.id.address_connected)
        mConnectionState = findViewById(R.id.gatt_connected)
    }
}
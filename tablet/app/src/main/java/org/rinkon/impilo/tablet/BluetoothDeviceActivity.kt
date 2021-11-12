package org.rinkon.impilo.tablet

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.rinkon.impilo.tablet.BluetoothLeService.LocalBinder

class BluetoothDeviceActivity : AppCompatActivity() {

    private val logTag: String = BluetoothDeviceActivity::class.simpleName!!

    // val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
    // val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
    private val LIST_NAME = "NAME"
    private val LIST_UUID = "UUID"

    private val replyBleData = byteArrayOf(0x78.toByte(), 0x02.toByte(), 0x51.toByte(), 0x53.toByte(), 0x00.toByte())
    private val replyBleDataHist = byteArrayOf(0x78.toByte(), 0x02.toByte(), 0x52.toByte(), 0x54.toByte(), 0x00.toByte())

    private var mConnectionState: TextView? = null
    private var mConnected = false
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private var mServiceConnected: RadioButton? = null
    private var mConnectBtn: Button? = null
    private var mDisconnectBtn: Button? = null
    private var mDeviceAddress: String? = null
    private var mConnectCount = 0
    private var mScanCount = 0
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
            disconnectUI()
        }

        override fun onBindingDied(name: ComponentName?) {
            super.onBindingDied(name)
        }

        override fun onNullBinding(name: ComponentName?) {
            super.onNullBinding(name)
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
                //clearUI()
                reconnect()
            } else if (BluetoothLeConst.ACTION_GATT_SERVICES_DISCOVERED == action) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService!!.getSupportedGattServices())
            } else if (BluetoothLeConst.ACTION_DATA_AVAILABLE == action) {
                val data = intent.getByteArrayExtra(BluetoothLeConst.EXTRA_DATA) ?: return
                if (data.isEmpty()) return
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

    // Auto disconnect
    private fun clearUI() {
        disconnectUI()
    }

    private fun reconnect() {
        runOnUiThread {
            unbindService(mServiceConnection)
            connectUI()
        }
    }

    private fun setWaits() {
        // Thread.sleep(10000)
    }

    private fun checkReceiveData(revData: ByteArray): Int {
        return 0
    }

    private fun updateConnectionState(resourceId: Int) {
        runOnUiThread { mConnectionState?.setText(resourceId) }
    }

    private fun displayGattServices(gattServices: List<BluetoothGattService?>?) {
        if (gattServices == null) return
        val unknownServiceString = resources.getString(R.string.unknown_service)
        val unknownCharaString = resources.getString(R.string.unknown_characteristic)
        val gattServiceData = ArrayList<HashMap<String, String>>()
        val gattCharacteristicData = ArrayList<ArrayList<HashMap<String, String>>>()
        val mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()

        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val currentServiceData = HashMap<String, String>()
            val uuid1 = gattService!!.uuid.toString()
            if (uuid1.contains("fff0")) {
                currentServiceData[LIST_NAME] = SampleGattAttributes.lookup(uuid1, unknownServiceString)
                currentServiceData[LIST_UUID] = uuid1
                gattServiceData.add(currentServiceData)
                val gattCharacteristicGroupData = ArrayList<HashMap<String, String>>()
                val gattCharacteristics = gattService.characteristics
                val charas = ArrayList<BluetoothGattCharacteristic>()

                // Loops through available Characteristics.
                for (gattCharacteristic in gattCharacteristics) {
                    charas.add(gattCharacteristic)
                    val currentCharaData = HashMap<String, String>()
                    val uuid2 = gattCharacteristic.uuid.toString()
                    if (uuid2.contains("fff4")) {
                        currentCharaData[LIST_NAME] = SampleGattAttributes.lookup(uuid2, unknownCharaString)
                        currentCharaData[LIST_UUID] = uuid2
                        gattCharacteristicGroupData.add(currentCharaData)
                        if (mGattCharacteristics != null) {
                            val charaProp = gattCharacteristic.properties
                            if (charaProp or BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                                // If there is an active notification on a characteristic, clear
                                // it first so it doesn't update the data field on the user interface.
                                if (mNotifyCharacteristic != null) {
                                    mBluetoothLeService!!.setCharacteristicNotification(mNotifyCharacteristic!!, false)
                                    mNotifyCharacteristic = null
                                }
                                mBluetoothLeService!!.readCharacteristic(gattCharacteristic)
                            }
                            if (charaProp or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                                mNotifyCharacteristic = gattCharacteristic
                                mBluetoothLeService!!.setCharacteristicNotification(gattCharacteristic, true)
                            }
                        }
                    }
                }
                mGattCharacteristics.add(charas)
                gattCharacteristicData.add(gattCharacteristicGroupData)
            }
        }
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
        deviceName.text = "device name: " + intent.getStringExtra("name")
        val deviceAddress: TextView = findViewById(R.id.device_address_text)
        mDeviceAddress = intent.getStringExtra("address")
        deviceAddress.text = "address: " + mDeviceAddress
        val scanCount: TextView = findViewById(R.id.device_scan_count)
        mScanCount = intent.getIntExtra("scanCount", 0)
        scanCount.text = "scans: " + mScanCount.toString()
        mConnectBtn = findViewById(R.id.device_connect)
        mDisconnectBtn = findViewById(R.id.device_disconnect)
        mConnectBtn?.setOnClickListener {
            connectUI()
        }
        mDisconnectBtn?.setOnClickListener {
            mBluetoothLeService!!.disconnect()
            disconnectUI()
        }
        mServiceConnected = findViewById(R.id.address_connected)
        mConnectionState = findViewById(R.id.gatt_connected)
    }

    private fun connectUI() {
        val scanCount: TextView = findViewById(R.id.device_scan_count)
        scanCount.text = "scans: " + mScanCount.toString() + " connects: " + (++mConnectCount).toString()
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        if (bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)) {
            mConnectBtn?.isEnabled = false
            mDisconnectBtn?.isEnabled = true
        }
    }

    private fun disconnectUI() {
        unbindService(mServiceConnection)
        mBluetoothLeService = null
        mServiceConnected?.isChecked = false
        mConnectBtn?.isEnabled = true
        mDisconnectBtn?.isEnabled = false
    }
}
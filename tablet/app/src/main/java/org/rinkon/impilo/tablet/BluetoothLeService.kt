package org.rinkon.impilo.tablet

import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.*

class BluetoothLeService : Service() {

    private val logTag: String = BluetoothLeService::class.simpleName!!

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mBluetoothDeviceAddress: String? = null

    private val stateDisconnected = 0
    private val stateConnecting = 1
    private val stateConnected = 2

    private var mConnectionState: Int = stateDisconnected

    private val svrUUIDKey = "0000ffe5-0000-1000-8000-00805f9b34fb"
    private val chrUUIDKey = "0000ffe9-0000-1000-8000-00805f9b34fb"

    val ACTION_GATT_CONNECTED = "org.rinkon.impilo.tablet.ACTION_GATT_CONNECTED"
    val ACTION_GATT_DISCONNECTED = "org.rinkon.impilo.tablet.ACTION_GATT_DISCONNECTED"
    val ACTION_GATT_SERVICES_DISCOVERED = "org.rinkon.impilo.tablet.ACTION_GATT_SERVICES_DISCOVERED"
    val ACTION_DATA_AVAILABLE = "org.rinkon.impilo.tablet.ACTION_DATA_AVAILABLE"
    val EXTRA_DATA = "org.rinkon.impilo.tablet.EXTRA_DATA"

    val UUID_HEART_RATE_MEASUREMENT = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT)

    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                mConnectionState = stateConnected
                broadcastUpdate(intentAction)
                Log.i(logTag, "Connected to GATT server.")
                // Attempts to discover services after successful connection.
                Log.i(
                    logTag, "Attempting to start service discovery:" +
                            mBluetoothGatt!!.discoverServices()
                )
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                mConnectionState = stateDisconnected
                Log.i(logTag, "Disconnected from GATT server.")
                broadcastUpdate(intentAction)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(logTag, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(
        action: String,
        characteristic: BluetoothGattCharacteristic
    ) {
        val intent = Intent(action)

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT == characteristic.uuid) {
            val flag = characteristic.properties
            val format: Int
            if (flag and 0x01 != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16
                Log.d(logTag, "Heart rate format UINT16.")
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8
                Log.d(logTag, "Heart rate format UINT8.")
            }
            val heartRate = characteristic.getIntValue(format, 1)
            Log.d(logTag, String.format("Received heart rate: %d", heartRate))
            intent.putExtra(EXTRA_DATA, heartRate.toString())
        } else {
            // For all other profiles, writes the data formatted in HEX.
            val data = characteristic.value
            if (data != null && data.isNotEmpty()) {
                intent.putExtra(EXTRA_DATA, data)
            }
        }
        sendBroadcast(intent)
    }

    class LocalBinder : Binder() {
        var service: BluetoothLeService? = null
    }

    private val mBinder: LocalBinder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        mBinder.service = this@BluetoothLeService
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close()
        return super.onUnbind(intent)
    }

    fun close() {
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }
    fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(logTag, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress && mBluetoothGatt != null) {
            Log.d(logTag, "Trying to use an existing mBluetoothGatt for connection.")
            return if (mBluetoothGatt!!.connect()) {
                mConnectionState = stateConnecting
                true
            } else {
                false
            }
        }
        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        if (device == null) {
            Log.w(logTag, "Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        Log.d(logTag, "Trying to create a new connection.")
        mBluetoothDeviceAddress = address
        mConnectionState = stateConnecting
        return true
    }

    fun disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(logTag, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt?.disconnect()
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(logTag, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt?.readCharacteristic(characteristic)
    }

    fun writeCharacteristic(charData: ByteArray): Int {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(logTag, "BluetoothAdapter not initialized")
            return -1
        }
        val service = mBluetoothGatt?.getService(UUID.fromString(svrUUIDKey))
        val characteristic1 = service?.getCharacteristic(UUID.fromString(chrUUIDKey))
        characteristic1?.value = charData
        characteristic1?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        val tmpResult = mBluetoothGatt?.writeCharacteristic(characteristic1)
        return if (true == tmpResult) 1 else 0
    }

    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(logTag, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt?.setCharacteristicNotification(characteristic, enabled)

        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.uuid)) {
            val descriptor = characteristic.getDescriptor(
                UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG)
            )
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            mBluetoothGatt?.writeDescriptor(descriptor)
        }
    }

    fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return mBluetoothGatt?.services
    }
}
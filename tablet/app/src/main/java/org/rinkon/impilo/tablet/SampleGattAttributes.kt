package org.rinkon.impilo.tablet

class SampleGattAttributes {

    companion object {
        fun lookup(uuid: String, defaultName: String): String {
            val name = attributes[uuid]
            return name ?: defaultName
        }

        //private val svrUUIDKey = "0000ffe5-0000-1000-8000-00805f9b34fb"
        //private val chrUUIDKey = "0000ffe9-0000-1000-8000-00805f9b34fb"

        private val attributes = HashMap<String, String>()
        //var BPM_METER_MEASUREMENT = "0000fff0-0000-1000-8000-00805f9b34fb"
        var HEART_RATE_MEASUREMENT = "0000ffe1-0000-1000-8000-00805f9b34fb"
        var CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

        init {
            // Sample Services.
            attributes["0000180d-0000-1000-8000-00805f9b34fb"] = "Heart Rate Service"
            attributes["0000180a-0000-1000-8000-00805f9b34fb"] = "Device Information Service"
            // Sample Characteristics.
            attributes[HEART_RATE_MEASUREMENT] = "Heart Rate Measurement"
            attributes[CLIENT_CHARACTERISTIC_CONFIG] = "Manufacturer Name String"
        }
    }
}
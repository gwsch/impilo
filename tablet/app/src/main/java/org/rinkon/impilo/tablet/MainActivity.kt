package org.rinkon.impilo.tablet

import android.Manifest.permission.*
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.annotations.AfterPermissionGranted

const val REQUEST_CODE_LOCATION_AND_BLUETOOTH_PERMISSION: Int = 123

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bpmBtn: Button = findViewById(R.id.buttonBPM)
        bpmBtn.setOnClickListener{
            Toast.makeText(applicationContext,"BPM", Toast.LENGTH_SHORT).show()
        }

        val settingsBtn: Button = findViewById(R.id.buttonSettings)
        settingsBtn.setOnClickListener{
            //Toast.makeText(applicationContext, "Settings", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
        }

        val bluetoothScanBtn: Button = findViewById(R.id.buttonBluetoothScan)
        bluetoothScanBtn.setOnClickListener{
            //Toast.makeText(applicationContext, "BluetoothScan", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@MainActivity, BluetoothScanActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @AfterPermissionGranted(REQUEST_CODE_LOCATION_AND_BLUETOOTH_PERMISSION)
    private fun permissionRequest() {
        if (EasyPermissions.hasPermissions(this, ACCESS_COARSE_LOCATION, BLUETOOTH, BLUETOOTH_ADMIN)) {
            // Already have permission, do the thing
            // ...
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.permission_location_and_bluetooth_rationale_message),
                REQUEST_CODE_LOCATION_AND_BLUETOOTH_PERMISSION,
                ACCESS_COARSE_LOCATION, BLUETOOTH, BLUETOOTH_ADMIN)
        }
    }
}
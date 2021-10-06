package org.rinkon.impilo.tablet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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

        var bluetoothScanBtn: Button = findViewById(R.id.buttonBluetoothScan)
        bluetoothScanBtn.setOnClickListener{
            //Toast.makeText(applicationContext, "BluetoothScan", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@MainActivity, BluetoothScanActivity::class.java)
            startActivity(intent)
        }
    }
}
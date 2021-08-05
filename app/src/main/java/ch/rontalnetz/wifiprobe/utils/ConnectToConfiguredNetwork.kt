@file:Suppress("DEPRECATION")

package ch.rontalnetz.wifiprobe.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import ch.rontalnetz.wifiprobe.databinding.ActivityConnectToConfiguredNetworkBinding

/**
 * This activity is responsible to connect to a configured network (wifi)
 * For API less than Q only.
 * Used by: MainActivity.onContextItemSelected().
 */
class ConnectToConfiguredNetwork : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var binding: ActivityConnectToConfiguredNetworkBinding
    private val locationRequestCODE = 201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inits the binding
        binding = ActivityConnectToConfiguredNetworkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wifiManager =
            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val spLayoutId = resources.getIdentifier("spinner_special_item", "layout", packageName)
        val arrayAdapter = ArrayAdapter(this, spLayoutId, getConfiguredNetworks())
        binding.spConfiguredSsid.adapter = arrayAdapter

    }

    /** Button 'CONNECT' clicked handler. */
    @Suppress("UNUSED_PARAMETER")
    fun connectToConfiguredWifi(view: View) {

        try {
            val wifiConfigs: List<WifiConfiguration>
            val selectedNet = binding.spConfiguredSsid.selectedItem.toString()
            // Request 'Location' Permission at Runtime.
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // User request for 'Location' permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    locationRequestCODE
                )
                return
            } else {
                wifiConfigs = wifiManager.configuredNetworks
            }
            val wifiConfig =
                wifiConfigs.firstOrNull { it.SSID.replace("\"", "") == selectedNet }

            if (wifiConfig != null) {
                wifiManager.enableNetwork(wifiConfig.networkId, true)
            } else {
                val myToast =
                    Toast.makeText(
                        applicationContext,
                        "Can't connect to $selectedNet",
                        Toast.LENGTH_LONG
                    )
                myToast.setGravity(Gravity.CENTER, 0, 0)
                myToast.show()
            }
        } catch (ex: Exception) {
            val myToast =
                Toast.makeText(
                    applicationContext,
                    ex.message,
                    Toast.LENGTH_LONG
                )
            myToast.setGravity(Gravity.CENTER, 0, 0)
            myToast.show()
        }
        finish()
    }

    private fun getConfiguredNetworks(): List<String> {

        // Request 'Location' Permission at Runtime.
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // User request for 'Location' permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationRequestCODE
            )
            return listOf("Please try again.")
        } else {

            val wifiConfigs = wifiManager.configuredNetworks
            return (
                    wifiConfigs.mapNotNull {
                        if (it.status != WifiConfiguration.Status.ENABLED) null else it.SSID
                    }.sorted().distinct().map { it.replace("\"", "") }
                    )
        }
    }
}
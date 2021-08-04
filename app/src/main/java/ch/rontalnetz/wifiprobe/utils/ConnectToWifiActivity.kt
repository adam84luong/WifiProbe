package ch.rontalnetz.wifiprobe.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import ch.rontalnetz.wifiprobe.CONNECT_WIFI_SSID
import ch.rontalnetz.wifiprobe.databinding.ActivityConnectToWifiBinding


/**
 * This activity is responsible to connect a wifi with an explicit
 * entered ssid and password.
 * Used by: RecyclerAdapterWifiItems inner class WifiViewHolder (context menu),
 */
class ConnectToWifiActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var ssid: String
    private lateinit var pwd: String
    private lateinit var type: String
    private lateinit var binding: ActivityConnectToWifiBinding
    private val locationRequestCODE = 301

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inits the binding
        binding = ActivityConnectToWifiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wifiManager =
            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        ssid = intent.getStringExtra(CONNECT_WIFI_SSID)!!
        // Sets SSID EditText with selected item from RecyclerAdapterWifiItems
        binding.ssid.text = ssid.toEditable()
        binding.password.text = "".toEditable()

        // Set Security with RadioButton WPA, WEP and Open only for Android < Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.llSecurity.visibility = GONE
        }
    }

    /** Button 'CONNECT' clicked handler. */
    @Suppress("UNUSED_PARAMETER")
    fun connectToWifi(view: View) {
        ssid = binding.ssid.text.toString()
        pwd = binding.password.text.toString()
        val selectedId = binding.rgSecurity.checkedRadioButtonId
        type = findViewById<RadioButton>(selectedId).text.toString()

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
        }

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                connectWifi(ssid, type, pwd)
            } else {
                connectWifiGtEqQ(ssid, pwd)
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

    /** Connect to Wifi for API level < Q (api 29)*/
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun connectWifi(networkSSID: String, type: String, networkPass: String) {
        val conf = WifiConfiguration()
        conf.SSID = "\"" + networkSSID + "\"" // Please note the quotes. String
        // should contain ssid in quotes
        when (type) {
            "WEP" -> {
                conf.wepKeys[0] = "\"" + networkPass + "\""
                conf.wepTxKeyIndex = 0
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
            }
            "WPA" -> {
                conf.preSharedKey = "\"" + networkPass + "\""
            }
            "Open" -> {
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            }
        }

        wifiManager.addNetwork(conf)
        val list = wifiManager.configuredNetworks
        for (i in list) {
            if (i.SSID != null && i.SSID == "\"" + networkSSID + "\"") {
                wifiManager.enableNetwork(i.networkId, true)
                break
            }
        }
    }

    /** Connect to Wifi for API level >= Q (api 29)*/
    @SuppressLint("NewApi")
    fun connectWifiGtEqQ(ssid: String, pwd: String) {

        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        val suggestion1 = WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(pwd)
            .setIsAppInteractionRequired(true) // Optional (Needs location permission)
            .build()

        val suggestionsList: MutableList<WifiNetworkSuggestion> = ArrayList()
        suggestionsList.add(suggestion1)

        val status = wifiManager.addNetworkSuggestions(suggestionsList)

        if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            // todo Error handling
        }

        val intentFilter = IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)

        val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION) {
                    return
                }
                // Post connection
            }
        }
        applicationContext.registerReceiver(broadcastReceiver, intentFilter)
    }


    fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)
}

package ch.rontalnetz.wifiprobe.scan

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import ch.rontalnetz.wifiprobe.MainActivity
import ch.rontalnetz.wifiprobe.data.WifiData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.log10
import kotlin.math.pow


class Scanner(
    var wifiActive: WifiData,
    var wifiList: MutableList<WifiData>,
    context: Context
) {

    private var wifiManager: WifiManager
    private val ctext: Context = context
    private var pass: Int = 0

    /** Lookup table with MAC Addresses to Company names */
    private lateinit var macLookupTab: String

    init {

        /** Broadcast Receiver for Wifi scanner */
        val wifiScanReceiver = object : BroadcastReceiver() {

            override fun onReceive(contxt: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    scanSuccess()
                } else {
                    scanFailure()
                }
            }
        }

        wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.applicationContext.registerReceiver(wifiScanReceiver, intentFilter)

        loadMacLookupTable()
    }


    /** Load MAC-Addr-Company-Name file from resources into a member string var */
    private fun loadMacLookupTable() {

        val resID = ctext.resources.getIdentifier("mac_lookup_tab", "raw", ctext.packageName)
        val macStm: InputStream = ctext.resources.openRawResource(resID)

        // defaults to UTF-8, auto close stream.
        macLookupTab = macStm.bufferedReader().use {
            it.readText()
        }
    }

    /** Check if Wifi is on. Wifi must be on for scanning */
    fun isWifiOn(): Boolean {
        return wifiManager.isWifiEnabled
    }


    fun startScan(pass: Int) {
        this.pass = pass

        // TODO: Replace GlobalScope... with the Programming paradigm ViewModel and data.
        // TODO: The use of GlobalScope.launch is not a good idea!

        // Immediately starts and stops the wifi action panel, these trigger a wifi scan.
        // Replacement for the StartScan. There is no limit for number of scan per minute.
        GlobalScope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val intentAW = Intent(Settings.Panel.ACTION_WIFI)
                    startActivityForResult(ctext as Activity, intentAW, 4711, null)
                    delay((ctext as MainActivity).wifiPanelStayTime)
                    ctext.finishActivity(4711)
                } else {
                    val intentAWS = Intent(Settings.ACTION_WIFI_SETTINGS)
                    startActivityForResult(ctext as Activity, intentAWS, 4728, null)
                    delay((ctext as MainActivity).wifiPanelStayTime)
                    ctext.finishActivity(4728)
                }
            } catch (ex: Exception) {
            }
        }
    }


    /** Callback routine when wifi scan successfully */
    fun scanSuccess() {
        // Code Part of Scan results
        val tempWifiList: MutableList<WifiData> = mutableListOf()

        val results = wifiManager.scanResults
        // Fill up the temp list with Wifi scan result
        for (i in 0 until results.size) {
            val wifi = WifiData(
                results[i].SSID ?: "?",
                results[i].BSSID ?: "?",
                results[i].level,
                results[i].operatorFriendlyName.toString(),
                "%.3f".format((results[i].frequency) / 1000.toDouble()),
                results[i].channelWidth.toString(),
                calculateDistance(
                    results[i].level.toDouble(), results[i].frequency.toDouble()
                ),
                findCompanyNameViaMacAddr(results[i].BSSID),
                results[i].capabilities ?: "?"
            )
            tempWifiList.add(wifi)
        }
        // Compares the wifi objects actually scanned with the existing wifi objects and
        // updates the existing wifi objects accordingly.
        tempWifiList.forEach { tmpItem ->
            // 1st Check if Entry with same MAC addr. exists
            val item = wifiList.firstOrNull { it.mac == tmpItem.mac }
            if (item != null) {
                // 2nd Check if any changes of the actual scanned and exists wifi object
                if (tmpItem != item) {
                    wifiList.remove(item)
                    wifiList.add(tmpItem)
                }
            } else {
                // Scanned wifi object doesn't exists, add it to the RecyclerAdapter List
                wifiList.add(tmpItem)
            }
        }

        wifiList.sortBy { it.rssi.absoluteValue }

        // Code Part of the state of any Wi-Fi connection that is active.
        val conInfo = wifiManager.connectionInfo as WifiInfo
        // gets correspond wifi data from scan results.
        val connWifi = wifiList.firstOrNull { it.mac == conInfo.bssid }
        if (connWifi != null) {
            wifiActive.ssid = connWifi.ssid
            wifiActive.mac = connWifi.mac
            wifiActive.rssi = connWifi.rssi
            wifiActive.name = connWifi.name
            wifiActive.freq = connWifi.freq
            wifiActive.chan = connWifi.chan
            wifiActive.dist = connWifi.dist
            wifiActive.router = connWifi.router
            wifiActive.security = connWifi.security
            // Link speed in Mbps. Local wifi network speed only
            wifiActive.linkSpeedMbps = conInfo.linkSpeed.toString()

            val cm = ctext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            wifiActive.ipAddress = "?"
            val netId = cm.activeNetwork
            val lp = cm.getLinkProperties(netId)
            if (lp != null) {
                // Returns all the LinkAddress on this link. Typically a link will have one IPv4
                // address and one or more IPv6 addresses.
                val la = lp.linkAddresses
                // Get ipv4 address
                val ipAddr = la.firstOrNull { it.prefixLength == 24 }
                if (ipAddr != null) {
                    wifiActive.ipAddress = ipAddr.address.hostAddress
                }
            }
        }

        (ctext as MainActivity).notifyWifiDataUpdated()
        if (this.pass == 2) {
            ctext.stopProgressBar()
        }
    }

    fun scanFailure() {
        // Handle failure, only message to user.
        (ctext as MainActivity).stopProgressBar()
        val myToast =
            Toast.makeText(ctext.applicationContext, "Wifi Scan Error.", Toast.LENGTH_LONG)
        myToast.setGravity(Gravity.CENTER, 0, 0)
        myToast.show()
    }


    /** Calculate router distance.
     * @param signalLevelInDb Wifi Level in dBm.
     * @param freqInMHz The primary 20 MHz frequency (in MHz), from scanResults.frequency */
    private fun calculateDistance(signalLevelInDb: Double, freqInMHz: Double): String {
        val exp = (27.55 - 20 * log10(freqInMHz) + abs(signalLevelInDb)) / 20.0
        val num = 10.0.pow(exp)
        return "%.2f".format(num)
    }


    /** Returns a company name for a input MAC Address */
    @Suppress("RegExpRedundantEscape")
    private fun findCompanyNameViaMacAddr(macAddr: String): String {

        var retValue = "?"
        val macOUI = macAddr.replace(".{9}$".toRegex(), "").replace("[:-]".toRegex(), "")
        val regx =
            """$macOUI\{(.*)\}""".toRegex(
                setOf(
                    RegexOption.IGNORE_CASE,
                    RegexOption.MULTILINE
                )
            )

        val matches = regx.find(this.macLookupTab)
        if (matches != null) {
            val (comp) = matches.destructured
            retValue = comp
        }
        return retValue
    }
}
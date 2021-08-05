package ch.rontalnetz.wifiprobe

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ch.rontalnetz.wifiprobe.data.WifiData
import ch.rontalnetz.wifiprobe.databinding.ActivityMainBinding
import ch.rontalnetz.wifiprobe.scan.Scanner
import ch.rontalnetz.wifiprobe.utils.*
import java.util.*


const val CONNECT_WIFI_SSID = "connect_wifi_ssid"

/** Constant 0 = No pass 2 execute */
const val NO_PASS2_EXECUTE = 0

/** Constant 5 = Number of repeats for the 'Loop Scan' */
const val NUMBER_OF_REPEATS = 5

/** Constant 5 = Delay between the repeats (Loop Scan) */
const val DELAY_BETWEEN_REPEATS = 5

/** Constant 300 = 300 ms Wifi panel display time (on start scan) */
const val WIFI_PANEL_DISPLAY_TIME = 300

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: RecyclerView.Adapter<RecyclerAdapterWifiItems.WifiViewHolder>? = null
    val wifiActiveLiveData = MutableLiveData<WifiData>()

    private val locationRequestCODE = 101

    companion object {
        /** Static variable for wifi data */
        var wifiList: MutableList<WifiData> = mutableListOf()

        /** Static variable for of any Wi-Fi connection that is active */
        var wifiActive: WifiData = WifiData("", "", 0, "", "", "", "", "", "")
    }

    private lateinit var scanner: Scanner
    private val _isListenerRegistered = "isListenerRegistered"

    // Vars for preferences ----------------------------------------------------------------
    /** SharedPreferences instance that points to the default file. */
    private lateinit var sharedPrefs: SharedPreferences
    private var changedListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var isListenerRegistered: Boolean = false

    /** Delay in milliseconds until pass 2 starts. 0 = No pass 2 execute */
    private var delayPass2: Long = NO_PASS2_EXECUTE.toLong()

    /** Timer for 2nd pass with the possibility to cancel the timer */
    private var timerPass2: Timer = Timer()

    /** Timer for 'Loop scan' with the possibility to cancel the timer */
    private var delayNextCall: Timer = Timer()

    private var numberOfRepeatScan: Int = NUMBER_OF_REPEATS
    private var delayBetweenRepeats: Int = DELAY_BETWEEN_REPEATS
    private var counterRepeats: Int = 0
    private var _counterRepeats = "counterRepeats"
    private var isRepeatScan = false
    private val _isRepeatScan = "isRepeatScan"

    /** Wifi panel display time (on start scan) */
    private var wifiPanelDisplayTime: Long = WIFI_PANEL_DISPLAY_TIME.toLong()
    val wifiPanelStayTime: Long
        get() {
            return wifiPanelDisplayTime
        }

    /** Day/Night/System default Modes */
    private var dayNightMode: String = "MODE_NIGHT_YES"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Gets a SharedPreferences instance.
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        loadPreferences()

        // Stop of the progress bar.
        binding.progBarScanning.setOnClickListener {
            // Stop Repeat Scan
            counterRepeats = 0
            // Stop progress bar and give start button free
            stopProgressBar()
        }

        binding.bnRepeatScan.setOnClickListener {
            if (!isRepeatScan) {
                counterRepeats = numberOfRepeatScan
                binding.tvRepeatScanTitle.text =
                    (getString(R.string.repeat_loop_n) + String.format("%02d", counterRepeats))
                wifiList.clear()
                isRepeatScan = true
                // Triggers 'Start Scan'
                binding.bnStartScan.callOnClick()
                binding.bnRepeatScan.disable()
            }
        }

        scanner = Scanner(wifiActive, wifiList, this)
        /** Click listener for ImageButton to start scan */
        binding.bnStartScan.setOnClickListener {
            if (!isWifiSwitchedOn(scanner)) {
                return@setOnClickListener
            }

            var pass = 1
            binding.bnStartScan.disable()
            // If two pass is set and non Looping
            if (delayPass2 > NO_PASS2_EXECUTE && !isRepeatScan) {
                binding.tvPassTitle.text = getString(R.string.scan_pass_12)
            } else {
                if (isRepeatScan) {
                    binding.tvPassTitle.text = getString(R.string.loop_scanning)
                } else {
                    binding.tvPassTitle.text = getString(R.string.scan_pass_1)
                }
                // Termination condition used by the scanner class.
                pass = 2
            }
            if (!isRepeatScan) {
                binding.bnRepeatScan.disable()
                wifiList.clear()
            }
            binding.progBarScanning.visibility = View.VISIBLE
            notifyWifiDataUpdated()
            // 1st pass of scan
            scanner.startScan(pass)

            // 2nd pass of scan after xxx sec., set in prefs.
            // 2nd pass only if no 'Loop Scan'.
            if (!isRepeatScan) {
                if (delayPass2 > NO_PASS2_EXECUTE) {
                    timerPass2 = Timer()
                    timerPass2.schedule(object : TimerTask() {
                        override fun run() {
                            runOnUiThread {
                                binding.tvPassTitle.text = getString(R.string.scan_pass_2)
                            }

                            scanner.startScan(++pass)
                        }
                    }, delayPass2)
                }
            }

            val scanInfo = Toast.makeText(
                applicationContext,
                getString(R.string.scanning), Toast.LENGTH_LONG
            )
            scanInfo.show()
        }

        binding.tvWifiIsOffTitle.setOnClickListener {
            binding.tvWifiIsOffTitle.text = ""
        }

        // Initialize the RecyclerView with a layout manager, create an instance of the adapter
        // and assign that instance to the RecyclerView object
        layoutManager = LinearLayoutManager(this)
        binding.includeRecview.rvWifiItems.layoutManager = layoutManager
        //
        adapter = RecyclerAdapterWifiItems(wifiList, this)
        binding.includeRecview.rvWifiItems.adapter = adapter

        wifiActiveLiveData.observe(this, {
            wifiActiveLiveDataObserve(it)
        })

        /** Context menu for current active wifi connection */
        binding.constlayoutWifiactive.setOnCreateContextMenuListener { menu, _, _ ->
            val inflater: MenuInflater = menuInflater
            inflater.inflate(R.menu.context_wifi_current_active, menu)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                menu.findItem(R.id.connect_configured_wifi).isVisible = false
            }
        }

        // Handles a simple click on the view 'active wifi details'.
        binding.constlayoutWifiactive.setOnClickListener {
            showWifiDetails()
        }
        setDayNight()

        setupPermissions()
    }

    override fun onResume() {
        super.onResume()
        loadPreferences()
        // Update UI for the active Wifi.
        wifiActiveLiveData.postValue(wifiActive)

        // Register a preference changed listener. Use of a field to prevent
        // unintended garbage collection. And prevents multiple registering with a flag.
        if (!isListenerRegistered) {
            changedListener =
                SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences, key: String ->
                    when (key) {
                        "day_night_mode" -> {
                            dayNightMode =
                                sharedPrefs.getString("day_night_mode", "MODE_NIGHT_YES")!!
                                    .toString()
                            setDayNight()
                        }
                        // "delay_msec_pass_2" not necessary, loadPreferences() new in onResume.
                    }
                }
            sharedPrefs.registerOnSharedPreferenceChangeListener(changedListener)
            isListenerRegistered = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putBoolean(_isListenerRegistered, isListenerRegistered)
            // Not necessary save wifiList cause static var.
            putBoolean(_isRepeatScan, isRepeatScan)
            putInt(_counterRepeats, counterRepeats)

            super.onSaveInstanceState(outState)
        }
    }

    override fun onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)
        state.run {
            isListenerRegistered = getBoolean(_isListenerRegistered)
            isRepeatScan = getBoolean(_isRepeatScan)
            counterRepeats = getInt(_counterRepeats, NUMBER_OF_REPEATS)
        }
    }


    /** Notify Wifi Data Changed for the RecyclerView Adapter*/
    fun notifyWifiDataUpdated() {
        adapter!!.notifyDataSetChanged()
        // LiveData for UI the Wi-Fi connection that is active.
        wifiActiveLiveData.postValue(wifiActive)
    }

    /** Called if fun scanSuccess() finished (Callback wifi scan successfully) */
    fun stopProgressBar() {
        if (isRepeatScan) {
            delayNextCall = Timer()
            if (--counterRepeats > 0) {
                delayNextCall.schedule(object : TimerTask() {
                    override fun run() {
                        // if... in the case that 'Repeating' has already finished
                        if (isRepeatScan) {
                            runOnUiThread {
                                binding.tvRepeatScanTitle.text =
                                    (getString(R.string.repeat_loop_n) + String.format(
                                        "%02d",
                                        counterRepeats
                                    ))

                                binding.bnStartScan.callOnClick()
                            }
                        }
                    }
                }, delayBetweenRepeats.toLong())
            } else {
                // Repeating is finished
                isRepeatScan = false
                delayNextCall.cancel()
                binding.progBarScanning.visibility = View.INVISIBLE
                binding.tvPassTitle.text = ""
                binding.bnStartScan.enable()
                binding.bnRepeatScan.enable()
                binding.tvRepeatScanTitle.text = getString(R.string.loop_scan)
            }
        } else {
            // No Repeat Scan
            timerPass2.cancel()
            binding.progBarScanning.visibility = View.INVISIBLE
            binding.tvPassTitle.text = ""
            binding.bnStartScan.enable()
            binding.bnRepeatScan.enable()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                // See res/layout/settings_activity.xml as container and the settings
                // points in res/xml/root_preferences.xml
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.wifi_details -> {
                showWifiDetails()
                true
            }
            R.id.connect_configured_wifi -> {
                // Connect to a list of configured network
                val intent = Intent(this, ConnectToConfiguredNetwork::class.java)
                startActivity(intent)
                true
            }
            R.id.panel_wifi -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val intentAW = Intent(Settings.Panel.ACTION_WIFI)
                    startActivity(intentAW)
                } else {
                    val intentAWS = Intent(Settings.ACTION_WIFI_SETTINGS)
                    startActivity(intentAWS)
                }
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    /** Check if Wifi is on, if not starts action_wifi, user can switch on manually */
    private fun isWifiSwitchedOn(scanner: Scanner): Boolean {

        if (!scanner.isWifiOn()) {
            binding.tvWifiIsOffTitle.text = getString(R.string.wifi_is_disabled)
            val myToast =
                Toast.makeText(
                    applicationContext, getString(R.string.wifi_is_disabled), Toast.LENGTH_LONG
                )
            myToast.setGravity(Gravity.TOP, 0, 0)
            myToast.show()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intentAW = Intent(Settings.Panel.ACTION_WIFI)
                ActivityCompat.startActivity(this, intentAW, null)
            } else {
                val intentAWS = Intent(Settings.ACTION_WIFI_SETTINGS)
                ActivityCompat.startActivityForResult(this, intentAWS, 4748, null)
            }
            return false
        }
        binding.tvWifiIsOffTitle.text = ""
        return true
    }

    private fun ImageButton.disable() {
        alpha = 0.5f
        isEnabled = false
    }

    private fun ImageButton.enable() {
        alpha = 1f
        isEnabled = true
    }

    private fun loadPreferences() {

        delayPass2 =
            sharedPrefs.getString("delay_msec_pass_2", NO_PASS2_EXECUTE.toString())!!.toLong()
        numberOfRepeatScan =
            sharedPrefs.getInt("number_of_repeats", NUMBER_OF_REPEATS)
        delayBetweenRepeats =
            sharedPrefs.getInt("delay_between_repeats", DELAY_BETWEEN_REPEATS)
        wifiPanelDisplayTime =
            sharedPrefs.getString("wifi_panel_scan_stay_time", WIFI_PANEL_DISPLAY_TIME.toString())!!
                .toLong()

        dayNightMode = sharedPrefs.getString("day_night_mode", "MODE_NIGHT_YES")!!.toString()
    }

    private fun setDayNight() {
        when (dayNightMode) {
            "MODE_NIGHT_YES" ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "MODE_NIGHT_NO" ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun wifiActiveLiveDataObserve(wifiData: WifiData) {
        binding.wifiactiveItem.tvSsid.text = wifiData.ssid
        binding.wifiactiveItem.tvMacAddr.text = wifiData.mac
        (wifiData.rssi.toString() + getString(R.string.dBmilliwatt)).also {
            binding.wifiactiveItem.tvNumLeveldB.text = it
        }
        binding.wifiactiveItem.tvNumLeveldB.setTextColor(
            getColoredStrength(
                applicationContext,
                wifiData.rssi
            )
        )
        binding.wifiactiveItem.tvRouter.text = wifiData.router
        ("SSID  (" + wifiData.freq + " GHz)").also { binding.wifiactiveItem.tvSsidTitle.text = it }

        var padLock = ContextCompat.getDrawable(this, R.drawable.ic_baseline_lock_24)
        if (!wifiData.security.matches(""".*\bW[EP][PA]\d?\b.*""".toRegex(RegexOption.IGNORE_CASE))) {
            padLock = ContextCompat.getDrawable(this, R.drawable.ic_baseline_lock_open_24)
        }
        binding.wifiactiveItem.ivPadLock.setImageDrawable(padLock)
        // Display inverted dBm value, range of prg.Bar 0-120.
        binding.wifiactiveItem.progressBarLevelDB.progress = 120 + wifiData.rssi
        // Sets progress bar's color due to the wifi strength
        binding.wifiactiveItem.progressBarLevelDB.progressTintList =
            getColoredStrength(applicationContext, wifiData.rssi)
    }

    /** Show Wifi Details of  Wi-Fi connection that is active */
    private fun showWifiDetails() {

        val dialog = WifiDetailDialog()
        val bundle = Bundle()
        bundle.putString("level", wifiActive.rssi.toString())
        bundle.putString("mac", wifiActive.mac)
        bundle.putString("ssid", wifiActive.ssid)
        bundle.putString("router", wifiActive.router)
        bundle.putString("rxspeed", wifiActive.linkSpeedMbps)
        bundle.putString("ipaddr", wifiActive.ipAddress)
        bundle.putString("freq", wifiActive.freq)
        bundle.putString("chwidth", wifiActive.chan)
        bundle.putString("security", wifiActive.security)
        bundle.putString("dist", wifiActive.dist)
        bundle.putString("name", wifiActive.name)

        dialog.arguments = bundle
        dialog.show(supportFragmentManager, "WifiDetailDialog")
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCloseDialogWifiDetails(view: View) {
        val fragment: Fragment? = supportFragmentManager.findFragmentByTag("WifiDetailDialog")
        if (fragment != null) {
            val dialog = fragment as DialogFragment
            dialog.dismiss()
        }
    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            locationRequestCODE
        )
    }
}

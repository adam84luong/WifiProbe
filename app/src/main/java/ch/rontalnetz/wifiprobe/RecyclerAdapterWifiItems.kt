package ch.rontalnetz.wifiprobe

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import ch.rontalnetz.wifiprobe.data.WifiData
import ch.rontalnetz.wifiprobe.databinding.WifiItemLayoutBinding
import ch.rontalnetz.wifiprobe.utils.ConnectToConfiguredNetwork
import ch.rontalnetz.wifiprobe.utils.ConnectToWifiActivity
import ch.rontalnetz.wifiprobe.utils.WifiDetailDialog
import ch.rontalnetz.wifiprobe.utils.getColoredStrength


class RecyclerAdapterWifiItems(val wifiItems: MutableList<WifiData>, val context: Context) :
    RecyclerView.Adapter<RecyclerAdapterWifiItems.WifiViewHolder>() {

    // 1. Gets the number of wifi items in the MutableList.
    override fun getItemCount(): Int {
        return wifiItems.size
    }


    /** 2. This method will be called by the RecyclerView to obtain a ViewHolder object.
     *     Inflate the wifi items view */
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): WifiViewHolder {

        // Class WifiItemLayoutBinding is generated class
        val itemBinding =
            WifiItemLayoutBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return WifiViewHolder(itemBinding)
    }

    // Binds each wifi item to a View
    override fun onBindViewHolder(viewHolder: WifiViewHolder, i: Int) {

        val item = wifiItems[i]
        (item.rssi.toString() + context.getString(R.string.dBmilliwatt)).also {
            viewHolder.viewBinding.tvNumLeveldB.text = it
        }
        viewHolder.viewBinding.tvNumLeveldB.setTextColor(
            getColoredStrength(
                context,
                item.rssi
            )
        )
        // Display inverted dBm value, range of prg.Bar 0-120.
        viewHolder.viewBinding.progressBarLevelDB.progress = 120 + item.rssi
        viewHolder.viewBinding.tvMacAddr.text = item.mac
        ("SSID  (" + item.freq + " GHz)").also { viewHolder.viewBinding.tvSsidTitle.text = it }
        viewHolder.viewBinding.tvSsid.text = if (item.ssid == "") "?" else item.ssid
        viewHolder.viewBinding.tvRouter.text = item.router

        var padLock = getDrawable(context, R.drawable.ic_baseline_lock_24)
        if (!item.security.matches(""".*\bW[EP][PA]\d?\b.*""".toRegex(RegexOption.IGNORE_CASE))) {
            padLock = getDrawable(context, R.drawable.ic_baseline_lock_open_24)
        }
        viewHolder.viewBinding.ivPadLock.setImageDrawable(padLock)
        // Sets progress bar's color due to the wifi strength
        viewHolder.viewBinding.progressBarLevelDB.progressTintList =
            getColoredStrength(context, item.rssi)
    }


    inner class WifiViewHolder(var viewBinding: WifiItemLayoutBinding) :
        RecyclerView.ViewHolder(viewBinding.root), View.OnCreateContextMenuListener,
        View.OnClickListener {

        init {
            viewBinding.root.setOnCreateContextMenuListener(this)
            viewBinding.root.setOnClickListener(this)
        }

        override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            val popupMenu = PopupMenu(context, v!!, Gravity.START)
            popupMenu.menuInflater.inflate(R.menu.context_wifi_items, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.wifi_details -> {
                        showWifiDetails(layoutPosition)
                    }
                    R.id.connect_configured_wifi -> {
                        // Call explicit intent with selected item SSID.
                        val intent = Intent(context, ConnectToConfiguredNetwork::class.java)
                        startActivity(context, intent, null)
                    }
                    R.id.connect_explicit_ssid -> {
                        // Call explicit intent with entering ssid and pwd
                        val intent = Intent(context, ConnectToWifiActivity::class.java).apply {
                            putExtra(CONNECT_WIFI_SSID, viewBinding.tvSsid.text)
                        }
                        startActivity(context, intent, null)
                    }
                }
                true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                popupMenu.menu.removeItem(R.id.connect_configured_wifi)
            }
            popupMenu.show()
        }

        // Handler for a simple click on an wifi detail item.
        override fun onClick(v: View?) {
            showWifiDetails(layoutPosition)
        }

        private fun showWifiDetails(pos: Int) {

            val dialog = WifiDetailDialog()
            val bundle = Bundle()
            bundle.putString("level", wifiItems[pos].rssi.toString())
            bundle.putString("mac", wifiItems[pos].mac)
            bundle.putString("ssid", wifiItems[pos].ssid)
            bundle.putString("router", wifiItems[pos].router)
            bundle.putString("rxspeed", "N/A")
            bundle.putString("ipaddr", "N/A")
            bundle.putString("freq", wifiItems[pos].freq)
            bundle.putString("chwidth", wifiItems[pos].chan)
            bundle.putString("security", wifiItems[pos].security)
            bundle.putString("dist", wifiItems[pos].dist)
            bundle.putString("name", wifiItems[pos].name)

            dialog.arguments = bundle
            val sfm = (context as AppCompatActivity).supportFragmentManager
            dialog.show(sfm, "WifiDetailDialog")
        }
    }
}
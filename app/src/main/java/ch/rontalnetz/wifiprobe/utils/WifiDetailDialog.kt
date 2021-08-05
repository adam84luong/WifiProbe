package ch.rontalnetz.wifiprobe.utils

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import ch.rontalnetz.wifiprobe.R
import ch.rontalnetz.wifiprobe.databinding.DialogWifiDetailBinding


class WifiDetailDialog : DialogFragment() {

    private lateinit var binding: DialogWifiDetailBinding

    /** Signal level in \[dBm].
     * dBm (decibel-milliwatts) is used to indicate that a power level is expressed in
     * decibels (dB) with reference to one milliwatt (mW). */
    private var level: String? = null

    /** MAC Address (Media-Access-Control-Address) */
    private var mac: String? = null

    /** SSID Name (Service Set Identifier). */
    private var ssid: String? = null

    /** Router or repeater name. */
    private var router: String? = null

    /** The current receive link speed in Mbps. */
    private var rxspeed: String? = null

    /** IP Address of current connected wifi. */
    private var ipaddr: String? = null

    /** The frequency (displayed in GHz) which the client is communicating with the access point. */
    private var freq: String? = null

    /** Channel bandwidth */
    private var chwidth: String? = null

    /** Security capabilities. */
    private var security: String? = null

    /** Distance to the WiFi access point (approximate). */
    private var dist: String? = null

    /** Operator friendly name. */
    private var name: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        if (arguments != null) {
            val mArgs = arguments
            level = mArgs?.getString("level", "?")
            mac = mArgs?.getString("mac", "?")
            ssid = mArgs?.getString("ssid", "?")
            router = mArgs?.getString("router", "?")
            rxspeed = mArgs?.getString("rxspeed", "?")
            ipaddr = mArgs?.getString("ipaddr", "?")
            freq = mArgs?.getString("freq", "?")
            chwidth = mArgs?.getString("chwidth", "?")
            security = mArgs?.getString("security", "?")
            dist = mArgs?.getString("dist", "?")
            name = mArgs?.getString("name", "?")
        }
        binding = DialogWifiDetailBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set wifi details into UI Views
        binding.tvNumLeveldB.text = level
        binding.tvMacAddr.text = mac
        binding.tvSsid.text = ssid
        binding.tvRouter.text = router
        binding.tvRxSpeed.text = rxspeed
        binding.tvIpAddr.text = ipaddr
        binding.tvFreq.text = freq
        binding.tvChWidth.text = chwidth
        binding.tvSecurity.text = security
        binding.tvDist.text = dist
        binding.tvName.text = name

        // Coloring level according to a specific wifi level dBm
        binding.tvNumLeveldB.setTextColor(getColoredStrength(requireContext(), level!!.toInt()))
        binding.tvWifidBmTitle.setTextColor(getColoredStrength(requireContext(), level!!.toInt()))

        // Set Tooltip according to wifi level dBm
        binding.tvNumLeveldB.tooltipText = getNotesToStrength(requireContext(), level!!.toInt())
        binding.tvWifidBmTitle.tooltipText = getNotesToStrength(requireContext(), level!!.toInt())

        if (binding.tvIpAddr.text == "N/A" && binding.tvRxSpeed.text == "N/A") {
            val colorDis = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.fg_dialog_wifi_detail_disabled
                )
            )
            binding.tvIpAddrTitle.setTextColor(colorDis)
            binding.tvIpAddr.setTextColor(colorDis)
            binding.tvLinkSpeedTitle.setTextColor(colorDis)
            binding.tvRxSpeed.setTextColor(colorDis)
        }
    }
}
package ch.rontalnetz.wifiprobe.data

import android.os.Parcel
import android.os.Parcelable

/** Class encapsulate data of a wifi access point. */
data class WifiData(
    /** SSID Name (Service Set Identifier). */
    var ssid: String,
    /** MAC address (Media-Access-Control-Address). */
    var mac: String,
    /** Signal level in \[dBm].
     * dBm (decibel-milliwatts) is used to indicate that a power level is expressed in
     * decibels (dB) with reference to one milliwatt (mW). */
    var rssi: Int,
    /** Operator friendly name. */
    var name: String,
    /** The frequency (displayed in GHz) which the client is communicating with the access point. */
    var freq: String,
    /** Channel bandwidth */
    var chan: String,
    /** Distance to the WiFi access point (approximate). */
    var dist: String,
    /** Router or repeater name. */
    var router: String,
    /** Security capabilities. */
    var security: String,
) : Parcelable {

    // Specific props for active connected wifi.
    /** The current receive link speed in Mbps. */
    var linkSpeedMbps: String = ""

    /** IP Address of current connected wifi. */
    var ipAddress: String = ""

    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readInt(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString()
    ) {
        linkSpeedMbps = parcel.readString().toString()
        ipAddress = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(ssid)
        parcel.writeString(mac)
        parcel.writeInt(rssi)
        parcel.writeString(name)
        parcel.writeString(freq)
        parcel.writeString(chan)
        parcel.writeString(dist)
        parcel.writeString(router)
        parcel.writeString(security)
        parcel.writeString(linkSpeedMbps)
        parcel.writeString(ipAddress)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WifiData> {
        override fun createFromParcel(parcel: Parcel): WifiData {
            return WifiData(parcel)
        }

        override fun newArray(size: Int): Array<WifiData?> {
            return arrayOfNulls(size)
        }
    }

}
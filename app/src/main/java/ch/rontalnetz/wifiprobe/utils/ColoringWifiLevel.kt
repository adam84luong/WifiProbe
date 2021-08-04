package ch.rontalnetz.wifiprobe.utils

import android.content.Context
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat.getColor
import ch.rontalnetz.wifiprobe.R

/**
 * Gets a color according to a specific wifi level.
 * @param rssi Signal level in \[dBm] (decibel-milliwatts).
 * @return The ColorStateList for the input Wifi strength.
 */
fun getColoredStrength(context: Context, rssi: Int): ColorStateList {

    return when {

        // -30 dBm Maximum signal strength.
        rssi >= -30 -> ColorStateList.valueOf(getColor(context, R.color.wifi_level_30))

        // -50 dBm Anything down to this level can be considered excellent signal strength.
        rssi < -30 && rssi >= -50 -> ColorStateList.valueOf(
            getColor(
                context,
                R.color.wifi_level_50
            )
        )

        // -60 dBm Good, reliable signal strength.
        rssi < -50 && rssi >= -60 -> ColorStateList.valueOf(
            getColor(
                context,
                R.color.wifi_level_60
            )
        )

        // -67 dBm Reliable signal strength.
        rssi < -60 && rssi >= -67 -> ColorStateList.valueOf(
            getColor(
                context,
                R.color.wifi_level_67
            )
        )

        // -70 dBm Not a strong signal. Light browsing and email.
        rssi < -67 && rssi >= -70 -> ColorStateList.valueOf(
            getColor(
                context,
                R.color.wifi_level_70
            )
        )

        // -80 dBm Unreliable signal strength, will not suffice for most services.
        rssi < -70 && rssi >= -80 -> ColorStateList.valueOf(
            getColor(
                context,
                R.color.wifi_level_80
            )
        )

        // -90 dBm The chances of even connecting are very low at this level.
        rssi < -80 && rssi >= -90 -> ColorStateList.valueOf(
            getColor(
                context,
                R.color.wifi_level_90
            )
        )

        else -> ColorStateList.valueOf(getColor(context, R.color.wifi_level_91))
    }
}

/**
 * Gets a textual notes according to a specific wifi level strength
 * @param rssi Signal level in \[dBm] (decibel-milliwatts).
 * @return the notes about strength for the input Wifi level.
 */
fun getNotesToStrength(context: Context, rssi: Int): String {

    return when {

        // -30 dBm Maximum signal strength.
        rssi >= -30 -> context.getString(R.string.level_30)

        // -50 dBm Anything down to this level can be considered excellent signal strength.
        rssi < -30 && rssi >= -50 -> context.getString(R.string.level_50)

        // -60 dBm Good, reliable signal strength.
        rssi < -50 && rssi >= -60 -> context.getString(R.string.level_60)

        // -67 dBm Reliable signal strength.
        rssi < -60 && rssi >= -67 -> context.getString(R.string.level_67)

        // -70 dBm Not a strong signal. Light browsing and email.
        rssi < -67 && rssi >= -70 -> context.getString(R.string.level_70)

        // -80 dBm Unreliable signal strength, will not suffice for most services.
        rssi < -70 && rssi >= -80 -> context.getString(R.string.level_80)

        // -90 dBm The chances of even connecting are very low at this level.
        rssi < -80 && rssi >= -90 -> context.getString(R.string.level_90)

        else -> context.getString(R.string.level_91)
    }

}
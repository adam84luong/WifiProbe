package ch.rontalnetz.wifiprobe

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference

open class SettingsActivity : AppCompatActivity() {

    // See res/layout/settings_activity.xml as container and the settings
    // points in res/xml/root_preferences.xml

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickBackButton(view: View) {
        onBackPressed()
    }

    /** The root_preferences, settings layout */
    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            // Set a minimum value 5 for SeekBarPreference 'Loop Scan: Number of repeats'
            val numRep: SeekBarPreference? = findPreference("number_of_repeats") as SeekBarPreference?
            if (numRep != null) {
                numRep.min = 5
            }
            // Set a minimum value 5 for SeekBarPreference 'Delay [sec] between repeats'
            val delayLoop: SeekBarPreference? = findPreference("delay_between_repeats") as SeekBarPreference?
            if (delayLoop != null) {
                delayLoop.min = 5
            }
        }
    }


}
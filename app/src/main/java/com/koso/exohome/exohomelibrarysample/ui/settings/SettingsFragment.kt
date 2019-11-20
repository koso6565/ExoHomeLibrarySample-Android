package com.koso.exohome.exohomelibrarysample.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.koso.exohome.exohomelibrarysample.R
import com.koso.exohome.exohomelibrarysample.utils.SharedPrefHandler

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resetButton = findPreference<Preference>("reset")
        resetButton?.setOnPreferenceClickListener {
            AlertDialog.Builder(context)
                .setMessage(R.string.reset_warning)
                .setPositiveButton(R.string.reset_app) { _, _ ->
                    SharedPrefHandler.clear()
                    Toast.makeText(context, "Data reset completed", Toast.LENGTH_SHORT).show()
                }.create().show()
            true
        }


    }
}
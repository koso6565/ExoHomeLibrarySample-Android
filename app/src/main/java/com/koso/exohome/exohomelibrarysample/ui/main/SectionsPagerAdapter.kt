package com.koso.exohome.exohomelibrarysample.ui.main

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

private val TAB_TITLES = arrayOf(
    "Phone",
    "Device"
)

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm) {

    @ExperimentalStdlibApi
    override fun getItem(position: Int): Fragment {
        when(position){
            0 -> return LoginFragment.newInstance()
            1 -> return ConnectFragment.newInstance()
        }
        return ConnectFragment.newInstance()
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return TAB_TITLES[position]
    }

    override fun getCount(): Int {
        // Show 2 total pages.
        return 2
    }
}
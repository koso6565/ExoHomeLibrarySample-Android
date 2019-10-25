package com.koso.exohome.exohomelibrarysample.ui.main

import android.content.Context
import android.view.MenuItem
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
@ExperimentalStdlibApi
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm) {
    val fragments = listOf<Fragment>(LoginFragment.newInstance(), ConnectFragment.newInstance())

    @ExperimentalStdlibApi
    override fun getItem(position: Int): Fragment {

        return fragments[position]
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return TAB_TITLES[position]
    }

    override fun getCount(): Int {
        // Show 2 total pages.
        return 2
    }

    fun onOptionSelected(item: MenuItem){
        getItem(0).onOptionsItemSelected(item)
        getItem(1).onOptionsItemSelected(item)
    }
}
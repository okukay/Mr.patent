package com.ssafy.mr_patent_android.ui.patent

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewContentAdapter(
    private val fragmentList: ArrayList<Fragment>
    , container : AppCompatActivity
) : FragmentStateAdapter(container.supportFragmentManager, container.lifecycle) {
    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }
}
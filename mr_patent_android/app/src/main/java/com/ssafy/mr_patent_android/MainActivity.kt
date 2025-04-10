package com.ssafy.mr_patent_android

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.ssafy.mr_patent_android.base.BaseActivity
import com.ssafy.mr_patent_android.databinding.ActivityMainBinding
import com.ssafy.mr_patent_android.ui.home.HomeFragmentDirections
import com.ssafy.mr_patent_android.util.ConnectionStateMonitor
import com.ssafy.mr_patent_android.ui.patent.PatentFragmentDirections
import com.ssafy.mr_patent_android.ui.patent.SimiliarityTestViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val TAG = "MainActivity_Mr_Patent"
class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate)  {
    val viewModel: MainViewModel by viewModels()
    private val tabOrder = listOf(
        R.id.nav_fragment_home,
        R.id.nav_fragment_patent,
        R.id.nav_fragment_study,
        R.id.nav_fragment_chat,
        R.id.nav_fragment_mypage
    )

    private var currentTabId = R.id.nav_fragment_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initNavigation()
        initConnection()
        viewModel.getMemberInfo()
    }

    fun initConnection(){
        ConnectionStateMonitor(this, {
            viewModel.setNetworkState(true)
        }, {
            viewModel.setNetworkState(false)
        })
    }

    fun initObserver(){
        viewModel.networkState.observe(this){
            Log.d(TAG, "initObserver: $it")
            if(it == true){
                binding.appbar.visibility = View.GONE
            }
            else{
                binding.appbar.visibility = View.VISIBLE
            }
        }
    }


    private fun initNavigation() {
        val navController = supportFragmentManager.findFragmentById(R.id.frame_layout)
            ?.findNavController() as NavHostController

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val newTabId = item.itemId
            if (newTabId == currentTabId) return@setOnItemSelectedListener true

            val currentIndex = tabOrder.indexOf(currentTabId)
            val newIndex = tabOrder.indexOf(newTabId)

            val (enterAnim, exitAnim, popEnterAnim, popExitAnim) = if (newIndex > currentIndex) {
                // 오른쪽으로 이동 (→)
                listOf(
                    R.anim.from_right,
                    R.anim.to_left,
                    R.anim.from_left,
                    R.anim.to_right
                )
            } else {
                // 왼쪽으로 이동 (←)
                listOf(
                    R.anim.from_left,
                    R.anim.to_right,
                    R.anim.from_right,
                    R.anim.to_left
                )
            }

            val options = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setEnterAnim(enterAnim)
                .setExitAnim(exitAnim)
                .setPopEnterAnim(popEnterAnim)
                .setPopExitAnim(popExitAnim)
                .build()

            navController.navigate(newTabId, null, options)
            currentTabId = newTabId
            true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (tabOrder.contains(destination.id)) {
                binding.bottomNavigationView.visibility = View.VISIBLE
                currentTabId = destination.id
            } else {
                binding.bottomNavigationView.visibility = View.GONE
            }
        }
    }

    override fun onBackPressed() {
        val navController = supportFragmentManager.findFragmentById(R.id.frame_layout)
            ?.findNavController() as NavHostController

        val current = navController.currentDestination?.id

        if (current == R.id.nav_fragment_chat || current == R.id.nav_fragment_chat || current == R.id.nav_fragment_mypage || current == R.id.nav_fragment_study || current == R.id.nav_fragment_patent) {
            finish()
        } else {
            super.onBackPressed()
        }
    }


}
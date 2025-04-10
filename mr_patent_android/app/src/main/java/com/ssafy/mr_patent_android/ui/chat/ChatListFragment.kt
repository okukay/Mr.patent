package com.ssafy.mr_patent_android.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.AppBarLayout
import com.launchdarkly.eventsource.ConnectStrategy
import com.launchdarkly.eventsource.EventSource
import com.launchdarkly.eventsource.background.BackgroundEventSource
import com.ssafy.mr_patent_android.MainActivity
import com.ssafy.mr_patent_android.MainViewModel
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.data.model.dto.ChatRoomDto
import com.ssafy.mr_patent_android.databinding.FragmentChatListBinding
import com.ssafy.mr_patent_android.util.LoadingDialogSkeleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

private const val TAG = "ChatListFragment"
class ChatListFragment : BaseFragment<FragmentChatListBinding>(FragmentChatListBinding::bind, R.layout.fragment_chat_list) {
    val viewModel: ChatListViewModel by viewModels()
    val activityViewModel: MainViewModel by activityViewModels()
    private var eventSource: BackgroundEventSource? = null
    lateinit var mainActivity : MainActivity

    val loadingDialog by lazy {
        LoadingDialogSkeleton(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity = activity as MainActivity
        initView()
        initObserver()
    }

    override fun onResume() {
        super.onResume()
        if (eventSource == null) {
            Log.d(TAG, "onResume: SSE 연결 시작")
            eventSource = BackgroundEventSource
                .Builder(
                    SseEventHandler(viewModel),
                    EventSource.Builder(
                        ConnectStrategy
                            .http(URL("https://j12d208.p.ssafy.io/api/chat/rooms/subscribe/${sharedPreferences.getUser().userId}"))
                            .header("Authorization", "Bearer ${sharedPreferences.getAToken()}")
                            .connectTimeout(5, TimeUnit.SECONDS)
                            .readTimeout(10, TimeUnit.MINUTES)
                    )
                )
                .threadPriority(Thread.MAX_PRIORITY)
                .build().apply {
                    start()
                }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: SSE 연결 종료 시도")
        lifecycleScope.launch(Dispatchers.IO) {
            eventSource?.close()
            eventSource = null
        }
    }
    private fun initView() {
        eventSource?.start()
    }

    private fun initObserver() {
        viewModel.loading.observe(viewLifecycleOwner) {
            if (it) {
                loadingDialog.show()
            } else {
                loadingDialog.dismiss()
            }
        }

        var cnt=0
        activityViewModel.networkState.observe(requireActivity()){
            if (isAdded) {
                if (it == false) {
                    cnt+=1
                    requireActivity().findViewById<AppBarLayout>(R.id.appbar).visibility = View.VISIBLE
                } else {
                    if (cnt>=0) {
                        eventSource?.start()

                        viewModel.getChatRoomList()
                        requireActivity().findViewById<AppBarLayout>(R.id.appbar).visibility =
                            View.GONE
                    }

                }
            }
        }
        val adapter = ChatListAdapter { roomDto ->
            findNavController().navigate(
                ChatListFragmentDirections.actionNavFragmentChatToChatFragment(
                    roomDto.receiverId, roomDto.expertId, roomDto.roomId, roomDto.userName, roomDto.userImage ?: ""
                )
            )
        }
        binding.rvChatList.adapter = adapter

        viewModel.chatRoomList.observe(viewLifecycleOwner) { chatList ->
            Log.d(TAG, "initObserver: $chatList")
            adapter.submitList(chatList)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
//        viewModel.setChatRoomList(emptyList())
        Log.d(TAG, "onDestroy: ")
    }


}

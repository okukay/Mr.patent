package com.ssafy.mr_patent_android.ui.chat

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.ActionBar.LayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.ssafy.mr_patent_android.MainViewModel
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.WebViewActivity
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.data.model.dto.ChatMessageDto
import com.ssafy.mr_patent_android.data.model.dto.UserDto
import com.ssafy.mr_patent_android.databinding.DialogChatProfileBinding
import com.ssafy.mr_patent_android.databinding.DialogSizeOverBinding
import com.ssafy.mr_patent_android.databinding.FragmentChatBinding
import com.ssafy.mr_patent_android.databinding.FragmentChatBottomSheetBinding
import com.ssafy.mr_patent_android.databinding.ItemPhotoPreviewBinding
import com.ssafy.mr_patent_android.util.FileUtil
import gun0912.tedimagepicker.builder.TedImagePicker
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader


private const val TAG = "ChatFragment"

class ChatFragment :
    BaseFragment<FragmentChatBinding>(FragmentChatBinding::bind, R.layout.fragment_chat) {
    private lateinit var messageListAdapter: MessageListAdapter
    private lateinit var photoAdapter: PhotoAdapter
    val viewModel: ChatViewModel by viewModels()
    val activityViewModel: MainViewModel by activityViewModels()
    private val roomId: String by lazy {
        navArgs<ChatFragmentArgs>().value.roomId
    }
    private val userId: Int by lazy {
        navArgs<ChatFragmentArgs>().value.userId
    }
    private val expertId: Int by lazy {
        navArgs<ChatFragmentArgs>().value.expertId
    }
    private val userName: String by lazy {
        navArgs<ChatFragmentArgs>().value.userName
    }
    private val userImage: String by lazy {
        navArgs<ChatFragmentArgs>().value.userImage
    }
    lateinit var stompClient: StompClient

    var isConnected = false

    lateinit var sizeOverdialog: Dialog


    val url = "wss://j12d208.p.ssafy.io/ws/chat"

    var headerList: MutableList<StompHeader> = mutableListOf()
    var modalBottomSheet: BottomSheetDialog? = null

    private fun retryConnect() {
        if (!isConnected) {
            Handler(Looper.getMainLooper()).postDelayed({
                connectStomp()
            }, 3000)
        }
    }

    @SuppressLint("CheckResult")
    private fun connectStomp() {

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, url)
        headerList.add(StompHeader("Authorization", "Bearer ${sharedPreferences.getAToken()}"))
        headerList.add(StompHeader("roomId", roomId))
        headerList.add(StompHeader("userId", sharedPreferences.getUser().userId.toString()))
//
        val heartBeatHandler = Handler(Looper.getMainLooper())
        val heartBeatRunnable = object : Runnable {
            override fun run() {
                if (isConnected) {
                    viewModel.sendStompHeartBeat(stompClient, roomId)
                    heartBeatHandler.postDelayed(this, 1000)
                }
            }
        }
        stompClient.disconnect()


        stompClient.lifecycle().subscribe { lifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d(TAG, "STOMP 연결됨")
                    isConnected = true
                    subscribeAll()
                    heartBeatHandler.post(heartBeatRunnable)

                }

                LifecycleEvent.Type.ERROR -> {
                    Log.e(TAG, "STOMP 오류", lifecycleEvent.exception)
                    isConnected = false
                }

                LifecycleEvent.Type.CLOSED -> {
                    Log.d(TAG, "STOMP 연결 종료됨")
                    isConnected = false
                    heartBeatHandler.removeCallbacks(heartBeatRunnable)
                }

                LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> retryConnect()
            }
        }
        stompClient.connect(headerList)

    }

    @SuppressLint("CheckResult")
    private fun subscribeAll() {
        stompClient.topic("/sub/chat/room/" + roomId, headerList)
            .subscribe({ topicMessage ->
                try {
                    val message = Gson().fromJson(
                        topicMessage.getPayload(),
                        ChatMessageDto::class.java
                    )

                    if (message.type == "ENTER") {
                        if (message.status >= 2) {
                            viewModel.setState(true)
                            requireActivity().runOnUiThread {
                                messageListAdapter.setRead()
                            }
                        } else {
                            viewModel.setState(false)
                        }
                    } else if (message.type == "LEAVE") {
                        viewModel.setState(false)
                    } else {
                        viewModel.setIsSend(true)
                        viewModel.addMessage(message)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process message", e)
                }
            }, { error ->
                Log.e(
                    "SUBSCRIPTION_ERROR",
                    "Topic subscription error: ${error.message}"
                )
            })
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initBottomSheet()
        sizeOverDialog()
        initObserver()
        initKeyBoard()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun initObserver() {
        activityViewModel.networkState.observe(requireActivity()) {
            if (isAdded) {
                if (it == false) {
                    binding.tvNetworkError.visibility = View.VISIBLE
                } else {
                    binding.tvNetworkError.visibility = View.GONE
                    connectStomp()
                    viewModel.setMessageList(emptyList())
                    viewModel.setIsLoading(false)
                    viewModel.getMessageList(roomId)
                }
            }
        }
        messageListAdapter = MessageListAdapter(
            UserDto(userName, userImage),
            emptyList(),
            object : MessageListAdapter.ItemClickListener {
                override fun onItemClick(url: String) {
                    if (expertId != -1) {
                        initUserDialog(expertId, url)
                    }
                }

                override fun onFileClick(url: String) {
                    if (url.isNotEmpty()) {
                        val intent = Intent(requireContext(), WebViewActivity::class.java)
                        intent.putExtra("url", url)
                        intent.putExtra("file", 1)

                        startActivity(intent)

                    }

                }

                override fun onPhotoClick(url: String) {
//                    initDialog(url)
                }
            })



        binding.rvChat.adapter = messageListAdapter


        viewModel.messageList.observe(viewLifecycleOwner) {
            if (viewModel.isSend.value == true) {
                if (it.size == 2) {
                    messageListAdapter.addMessage(it[1], 1)
                    messageListAdapter.addMessage(it[0], 1)
                    binding.rvChat.post {
                        binding.rvChat.invalidate()
                    }
                } else {
                    messageListAdapter.addMessage(it[0], 0)

                }
                viewModel.setIsSend(false)
                if (it != null) {
                    binding.rvChat.scrollToPosition(0)
                }

            } else {
                requireActivity().runOnUiThread {
                    messageListAdapter.updateMessages(it)
                }

                val visibleMessages = it.filter { message -> message.messageType != "DIVIDER" }
                if (visibleMessages.size <= 20) {
                    binding.rvChat.scrollToPosition(0)
                }

            }
        }

    }

    fun initView() {
        binding.btnSend.isEnabled = false
        binding.tvChatHeader.text = userName
        binding.etMessage.addTextChangedListener {
            if (it.toString().isNotEmpty()) {
                binding.btnSend.isEnabled = true
            } else {
                binding.btnSend.isEnabled = false
            }
        }


        binding.btnSend.setOnClickListener {
            viewModel.sendMessage(
                userId,
                roomId,
                binding.etMessage.text.toString(),
                null,
                requireContext(),
                stompClient
            )

            binding.etMessage.text.clear()
        }

        binding.rvChat.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val firstVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                if ((viewModel.messageList.value?.size)?.minus(firstVisibleItemPosition) ?: 11 <= 10) {
                    if (viewModel.isLoading.value == false) {
                        viewModel.getMessageList(
                            roomId,
                            viewModel.messageList.value?.last()?.chatId
                        )
                    }
                }
            }
        })
    }

    fun initDialog(urls: String) {
        val dialog = Dialog(requireContext())
        val dialogBinding = ItemPhotoPreviewBinding.inflate(layoutInflater)

        val wrapper = FrameLayout(requireContext())
        wrapper.setPadding(200, 200, 200, 200)
        wrapper.addView(dialogBinding.root)

        dialog.setContentView(wrapper)

        dialog.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        Glide.with(requireContext())
            .load(urls)
            .fallback(R.drawable.chat_image_loader)
            .error(R.drawable.chat_image_loader)
            .into(dialogBinding.previewImage)

        dialogBinding.previewImage.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

    }

    fun initUserDialog(expertId: Int, urls: String) {
        val dialog = Dialog(requireContext())

        val dialogBinding = DialogChatProfileBinding.inflate(layoutInflater)

        viewModel.user.observe(viewLifecycleOwner) {
            dialogBinding.tvName.text = it.userName
            dialogBinding.tvDescription.text = it.expertDescription
            dialogBinding.tvAddress.text = it.expertAddress
            dialogBinding.tvPhone.text = it.expertPhone

            Glide.with(requireContext())
                .load(urls)
                .into(dialogBinding.ivProfile)


            if (it.category.isNotEmpty()) {
                it.category.forEach { category ->
                    when (category) {
                        "기계공학" -> dialogBinding.tvFieldMecha.visibility = View.VISIBLE
                        "전기/전자" -> dialogBinding.tvFieldElec.visibility = View.VISIBLE
                        "화학공학" -> dialogBinding.tvFieldChemi.visibility = View.VISIBLE
                        "생명공학" -> dialogBinding.tvFieldLife.visibility = View.VISIBLE
                    }
                }
            }
        }
        viewModel.getUser(expertId)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()


    }

    fun sizeOverDialog() {
        sizeOverdialog = Dialog(requireContext())
        sizeOverdialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogSizeOverBinding.inflate(layoutInflater)
        sizeOverdialog.setContentView(dialogBinding.root)
        sizeOverdialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.dlBtnYes.setOnClickListener {
            sizeOverdialog.dismiss()
        }
    }


    fun initBottomSheet() {
        val bottomBinding = FragmentChatBottomSheetBinding.inflate(layoutInflater)
        modalBottomSheet = BottomSheetDialog(requireContext())
        modalBottomSheet?.setContentView(bottomBinding.root)


        bottomBinding.btnDown.setOnClickListener {
            modalBottomSheet?.dismiss()
        }


        bottomBinding.btnGallery.setOnClickListener {
            TedImagePicker.with(requireContext())
                .max(5, "사진은 최대 5장까지 선택 가능합니다.")
                .showCameraTile(false)
                .startMultiImage { uriList ->
                    uriList.forEach { uri ->
                        if (FileUtil().isFileSizeValid(requireContext(), uri)) {
                            viewModel.addImage(ChatMessageDto.Files("", "", uri))
                        } else {
                            if (!sizeOverdialog.isShowing) {
                                sizeOverdialog.show()
                            }
                        }
                    }
                }

        }

        bottomBinding.btnFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf(
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    )
                )
            }
            startActivityForResult(intent, 1)
        }

        viewModel.file.observe(viewLifecycleOwner) {
            if (it != null) {
                bottomBinding.btnSend.isEnabled = true

                bottomBinding.previewImage.btnDelete.visibility = View.VISIBLE
                bottomBinding.previewImage.btnDelete.setOnClickListener {
                    viewModel.setFile(null)
                }
                bottomBinding.chatBottomSheetMenu.visibility = View.GONE
                bottomBinding.previewImage.root.visibility = View.VISIBLE
                FileUtil().getFileExtension(requireContext(), it)?.let { it1 ->
                    if (it1 == "pdf") {
                        bottomBinding.previewImage.previewImage.setImageResource(R.drawable.pdf_icon)
                    } else if (it1 == "doc" || it1 == "docx") {
                        bottomBinding.previewImage.previewImage.setImageResource(R.drawable.word_icon)
                    }
                }
                bottomBinding.previewImage.tvFileSize.text =
                    FileUtil().formatFileSize(FileUtil().getFileSize(requireContext(), it))
                bottomBinding.previewImage.tvFileName.text =
                    FileUtil().getFileName(requireContext(), it)
            } else {

                bottomBinding.btnSend.isEnabled = false
                bottomBinding.chatBottomSheetMenu.visibility = View.VISIBLE
                bottomBinding.previewImage.root.visibility = View.GONE
            }
        }

        bottomBinding.btnSend.setOnClickListener {
            val file = viewModel.file.value
            val image = viewModel.image.value
            val files = mutableListOf<ChatMessageDto.Files>()
            if (file != null) {
                files.add(
                    ChatMessageDto.Files(
                        FileUtil().getFileName(requireContext(), file)!!,
                        "",
                        file
                    )
                )
            } else {
                if (image != null) {
                    files.addAll(image)
                }

            }
            viewModel.sendMessage(
                userId,
                roomId,
                binding.etMessage.text.toString(),
                files,
                requireContext(),
                stompClient
            )
            viewModel.setFile(null)
            viewModel.deleteImage()
            modalBottomSheet!!.dismiss()
        }

        photoAdapter = PhotoAdapter(1, listOf()) {
            viewModel.removeImage(it)
            bottomBinding.rvImage.adapter?.notifyItemRemoved(it)
        }

        bottomBinding.rvImage.adapter = photoAdapter

        viewModel.image.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                bottomBinding.btnSend.isEnabled = true
                bottomBinding.chatBottomSheetMenu.visibility = View.GONE
                bottomBinding.rvImage.visibility = View.VISIBLE
                photoAdapter.updateItem(it)

            } else {
                bottomBinding.btnSend.isEnabled = false
                bottomBinding.chatBottomSheetMenu.visibility = View.VISIBLE
                bottomBinding.rvImage.visibility = View.GONE
            }
        }

        binding.btnAdd.setOnClickListener {
            modalBottomSheet?.show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        if (::stompClient.isInitialized && stompClient.isConnected()) {
            stompClient.disconnect()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                if (FileUtil().isFileSizeValid(requireContext(), uri)) {
                    viewModel.setFile(uri)
                } else {
                    if (!sizeOverdialog.isShowing) {
                        sizeOverdialog.show()
                    } else {
                    }

                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun initKeyBoard() {
        val rootView = view?.rootView
        val recyclerView = binding.rvChat

        rootView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            private var lastHeight = 0

            override fun onGlobalLayout() {
                val rect = Rect()
                rootView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootView.height
                val keypadHeight = screenHeight - rect.bottom

                if (lastHeight != keypadHeight) {
                    if (keypadHeight > screenHeight * 0.15) {
                        recyclerView.scrollToPosition(0)
                    }
                    lastHeight = keypadHeight
                }
            }
        })

        binding.rvChat.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val focused = activity?.currentFocus
                if (focused is EditText) {
                    focused.clearFocus()
                    hideKeyboard(focused)
                }
            }
            false
        }
    }

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

}

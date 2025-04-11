package com.ssafy.mr_patent_android.ui.chat

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.data.model.dto.ChatRoomDto
import com.ssafy.mr_patent_android.databinding.ListItemChatRoomBinding
import com.ssafy.mr_patent_android.util.TimeUtil
import java.net.URLDecoder

class ChatListAdapter(private val itemClickListener: ItemClickListener) :
    RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    private var chatList: List<ChatRoomDto> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding =
            ListItemChatRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    override fun getItemCount(): Int = chatList.size

    fun interface ItemClickListener {
        fun onItemClick(roomDto: ChatRoomDto)
    }

    fun submitList(newList: List<ChatRoomDto>) {
        val diffCallback = ChatListDiffCallback(chatList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        chatList = newList
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ChatViewHolder(private val binding: ListItemChatRoomBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chatRoom: ChatRoomDto) {
            binding.tvName.text = chatRoom.userName
            binding.tvChatPreview.text = chatRoom.lastMessage
            binding.tvTime.text = TimeUtil().formatLocalToStringMonthTime(
                TimeUtil().parseUtcWithJavaTime(chatRoom.lastMessageTime)
            )

            if (chatRoom.unreadCount > 0) {
                binding.tvUnreadCount.visibility = View.VISIBLE
                binding.tvUnreadCount.text = chatRoom.unreadCount.toString()
            } else {
                binding.tvUnreadCount.visibility = View.GONE
            }

            if (!chatRoom.userImage.isNullOrBlank()) {
                Glide.with(binding.root)
                    .load(chatRoom.userImage)
                    .circleCrop()
                    .error(R.drawable.user_profile)
                    .signature(ObjectKey(extractFileNameOnly(chatRoom.userImage) ?: ""))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.ivPatentAttorney)
            }

            binding.listItemChatRoom.setOnClickListener {
                itemClickListener.onItemClick(chatRoom)
            }
        }
    }

    private fun extractFileNameOnly(url: String): String? {
        val path = url.substringBefore("?").substringAfterLast("/")
        val decoded = URLDecoder.decode(path, "UTF-8")
        val fileName = decoded.substringAfterLast("/")
        return fileName.substringBeforeLast(".")
    }
}

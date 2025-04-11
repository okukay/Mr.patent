package com.ssafy.mr_patent_android.ui.chat

import androidx.recyclerview.widget.DiffUtil
import com.ssafy.mr_patent_android.data.model.dto.ChatRoomDto

class ChatListDiffCallback(
    private val oldList: List<ChatRoomDto>,
    private val newList: List<ChatRoomDto>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].roomId == newList[newItemPosition].roomId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldList[oldItemPosition]
        val new = newList[newItemPosition]
        val result = old.roomId == new.roomId &&
                old.lastMessageTime == new.lastMessageTime
                && old.lastMessage == new.lastMessage
                && old.unreadCount == new.unreadCount

        return result
    }
}

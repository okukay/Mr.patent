package com.ssafy.mr_patent_android.ui.chat

import androidx.recyclerview.widget.DiffUtil
import com.ssafy.mr_patent_android.data.model.dto.ChatMessageDto

class ChatDiffCallback(
    private val oldList: List<ChatMessageDto>,
    private val newList: List<ChatMessageDto>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].chatId == newList[newItemPosition].chatId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldList[oldItemPosition]
        val new = newList[newItemPosition]
        val result = old.chatId == new.chatId &&
                old.isRead == new.isRead

        return result
    }
}

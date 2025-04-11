package com.ssafy.mr_patent_android.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.data.model.dto.ChatMessageDto
import com.ssafy.mr_patent_android.data.model.dto.UserDto
import com.ssafy.mr_patent_android.databinding.ListItemChatDividerBinding
import com.ssafy.mr_patent_android.databinding.ListItemChatFileBinding
import com.ssafy.mr_patent_android.databinding.ListItemChatMessageBinding
import com.ssafy.mr_patent_android.databinding.ListItemChatPhotoBinding
import com.ssafy.mr_patent_android.util.TimeUtil
import java.time.format.DateTimeFormatter


private const val TAG = "MessageListAdapter"

open class MessageListAdapter(
    var user: UserDto,
    var messageList: List<ChatMessageDto>,
    val itemClickListener: ItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == MESSAGE_CONTENT) {

            val binding = ListItemChatMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return MessageViewHolder(binding)
        } else if (viewType == FILE_CONTENT) {
            val binding = ListItemChatFileBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return FileViewHolder(binding)
        } else if (viewType == PHOTO_CONTENT) {
            val binding = ListItemChatPhotoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return PhotoViewHolder(binding)
        } else if (viewType == DIVIDER) {
            val binding = ListItemChatDividerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return DividerViewHolder(binding)
        } else {
            val binding = ListItemChatDividerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return DividerViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MessageViewHolder -> holder.bind(position)
            is FileViewHolder -> holder.bind(position)
            is PhotoViewHolder -> holder.bind(position)
            is DividerViewHolder -> holder.bind(position)
        }
    }

    override fun getItemCount(): Int = messageList.size

    override fun getItemViewType(position: Int): Int {
        return when (messageList[position].messageType) {
            "TEXT" -> MESSAGE_CONTENT
            "PDF", "WORD" -> FILE_CONTENT
            "IMAGE" -> PHOTO_CONTENT
            "DIVIDER" -> DIVIDER
            else -> NO_CONTENT
        }
    }

    interface ItemClickListener {
        fun onItemClick(url: String)
        fun onFileClick(url: String)
        fun onPhotoClick(url: String)
    }

    inner class MessageViewHolder(private val binding: ListItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val date = TimeUtil().parseUtcWithJavaTime(
                messageList[position].timestamp ?: "2025-04-05T23:30:52.123Z"
            )
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val formattedDateTime = date.format(formatter)

            messageList[position].let {
                if (it.userId == sharedPreferences.getUser().userId) {
                    binding.llOtherMessageText.visibility = View.GONE
                    binding.llUserMessageText.visibility = View.VISIBLE
                    binding.tvUserMessageText.text = it.message
                    binding.tvUserMessageTime.text = formattedDateTime
                    binding.vUserMessageIsRead.visibility =
                        if (it.isRead) View.GONE else View.VISIBLE
                } else {
                    binding.llUserMessageText.visibility = View.GONE
                    binding.llOtherMessageText.visibility = View.VISIBLE
                    binding.tvOtherMessageText.text = it.message
                    binding.tvOtherMessageTime.text = formattedDateTime
                    binding.profileImageOtherText.setOnClickListener {
                        itemClickListener.onItemClick(user.userImage)
                    }
                    binding.otherNameText.text = user.userName
                    Glide.with(binding.root)
                        .load(user.userImage)
                        .circleCrop()
                        .fallback(R.drawable.user_profile)
                        .error(R.drawable.user_profile)
                        .into(binding.profileImageOtherText)
                }
            }

        }
    }

    inner class FileViewHolder(private val binding: ListItemChatFileBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val date = TimeUtil().parseUtcWithJavaTime(
                messageList[position].timestamp ?: "2025-04-05T23:30:52.123Z"
            )
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val formattedDateTime = date.format(formatter)


            messageList[position].let {
                if (it.userId == sharedPreferences.getUser().userId) {
                    binding.llOtherMessageFile.visibility = View.GONE
                    binding.llUserMessageFile.visibility = View.VISIBLE
                    binding.tvUserMessageTimeFile.text = formattedDateTime
                    binding.icUserMessageFile.tvFileName.text = it.fileName
                    binding.icUserMessageFile.layoutFilePreview.setOnClickListener {
                        messageList[position].fileUrl?.let { it1 ->
                            itemClickListener.onFileClick(
                                it1
                            )
                        }
                    }
                    binding.viewUserMessageFileIsread.visibility =
                        if (it.isRead) View.GONE else View.VISIBLE
                    if ("PDF".equals(it.messageType)) {
                        binding.icUserMessageFile.previewImage.setImageResource(R.drawable.pdf_icon)
                    } else {
                        binding.icUserMessageFile.previewImage.setImageResource(R.drawable.word_icon)
                    }
                } else {
                    binding.llUserMessageFile.visibility = View.GONE
                    binding.llOtherMessageFile.visibility = View.VISIBLE
                    binding.tvOtherMessageTimePhoto.text = formattedDateTime
                    binding.icOtherMessageFile.tvFileName.text = it.fileName
                    if ("PDF".equals(it.messageType)) {
                        binding.icOtherMessageFile.previewImage.setImageResource(R.drawable.pdf_icon)
                    } else {
                        binding.icOtherMessageFile.previewImage.setImageResource(R.drawable.word_icon)
                    }
                    binding.icOtherMessageFile.layoutFilePreview.setOnClickListener {
                        messageList[position].fileUrl?.let { it1 ->
                            itemClickListener.onFileClick(
                                it1
                            )
                        }
                    }

                    binding.profileImageOther.setOnClickListener {
                        itemClickListener.onItemClick(user.userImage)
                    }
                    binding.otherName.text = user.userName
                    Glide.with(binding.root)
                        .load(user.userImage)
                        .circleCrop()
                        .fallback(R.drawable.user_profile)
                        .error(R.drawable.user_profile)
                        .into(binding.profileImageOther)

                }

            }

        }
    }

    inner class PhotoViewHolder(private val binding: ListItemChatPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {

            messageList[position].let {
                val date =
                    TimeUtil().parseUtcWithJavaTime(it.timestamp ?: "2025-04-05T23:30:52.123Z")
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val formattedDateTime = date.format(formatter)
                if (it.userId == sharedPreferences.getUser().userId) {
                    binding.viewUserMessagePhotoIsread.visibility =
                        if (it.isRead) View.GONE else View.VISIBLE

                    binding.llOtherMessagePhoto.visibility = View.GONE
                    binding.llUserMessagePhoto.visibility = View.VISIBLE
                    binding.tvUserMessageTimePhoto.text = formattedDateTime
                    Glide.with(binding.root)
                        .load(messageList[position].fileUrl)
                        .fallback(R.drawable.user_profile)
                        .error(R.drawable.image_load_error_icon)
                        .into(binding.ivUserMessagePhoto)
                    binding.ivUserMessagePhoto.setOnClickListener {
                        messageList[position].fileUrl?.let { it1 ->
                            itemClickListener.onPhotoClick(
                                it1
                            )
                        }
                    }


                } else {
                    binding.llUserMessagePhoto.visibility = View.GONE
                    binding.llOtherMessagePhoto.visibility = View.VISIBLE
                    binding.tvOtherMessageTimePhoto.text = formattedDateTime
                    Glide.with(binding.root)
                        .load(messageList[position].fileUrl)
                        .transform(RoundedCorners(20))
                        .fallback(R.drawable.user_profile)
                        .error(R.drawable.user_profile)
                        .into(binding.ivOtherMessagePhoto)
                    binding.ivOtherMessagePhoto.setOnClickListener {
                        messageList[position].fileUrl?.let { it1 ->
                            itemClickListener.onPhotoClick(
                                it1
                            )
                        }
                    }
                    binding.profileImageOther.setOnClickListener {
                        itemClickListener.onItemClick(user.userImage)
                    }
                    binding.otherName.text = user.userName
                    Glide.with(binding.root)
                        .load(user.userImage)
                        .circleCrop()
                        .fallback(R.drawable.user_profile)
                        .error(R.drawable.user_profile)
                        .into(binding.profileImageOther)
                }
            }


        }
    }

    inner class DividerViewHolder(private val binding: ListItemChatDividerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.tvChatDividerTime.text = messageList[position].timestamp
        }
    }

    companion object {
        const val MESSAGE_CONTENT = 0
        const val FILE_CONTENT = 1
        const val PHOTO_CONTENT = 2
        const val DIVIDER = 3
        const val NO_CONTENT = 4
    }

    fun addMessage(message: ChatMessageDto, state: Int) {
        val mutableList = messageList.toMutableList()
        mutableList.add(0, message)
        messageList = mutableList
        notifyItemInserted(0)
    }

    fun updateMessages(newMessages: List<ChatMessageDto>) {
        if (messageList.size > 0 && newMessages.size > 0) {
        }

        val diffCallback = ChatDiffCallback(messageList, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        messageList = newMessages.map { it.copy() }

        diffResult.dispatchUpdatesTo(this)

        notifyDataSetChanged()

    }

    fun setRead() {
        val updatedList = messageList.map {
            if (it.userId == sharedPreferences.getUser().userId && !it.isRead) {
                it.copy(isRead = true)
            } else {
                it
            }
        }

        updateMessages(updatedList)
    }


}

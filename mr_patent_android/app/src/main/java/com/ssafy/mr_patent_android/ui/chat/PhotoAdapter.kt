package com.ssafy.mr_patent_android.ui.chat

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.data.model.dto.ChatMessageDto
import com.ssafy.mr_patent_android.data.model.dto.ChatRoomDto
import com.ssafy.mr_patent_android.databinding.ItemPhotoPreviewBinding
import com.ssafy.mr_patent_android.databinding.ListItemChatRoomBinding

class PhotoAdapter(val flag : Int=0, var photoList: List<ChatMessageDto.Files>, val itemClickListener:ItemClickListener) : RecyclerView.Adapter<PhotoAdapter.PhotoAdapter>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoAdapter {
        val binding =
            ItemPhotoPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoAdapter(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: PhotoAdapter, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = photoList.size

    fun interface ItemClickListener {
        fun onItemClick(position: Int)
    }

    inner class PhotoAdapter(private val binding: ItemPhotoPreviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {

            Glide.with(binding.root)
                .load(photoList[position].fileUri)
                .error(R.drawable.image_load_error_icon)
                .into(binding.previewImage)

            if (flag == 1 ){
                binding.btnDelete.visibility = ViewGroup.VISIBLE
                binding.btnDelete.setOnClickListener {
                    itemClickListener.onItemClick(position)
                }
            }


        }
    }
    fun removeItem(position: Int) {
        (photoList as ArrayList).removeAt(position)
        notifyItemRemoved(position)
    }

    fun updateItem(item: List<ChatMessageDto.Files>) {
            photoList = item
        notifyDataSetChanged()
    }
}
package com.ssafy.mr_patent_android.ui.patent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.data.model.dto.FolderDto
import com.ssafy.mr_patent_android.databinding.ListItemPatentFolderBinding
import com.ssafy.mr_patent_android.util.TimeUtil

class FolderAdapter(var editFlag: Boolean = false, var deleteFlag: Boolean = false, var folders: MutableList<FolderDto.Folder>, val itemClickListener : ItemClickListener)
    : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ListItemPatentFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = folders.size

    fun interface ItemClickListener {
        fun onItemClick(position: Int)
    }

    inner class FolderViewHolder(private val binding: ListItemPatentFolderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.tvFolderName.text = folders[position].uerPatentFolderTitle

            val date = TimeUtil().formatLocalDateTimeToString(TimeUtil().parseUtcWithJavaTime(folders[position].createdAt.plus("Z")))
            binding.tvFolderRecent.text = itemView.context.getString(R.string.tv_date_create, date)
            binding.clListItemPatentFolder.setOnClickListener {
                itemClickListener.onItemClick(position)
            }

            binding.ivFolderEdit.setOnClickListener {
                itemClickListener.onItemClick(position)
            }

            binding.ivFolderDelete.setOnClickListener {
                itemClickListener.onItemClick(position)
            }

            if (editFlag) {
                binding.ivFolderEdit.visibility = View.VISIBLE
            } else {
                binding.ivFolderEdit.visibility = View.GONE
            }

            if (deleteFlag) {
                binding.ivFolderDelete.visibility = View.VISIBLE
            } else {
                binding.ivFolderDelete.visibility = View.GONE
            }
        }
    }
}
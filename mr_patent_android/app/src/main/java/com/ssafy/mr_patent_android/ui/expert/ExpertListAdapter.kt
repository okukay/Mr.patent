package com.ssafy.mr_patent_android.ui.expert

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.data.model.dto.UserDto
import com.ssafy.mr_patent_android.databinding.ListItemExpertBinding


class ExpertListAdapter(val groupList: List<UserDto>, val itemClickListener: ItemClickListener) :
    RecyclerView.Adapter<ExpertListAdapter.ExpertListAdapter>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpertListAdapter {
        val binding =
            ListItemExpertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpertListAdapter(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ExpertListAdapter, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = groupList.size

    fun interface ItemClickListener {
        fun onItemClick(id: Int)
    }

    inner class ExpertListAdapter(private val binding: ListItemExpertBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {

            binding.listItemExpert.setOnClickListener {
                itemClickListener.onItemClick(groupList[position].expertId)
            }
            if (groupList[position].category.isNotEmpty()) {
                groupList[position].category.forEach { category ->
                    when (category) {
                        "기계공학" -> binding.tvFieldMecha.visibility = View.VISIBLE
                        "전기/전자" -> binding.tvFieldElec.visibility = View.VISIBLE
                        "화학공학" -> binding.tvFieldChemi.visibility = View.VISIBLE
                        "생명공학" -> binding.tvFieldLife.visibility = View.VISIBLE
                    }
                }
            }

            Glide.with(binding.root)
                .load(groupList[position].userImage)
                .circleCrop()
                .signature(ObjectKey(groupList[position].userImage))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.user_profile)
                .into(binding.ivExpert)
            binding.tvExpertName.text = groupList[position].userName
            binding.tvExpertDescription.text = groupList[position].expertDescription

        }
    }
}

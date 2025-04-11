package com.ssafy.mr_patent_android.ui.patent

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.data.model.response.PatentRecentResponse
import com.ssafy.mr_patent_android.databinding.ListItemPatentRecentBinding
import com.ssafy.mr_patent_android.util.TimeUtil

class PatentRecentAdapter(var patents: List<PatentRecentResponse.PatentDraft>, val itemClickListener : ItemClickListener)
    : RecyclerView.Adapter<PatentRecentAdapter.PatentRecentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatentRecentViewHolder {
        val binding = ListItemPatentRecentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PatentRecentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatentRecentViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = patents.size

    fun interface ItemClickListener {
        fun onItemClick(position: Int)
    }

    inner class PatentRecentViewHolder(private val binding: ListItemPatentRecentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val date = TimeUtil().formatLocalDateTimeToString(TimeUtil().parseUtcWithJavaTime(patents[position].createdAt))
            binding.tvPatentTitle.text = patents[position].patentDraftTitle
            binding.tvDateTest.text = itemView.context.getString(R.string.tv_date_test, date)
            binding.tvPatentContent.text = patents[position].patentDraftSummary
            binding.btnEdit.setOnClickListener {
                itemClickListener.onItemClick(position)
            }
        }
    }
}
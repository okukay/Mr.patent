package com.ssafy.mr_patent_android.ui.mypage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.data.model.response.SimiliarityResultResponse
import com.ssafy.mr_patent_android.databinding.ListItemSimiliarityPatentContentBinding

private const val TAG = "ReportResultAdapter_Mr_Patent"
class ReportResultAdapter(var results: List<SimiliarityResultResponse.SimiliarPatent>)
    : RecyclerView.Adapter<ReportResultAdapter.ReportResultViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportResultViewHolder {
        val binding = ListItemSimiliarityPatentContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportResultViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = results.size

    inner class ReportResultViewHolder(private val binding: ListItemSimiliarityPatentContentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.tvPatentContentSimiliarity.text = itemView.context.getString(
                R.string.tv_patent_similiarity_id,
                results[position].patentApplicationNumber
            )
            binding.tvPatentContentWrite.text = results[position].patentTitle
        }
    }
}
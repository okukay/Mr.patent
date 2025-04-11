package com.ssafy.mr_patent_android.ui.mypage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.data.model.response.PatentListResponse
import com.ssafy.mr_patent_android.databinding.ListItemPatentBinding
import com.ssafy.mr_patent_android.util.TimeUtil
import java.math.RoundingMode

class PatentFolderDetailAdapter(var patents: List<PatentListResponse.PatentSummaryInfo>, val itemClickListener : ItemClickListener)
    : RecyclerView.Adapter<PatentFolderDetailAdapter.PatentFolderDetailViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatentFolderDetailViewHolder {
        val binding = ListItemPatentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PatentFolderDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatentFolderDetailViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = patents.size

    fun interface ItemClickListener {
        fun onItemClick(position: Int)
    }

    inner class PatentFolderDetailViewHolder(private val binding: ListItemPatentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.tvPatentName.text = patents[position].patentDraftTitle
            binding.tvDateTest.text = itemView.context.getString(R.string.tv_date_test,
                        TimeUtil().formatLocalDateTimeToString(TimeUtil().parseUtcWithJavaTime(patents[position].createdAt.plus("Z")))
                )

            val score = (patents[position].detailedComparisonTotalScore*100).toBigDecimal().setScale(0, RoundingMode.HALF_UP).toInt()

            val similiarityResult = when(score) {
                in 0..50 -> "적합"
                in 51..70 -> "부분 적합"
                else -> "부적합"
            }
            binding.tvSimiliarityTestResult.text = itemView.context.getString(R.string.tv_similiarity_test_result, similiarityResult)

            binding.tvSimiliarityTestResultPercent.text = "${score}%"
            binding.pbSimiliarityResult.progress = score
            when (similiarityResult) {
                "적합" -> {
                    binding.pbSimiliarityResult.progressTintList = itemView.context.getColorStateList(R.color.mr_blue)
                }
                "부분 적합" -> {
                    binding.pbSimiliarityResult.progressTintList = itemView.context.getColorStateList(R.color.mr_green)
                }
                "부적합" -> {
                    binding.pbSimiliarityResult.progressTintList = itemView.context.getColorStateList(R.color.mr_red)
                }
            }
            binding.tvPatentDetailGo.setOnClickListener {
                itemClickListener.onItemClick(position)
            }
        }
    }
}
package com.ssafy.mr_patent_android.ui.mypage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.data.model.dto.FitnessFrameDto
import com.ssafy.mr_patent_android.databinding.ListItemFailStandardBinding

class ReportResultFailAdapter(var fitnessContents: List<FitnessFrameDto>)
    : RecyclerView.Adapter<ReportResultFailAdapter.ReportResultFailViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportResultFailViewHolder {
        val binding = ListItemFailStandardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportResultFailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportResultFailViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = fitnessContents.size

    inner class ReportResultFailViewHolder(private val binding: ListItemFailStandardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.tvTitle.text = fitnessContents[position].title
            binding.cbCheck.isChecked = true
            if (fitnessContents[position].check) {
                binding.cbCheck.buttonTintList = binding.root.context.resources.getColorStateList(
                    R.color.mr_blue, null)
            } else {
                binding.cbCheck.buttonTintList = binding.root.context.resources.getColorStateList(
                    R.color.mr_red, null)
                binding.cbCheck.buttonIconTintList = binding.root.context.resources.getColorStateList(
                    R.color.mr_red, null)
            }
        }
    }
}
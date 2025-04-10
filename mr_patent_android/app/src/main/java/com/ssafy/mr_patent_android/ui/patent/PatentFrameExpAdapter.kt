package com.ssafy.mr_patent_android.ui.patent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.mr_patent_android.data.model.dto.PatentFrameDto
import com.ssafy.mr_patent_android.databinding.ListItemSpecBinding

class PatentFrameExpAdapter(
    val mode: String
    , var patentFrameExpList: List<PatentFrameDto>
) : RecyclerView.Adapter<PatentFrameExpAdapter.PatentFrameExpViewHolder>() {

    private val editTexts: MutableList<EditText> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatentFrameExpViewHolder {
        val binding = ListItemSpecBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PatentFrameExpViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatentFrameExpViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = patentFrameExpList.size

    fun interface ItemClickListener {
        fun onItemClick(position: Int)
    }

    inner class PatentFrameExpViewHolder(private val binding: ListItemSpecBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.tvSpecTitle.text = patentFrameExpList[position].title
            binding.etSpecContent.setText(patentFrameExpList[position].content ?: "")

            editTexts.add(binding.etSpecContent)

            if (mode == "select") {
                binding.etSpecContent.isEnabled = false
            }
            binding.listItemSpec.setOnClickListener {
                toggleLayout(patentFrameExpList[position].isExpanded, binding.ivToggle, binding.etSpecContent)
                patentFrameExpList[position].isExpanded = !patentFrameExpList[position].isExpanded
            }
        }
    }

    fun toggleLayout(isExpanded: Boolean, view: View, layout: View) {
        if (isExpanded) {
            view.rotation = 180f
            layout.visibility = View.VISIBLE
        } else {
            view.rotation = 0f
            layout.visibility = View.GONE
        }
    }


    fun expFillInput(): Boolean {
        return editTexts.all { it.text.isNotBlank() }
    }
}
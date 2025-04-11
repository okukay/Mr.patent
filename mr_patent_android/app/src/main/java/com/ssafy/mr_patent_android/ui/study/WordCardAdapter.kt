package com.ssafy.mr_patent_android.ui.study

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.mr_patent_android.data.model.dto.WordDto
import com.ssafy.mr_patent_android.databinding.ListItemCardBinding

class WordCardAdapter(
    val word_list: List<WordDto.Word>,
    val itemClickListener: ItemClickListener
) : RecyclerView.Adapter<WordCardAdapter.WordCardViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordCardViewHolder {
        val binding =
            ListItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WordCardViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: WordCardViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = word_list.size

    fun interface ItemClickListener {
        fun onItemClick(position: Int): Boolean
    }

    inner class WordCardViewHolder(private val binding: ListItemCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            val word = word_list[position]

            binding.flashCardWord.cardStudyText.text = word.word_name
            binding.flashCardMean.cardStudyText.text = word.word_mean
        }

    }

    fun updateBookmarkState(position: Int, updatedWord: WordDto.Word) {
        (word_list as MutableList)[position] = updatedWord
        notifyItemChanged(position)
    }


}

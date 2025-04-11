package com.ssafy.mr_patent_android.ui.study

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.data.model.dto.WordDto
import com.ssafy.mr_patent_android.databinding.ListItemWordBinding

class WordAllAdapter(
    private val wordList: MutableList<WordDto.Word>,
    private val itemClickListener: ItemClickListener
) : RecyclerView.Adapter<WordAllAdapter.WordViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding =
            ListItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WordViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = wordList.size

    fun interface ItemClickListener {
        fun onItemClick(position: Int, checked: Boolean): Boolean
    }

    inner class WordViewHolder(private val binding: ListItemWordBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val word = wordList[position]
            binding.tvWord.text = word.word_name
            binding.tvMean.text = word.word_mean
            binding.tBtnBookmark.isChecked = word.is_bookmarked

            binding.expandableLayout.isVisible = word.checked ?: false
            binding.tBtnExpand.isChecked = word.checked ?: false

            binding.expandItem.setOnClickListener {
                binding.tBtnExpand.isChecked = !binding.expandableLayout.isVisible
                binding.expandableLayout.isVisible = !binding.expandableLayout.isVisible
            }

            setBookmarkIcon(word.is_bookmarked)

            binding.bookmarkItem.setOnClickListener() {
                val toggled = !word.is_bookmarked

                setBookmarkIcon(toggled)

                val success =
                    itemClickListener.onItemClick(position, binding.expandableLayout.isVisible)
                if (!success) {
                    setBookmarkIcon(word.is_bookmarked)
                }
            }

            binding.bookmarkItem.setOnClickListener() {
                itemClickListener.onItemClick(position, binding.expandableLayout.isVisible)
            }
        }

        private fun setBookmarkIcon(isBookmarked: Boolean) {
            val resId = if (isBookmarked) {
                R.drawable.bookmark_fill
            } else {
                R.drawable.bookmark_border
            }
            binding.tBtnBookmark.setBackgroundResource(resId)
        }
    }


    fun updateBookmarkState(position: Int, updatedWord: WordDto.Word, checked: Boolean) {
        wordList[position] = updatedWord
        wordList[position].checked = checked
        notifyItemChanged(position)
    }


}

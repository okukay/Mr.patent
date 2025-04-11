package com.ssafy.mr_patent_android.ui.study

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.data.model.dto.Question
import com.ssafy.mr_patent_android.databinding.ItemQuizOptionBinding

class QuizOptionAdapter(
    private var options: List<Question.Option>,
    private val correctOptionId: Int,
    val itemClickListener: ItemClickListener
) : RecyclerView.Adapter<QuizOptionAdapter.OptionViewHolder>() {

    private var selectedOption: Int? = null
    private var isAnswerChecked = false

    inner class OptionViewHolder(private val binding: ItemQuizOptionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(option: Question.Option) {
            binding.tvOptionText.text = option.optionText

            binding.tvOptionText.setBackgroundResource(R.drawable.rounded_background_stroke)

            if (isAnswerChecked) {
                when (option.optionId) {
                    correctOptionId -> {
                        binding.tvOptionText.setBackgroundResource(R.drawable.quiz_option_correct)
                        binding.tvIndicator.visibility = View.VISIBLE
                        binding.tvIndicator.setTextColor(
                            binding.root.context.getColor(R.color.mr_green)
                        )
                        binding.tvIndicator.text = "O"
                    }

                    selectedOption -> {
                        if (selectedOption != correctOptionId) {
                            binding.tvOptionText.setBackgroundResource(R.drawable.quiz_option_wrong)
                            binding.tvIndicator.visibility = View.VISIBLE
                            binding.tvIndicator.setTextColor(
                                binding.root.context.getColor(R.color.mr_red)
                            )
                            binding.tvIndicator.text = "X"
                        }
                    }
                }
            }

            binding.tvOptionText.setOnClickListener {
                if (!isAnswerChecked) {
                    isAnswerChecked = true
                    selectedOption = option.optionId
                    itemClickListener.onItemClick(option.optionId)
                    notifyDataSetChanged()
                }
            }
        }
    }

    fun interface ItemClickListener {
        fun onItemClick(id: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val binding =
            ItemQuizOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount(): Int = options.size
}

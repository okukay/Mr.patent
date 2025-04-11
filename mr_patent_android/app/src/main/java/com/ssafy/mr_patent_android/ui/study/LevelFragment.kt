package com.ssafy.mr_patent_android.ui.study

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentLevelBinding


class LevelFragment : BaseFragment<FragmentLevelBinding>(FragmentLevelBinding::bind, R.layout.fragment_level) {
    val levelId by lazy {
        navArgs<LevelFragmentArgs>().value.levelId}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTitle.text = "Level $levelId"

        binding.cardStudy.setOnClickListener {
            findNavController().navigate(LevelFragmentDirections.actionLevelFragmentToStudyCardFragment(levelId))
        }

        binding.cardQuiz.setOnClickListener {
            findNavController().navigate(LevelFragmentDirections.actionLevelFragmentToQuizFragment(levelId))
        }
    }
}
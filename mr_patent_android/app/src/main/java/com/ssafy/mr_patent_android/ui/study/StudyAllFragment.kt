package com.ssafy.mr_patent_android.ui.study

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentStudyAllBinding

class StudyAllFragment : BaseFragment<FragmentStudyAllBinding>(
    FragmentStudyAllBinding::bind,
    R.layout.fragment_study_all
) {
    val viewModel: StudyCardViewModel by viewModels()
    private lateinit var wordAllAdapter: WordAllAdapter
    val level_id by lazy {
        navArgs<StudyAllFragmentArgs>().value.levelId
    }
    val type by lazy {
        navArgs<StudyAllFragmentArgs>().value.type
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (type == "bookmark") {
            binding.tvTitle.text = "북마크 단어장"
            viewModel.getBookmarkWordList(level_id)
        } else {
            binding.tvTitle.text = "Level $level_id"
            viewModel.getWordList(level_id)
        }

        binding.btnStudyCard.setOnClickListener {
            findNavController().navigate(
                StudyAllFragmentDirections.actionStudyAllFragmentToStudyCardFragment(
                    level_id, type
                ),
                NavOptions.Builder()
                    .setPopUpTo(R.id.studyAllFragment, true)
                    .build()
            )
        }

        initObserver()
    }

    fun initObserver() {
        viewModel.loading.observe(viewLifecycleOwner) {
            if (it) {
                showLoadingDialog()
            } else {
                dismissLoadingDialog()
            }
        }

        viewModel.wordList.observe(viewLifecycleOwner) { list ->
            if (!::wordAllAdapter.isInitialized) {
                wordAllAdapter = WordAllAdapter(list.toMutableList()) { position, checked ->
                    if (viewModel.isLoading.value == true) return@WordAllAdapter false

                    val result = viewModel.createBookmark(position)
                    val updatedWord = viewModel.wordList.value?.get(position)

                    if (result && updatedWord != null) {
                        wordAllAdapter.updateBookmarkState(position, updatedWord, checked)
                    }

                    return@WordAllAdapter result
                }
                binding.rvStudyAll.adapter = wordAllAdapter
            }
        }
    }

}
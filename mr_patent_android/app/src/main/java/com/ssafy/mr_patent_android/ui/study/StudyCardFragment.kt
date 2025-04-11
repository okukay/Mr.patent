package com.ssafy.mr_patent_android.ui.study

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentStudyCardBinding

private const val TAG = "StudyCardFragment"

class StudyCardFragment : BaseFragment<FragmentStudyCardBinding>(
    FragmentStudyCardBinding::bind,
    R.layout.fragment_study_card
) {
    val viewModel: StudyCardViewModel by viewModels()
    lateinit var wordCardAdapter: WordCardAdapter
    val level_id by lazy {
        navArgs<StudyCardFragmentArgs>().value.levelId
    }
    val type by lazy {
        navArgs<StudyCardFragmentArgs>().value.type
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObserver()


    }

    fun initView() {
        if (type == "bookmark") {
            binding.tvTitle.text = "북마크 단어장"
            viewModel.getBookmarkWordList(level_id)
            binding.tvSequence.text =
                "${binding.vpStudyCard.currentItem + 1}/${viewModel.total.value ?: ""}"
            binding.vpStudyCard.setPageTransformer { page, position ->
                val firstItem = binding.vpStudyCard.currentItem == 0
                val lastItem =
                    binding.vpStudyCard.currentItem == viewModel.total.value?.toInt()!! - 1
                binding.btnBack.isVisible = !firstItem
                binding.btnNext.isVisible = !lastItem

                binding.tvSequence.text =
                    "${binding.vpStudyCard.currentItem + 1}/${viewModel.total.value ?: ""}"
            }
        } else {
            binding.tvTitle.text = "Level $level_id"
            viewModel.getWordList(level_id)
            binding.tvSequence.text = "${binding.vpStudyCard.currentItem + 1}/30"
            binding.vpStudyCard.setPageTransformer { page, position ->
                binding.tvSequence.text = "${binding.vpStudyCard.currentItem + 1}/30"

                val firstItem = binding.vpStudyCard.currentItem == 0
                val lastItem = binding.vpStudyCard.currentItem == 29

                binding.btnBack.isVisible = !firstItem
                binding.btnNext.isVisible = !lastItem
            }
        }
        binding.btnAll.setOnClickListener {
            findNavController().navigate(
                StudyCardFragmentDirections.actionStudyCardFragmentToStudyAllFragment(
                    level_id,
                    type
                ),
                NavOptions.Builder()
                    .setPopUpTo(R.id.studyCardFragment, true)
                    .build()
            )
        }



        binding.btnNext.setOnClickListener {
            binding.vpStudyCard.setCurrentItem(binding.vpStudyCard.currentItem + 1, true)
        }

        binding.btnBack.setOnClickListener {
            binding.vpStudyCard.setCurrentItem(binding.vpStudyCard.currentItem - 1, true)
        }

        binding.vpStudyCard.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.right = 48
                outRect.left = 48
            }
        })
    }

    fun initObserver() {
        viewModel.loading.observe(viewLifecycleOwner) {
            if (it) {
                showLoadingDialog()
            } else {
                dismissLoadingDialog()
            }
        }

        viewModel.wordList.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.tvSequence.text = "0/0"
                return@observe
            }
            it?.let { list ->
                if (!::wordCardAdapter.isInitialized) {
                    wordCardAdapter = WordCardAdapter(list.toMutableList()) { position ->
                        if (viewModel.isLoading.value == true) return@WordCardAdapter false

                        val result = viewModel.createBookmark(position)
                        val updatedWord = viewModel.wordList.value?.get(position)

                        if (result && updatedWord != null) {
                            wordCardAdapter.updateBookmarkState(position, updatedWord)
                        }

                        return@WordCardAdapter result
                    }
                    binding.vpStudyCard.adapter = wordCardAdapter
                } else {
                }
            }
        }
        binding.vpStudyCard.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateExternalBookmarkState()
            }
        })
        binding.tBtnBookmark.setOnClickListener {
            val current = binding.vpStudyCard.currentItem
            if (viewModel.isLoading.value == true) return@setOnClickListener

            val result = viewModel.createBookmark(current)
            val updatedWord = viewModel.wordList.value?.getOrNull(current)
            if (result && updatedWord != null) {
                wordCardAdapter.updateBookmarkState(current, updatedWord)
                updateExternalBookmarkState()
            }
        }

    }

    private fun updateExternalBookmarkState() {
        val current = binding.vpStudyCard.currentItem
        val word = viewModel.wordList.value?.getOrNull(current)
        word?.let {
            val resId = if (it.is_bookmarked) {
                R.drawable.bookmark_fill
            } else {
                R.drawable.bookmark_border
            }
            binding.tBtnBookmark.setBackgroundResource(resId)
        }
    }

}
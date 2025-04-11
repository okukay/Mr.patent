package com.ssafy.mr_patent_android.ui.study

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.DialogStartQuizBinding
import com.ssafy.mr_patent_android.databinding.FragmentQuizResultBinding

private const val TAG = "QuizResultFragment"

class QuizResultFragment : BaseFragment<FragmentQuizResultBinding>(
    FragmentQuizResultBinding::bind,
    R.layout.fragment_quiz_result
) {
    private lateinit var gestureDetector: GestureDetectorCompat
    private val viewModel: StudyCardViewModel by viewModels()

    private lateinit var wordAllAdapter: WordAllAdapter
    val wrongQuiz by lazy {
        navArgs<QuizResultFragmentArgs>().value.answerDto
    }
    val levelId by lazy {
        navArgs<QuizResultFragmentArgs>().value.levelId
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "onViewCreated: $wrongQuiz")
        initView()
        initObserver()

        binding.tvQuizResult.text = getString(R.string.quiz_result, 8)
    }

    fun initView() {
        viewModel.getQuizResult(levelId, wrongQuiz.toList())
        binding.tvTitle.text = "퀴즈 결과"
    }

    fun initObserver() {
        viewModel.resultData.observe(viewLifecycleOwner) { result ->
            binding.tvQuizResult.text = getString(R.string.quiz_result, result.score)
            binding.tvQuizScore.text = "${result.score}/10"
            if (result.score == 10) {
                binding.tvQuizPassed.text = "Passed"
                binding.circularProgressIndicator.progress = 10
                binding.btnGoLevelList.setBackgroundColor(resources.getColor(R.color.mr_blue))
                binding.tvWrongList.visibility = View.GONE
            } else {
                initSlide()
                if (result.score <= 7) {
                    binding.tvQuizMent.visibility = View.VISIBLE
                    binding.tvQuizMent.text = getString(R.string.quiz_ment)
                    binding.circularProgressIndicator.setIndicatorColor(
                        resources.getColor(R.color.mr_red, null)
                    )
                    binding.tvQuizPassed.text = "Failed"

                    binding.btnRetry.visibility = View.VISIBLE
                    binding.btnGoLevelList.setBackgroundColor(resources.getColor(R.color.mr_gray))
                    binding.btnRetry.setOnClickListener {
                        initDialog()
                    }
                } else {
                    binding.tvQuizPassed.text = "Passed"
                    binding.btnGoLevelList.setBackgroundColor(resources.getColor(R.color.mr_blue))
                }

                if (!::wordAllAdapter.isInitialized) {
                    wordAllAdapter = WordAllAdapter(
                        (viewModel.resultData.value?.words ?: listOf()).toMutableList()
                    ) { position, checked ->
                        if (viewModel.isLoading.value == true) return@WordAllAdapter false

                        val result = viewModel.createBookmark(position)
                        val updatedWord = viewModel.wordList.value?.get(position)

                        if (result && updatedWord != null) {
                            wordAllAdapter.updateBookmarkState(position, updatedWord, checked)
                        }

                        return@WordAllAdapter result
                    }
                    binding.rvWrongList.adapter = wordAllAdapter
                }

                binding.circularProgressIndicator.progress = result.score

            }


            binding.btnGoLevelList.setOnClickListener {
                findNavController().navigate(
                    QuizResultFragmentDirections.actionQuizResultFragmentToNavFragmentStudy(),
                    NavOptions.Builder()
                        .setPopUpTo(R.id.nav_fragment_study, false)
                        .build()
                )
            }
        }
    }

    fun initDialog() {
        val dialog = Dialog(requireContext())
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)

        val bindingDialog =
            DialogStartQuizBinding.inflate(LayoutInflater.from(requireContext()), null, false)

        dialog.setContentView(bindingDialog.root)

        bindingDialog.dlBtnYes.setOnClickListener {
            findNavController().navigate(
                QuizResultFragmentDirections.actionQuizResultFragmentToQuizFragment(
                    levelId
                ),
                NavOptions.Builder()
                    .setPopUpTo(R.id.quizResultFragment, true)
                    .build()
            )
            dialog.dismiss()
        }
        bindingDialog.dlBtnNo.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun initSlide() {
        val slidePanel = binding.suplQuizResult

        slidePanel.isTouchEnabled = true

        gestureDetector = GestureDetectorCompat(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    slidePanel.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
                    return super.onFling(e1, e2, velocityX, velocityY)
                }
            })
        binding.tvWrongList.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }


}
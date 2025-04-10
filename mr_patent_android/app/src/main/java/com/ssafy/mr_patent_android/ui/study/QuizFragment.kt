package com.ssafy.mr_patent_android.ui.study

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.data.model.dto.QuizDto
import com.ssafy.mr_patent_android.databinding.DialogQuizOutBinding
import com.ssafy.mr_patent_android.databinding.FragmentQuizBinding

private const val TAG = "QuizFragment"
class QuizFragment : BaseFragment<FragmentQuizBinding>(FragmentQuizBinding::bind, R.layout.fragment_quiz) {
    private val quizViewModel: QuizViewModel by viewModels()
    private lateinit var optionAdapter: QuizOptionAdapter
    val levelId by lazy {
        navArgs<QuizFragmentArgs>().value.levelId
    }

    private var currentQuestionIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            initDialog()
        }

        initView()
        initObserver()
    }
    fun initDialog() {
        val dialogBinding = DialogQuizOutBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        dialogBinding.dlBtnOut.setOnClickListener {
            findNavController().popBackStack()
            dialog.dismiss()
        }

        dialogBinding.dlBtnNo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun initView(){

        quizViewModel.getQuiz(1)

        binding.tvTitle.text = "Level ${levelId} 퀴즈"
        binding.rvQuizOptions.layoutManager = LinearLayoutManager(requireContext())

        binding.button.setOnClickListener {
            quizViewModel.quizData.value?.let { quizData ->
                if (currentQuestionIndex < quizData.questions.size - 1) {
                    currentQuestionIndex++
                    updateQuestion(quizData)

                    it.visibility = View.INVISIBLE
                } else {
                    binding.button.text = "퀴즈 완료"
                    Log.d(TAG, "initView: ${quizViewModel.wrongAnswers.value}")
                    quizViewModel.wrongAnswers.value?.let { wrongAnswers ->
                        Log.d(TAG, "initView: $wrongAnswers")
                    }
                    findNavController().navigate(
                        QuizFragmentDirections.actionQuizFragmentToQuizResultFragment((quizViewModel.wrongAnswers.value?: emptyList<Int>()).toIntArray()  , levelId),
                        NavOptions.Builder()
                            .setPopUpTo(R.id.quizFragment, true)
                            .build()
                    )
                }
            }
        }
    }

    fun initObserver(){
        quizViewModel.quizData.observe(viewLifecycleOwner) { quizData ->
            Log.d(TAG, "onViewCreated: $quizData")
            currentQuestionIndex = 0
            updateQuestion(quizData)
        }

    }

    private fun updateQuestion(quizData: QuizDto) {
        val question = quizData.questions[currentQuestionIndex]
        
        binding.cardStudy.text = question.questionText
        
        optionAdapter = QuizOptionAdapter(question.options, question.correctOption, question.wordId, quizViewModel){
            if(it != question.correctOption) {
                Log.d(TAG, "updateQuestion: ${question.wordId}")
                quizViewModel.addWrongQuiz(question.wordId)
            }
            binding.button.visibility = View.VISIBLE
        }
        binding.rvQuizOptions.adapter = optionAdapter

        binding.tvSequence.text = "${currentQuestionIndex + 1}/${quizData.questions.size}"
        binding.linearProgressIndicator.progress = (currentQuestionIndex )

        binding.button.text = if (currentQuestionIndex == quizData.questions.size - 1) "퀴즈 완료" else "다음 문제"
    }
}

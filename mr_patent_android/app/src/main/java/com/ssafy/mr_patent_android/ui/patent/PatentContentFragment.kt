package com.ssafy.mr_patent_android.ui.patent

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.tabs.TabLayoutMediator
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.data.model.dto.PatentDraftDto
import com.ssafy.mr_patent_android.data.model.dto.PatentFrameDto
import com.ssafy.mr_patent_android.data.model.dto.PatentTitleDto
import com.ssafy.mr_patent_android.data.model.response.PatentRecentResponse
import com.ssafy.mr_patent_android.databinding.FragmentPatentContentBinding
import com.ssafy.mr_patent_android.ui.mypage.ReportResultViewModel
import com.ssafy.mr_patent_android.util.LoadingDialog

private const val TAG = "PatentContentFragment_Mr_Patent"
class PatentContentFragment : BaseFragment<FragmentPatentContentBinding>(
    FragmentPatentContentBinding::bind, R.layout.fragment_patent_content
) {
    private val args: PatentContentFragmentArgs by navArgs()
    private val fileViewModel: FileViewModel by activityViewModels()
    private val patentViewModel : PatentViewModel by activityViewModels()
    private val reportResultViewModel : ReportResultViewModel by activityViewModels()
    private val similiarityTestViewModel : SimiliarityTestViewModel by activityViewModels()

    private lateinit var expFragment: PatentContentExpFragment
    private lateinit var claimFragment: PatentContentClaimFragment
    private lateinit var summaryFragment: PatentContentSummaryFragment

    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initObserver()
    }

    private fun initView() {
        loadingDialog = LoadingDialog(requireContext())
        reportResultViewModel.setMode(args.mode)

        when (args.mode) {
            "select" -> {
                binding.btnSimiliarityTest.visibility = View.GONE
                binding.clReportItems.visibility = View.VISIBLE
                reportResultViewModel.getPatentContent(args.id)
            }
            "update" -> {
                binding.btnSimiliarityTest.visibility = View.VISIBLE
                binding.clReportItems.visibility = View.GONE
                reportResultViewModel.getPatentContent(patentViewModel.patentDraftId.value!!)
            }
            "write" -> {
                binding.btnSimiliarityTest.visibility = View.VISIBLE
                binding.clReportItems.visibility = View.GONE
                splitContent(null)
                initViewPager()
            }
            "upload" -> {
                binding.btnSimiliarityTest.visibility = View.VISIBLE
                binding.clReportItems.visibility = View.GONE
            }
        }


        binding.tvBefore.setOnClickListener {
            when (args.mode) {
                "select" -> findNavController().popBackStack()
                "update" -> findNavController().popBackStack()
                "write" -> findNavController().popBackStack()
                "upload" -> {
                    fileViewModel.setPatentContent(null)
                    findNavController().popBackStack(R.id.fileUploadFragment, false)
                }
            }
        }

        binding.btnReportConfirm.setOnClickListener {
            findNavController().navigate(PatentContentFragmentDirections.actionPatentContentFragmentToReportResultFragment(args.id))
        }

        binding.btnFileExtraction.setOnClickListener {
            setDialogFileExtraction()
        }

        binding.btnSimiliarityTest.setOnClickListener {
            if (isFillInput()) {
                if (similiarityTestViewModel.testState.value == "waiting") {
                    showCustomToast("현재 유사도 검사가 진행 중입니다.")
                    findNavController().navigate(PatentContentFragmentDirections.actionPatentContentFragmentToSimiliarityTestFragment("processing"))
                } else {
                    loadingDialog.show()
                    setDialogSimiliarityTest()
                }
            } else {
                showCustomToast("빈 칸을 입력해주세요.")
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (args.mode) {
                    "upload" -> {
                        fileViewModel.setPatentContent(null)
                        findNavController().popBackStack(R.id.fileUploadFragment, false)
                    }
                    "write" -> {
                        findNavController().popBackStack()
                    }
                    "update" -> {
                        findNavController().popBackStack()
                    }
                    "select" -> {
                        findNavController().popBackStack()
                    }
                }
            }
        })
    }

    private fun initObserver() {
        fileViewModel.patentContent.observe(viewLifecycleOwner, { // upload
            splitContent(it)
            initViewPager()
        })

        reportResultViewModel.patentContent.observe(viewLifecycleOwner, { // select, update
            if (args.mode == "select") {
                binding.tvDraftWrite.text =  it.patentDraftTitle
            }
            splitContent(it)
            initViewPager()
        })

        similiarityTestViewModel.patentId.observe(viewLifecycleOwner, {
            patentViewModel.getRecentPatentList()
            similiarityTestViewModel.similiaritytest(it)
        })

        similiarityTestViewModel.testState.observe(viewLifecycleOwner, {
            it?.let {
                if (it == "processing" && args.mode != "select") {
                    splitContent(null)
                    findNavController().navigate(PatentContentFragmentDirections.actionPatentContentFragmentToSimiliarityTestFragment("processing"))
                }
            }
        })
    }

    private fun splitContent(patentDraft: PatentRecentResponse.PatentDraft?) {
        val patentTitleExp = PatentTitleDto.PatentTitleExpDto()
        reportResultViewModel.setPatentExpContents(listOf(
//            PatentFrameDto(patentTitleExp.patentDraftTitleExp, patentDraft?.patentDraftTitle ?: getString(R.string.patentDraftTitleExp))
//            , PatentFrameDto(patentTitleExp.patentDraftTechnicalFieldExp, patentDraft?.patentDraftTechnicalField ?: getString(R.string.patentDraftTechnicalFieldExp))
//            , PatentFrameDto(patentTitleExp.patentDraftBackgroundExp, patentDraft?.patentDraftBackground ?: getString(R.string.patentDraftBackgroundExp))
//            , PatentFrameDto(patentTitleExp.patentDraftProblemExp, patentDraft?.patentDraftProblem ?: getString(R.string.patentDraftProblemExp))
//            , PatentFrameDto(patentTitleExp.patentDraftSolutionExp, patentDraft?.patentDraftSolution ?: getString(R.string.patentDraftSolutionExp))
//            , PatentFrameDto(patentTitleExp.patentDraftEffectExp, patentDraft?.patentDraftEffect ?: getString(R.string.patentDraftEffectExp))
//            , PatentFrameDto(patentTitleExp.patentDraftDetailedExp, patentDraft?.patentDraftDetailed ?: getString(R.string.patentDraftDetailedExp))
            PatentFrameDto(patentTitleExp.patentDraftTitleExp, patentDraft?.patentDraftTitle ?: "")
            , PatentFrameDto(patentTitleExp.patentDraftTechnicalFieldExp, patentDraft?.patentDraftTechnicalField ?: "")
            , PatentFrameDto(patentTitleExp.patentDraftBackgroundExp, patentDraft?.patentDraftBackground ?: "")
            , PatentFrameDto(patentTitleExp.patentDraftProblemExp, patentDraft?.patentDraftProblem ?: "")
            , PatentFrameDto(patentTitleExp.patentDraftSolutionExp, patentDraft?.patentDraftSolution ?: "")
            , PatentFrameDto(patentTitleExp.patentDraftEffectExp, patentDraft?.patentDraftEffect ?: "")
            , PatentFrameDto(patentTitleExp.patentDraftDetailedExp, patentDraft?.patentDraftDetailed ?: "")
        ))

        val patentTitleClaim = PatentTitleDto.PatentTitleClaimDto()
        reportResultViewModel.setPatentClaimContents(listOf(
            PatentFrameDto(patentTitleClaim.patentDraftClaim, patentDraft?.patentDraftClaim ?: "")
//            PatentFrameDto(patentTitleClaim.patentDraftClaim, patentDraft?.patentDraftClaim ?: getString(R.string.patentDraftClaim))
        ))

        val patentTitleSummary = PatentTitleDto.PatentTitleSummaryDto()
        reportResultViewModel.setPatentSummaryContents(listOf(
            PatentFrameDto(patentTitleSummary.patentDraftSummary, patentDraft?.patentDraftSummary ?: "")
//            PatentFrameDto(patentTitleSummary.patentDraftSummary, patentDraft?.patentDraftSummary ?: getString(R.string.patentDraftSummary))
        ))
    }

    private fun initViewPager() {
        val vpContents = binding.vpContents
        val tlContentsClassification = binding.tlContentsClassificaton

        val fragmentList = ArrayList<Fragment>()
        expFragment = PatentContentExpFragment()
        claimFragment = PatentContentClaimFragment()
        summaryFragment = PatentContentSummaryFragment()

        fragmentList.add(expFragment)
        fragmentList.add(claimFragment)
        fragmentList.add(summaryFragment)

        vpContents.offscreenPageLimit = 3
        vpContents.adapter = ViewContentAdapter(fragmentList, requireActivity() as AppCompatActivity)
        val tabArray = arrayOf("발명의 설명", "청구범위", "요약서")
        TabLayoutMediator(tlContentsClassification, vpContents) { tab, position ->
            tab.text = tabArray[position]
        }.attach()
    }

    private fun setDialogSimiliarityTest() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_similiarity_test, null)
        val dialogBuilder = Dialog(requireContext())
        dialogBuilder.setContentView(dialogView)
        dialogBuilder.create()
        dialogBuilder.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ((context.resources.displayMetrics.widthPixels) * 0.8).toInt(),
                ((context.resources.displayMetrics.heightPixels) * 0.2).toInt()
            )
        }
        dialogBuilder.show()

        val confirm = dialogView.findViewById<Button>(R.id.btn_similiarity_test_confirm)
        val cancel = dialogView.findViewById<Button>(R.id.btn_similiarity_text_cancel)

        confirm.setOnClickListener {
            val expAdapter = expFragment.binding.rvPatentContentExp
            val childCount = expAdapter.childCount
            val specContents = mutableListOf<String>()
            for (i in 0 until childCount) {
                val etExp = expAdapter[i].findViewById<EditText>(R.id.et_spec_content).text.toString()
                specContents.add(etExp)
            }

            val draftDto = PatentDraftDto(
                specContents[0],
                specContents[1],
                specContents[2],
                specContents[3],
                specContents[4],
                specContents[5],
                specContents[6],
                claimFragment.getPatentClaimContents(),
                summaryFragment.getPatentSummaryContents(),
                patentViewModel.folderId.value!!
            )
            Log.d(TAG, "setDialogSimiliarityTest: ${draftDto}")

            similiarityTestViewModel.addDraft(patentViewModel.folderId.value!!, draftDto)
            dialogBuilder.dismiss()
        }

        cancel.setOnClickListener {
            dialogBuilder.dismiss()
        }
    }

    private fun setDialogFileExtraction() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_file_extraction, null)
        val dialogBuilder = Dialog(requireContext())
        dialogBuilder.setContentView(dialogView)
        dialogBuilder.create()
        dialogBuilder.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ((context.resources.displayMetrics.widthPixels) * 0.8).toInt(),
                ((context.resources.displayMetrics.heightPixels) * 0.3).toInt()
            )
        }
        dialogBuilder.show()

        val btnExtraction = dialogView.findViewById<Button>(R.id.btn_extraction)
        val tvPdf = dialogView.findViewById<TextView>(R.id.tv_pdf)

        tvPdf.setOnClickListener {
            fileViewModel.setExtractionType("pdf")
            tvPdf.setBackgroundResource(R.drawable.rounded_background_stroke_active)
        }

        btnExtraction.setOnClickListener {
            dialogBuilder.dismiss()
            reportResultViewModel.getPdfFile(requireContext(), args.id, reportResultViewModel.patentContent.value!!.patentDraftTitle)
            setDialogFileComplete()
        }
    }

    private fun setDialogFileComplete() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_file_complete, null)
        val dialogBuilder = Dialog(requireContext())
        dialogBuilder.setContentView(dialogView)
        dialogBuilder.create()
        dialogBuilder.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ((context.resources.displayMetrics.widthPixels) * 0.8).toInt(),
                ((context.resources.displayMetrics.heightPixels) * 0.2).toInt()
            )
        }

        fileViewModel.setExtractionType("")

        val tvExpertConsult = dialogView.findViewById<TextView>(R.id.tv_expert_consult)
        val btnListExpertGo = dialogView.findViewById<Button>(R.id.btn_list_expert_go)

        if (sharedPreferences.getUser().userRole == 1) {
            tvExpertConsult.text= "파일 내용을 확인해주세요."
            btnListExpertGo.text = "확인"
        }

        btnListExpertGo.setOnClickListener {
            dialogBuilder.dismiss()
            if (sharedPreferences.getUser().userRole == 0) {
                findNavController().navigate(R.id.patentAttorneyListFragment)
            }
        }

        dialogBuilder.show()
    }

    fun isFillInput() : Boolean {
        val result = expFragment.expFillInput()
                && summaryFragment.summaryFillInput()
                && claimFragment.claimFillInput()

        if (result) {
            return true
        } else {
            showCustomToast("빈 칸을 입력해주세요.")
            return false
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String, value: String) =
            PatentContentFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
package com.ssafy.mr_patent_android.ui.mypage

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.data.model.dto.FitnessFrameDto
import com.ssafy.mr_patent_android.data.model.dto.PatentTitleDto
import com.ssafy.mr_patent_android.data.model.response.FitnessResultResponse
import com.ssafy.mr_patent_android.data.model.response.SimiliarityResultResponse
import com.ssafy.mr_patent_android.databinding.FragmentReportResultBinding

private const val TAG = "ReportResultFragment_Mr_Patent"
class ReportResultFragment : BaseFragment<FragmentReportResultBinding>(
    FragmentReportResultBinding::bind, R.layout.fragment_report_result
) {
    private val args: ReportResultFragmentArgs by navArgs()

    private val reportResultViewModel : ReportResultViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initObserver()
    }

    private fun initView() {
        reportResultViewModel.getFitnessResult(args.id)
        binding.tvReportName.text = args.name

        binding.tvBefore.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnConfirm.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun initObserver() {
        reportResultViewModel.fitnessResult.observe(viewLifecycleOwner) {
            binding.tvFitnessResult.text = it
            when (it) {
                "PASS" -> {
                    binding.tvFitnessResult.setTextColor(resources.getColor(R.color.mr_blue))
                    binding.rvSimiliarityResultPass.visibility = View.VISIBLE
                    binding.rvSimiliarityResultFail.visibility = View.GONE
                    reportResultViewModel.getSimiliarityResult(args.id)
                }
                "FAIL" -> {
                    binding.tvFitnessResult.setTextColor(resources.getColor(R.color.mr_red))
                    binding.tvPatentSimiliarity.text = "기준 별 적합도 결과"
                    binding.tvSwipeExp.visibility = View.INVISIBLE
                    binding.rvSimiliarityResultPass.visibility = View.GONE
                    binding.rvSimiliarityResultFail.visibility = View.VISIBLE
                }
            }
        }

        reportResultViewModel.fitnessContents.observe(viewLifecycleOwner) { // 적합도 결과 FAIL
            binding.rvSimiliarityResultFail.layoutManager = LinearLayoutManager(requireContext())
            binding.rvSimiliarityResultFail.adapter = ReportResultFailAdapter(listOf(
                FitnessFrameDto(PatentTitleDto.PatentTitleExpDto().patentDraftTechnicalFieldExp, it.technicalField)
                , FitnessFrameDto(PatentTitleDto.PatentTitleExpDto().patentDraftBackgroundExp, it.background)
                , FitnessFrameDto(PatentTitleDto.PatentTitleExpDto().patentDraftProblemExp, it.problem)
                , FitnessFrameDto(PatentTitleDto.PatentTitleExpDto().patentDraftSolutionExp, it.solution)
                , FitnessFrameDto(PatentTitleDto.PatentTitleExpDto().patentDraftEffectExp, it.effect)
                , FitnessFrameDto(PatentTitleDto.PatentTitleExpDto().patentDraftDetailedExp, it.detailed)
                , FitnessFrameDto(PatentTitleDto.PatentTitleSummaryDto().patentDraftSummary, it.summary)
                , FitnessFrameDto(PatentTitleDto.PatentTitleClaimDto().patentDraftClaim, it.claim)
            ))
        }

        reportResultViewModel.similiarityResult.observe(viewLifecycleOwner) { // 적합도 결과 PASS
            Log.d(TAG, "initObserver: 유사도 나와라: ${it}")
            binding.rvSimiliarityResultPass.layoutManager = LinearLayoutManager(requireContext())
            binding.rvSimiliarityResultPass.adapter = ReportResultAdapter(it)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String, value: String) =
            ReportResultFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
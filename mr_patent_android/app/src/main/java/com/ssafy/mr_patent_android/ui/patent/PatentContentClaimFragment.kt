package com.ssafy.mr_patent_android.ui.patent

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import com.ssafy.mr_patent_android.R

;
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentPatentContentClaimBinding
import com.ssafy.mr_patent_android.ui.mypage.ReportResultViewModel

private const val TAG = "PatentContentClaimFragment_Mr_Patent"
class PatentContentClaimFragment : BaseFragment<FragmentPatentContentClaimBinding>(
    FragmentPatentContentClaimBinding::bind, R.layout.fragment_patent_content_claim
) {
    private val reportResultViewModel: ReportResultViewModel by activityViewModels()
    private var isExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
    }

    private fun initView() {
        if (reportResultViewModel.mode.value == "select") {
            binding.etSpecContent.isEnabled = false
        }
        binding.etSpecContent.setText(reportResultViewModel.patentClaimContents.value!![0].content)

        binding.clClaims.setOnClickListener {
            toggleLayout(isExpanded, binding.ivToggle, binding.etSpecContent)
            isExpanded = !isExpanded
        }
    }

    fun claimFillInput() : Boolean {
        return binding.etSpecContent.text.isNotBlank()
    }

    fun getPatentClaimContents() : String {
        return binding.etSpecContent.text.toString()
    }

    fun toggleLayout(isExpanded: Boolean, view: View, layout: View) {
        if (isExpanded) {
            view.rotation = 180f
            layout.visibility = View.VISIBLE
        } else {
            view.rotation = 0f
            layout.visibility = View.GONE
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String, value: String) =
            PatentContentClaimFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
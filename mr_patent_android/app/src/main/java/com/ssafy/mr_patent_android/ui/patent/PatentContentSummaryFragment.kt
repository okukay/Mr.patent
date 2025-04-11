package com.ssafy.mr_patent_android.ui.patent

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import androidx.fragment.app.activityViewModels
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentPatentContentSummaryBinding
import com.ssafy.mr_patent_android.ui.mypage.ReportResultViewModel
import com.ssafy.mr_patent_android.util.FilePicker
import com.ssafy.mr_patent_android.util.FileUtil
import kotlin.math.log

private const val TAG = "PatentContentSummaryFragment_Mr_Patent"
class PatentContentSummaryFragment : BaseFragment<FragmentPatentContentSummaryBinding>(
    FragmentPatentContentSummaryBinding::bind, R.layout.fragment_patent_content_summary
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

        binding.etSpecContent.setText(reportResultViewModel.patentSummaryContents.value!![0].content)
        binding.clSummary.setOnClickListener {
            toggleLayout(isExpanded, binding.ivToggle, binding.etSpecContent)
            isExpanded = !isExpanded
        }
    }

    fun summaryFillInput() : Boolean {
        return binding.etSpecContent.text.isNotBlank()
    }

    fun getPatentSummaryContents() : String {
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
            PatentContentSummaryFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
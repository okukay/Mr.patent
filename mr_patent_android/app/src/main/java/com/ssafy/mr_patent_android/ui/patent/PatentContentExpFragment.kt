package com.ssafy.mr_patent_android.ui.patent

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentPatentContentExpBinding
import com.ssafy.mr_patent_android.ui.mypage.ReportResultViewModel

private const val TAG = "PatentContentExpFragment_Mr_Patent"
class PatentContentExpFragment : BaseFragment<FragmentPatentContentExpBinding>(
    FragmentPatentContentExpBinding::bind, R.layout.fragment_patent_content_exp
) {
    private val reportResultViewModel: ReportResultViewModel by activityViewModels()

    private var isExpanded = mutableListOf(false, false, false, false, false, false, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
    }

    private fun initView() {
        if (reportResultViewModel.mode.value == "select") {
            binding.etSpecContent1.isEnabled = false
            binding.etSpecContent2.isEnabled = false
            binding.etSpecContent3.isEnabled = false
            binding.etSpecContent4.isEnabled = false
            binding.etSpecContent5.isEnabled = false
            binding.etSpecContent6.isEnabled = false
            binding.etSpecContent7.isEnabled = false
        }

        binding.etSpecContent1.setText(reportResultViewModel.patentExpContents.value!![0].content)
        binding.clTitle.setOnClickListener {
            toggleLayout(isExpanded[0], binding.ivToggle1, binding.etSpecContent1)
            isExpanded[0] = !isExpanded[0]
        }

        binding.etSpecContent2.setText(reportResultViewModel.patentExpContents.value!![1].content)
        binding.clTechnicalField.setOnClickListener {
            toggleLayout(isExpanded[1], binding.ivToggle2, binding.etSpecContent2)
            isExpanded[1] = !isExpanded[1]
        }

        binding.etSpecContent3.setText(reportResultViewModel.patentExpContents.value!![2].content)
        binding.clBackground.setOnClickListener {
            toggleLayout(isExpanded[2], binding.ivToggle3, binding.etSpecContent3)
            isExpanded[2] = !isExpanded[2]
        }

        binding.etSpecContent4.setText(reportResultViewModel.patentExpContents.value!![3].content)
        binding.clProblem.setOnClickListener {
            toggleLayout(isExpanded[3], binding.ivToggle4, binding.etSpecContent4)
            isExpanded[3] = !isExpanded[3]
        }

        binding.etSpecContent5.setText(reportResultViewModel.patentExpContents.value!![4].content)
        binding.clSolution.setOnClickListener {
            toggleLayout(isExpanded[4], binding.ivToggle5, binding.etSpecContent5)
            isExpanded[4] = !isExpanded[4]
        }

        binding.etSpecContent6.setText(reportResultViewModel.patentExpContents.value!![5].content)
        binding.clEffect.setOnClickListener {
            toggleLayout(isExpanded[5], binding.ivToggle6, binding.etSpecContent6)
            isExpanded[5] = !isExpanded[5]
        }

        binding.etSpecContent7.setText(reportResultViewModel.patentExpContents.value!![6].content)
        binding.clDetailed.setOnClickListener {
            toggleLayout(isExpanded[6], binding.ivToggle7, binding.etSpecContent7)
            isExpanded[6] = !isExpanded[6]
        }
    }

    fun expFillInput(): Boolean {
        return (binding.etSpecContent1.text.isNotBlank()
                && binding.etSpecContent2.text.isNotBlank()
                && binding.etSpecContent3.text.isNotBlank()
                && binding.etSpecContent4.text.isNotBlank()
                && binding.etSpecContent5.text.isNotBlank()
                && binding.etSpecContent6.text.isNotBlank()
                && binding.etSpecContent7.text.isNotBlank())
    }

    fun getPatentExp1Contents() : String {
        return binding.etSpecContent1.text.toString()
    }

    fun getPatentExp2Contents() : String {
        return binding.etSpecContent2.text.toString()
    }

    fun getPatentExp3Contents() : String {
        return binding.etSpecContent3.text.toString()
    }

    fun getPatentExp4Contents() : String {
        return binding.etSpecContent4.text.toString()
    }

    fun getPatentExp5Contents() : String {
        return binding.etSpecContent5.text.toString()
    }

    fun getPatentExp6Contents() : String {
        return binding.etSpecContent6.text.toString()
    }

    fun getPatentExp7Contents() : String {
        return binding.etSpecContent7.text.toString()
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
            PatentContentExpFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
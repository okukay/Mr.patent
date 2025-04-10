package com.ssafy.mr_patent_android.ui.mypage

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.data.model.response.PatentListResponse
import com.ssafy.mr_patent_android.databinding.FragmentPatentFolderDetailBinding
import com.ssafy.mr_patent_android.ui.patent.FolderAdapter

private const val TAG = "PatentFolderDetailFragment_Mr_Patent"
class PatentFolderDetailFragment : BaseFragment<FragmentPatentFolderDetailBinding>(
    FragmentPatentFolderDetailBinding::bind, R.layout.fragment_patent_folder_detail
) {
    private val patentFolderDetailViewModel : PatentFolderDetailViewModel by activityViewModels()

    private val args : PatentFolderDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initObserver()
    }

    private fun initView() {
        binding.tvFolderName.text = args.folderName
        patentFolderDetailViewModel.getPatentList(patentFolderDetailViewModel.folderId.value!!)

        binding.tvBefore.setOnClickListener {
            findNavController().popBackStack()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        })
    }

    private fun initObserver() {
        patentFolderDetailViewModel.patents.observe(viewLifecycleOwner) {
            binding.rvFolderItems.layoutManager = LinearLayoutManager(requireContext())
            val previewList: MutableList<PatentListResponse.PatentSummaryInfo> = mutableListOf()
            for (i in 0..it.size-1) {
//                if (it[i].detailedComparisonTotalScore != 0.0) {
                    previewList.add(it[i])
//                }
            }
            if (previewList.isNotEmpty()) {
                binding.rvFolderItems.adapter = PatentFolderDetailAdapter(previewList) { position ->
                    findNavController().navigate(
                        PatentFolderDetailFragmentDirections.actionPatentFolderDetailFragmentToPatentContentFragment(it[position].patentDraftId, "select")
                    )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String, value: String) =
            PatentFolderDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
package com.ssafy.mr_patent_android.ui.patent

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.annotations.SerializedName
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.data.model.dto.FolderDto
import com.ssafy.mr_patent_android.data.model.response.PatentRecentResponse
import com.ssafy.mr_patent_android.databinding.FragmentPatentBinding

private const val TAG = "PatentFragment_Mr_Patent"
class PatentFragment : BaseFragment<FragmentPatentBinding>(
    FragmentPatentBinding::bind, R.layout.fragment_patent
) {

    private val patentViewModel: PatentViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initObserver()
    }

    private fun initView() {
        patentViewModel.getRecentPatentList()

        binding.clFileUpload.setOnClickListener {
            patentViewModel.setDraftType("upload")
            findNavController().navigate(R.id.patentFolderChoiceFragment)
        }

        binding.clWrite.setOnClickListener {
            patentViewModel.setDraftType("write")
            findNavController().navigate(R.id.patentFolderChoiceFragment)
        }
    }

    private fun initObserver() {
        patentViewModel.patentsRecent.observe(viewLifecycleOwner) {
            binding.rvRecentDrafts.layoutManager = LinearLayoutManager(requireContext())
            binding.rvRecentDrafts.adapter = PatentRecentAdapter(it) { position ->
                Log.d(TAG, "initObserver: patentsRecent ${it[position].patentDraftId}")
                patentViewModel.setDraftType("update")
                patentViewModel.setPatentDraftId(it[position].patentDraftId)
                findNavController().navigate(R.id.patentFolderChoiceFragment)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String, value: String) =
            PatentFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
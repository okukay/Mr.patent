package com.ssafy.mr_patent_android.ui.patent

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.data.model.dto.FolderDto
import com.ssafy.mr_patent_android.databinding.FragmentPatentFolderChoiceBinding

private const val TAG = "PatentFolderChoiceFragment_Mr_Patent"
class PatentFolderChoiceFragment : BaseFragment<FragmentPatentFolderChoiceBinding>(
    FragmentPatentFolderChoiceBinding::bind, R.layout.fragment_patent_folder_choice
) {
    private val patentViewModel : PatentViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initObserver()
    }

    private fun initView() {
        patentViewModel.getFolderList()

        binding.tvBefore.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.ivFolderAdd.setOnClickListener {
            setDialogFolderAdd()
        }
    }

    private fun initObserver() {
        patentViewModel.folders.observe(viewLifecycleOwner) {
            binding.rvFolderItems.layoutManager = LinearLayoutManager(requireContext())
            binding.rvFolderItems.adapter = FolderAdapter(false, false, it) { position ->
                patentViewModel.setFolderId(it[position].userPatentFolderId)
                when(patentViewModel.draftType.value) {
                    "upload" -> findNavController().navigate(R.id.fileUploadFragment)
                    "write" -> findNavController().navigate(PatentFolderChoiceFragmentDirections.actionPatentFolderChoiceFragmentToPatentContentFragment(-1, "write"))
                    "update" -> findNavController().navigate(PatentFolderChoiceFragmentDirections.actionPatentFolderChoiceFragmentToPatentContentFragment(patentViewModel.patentDraftId.value!!, "update"))
                }
            }
        }
    }

    private fun setDialogFolderAdd() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_folder_add, null)
        val dialogBuilder = Dialog(requireContext())
        dialogBuilder.setContentView(dialogView)
        dialogBuilder.create()
        dialogBuilder.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ((context.resources.displayMetrics.widthPixels) * 0.8).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        dialogBuilder.show()

        val btnFolderAdd = dialogView.findViewById<Button>(R.id.btn_folder_add)
        val etFolderName = dialogView.findViewById<EditText>(R.id.et_folder_name)

        btnFolderAdd.setOnClickListener {
            when (etFolderName.length()) {
                0 -> { showCustomToast("폴더명을 입력해주세요.") }
                in 1..30 -> {
                    patentViewModel.addFolder(etFolderName.text.toString())
                    dialogBuilder.dismiss()
                }
                else -> { showCustomToast("폴더 이름은 30자 까지 가능합니다.") }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String, value: String) =
            PatentFolderChoiceFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
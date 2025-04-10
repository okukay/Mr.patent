package com.ssafy.mr_patent_android.ui.patent

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
        val tvLengthLimit = dialogView.findViewById<TextView>(R.id.tv_length_limit)

        etFolderName.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                true
            } else {
                false
            }
        }

        etFolderName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val textLength = s?.length ?: 0
                tvLengthLimit.setText("$textLength/20")
            }
        })

        btnFolderAdd.setOnClickListener {
            if (etFolderName.text.isBlank()) {
                showCustomToast("폴더명을 입력해주세요.")
            } else {
                patentViewModel.addFolder(etFolderName.text.toString())
                dialogBuilder.dismiss()
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
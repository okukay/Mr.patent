package com.ssafy.mr_patent_android.ui.mypage

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
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.data.model.dto.FolderDto
import com.ssafy.mr_patent_android.databinding.FragmentPatentFolderBinding
import com.ssafy.mr_patent_android.ui.patent.FolderAdapter
import com.ssafy.mr_patent_android.ui.patent.PatentViewModel

private const val TAG = "PatentFolderFragment_Mr_Patent"
class PatentFolderFragment : BaseFragment<FragmentPatentFolderBinding>(
    FragmentPatentFolderBinding::bind, R.layout.fragment_patent_folder
) {
    private val patentViewModel : PatentViewModel by activityViewModels()
    private val patentFolderDetailViewModel : PatentFolderDetailViewModel by activityViewModels()

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

        binding.btnFolderEdit.setOnClickListener {
            patentViewModel.setEditFlag(true)
            binding.rvPatentFolders.adapter = FolderAdapter(true, false, patentViewModel.folders.value!!) { position ->
                setDialogFolderEdit(position)
            }
        }

        binding.btnFolderDelete.setOnClickListener {
            patentViewModel.setDeleteFlag(true)
            binding.rvPatentFolders.adapter = FolderAdapter(false, true, patentViewModel.folders.value!!) { position ->
                setDialogFolderDelete(position)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (patentViewModel.editFlag.value == true || patentViewModel.deleteFlag.value == true) {
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.patentFolderFragment)
                    patentViewModel.setEditFlag(false)
                    patentViewModel.setDeleteFlag(false)
                } else {
                    findNavController().popBackStack()
                }
            }
        })
    }

    private fun initObserver() {
        patentViewModel.folders.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                binding.ivFolderAdd.visibility = View.GONE
                binding.clFolderState.visibility = View.VISIBLE
                binding.rvPatentFolders.layoutManager = LinearLayoutManager(requireContext())
                binding.rvPatentFolders.adapter = FolderAdapter(false, false, it) { position ->
                    patentFolderDetailViewModel.setFolderId(it[position].userPatentFolderId)
                    findNavController().navigate(PatentFolderFragmentDirections.actionPatentFolderFragmentToPatentFolderDetailFragment(it[position].uerPatentFolderTitle))
                }
            } else {
                binding.ivFolderAdd.visibility = View.VISIBLE
                binding.clFolderState.visibility = View.GONE
                binding.rvPatentFolders.layoutManager = LinearLayoutManager(requireContext())
                binding.rvPatentFolders.adapter = FolderAdapter(false, false, it)  {}
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

        etFolderName.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                true
            } else {
                false
            }
        }

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

    private fun setDialogFolderEdit(position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_folder_edit, null)
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

        val etFolderName = dialogView.findViewById<EditText>(R.id.et_folder_name)
        val btnFolderEdit = dialogView.findViewById<Button>(R.id.btn_folder_edit)
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

        btnFolderEdit.setOnClickListener {
            if (etFolderName.text.isBlank()) {
                showCustomToast("변경할 폴더명을 입력해주세요.")
            } else {
                patentViewModel.editFolder(patentViewModel.folders.value!![position].userPatentFolderId, etFolderName.text.toString())
                dialogBuilder.dismiss()
            }
        }
    }

    private fun setDialogFolderDelete(position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_folder_delete, null)
        val dialogBuilder = Dialog(requireContext())
        dialogBuilder.setContentView(dialogView)
        dialogBuilder.create()
        dialogBuilder.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ((context.resources.displayMetrics.widthPixels) * 0.6).toInt(),
                ((context.resources.displayMetrics.heightPixels) * 0.14).toInt()
            )
        }
        dialogBuilder.show()

        val btnFolderDeleteConfirm = dialogView.findViewById<Button>(R.id.btn_folder_delete_confirm)
        val btnFolderDeleteCancel = dialogView.findViewById<Button>(R.id.btn_folder_delete_cancel)

        btnFolderDeleteConfirm.setOnClickListener {
            patentViewModel.deleteFolder(patentViewModel.folders.value!![position].userPatentFolderId)
            dialogBuilder.dismiss()
        }

        btnFolderDeleteCancel.setOnClickListener {
            dialogBuilder.dismiss()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String, value: String) =
            PatentFolderFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
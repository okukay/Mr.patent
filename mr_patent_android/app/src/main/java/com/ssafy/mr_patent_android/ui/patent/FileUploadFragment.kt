package com.ssafy.mr_patent_android.ui.patent

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentFileUploadBinding
import com.ssafy.mr_patent_android.ui.mypage.ReportResultViewModel
import com.ssafy.mr_patent_android.util.FilePicker
import com.ssafy.mr_patent_android.util.FileUtil
import com.ssafy.mr_patent_android.util.LoadingDialog
import java.io.File

private const val TAG = "FileUploadFragment_Mr_Patent"
class FileUploadFragment : BaseFragment<FragmentFileUploadBinding>(
    FragmentFileUploadBinding::bind, R.layout.fragment_file_upload
) {
    private val fileViewModel : FileViewModel by activityViewModels()

    private lateinit var filePickerUtil: FilePicker

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
        filePickerUtil = FilePicker(this) { uri ->
            val fileSize = FileUtil().getFileSize(requireContext(), uri)
            if(fileSize >= 1024 * 1024 * 5) {
                setDialogSizeOver()
                return@FilePicker
            } else {
                setInfo(uri)
                fileViewModel.setFileUri(uri.toString())
            }
        }

        binding.tvBefore.setOnClickListener {
            backstack()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backstack()
            }
        })

        binding.ivFileUpload.setOnClickListener {
            filePickerUtil.checkPermissionAndOpenStorage()
        }

        binding.tvFileUpload.setOnClickListener {
            filePickerUtil.checkPermissionAndOpenStorage()
        }

        binding.btnSpecConfirm.setOnClickListener {
            if (fileViewModel.fileUri.value != null) {
                val file = FileUtil().getFileFromUri(requireContext()
                    , Uri.parse(fileViewModel.fileUri.value)
                    , FileUtil().getFileName(requireContext(), Uri.parse(fileViewModel.fileUri.value))!!.substringBefore(".")
                    , "pdf")
                loadingDialog.show()
                fileViewModel.getOcrContent(file)
            } else {
                showCustomToast("명세서 파일을 등록해주세요!")
            }
        }
    }

    private fun initObserver() {
        fileViewModel.toastMsg.observe(viewLifecycleOwner, {
            if (it == "PDF_PARSE_ERROR") {
                showCustomToast("명세서 내용을 확인하지 못했습니다.")
                loadingDialog.dismiss()
            }
        })

        fileViewModel.patentContent.observe(viewLifecycleOwner, {
            it?.let {
                loadingDialog.dismiss()
                findNavController().navigate(FileUploadFragmentDirections.actionFileUploadFragmentToPatentContentFragment(-1, "upload"))
            }
        })

        fileViewModel.fileUri.observe(viewLifecycleOwner, {
            it?.let { setInfo(Uri.parse(it)) }
        })
    }

    private fun setDialogSizeOver() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_size_over, null)
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

        val dlBtnYes = dialogView.findViewById<Button>(R.id.dl_btn_yes)

        dlBtnYes.setOnClickListener {
            dialogBuilder.dismiss()
        }
    }

    private fun setInfo(uri: Uri) {
        val fileName = FileUtil().getFileName(requireContext(), uri)
        val fileSize = FileUtil().getFileSize(requireContext(), uri)
        binding.ivPdf.visibility = View.VISIBLE
        binding.ivFileUpload.visibility = View.INVISIBLE
        binding.tvFileName.visibility = View.VISIBLE
        binding.tvFileSize.visibility = View.VISIBLE
        binding.tvFileName.text = fileName
        binding.tvFileSize.text = FileUtil().formatFileSize(fileSize)
    }

    private fun backstack() {
        findNavController().popBackStack()
        fileViewModel.setFileUri(null)
        fileViewModel.setPatentContent(null)
        binding.ivFileUpload.visibility = View.VISIBLE
        binding.tvFileName.visibility = View.INVISIBLE
        binding.tvFileSize.visibility = View.INVISIBLE
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String, value: String) =
            FileUploadFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
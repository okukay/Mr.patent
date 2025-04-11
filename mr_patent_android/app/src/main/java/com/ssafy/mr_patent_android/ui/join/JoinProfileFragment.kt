package com.ssafy.mr_patent_android.ui.join

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentJoinProfileBinding
import com.ssafy.mr_patent_android.util.FileUtil
import com.ssafy.mr_patent_android.util.ImageCompressor
import com.ssafy.mr_patent_android.util.ImagePicker
import com.ssafy.mr_patent_android.util.ImageUtil
import com.ssafy.mr_patent_android.util.LoadingDialog
import gun0912.tedimagepicker.builder.TedImagePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

private const val TAG = "JoinProfileFragment_Mr_Patent"
class JoinProfileFragment : BaseFragment<FragmentJoinProfileBinding>(
    FragmentJoinProfileBinding::bind, R.layout.fragment_join_profile
) {

    private val joinViewModel: JoinViewModel by activityViewModels()
    private lateinit var imagePickerUtil: ImagePicker
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
//        imagePickerUtil = ImagePicker(this) { uri ->
//            if (FileUtil().isFileSizeValid(requireContext(), uri) == false) {
//                setDialogSizeOver()
//                return@ImagePicker
//            }
//
//            val extension = FileUtil().getFileExtension(requireContext(), uri)
//            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver,  uri)
//
//            when (extension) {
//                "jpg", "jpeg" -> {
//                    loadingDialog.show()
//                    val rotatedBitmap = ImageUtil().rotateBitmapIfNeeded(requireContext(), bitmap, uri)
//                    val rotatedUri = ImageUtil().bitmapToUri(requireContext(), rotatedBitmap, extension)
//                    handleImageJpgSelected(rotatedUri, extension)
//                }
//                "png" -> {
//                    handleImagePngSelected(bitmap, extension)
//                }
//            }
//        }

        binding.tvBefore.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnPass.setOnClickListener {
            if (joinViewModel.userRole.value == 0)
                findNavController().navigate(R.id.nav_joinMemberFragment)
            else {
                findNavController().navigate(R.id.nav_joinExpertFragment)
            }
            joinViewModel.setUserImage("")
        }

        binding.btnNext.setOnClickListener {
            if (joinViewModel.userRole.value == 0) {
                findNavController().navigate(R.id.nav_joinMemberFragment)
            } else {
                findNavController().navigate(R.id.nav_joinExpertFragment)
            }
            if (joinViewModel.userImage.value == requireContext().resources.getString(R.string.default_image)
                || joinViewModel.userImage.value == null) {
                joinViewModel.setUserImage("")
            }
        }

        binding.ivProfile.setOnClickListener {
//            imagePickerUtil.checkPermissionAndOpenGallery()
            setTedImagePicker()
        }

        binding.tvProfileRegister.setOnClickListener {
//            imagePickerUtil.checkPermissionAndOpenGallery()
            setTedImagePicker()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
    }

    private fun initObserver() {
        joinViewModel.userImage.observe(viewLifecycleOwner, {
            Log.d(TAG, "initObserver: userImage: ${joinViewModel.userImage.value}")
            Glide.with(requireContext())
                .load(it)
                .fallback(R.drawable.user_profile)
                .error(R.drawable.image_load_error_icon)
                .circleCrop()
                .into(binding.ivProfile)
        })
    }

    fun setTedImagePicker() {
        TedImagePicker.with(requireContext()).start { uri ->
            if (FileUtil().isFileSizeValid(requireContext(), uri) == false) {
                setDialogSizeOver()
                return@start
            }

            val extension = FileUtil().getFileExtension(requireContext(), uri)
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver,  uri)
            when (extension) {
                "jpg", "jpeg" -> {
                    loadingDialog.show()
                    val rotatedBitmap = ImageUtil().rotateBitmapIfNeeded(requireContext(), bitmap, uri)
                    val rotatedUri = ImageUtil().bitmapToUri(requireContext(), rotatedBitmap, extension)
                    handleImageJpgSelected(rotatedUri, extension)
                    handleImageJpgSelected(uri, extension)
                }
                "png" -> {
                    handleImagePngSelected(bitmap, extension)
                }
            }
        }
    }

    private fun handleImageJpgSelected(uri: Uri, extension: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            ImageCompressor(requireContext()).compressImage(uri, 500 * 1024)?.let { bytes ->
                val compressedUri = byteArrayToUri(bytes, extension)
                joinViewModel.setUserImage(compressedUri.toString())
                loadingDialog.dismiss()
            } ?: run {
                showCustomToast("이미지 처리 실패")
            }
        }
    }

    private fun handleImagePngSelected(bitmap: Bitmap, extension: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val resizedBitmap = ImageUtil().resizeBitmap(bitmap, 600, 600)
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val bytes = outputStream.toByteArray()
            val compressedUri = byteArrayToUri(bytes, extension)
            joinViewModel.setUserImage(compressedUri.toString())
        }
    }

    private suspend fun byteArrayToUri(byteArray: ByteArray, extension: String): Uri {
        return withContext(Dispatchers.IO) {
            val fileName = "compressed_${System.currentTimeMillis()}.$extension"
            val file = File(requireContext().cacheDir, fileName)
            file.writeBytes(byteArray)
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            uri
        }
    }

    private fun setDialogSizeOver() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_size_over, null)
        val overTextView = dialogView.findViewById<TextView>(R.id.tv_text2)
        overTextView.setText("5mb 이상의 이미지는 업로드가 불가능해요.")
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

    companion object {
        @JvmStatic
        fun newInstance(key: String, value: String) =
            JoinProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
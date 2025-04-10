package com.ssafy.mr_patent_android.ui.mypage

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
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.data.model.dto.ProfileEditRequest
import com.ssafy.mr_patent_android.databinding.FragmentProfileEditBinding
import com.ssafy.mr_patent_android.ui.address.AddressViewModel
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

private const val TAG = "ProfileEditFragment_Mr_Patent"
class ProfileEditFragment : BaseFragment<FragmentProfileEditBinding>(
    FragmentProfileEditBinding::bind, R.layout.fragment_profile_edit
) {
    private var isExpanded = mutableListOf(false, false, false, false, false)
    private val args: ProfileEditFragmentArgs by navArgs()
    private var flag = false

    private lateinit var imagePickerUtil: ImagePicker
    private lateinit var imageUri: Uri
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var contentType: String

    private val profileEditViewModel : ProfileEditViewModel by activityViewModels()
    private val addressViewModel : AddressViewModel by activityViewModels()

    private lateinit var profileEditRequest : ProfileEditRequest

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

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        })

        binding.tvEditProfile.setOnClickListener {
//            imagePickerUtil.checkPermissionAndOpenGallery()
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

        binding.clProfileEditCategoryExpert.setOnClickListener {
            toggleLayout(isExpanded[0], binding.ivToggle1, binding.hsvFilter)
            isExpanded[0] = !isExpanded[0]
        }

        binding.clProfileEditDescriptionExpert.setOnClickListener {
            toggleLayout(isExpanded[1], binding.ivToggle2, binding.etDescription)
            isExpanded[1] = !isExpanded[1]
        }

        binding.clProfileEditAddressExpert.setOnClickListener {
            toggleLayout(isExpanded[2], binding.ivToggle3, binding.clEtAddress)
            isExpanded[2] = !isExpanded[2]
        }

        binding.clProfileEditPhoneExpert.setOnClickListener {
            toggleLayout(isExpanded[3], binding.ivToggle4, binding.etPhone)
            isExpanded[3] = !isExpanded[3]
        }

        binding.clProfileEditItemsMember.setOnClickListener {
            toggleLayout(isExpanded[4], binding.ivToggle, binding.etName)
            isExpanded[4] = !isExpanded[4]
        }

        binding.ivSearch.setOnClickListener {
            profileEditViewModel.setEditDescription(binding.etDescription.text.toString())
            profileEditViewModel.setEditPhone(binding.etPhone.text.toString())
            profileEditViewModel.setEditAddress2(binding.etAddress2.text.toString())
            profileEditViewModel.setIsExpanded(
                mutableListOf(isExpanded[0], isExpanded[1], isExpanded[2], isExpanded[3])
            )
            findNavController().navigate(R.id.addressSearchFragment)
        }

        binding.cgFilter.setOnCheckedStateChangeListener { group, index ->
            if (index.isEmpty()) {
                group.check(group.tag as? Int ?: R.id.chip_chemi)
            } else {
                group.tag = index.first()
            }
        }

        binding.btnEdit.setOnClickListener {
            loadingDialog.show()
            profileEditRequest = ProfileEditRequest(null, null, null, null, null, null)

            when (args.role) {
                "member" -> {
                    if (binding.etName.text.toString() != profileEditViewModel.memberInfo.value?.userName) {
                        if (binding.etName.text.isNotBlank() && binding.etName.text.toString().length in (3..12)) {
                            profileEditRequest.userName = binding.etName.text.toString()
                        } else {
                            showCustomToast("이름이 형식에 맞지 않습니다.")
                        }
                    }
                }

                "expert" -> {
                    if (binding.etDescription.text.toString() != profileEditViewModel.expertInfo.value?.expertDescription) {
                        if (binding.etDescription.text.isNotBlank() && binding.etDescription.text.toString().length in (1..500)) {
                            profileEditRequest.expertDescription = binding.etDescription.text.toString()
                        } else {
                            showCustomToast("소개글이 형식에 맞지 않습니다.")
                        }
                    }

                    if (binding.etPhone.text.toString() != profileEditViewModel.expertInfo.value?.expertPhone) {
                        if (binding.etPhone.text.isNotBlank() && binding.etPhone.text.toString().length in (9..11)) {
                            profileEditRequest.expertPhone = binding.etPhone.text.toString()
                        } else {
                            showCustomToast("전화번호가 형식에 맞지 않습니다.")
                        }
                    }

                    val addressChanged1 = binding.etAddress1.text.toString() != profileEditViewModel.expertInfo.value?.expertAddress?.substringBefore("\\")
                    val addressChanged2 = binding.etAddress2.text.toString() != profileEditViewModel.expertInfo.value?.expertAddress?.substringAfter("\\")
                    Log.d(TAG, "initView: et1: ${binding.etAddress1.text}")
                    Log.d(TAG, "initView: et2: ${binding.etAddress2.text}")
                    if (addressChanged1 || addressChanged2) {
                        if (binding.etAddress2.text.isBlank()) {
                            profileEditRequest.expertAddress = binding.etAddress1.text.toString()
                        } else {
                            val address = binding.etAddress1.text.toString() + "\\" + binding.etAddress2.text.toString()
                            profileEditRequest.expertAddress = address
                        }
                    }

                    val newCategories = mutableListOf<ProfileEditRequest.Category>()
                    for (i in 0 until binding.cgFilter.childCount) {
                        val chip = binding.cgFilter.getChildAt(i) as Chip
                        if (chip.isChecked) {
                            val category = ProfileEditRequest.Category(chip.text.toString())
                            newCategories.add(category)
                        }
                    }

                    if (newCategories.toSet() != profileEditViewModel.expertInfo.value?.expertCategory?.toSet()) {
                        profileEditRequest.expertCategories = newCategories
                    }
                }
            }

            lifecycleScope.launch {
                if (profileEditViewModel.profileImage.value != profileEditViewModel.currentImage.value
                    && profileEditViewModel.currentImage.value != null) {
                    val fileUri = Uri.parse(profileEditViewModel.currentImage.value)
                    val fileName = FileUtil().getFileName(requireContext(), fileUri)
                    val extension = FileUtil().getFileExtension(requireContext(), Uri.parse(profileEditViewModel.currentImage.value))
                    if (extension == "jpg" || extension == "jpeg") {
                        contentType = "image/jpeg"
                    } else {
                        contentType = "image/png"
                    }
                    profileEditRequest.userImage = fileName
                    profileEditViewModel.uploadFile(requireContext(), Uri.parse(profileEditViewModel.currentImage.value!!), fileName!!, extension!!, contentType)
                }

                val isEdited = profileEditRequest.userName != null
                        || profileEditRequest.userImage != null
                        || profileEditRequest.expertDescription != null
                        || profileEditRequest.expertAddress != null
                        || profileEditRequest.expertPhone != null
                        || profileEditRequest.expertCategories != null
                if (isEdited) {
                    Log.d(TAG, "initView: address: ${profileEditRequest.expertAddress}")
                    profileEditViewModel.editUserInfo(profileEditRequest)
                } else {
                    loadingDialog.dismiss()
                    showCustomToast("변경 사항이 없습니다.")
                }
            }
        }
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

    private fun initObserver() {
        profileEditViewModel.toastMsg.observe(viewLifecycleOwner) {
            if (it == "회원정보가 성공적으로 수정되었습니다.") {
                loadingDialog.dismiss()
                showCustomToast(it)
                profileEditViewModel.clearToastMsg()
                findNavController().popBackStack()
            }
        }

        profileEditViewModel.editDescription.observe(viewLifecycleOwner, {
            binding.etDescription.setText(it)
        })

        profileEditViewModel.editPhone.observe(viewLifecycleOwner, {
            binding.etPhone.setText(it)
        })

        profileEditViewModel.editAddress1.observe(viewLifecycleOwner, {
            binding.etAddress1.setText(it)
        })

        profileEditViewModel.editAddress2.observe(viewLifecycleOwner, {
            binding.etAddress2.setText(it)
        })

        profileEditViewModel.memberInfo.observe(viewLifecycleOwner) {
            when (args.role) {
                "member" -> {
                    binding.etName.setText(it.userName)
                    binding.clProfileEditItemsMember.visibility = View.VISIBLE
                    binding.clProfileEditItemsExpert.visibility = View.GONE
                }
                "expert" -> {
                    binding.clProfileEditItemsMember.visibility = View.GONE
                    binding.clProfileEditItemsExpert.visibility = View.VISIBLE
                    for (categories in it.expertCategory) {
                        when (categories.categoryName) {
                            "화학공학" -> binding.chipChemi.isChecked = true
                            "기계공학" -> binding.chipMecha.isChecked = true
                            "생명공학" -> binding.chipLife.isChecked = true
                            "전기/전자" -> binding.chipElec.isChecked = true
                        }
                    }
                }
            }
        }

        profileEditViewModel.profileImage.observe(viewLifecycleOwner, {
            if (it != "") {
                imageUri = Uri.parse(it)
                profileEditViewModel.setCurrentImage(imageUri.toString())
            } else {
                Glide.with(requireContext())
                    .load(R.drawable.user_profile)
                    .circleCrop()
                    .into(binding.ivProfile)
            }
        })

        addressViewModel.address.observe(viewLifecycleOwner) {
            binding.etAddress1.setText(it)
            binding.etAddress2.setText(profileEditViewModel.editAddress2.value)
            binding.etDescription.setText(profileEditViewModel.editDescription.value)
            binding.etPhone.setText(profileEditViewModel.editPhone.value)
        }

        profileEditViewModel.currentImage.observe(viewLifecycleOwner, {
            Log.d(TAG, "initObserver: currentImage: ${profileEditViewModel.currentImage.value}")
            Glide.with(requireContext())
                .load(it)
                .fallback(R.drawable.user_profile) // null
                .error(R.drawable.image_load_error_icon)
                .circleCrop()
                .into(binding.ivProfile)
        })

        profileEditViewModel.isExpanded.observe(viewLifecycleOwner, {
            isExpanded = it
            toggleLayout(isExpanded[0], binding.ivToggle1, binding.hsvFilter)
            toggleLayout(isExpanded[1], binding.ivToggle2, binding.etDescription)
            toggleLayout(isExpanded[2], binding.ivToggle3, binding.clEtAddress)
            toggleLayout(isExpanded[3], binding.ivToggle4, binding.etPhone)
            for (i in 0 until 4) {
                isExpanded[i] = !isExpanded[i]
            }
        })
    }

    private fun handleImageJpgSelected(uri: Uri, extension: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            ImageCompressor(requireContext()).compressImage(uri, 500 * 1024)?.let { bytes ->
                val compressedUri = byteArrayToUri(bytes, extension)
                profileEditViewModel.setCurrentImage(compressedUri.toString())
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
            profileEditViewModel.setCurrentImage(compressedUri.toString())
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
            ProfileEditFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
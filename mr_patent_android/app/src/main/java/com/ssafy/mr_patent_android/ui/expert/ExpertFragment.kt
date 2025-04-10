package com.ssafy.mr_patent_android.ui.expert

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.data.model.dto.UserDto
import com.ssafy.mr_patent_android.databinding.DialogChatBinding
import com.ssafy.mr_patent_android.databinding.FragmentExpertBinding
import com.ssafy.mr_patent_android.ui.chat.ChatFragmentArgs
import com.ssafy.mr_patent_android.ui.mypage.ProfileEditViewModel

private const val TAG = "ExpertFragment_Mr_Patent"
class ExpertFragment :
    BaseFragment<FragmentExpertBinding>(FragmentExpertBinding::bind, R.layout.fragment_expert) {
    val viewModel: ExpertViewModel by viewModels()
    val profileEditViewModel : ProfileEditViewModel by activityViewModels()
    val expert_id by lazy {
        navArgs<ExpertFragmentArgs>().value.expertId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initObserver()

    }

    fun initView() {
        viewModel.getExpert(expert_id)
        
        if (sharedPreferences.getUser().userRole == 1) {
            binding.btnEditProfile.visibility = View.VISIBLE
            binding.btnEditProfile.setOnClickListener {
                findNavController().navigate(ExpertFragmentDirections.actionPatentAttorneyFragmentToProfileEditFragment(
                    "expert", expert_id
                ))
            }
            binding.fabChat.visibility = View.GONE
        } else {
            binding.fabChat.setOnClickListener {
                initDialog()
            }
        }

        binding.tvBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    fun initObserver() {
        viewModel.expert.observe(viewLifecycleOwner) {
            setInfo(it)
        }

        profileEditViewModel.memberInfo.observe(viewLifecycleOwner, {
            setInfo(it)
        })
    }

    private fun setInfo(it: UserDto) {
        binding.tvName.text = it.userName
        binding.tvIntro.text = it.expertDescription
        binding.tvInfo.text = it.expertGetDate
        binding.tvPhone.text = it.expertPhone
        val address = it.expertAddress
        if (address.contains("\\")) {
            binding.tvAddress.text = address.substringBefore("\\").plus(" ").plus(address.substringAfter("\\"))
        } else {
            binding.tvAddress.text = address
        }


        if (it.expertCategory.isNotEmpty()) {
            it.expertCategory.forEach {
                when (it.categoryName) {
                    "기계공학" -> binding.tvFieldMecha.visibility = View.VISIBLE
                    "전기/전자" -> binding.tvFieldElec.visibility = View.VISIBLE
                    "화학공학" -> binding.tvFieldChemi.visibility = View.VISIBLE
                    "생명공학" -> binding.tvFieldLife.visibility = View.VISIBLE
                }
            }
        }

        Glide
            .with(binding.root)
            .load(sharedPreferences.getUser().userImage)
            .circleCrop()
            .placeholder(R.drawable.user_profile)
            .into(binding.ivProfile)
    }

    fun initDialog() {
        val dialog = Dialog(requireContext())

        val dialogBinding = DialogChatBinding.inflate(layoutInflater)

        dialogBinding.tvName.text = viewModel.expert.value?.userName

        dialogBinding.dlBtnYes.setOnClickListener {
            val expert= viewModel.expert.value!!
            viewModel.startChat(expert.userId)
            viewModel.roomId.observe(viewLifecycleOwner) {
                Log.d(TAG, "initDialog: $it")
                if (it != null) {
                    findNavController().navigate(
                        ExpertFragmentDirections.actionPatentAttorneyFragmentToChatFragment(
                            expert.userId, expert.expertId, it, expert.userName, expert.userImage)
                    )
                }else {
                    showCustomToast("정보를 받아올 수 없습니다.")
                }
            }
            dialog.dismiss()
        }
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialogBinding.dlBtnNo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.show()
    }
}
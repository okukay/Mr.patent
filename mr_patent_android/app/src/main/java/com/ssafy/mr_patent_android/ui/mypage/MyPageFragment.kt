package com.ssafy.mr_patent_android.ui.mypage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentMyPageBinding
import com.ssafy.mr_patent_android.ui.login.LoginActivity

private const val TAG = "MyPageFragment_Mr_Patent"
class MyPageFragment : BaseFragment<FragmentMyPageBinding>(
    FragmentMyPageBinding::bind, R.layout.fragment_my_page
) {
    private val userLeaveViewModel : UserLeaveViewModel by activityViewModels()
    private val profileEditViewModel : ProfileEditViewModel by activityViewModels()

    private lateinit var imageUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initObserver()
    }

    private fun initView() {
        binding.tvUserName.text = sharedPreferences.getUser().userName + "님"

        if (sharedPreferences.getUser().userRole == 0) {
            binding.tvProfileUpdate.text = "> 프로필 수정"
        } else {
            binding.tvProfileUpdate.text = "> 프로필 보기"
        }

        Glide.with(requireContext())
            .load(sharedPreferences.getUser().userImage)
            .circleCrop()
            .into(binding.ivProfile)

        binding.tvProfileUpdate.setOnClickListener {
            when (sharedPreferences.getUser().userRole) {
                0 -> {
                    findNavController().navigate(MyPageFragmentDirections.actionNavFragmentMypageToProfileEditFragment(
                        "member", sharedPreferences.getUser().userId
                    ))
                }
                1-> {
                    if (profileEditViewModel.expertId.value != null) {
                        findNavController().navigate(MyPageFragmentDirections.actionNavFragmentMypageToPatentAttorneyFragment2(profileEditViewModel.expertId.value!!))
                    } else {
                        showCustomToast("잠시 후에 다시 시도해주세요.")
                    }
                }
            }
        }

        // 특허 분석 리포트
        binding.tvPatentReports.setOnClickListener {
            findNavController().navigate(R.id.patentFolderFragment)
        }

        // 알림 설정
        // TODO

        binding.llPasswordChange.setOnClickListener {
            findNavController().navigate(R.id.pwdEditFragment)
        }

        binding.llLogout.setOnClickListener {
            userLeaveViewModel.clearToken()
            userLeaveViewModel.logout()
        }

        binding.llLeave.setOnClickListener {
            findNavController().navigate(R.id.userDeleteFragment)
        }
    }

    private fun initObserver() {
        userLeaveViewModel.toastMsg.observe(viewLifecycleOwner) {
            if (it == "로그아웃 되었습니다.") {
                showCustomToast(it)
                sharedPreferences.clearToken()
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()
            }
        }

        profileEditViewModel.profileImage.observe(viewLifecycleOwner, {
            Log.d(TAG, "initObserver: profileImage: ${it}")
            if (it != null) {
                Glide.with(requireContext())
                    .load(it)
                    .circleCrop()
                    .into(binding.ivProfile)
            } else {
                Glide.with(requireContext())
                    .load(R.drawable.user_profile)
                    .circleCrop()
                    .into(binding.ivProfile)
            }
        })

        profileEditViewModel.memberInfo.observe(viewLifecycleOwner, {
            Log.d(TAG, "initObserver: userImage: ${it.userImage}")
            profileEditViewModel.getImage(it.userImage)
        })
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String, value: String) =
            MyPageFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
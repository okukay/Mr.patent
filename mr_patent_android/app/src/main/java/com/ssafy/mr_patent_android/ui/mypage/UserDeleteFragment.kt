package com.ssafy.mr_patent_android.ui.mypage

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentUserDeleteBinding
import com.ssafy.mr_patent_android.ui.login.LoginActivity

class UserDeleteFragment : BaseFragment<FragmentUserDeleteBinding>(
    FragmentUserDeleteBinding::bind, R.layout.fragment_user_delete
) {
    private val userRemoveViewModel: UserLeaveViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initObserver()
    }

    private fun initView() {
        binding.btnUserDelete.setOnClickListener {
            userRemoveViewModel.deleteUser()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun initObserver() {
        userRemoveViewModel.toastMsg.observe(viewLifecycleOwner) {
            if (it == "탈퇴가 완료되었습니다.") {
                showCustomToast(it)
                sharedPreferences.clearToken()
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String, value: String) =
            UserDeleteFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
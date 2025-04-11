package com.ssafy.mr_patent_android.ui.mypage

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentPwdEditBinding

class PwdEditFragment : BaseFragment<FragmentPwdEditBinding>(
    FragmentPwdEditBinding::bind, R.layout.fragment_pwd_edit
) {
    private val pwdEditViewModel : PwdEditViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initObserver()
    }

    private fun initView() {

        binding.tvBefore.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.tvNextEdit.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnPwdEdit.setOnClickListener {
            if (binding.etPwdCurrent.text.isBlank() || binding.etPwdNew.text.isBlank() || binding.etPwdNewConfirm.text.isBlank()) {
                showCustomToast("모든 항목을 입력해주세요.")
            } else if (isValidPw(binding.etPwdNew.text.toString())){
                if (binding.etPwdNew.text.toString() != binding.etPwdNewConfirm.text.toString()){
                    showCustomToast("비밀번호가 일치하지 않습니다.")
                } else {
                    pwdEditViewModel.pwdEdit(binding.etPwdCurrent.text.toString(), binding.etPwdNew.text.toString())
                }
            }
        }
    }

    private fun initObserver() {
        pwdEditViewModel.toastMsg.observe(viewLifecycleOwner) {
            showCustomToast(it)
            findNavController().popBackStack()
        }
    }

    fun isValidPw(pw: String): Boolean {
        val regex = "^[a-zA-Z0-9!@#\$%^&*()]{8,16}$".toRegex()
        val result = regex.matches(pw.trim())
        if (!result) {
            showCustomToast("비밀번호가 형식에 맞지 않습니다.")
        }
        return result
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
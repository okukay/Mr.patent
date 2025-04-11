package com.ssafy.mr_patent_android.ui.join

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentJoinFinishBinding
import com.ssafy.mr_patent_android.ui.login.LoginActivity

private const val TAG = "JoinFinishFragment_Mr_Patent"
class JoinFinishFragment : BaseFragment<FragmentJoinFinishBinding>(
    FragmentJoinFinishBinding::bind, R.layout.fragment_join_finish
) {

    private val joinViewModel : JoinViewModel by activityViewModels()

    private var role: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        role = arguments?.getString("role")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
    }

    private fun initView() {
        if (joinViewModel.userRole.value == 0) {
            binding.tvJoinFinish.text = "가입 완료!"
            binding.tvJoinFinishExp.text = joinViewModel.userName.value + "님, 환영해요 😊"
            joinViewModel.reset()
        } else if (joinViewModel.userRole.value == 1) {
            binding.tvJoinFinish.text = "가입 신청 완료!"
            binding.tvJoinFinishExp.text = "관리자의 승인까지 최대 7일이 소요됩니다 😊"
            joinViewModel.reset()
        }

        binding.btnConfirm.setOnClickListener {
            findNavController().popBackStack(R.id.nav_loginFragment, false)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().popBackStack(R.id.nav_loginFragment, false)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String, value: String) =
            JoinFinishFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
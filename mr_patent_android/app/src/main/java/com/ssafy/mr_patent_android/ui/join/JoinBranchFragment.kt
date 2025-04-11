package com.ssafy.mr_patent_android.ui.join

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentJoinBranchBinding
import com.ssafy.mr_patent_android.ui.login.LoginActivity

class JoinBranchFragment : BaseFragment<FragmentJoinBranchBinding>(
        FragmentJoinBranchBinding::bind, R.layout.fragment_join_branch
) {
    private val joinViewModel: JoinViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
    }

    private fun initView() {
        binding.tvBefore.setOnClickListener {
            findNavController().popBackStack()
            joinViewModel.setUserRole(-1)
            joinViewModel.setUserImage(requireContext().resources.getString(R.string.default_image))
        }

        binding.clGeneralPerson.setOnClickListener {
            findNavController().navigate(R.id.nav_joinProfileFragment)
            joinViewModel.setUserRole(0)
        }

        binding.clExpert.setOnClickListener {
            findNavController().navigate(R.id.nav_joinProfileFragment)
            joinViewModel.setUserRole(1)
        }

        if (joinViewModel.userRole.value == 0) {
            binding.clGeneralPerson.setBackgroundResource(R.drawable.rounded_background_stroke_active)
            binding.clExpert.setBackgroundResource(R.drawable.rounded_background)
        } else if (joinViewModel.userRole.value == 1) {
            binding.clGeneralPerson.setBackgroundResource(R.drawable.rounded_background)
            binding.clExpert.setBackgroundResource(R.drawable.rounded_background_stroke_active)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            joinViewModel.setUserRole(-1)
            joinViewModel.setUserImage(requireContext().resources.getString(R.string.default_image))
            findNavController().popBackStack()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String, value: String) =
            JoinBranchFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
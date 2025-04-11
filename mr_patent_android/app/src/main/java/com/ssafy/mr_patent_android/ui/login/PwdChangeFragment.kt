package com.ssafy.mr_patent_android.ui.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.data.model.dto.PwdChangeRequest
import com.ssafy.mr_patent_android.databinding.FragmentPwdChangeBinding


class PwdChangeFragment : Fragment() {
    lateinit var binding: FragmentPwdChangeBinding
    val viewModel : PwdChangeViewModel by viewModels()
    var email = ""
    var code = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPwdChangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args: PwdChangeFragmentArgs by navArgs()
        email = args.email
        code = args.code

        initView()
        initObserver()

    }

    fun initView() {
        binding.etPwd.addTextChangedListener {
            if(binding.etPwd.text.toString().length >= 8){
                if(binding.etPwd.text.toString() == binding.etPwdConfirm.text.toString()){
                    binding.btnChabgePwd.isEnabled = true
                } else {
                    binding.btnChabgePwd.isEnabled = false
                }
            }
        }

        binding.etPwdConfirm.addTextChangedListener {
            if(binding.etPwd.text.toString().length >= 8){
                if(binding.etPwd.text.toString() == binding.etPwdConfirm.text.toString()){
                    binding.btnChabgePwd.isEnabled = true
                } else {
                    binding.etPwdConfirm.error = "비밀번호가 일치하지 않습니다."
                    binding.btnChabgePwd.isEnabled = false
                }
            }
        }


        binding.btnChabgePwd.setOnClickListener {
            viewModel.changePwd(PwdChangeRequest(email, null, binding.etPwd.text.toString()))
        }

    }

    fun initObserver() {
        viewModel.changeState.observe(viewLifecycleOwner, {
            if(it) {
                Toast.makeText(requireContext(), "비밀번호 변경 성공", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.nav_loginFragment)
            } else {
                Toast.makeText(requireContext(), "비밀번호 변경 실패", Toast.LENGTH_SHORT).show()
            }
        })

    }
}
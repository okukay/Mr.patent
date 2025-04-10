package com.ssafy.mr_patent_android.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.ssafy.mr_patent_android.MainActivity
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentLoginBinding

private const val TAG = "LoginFragment"
class LoginFragment : BaseFragment<FragmentLoginBinding>(FragmentLoginBinding::bind, R.layout.fragment_login) {
    val viewModel: LoginViewModel by viewModels()
    var emailFlag= false
    var pwdFlag= false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObserver()

    }
    fun initView(){
        binding.tvPwdForgot.setOnClickListener {
            // 비밀번호 찾기
            findNavController().navigate(R.id.nav_emailVerifyFragment)
        }

        binding.tvSignup.setOnClickListener {
            // 회원가입
            findNavController().navigate(R.id.nav_joinBranchFragment)
        }

        binding.btnLogin.setOnClickListener {
            if(checkEmail(binding.etEmail.text.toString()) && binding.etPwd.text.length >=2){
                viewModel.login(binding.etEmail.text.toString(), binding.etPwd.text.toString())
            }


        }

        viewModel.toast.observe(viewLifecycleOwner, {
            it?.let {
                showCustomToast(it)
            }
        })

        binding.etEmail.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                if (checkEmail(binding.etEmail.text.toString())) {
                    emailFlag = true
                } else {
                    binding.etEmail.error = "이메일 형식이 아닙니다."
                }
            }
        }
        binding.etPwd.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                if (binding.etPwd.text.length <2) {
                    binding.etPwd.error = "6자 이상 입력해주세요."
                }
            }
        }

        binding.etEmail.addTextChangedListener {
            if (checkEmail(binding.etEmail.text.toString())) {
                emailFlag = true
            } else {
                emailFlag = false
            }
            if (emailFlag && pwdFlag) {
                binding.btnLogin.isEnabled = true
            } else {
                binding.btnLogin.isEnabled = false
            }
        }

        binding.etPwd.addTextChangedListener {
            if (binding.etPwd.text.length >= 2) {
                pwdFlag = true
            } else {
                pwdFlag = false
            }
            if (emailFlag && pwdFlag) {
                binding.btnLogin.isEnabled = true
            } else {
                binding.btnLogin.isEnabled = false
            }
        }

}
    fun initObserver(){
        viewModel.loginState.observe(viewLifecycleOwner, {
            if (it){
                // 로그인
                Log.d(TAG, "로그인 성공")

                initFcm()
                val intent= Intent(requireContext(), MainActivity::class.java)
                intent.flags= Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        })
    }

    private fun initFcm(){

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {

                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result

            viewModel.sendFcmToken(token)
            Log.d(TAG, "FCM 토큰: $token")
        })
    }

    fun checkEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

}
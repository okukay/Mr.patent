package com.ssafy.mr_patent_android.ui.join

import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentJoinMemberBinding
import com.ssafy.mr_patent_android.ui.login.EmailVerifyViewModel
import com.ssafy.mr_patent_android.ui.login.LoginActivity
import com.ssafy.mr_patent_android.util.FileUtil
import kotlinx.coroutines.launch
import kotlin.math.log

private const val TAG = "JoinMemberFragment_Mr_Patent"
class JoinMemberFragment : BaseFragment<FragmentJoinMemberBinding>(
    FragmentJoinMemberBinding::bind, R.layout.fragment_join_member
) {
    private val joinViewModel : JoinViewModel by activityViewModels()
    private val emailVerifyViewModel : EmailVerifyViewModel by viewModels()

    private var emailFlag : Boolean = false
    private var timer: CountDownTimer? = null

    private lateinit var contentType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initObserver()
    }

    private fun initView() {
        joinViewModel.setToastMsg(null)
        binding.tvBefore.setOnClickListener {
            if (joinViewModel.userImage.value == "") {
                joinViewModel.setUserImage(requireContext().resources.getString(R.string.default_image))
            }
            emailVerifyViewModel.reset()
            findNavController().popBackStack()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (joinViewModel.userImage.value == "") {
                joinViewModel.setUserImage(requireContext().resources.getString(R.string.default_image))
            }
            emailVerifyViewModel.reset()
            findNavController().popBackStack()
        }

        binding.btnJoin.setOnClickListener {
            if (emailFlag
                && isFillInput()
                && isValidName(binding.etName.text.toString())
                && isValidPw(binding.etPw.text.toString())) {
                if (joinViewModel.userImage.value != "") {
                    lifecycleScope.launch {
                        var fileUri = Uri.parse(joinViewModel.userImage.value)
                        val fileName = FileUtil().getFileName(requireContext(), fileUri)
                        val extension = FileUtil().getFileExtension(requireContext(), fileUri)
                        if (extension == "jpg" || extension == "jpeg") {
                            contentType = "image/jpeg"
                        } else {
                            contentType = "image/png"
                        }
                        joinViewModel.uploadFile(requireContext(), fileUri, fileName!!, extension!!, contentType)
                        joinViewModel.joinMember(binding.etEmail.text.toString(), binding.etName.text.toString(), binding.etPw.text.toString(), fileName)
                    }
                } else {
                    joinViewModel.joinMember(binding.etEmail.text.toString(), binding.etName.text.toString(), binding.etPw.text.toString(), "")
                }
            }
        }

        binding.btnEmailDupl.setOnClickListener {
            if(binding.etEmail.text.toString().isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(binding.etEmail.text.toString()).matches()) {
                showCustomToast("이메일을 확인해주세요.")
            } else {
                joinViewModel.checkDuplEmail(binding.etEmail.text.toString())
            }
        }

        binding.btnVerificationSend.setOnClickListener {
            emailVerifyViewModel.sendCode((binding.etEmail.text.toString()))
            binding.btnVerificationSend.isEnabled = false
        }

        binding.btnVerify.setOnClickListener {
            emailVerifyViewModel.emailVerify(binding.etEmail.text.toString(), binding.etCode.text.toString())
        }
    }

    private fun initObserver() {
        joinViewModel.toastMsg.observe(viewLifecycleOwner, {
          it?.let { showCustomToast(it) }
        })

        emailVerifyViewModel.toastMsg.observe(viewLifecycleOwner, {
            it?.let { showCustomToast(it) }
        })

        joinViewModel.checkDuplEmail.observe(viewLifecycleOwner,{
            if (it == true) {
                binding.tvIdDupl.visibility = View.VISIBLE
                binding.btnVerificationSend.visibility = View.GONE
            } else if (it == false) {
                binding.btnVerificationSend.visibility = View.VISIBLE
                binding.clVerify.visibility = View.VISIBLE
                binding.tvIdDupl.visibility = View.GONE
                binding.btnVerificationSend.isEnabled = true
            }
        })

        emailVerifyViewModel.codeState.observe(viewLifecycleOwner, {
            if(it) {
                startTimer()
                binding.btnVerificationSend.isEnabled = false
                binding.etCode.isEnabled = true
                showCustomToast("코드 전송 성공")

                binding.etCode.addTextChangedListener {
                    val isEnable = binding.etCode.text.toString().length == 6
                    binding.btnVerify.isEnabled = isEnable
                }
            } else {
                binding.btnVerificationSend.isEnabled = true
                binding.etCode.isEnabled = false
                binding.btnVerify.isEnabled = false
            }
        })

        emailVerifyViewModel.emailVerifyState.observe(viewLifecycleOwner, {
            if(it) {
                emailFlag = true
                binding.btnVerify.isEnabled = false
                stopTimer()
            } else {
            }
        })

        joinViewModel.joinState.observe(viewLifecycleOwner, {
            it?.let {
                if(it) {
                    findNavController().navigate(R.id.nav_joinFinishFragment)
                    joinViewModel.setJoinState(false)
                }
            }
        })
    }

    fun startTimer() {
        binding.tvTime.visibility = View.VISIBLE
        timer = object : CountDownTimer(600000, 1000) {
            override fun onTick(time: Long) {
                binding.tvTime.text = "남은 시간: ${time/60000}분 ${(time%60000) / 1000}초"
            }

            override fun onFinish() {
                showCustomToast("인증 시간이 만료되었습니다. 다시 시도해주세요.")
                binding.tvTime.text=""
                emailVerifyViewModel.setCodeState(false)
            }
        }
        timer?.start()
    }

    fun stopTimer() {
        timer?.cancel()
        binding.tvTime.text = ""
        timer = null
    }

    fun isFillInput(): Boolean {
        val result = binding.etEmail.text.toString().isNotBlank()
                && binding.etName.text.toString().isNotBlank()
                && binding.etPw.text.toString().isNotBlank()

        if (result) {
            return true
        } else {
            showCustomToast("빈 칸을 입력해주세요.")
            return false
        }
    }

    fun isValidName(name: String): Boolean {
        val regex = "^[a-zA-Z가-힣]{3,12}$".toRegex()
        val result = regex.matches(name)
        if (!result) {
            showCustomToast("이름이 형식에 맞지 않습니다.")
        }
        return regex.matches(name)
    }

    fun isValidPw(pw: String): Boolean {
        val regex = "^[a-zA-Z0-9!@#\$%^&*()]{8,16}$".toRegex()
        val result = regex.matches(pw)
        if (!result) {
            showCustomToast("비밀번호가 형식에 맞지 않습니다.")
        }
        return result
    }

    override fun onDestroyView() {
        timer?.cancel()
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance(key : String, value : String) =
            JoinProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
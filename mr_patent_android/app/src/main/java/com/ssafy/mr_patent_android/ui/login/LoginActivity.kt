package com.ssafy.mr_patent_android.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.ssafy.mr_patent_android.MainActivity
import com.ssafy.mr_patent_android.base.BaseActivity
import com.ssafy.mr_patent_android.databinding.ActivityLoginBinding

class LoginActivity : BaseActivity<ActivityLoginBinding>(ActivityLoginBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

}
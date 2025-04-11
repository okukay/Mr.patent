package com.ssafy.mr_patent_android.ui.address

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AddressViewModel : ViewModel() {
    private val _toastMsg = MutableLiveData<String>()
    val toastMsg: LiveData<String>
        get() = _toastMsg

    private val _address = MutableLiveData<String>()
    val address: LiveData<String>
        get() = _address

    fun setAddress(address: String) {
        _address.value = address
    }
}
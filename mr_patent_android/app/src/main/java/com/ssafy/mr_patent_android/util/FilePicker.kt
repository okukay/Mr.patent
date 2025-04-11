package com.ssafy.mr_patent_android.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.text.DecimalFormat

private const val TAG = "FilePicker_Mr_Patent"
class FilePicker(
    private val fragment: Fragment,
    private val onFileSelected: (Uri) -> Unit
) {
    private val requestPermissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openStorage()
        } else {
            Toast.makeText(fragment.requireContext(), "권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickFileLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            onFileSelected(it)
        }
    }

    fun checkPermissionAndOpenStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openStorage()
        } else {
            if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openStorage()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun openStorage() {
        pickFileLauncher.launch(arrayOf("application/pdf"))
    }
}
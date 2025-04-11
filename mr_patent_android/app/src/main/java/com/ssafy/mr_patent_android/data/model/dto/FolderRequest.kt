package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class FolderRequest (
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user_patent_folder_title") val userPatentFolderTitle: String
)
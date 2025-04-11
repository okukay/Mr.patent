package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class FolderDto(
    @SerializedName("folders") val folders: List<Folder>,
) {
    data class Folder(
        @SerializedName("user_patent_folder_id") val userPatentFolderId: Int,
        @SerializedName("user_patent_folder_title") val uerPatentFolderTitle: String,
        @SerializedName("created_at") val createdAt: String
    )
}
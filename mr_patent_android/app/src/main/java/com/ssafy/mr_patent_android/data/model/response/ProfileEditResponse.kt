package com.ssafy.mr_patent_android.data.model.response

import com.google.gson.annotations.SerializedName

data class ProfileEditResponse(
    @SerializedName("message") val message: String,
    @SerializedName("user_updated_at") val userUpdatedAt: String
)
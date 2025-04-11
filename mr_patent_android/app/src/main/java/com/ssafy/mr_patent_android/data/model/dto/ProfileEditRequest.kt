package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class ProfileEditRequest(
    @SerializedName("user_name") var userName: String?,
    @SerializedName("user_image") var userImage: String?,
    @SerializedName("expert_description") var expertDescription: String?,
    @SerializedName("expert_address") var expertAddress: String?,
    @SerializedName("expert_phone") var expertPhone: String?,
    @SerializedName("expert_categories") var expertCategories : MutableList<Category>?
) {
    data class Category(
        @SerializedName("category_name") val categoryName: String
    )
}
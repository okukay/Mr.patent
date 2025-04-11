package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class JoinExpertRequest(
    @SerializedName("user_email") val userEmail: String,
    @SerializedName("user_pw") val userPw: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_image") val userImage: String,
    @SerializedName("user_role") val userRole: Int,
    @SerializedName("expert_identification") val expertIdentification: String,
    @SerializedName("expert_description") val expertDescription: String,
    @SerializedName("expert_address") val expertAddress: String,
    @SerializedName("expert_phone") val expertPhone: String,
    @SerializedName("expert_get_date") val expertGetDate: String,
    @SerializedName("expert_license") val expertLicense: String,
    @SerializedName("expert_categories") val expertCategory: MutableList<Category>
) {
    data class Category(
        @SerializedName("category_name") val categoryName: String
    )
}
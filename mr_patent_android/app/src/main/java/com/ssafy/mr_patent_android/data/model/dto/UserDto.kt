package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user_email") val userEmail: String,
    @SerializedName("user_name")val userName: String,
    @SerializedName("user_role")val userRole: Int,
    @SerializedName("expert_address")val expertAddress: String,
    @SerializedName("expert_description") val expertDescription: String,
    @SerializedName("expert_get_date")val expertGetDate: String,
    @SerializedName("expert_id")val expertId: Int,
    @SerializedName("expert_license_number")val expertLicenseNumber: String,
    @SerializedName("expert_phone")val expertPhone: String,
    @SerializedName("expert_categories")val expertCategory: MutableList<Category>,
    @SerializedName("user_image")val userImage:String,
    @SerializedName("expert_created_at")val expertCreatedAt: String,
    @SerializedName("category")val category: MutableList<String> = mutableListOf(),
){

    data class Category(
        @SerializedName("category_name") val categoryName: String
    )

    constructor(userEmail: String):this(-1, userEmail, "", -1,
        "","","",
        -1,"","",  mutableListOf(), "", "")


    constructor():this(-1, "","",-1,
        "","","",
        -1,"","",  mutableListOf(), "", "")

    constructor(userId: Int, userName: String, userRole: Int):this(userId, "", userName, userRole,
        "","","",
        -1,"","",  mutableListOf(),"", "")

    constructor(userId: Int, userName: String, userRole: Int, userImage: String):this(userId, "", userName, userRole,
        "","","",
        -1,"","",  mutableListOf(),userImage, "")

    constructor(userId: Int, userName: String, userRole: Int, expertId: Int):this(userId, "", userName, userRole,
        "","","",
        expertId,"","",  mutableListOf(),"", "")

    constructor(userId: Int, userName: String, expertAddress: String, expertDescription: String, expertGetDate: String, expertId: Int, expertLicenseNumber: String, expertPhone: String, userImage: String, expertCreatedAt: String):
            this(userId,"",  userName, -1, expertAddress, expertDescription, expertGetDate, expertId, expertLicenseNumber, expertPhone, mutableListOf(),userImage,expertCreatedAt)

    constructor(userName: String,userImage: String) : this(-1, "", userName, -1, "", "", "", -1, "", "", mutableListOf(), userImage, "")

    constructor(userId: Int, userEmail: String, userName: String, userImage: String, userRole: Int)
            : this(userId, userEmail, userName, userRole, "", "", "", -1, "", "", mutableListOf(), userImage, "")

    constructor(userId: Int, userEmail: String,  userName: String, expertId: Int, expertAddress: String, expertDescription: String, expertGetDate: String, expertPhone: String, expertCreatedAt: String, userImage: String, expertCategory: MutableList<Category>)
            : this(userId, userEmail, userName, 1, expertAddress, expertDescription, expertGetDate, expertId, "", expertPhone, expertCategory, userImage, expertCreatedAt)

    // 변리사 정보 조회
    constructor(userId: Int, userEmail: String, userName: String, expertAddress: String, expertDescription: String, expertGetDate: String, expertId: Int, expertPhone: String, expertCategory: MutableList<Category>, userImage: String)
            : this(userId, userEmail, userName, 1, expertAddress, expertDescription, expertGetDate, expertId, "", expertPhone, expertCategory, userImage, "")

}
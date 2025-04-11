package com.ssafy.mr_patent_android.data.model.response

import com.google.gson.annotations.SerializedName

data class AddDraftResponse(
    @SerializedName("patent_draft_id") val patentDraftId: Int,
    @SerializedName("created_at") val createdAt: String
)
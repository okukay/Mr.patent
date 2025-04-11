package com.ssafy.mr_patent_android.data.model.response

import com.google.gson.annotations.SerializedName

data class FitnessResultResponse (
    @SerializedName("fitness_id") val fitnessId: Int,
    @SerializedName("patent_draft_id") val patentDraftId: Int,
    @SerializedName("is_corrected") val isCorrected: Int,
    @SerializedName("details") val details: FitnessContent,
    @SerializedName("created_at") val createdAt: String
) {
    data class FitnessContent(
        @SerializedName("technical_field") val technicalField: Boolean,
        @SerializedName("background") val background: Boolean,
        @SerializedName("problem") val problem: Boolean,
        @SerializedName("solution") val solution: Boolean,
        @SerializedName("effect") val effect: Boolean,
        @SerializedName("detailed") val detailed: Boolean,
        @SerializedName("summary") val summary: Boolean,
        @SerializedName("claim") val claim: Boolean,
    )
}
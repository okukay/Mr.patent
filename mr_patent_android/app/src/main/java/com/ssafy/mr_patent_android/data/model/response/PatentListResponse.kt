package com.ssafy.mr_patent_android.data.model.response

import com.google.gson.annotations.SerializedName

data class PatentListResponse (
    @SerializedName("patents") val patents: List<PatentSummaryInfo>
) {
    data class PatentSummaryInfo(
        @SerializedName("patent_draft_id") val patentDraftId: Int,
        @SerializedName("patent_draft_title") val patentDraftTitle: String,
        @SerializedName("fitness_is_corrected") val fitnessIsCorrected: Boolean,
        @SerializedName("detailed_comparison_total_score") val detailedComparisonTotalScore: Double,
        @SerializedName("created_at") val createdAt: String
    )
}
package com.ssafy.mr_patent_android.data.model.response

import com.google.gson.annotations.SerializedName

data class PatentRecentResponse(
    @SerializedName("patent_drafts") val patentDrafts: List<PatentDraft>
) {
    data class PatentDraft(
       @SerializedName("patent_draft_id") val patentDraftId: Int
        , @SerializedName("user_patent_folder_id") val userPatentFolderId: Int
        , @SerializedName("patent_draft_title") val patentDraftTitle: String
        , @SerializedName("patent_draft_technical_field") val patentDraftTechnicalField: String
        , @SerializedName("patent_draft_background") val patentDraftBackground: String
        , @SerializedName("patent_draft_problem") val patentDraftProblem: String
        , @SerializedName("patent_draft_solution") val patentDraftSolution: String
        , @SerializedName("patent_draft_effect") val patentDraftEffect: String
        , @SerializedName("patent_draft_detailed") val patentDraftDetailed: String
        , @SerializedName("patent_draft_summary") val patentDraftSummary: String
        , @SerializedName("patent_draft_claim") val patentDraftClaim: String
        , @SerializedName("created_at") val createdAt: String
        , @SerializedName("updated_at") val updatedAt: String
    )
}
package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class PatentDraftDto(
    @SerializedName("patent_draft_title") val patentDraftTitle: String
    , @SerializedName("patent_draft_technical_field") val patentDraftTechnicalField: String
    , @SerializedName("patent_draft_background") val patentDraftBackground: String
    , @SerializedName("patent_draft_problem") val patentDraftProblem: String
    , @SerializedName("patent_draft_solution") val patentDraftSolution: String
    , @SerializedName("patent_draft_effect") val patentDraftEffect: String
    , @SerializedName("patent_draft_detailed") val patentDraftDetailed: String
    , @SerializedName("patent_draft_claim") val patentDraftClaim: String
    , @SerializedName("patent_draft_summary") val patentDraftSummary: String
    , @SerializedName("user_patent_folder_id") val folderId: Int
)
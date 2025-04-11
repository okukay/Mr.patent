package com.ssafy.mr_patent_android.data.model.response

import com.google.gson.annotations.SerializedName

data class PatentContentResponse (
    @SerializedName("patent_draft_title") val patentDraftTitle: String,
    @SerializedName("patent_draft_technical_field") val patentDraftTechnicalField: String,
    @SerializedName("patent_draft_background") val patentDraftBackground: String,
    @SerializedName("patent_draft_problem") val patentDraftProblem: String,
    @SerializedName("patent_draft_solution") val patentDraftSolution: String,
    @SerializedName("patent_draft_effect") val patentDraftEffect: String,
    @SerializedName("patent_draft_detailed") val patentDraftDetailed: String,
    @SerializedName("patent_draft_summary") val patentDraftSummary: String,
    @SerializedName("patent_draft_claim") val patentDraftClaim: String,
    @SerializedName("raw_text") val rawText: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
) {
    constructor(patentDraftTitle: String, patentDraftTechnicalField: String, patentDraftBackground: String,
                patentDraftProblem: String, patentDraftSolution: String, patentDraftEffect: String,
                patentDraftDetailed: String, patentDraftClaim: String, patentDraftSummary: String, createdAt: String, updatedAt: String) :
            this(patentDraftTitle, patentDraftTechnicalField, patentDraftBackground, patentDraftProblem, patentDraftSolution,
                patentDraftEffect, patentDraftDetailed, patentDraftClaim, patentDraftSummary, "", createdAt, updatedAt)

    constructor(patentDraftTitle: String, patentDraftTechnicalField: String, patentDraftBackground: String,
                patentDraftProblem: String, patentDraftSolution: String, patentDraftEffect: String,
                patentDraftDetailed: String, patentDraftClaim: String, patentDraftSummary: String, rawText: String) :
            this(patentDraftTitle, patentDraftTechnicalField, patentDraftBackground, patentDraftProblem, patentDraftSolution,
                patentDraftEffect, patentDraftDetailed, patentDraftClaim, patentDraftSummary, rawText, "", "")

    // OCR
    constructor(patentDraftTitle: String, patentDraftTechnicalField: String, patentDraftBackground: String,
                patentDraftProblem: String, patentDraftSolution: String, patentDraftEffect: String,
                patentDraftDetailed: String, patentDraftClaim: String, patentDraftSummary: String)
    : this(patentDraftTitle, patentDraftTechnicalField, patentDraftBackground, patentDraftProblem, patentDraftSolution,
        patentDraftEffect, patentDraftDetailed, patentDraftClaim, patentDraftSummary, "", "", "")

}
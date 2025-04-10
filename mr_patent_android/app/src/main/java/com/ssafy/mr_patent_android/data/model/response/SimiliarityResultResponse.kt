package com.ssafy.mr_patent_android.data.model.response

import com.google.gson.annotations.SerializedName

data class SimiliarityResultResponse (
    @SerializedName("similiarity_id") val similiarityId: Int,
    @SerializedName("patent_draft_id") val patentDraftId: Int,
    @SerializedName("similar_patents") val similiarPatents: List<SimiliarPatent>
){
    data class SimiliarPatent(
        @SerializedName("patent_id") val patentId: Int
        , @SerializedName("patent_title") val patentTitle: String
        , @SerializedName("patent_application_number") val patentApplicationNumber: String
        , @SerializedName("similarity_score") val similiarityScore: Int
        , @SerializedName("title_score") val titleScore: Int
        , @SerializedName("summary_score") val summaryScore: Int
        , @SerializedName("claim_score") val claimScore: Int
    )
}
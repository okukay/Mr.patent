package com.ssafy.mr_patent_android.data.model.dto

data class PatentTitleDto(
    val titleExp: List<PatentTitleExpDto>
    , val titleClaim : List<PatentTitleClaimDto>
    , val titleSummary : List<PatentTitleSummaryDto>
) {
    data class PatentTitleExpDto(
        val patentDraftTitleExp: String ="발명의 명칭",
        val patentDraftTechnicalFieldExp: String = "기술분야",
        val patentDraftBackgroundExp: String = "배경기술",
        val patentDraftProblemExp: String = "해결하려는 과제",
        val patentDraftSolutionExp: String = "과제의 해결 수단",
        val patentDraftEffectExp: String = "발명의 효과",
        val patentDraftDetailedExp: String ="발명을 실시하기 위한 구체적인 내용"
    )

    data class PatentTitleClaimDto(
        val patentDraftClaim: String = "청구 범위"
    )

    data class PatentTitleSummaryDto(
        val patentDraftSummary: String = "요약"
    )
}
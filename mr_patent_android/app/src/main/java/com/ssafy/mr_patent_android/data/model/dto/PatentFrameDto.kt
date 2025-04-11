package com.ssafy.mr_patent_android.data.model.dto

data class PatentFrameDto(
    val title: String
    , val content: String
    , var isExpanded: Boolean = false
)
package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class LevelDto(
    @SerializedName("levels")val levels: List<Level>
){
    data class Level(
        @SerializedName("best_score")val best_score: Int,
        @SerializedName("accessible")val is_accessible: Boolean,
        @SerializedName("passed")val is_passed: Boolean,
        @SerializedName("level_id")val level_id: Int,
        @SerializedName("level_name")val level_name: String,
        @SerializedName("count")val count: Int=-1,
    )
}
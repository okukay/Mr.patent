package com.ssafy.mr_patent_android.ui.study

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.data.model.dto.LevelDto
import com.ssafy.mr_patent_android.databinding.FragmentLevelListBinding

class LevelListFragment : BaseFragment<FragmentLevelListBinding>(
    FragmentLevelListBinding::bind,
    R.layout.fragment_level_list
) {
    val viewModel: LevelListViewModel by viewModels()
    val bgColor = arrayOf("#E5EEFF", "#D9E5FF", "#C6D9FF", "#ACC8FF", "#9BB9F3", "#6D9AF0")
    val cardList = intArrayOf(
        R.id.card_level1,
        R.id.card_level2,
        R.id.card_level3,
        R.id.card_level4,
        R.id.card_level5,
        R.id.card_dictionary
    )


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initObserver()

    }

    fun initView() {
        viewModel.getLevels()

    }

    fun initObserver() {
        viewModel.levelList.observe(viewLifecycleOwner, {
            it?.let {
                for (i in it.levels.indices) {
                    val level = it.levels[i]
                    setLevelCard(level, i)
                }
            }
        })

    }

    fun setLevelCard(level: LevelDto.Level, index: Int) {
        val cardView = view?.findViewById<CardView>(cardList[index])
        cardView?.let {
            val card = it.findViewById<CardView>(cardList[index])
            val titleTv = it.findViewById<TextView>(R.id.tv_level_title)
            val descTv = it.findViewById<TextView>(R.id.tv_level_desc)
            val lockIcon = it.findViewById<LinearLayout>(R.id.iv_lock_icon)
            val arrowIv = it.findViewById<ImageView>(R.id.iv_arrow)
            val icon = it.findViewById<ImageView>(R.id.iv_icon)

            card.setCardBackgroundColor(Color.parseColor(bgColor[index]))
            titleTv.text = level.level_name

            card.setOnClickListener {
                if (level.is_accessible) {
                    findNavController().navigate(
                        LevelListFragmentDirections.actionNavFragmentStudyToLevelFragment(
                            level.level_id
                        )
                    )
                }
            }

            var text = ""
            var color = ""
            if (level.is_accessible) {
                if (level.is_passed) {
                    text = "학습완료 | ${level.best_score}점"
                    color = "#1E54BC"
                } else {
                    if (level.best_score == 100) {
                        text = "미응시"
                        color = "#9E9E9E"
                    } else {
                        text = "학습중 | ${level.best_score}점"
                        color = "#FF6756"
                    }
                }
                descTv.text = text
                descTv.setTextColor(Color.parseColor(color))
            } else {
                lockIcon.visibility = View.VISIBLE
            }


            if (index == 5) {
                icon.visibility = View.VISIBLE
                arrowIv.visibility = View.GONE
                lockIcon.visibility = View.GONE
                descTv.visibility = View.GONE
                card.setOnClickListener {
                    findNavController().navigate(LevelListFragmentDirections.actionNavFragmentStudyToBookmarkListFragment())
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.setLoading(false)
    }

}
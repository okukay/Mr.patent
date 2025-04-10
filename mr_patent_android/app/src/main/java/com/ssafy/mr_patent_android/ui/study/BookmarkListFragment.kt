package com.ssafy.mr_patent_android.ui.study

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.data.model.dto.LevelDto
import com.ssafy.mr_patent_android.databinding.FragmentBookmarkListBinding
import com.ssafy.mr_patent_android.databinding.FragmentLevelListBinding

class BookmarkListFragment : BaseFragment<FragmentBookmarkListBinding>(
    FragmentBookmarkListBinding::bind,
    R.layout.fragment_bookmark_list
) {
    val viewModel: BookmarkViewModel by viewModels()
    val cardList= intArrayOf(
        R.id.bookmark_level1,
        R.id.bookmark_level2,
        R.id.bookmark_level3,
        R.id.bookmark_level4,
        R.id.bookmark_level5,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initObserver()

    }

    fun initView(){
        viewModel.getBookmarkList()

    }

    fun initObserver(){
        viewModel.loading.observe(viewLifecycleOwner) {
            if (it) {
                showLoadingDialog()
            } else {
                dismissLoadingDialog()
            }
        }


        viewModel.bookmarkList.observe(viewLifecycleOwner, {
            it?.let {
                for (i in 0 until it.levels.size) {
                    setLevelCard(it.levels[i], i)
                }
            }
        })
    }

    fun setLevelCard(level: LevelDto.Level, index: Int ) {
        val cardView = view?.findViewById<CardView>(cardList[index])
        cardView?.let {
            val bookmarkText = it.findViewById<TextView>(R.id.tv_bookmark_count)
            bookmarkText.text = "북마크한 개수: ${level.count}"
            val ivLevelImage = it.findViewById<ImageView>(R.id.iv_bookmark_icon)

            ivLevelImage.setImageResource(
                when (index) {
                    0 -> R.drawable.book1_icon
                    1 -> R.drawable.book2_icon
                    2 -> R.drawable.book3_icon
                    3 -> R.drawable.book4_icon
                    4 -> R.drawable.book5_icon
                    else -> R.drawable.book1_icon
                }
            )

            it.setOnClickListener {
                findNavController().navigate(BookmarkListFragmentDirections.actionBookmarkListFragmentToStudyCardFragment(level.level_id,"bookmark"))
            }
        }
    }

}
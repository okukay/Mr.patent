package com.ssafy.mr_patent_android.ui.expert

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.databinding.FragmentExpertListBinding
import com.ssafy.mr_patent_android.ui.home.HomeFragment.Companion.categoryMap

class ExpertListFragment : BaseFragment<FragmentExpertListBinding>(
    FragmentExpertListBinding::bind,
    R.layout.fragment_expert_list
) {
    private lateinit var expertListAdapter: ExpertListAdapter
    val viewModel: ExpertListViewModel by viewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initExpertView()
        initSort()
        initAdapter()
        initObserver()

    }

    private fun initAdapter() {
        viewModel.expertList.observe(viewLifecycleOwner) {
            expertListAdapter = ExpertListAdapter(it) { id ->
                findNavController().navigate(
                    ExpertListFragmentDirections.actionPatentAttorneyListFragmentToPatentAttorneyFragment(
                        id
                    )
                )
            }
            binding.rvPatentAttorneys.adapter = expertListAdapter
        }
    }


    private fun initSort() {
        val items = resources.getStringArray(R.array.align_array)

        val myAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, items)

        binding.spinnerPatentAttorneys.adapter = myAdapter

        binding.spinnerPatentAttorneys.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (viewModel.expertList.value.isNullOrEmpty()) {
                    } else {
                        val expertList = viewModel.expertList.value!!

                        when (position) {
                            0 -> {
                                viewModel.setNewExpertList(expertList.sortedBy { it.expertId })
                                binding.rvPatentAttorneys.adapter = ExpertListAdapter(
                                    viewModel.newExpertList.value ?: listOf()
                                ) { id ->
                                    findNavController().navigate(
                                        ExpertListFragmentDirections.actionPatentAttorneyListFragmentToPatentAttorneyFragment(
                                            id
                                        )
                                    )
                                }
                            }

                            1 -> {
                                viewModel.setNewExpertList(expertList.sortedByDescending { it.expertId })
                                binding.rvPatentAttorneys.adapter = ExpertListAdapter(
                                    viewModel.newExpertList.value ?: listOf()
                                ) { id ->
                                    findNavController().navigate(
                                        ExpertListFragmentDirections.actionPatentAttorneyListFragmentToPatentAttorneyFragment(
                                            id
                                        )
                                    )
                                }
                            }

                            else -> {
                                viewModel.setNewExpertList(expertList.sortedBy { it.expertGetDate })
                                binding.rvPatentAttorneys.adapter = ExpertListAdapter(
                                    viewModel.newExpertList.value ?: listOf()
                                ) { id ->
                                    findNavController().navigate(
                                        ExpertListFragmentDirections.actionPatentAttorneyListFragmentToPatentAttorneyFragment(
                                            id
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {

                }
            }
    }

    private fun initExpertView() {
        viewModel.getExpertList()
        binding.cgFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            viewModel.setFilterState(checkedIds.map { categoryMap[it] })
        }
    }

    private fun initObserver() {

        viewModel.filterState.observe(viewLifecycleOwner) { filters ->
            val filteredList = if (filters.isEmpty()) {
                viewModel.expertList.value
            } else {
                viewModel.expertList.value?.filter { expert ->
                    filters.any { filter -> expert.category.contains(filter) }
                }
            }

            val sortedList = when (binding.spinnerPatentAttorneys.selectedItemPosition) {
                0 -> filteredList?.sortedByDescending { it.expertCreatedAt }
                1 -> filteredList?.sortedBy { it.expertCreatedAt }
                else -> filteredList?.sortedByDescending { it.expertGetDate }
            }

            viewModel.setNewExpertList(sortedList)

            expertListAdapter = ExpertListAdapter(sortedList ?: listOf()) { id ->
                findNavController().navigate(
                    ExpertListFragmentDirections.actionPatentAttorneyListFragmentToPatentAttorneyFragment(
                        id
                    )
                )
            }
            binding.rvPatentAttorneys.adapter = expertListAdapter
        }
    }
}
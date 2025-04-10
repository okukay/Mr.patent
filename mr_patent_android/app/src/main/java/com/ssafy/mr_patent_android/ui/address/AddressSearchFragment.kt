package com.ssafy.mr_patent_android.ui.address

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.BaseFragment
import com.ssafy.mr_patent_android.data.model.dto.AddressDto
import com.ssafy.mr_patent_android.databinding.FragmentAddressSearchBinding
import java.util.Locale

private const val TAG = "AddressSearchFragment_Mr_Patent"
class AddressSearchFragment : BaseFragment<FragmentAddressSearchBinding>(
    FragmentAddressSearchBinding::bind, R.layout.fragment_address_search
) {
    private val addressViewModel : AddressViewModel by activityViewModels()

    private lateinit var placesClient: PlacesClient
    private var addressList: MutableList<AddressDto> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.api_key), Locale.KOREA)
        }
        placesClient = Places.createClient(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initObserver()
    }

    private fun initView() {
        binding.tvBefore.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.ivAddressSearch.setOnClickListener {
            binding.clAddressInfo.visibility = View.GONE

            val searchText = binding.etAddressSearch.text.toString()
            search(searchText)
        }

        binding.etAddressSearch.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                true
            } else {
                false
            }
        }
    }

    private fun initObserver() {

    }

    private fun search(searchText: String) {
        val placeFields: List<Place.Field> = listOf(
            Place.Field.ID,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.ADDRESS_COMPONENTS
        )

        val searchByTextRequest = SearchByTextRequest.builder(searchText, placeFields)
            .setMaxResultCount(10)
            .build()

        placesClient.searchByText(searchByTextRequest)
            .addOnSuccessListener { response ->
                val places: List<Place> = response.places

                addressList.clear()
                for (place in places) {
                    val postalCode = place.addressComponents?.asList()?.find { it.types.contains("postal_code") }?.name ?: ""
                    addressList.add(AddressDto(
                        (addressList.size + 1).toString()
                        , place.address
                        , postalCode
                    ))
                }
                initAdapter()
            }.addOnFailureListener { exception ->
                binding.tvAddressNone.visibility = View.VISIBLE
            }

    }

    private fun initAdapter() {
        binding.tvAddressNone.visibility = View.GONE
        binding.rvAddressList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAddressList.adapter = AddressAdapter(addressList) { position ->
            addressViewModel.setAddress(addressList[position].address)
            findNavController().popBackStack()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String, value: String) =
            AddressSearchFragment().apply {
                arguments = Bundle().apply {
                    putString(key, value)
                }
            }
    }
}
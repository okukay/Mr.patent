package com.ssafy.mr_patent_android.ui.address

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.mr_patent_android.data.model.dto.AddressDto
import com.ssafy.mr_patent_android.databinding.ListItemAddressBinding

class AddressAdapter(var addressList: MutableList<AddressDto>, val itemClickListener : ItemClickListener)
    : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val binding = ListItemAddressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = addressList.size

    fun interface ItemClickListener {
        fun onItemClick(position: Int)
    }

    inner class AddressViewHolder(private val binding: ListItemAddressBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.tvNo.text = addressList[position].index
            binding.tvStreetAddress.text = addressList[position].address
            binding.tvZipCode.text = addressList[position].zipCode
            binding.listItemAddress.setOnClickListener {
                itemClickListener.onItemClick(position)
            }
        }
    }
}
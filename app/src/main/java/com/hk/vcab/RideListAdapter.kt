package com.hk.vcab

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class RideListAdapter(context: Context, private val rides: List<String>) :
    ArrayAdapter<String>(context, 0, rides) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rideView = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
        val rideText = rideView.findViewById<TextView>(android.R.id.text1)
        rideText.text = rides[position]
        return rideView
    }
}

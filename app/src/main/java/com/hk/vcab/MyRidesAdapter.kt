package com.hk.vcab

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth

class MyRidesAdapter(
    private val context: Context,
    private val rides: List<Map<String, Any>>
) : BaseAdapter() {

    override fun getCount(): Int = rides.size
    override fun getItem(position: Int): Any = rides[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val ride = rides[position]
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.my_ride_item, parent, false)

        val rideLayout = view.findViewById<LinearLayout>(R.id.rideItemLayout)
        val rideText = view.findViewById<TextView>(R.id.rideTextView)

        val pickup = ride["pickup"] as? String ?: ""
        val drop = ride["drop"] as? String ?: ""
        val date = ride["date"] as? String ?: ""
        val time = ride["time"] as? String ?: ""

        rideText.text = "$pickup → $drop\n$date $time"

        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        val driverId = ride["driverId"] as? String

        if (driverId == currentUserEmail) {
            // Driver has accepted this ride → green background
            rideLayout.setBackgroundColor(Color.parseColor("#D0F0C0")) // light green
        } else {
            rideLayout.setBackgroundColor(Color.WHITE)
        }

        return view
    }
}

package com.hk.vcab

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RideAdapter(
    private val context: Context,
    private val rides: MutableList<Map<String, Any>>,
    private val onRideAccepted: () -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = rides.size

    override fun getItem(position: Int): Any = rides[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val ride = rides[position]
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.ride_item, parent, false)

        val rideDetails = view.findViewById<TextView>(R.id.rideDetails)
        val acceptBtn = view.findViewById<Button>(R.id.acceptRideBtn)

        val pickup = ride["pickup"] as? String ?: ""
        val drop = ride["drop"] as? String ?: ""
        val date = ride["date"] as? String ?: ""
        val time = ride["time"] as? String ?: ""
        val carType = ride["carType"] as? String ?: ""
        val passengers = (ride["passengerCount"] as? Long)?.toInt() ?: 0

        rideDetails.text = "$pickup to $drop\n$date $time\n$carType ($passengers)"

        acceptBtn.setOnClickListener {
            val rideId = ride["id"] as String
            val driverEmail = FirebaseAuth.getInstance().currentUser?.email ?: "unknown"

            FirebaseFirestore.getInstance().collection("bookings")
                .document(rideId)
                .update("driverId", driverEmail)
                .addOnSuccessListener {
                    Toast.makeText(context, "Ride accepted!", Toast.LENGTH_SHORT).show()
                    rides.removeAt(position)
                    notifyDataSetChanged()
                    onRideAccepted()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to accept ride", Toast.LENGTH_SHORT).show()
                }
        }

        return view
    }
}

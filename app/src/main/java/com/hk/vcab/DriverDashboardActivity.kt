package com.hk.vcab

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class DriverDashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rideListView: ListView
    private val rideList: MutableList<Map<String, Any>> = mutableListOf()
    private lateinit var adapter: RideAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_dashboard)

        db = FirebaseFirestore.getInstance()
        rideListView = findViewById(R.id.rideListView)

        adapter = RideAdapter(this, rideList) {
            // Optional: do something when a ride is accepted
        }

        rideListView.adapter = adapter

        loadAvailableRides()
    }

    private fun loadAvailableRides() {
        db.collection("bookings")
            .get()
            .addOnSuccessListener { result ->
                rideList.clear()
                for (document in result) {
                    val driverId = document.getString("driverId")
                    if (driverId.isNullOrEmpty()) {
                        val ride = document.data.toMutableMap()
                        ride["id"] = document.id
                        rideList.add(ride)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load rides", Toast.LENGTH_SHORT).show()
            }
    }
}

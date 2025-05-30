package com.hk.vcab

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyBookingActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var bookingListView: ListView
    private lateinit var viewMatchedRidesButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_booking)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        bookingListView = findViewById(R.id.bookingListView)
        viewMatchedRidesButton = findViewById(R.id.viewMatchedRidesButton)

        viewMatchedRidesButton.setOnClickListener {
            val intent = Intent(this, MatchedRidesActivity::class.java)
            startActivity(intent)
        }

        loadBookings()
    }

    private fun loadBookings() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("bookings")
                .whereEqualTo("uid", currentUser.uid)
                .get()
                .addOnSuccessListener { result ->
                    val bookings = mutableListOf<String>()
                    val bookingIds = mutableListOf<String>()

                    for (doc in result) {
                        val joinedUids = doc.get("joinedUids") as? Map<*, *>
                        val hasJoins = joinedUids != null && joinedUids.isNotEmpty()

                        // ❌ Skip if someone has joined
                        if (hasJoins) continue

                        val pickup = doc.getString("pickup") ?: ""
                        val drop = doc.getString("drop") ?: ""
                        val date = doc.getString("date") ?: ""
                        val time = doc.getString("time") ?: ""
                        val carType = doc.getString("carType") ?: "N/A"
                        val passengerCount = doc.getLong("passengerCount")?.toInt() ?: 0
                        val bookingId = doc.id

                        bookings.add(
                            "From: $pickup\n" +
                                    "To: $drop\n" +
                                    "Date: $date\n" +
                                    "Time: $time\n" +
                                    "Car Type: $carType\n" +
                                    "Passengers: $passengerCount"
                        )
                        bookingIds.add(bookingId)
                    }

                    val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, bookings)
                    bookingListView.adapter = adapter

                    bookingListView.setOnItemClickListener { _, _, position, _ ->
                        val selectedBookingId = bookingIds[position]
                        val intent = Intent(this, EditBookingActivity::class.java)
                        intent.putExtra("bookingId", selectedBookingId)
                        startActivity(intent)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to fetch bookings", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBookings()
    }
}

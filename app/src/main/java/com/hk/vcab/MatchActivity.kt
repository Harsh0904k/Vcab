package com.hk.vcab

import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalTime
import java.time.format.DateTimeParseException

class MatchActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var listView: ListView
    private val matchedUsers = mutableListOf<String>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)

        db = FirebaseFirestore.getInstance()
        listView = findViewById(R.id.matchListView)

        val drop = intent.getStringExtra("drop")
        val date = intent.getStringExtra("date")
        val time = intent.getStringExtra("time")

        if (drop == null || date == null || time == null) {
            Toast.makeText(this, "Missing intent data", Toast.LENGTH_SHORT).show()
            return
        }

        val targetTime: LocalTime
        try {
            targetTime = LocalTime.parse(time)
        } catch (e: DateTimeParseException) {
            Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show()
            return
        }

        val minTime = targetTime.minusMinutes(15)
        val maxTime = targetTime.plusMinutes(15)

        db.collection("bookings")
            .whereEqualTo("drop", drop)
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val bookingTimeStr = doc.getString("time")
                    try {
                        val bookingTime = LocalTime.parse(bookingTimeStr)
                        if (bookingTime in minTime..maxTime) {
                            val pickup = doc.getString("pickup") ?: "N/A"
                            val userId = doc.getString("uid") ?: "Unknown"
                            matchedUsers.add("👤 $userId\n📍 Pickup: $pickup\n⏰ Time: $bookingTimeStr")
                        }
                    } catch (e: DateTimeParseException) {
                        // Skip entries with invalid time format
                    }
                }

                if (matchedUsers.isEmpty()) {
                    matchedUsers.add("No matches found.")
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, matchedUsers)
                listView.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Match search failed", Toast.LENGTH_SHORT).show()
            }
    }
}

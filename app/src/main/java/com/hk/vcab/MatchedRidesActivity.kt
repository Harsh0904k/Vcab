package com.hk.vcab

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MatchedRidesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var matchedListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matched_rides)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        matchedListView = findViewById(R.id.matchedListView)

        val userId = auth.currentUser?.uid ?: return
        val inflater = LayoutInflater.from(this)

        db.collection("bookings")
            .whereEqualTo("uid", userId)
            .get()
            .addOnSuccessListener { userBookings ->
                if (userBookings.isEmpty) {
                    Toast.makeText(this, "You have no bookings", Toast.LENGTH_SHORT).show()
                } else {
                    handleUserBooking(userBookings.first(), userId, inflater)
                }
            }
    }

    private fun handleUserBooking(
        userBooking: DocumentSnapshot,
        userId: String,
        inflater: LayoutInflater
    ) {
        val pickup = userBooking.getString("pickup")
        val drop = userBooking.getString("drop")
        val date = userBooking.getString("date")
        val time = userBooking.getString("time")
        val carType = userBooking.getString("carType")
        val myPassengerCount = userBooking.getLong("passengerCount")?.toInt() ?: 1

        if (pickup == null || drop == null || date == null || time == null || carType == null) {
            Toast.makeText(this, "Your booking data is incomplete", Toast.LENGTH_SHORT).show()
            return
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val userTime: Date = timeFormat.parse(time) ?: return
        val calendar = Calendar.getInstance()

        calendar.time = userTime
        calendar.add(Calendar.MINUTE, -45)
        val lowerBound = calendar.time

        calendar.time = userTime
        calendar.add(Calendar.MINUTE, 45)
        val upperBound = calendar.time

        db.collection("bookings")
            .whereEqualTo("pickup", pickup)
            .whereEqualTo("drop", drop)
            .whereEqualTo("date", date)
            .whereEqualTo("carType", carType)
            .get()
            .addOnSuccessListener { results ->

                val matchedRidesView = mutableListOf<View>()

                for (doc in results) {
                    val rideCreatorId = doc.getString("uid") ?: continue
                    val joinedUids = doc.get("joinedUids") as? Map<*, *> ?: emptyMap<String, Long>()

                    if (userId == rideCreatorId || joinedUids.containsKey(userId)) continue

                    val matchedTimeStr = doc.getString("time") ?: continue
                    val matchedName = doc.getString("name") ?: "Unknown"
                    val matchedPassengerCount = doc.getLong("passengerCount")?.toInt() ?: 1
                    val matchedRideId = doc.id

                    try {
                        val matchedTime = timeFormat.parse(matchedTimeStr) ?: continue
                        if (matchedTime !in lowerBound..upperBound) continue

                        var totalJoined = 0
                        for ((_, count) in joinedUids) {
                            totalJoined += (count as? Long)?.toInt() ?: 1
                        }

                        val capacity = if (carType == "5-seater") 5 else 7
                        val totalIfJoined = matchedPassengerCount + totalJoined + myPassengerCount

                        if (totalIfJoined <= capacity) {
                            val matchedRideView = inflater.inflate(R.layout.matched_ride_item, null)
                            val rideDetailsTextView = matchedRideView.findViewById<TextView>(R.id.rideDetails)
                            val passengerCountText = matchedRideView.findViewById<TextView>(R.id.passengerCountText)
                            val joinRideButton = matchedRideView.findViewById<Button>(R.id.btnJoinRide)

                            passengerCountText.text = "Passengers: ${matchedPassengerCount + totalJoined}/$capacity"
                            rideDetailsTextView.text =
                                "Name: $matchedName\nFrom: $pickup\nTo: $drop\nDate: $date\nTime: $matchedTimeStr"

                            joinRideButton.setOnClickListener {
                                db.collection("myRides")
                                    .whereEqualTo("rideId", matchedRideId)
                                    .whereEqualTo("userId", userId)
                                    .get()
                                    .addOnSuccessListener { existingDocs ->
                                        if (existingDocs.isEmpty) {
                                            val userName = auth.currentUser?.displayName ?: "Unknown"
                                            val userRide = hashMapOf(
                                                "rideId" to matchedRideId,
                                                "userId" to userId,
                                                "userName" to userName,
                                                "passengerCount" to myPassengerCount
                                            )
                                            db.collection("myRides").add(userRide)
                                                .addOnSuccessListener {
                                                    val rideRef = db.collection("bookings").document(matchedRideId)
                                                    rideRef.get().addOnSuccessListener { rideDoc ->
                                                        val updatedMap = (rideDoc.get("joinedUids") as? MutableMap<String, Long>)?.toMutableMap()
                                                            ?: mutableMapOf()
                                                        updatedMap[userId] = myPassengerCount.toLong()
                                                        rideRef.update("joinedUids", updatedMap)

                                                        val creatorId = rideDoc.getString("uid") ?: ""
                                                        val totalJoinedPassengers = updatedMap.values.sumOf { it } + (rideDoc.getLong("passengerCount")
                                                            ?: 0)

                                                        val rideDataForCreator = hashMapOf(
                                                            "rideId" to matchedRideId,
                                                            "uid" to creatorId,
                                                            "pickup" to rideDoc.getString("pickup"),
                                                            "drop" to rideDoc.getString("drop"),
                                                            "date" to rideDoc.getString("date"),
                                                            "time" to rideDoc.getString("time"),
                                                            "carType" to rideDoc.getString("carType"),
                                                            "joinedPassengerCount" to totalJoinedPassengers
                                                        )

                                                        db.collection("myBookings")
                                                            .whereEqualTo("rideId", matchedRideId)
                                                            .whereEqualTo("uid", creatorId)
                                                            .get()
                                                            .addOnSuccessListener { creatorDocs ->
                                                                if (creatorDocs.isEmpty) {
                                                                    db.collection("myBookings").add(rideDataForCreator)
                                                                } else {
                                                                    for (doc in creatorDocs) {
                                                                        db.collection("myBookings").document(doc.id)
                                                                            .update("joinedPassengerCount", totalJoinedPassengers)
                                                                    }
                                                                }
                                                            }

                                                        db.collection("bookings")
                                                            .whereEqualTo("uid", userId)
                                                            .get()
                                                            .addOnSuccessListener { userBookings ->
                                                                for (userDoc in userBookings) {
                                                                    if (userDoc.id != matchedRideId) {
                                                                        db.collection("bookings").document(userDoc.id).delete()
                                                                    }
                                                                }
                                                                Toast.makeText(this, "Joined Ride", Toast.LENGTH_SHORT).show()
                                                                startActivity(Intent(this, MyRidesActivity::class.java))
                                                                finish()
                                                            }
                                                    }
                                                }
                                        } else {
                                            Toast.makeText(this, "You already joined this ride", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }

                            matchedRidesView.add(matchedRideView)
                        }
                    } catch (_: Exception) {
                        continue
                    }
                }

                if (matchedRidesView.isEmpty()) {
                    val emptyView = TextView(this)
                    emptyView.text = "No matched rides found."
                    matchedRidesView.add(emptyView)
                }

                matchedListView.adapter = object : BaseAdapter() {
                    override fun getCount() = matchedRidesView.size
                    override fun getItem(position: Int) = matchedRidesView[position]
                    override fun getItemId(position: Int) = position.toLong()
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup?) =
                        matchedRidesView[position]
                }
            }
    }
}
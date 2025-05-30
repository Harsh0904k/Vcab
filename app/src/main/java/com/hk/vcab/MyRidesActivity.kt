package com.hk.vcab

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyRidesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var myRidesListView: ListView
    private lateinit var progressBar: ProgressBar

    private val ridesList = mutableListOf<Pair<String, String>>()
    private val userRideIds = mutableSetOf<String>()
    private val greenHighlightRides = mutableSetOf<String>()
    private val leftRideIds = mutableSetOf<String>()
    private val rejoinedRideIds = mutableSetOf<String>()

    private var joinedRidesLoaded = false
    private var createdRidesLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_rides)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        myRidesListView = findViewById(R.id.myRidesListView)
        progressBar = findViewById(R.id.progressBar)

        loadRideHistoryThenRides()
    }

    private fun loadRideHistoryThenRides() {
        val currentUser = auth.currentUser ?: return
        leftRideIds.clear()
        progressBar.visibility = View.VISIBLE

        db.collection("rideHistory")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    doc.getString("rideId")?.let { leftRideIds.add(it) }
                }
                joinedRidesLoaded = false
                createdRidesLoaded = false
                userRideIds.clear()
                greenHighlightRides.clear()
                rejoinedRideIds.clear()

                loadUserJoinedRides()
                loadUserCreatedRides()
            }
    }

    private fun loadUserJoinedRides() {
        val currentUser = auth.currentUser ?: return

        db.collection("myRides")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val rideId = doc.getString("rideId")
                    if (rideId != null) {
                        rejoinedRideIds.add(rideId)
                        userRideIds.add(rideId)
                    }
                }

                leftRideIds.removeAll(rejoinedRideIds)
                joinedRidesLoaded = true
                maybeLoadRidesDetails()
            }
    }

    private fun loadUserCreatedRides() {
        val currentUser = auth.currentUser ?: return

        db.collection("bookings")
            .whereEqualTo("uid", currentUser.uid)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val rideId = doc.id
                    if (leftRideIds.contains(rideId)) continue

                    val joinedUids = doc.get("joinedUids") as? Map<String, Long> ?: emptyMap()
                    val otherJoiners = joinedUids.keys.filter { it != currentUser.uid }

                    if (otherJoiners.isNotEmpty()) {
                        userRideIds.add(rideId)
                    }
                }
                createdRidesLoaded = true
                maybeLoadRidesDetails()
            }
    }

    private fun maybeLoadRidesDetails() {
        if (joinedRidesLoaded && createdRidesLoaded) {
            loadRidesDetails()
        }
    }

    private fun loadRidesDetails() {
        ridesList.clear()
        if (userRideIds.isEmpty()) {
            setupAdapter()
            progressBar.visibility = View.GONE
            return
        }

        var processedCount = 0
        val currentUser = auth.currentUser ?: return

        for (rideId in userRideIds) {
            db.collection("bookings").document(rideId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val pickup = doc.getString("pickup") ?: "Unknown"
                        val drop = doc.getString("drop") ?: "Unknown"
                        val date = doc.getString("date") ?: "Unknown"
                        val time = doc.getString("time") ?: "Unknown"
                        val creatorId = doc.getString("uid") ?: ""
                        val driverId = doc.getString("driverId") ?: ""
                        val joinedUids = doc.get("joinedUids") as? Map<String, Long> ?: emptyMap()
                        val carType = doc.getString("carType") ?: "5-seater"
                        val creatorPassengerCount = doc.getLong("passengerCount") ?: 1L

                        if (driverId.isNotEmpty()) {
                            greenHighlightRides.add(rideId)
                        }

                        val maxSeats = if (carType == "7-seater") 7 else 5
                        val joinedSeats = joinedUids.values.sum()
                        val totalPassengers = creatorPassengerCount + joinedSeats

                        db.collection("users").document(creatorId).get()
                            .addOnSuccessListener { creatorDoc ->
                                val creatorName = creatorDoc.getString("name") ?: "Unknown"
                                val isCreator = creatorId == currentUser.uid
                                val creatorNote = if (isCreator) "Created by me\n" else ""

                                val rideText = buildString {
                                    append(creatorNote)
                                    append("Created By: $creatorName\n")
                                    append("From: $pickup\n")
                                    append("To: $drop\n")
                                    append("Date: $date\n")
                                    append("Time: $time\n")
                                    append("Passengers: $totalPassengers/$maxSeats")
                                }

                                addRideToList(rideId, rideText)
                                processedCount++
                                if (processedCount == userRideIds.size) {
                                    setupAdapter()
                                    progressBar.visibility = View.GONE
                                }
                            }
                            .addOnFailureListener {
                                processedCount++
                                if (processedCount == userRideIds.size) {
                                    setupAdapter()
                                    progressBar.visibility = View.GONE
                                }
                            }
                    } else {
                        processedCount++
                        if (processedCount == userRideIds.size) {
                            setupAdapter()
                            progressBar.visibility = View.GONE
                        }
                    }
                }
        }
    }

    private fun addRideToList(rideId: String, rideText: String) {
        ridesList.add(Pair(rideId, rideText))
    }

    private fun setupAdapter() {
        val adapter = object : BaseAdapter() {
            override fun getCount(): Int = ridesList.size
            override fun getItem(position: Int): Any = ridesList[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val inflater = LayoutInflater.from(this@MyRidesActivity)
                val view = inflater.inflate(R.layout.my_ride_item, parent, false)

                val rideText = view.findViewById<TextView>(R.id.myRideDetails)
                val leaveBtn = view.findViewById<Button>(R.id.btnLeaveRide)

                val (rideId, rideInfo) = ridesList[position]
                rideText.text = rideInfo

                if (greenHighlightRides.contains(rideId)) {
                    view.setBackgroundResource(R.drawable.card_background_green)
                    rideText.setTextColor(Color.WHITE)
                    leaveBtn.setTextColor(Color.WHITE)
                } else {
                    view.setBackgroundResource(R.drawable.card_background)
                    rideText.setTextColor(Color.BLACK)
                    leaveBtn.setTextColor(Color.BLACK)
                }

                leaveBtn.setOnClickListener {
                    val userId = auth.currentUser?.uid ?: return@setOnClickListener

                    db.collection("myRides")
                        .whereEqualTo("rideId", rideId)
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                val myRideDoc = querySnapshot.documents[0]
                                val docId = myRideDoc.id
                                val leavingPassengerCount = myRideDoc.getLong("passengerCount")?.toInt() ?: 1

                                db.collection("myRides").document(docId).delete()

                                val bookingRef = db.collection("bookings").document(rideId)
                                bookingRef.get().addOnSuccessListener { bookingDoc ->
                                    val joinedUids = bookingDoc.get("joinedUids") as? MutableMap<String, Long> ?: mutableMapOf()
                                    joinedUids.remove(userId)

                                    bookingRef.update("joinedUids", joinedUids).addOnSuccessListener {
                                        val historyData = hashMapOf(
                                            "rideId" to rideId,
                                            "userId" to userId,
                                            "passengerCount" to leavingPassengerCount,
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                        db.collection("rideHistory").add(historyData)

                                        Toast.makeText(this@MyRidesActivity, "Left the ride", Toast.LENGTH_SHORT).show()
                                        loadRideHistoryThenRides()
                                    }
                                }
                            } else {
// Not found in myRides, might be creator
                                val bookingRef = db.collection("bookings").document(rideId)
                                bookingRef.get().addOnSuccessListener { bookingDoc ->
                                    val creatorId = bookingDoc.getString("uid") ?: return@addOnSuccessListener
                                    if (creatorId != userId) {
                                        Toast.makeText(this@MyRidesActivity, "Not authorized to leave this ride", Toast.LENGTH_SHORT).show()
                                        return@addOnSuccessListener
                                    }

                                    val joinedUids = (bookingDoc.get("joinedUids") as? MutableMap<String, Long>) ?: mutableMapOf()

                                    if (joinedUids.isEmpty()) {
                                        bookingRef.delete().addOnSuccessListener {
                                            val historyData = hashMapOf(
                                                "rideId" to rideId,
                                                "userId" to userId,
                                                "passengerCount" to 1,
                                                "timestamp" to System.currentTimeMillis()
                                            )
                                            db.collection("rideHistory").add(historyData)
                                            Toast.makeText(this@MyRidesActivity, "Ride deleted", Toast.LENGTH_SHORT).show()
                                            loadRideHistoryThenRides()
                                        }
                                    } else {
                                        val newCreatorId = joinedUids.keys.first()
                                        val newCreatorPassengerCount = joinedUids[newCreatorId] ?: 1
                                        joinedUids.remove(newCreatorId)

                                        db.collection("users").document(newCreatorId).get()
                                            .addOnSuccessListener { newUserDoc ->
                                                val newCreatorName = newUserDoc.getString("name") ?: "Unknown"

                                                bookingRef.update(
                                                    mapOf(
                                                        "uid" to newCreatorId,
                                                        "name" to newCreatorName,
                                                        "joinedUids" to joinedUids,
                                                        "passengerCount" to newCreatorPassengerCount
                                                    )
                                                ).addOnSuccessListener {
                                                    val newMyRide = hashMapOf(
                                                        "rideId" to rideId,
                                                        "userId" to newCreatorId,
                                                        "passengerCount" to newCreatorPassengerCount
                                                    )

                                                    db.collection("myRides")
                                                        .whereEqualTo("rideId", rideId)
                                                        .whereEqualTo("userId", newCreatorId)
                                                        .get()
                                                        .addOnSuccessListener { querySnapshot ->
                                                            if (querySnapshot.isEmpty) {
                                                                db.collection("myRides").add(newMyRide)
                                                            }

                                                            val historyData = hashMapOf(
                                                                "rideId" to rideId,
                                                                "userId" to userId,
                                                                "passengerCount" to 1,
                                                                "timestamp" to System.currentTimeMillis()
                                                            )

                                                            db.collection("rideHistory").add(historyData)
                                                                .addOnSuccessListener {
                                                                    Toast.makeText(
                                                                        this@MyRidesActivity,
                                                                        "Left ride. New creator assigned.",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    loadRideHistoryThenRides()
                                                                }
                                                        }
                                                }
                                            }
                                    }
                                }
                            }
                        }


                }

                return view
            }
        }

        myRidesListView.adapter = adapter
    }
}

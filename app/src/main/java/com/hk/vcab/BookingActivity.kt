package com.hk.vcab

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BookingActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0
    private var selectedHour = 0
    private var selectedMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val pickupSpinner = findViewById<Spinner>(R.id.pickupSpinner)
        val dropSpinner = findViewById<Spinner>(R.id.dropSpinner)
        val dateEditText = findViewById<EditText>(R.id.dateEditText)
        val timeEditText = findViewById<EditText>(R.id.timeEditText)
        val carTypeSpinner = findViewById<Spinner>(R.id.spinnerCarType)
        val passengerCountEditText = findViewById<EditText>(R.id.editPassengerCount)
        val bookButton = findViewById<Button>(R.id.bookButton)
        val viewMyBookingsButton = findViewById<Button>(R.id.viewMyBookingsButton)
        val myRidesButton = findViewById<Button>(R.id.myRidesButton)

        val locations = listOf("Vit Bhopal", "Bhopal Railway Station", "Bhopal Airport", "Indore", "Bhopal Bus Stand")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        pickupSpinner.adapter = adapter
        dropSpinner.adapter = adapter

        val carTypes = resources.getStringArray(R.array.car_type_array)
        val carTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, carTypes)
        carTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        carTypeSpinner.adapter = carTypeAdapter

        // Date picker
        dateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month
                    selectedDay = dayOfMonth
                    dateEditText.setText("$dayOfMonth/${month + 1}/$year")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Time picker
        timeEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute
                    timeEditText.setText(String.format("%02d:%02d", hourOfDay, minute))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        // Booking submission
        bookButton.setOnClickListener {
            val pickup = pickupSpinner.selectedItem.toString()
            val drop = dropSpinner.selectedItem.toString()
            val date = dateEditText.text.toString()
            val time = timeEditText.text.toString()
            val carType = carTypeSpinner.selectedItem.toString()
            val passengerCountStr = passengerCountEditText.text.toString()

            if (pickup.isNotEmpty() && drop.isNotEmpty() && date.isNotEmpty() && time.isNotEmpty() && passengerCountStr.isNotEmpty()) {
                val passengerCount = passengerCountStr.toIntOrNull()
                if (passengerCount == null || passengerCount <= 0) {
                    Toast.makeText(this, "Enter a valid number of passengers", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // ✅ Check that selected date and time is now or in the future
                val selectedDateTime = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)
                }
                val now = Calendar.getInstance()

                if (selectedDateTime.before(now)) {
                    Toast.makeText(this, "Date and time must be in the future", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val currentUser = auth.currentUser!!
                val userId = currentUser.uid

                db.collection("users").document(userId).get()
                    .addOnSuccessListener { userDoc ->
                        val userName = userDoc.getString("name") ?: "Unknown"

                        val rideId = db.collection("bookings").document().id
                        val booking = hashMapOf(
                            "rideId" to rideId,
                            "uid" to userId, // Creator's UID
                            "name" to userName, // Creator's name
                            "pickup" to pickup,
                            "drop" to drop,
                            "date" to date,
                            "time" to time,
                            "carType" to carType,
                            "passengerCount" to passengerCount,
                            "joinedUids" to listOf(userId),
                            "driverId" to null,
                            "driverName" to null
                        )

                        db.collection("bookings").document(rideId).set(booking)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Booking saved", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, MatchedRidesActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Booking failed", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to fetch user name", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        viewMyBookingsButton.setOnClickListener {
            val intent = Intent(this, MyBookingActivity::class.java)
            startActivity(intent)
        }

        myRidesButton.setOnClickListener {
            val intent = Intent(this, MyRidesActivity::class.java)
            startActivity(intent)
        }
    }
}

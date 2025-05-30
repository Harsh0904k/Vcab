package com.hk.vcab

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class EditBookingActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var pickupSpinner: Spinner
    private lateinit var dropSpinner: Spinner
    private lateinit var dateEditText: EditText
    private lateinit var timeEditText: EditText
    private lateinit var deleteButton: Button
    private lateinit var saveButton: Button
    private lateinit var spinnerCarType: Spinner
    private lateinit var editPassengerCount: EditText
    private lateinit var bookingId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_booking)

        db = FirebaseFirestore.getInstance()

        pickupSpinner = findViewById(R.id.pickupSpinner)
        dropSpinner = findViewById(R.id.dropSpinner)
        dateEditText = findViewById(R.id.dateEditText)
        timeEditText = findViewById(R.id.timeEditText)
        spinnerCarType = findViewById(R.id.spinnerCarType)
        editPassengerCount = findViewById(R.id.editPassengerCount)
        deleteButton = findViewById(R.id.deleteButton)
        saveButton = findViewById(R.id.saveButton)

        bookingId = intent.getStringExtra("bookingId") ?: ""
        Log.d("EditBooking", "Received bookingId: $bookingId")

        val locations = listOf(
            "Vit Bhopal",
            "Bhopal Railway Station",
            "Bhopal Airport",
            "Indore",
            "Bhopal Bus Stand"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        pickupSpinner.adapter = adapter
        dropSpinner.adapter = adapter

        if (bookingId.isNotEmpty()) {
            db.collection("bookings").document(bookingId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val pickup = document.getString("pickup") ?: ""
                        val drop = document.getString("drop") ?: ""
                        val date = document.getString("date") ?: ""
                        val time = document.getString("time") ?: ""
                        val carType = document.getString("carType") ?: ""
                        val passengerCount = (document.getLong("passengerCount") ?: 1).toInt()

                        pickupSpinner.setSelection(locations.indexOf(pickup))
                        dropSpinner.setSelection(locations.indexOf(drop))
                        dateEditText.setText(date)
                        timeEditText.setText(time)

                        val carTypes = resources.getStringArray(R.array.car_type_array)
                        spinnerCarType.setSelection(carTypes.indexOf(carType))
                        editPassengerCount.setText(passengerCount.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load booking details", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Invalid booking ID", Toast.LENGTH_SHORT).show()
            finish()
        }

        dateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    dateEditText.setText("$dayOfMonth/${month + 1}/$year")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        timeEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePicker = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
                    timeEditText.setText(formattedTime)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePicker.show()
        }

        saveButton.setOnClickListener {
            if (bookingId.isEmpty()) {
                Toast.makeText(this, "Invalid booking ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pickup = pickupSpinner.selectedItem.toString()
            val drop = dropSpinner.selectedItem.toString()
            val date = dateEditText.text.toString()
            val time = timeEditText.text.toString()
            val carType = spinnerCarType.selectedItem.toString()
            val passengerCount = editPassengerCount.text.toString().toIntOrNull() ?: 1

            if (pickup.isNotEmpty() && drop.isNotEmpty() && date.isNotEmpty() && time.isNotEmpty()) {
                val updatedBooking = hashMapOf(
                    "pickup" to pickup,
                    "drop" to drop,
                    "date" to date,
                    "time" to time,
                    "carType" to carType,
                    "passengerCount" to passengerCount
                )

                db.collection("bookings").document(bookingId)
                    .update(updatedBooking as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Booking updated successfully", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update booking", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        deleteButton.setOnClickListener {
            if (bookingId.isEmpty()) {
                Toast.makeText(this, "Booking ID is missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("bookings").document(bookingId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Booking canceled successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to cancel booking", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

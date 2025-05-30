# 🚖 VCab - Shared Cab Booking App for Students and Drivers

VCab is an Android application designed to simplify and optimize cab sharing among college students and drivers. It helps users book shared rides, match with others heading to the same destination, and minimize travel costs efficiently.

> ⚠️ **Note**: This is a **beta release** and still under active development. Please report any bugs or suggestions. Created by **Harsh Kumar**.

---

## 📲 Features

- 🔐 **User Authentication** using Firebase (students & drivers)
- 🗺️ **Create Bookings** with pickup, drop, date, time, car type & passenger count
- 🔍 **Smart Matching**: Automatically matches users going the same route at similar times
- 👨‍👩‍👧‍👦 **5-seater / 7-seater options** with live capacity management
- 🧭 **Drivers can view and accept rides**
- ✅ **Leave / Edit bookings**
- 🗂️ **My Bookings**, **My Rides**, and **Ride History**
- 🌗 **Dark mode support**
- 📄 **Intro screen** with app overview
- 🔁 **Smooth transitions** and animations

---

## 🚦 Getting Started

### 🔧 Requirements
- Android Studio (Hedgehog or newer recommended)
- Firebase project with Email/Password Auth enabled
- Internet access

### 🔗 Firebase Setup
1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Enable **Email/Password Authentication**
3. Add a **Realtime Database** or **Firestore** and configure rules
4. Download `google-services.json` and place it inside `app/` directory

---

## 📥 APK Testing (Beta)

1. Install the provided `.apk` on your Android phone
2. If you see a warning like **"App may harm your device"**, it's safe to ignore because the app is not on Play Store yet
3. Allow permissions and run the app
4. Try both **student login** and **driver login** (`email ending with @driver.com` becomes driver)

---

## 🧪 How to Test

1. Open the app and read the **intro screen**
2. Create an account (as student or driver)
3. Book a cab with details like pickup, drop, date, car type, etc.
4. Use another account to book a matching cab and check if they appear in **Matched Rides**
5. Try joining a ride and observe if it disappears from matches
6. Test "Leave Ride", edit ride, and login transitions
7. Provide feedback on UI, bugs, or usability

---

## 🧑‍💻 Developer

**Harsh Kumar**  
📧 Email: harshpvt09@gmail.com  
📞 Phone: +91-7667884004

---

## 📌 License

This project is currently private and not licensed for commercial distribution. For personal or educational use only.

---

## 🛠️ Roadmap (Upcoming Features)

- Push notifications for ride updates
- In-app chat between matched users
- Admin dashboard
- Auto-complete address using Maps API
- Payment integration

---

## ❤️ Feedback

Please help improve the app by reporting bugs, giving feedback, or suggesting features. You can open issues here or contact the developer directly.


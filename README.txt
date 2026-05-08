# ChatApplication — Anonymous Random Chat App

A real-time Android chat application that connects users anonymously for private 1-on-1 conversations. The app uses Firebase services to handle authentication, matchmaking, and live messaging while ensuring smooth session handling and cleanup.

---

##  Features

* User authentication using email and password
* Random anonymous user matchmaking
* Real-time 1-on-1 messaging
* Instant chat room creation and synchronization
* Automatic session termination when a user leaves
* Database cleanup using Firebase `onDisconnect()`
* Concurrency-safe matching using Firebase transactions

---

## App Showcase

```
![Chat App Showcase showing login, matchmaking and real-time chat](screenshots/Showcase.png)

```

## Demo 

![Demo](screenshots/ShowcaseDemo.gif)

---

## Tech Stack

* **Language:** Java (Android)
* **Backend:** Firebase Realtime Database
* **Authentication:** Firebase Authentication
* **Architecture:** Activity-based Android architecture
* **Tools:** Android Studio

---

## How It Works (Matchmaking Logic)

1. User clicks **"Find Someone"**
2. App checks the `/waiting_room` node in Firebase
3. If another user is waiting:

   * A unique chat room is created
   * Both users are connected instantly
4. If no user is available:

   * Current user is added to the waiting queue
   * App listens for incoming match
5. When matched:

   * Both users enter a private chat room
6. If any user leaves:

   * Chat room is deleted
   * Other user is automatically returned to matchmaking

---

## Setup Instructions

### 1. Create Firebase Project

1. Go to https://console.firebase.google.com
2. Click **Add Project**
3. Enter project name and continue

---

### 2. Register Android App

1. Click Android icon in Firebase console
2. Enter package name:

   ```
   com.example.chatapp
   ```
3. Download `google-services.json`
4. Place it inside the `/app` folder

---

### 3. Enable Authentication

1. Go to **Authentication → Get Started**
2. Enable **Email/Password**
3. Save changes

---

### 4. Setup Realtime Database

1. Go to **Realtime Database**
2. Click **Create Database**
3. Choose location
4. Start in **Test Mode**

---

### 5. Run the Project

#### Option A: Physical Device

* Enable Developer Options
* Enable USB Debugging
* Connect device and run via Android Studio

#### Option B: Emulator

* Open Device Manager in Android Studio
* Create virtual device
* Run the app

---

## Project Highlights

* Designed a real-time matchmaking system
* Solved race conditions using Firebase transactions
* Implemented lifecycle-aware session handling
* Built a responsive real-time chat experience

---

## Future Improvements

* Push notifications for new messages
* User reporting/blocking system
* Typing indicators
* Media sharing (images/files)
* Migration to Firestore for scalability

---

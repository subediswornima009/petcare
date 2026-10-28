# PetCare

PetCare is an Android application for managing everyday pet care in one place. Users can create pet profiles, organize reusable care routines, save nearby pet-care places, and track expenses.

## Features

- User registration, login, and profile management
- Pet profiles with pet details, photos, medical records, and care checklists
- Reusable care tasks for feeding, walks, grooming, medication, and health care
- Scheduled reminders and notification support for care tasks
- Expense tracking by pet and category, with monthly spending summaries
- Saved vets, groomers, parks, and pet stores
- Google Maps and location support for selecting and opening saved places
- Optional SMS/contact delegation for sharing a pet care checklist
- Local Room database for storing users, pets, care tasks, places, and expenses on the device

## Tech stack

- Java
- Android SDK 34
- Minimum Android version: API 24 (Android 7.0)
- Android Gradle Plugin 8.2.0
- AndroidX AppCompat and Material Components
- Room 2.6.1
- Google Maps and Play Services Location
- OkHttp and Picasso

## Requirements

- Android Studio with Android SDK 34 installed
- JDK 8 or newer compatible with your Android Studio setup
- An Android device or emulator running Android 7.0 (API 24) or newer
- A Google Maps API key with Maps SDK for Android enabled

## Getting started

1. Clone or open this project in Android Studio.
2. Allow Android Studio to sync the Gradle files and install any missing SDK components.
3. Add `MAPS_API_KEY=your_google_maps_api_key` to the ignored `local.properties` file, or set the `MAPS_API_KEY` environment variable. The build injects it into the app manifest.
4. Connect an Android device or start an emulator.
5. Run the `app` configuration from Android Studio, or use the Gradle wrapper:

   ```powershell
   .\gradlew.bat assembleDebug
   ```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions

Depending on the features used, Android may request permission for:

- Fine/coarse location, for map and place features
- Notifications, for care reminders
- Contacts and SMS, for checklist delegation
- Internet access, for maps and network-backed image loading

Permissions are requested at runtime where required by Android.

## Project structure

```text
app/
└── src/main/
    ├── java/com/swornima/petcare/   # Activities, adapters, reminders, and models
    ├── java/com/swornima/petcare/db/ # Room entities, DAOs, database, and sessions
    ├── res/layout/                   # Screen and list-item layouts
    ├── res/drawable/                 # Icons, backgrounds, and UI assets
    └── AndroidManifest.xml           # App components and permissions
```

## Data storage

PetCare currently uses a local Room database. User accounts and pet-care data are stored on the device rather than in a remote authentication or cloud database. Clearing app data or uninstalling the app removes locally stored data.

## Build commands

```powershell
# Build the debug APK
.\gradlew.bat assembleDebug

# Run unit tests
.\gradlew.bat test

# Clean generated build outputs
.\gradlew.bat clean
```

## Package information

- Application ID: `com.swornima.petcare`
- Version: `1.0` (version code `1`)

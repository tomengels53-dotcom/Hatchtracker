# Activity Navigation Guide

## Overview
This app uses Activity-based navigation with Jetpack Compose for UI. All Activities share data through a centralized `DataRepository`.

## Activity Structure

### MainActivity
**Location:** `app/src/main/java/com/example/hatchtracker/MainActivity.kt`
- Main menu screen with buttons to navigate to all other Activities
- Displays summary of birds and incubations
- Creates notification channel on startup

### Bird Management Activities

#### ListBirdActivity
**Location:** `app/src/main/java/com/example/hatchtracker/ListBirdActivity.kt`
- Displays list of all birds using `BirdListScreen` composable
- Has button to navigate to `AddBirdActivity`
- Reads data from `DataRepository.birdList`

#### AddBirdActivity
**Location:** `app/src/main/java/com/example/hatchtracker/AddBirdActivity.kt`
- Form to add new birds using `AddBirdScreen` composable
- Saves to `DataRepository.birdList`
- Automatically navigates back to `ListBirdActivity` after saving

### Incubation Management Activities

#### ListIncubationActivity
**Location:** `app/src/main/java/com/example/hatchtracker/ListIncubationActivity.kt`
- Displays list of all incubations using `IncubationListScreen` composable
- Has button to navigate to `AddIncubationActivity`
- Reads data from `DataRepository.incubationList`

#### AddIncubationActivity
**Location:** `app/src/main/java/com/example/hatchtracker/AddIncubationActivity.kt`
- Form to add new incubations using `AddIncubationScreen` composable
- Saves to `DataRepository.incubationList`
- Automatically schedules notifications
- Automatically navigates back to `ListIncubationActivity` after saving

## Data Storage: DataRepository

### Location
`app/src/main/java/com/example/hatchtracker/DataRepository.kt`

### Purpose
Centralized singleton object that stores temporary data shared across all Activities.

### Data Structure
```kotlin
object DataRepository {
    val birdList: MutableList<Bird> = mutableListOf()
    val incubationList: MutableList<Incubation> = mutableListOf()
}
```

### How Data is Shared

#### Reading Data
All Activities can directly access the lists:
```kotlin
// Get all birds
val birds = DataRepository.getAllBirds()

// Get all incubations
val incubations = DataRepository.getAllIncubations()

// Get specific bird by ID
val bird = DataRepository.getBirdById(1)
```

#### Writing Data
Data is automatically saved when using the existing Compose screens:
- `AddBirdScreen` saves to `DataRepository.birdList` (via the `birdList` variable)
- `AddIncubationScreen` saves to `DataRepository.incubationList` (via the `incubationList` variable)

The global `birdList` and `incubationList` variables are now property delegates that access `DataRepository`, ensuring backward compatibility.

### Benefits of DataRepository
1. **Shared State**: All Activities see the same data
2. **No Intent Extras Needed**: No need to pass large lists through Intents
3. **Simple Access**: Direct property access from any Activity
4. **Thread-Safe**: Singleton pattern ensures consistent access

## Navigation Flow

### From MainActivity

```
MainActivity
├── "View Birds" → ListBirdActivity
│   └── "Add New Bird" → AddBirdActivity → (saves) → ListBirdActivity
│
├── "Add New Bird" → AddBirdActivity → (saves) → ListBirdActivity
│
├── "View Incubations" → ListIncubationActivity
│   └── "Add New Incubation" → AddIncubationActivity → (saves) → ListIncubationActivity
│
└── "Add New Incubation" → AddIncubationActivity → (saves) → ListIncubationActivity
```

### Navigation Implementation

#### Starting an Activity
```kotlin
// In MainActivity or any Activity
val intent = Intent(this, ListBirdActivity::class.java)
startActivity(intent)
```

#### Finishing an Activity
```kotlin
// In AddBirdActivity or AddIncubationActivity after saving
finish() // Returns to previous Activity
```

## Passing Data Between Activities

### Current Implementation (Using DataRepository)

**No Intent Extras Needed!** All Activities share the same `DataRepository` instance, so data is automatically available.

#### Example: Adding a Bird
1. User taps "Add New Bird" in `MainActivity`
2. `AddBirdActivity` opens
3. User fills form and saves
4. `AddBirdScreen` saves to `DataRepository.birdList`
5. `AddBirdActivity` calls `finish()`
6. User returns to previous screen
7. `ListBirdActivity` automatically shows the new bird (reads from `DataRepository.birdList`)

### Alternative: Using Intent Extras (For Simple Data)

If you need to pass simple data (like an ID), you can use Intent extras:

#### Sending Data
```kotlin
val intent = Intent(this, AddBirdActivity::class.java)
intent.putExtra("bird_id", birdId)
intent.putExtra("edit_mode", true)
startActivity(intent)
```

#### Receiving Data
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val birdId = intent.getIntExtra("bird_id", -1)
    val editMode = intent.getBooleanExtra("edit_mode", false)
    
    // Use the data...
}
```

### For Complex Data (Lists, Objects)

**Use DataRepository** - It's already set up for this purpose.

## Activity Registration

All Activities are registered in `AndroidManifest.xml`:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:label="@string/app_name"
    android:theme="@style/Theme.HatchTracker">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<activity
    android:name=".AddBirdActivity"
    android:exported="false"
    android:label="Add Bird"
    android:theme="@style/Theme.HatchTracker"
    android:parentActivityName=".MainActivity" />

<!-- Similar for other Activities -->
```

## Back Navigation

### System Back Button
- Automatically handled by Android
- Returns to previous Activity in the stack
- `parentActivityName` in manifest enables up navigation

### Programmatic Navigation
```kotlin
// Finish current Activity
finish()

// Or navigate to specific Activity
val intent = Intent(this, MainActivity::class.java)
intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP // Clears activity stack
startActivity(intent)
finish()
```

## Data Persistence Notes

### Current Implementation
- **Temporary Storage**: Data is stored in memory only
- **Lost on App Close**: Data is cleared when app is closed
- **Shared During Session**: All Activities see the same data during app session

### For Production
Replace `DataRepository` with:
- **Room Database**: For persistent local storage
- **ViewModel + LiveData/StateFlow**: For reactive data management
- **Repository Pattern**: For data abstraction

## Testing Navigation

### Test Flow
1. Launch app → `MainActivity` opens
2. Tap "View Birds" → `ListBirdActivity` opens
3. Tap "Add New Bird" → `AddBirdActivity` opens
4. Fill form and save → Returns to `ListBirdActivity`
5. Press back → Returns to `MainActivity`
6. Tap "Add New Incubation" → `AddIncubationActivity` opens
7. Fill form and save → Returns to previous screen

### Verify Data Sharing
1. Add a bird in `AddBirdActivity`
2. Navigate to `ListBirdActivity`
3. Verify the bird appears in the list
4. Navigate to `AddIncubationActivity`
5. Verify the bird appears in the dropdown

## Summary

- **Navigation**: Use `Intent` and `startActivity()` to navigate between Activities
- **Data Sharing**: Use `DataRepository` singleton for shared data
- **No Intent Extras Needed**: Lists are automatically shared via `DataRepository`
- **Back Navigation**: Use `finish()` or system back button
- **All Activities Registered**: In `AndroidManifest.xml`

# Keep InputMethodService subclass and related classes.
-keep class com.keyboard.ime.** { *; }
-keep class com.keyboard.prediction.** { *; }

# Room schema + entities.
-keep class androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *

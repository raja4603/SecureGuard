-keep class com.google.auto.value.** { *; }
-dontwarn com.google.auto.value.**

# Keep all public members of TensorFlow Lite classes.
-keep public class org.tensorflow.lite.** {
    public *;
}

# Keep Room generated classes
-keep class **_Impl {
    *;
}
-keep class androidx.room.paging.LimitOffsetDataSource { *; }

# Keep data classes used by Room
-keepclassmembers class * extends androidx.room.Entity {
    public <init>(...);
}

# Keep necessary classes for Jetpack Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class androidx.compose.runtime.reflect.** { *; }

# Keep WorkManager implementation
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}


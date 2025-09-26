
# Xabber classes - many of them loaded via class loader
-keep class com.xabber.** { *; }

# Marshmallow removed Notification.setLatestEventInfo()
-dontwarn android.app.Notification

# RxJava
-dontwarn rx.internal.util.**
-dontwarn sun.misc.**
-dontwarn module-info

-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

-dontnote rx.internal.util.PlatformDependent

# google
-keep class com.google.**
-dontwarn com.google.**

# realm
-keep class io.realm.annotations.RealmModule
-keep @io.realm.annotations.RealmModule class *
-keep class io.realm.internal.Keep
-keep @io.realm.internal.Keep class * { *; }
-dontwarn javax.**
-dontwarn io.realm.**
-keepnames public class * extends io.realm.RealmObject
-keep public class * extends io.realm.RealmObject { *; }
-keep class io.realm.** { *; }

# EbentBus
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# smack
-keep class org.jivesoftware.smack.** { *; }
-keep class org.jivesoftware.smackx.** { *; }

# This dnsjava class uses old Sun API
-dontnote org.xbill.DNS.spi.DNSJavaNameServiceDescriptor
-dontwarn org.xbill.DNS.spi.DNSJavaNameServiceDescriptor

# See http://stackoverflow.com/questions/5701126, happens in dnsjava
-optimizations !code/allocation/variable

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Google API-Client
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}
-dontwarn com.google.api.client.extensions.android.**
-dontwarn com.google.api.client.googleapis.extensions.android.**
-dontwarn com.google.android.gms.**
-dontnote java.nio.file.Files, java.nio.file.Path
-dontnote **.ILicensingService
-dontnote sun.misc.Unsafe
-dontwarn sun.misc.Unsafe

# retrofit
-dontwarn retrofit2.Platform$Java8

# Android Image Cropper
-keep class androidx.appcompat.widget.** { *; }

# Suppress warnings for Guava and Hamcrest
-dontwarn com.google.common.**
-dontwarn org.hamcrest.**

# Keep ListenableFuture
-keep class com.google.common.util.concurrent.ListenableFuture { *; }

# Keep Firebase and Google Play Services classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep Smack classes
-keep class org.jivesoftware.smack.** { *; }
-keep class org.jivesoftware.smackx.** { *; }

# Keep OTR classes
-keep class org.otr4j.** { *; }

# Keep Kotlin serialization
-keep class kotlinx.serialization.** { *; }

# Keep androidx.activity
-keep class androidx.activity.** { *; }

# Keep Realm classes
-keep class io.realm.** { *; }
-keepnames public class * extends io.realm.RealmObject
-keep public class * extends io.realm.RealmObject { *; }
-dontwarn io.realm.**

# Keep MemorizingTrustManager
-keep class de.duenndns.ssl.** { *; }

# Keep EventBus classes
-keep class org.greenrobot.eventbus.** { *; }
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# Suppress TestNG warnings
-dontwarn org.testng.**

# Exclude android.car classes from ProGuard processing
-dontwarn android.car.**

# RxJava
-dontwarn rx.internal.util.**
-dontwarn sun.misc.**
-dontwarn module-info
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}
-dontnote rx.internal.util.PlatformDependent

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# Google API-Client
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}
-dontwarn com.google.api.client.extensions.android.**
-dontwarn com.google.api.client.googleapis.extensions.android.**
-dontnote java.nio.file.Files, java.nio.file.Path
-dontnote **.ILicensingService
-dontnote sun.misc.Unsafe
-dontwarn sun.misc.Unsafe

# Retrofit
-dontwarn retrofit2.Platform$Java8

# Android Image Cropper
-keep class androidx.appcompat.widget.** { *; }

# DNSJava
-dontnote org.xbill.DNS.spi.DNSJavaNameServiceDescriptor
-dontwarn org.xbill.DNS.spi.DNSJavaNameServiceDescriptor
-optimizations !code/allocation/variable

-keep class org.xbill.DNS.** { *; }
-dontwarn org.xbill.DNS.**

# Marshmallow Notification
-dontwarn android.app.Notification
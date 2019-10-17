
# Xabber classes - many of them loaded via class loader
-keep class com.xabber.** { *; }

# Marshmallow removed Notification.setLatestEventInfo()
-dontwarn android.app.Notification

# RxJava
-dontwarn rx.internal.util.**
-dontwarn sun.misc.**

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
-dontobfuscate

# This class is only being referenced from navigation.xml
-keep class rs.ltt.jmap.mua.util.KeywordLabel

# JMAP entities
-keep class rs.ltt.jmap.common.** {*;}

# Bouncecastle provider (required for autocrypt)
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }

# Logger
-keep class org.slf4j.** {*;}
-keep class ch.qos.** {*;}

-dontwarn javax.mail.**
-dontwarn javax.naming.**
-dontwarn com.fasterxml.jackson.**
-dontwarn java.lang.ClassValue


# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

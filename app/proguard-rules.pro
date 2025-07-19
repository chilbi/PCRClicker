# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# 保留所有继承自 Activity 的类（避免被混淆后崩溃）
-keep public class * extends android.app.Activity

# 保留 R 文件中的所有资源 ID
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 保留所有 Serializable 实现类的成员
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留自定义服务类
-keep public class * extends android.accessibilityservice.AccessibilityService {
    public protected *;
}

# 保留 Manifest 中注册的 Service
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# keytool -genkey -v -keystore pcrclicker.jks -keyalg RSA -keysize 2048 -validity 10000 -alias pcrclicker
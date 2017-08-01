-dontwarn
-dontoptimize
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-applymapping build-outputs/demo-base-mapping.txt
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-ignorewarnings
-keepattributes *Annotation*
-ignorewarnings
-useuniqueclassmembernames
-dontwarn android.net.http.SslError


# *默认不添加混淆的行号，打包工具动态配置
# *内部测试包或灰度包，打包时保留行号配置，包体积会增加500KB以上
# *正式发布的渠道包或给第三方的包，不保留行号
#-renamesourcefileattribute SourceFile
#-keepattributes SourceFile,LineNumberTable


#-keep public class * extends android.app.Application
#-keep public class * extends android.app.Service
#-keep public class * extends android.content.BroadcastReceiver
#-keep public class * extends android.content.ContentProvider
#-keep public class * extends android.app.backup.BackupAgentHelper
#-keep public class * extends android.preference.Preference
#-keep public class com.android.vending.licensing.ILicensingService
-keep public class * extends android.os.IInterface

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keep public class * extends android.app.Fragment
-keep public class * extends android.support.v4.app.Fragment

-keep class com.huanju.chajiandemo.TestActivityTwo {*;}
-keep class com.huanju.chajianhuatest.bundlebase.BundleBaseActivity  {
      public <fields>;
      public <methods>;
}

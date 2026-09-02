# VerifyBlind Demo — R8 kuralları
#
# Ana uygulamayla aynı Play gerekliliğine tabi (DEX code optimization, min. %25, Şubat 2027).
# Ana uygulamadan farkı: burada yeniden-üretilebilirlik/DEX-hash şeffaflığı iddiası YOK, çünkü
# bu bir entegrasyon örneği; dolayısıyla obfuscation'ı kapatmak için bir gerekçemiz de yok ve
# varsayılan (açık) bırakılıyor.

# SDK'nın tamamı: demo bir entegrasyon örneği, SDK yüzeyi budanmamalı.
# Ayrıca SDK'nın Gson modelleri (StartAuthRequest, PartnerBackendResponse, PopResultResponse)
# yansımayla okunuyor — bu kural onları da kapsıyor.
-keep class com.verifyblind.sdk.** { *; }

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod

# Retrofit + R8 full mode (AGP 8.2 varsayılanı): keep edilmeyen sınıflardan generic imzalar
# silinir; SDK'nın API'si `suspend fun ...: Response<T>` olduğu için Retrofit dönüş tipini
# ParameterizedType'a cast ederken ClassCastException alır. Ana uygulamada üretimde yaşandı
# (Sentry 143088976) — orada Retrofit 2.9.0 bu kuralları taşımıyordu.
# SDK Retrofit 2.11 kullanıyor ve 2.11 bunları kendi taşıyor; yine de açıkça yazıyoruz:
# tekrar zararsız, ama sürüm düşerse sessizce kırılmasın.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Gson: TypeToken'ın generic bilgisi de full mode'da silinebiliyor.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-dontwarn okhttp3.**
-dontwarn retrofit2.**

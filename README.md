# VerifyBlind Demo (Android)

**[🇹🇷 Türkçe](#türkçe) · [🇬🇧 English](#english)**

---

## Türkçe

VerifyBlind Android SDK'sını (`sdk-android`) tüketen örnek uygulama — `example-mobile-ios`'in Android
karşılığı. Bir partner'ın VerifyBlind'i kendi uygulamasına nasıl entegre edeceğini gösterir.

- Bundle ID / paket: `com.verifyblind.example`
- Mağaza/görünen ad: **VerifyBlind Demo**
- SDK: [`sdk-android`](https://github.com/VerifyBlind/sdk-android) — yerel modül olarak dahil edilir
- **Cihaz attestation'ı YOKTUR** (bilinçli: VerifyBlind'in zero-knowledge güvenliğinin parçası değil)

### Akış
1. "VerifyBlind ile Doğrula" → SDK ephemeral RSA keypair üretir, partner backend'inden bir `nonce` alır.
2. SDK, VerifyBlind uygulamasını App Link ile açar (`app.verifyblind.com/request?...`).
3. Kullanıcı doğrulamayı VerifyBlind'de tamamlar; bu demo'ya dönünce sonuç poll edilir ve şifreli yanıt
   **lokalde** çözülür.

### Uygulamaya geri dönüş (deeplink)
VerifyBlind, doğrulama bitince (başarı **veya** iptal) kullanıcıyı bu demo'ya geri getirir:
1. SDK'ya geri-dönüş URL'i geçilir: `startAuthentication(..., returnUrl = "verifyblinddemo://callback")`.
2. `verifyblinddemo` şeması `AndroidManifest.xml`'de kayıtlıdır:
   `<data android:scheme="verifyblinddemo" android:host="callback" />`.
3. **Aynı şema** Partner Portal → *Ayarlar → Uygulama Geri-Dönüş Şeması*'na (`verifyblinddemo`) yazılmalıdır —
   VerifyBlind yalnızca şeması kayıtlıyla eşleşen return URL'i açar (fail-closed, açık-yönlendirme önlemi).

Bitince VerifyBlind `verifyblinddemo://callback?nonce=..&status=success` (ya da `status=cancelled`) açar →
demo öne gelir ve sonucu poll eder.

### Çalıştırma
1. Bu repo'yu `sdk-android` ile **yan yana** klonlayın — `settings.gradle.kts` SDK'yı
   `../sdk-android/verifyblind` yolundan dahil eder.
2. Android Studio'da açıp Gradle senkronize edin.
3. Kendi partner backend'inizle denemek için yapılandırmadaki `partnerBackendUrl`'i değiştirin.

> Adım adım kurulum (Play yükleme dahil): [`developer_setup_guide.md`](developer_setup_guide.md).

🌐 [verifyblind.com](https://verifyblind.com) · 📦 [Android SDK](https://github.com/VerifyBlind/sdk-android) · 🍎 [iOS demo](https://github.com/VerifyBlind/example-mobile-ios)

---

## English

An example app that consumes the VerifyBlind Android SDK (`sdk-android`) — the Android counterpart of
`example-mobile-ios`. It shows how a partner integrates VerifyBlind into their own app.

- Bundle ID / package: `com.verifyblind.example`
- Store / display name: **VerifyBlind Demo**
- SDK: [`sdk-android`](https://github.com/VerifyBlind/sdk-android) — included as a local module
- **No device attestation** (intentional: it is not part of VerifyBlind's zero-knowledge security)

### Flow
1. "Verify with VerifyBlind" → the SDK generates an ephemeral RSA keypair and gets a `nonce` from the partner backend.
2. The SDK opens the VerifyBlind app via an App Link (`app.verifyblind.com/request?...`).
3. The user completes verification in VerifyBlind; back in this demo the result is polled and the
   encrypted response is decrypted **locally**.

### Returning to your app (deeplink)
VerifyBlind brings the user back to this demo when the flow ends (success **or** cancel):
1. Pass a return URL to the SDK: `startAuthentication(..., returnUrl = "verifyblinddemo://callback")`.
2. The `verifyblinddemo` scheme is registered in `AndroidManifest.xml`:
   `<data android:scheme="verifyblinddemo" android:host="callback" />`.
3. The **same scheme** must be set in Partner Portal → *Settings → App Return Scheme* (`verifyblinddemo`) —
   VerifyBlind only opens a return URL whose scheme matches the registered one (fail-closed, prevents open-redirect).

When done, VerifyBlind opens `verifyblinddemo://callback?nonce=..&status=success` (or `status=cancelled`),
foregrounding the demo, which then polls for the result.

### Running
1. Clone this repo **alongside** `sdk-android` — `settings.gradle.kts` includes the SDK from
   `../sdk-android/verifyblind`.
2. Open in Android Studio and sync Gradle.
3. Change `partnerBackendUrl` in the configuration to try it with your own partner backend.

> Step-by-step setup (including Play upload): [`developer_setup_guide.md`](developer_setup_guide.md).

🌐 [verifyblind.com](https://verifyblind.com) · 📦 [Android SDK](https://github.com/VerifyBlind/sdk-android) · 🍎 [iOS demo](https://github.com/VerifyBlind/example-mobile-ios)

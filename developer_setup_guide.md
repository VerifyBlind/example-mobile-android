# VerifyBlind Android Test App - Geliştirici Kurulum Rehberi

Bu rehber, VerifyBlind Android SDK'sını kullanarak kendi partner hesabınızı test uygulamasına nasıl bağlayacağınızı ve güvenlik ayarlarını nasıl yapılandıracağınızı açıklar.

## 1. Başlangıç ve Bağlantı

Test uygulamasını kendi partner hesabınızla çalıştırmak için `local.properties` dosyasını yapılandırmanız gerekmektedir.

1.  Proje kök dizinindeki `local.properties.example` dosyasını `local.properties` adıyla kopyalayın.
2.  Aşağıdaki alanları kendi bilgilerinizle doldurun:

```properties
# Kendi partner backend URL'iniz (İsteği imzalayıp API'ye ileten endpoint)
verifyblind.partnerBackendUrl=https://sizin-partner-backend.com/api/verify/generate

# Opsiyonel: App Link baz URL'i (Özel bir ortam kullanmıyorsanız değiştirmeyin)
verifyblind.appLinkBase=https://app.verifyblind.com/request
```

## 2. Güvenlik Ayarları (Tavsiye Edilen)

Aşağıdaki ayarlar zorunlu değildir ancak partner hesabınızın güvenliğini sağlamak ve fraud (sahtecilik) girişimlerini önlemek için **şiddetle tavsiye edilir**.

### A. Certificate Pinning (Sertifika Sabitleme)

Uygulamanız ile partner backend'iniz arasındaki trafiğin (Man-in-the-Middle saldırılarıyla) izlenmesini engeller.

-   **Nasıl Yapılır?** Backend sunucunuzun SSL sertifikasının SHA-256 hash değerlerini `local.properties` dosyasına ekleyin.
-   **Neden Önemli?** Sadece güvenilen sertifikaya sahip sunucuyla konuşulmasını garanti eder.

```properties
# Virgülle ayrılmış sha256 hash'leri. En az bir asıl, bir yedek (backup) pin eklemeniz önerilir.
verifyblind.certificatePins=sha256/PRIMARY_HASH...,sha256/BACKUP_HASH...
```

### B. Play Integrity Attestation (Cihaz Onayı)

İşlemin gerçekten sizin orijinal uygulamanızdan ve güvenli bir Android cihazdan gelip gelmediğini doğrular.

-   **Nasıl Yapılır?**
    1.  [Google Cloud Console](https://console.cloud.google.com/) üzerinden bir proje oluşturun.
    2.  Play Integrity API'yi etkinleştirin.
    3.  Bulut Proje Numaranızı (Cloud Project Number) `local.properties` dosyasına ekleyin.
-   **Neden Önemli?** Emülatörler, rootlu cihazlar veya değiştirilmiş (tampered) APK'lar üzerinden gelen istekleri engellemenizi sağlar.

```properties
# Google Cloud Bulut Proje Numarası
verifyblind.cloudProjectNumber=123456789012
```

## 3. verifyEndpoint Yapılandırması

Partner backend tarafındaki polling (sonuç sorgulama) endpoint'inin adını belirtmek için `verifyblind.verifyEndpoint` anahtarını kullanabilirsiniz.

```properties
# Polling endpoint ismi. Varsayılan: verify
verifyblind.verifyEndpoint=verify
```

Bu değer, `partnerBackendUrl` sonuna eklenerek (örneğin: `.../verifyblind-android-test/verify`) sorgulama yapılır.

---
**Önemli Not:** Pinning ve Attestation ayarları opsiyoneldir. Ancak bu ayarlar eksik olduğunda sisteminiz Fraud ve Bot saldırılarına karşı açık hale gelebilir. Güvenli bir entegrasyon için bu adımları tamamlamanız şiddetle önerilir.

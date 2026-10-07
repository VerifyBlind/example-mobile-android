# VerifyBlind Android Demo — Geliştirici Kurulum Rehberi

Bu rehber, demo uygulamasını kendi partner hesabınıza nasıl bağlayacağınızı açıklar.

## 1. Bağlantı ayarları

Uygulama, ayarlarını proje kök dizinindeki `verifyblind.properties` dosyasından okur; `local.properties`
varsa aynı anahtarlar onunla ezilir. İki dosya da git'e girmez.

1. `local.properties.example` dosyasını `verifyblind.properties` (ya da `local.properties`) adıyla kopyalayın.
2. Alanları kendi bilgilerinizle doldurun:

```properties
# Partner backend'inizdeki aracı (proxy) endpoint'in taban adresi.
# Bu endpoint public_key ve custom_data'yı POST /api/pop/generate'e iletir, X-API-Key ekler ve
# validations'ı uygulamadan değil KENDİ ayarından koyar (istek değiştirilebilir: "18+" yerine
# "1+" soran biri de imzalı age: true alır).
VERIFYBLIND_PARTNER_BACKEND_URL=https://sizin-partner-backend.com/api/

# Taban adrese eklenen göreli yol (varsayılan: generate)
VERIFYBLIND_GENERATE_ENDPOINT=generate

# VerifyBlind App Link adresi (değiştirmeyin)
VERIFYBLIND_APP_LINK_BASE=https://app.verifyblind.com/request

# Sonucun sorgulandığı VerifyBlind API (varsayılan: https://api.verifyblind.com)
VERIFYBLIND_API_URL=https://api.verifyblind.com
```

Sonuç, SDK tarafından doğrudan VerifyBlind API'sinden (`GET /api/pop/result/{nonce}`) sorgulanır ve
cihazda çözülür; partner backend'inizde ayrı bir sorgulama endpoint'i gerekmez.

## 2. Sertifika sabitleme (isteğe bağlı)

Uygulama ile partner backend'iniz arasındaki trafiği yalnızca belirlediğiniz sertifikalara bağlar.
Backend sunucunuzun sertifika hash'lerini virgülle ayırarak ekleyin; en az bir asıl ve bir yedek pin
önerilir.

```properties
verifyblind.certificatePins=sha256/PRIMARY_HASH...,sha256/BACKUP_HASH...
```

Uygulamadaki "Güvenlik kontrollerini atla" anahtarı açıksa sabitleme devre dışı kalır; bu anahtar
yalnızca geliştirme içindir.

## 3. Yerel geliştirme

Debug derlemede `USE_LOCAL_API=true` verirseniz uygulama yerel ortamı kullanır:

```properties
USE_LOCAL_API=true
VERIFYBLIND_PARTNER_BACKEND_URL_LOCAL=http://10.0.2.2:3001/api/
VERIFYBLIND_API_URL_LOCAL=http://10.0.2.2:5102
```

Release derlemesi her zaman `VERIFYBLIND_PARTNER_BACKEND_URL` ve `VERIFYBLIND_API_URL` değerlerini kullanır.

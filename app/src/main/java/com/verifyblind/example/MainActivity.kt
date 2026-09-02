package com.verifyblind.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import android.text.format.DateFormat
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.verifyblind.sdk.VerifyBlindAndroidSDK
import com.verifyblind.sdk.VerifyBlindConfig
import com.verifyblind.sdk.VerifyBlindException
import com.verifyblind.example.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Date
import kotlin.coroutines.coroutineContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Aktif poll coroutine'i — yalnızca ön planda yaşar (onResume başlatır, onPause iptal eder). */
    private var pollJob: Job? = null

    /** Sunucu-kapılı yerel ÖRNEK (EXAMPLE) önizleme modu — app-config'ten launch'ta okunur. */
    private var previewEnabled = false

    /**
     * SDK instance ve aktif nonce companion'da tutulur ki Activity yeniden yaratılsa bile
     * (arka planda kill, config change) ephemeral keypair ve nonce kaybolmasın.
     * Not: tam process death'te bunlar da kaybolur — o durumda "oturum yeniden başlatıldı" gösterilir.
     */
    companion object {
        private const val KEY_ACTIVE_NONCE = "active_nonce"
        // VerifyBlind'ın doğrulama bitince bu uygulamayı öne getirmek için açacağı geri-dönüş deeplink'i.
        // Şema (verifyblinddemo) manifest'te kayıtlı ve partner-portal'daki "app return scheme" ile eşleşmeli.
        private const val RETURN_URL = "verifyblinddemo://callback"
        private var activeSdk: VerifyBlindAndroidSDK? = null
        private var activeNonce: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeToEdgeInsets()

        binding.btnStartAuth.setOnClickListener { onStartAuthClicked() }
        binding.btnCancelPolling.setOnClickListener { cancelPollingAndReset() }
        binding.btnClearLog.setOnClickListener {
            binding.tvLog.text = getString(R.string.log_cleared)
        }
        binding.btnCopyResult.setOnClickListener { copyResultToClipboard() }
        binding.btnCopyLog.setOnClickListener { copyLogToClipboard() }

        log(getString(R.string.log_ready))
        log(getString(R.string.log_env, if (BuildConfig.USE_LOCAL_API) "LOCAL" else "PRODUCTION"))

        // Recreate sonrası aktif nonce'u geri yükle (companion boşsa bundle'dan)
        if (activeNonce == null) {
            activeNonce = savedInstanceState?.getString(KEY_ACTIVE_NONCE)
        }

        // Geri tuşu uygulamayı tamamen kapatsın (recents'ten de kalksın).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAndRemoveTask()
            }
        })

        handleIncomingIntent(intent)

        lifecycleScope.launch {
            previewEnabled = AppConfigClient.isPreviewEnabled(
                BuildConfig.VERIFYBLIND_API_URL, BuildConfig.VERSION_NAME
            )
            if (previewEnabled) binding.btnStartAuth.text = getString(R.string.btn_preview)
        }
    }

    /**
     * Edge-to-edge (targetSdk 35 / Android 15): sistem çubukları şeffaftır ve içerik
     * onların arkasına çizilir. İçeriğin status bar'ın ALTINDAN başlaması için
     * scroll alanına system-bar üst/alt padding'i uygula. Koyu zemin → açık (beyaz) ikonlar.
     */
    private fun applyEdgeToEdgeInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.scrollRoot.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // KRİTİK: Poll YALNIZCA ön planda yapılır. Kullanıcı VerifyBlind app'ten döndüğünde
        // burası tetiklenir ve ağ/DNS ön planda sorunsuz çalışır. (Arka planda poll etmek,
        // agresif batarya/ağ kısıtı olan cihazlarda "Unable to resolve host" hatasına yol açıyordu.)
        resumePollingIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        // Arka plana giderken poll'u durdur: arka plan DNS hatalarını ve boşa giden turları engeller.
        // onResume tekrar başlatır.
        pollJob?.cancel()
        pollJob = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        activeNonce?.let { outState.putString(KEY_ACTIVE_NONCE, it) }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data?.scheme == "verifyblinddemo") {
            val uri = intent.data
            val nonce = uri?.getQueryParameter("nonce")
            val status = uri?.getQueryParameter("status")

            log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            log(getString(R.string.log_deeplink))
            log("URI: $uri")

            val resolved = nonce ?: activeNonce
            if (resolved != null && status != "cancelled" && status != "failed") {
                activeNonce = resolved
                log(getString(R.string.log_deeplink_nonce, resolved))
                // Polling onResume() içinde başlatılır (ön plan garantisi).
            } else {
                log(getString(R.string.log_deeplink_fail, status ?: "null"))
            }
        }
    }

    // ============================================================
    //  Doğrulamayı başlat
    // ============================================================
    private fun onStartAuthClicked() {
        if (previewEnabled) { runPreviewFlow(); return }

        val partnerBackendUrl = BuildConfig.VERIFYBLIND_PARTNER_BACKEND_URL
        val skipSecurity = binding.switchSkipSecurity.isChecked

        if (partnerBackendUrl.isBlank()) {
            toast(getString(R.string.toast_no_backend))
            return
        }

        val vals = mutableMapOf<String, Any>()
        if (binding.cbValidationAge.isChecked) vals["age"] = "18+"
        if (binding.cbValidationUserId.isChecked) vals["user_id"] = true
        val validations = vals.ifEmpty { null }

        binding.btnStartAuth.isEnabled = false
        binding.tvStatusBadge.visibility = View.GONE
        showOverlay(R.string.overlay_opening)
        log("━━━━━━━━━━━━━━━━━━")
        log(getString(R.string.log_start))
        if (validations != null) log(getString(R.string.log_validations, validations.toString()))

        lifecycleScope.launch {
            try {
                val pins = BuildConfig.VERIFYBLIND_CERTIFICATE_PINS
                    .split(",")
                    .filter { it.isNotBlank() }

                val config = VerifyBlindConfig(
                    partnerBackendUrl = partnerBackendUrl,
                    verifyblindAppLinkBase = BuildConfig.VERIFYBLIND_APP_LINK_BASE,
                    verifyblindApiUrl = BuildConfig.VERIFYBLIND_API_URL,
                    skipSecurityChecks = skipSecurity,
                    certificatePins = if (skipSecurity || pins.isEmpty()) null else pins,
                    generateEndpoint = BuildConfig.VERIFYBLIND_GENERATE_ENDPOINT
                )

                val sdk = VerifyBlindAndroidSDK(config)
                activeSdk = sdk

                log(getString(R.string.log_opening))
                val result = sdk.startAuthentication(
                    context = this@MainActivity,
                    validations = validations,
                    returnUrl = RETURN_URL
                )

                activeNonce = result.nonce
                log(getString(R.string.log_request_created, result.nonce))
                log(getString(R.string.log_complete_in_app))

                binding.tvResultValidations.text = getString(R.string.result_waiting)
                // NOT: Poll burada BAŞLAMAZ — onResume() (ön plan) tetikler.
                // Overlay "açılıyor" mesajıyla görünür kalır; dönüşte onResume "bekleniyor"a çevirir.
            } catch (e: VerifyBlindException) {
                if (e.code == VerifyBlindException.ErrorCode.USER_CANCELLED) {
                    showCancelled(e)
                } else {
                    log("❌ [${e.code}] ${e.message}")
                    reportFailure(e, getString(R.string.err_start, e.message ?: getString(R.string.err_generic)))
                }
                hideOverlay()
                binding.btnStartAuth.isEnabled = true
            } catch (e: Exception) {
                log("❌ ${e.message}")
                reportFailure(e, e.message ?: getString(R.string.err_generic))
                hideOverlay()
                binding.btnStartAuth.isEnabled = true
            }
        }
    }

    // ============================================================
    //  ÖRNEK (EXAMPLE) önizleme akışı — tamamen yerel, ağ/SDK/main app YOK
    // ============================================================
    private fun runPreviewFlow() {
        val ageOver18 = binding.cbValidationAge.isChecked
        val requestUserId = binding.cbValidationUserId.isChecked

        binding.btnStartAuth.isEnabled = false
        binding.tvStatusBadge.visibility = View.GONE
        showOverlay(R.string.overlay_opening)
        log("━━━━━━━━━━━━━━━━━━")
        log(getString(R.string.log_start))
        log(getString(R.string.log_preview_marker))

        lifecycleScope.launch {
            delay(600); log(getString(R.string.log_opening))
            delay(600); log(getString(R.string.log_request_created, PreviewSimulator.EXAMPLE_NONCE))
            log(getString(R.string.log_complete_in_app))
            delay(600); log(getString(R.string.log_polling))
            delay(800)
            val exampleResult = PreviewSimulator.buildExampleResult(ageOver18, requestUserId)
            binding.tvResultValidations.text = renderResultJson(exampleResult)
            showStatus(getString(R.string.status_example), R.color.example_text, R.drawable.bg_status_pill_example)
            log(getString(R.string.log_result_applied))
            hideOverlay()
            binding.btnStartAuth.isEnabled = true
        }
    }

    // ============================================================
    //  Ön plan polling
    // ============================================================
    /**
     * Kullanıcı beklemeyi keser (VerifyBlind'da vazgeçti / akış yarıda kaldı). Polling'i durdurur,
     * aktif nonce'u bırakır ve başlat butonunu yeniden açar → hemen yeni istek gönderilebilir (Item 3c).
     */
    private fun cancelPollingAndReset() {
        pollJob?.cancel()
        pollJob = null
        activeNonce = null
        hideOverlay()
        binding.btnStartAuth.isEnabled = true
        binding.tvStatusBadge.visibility = View.GONE
        log(getString(R.string.log_polling_cancelled))
    }

    private fun resumePollingIfNeeded() {
        val nonce = activeNonce ?: return
        if (pollJob?.isActive == true) return

        val sdk = activeSdk
        if (sdk == null) {
            // Process death sonrası keypair kayboldu → şifreli yanıt çözülemez.
            log(getString(R.string.log_session_restarted))
            activeNonce = null
            hideOverlay()
            binding.btnStartAuth.isEnabled = true
            return
        }

        binding.btnStartAuth.isEnabled = false
        showOverlay(R.string.overlay_waiting)
        pollJob = lifecycleScope.launch { pollForResult(sdk, nonce) }
    }

    private suspend fun pollForResult(sdk: VerifyBlindAndroidSDK, nonce: String) {
        log(getString(R.string.log_polling))
        var pollCount = 0
        val maxPolls = 60 // 60 * 1s = 60sn ön plan polling
        try {
            while (pollCount < maxPolls && coroutineContext.isActive) {
                delay(1000)
                pollCount++

                val result = try {
                    sdk.checkVerificationResult(nonce)
                } catch (e: VerifyBlindException) {
                    if (e.code == VerifyBlindException.ErrorCode.USER_CANCELLED) {
                        showCancelled(e)
                    } else {
                        log("❌ [${e.code}] ${e.message}")
                        reportFailure(e, e.message ?: getString(R.string.err_generic))
                    }
                    activeNonce = null
                    return
                }

                if (result != null) {
                    applyResult(result)
                    log(getString(R.string.log_result_applied))
                    activeNonce = null
                    return
                }

                if (pollCount % 10 == 0) log(getString(R.string.log_waiting_secs, pollCount))
            }
            // Zaman aşımı: activeNonce korunur; tekrar ön plana gelince yeniden denenir.
            log(getString(R.string.log_timeout))
        } finally {
            hideOverlay()
            binding.btnStartAuth.isEnabled = true
        }
    }

    // ============================================================
    //  Sonuç / durum gösterimi
    // ============================================================
    private fun applyResult(result: Map<String, Any>) {
        runOnUiThread {
            binding.tvResultValidations.text = renderResultJson(result)
            showStatus(getString(R.string.status_success), R.color.success_text, R.drawable.bg_status_pill)
        }
    }

    // Demo amacıyla partner backend'in GERÇEKTE aldığı payload aynen gösterilir
    // (user_id = TCKN değil, partner'a özel takma kimlik → maskelenmez).
    private fun renderResultJson(result: Map<String, Any>): String {
        val json = JSONObject()
        result["user_id"]?.let { json.put("user_id", it.toString()) }
        (result["validations"] as? Map<*, *>)?.forEach { (k, v) -> json.put(k.toString(), formatVal(v)) }
        result["nsbd_id"]?.let { json.put("nsbd_id", it.toString()) }
        result["doc_id"]?.let { json.put("doc_id", it.toString()) }
        result.forEach { (k, v) ->
            if (k !in setOf("user_id", "nsbd_id", "doc_id", "nonce", "validations")) json.put(k, formatVal(v))
        }
        return json.toString(2)
    }

    private fun showCancelled(e: VerifyBlindException) {
        val (title, detail) = when (e.cancelReason) {
            "no_card_registered" -> getString(R.string.cancel_no_card_title) to getString(R.string.cancel_no_card_detail)
            "user_declined" -> getString(R.string.cancel_declined_title) to getString(R.string.cancel_declined_detail)
            "fingerprint_failed" -> getString(R.string.cancel_fingerprint_title) to getString(R.string.cancel_fingerprint_detail)
            "session_expired" -> getString(R.string.cancel_session_title) to getString(R.string.cancel_session_detail)
            else -> getString(R.string.cancel_generic_title) to getString(R.string.cancel_generic_detail)
        }
        log(getString(R.string.log_cancel, title, e.cancelReason ?: "user_cancelled"))
        runOnUiThread {
            binding.tvResultValidations.text = getString(R.string.result_cancelled_block, title, detail)
            showStatus("● $title", R.color.error_text, R.drawable.bg_status_pill_error)
        }
    }

    /**
     * Hatayı kullanıcıya gösterir: bağlantı sorunuysa kanonik "internetini kontrol et" metni +
     * [Tamam] diyaloğu, değilse geliştiriciye dönük teknik metin (bu bir entegrasyon örneği).
     *
     * Neden: ham istisna metni ekrana basıldığında kullanıcı internetsizken
     * `Unable to resolve host "api.verifyblind.com"` görüyordu — anlaşılmaz olmasının yanında
     * sunucu adını içerdiği için servis çökmüş izlenimi veriyordu.
     */
    private fun reportFailure(error: Throwable, technicalMessage: String) {
        if (isConnectionProblem(error)) {
            showError(getString(R.string.err_no_connection))
            showDialog(getString(R.string.err_no_connection_title), getString(R.string.err_no_connection))
        } else {
            showError(technicalMessage)
            showDialog(getString(R.string.err_title), technicalMessage)
        }
    }

    /**
     * HTTP cevabı hiç alınamadı mı (DNS/TCP/TLS/timeout)? SDK bunu `NETWORK_ERROR` ile bildirir;
     * doğrudan yakalanan istisnalarda taşıma hatalarının tamamı `IOException` türevidir. Sebep
     * zinciri de taranır (SDK istisnayı sarmalar).
     */
    private fun isConnectionProblem(error: Throwable): Boolean {
        if ((error as? VerifyBlindException)?.code == VerifyBlindException.ErrorCode.NETWORK_ERROR) return true
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < 5) {
            if (current is java.io.IOException) return true
            current = current.cause
            depth++
        }
        return false
    }

    private fun showDialog(title: String, message: String) {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.btn_ok), null)
                .show()
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            binding.tvResultValidations.text = getString(R.string.result_error_block, message)
            showStatus(getString(R.string.status_error), R.color.error_text, R.drawable.bg_status_pill_error)
        }
    }

    private fun showStatus(text: String, @androidx.annotation.ColorRes colorRes: Int, @androidx.annotation.DrawableRes bgRes: Int) {
        binding.tvStatusBadge.apply {
            this.text = text
            setTextColor(resources.getColor(colorRes, theme))
            setBackgroundResource(bgRes)
            visibility = View.VISIBLE
        }
    }

    // ============================================================
    //  Overlay (yükleniyor)
    // ============================================================
    private fun showOverlay(@StringRes msgRes: Int) {
        runOnUiThread {
            binding.tvOverlayMessage.setText(msgRes)
            binding.overlayLoading.visibility = View.VISIBLE
        }
    }

    private fun hideOverlay() {
        runOnUiThread { binding.overlayLoading.visibility = View.GONE }
    }

    private fun copyResultToClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("VerifyBlind", binding.tvResultValidations.text))
        toast(getString(R.string.toast_copied))
    }

    private fun copyLogToClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("VerifyBlind Log", binding.tvLog.text))
        toast(getString(R.string.toast_log_copied))
    }

    private fun formatVal(v: Any?): String = when (v) {
        true -> getString(R.string.value_yes)
        false -> getString(R.string.value_no)
        else -> v?.toString() ?: "null"
    }

    private fun log(message: String) {
        runOnUiThread {
            val time = DateFormat.format("HH:mm:ss", Date()).toString()
            binding.tvLog.append("[$time] $message\n")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

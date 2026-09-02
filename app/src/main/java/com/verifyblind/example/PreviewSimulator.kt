package com.verifyblind.example

/**
 * Self-contained (Android-free) logic for the local EXAMPLE preview.
 * No network, no SDK, no main app. Produces the same result shape that
 * [MainActivity.applyResult]/[MainActivity.renderResultJson] renders from a real verification.
 */
object PreviewSimulator {
    const val EXAMPLE_NONCE = "example-nonce"

    fun buildExampleResult(ageOver18: Boolean, requestUserId: Boolean): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        if (requestUserId) result["user_id"] = "example-user-id-token"
        val validations = mutableMapOf<String, Any>()
        if (ageOver18) validations["age"] = "18+"
        if (validations.isNotEmpty()) result["validations"] = validations
        result["nsbd_id"] = "example"
        result["doc_id"] = "example"
        return result
    }
}

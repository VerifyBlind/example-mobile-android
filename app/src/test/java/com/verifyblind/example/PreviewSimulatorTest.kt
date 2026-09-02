package com.verifyblind.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PreviewSimulatorTest {

    @Test
    fun includesRequestedValidationsAndUserId() {
        val r = PreviewSimulator.buildExampleResult(ageOver18 = true, requestUserId = true)
        assertEquals("example-user-id-token", r["user_id"])
        @Suppress("UNCHECKED_CAST")
        val validations = r["validations"] as Map<String, Any>
        assertEquals("18+", validations["age"])
        assertEquals("example", r["nsbd_id"])
        assertEquals("example", r["doc_id"])
    }

    @Test
    fun omitsUnrequestedFields() {
        val r = PreviewSimulator.buildExampleResult(ageOver18 = false, requestUserId = false)
        assertFalse(r.containsKey("user_id"))
        assertFalse(r.containsKey("validations"))
    }
}

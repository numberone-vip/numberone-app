package com.numberone.numberone.core.error

import kotlinx.serialization.SerializationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ErrorPresentationTest {

    @After fun reset() { ErrorPresentation.showTechnicalDetails = false }

    @Test fun release_shows_friendly_message_and_category_code_only() {
        ErrorPresentation.showTechnicalDetails = false
        val error = IOException("https://internal.example/rpc failed")
        assertEquals(ErrorMapper.toUserMessage(error) + "\n\n[NETWORK]", ErrorPresentation.describe(error))
    }

    @Test fun release_never_leaks_raw_rpc_or_url_text() {
        ErrorPresentation.showTechnicalDetails = false
        val text = ErrorPresentation.describe(IllegalStateException("admin_list_customers: secret_column missing"))
        assertFalse(text.contains("admin_list_customers"))
        assertFalse(text.contains("secret_column"))
    }

    @Test fun debug_adds_exception_type_and_raw_message() {
        ErrorPresentation.showTechnicalDetails = true
        val text = ErrorPresentation.describe(RuntimeException("boom"))
        assertTrue(text.contains("[RuntimeException: boom]"))
    }

    @Test fun debug_unwraps_typed_exception_to_its_cause() {
        ErrorPresentation.showTechnicalDetails = true
        val text = ErrorPresentation.describe(AppException(AppError.ServerResponse, SerializationException("bad")))
        assertTrue(text.contains("SerializationException"))
    }

    @Test fun debug_does_not_repeat_friendly_text_as_technical() {
        ErrorPresentation.showTechnicalDetails = true
        val error = UserFacingException("نص جاهز")
        assertEquals(ErrorMapper.toUserMessage(error), ErrorPresentation.describe(error))
    }
}

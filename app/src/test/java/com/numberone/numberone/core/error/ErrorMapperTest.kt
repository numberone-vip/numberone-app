package com.numberone.numberone.core.error

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ErrorMapperTest {

    @Test fun network_failure_is_classified_as_network() {
        assertEquals(AppError.Network, ErrorMapper.classify(IOException("socket closed")))
    }

    @Test fun malformed_body_is_server_response_error() {
        assertEquals(AppError.ServerResponse, ErrorMapper.classify(SerializationException("bad json")))
    }

    @Test fun supabase_auth_messages_map_to_auth_errors() {
        assertEquals(AppError.InvalidCredentials, ErrorMapper.classify(Exception("Invalid login credentials")))
        assertEquals(AppError.EmailNotConfirmed, ErrorMapper.classify(Exception("Email not confirmed")))
        assertEquals(AppError.EmailAlreadyRegistered, ErrorMapper.classify(Exception("User already registered")))
        assertEquals(AppError.WeakPassword, ErrorMapper.classify(Exception("Password should be at least 6 characters")))
        assertEquals(AppError.RateLimited, ErrorMapper.classify(Exception("over_request_rate_limit")))
    }

    @Test fun session_and_business_errors_are_classified() {
        assertEquals(AppError.SessionExpired, ErrorMapper.classify(Exception("JWT expired")))
        assertEquals(AppError.NoStock, ErrorMapper.classify(Exception("No stock for operator")))
        assertEquals(AppError.InsufficientBalance, ErrorMapper.classify(Exception("insufficient funds")))
    }

    @Test fun arabic_validation_message_is_passed_through() {
        val error = IllegalArgumentException("أدخل رقم معاملة صالحاً")
        assertEquals(AppError.Localized("أدخل رقم معاملة صالحاً"), ErrorMapper.classify(error))
    }

    @Test fun english_illegal_argument_is_not_leaked_as_raw_text() {
        assertEquals(AppError.Unknown, ErrorMapper.classify(IllegalArgumentException("rpc admin_x failed")))
    }

    @Test fun typed_app_exception_is_returned_unchanged() {
        assertEquals(AppError.InsufficientBalance, ErrorMapper.classify(AppException(AppError.InsufficientBalance)))
    }

    @Test fun virtual_number_provider_messages_are_classified() {
        assertEquals(AppError.PendingReview, ErrorMapper.classify(Exception("payment not confirmed yet")))
        assertEquals(AppError.DuplicateRequest, ErrorMapper.classify(Exception("request already used with different params")))
        assertEquals(AppError.PendingOrderExists, ErrorMapper.classify(Exception("order still pending")))
        assertEquals(AppError.NotFound, ErrorMapper.classify(Exception("activation not found")))
        assertEquals(AppError.ProviderUnavailable, ErrorMapper.classify(Exception("provider failed (400)")))
    }

    @Test fun typed_service_in_use_error_is_preserved() {
        assertEquals(AppError.ServiceInUse, ErrorMapper.classify(AppException(AppError.ServiceInUse)))
    }

    @Test fun money_server_messages_are_classified_precisely() {
        assertEquals(AppError.TooManyPendingOrders, ErrorMapper.classify(Exception("Too many orders are awaiting payment review")))
        assertEquals(AppError.PendingOrderExists, ErrorMapper.classify(Exception("You already have a pending payment order")))
        assertEquals(AppError.OrderExpired, ErrorMapper.classify(Exception("Order is expired or cannot accept payment")))
        assertEquals(AppError.InsufficientBalance, ErrorMapper.classify(Exception("Insufficient wallet balance")))
    }

    @Test fun null_and_unknown_fall_back_to_unknown() {
        assertEquals(AppError.Unknown, ErrorMapper.classify(null))
        assertEquals(AppError.Unknown, ErrorMapper.classify(RuntimeException("something odd")))
    }

    @Test fun user_facing_exception_keeps_its_text() {
        assertEquals(AppError.Localized("نص جاهز"), ErrorMapper.classify(UserFacingException("نص جاهز")))
    }
}

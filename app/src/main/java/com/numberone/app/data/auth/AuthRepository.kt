package com.numberone.app.data.auth

import com.numberone.app.data.remote.SupabaseClientProvider
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.loginWith
import io.github.jan.supabase.gotrue.signUpWith
import io.github.jan.supabase.gotrue.sendRecoveryEmail
import io.github.jan.supabase.gotrue.logout
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.eq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

enum class StaffAccessRole {
    ADMIN,
    STAFF,
}

class AuthRepository {
    private val client = SupabaseClientProvider.client

    suspend fun signInForAdmin(email: String, password: String): Result<StaffAccessRole> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(email.isNotBlank()) { "أدخل البريد الإلكتروني." }
                require(password.isNotBlank()) { "أدخل كلمة المرور." }

                client.gotrue.loginWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }

                val userId = client.gotrue.currentSessionOrNull()?.user?.id
                    ?: error("تعذر قراءة جلسة تسجيل الدخول.")

                val adminRows = client.postgrest["admin_users"]
                    .select {
                        eq("user_id", userId)
                    }
                    .decodeList<JsonObject>()

                if (adminRows.isNotEmpty()) {
                    return@runCatching StaffAccessRole.ADMIN
                }

                val staffRows = client.postgrest["staff_members"]
                    .select {
                        eq("user_id", userId)
                    }
                    .decodeList<JsonObject>()

                val activeStaff = staffRows.firstOrNull { row ->
                    row["is_active"]?.jsonPrimitive?.booleanOrNull == true
                }

                if (activeStaff != null) {
                    return@runCatching StaffAccessRole.STAFF
                }

                client.gotrue.logout()
                error("هذا الحساب لا يملك صلاحية دخول لوحة الإدارة.")
            }
        }

    fun currentCustomerUserId(): String? =
        client.gotrue.currentSessionOrNull()?.user?.id

    /**
     * Restores the correct destination when an existing Supabase session belongs to staff.
     * A persisted administrator session must not be mistaken for a customer session.
     */
    suspend fun currentStaffAccessRole(): StaffAccessRole? =
        withContext(Dispatchers.IO) {
            val userId = client.gotrue.currentSessionOrNull()?.user?.id
                ?: return@withContext null

            val adminRows = client.postgrest["admin_users"]
                .select { eq("user_id", userId) }
                .decodeList<JsonObject>()

            if (adminRows.isNotEmpty()) {
                return@withContext StaffAccessRole.ADMIN
            }

            val staffRows = client.postgrest["staff_members"]
                .select { eq("user_id", userId) }
                .decodeList<JsonObject>()

            val activeStaff = staffRows.firstOrNull { row ->
                row["is_active"]?.jsonPrimitive?.booleanOrNull == true
            }

            if (activeStaff != null) StaffAccessRole.STAFF else null
        }

    suspend fun signInCustomer(email: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(email.isNotBlank()) { "أدخل البريد الإلكتروني." }
                require(password.isNotBlank()) { "أدخل كلمة المرور." }
                client.gotrue.loginWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                client.gotrue.currentSessionOrNull()?.user?.id
                    ?: error("تم تسجيل الدخول لكن تعذر قراءة حسابك.")
            }
        }

    suspend fun signUpCustomer(email: String, password: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(email.isNotBlank()) { "أدخل البريد الإلكتروني." }
                require(android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                    "أدخل بريداً إلكترونياً صحيحاً."
                }
                require(password.length >= 8) { "كلمة المرور يجب أن تكون 8 أحرف على الأقل." }
                client.gotrue.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                client.gotrue.currentSessionOrNull() != null
            }
        }

    suspend fun sendCustomerPasswordReset(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(email.isNotBlank()) { "أدخل بريدك الإلكتروني أولاً." }
                client.gotrue.sendRecoveryEmail(email.trim())
            }
        }

    suspend fun signOutCustomer() {
        withContext(Dispatchers.IO) {
            client.gotrue.logout()
        }
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            client.gotrue.logout()
        }
    }
}

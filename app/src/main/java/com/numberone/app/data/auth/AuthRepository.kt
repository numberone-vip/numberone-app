package com.numberone.app.data.auth

import com.numberone.app.data.remote.SupabaseClientProvider
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.loginWith
import io.github.jan.supabase.gotrue.logout
import io.github.jan.supabase.gotrue.signUpWith
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

    suspend fun signInForCustomer(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(email.isNotBlank()) { "أدخل البريد الإلكتروني." }
                require(password.isNotBlank()) { "أدخل كلمة المرور." }
                client.gotrue.loginWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                check(client.gotrue.currentSessionOrNull()?.user?.id != null) {
                    "لم يتم إنشاء جلسة دخول. حاول مرة أخرى."
                }
            }
        }

    suspend fun signUpForCustomer(email: String, password: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(email.isNotBlank()) { "أدخل البريد الإلكتروني." }
                require(password.length >= 8) { "كلمة المرور يجب أن تكون 8 أحرف على الأقل." }
                client.gotrue.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                // false means Supabase accepted signup but email confirmation is still required.
                client.gotrue.currentSessionOrNull()?.user?.id != null
            }
        }

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
                    .select { eq("user_id", userId) }
                    .decodeList<JsonObject>()

                if (adminRows.isNotEmpty()) {
                    return@runCatching StaffAccessRole.ADMIN
                }

                val staffRows = client.postgrest["staff_members"]
                    .select { eq("user_id", userId) }
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

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            client.gotrue.logout()
        }
    }

    fun currentUserId(): String? = client.gotrue.currentSessionOrNull()?.user?.id
}

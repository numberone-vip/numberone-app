package com.numberone.numberone.data.remote

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.error.UserFacingException
import com.numberone.numberone.domain.model.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.cancellation.CancellationException

class SupabaseAuthDataSource {
    private val supabase = SupabaseClientProvider.client

    suspend fun signIn(email: String, password: String): UserProfile {
        supabase.auth.signInWith(Email) { this.email = email; this.password = password }
        return currentProfile() ?: throw IllegalStateException(tr("تعذر إنشاء جلسة المستخدم"))
    }

    suspend fun signUp(email: String, password: String) {
        require(password.length >= 8) { tr("كلمة المرور يجب ألا تقل عن 8 أحرف") }
        if (isEmailAvailable(email) == false) {
            throw UserFacingException(tr("هذا البريد الإلكتروني مسجل مسبقاً. سجّل الدخول أو استخدم بريداً آخر."))
        }
        // When the server hides duplicates, Supabase's own "already registered" error is mapped by ErrorMapper.
        supabase.auth.signUpWith(Email) { this.email = email; this.password = password }
    }

    /**
     * Optional duplicate check backed by the `check_email_available` RPC
     * (see supabase/optional/check_email_available.sql).
     * Returns null when the RPC is not deployed or unreachable, so sign-up still works without it.
     */
    private suspend fun isEmailAvailable(email: String): Boolean? = try {
        supabase.postgrest.rpcDecode<Boolean>(
            "check_email_available",
            buildJsonObject { put("p_email", email.trim().lowercase()) }
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /** UI hint only; the server re-checks is_admin() inside every admin RPC. */
    suspend fun isAdmin(): Boolean = supabase.postgrest.rpcDecode<Boolean>("is_admin")
    suspend fun signOut() = supabase.auth.signOut()
    fun getCurrentUser(): UserProfile? = currentProfile()

    private fun currentProfile(): UserProfile? {
        val user = supabase.auth.currentUserOrNull() ?: return null
        return UserProfile(id = user.id, email = user.email, phone = user.phone)
    }
}

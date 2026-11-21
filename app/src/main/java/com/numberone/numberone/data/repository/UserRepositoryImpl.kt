
package com.numberone.numberone.data.repository

import com.numberone.numberone.data.remote.SupabaseAuthDataSource
import com.numberone.numberone.domain.model.UserProfile
import com.numberone.numberone.domain.repository.UserRepository

/**
 * Number One - User Repository Implementation
 * Connects authentication operations to Supabase.
 * Safely wraps errors using Kotlin Result.
 */
class UserRepositoryImpl(
    private val authDataSource: SupabaseAuthDataSource
) : UserRepository {

    private suspend fun attachUserRole(
        user: UserProfile
    ): UserProfile {

        val adminStatus = runCatching {
            authDataSource.isAdmin()
        }.getOrDefault(false)

        return user.copy(
            isAdmin = adminStatus
        )
    }

    override suspend fun signIn(
        email: String,
        password: String
    ): Result<UserProfile> {

        return runCatching {
            val user = authDataSource.signIn(
                email = email,
                password = password
            )

            attachUserRole(user)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        authDataSource.signUp(email.trim(), password)
    }

    override suspend fun signOut(): Result<Unit> {
        return runCatching {
            authDataSource.signOut()
        }
    }

    override suspend fun getCurrentUser(): Result<UserProfile?> {

        return runCatching {
            val user = authDataSource.getCurrentUser()

            if (user == null) {
                null
            } else {
                attachUserRole(user)
            }
        }
    }

    override suspend fun isAdmin(): Result<Boolean> {
        return runCatching {
            authDataSource.isAdmin()
        }
    }
}

package com.numberone.numberone.domain.repository

import com.numberone.numberone.domain.model.UserProfile

interface UserRepository {
    suspend fun signIn(email: String, password: String): Result<UserProfile>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): Result<UserProfile?>
    suspend fun isAdmin(): Result<Boolean>
}

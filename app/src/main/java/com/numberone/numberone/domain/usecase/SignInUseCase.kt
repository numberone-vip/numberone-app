
package com.numberone.numberone.domain.usecase

import com.numberone.numberone.domain.model.UserProfile
import com.numberone.numberone.domain.repository.UserRepository

/**
 * Number One - Sign In Use Case
 * Validates login credentials before authentication.
 */
class SignInUseCase(
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<UserProfile> {

        val cleanedEmail = email.trim()

        if (cleanedEmail.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Email cannot be empty.")
            )
        }

        val emailRegex =
            Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        if (!emailRegex.matches(cleanedEmail)) {
            return Result.failure(
                IllegalArgumentException("Invalid email address.")
            )
        }

        if (password.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Password cannot be empty.")
            )
        }

        return userRepository.signIn(
            email = cleanedEmail,
            password = password
        )
    }
}

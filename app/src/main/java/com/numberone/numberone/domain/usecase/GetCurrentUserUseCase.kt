
package com.numberone.numberone.domain.usecase
import com.numberone.numberone.domain.model.UserProfile
import com.numberone.numberone.domain.repository.UserRepository

/**
 * Number One - Get Current User Use Case
 * Retrieves the currently authenticated user.
 */
class GetCurrentUserUseCase(
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(): Result<UserProfile?> {
        return userRepository.getCurrentUser()
    }
}

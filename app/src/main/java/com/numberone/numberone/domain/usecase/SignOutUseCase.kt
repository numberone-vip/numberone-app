
package com.numberone.numberone.domain.usecase

import com.numberone.numberone.domain.repository.UserRepository

/**
 * Number One - Sign Out Use Case
 * Signs out the current user.
 */
class SignOutUseCase(
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(): Result<Unit> {
        return userRepository.signOut()
    }
}

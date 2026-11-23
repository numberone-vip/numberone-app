package com.numberone.numberone.domain.usecase
import com.numberone.numberone.domain.repository.UserRepository
/**
 * Number One - Check Admin Use Case
 * Checks whether the current authenticated user is an administrator.
 */
class CheckAdminUseCase(
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(): Result<Boolean> {
        return userRepository.isAdmin()
    }
}

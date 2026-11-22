package com.numberone.numberone.domain.usecase

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.domain.repository.UserRepository

class SignUpUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        if (!Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(email.trim()))
            return Result.failure(IllegalArgumentException(tr("أدخل بريداً إلكترونياً صحيحاً")))
        if (password.length < 8) return Result.failure(IllegalArgumentException(tr("كلمة المرور يجب ألا تقل عن 8 أحرف")))
        return repository.signUp(email.trim(), password)
    }
}

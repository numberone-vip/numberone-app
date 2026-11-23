
package com.numberone.numberone.domain.model

import kotlinx.serialization.Serializable

/**
 * Number One - User Profile Model
 * Represents the signed-in Supabase Auth user.
 */
@Serializable
data class UserProfile(

    val id: String = "",

    val email: String? = null,

    val phone: String? = null,

    val createdAt: String? = null,

    val isAdmin: Boolean = false
)

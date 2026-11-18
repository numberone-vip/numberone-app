package com.numberone.app.data.repository

import com.numberone.app.data.remote.SupabaseClientProvider
import com.numberone.app.data.remote.dto.ServiceDto
import com.numberone.app.data.remote.dto.toDomain
import com.numberone.app.domain.model.VirtualService
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ServicesRepository {

    private val client = SupabaseClientProvider.client

    suspend fun getAvailableServices(): Result<List<VirtualService>> = withContext(Dispatchers.IO) {
        runCatching {
            val dtoList = client.postgrest["services"]
                .select()
                .decodeList<ServiceDto>()

            dtoList.map { it.toDomain() }
        }
    }
}
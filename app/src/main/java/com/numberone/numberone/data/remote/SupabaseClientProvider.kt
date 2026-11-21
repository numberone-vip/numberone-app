package com.numberone.numberone.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/** Shared Supabase client configured for the connected Number One project. */
object SupabaseClientProvider {
    private const val SUPABASE_URL = "https://rlxcsnposxvsyhwtmgbz.supabase.co"
    // A Supabase publishable key is intended for use in client applications. RLS remains the security boundary.
    private const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_0JRFmUNoVPY-SJjde4UmRg_MiFs94lD"

    val client: SupabaseClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
            install(Functions)
        }
    }
}

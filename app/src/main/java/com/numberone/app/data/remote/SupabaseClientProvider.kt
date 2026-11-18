
package com.numberone.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {

    private const val SUPABASE_URL =
        "https://rlxcsnposxvsyhwtmgbz.supabase.co"

    private const val SUPABASE_ANON_KEY =
        "sb_publishable_0JRFmUNoVPY-SJjde4UmRg_MiFs94lD"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }

    fun init() {
        client
    }
}

package com.example.data.remote

import com.example.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Single entry point for the Supabase backend.
 *
 * The client is created lazily so guest usage never pays for initialization
 * and the network is never touched unless the user actually signs in.
 * The anon/publishable key is public-safe: row-level security policies in
 * the database are the real security boundary.
 */
object SupabaseProvider {
    val client by lazy {
        val url = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        require(url.isNotBlank() && anonKey.isNotBlank()) {
            "Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to .env and rebuild."
        }
        createSupabaseClient(url, anonKey) {
            install(Auth)
            install(Postgrest)
        }
    }
}

package de.max.roehrl.vueddit2.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


// https://developer.android.com/topic/libraries/architecture/datastore
class Store(context: Context) {
    val PREFERENCE_NAME = "reddit"
    val USERNAME = preferencesKey<String>("username")
    val AUTH_TOKEN = preferencesKey<String>("authToken")
    val VALID_UNTIL = preferencesKey<Int>("validUntil")
    val REFRESH_TOKEN = preferencesKey<String>("refreshToken")
    private val preferenceDataStore: DataStore<Preferences> = context.createDataStore(name = PREFERENCE_NAME)
    val username : Flow<String> = get(USERNAME)

    @InternalCoroutinesApi
    fun read() {
        CoroutineScope(Dispatchers.IO).launch {
            preferenceDataStore.data.collect { preferences ->
                val username = preferences[USERNAME] ?: ""
                val authToken = preferences[AUTH_TOKEN] ?: ""
                val validUntil = preferences[VALID_UNTIL] ?: 0
                val refreshToken = preferences[REFRESH_TOKEN] ?: ""
            }
        }
    }

    fun <T> get(key : Preferences.Key<T>) : Flow<T> {
        return preferenceDataStore.data.map { value -> value[key]!! }
    }

    suspend fun updateProfile(username: String, authToken: String, validUntil: Int, refreshToken: String) {
        preferenceDataStore.edit { preferences ->
            preferences[USERNAME] = username
            preferences[AUTH_TOKEN] = authToken
            preferences[VALID_UNTIL] = validUntil
            preferences[REFRESH_TOKEN] = refreshToken
        }
    }
}
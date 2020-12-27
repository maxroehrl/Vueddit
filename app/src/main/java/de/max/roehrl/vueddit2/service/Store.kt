package de.max.roehrl.vueddit2.service

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import de.max.roehrl.vueddit2.model.SingletonHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import java.io.IOException


// https://developer.android.com/topic/libraries/architecture/datastore
class Store private constructor(context: Context) {
    private val preferenceDataStore: DataStore<Preferences> = context.createDataStore(PREFERENCE_NAME)
    val username : Flow<String?> = getFlow(USERNAME)
    val authToken : Flow<String?> = getFlow(AUTH_TOKEN)
    val validUntil : Flow<Int?> = getFlow(VALID_UNTIL)
    val refreshToken : Flow<String?> = getFlow(REFRESH_TOKEN)

    companion object : SingletonHolder<Store, Context>(::Store) {
        private const val PREFERENCE_NAME = "reddit"
        private val USERNAME = preferencesKey<String>("username")
        private val AUTH_TOKEN = preferencesKey<String>("authToken")
        private val VALID_UNTIL = preferencesKey<Int>("validUntil")
        private val REFRESH_TOKEN = preferencesKey<String>("refreshToken")
    }

    private fun <T> getFlow(key: Preferences.Key<T>): Flow<T?> {
        return preferenceDataStore.data
            .catch { exception ->
                // dataStore.data throws an IOException when an error is encountered when reading data
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { value ->
                value[key]
            }
    }

    private suspend fun <T> get(key: Preferences.Key<T>) : T? {
        var value : T? = null
        getFlow(key).collect { token ->
            value = token
        }
        Log.e("Store", value.toString())
        return value
    }

    suspend fun getUsername() : String? {
        return get(USERNAME)
    }

    suspend fun getRefreshToken() : String? {
        return get(REFRESH_TOKEN)
    }

    suspend fun getValidUntil() : Int? {
        return get(VALID_UNTIL)
    }

    suspend fun getAuthToken() : String? {
        return get(AUTH_TOKEN)
    }

    suspend fun updateTokens(authToken: String, validUntil: Int, refreshToken: String) {
        preferenceDataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = authToken
            preferences[VALID_UNTIL] = validUntil
            preferences[REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun updateUserName(username: String) {
        preferenceDataStore.edit { preferences ->
            preferences[USERNAME] = username
        }
    }
}
package de.max.roehrl.vueddit2.service

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.createDataStore
import de.max.roehrl.vueddit2.model.SingletonHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException


// https://developer.android.com/topic/libraries/architecture/datastore
class Store private constructor(context: Context) {
    private val preferenceDataStore: DataStore<Preferences> = context.createDataStore(PREFERENCE_NAME)
    private val username : Flow<String?> = getFlow(USERNAME)
    private val authToken : Flow<String?> = getFlow(AUTH_TOKEN)
    private val validUntil : Flow<Long?> = getFlow(VALID_UNTIL)
    private val refreshToken : Flow<String?> = getFlow(REFRESH_TOKEN)

    companion object : SingletonHolder<Store, Context>(::Store) {
        private const val TAG = "Store"
        private const val PREFERENCE_NAME = "reddit"
        private val USERNAME = stringPreferencesKey("username")
        private val AUTH_TOKEN = stringPreferencesKey("authToken")
        private val VALID_UNTIL = longPreferencesKey("validUntil")
        private val REFRESH_TOKEN = stringPreferencesKey("refreshToken")
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

    suspend fun getUsername() : String? {
        return username.first()
    }

    suspend fun getRefreshToken() : String? {
        return refreshToken.first()
    }

    suspend fun getValidUntil() : Long? {
        return validUntil.first()
    }

    suspend fun getAuthToken() : String? {
        return authToken.first()
    }

    suspend fun updateTokens(authToken: String, validUntil: Long, refreshToken: String?) {
        preferenceDataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = authToken
            preferences[VALID_UNTIL] = validUntil
            if (refreshToken != null) {
                if (refreshToken.isEmpty()) {
                    Log.w(TAG, "Resetting refresh token")
                }
                preferences[REFRESH_TOKEN] = refreshToken
            }
        }
    }

    suspend fun updateUserName(username: String) {
        preferenceDataStore.edit { preferences ->
            preferences[USERNAME] = username
        }
    }

    suspend fun logoutUser() {
        updateTokens("", 0, "")
        updateUserName("")
    }
}
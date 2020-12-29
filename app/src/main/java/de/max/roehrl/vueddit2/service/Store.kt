package de.max.roehrl.vueddit2.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesKey
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
    val username : Flow<String?> = getFlow(USERNAME)
    val authToken : Flow<String?> = getFlow(AUTH_TOKEN)
    val validUntil : Flow<Long?> = getFlow(VALID_UNTIL)
    val refreshToken : Flow<String?> = getFlow(REFRESH_TOKEN)

    companion object : SingletonHolder<Store, Context>(::Store) {
        private const val PREFERENCE_NAME = "reddit"
        private val USERNAME = preferencesKey<String>("username")
        private val AUTH_TOKEN = preferencesKey<String>("authToken")
        private val VALID_UNTIL = preferencesKey<Long>("validUntil")
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
                preferences[REFRESH_TOKEN] = refreshToken
            }
        }
    }

    suspend fun updateUserName(username: String) {
        preferenceDataStore.edit { preferences ->
            preferences[USERNAME] = username
        }
    }
}
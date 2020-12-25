package de.max.roehrl.vueddit2.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import de.max.roehrl.vueddit2.model.SingletonHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException


// https://developer.android.com/topic/libraries/architecture/datastore
class Store private constructor(context: Context) {
    private val PREFERENCE_NAME = "reddit"
    private val USERNAME = preferencesKey<String>("username")
    private val AUTH_TOKEN = preferencesKey<String>("authToken")
    private val VALID_UNTIL = preferencesKey<Int>("validUntil")
    private val REFRESH_TOKEN = preferencesKey<String>("refreshToken")
    private val preferenceDataStore: DataStore<Preferences> = context.createDataStore(PREFERENCE_NAME)
    val username : Flow<String?> = get(USERNAME)
    val authToken : Flow<String?> = get(AUTH_TOKEN)
    val validUntil : Flow<Int?> = get(VALID_UNTIL)
    val refreshToken : Flow<String?> = get(REFRESH_TOKEN)

    companion object : SingletonHolder<Store, Context>(::Store)

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

    fun <T> get(key: Preferences.Key<T>): Flow<T?> {
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

    suspend fun updateTokens(authToken: String, validUntil: Int, refreshToken: String) {
        preferenceDataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = authToken
            preferences[VALID_UNTIL] = validUntil
            preferences[REFRESH_TOKEN] = refreshToken
        }
    }
}
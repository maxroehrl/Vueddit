package de.max.roehrl.vueddit2.service

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import de.max.roehrl.vueddit2.model.Subreddit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(Store.PREFERENCE_NAME)

// https://developer.android.com/topic/libraries/architecture/datastore
class Store private constructor(val context: Context) {
    private val username: Flow<String?> = getFlow(USERNAME)
    private val authToken: Flow<String?> = getFlow(AUTH_TOKEN)
    private val validUntil: Flow<Long?> = getFlow(VALID_UNTIL)
    private val refreshToken: Flow<String?> = getFlow(REFRESH_TOKEN)
    private val visitedSubreddits: Flow<Set<String>?> = getFlow(VISITED_SUBS)
    private val starredSubreddits: Flow<Set<String>?> = getFlow(STARRED_SUBS)
    private val cachedSubscribedSubreddits: Flow<Set<String>?> = getFlow(CACHED_SUBSCRIBED_SUBS)
    private val cachedMultiSubreddits: Flow<Set<String>?> = getFlow(CACHED_MULTI_SUBS)

    companion object : SingletonHolder<Store, Context>(::Store) {
        private const val TAG = "Store"
        const val PREFERENCE_NAME = "reddit"
        private val USERNAME = stringPreferencesKey("username")
        private val AUTH_TOKEN = stringPreferencesKey("authToken")
        private val VALID_UNTIL = longPreferencesKey("validUntil")
        private val REFRESH_TOKEN = stringPreferencesKey("refreshToken")
        private val VISITED_SUBS = stringSetPreferencesKey("visitedSubs")
        private val STARRED_SUBS = stringSetPreferencesKey("starredSubs")
        private val CACHED_SUBSCRIBED_SUBS = stringSetPreferencesKey("cachedSubscribedSubs")
        private val CACHED_MULTI_SUBS = stringSetPreferencesKey("cachedMultiSubs")
    }

    private fun <T> getFlow(key: Preferences.Key<T>): Flow<T?> {
        return context.dataStore.data
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

    suspend fun getUsername(): String? {
        return username.first()
    }

    suspend fun getRefreshToken(): String? {
        return refreshToken.first()
    }

    suspend fun getValidUntil(): Long? {
        return validUntil.first()
    }

    suspend fun getAuthToken(): String? {
        return authToken.first()
    }

    suspend fun getVisitedSubreddits(): Set<String> {
        return visitedSubreddits.first() ?: emptySet()
    }

    suspend fun getStarredSubreddits(): Set<String> {
        return starredSubreddits.first() ?: emptySet()
    }

    suspend fun getCachedSubscribedSubreddits(): Set<String> {
        return cachedSubscribedSubreddits.first() ?: emptySet()
    }

    suspend fun getCachedMultiSubreddits(): Set<String> {
        return cachedMultiSubreddits.first() ?: emptySet()
    }

    suspend fun updateTokens(authToken: String, validUntil: Long, refreshToken: String?) {
        context.dataStore.edit { preferences ->
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

    suspend fun addToStarredSubreddits(name: String) {
        context.dataStore.edit { preferences ->
            val starred = preferences[STARRED_SUBS]?.toMutableSet() ?: mutableSetOf()
            starred.add(name)
            preferences[STARRED_SUBS] = starred
        }
    }

    suspend fun removeFromStarredSubreddits(name: String) {
        context.dataStore.edit { preferences ->
            val starred = preferences[STARRED_SUBS]?.toMutableSet() ?: mutableSetOf()
            starred.remove(name)
            preferences[STARRED_SUBS] = starred
        }
    }

    suspend fun addToVisitedSubreddits(name: String) {
        context.dataStore.edit { preferences ->
            val visited = preferences[VISITED_SUBS]?.toMutableSet() ?: mutableSetOf()
            visited.add(name)
            preferences[VISITED_SUBS] = visited.filter { !Subreddit.defaultSubreddits.contains(Subreddit.fromName(it)) }.toSet()
        }
    }

    suspend fun removeFromVisitedSubreddits(name: String) {
        context.dataStore.edit { preferences ->
            val visited = preferences[VISITED_SUBS]?.toMutableSet() ?: mutableSetOf()
            visited.remove(name)
            preferences[VISITED_SUBS] = visited
        }
    }

    suspend fun updateCachedSubscribedSubreddits(subreddits: List<Subreddit>) {
        context.dataStore.edit { preferences ->
            preferences[CACHED_SUBSCRIBED_SUBS] = subreddits.map { it.name }.toSet()
        }
    }

    suspend fun updateCachedMultiSubreddits(subreddits: List<Subreddit>) {
        context.dataStore.edit { preferences ->
            preferences[CACHED_MULTI_SUBS] = subreddits.map { it.name }.toSet()
        }
    }

    suspend fun updateUserName(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME] = username
        }
    }

    suspend fun logoutUser() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
package com.example.learningdashboard.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.learningdashboard.domain.repository.UserPreferencesRepository
import com.example.learningdashboard.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.Preferences.DATASTORE_NAME)

class UserPreferencesRepositoryImpl(
    private val context: Context
) : UserPreferencesRepository {

    private val keyIsLoggedIn = booleanPreferencesKey(Constants.Preferences.KEY_IS_LOGGED_IN)

    override val isLoggedIn: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[keyIsLoggedIn] ?: false
    }.flowOn(Dispatchers.IO)

    override suspend fun setLoggedIn(isLoggedIn: Boolean) {
        withContext(Dispatchers.IO) {
            context.userDataStore.edit { preferences ->
                preferences[keyIsLoggedIn] = isLoggedIn
            }
        }
    }
}

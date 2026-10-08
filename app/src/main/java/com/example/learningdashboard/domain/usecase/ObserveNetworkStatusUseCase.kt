package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.data.util.NetworkManager
import kotlinx.coroutines.flow.Flow

interface ObserveNetworkStatusUseCase {
    val isOnline: Flow<Boolean>
    fun isCurrentlyOnline(): Boolean
}

class ObserveNetworkStatusUseCaseImpl(
    private val networkManager: NetworkManager
) : ObserveNetworkStatusUseCase {
    override val isOnline: Flow<Boolean> = networkManager.isOnline
    override fun isCurrentlyOnline(): Boolean = networkManager.isCurrentlyOnline()
}

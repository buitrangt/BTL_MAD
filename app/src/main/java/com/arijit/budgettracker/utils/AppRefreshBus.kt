package com.arijit.budgettracker.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Simple in-process event bus to notify screens to reload after
 * transactions/categories change.
 */
object AppRefreshBus {
    private val _refreshTick = MutableLiveData(0L)
    val refreshTick: LiveData<Long> = _refreshTick

    fun notifyChanged() {
        _refreshTick.postValue(System.currentTimeMillis())
    }
}


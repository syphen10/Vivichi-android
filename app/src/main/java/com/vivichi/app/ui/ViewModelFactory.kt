package com.vivichi.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vivichi.app.data.PetRepository
import com.vivichi.app.notify.ReminderScheduler

class ViewModelFactory(
    private val repository: PetRepository,
    private val scheduler: ReminderScheduler?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VivichiViewModel::class.java)) {
            return VivichiViewModel(repository, scheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}

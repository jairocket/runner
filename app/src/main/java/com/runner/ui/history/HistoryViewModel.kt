package com.runner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class HistoryViewModel(private val repo: RunRepository) : ViewModel() {

    val runs: List<RunActivity> = repo.getAll()

    companion object {
        fun factory(repo: RunRepository = MockRunRepository()): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HistoryViewModel(repo) as T
            }
    }
}

package com.runner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RunDetailViewModel(
    private val repo: RunRepository,
    private val runId: String
) : ViewModel() {

    val run: RunActivity? = repo.getById(runId)

    companion object {
        fun factory(runId: String, repo: RunRepository = MockRunRepository()): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RunDetailViewModel(repo, runId) as T
            }
    }
}

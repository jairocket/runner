package com.runner.ui.history

interface RunRepository {
    fun getAll(): List<RunActivity>
    fun getById(id: String): RunActivity?
}

package com.runner.ui.history

class MockRunRepository : RunRepository {
    override fun getAll(): List<RunActivity> = mockRuns
    override fun getById(id: String): RunActivity? = mockRuns.find { it.id == id }
}

package com.example.lab08

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    private val dao: TaskDao,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filter = MutableStateFlow(TaskFilter.TODAS)
    val filter: StateFlow<TaskFilter> = _filter

    private val _sort = MutableStateFlow(TaskSort.FECHA)
    val sort: StateFlow<TaskSort> = _sort

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter

    val visibleTasks: StateFlow<List<Task>> = combine(
        _tasks, _searchQuery, _filter, _sort, _categoryFilter
    ) { tasks, query, filter, sort, category ->
        val result = tasks
            .filter { query.isBlank() || it.description.contains(query, ignoreCase = true) }
            .filter {
                when (filter) {
                    TaskFilter.TODAS -> true
                    TaskFilter.PENDIENTES -> !it.isCompleted
                    TaskFilter.COMPLETADAS -> it.isCompleted
                }
            }
            .filter { category == null || it.category == category }
        when (sort) {
            TaskSort.FECHA -> result.sortedByDescending { it.createdAt }
            TaskSort.NOMBRE -> result.sortedBy { it.description.lowercase() }
            TaskSort.ESTADO -> result.sortedBy { it.isCompleted }
            TaskSort.PRIORIDAD -> result.sortedByDescending { it.priority.ordinal }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = _tasks
        .map { list -> list.map { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _tasks.value = dao.getAllTasks()
        }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setFilter(filter: TaskFilter) { _filter.value = filter }
    fun setSort(sort: TaskSort) { _sort.value = sort }
    fun setCategoryFilter(category: String?) { _categoryFilter.value = category }

    fun addTask(
        description: String,
        category: String,
        priority: Priority,
        recurrence: Recurrence,
        reminderMinutes: Long
    ) {
        viewModelScope.launch {
            val id = dao.insertTask(
                Task(
                    description = description,
                    category = category,
                    priority = priority,
                    recurrence = recurrence
                )
            ).toInt()
            if (reminderMinutes > 0) {
                scheduler.schedule(id, description, reminderMinutes)
            }
            _tasks.value = dao.getAllTasks()
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            dao.updateTask(updated)
            if (updated.isCompleted) {
                scheduler.cancel(task.id)
                if (task.recurrence != Recurrence.NINGUNA) {
                    dao.insertTask(
                        task.copy(id = 0, isCompleted = false, createdAt = System.currentTimeMillis())
                    )
                }
            }
            _tasks.value = dao.getAllTasks()
        }
    }

    fun updateTask(task: Task, reminderMinutes: Long) {
        viewModelScope.launch {
            dao.updateTask(task)
            if (reminderMinutes > 0) {
                scheduler.schedule(task.id, task.description, reminderMinutes)
            }
            _tasks.value = dao.getAllTasks()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
            scheduler.cancel(task.id)
            _tasks.value = dao.getAllTasks()
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            _tasks.value.forEach { scheduler.cancel(it.id) }
            dao.deleteAllTasks()
            _tasks.value = emptyList()
        }
    }
}
package com.example.lab08

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority(val label: String) { BAJA("Baja"), MEDIA("Media"), ALTA("Alta") }
enum class Recurrence(val label: String) { NINGUNA("No repite"), DIARIA("Diaria"), SEMANAL("Semanal") }
enum class TaskFilter(val label: String) { TODAS("Todas"), PENDIENTES("Pendientes"), COMPLETADAS("Completadas") }
enum class TaskSort(val label: String) { FECHA("Fecha"), NOMBRE("Nombre"), ESTADO("Estado"), PRIORIDAD("Prioridad") }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "priority") val priority: Priority = Priority.MEDIA,
    @ColumnInfo(name = "category") val category: String = "General",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "recurrence") val recurrence: Recurrence = Recurrence.NINGUNA
)
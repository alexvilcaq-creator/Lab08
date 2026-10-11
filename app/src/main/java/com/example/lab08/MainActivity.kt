package com.example.lab08

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.lab08.ui.theme.Lab08Theme
import androidx.compose.foundation.verticalScroll

// Opciones de recordatorio: texto y minutos de espera
private val reminderOptions = listOf(
    "Sin recordatorio" to 0L,
    "En 1 minuto" to 1L,
    "En 1 hora" to 60L,
    "En 1 día" to 1440L
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab08Theme {
                // Pedir permiso de notificaciones (Android 13 o superior)
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val db = remember {
                    Room.databaseBuilder(
                        applicationContext,
                        TaskDatabase::class.java,
                        "task_db"
                    )
                        .fallbackToDestructiveMigration(true)
                        .build()
                }
                val viewModel = remember {
                    TaskViewModel(db.taskDao(), ReminderScheduler(applicationContext))
                }

                TaskScreen(viewModel)
            }
        }
    }
}

@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.visibleTasks.collectAsState()
    val allTasks by viewModel.tasks.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar tarea")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mis tareas", style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = { viewModel.deleteAllTasks() }) {
                    Text("Borrar todas")
                }
            }

            Text(
                text = "${allTasks.count { !it.isCompleted }} pendientes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar tareas") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { viewModel.setFilter(option) },
                        label = { Text(option.label) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = categoryFilter == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text("Todas las categorías") }
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = categoryFilter == category,
                        onClick = { viewModel.setCategoryFilter(category) },
                        label = { Text(category) }
                    )
                }
            }

            Box {
                TextButton(onClick = { sortMenuOpen = true }) {
                    Text("Ordenar por: ${sort.label}")
                }
                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = { sortMenuOpen = false }
                ) {
                    TaskSort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                viewModel.setSort(option)
                                sortMenuOpen = false
                            }
                        )
                    }
                }
            }

            if (tasks.isEmpty()) {
                Text(
                    text = "No hay tareas. Toca + para agregar una.",
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(tasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onToggle = { viewModel.toggleTaskCompletion(task) },
                        onEdit = { taskToEdit = task },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        TaskDialog(
            title = "Nueva tarea",
            task = null,
            confirmLabel = "Agregar",
            onConfirm = { description, category, priority, recurrence, reminder ->
                viewModel.addTask(description, category, priority, recurrence, reminder)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    taskToEdit?.let { task ->
        TaskDialog(
            title = "Editar tarea",
            task = task,
            confirmLabel = "Guardar",
            onConfirm = { description, category, priority, recurrence, reminder ->
                viewModel.updateTask(
                    task.copy(
                        description = description,
                        category = category,
                        priority = priority,
                        recurrence = recurrence
                    ),
                    reminder
                )
                taskToEdit = null
            },
            onDismiss = { taskToEdit = null }
        )
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (task.priority) {
        Priority.ALTA -> MaterialTheme.colorScheme.error
        Priority.MEDIA -> MaterialTheme.colorScheme.tertiary
        Priority.BAJA -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val details = buildString {
        append("${task.category} • Prioridad ${task.priority.label}")
        if (task.recurrence != Recurrence.NINGUNA) append(" • ${task.recurrence.label}")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.description,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = priorityColor
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Editar tarea")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar tarea")
        }
    }
}

@Composable
fun TaskDialog(
    title: String,
    task: Task?,
    confirmLabel: String,
    onConfirm: (String, String, Priority, Recurrence, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var description by remember { mutableStateOf(task?.description ?: "") }
    var category by remember { mutableStateOf(task?.category ?: "General") }
    var priority by remember { mutableStateOf(task?.priority ?: Priority.MEDIA) }
    var recurrence by remember { mutableStateOf(task?.recurrence ?: Recurrence.NINGUNA) }
    var reminder by remember { mutableStateOf(0L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoría") },
                    singleLine = true
                )
                Text("Prioridad", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.entries.forEach { option ->
                        FilterChip(
                            selected = priority == option,
                            onClick = { priority = option },
                            label = { Text(option.label) }
                        )
                    }
                }
                Text("Repetición", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Recurrence.entries.forEach { option ->
                        FilterChip(
                            selected = recurrence == option,
                            onClick = { recurrence = option },
                            label = { Text(option.label) }
                        )
                    }
                }
                Text("Recordatorio", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reminderOptions.forEach { (label, minutes) ->
                        FilterChip(
                            selected = reminder == minutes,
                            onClick = { reminder = minutes },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (description.isNotBlank()) {
                    onConfirm(
                        description.trim(),
                        category.trim().ifBlank { "General" },
                        priority,
                        recurrence,
                        reminder
                    )
                }
            }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
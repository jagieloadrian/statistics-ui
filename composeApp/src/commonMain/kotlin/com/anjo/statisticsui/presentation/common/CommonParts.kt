package com.anjo.statisticsui.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.navigation.NavHostController
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Arrow_back
import com.composables.icons.materialsymbols.rounded.Delete
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Like [runCatching], but lets coroutine cancellation through. */
suspend fun <T> catching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

/** Launches [block]; clears [onError] first and reports any failure message to it. */
fun CoroutineScope.attempt(onError: (String?) -> Unit, block: suspend CoroutineScope.() -> Unit) = launch {
    onError(null)
    catching { block() }.onFailure { onError(it.message ?: it.toString()) }
}

@Composable
fun ScreenHeader(title: String, navController: NavHostController, actions: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(MaterialSymbols.Rounded.Arrow_back, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

@Composable
fun ErrorText(error: String?) {
    if (error != null) {
        Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = SMALL_PADDING))
    }
}

@Composable
fun Field(label: String, value: String, modifier: Modifier = Modifier, enabled: Boolean = true, singleLine: Boolean = true,
          password: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = singleLine,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier
    )
}

/** Destructive action: first click arms the button and shows [confirmText], the second runs [onConfirm]. */
@Composable
fun ConfirmButton(text: String, confirmText: String, onConfirm: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { if (armed) onConfirm(); armed = !armed }) { Text(if (armed) confirmText else text) }
}

/** Button showing [value]; opens a menu of [options]. */
@Composable
fun Picker(value: String, options: List<String>, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }) { Text(value.ifBlank { "Choose…" }) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { open = false; onSelect(option) })
            }
        }
    }
}

/** Editable rows of [items] with remove buttons and an "Add" button; [row] edits one item. */
@Composable
fun <T> ListEditor(title: String, items: List<T>, onChange: (List<T>) -> Unit, new: () -> T,
                   row: @Composable RowScope.(T, (T) -> Unit) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = SMALL_MEDIUM_PADDING))
    items.forEachIndexed { i, item ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            row(item) { changed -> onChange(items.mapIndexed { j, old -> if (j == i) changed else old }) }
            IconButton(onClick = { onChange(items.filterIndexed { j, _ -> j != i }) }) {
                Icon(MaterialSymbols.Rounded.Delete, contentDescription = "Remove", tint = Color.Gray)
            }
        }
    }
    TextButton(onClick = { onChange(items + new()) }) {
        Icon(MaterialSymbols.Rounded.Add, contentDescription = null)
        Text("Add")
    }
}

/** Text field for an optional number; keeps half-typed text such as `1.` or `-` until it parses. */
@Composable
fun NumberField(label: String, value: Double?, modifier: Modifier = Modifier, onChange: (Double?) -> Unit) {
    fun Double.plain() = if (this % 1.0 == 0.0) toLong().toString() else toString()
    var text by remember { mutableStateOf(value?.plain().orEmpty()) }
    if (text.toDoubleOrNull() != value) text = value?.plain().orEmpty() // changed from outside
    Field(label, text, modifier) { text = it; onChange(it.toDoubleOrNull()) }
}

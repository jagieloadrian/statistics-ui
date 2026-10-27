package com.anjo.statisticsui.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Arrow_back
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
            Icon(MaterialSymbols.Rounded.Arrow_back, contentDescription = "Back", tint = Color.White)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
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
          onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = singleLine,
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

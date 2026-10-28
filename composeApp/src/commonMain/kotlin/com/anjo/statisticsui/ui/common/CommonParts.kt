package com.anjo.statisticsui.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Arrow_back
import com.composables.icons.materialsymbols.rounded.Delete
import com.composables.icons.materialsymbols.rounded.Error

val LocalPrefs = compositionLocalOf { Prefs() }

private val PICKER_WIDTH = 180.dp

// M3 window size classes: compact is a phone in portrait, expanded starts at landscape tablets
val COMPACT_MAX_WIDTH = 600.dp
val EXPANDED_MIN_WIDTH = 840.dp

@Composable
fun windowWidth(): Dp =
    with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width
            .toDp()
    }

@Composable
fun isCompact() = windowWidth() < COMPACT_MAX_WIDTH

@Composable
fun screenPadding() = if (isCompact()) SMALL_MEDIUM_PADDING else LARGE_MEDIUM_PADDING

@Composable
fun ScreenHeader(
    title: String,
    navController: NavHostController,
    actions: @Composable () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(MaterialSymbols.Rounded.Arrow_back, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        ScreenTitle(title, Modifier.weight(1f))
        actions()
    }
}

@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = SMALL_MEDIUM_PADDING),
    )
}

// Colour alone does not carry the error: icon plus text
@Composable
fun ErrorText(error: String?) {
    if (error != null) {
        Row(Modifier.padding(vertical = SMALL_PADDING), horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Icon(MaterialSymbols.Rounded.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    password: Boolean = false,
    keyboardType: KeyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
    help: String? = null,
    error: String? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = singleLine,
        isError = error != null,
        supportingText = (error ?: help)?.let { { Text(it) } },
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
    )
}

@Composable
fun CheckboxField(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    // The whole row toggles, so the label is part of the touch target
    Row(
        Modifier.toggleable(checked, role = Role.Checkbox, onValueChange = onChange).padding(end = SMALL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked, onCheckedChange = null, modifier = Modifier.padding(SMALL_PADDING))
        Text(label, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ConfirmButton(
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
) {
    var armed by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = {
            if (armed) onConfirm()
            armed = !armed
        },
        colors =
            if (armed) {
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                )
            } else {
                ButtonDefaults.outlinedButtonColors()
            },
    ) { Text(if (armed) confirmText else text) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Picker(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier.width(PICKER_WIDTH)) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    open = false
                    onSelect(option)
                })
            }
        }
    }
}

@Composable
fun <T> ListEditor(
    title: String,
    addText: String,
    items: List<T>,
    onChange: (List<T>) -> Unit,
    new: () -> T,
    row: @Composable RowScope.(T, (T) -> Unit) -> Unit,
) {
    SectionTitle(title)
    items.forEachIndexed { i, item ->
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING),
        ) {
            row(item) { changed -> onChange(items.mapIndexed { j, old -> if (j == i) changed else old }) }
            IconButton(onClick = { onChange(items.filterIndexed { j, _ -> j != i }) }) {
                Icon(MaterialSymbols.Rounded.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    TextButton(onClick = { onChange(items + new()) }) {
        Icon(MaterialSymbols.Rounded.Add, contentDescription = null)
        Text(addText)
    }
}

@Composable
fun NumberField(
    label: String,
    value: Double?,
    modifier: Modifier = Modifier,
    help: String? = null,
    error: String? = null,
    onChange: (Double?) -> Unit,
) {
    fun Double.plain() = if (this % 1.0 == 0.0) toLong().toString() else toString()
    var text by remember { mutableStateOf(value?.plain().orEmpty()) }
    val changedOutside = text.toDoubleOrNull() != value
    if (changedOutside) text = value?.plain().orEmpty()
    Field(label, text, modifier, keyboardType = KeyboardType.Decimal, help = help, error = error) {
        text = it
        onChange(it.toDoubleOrNull())
    }
}

package com.anjo.statisticsui.validation

import com.anjo.statisticsui.model.CHANNEL_TYPES
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.Dataset
import com.anjo.statisticsui.model.PUBSUB_CHANNEL
import com.anjo.statisticsui.model.Rule
import com.anjo.statisticsui.model.Source
import com.anjo.statisticsui.model.platformJson
import kotlinx.serialization.SerializationException

typealias FieldErrors = Map<String, String>

const val MIN_PASSWORD_LENGTH = 10
const val MIN_REFRESH_SECONDS = 5
val ROLES = listOf("viewer", "editor", "admin")

private const val REQUIRED = "Required"
private const val RESERVED_NAME = "ts"
private val ID = Regex("^[A-Za-z_][A-Za-z0-9_-]{0,62}$")
private val NAME = Regex("^[A-Za-z_][A-Za-z0-9_]{0,62}$")
private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+$")

fun emailError(email: String): String? =
    when {
        email.isBlank() -> REQUIRED
        !EMAIL.matches(email.trim()) -> "Not an email address"
        else -> null
    }

fun newPasswordError(password: String): String? =
    if (password.length < MIN_PASSWORD_LENGTH) "At least $MIN_PASSWORD_LENGTH characters" else null

fun idError(
    id: String,
    existingIds: Collection<String> = emptyList(),
): String? =
    when {
        id.isBlank() -> REQUIRED
        !ID.matches(id) -> "Letters, digits, - and _, starting with a letter or _, at most 63 characters"
        id in existingIds -> "Already exists"
        else -> null
    }

fun jsonError(text: String): String? {
    if (text.isBlank()) return REQUIRED
    return try {
        platformJson.parseToJsonElement(text)
        null
    } catch (e: SerializationException) {
        "Not valid JSON: ${e.message}"
    }
}

fun positiveWholeNumber(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it > 0 }

fun loginErrors(
    email: String,
    password: String,
): FieldErrors =
    errorsOf(
        "email" to emailError(email),
        "password" to REQUIRED.takeIf { password.isEmpty() },
    )

fun passwordChangeErrors(
    current: String,
    new: String,
    repeated: String,
): FieldErrors =
    errorsOf(
        "current" to REQUIRED.takeIf { current.isEmpty() },
        "new" to newPasswordError(new),
        "repeated" to "Passwords differ".takeIf { repeated != new },
    )

fun newUserErrors(
    email: String,
    role: String,
    password: String,
): FieldErrors =
    errorsOf(
        "email" to emailError(email),
        "role" to "One of ${ROLES.joinToString()}".takeIf { role !in ROLES },
        "password" to newPasswordError(password),
    )

fun newDashboardErrors(
    id: String,
    existingIds: Collection<String>,
): FieldErrors = errorsOf("id" to idError(id.trim(), existingIds))

fun datasetErrors(
    dataset: Dataset,
    calculatedNames: List<String>,
    retention: String,
    existingIds: Collection<String>,
): FieldErrors {
    val names = dataset.measures.map { it.name } + dataset.dimensions.map { it.name } + calculatedNames
    return errorsOf(
        "id" to idError(dataset.id, existingIds),
        "names" to namesError(names),
        "retention" to "A whole number above 0, or empty".takeIf { retention.isNotBlank() && positiveWholeNumber(retention) == null },
    )
}

fun sourceErrors(
    source: Source,
    rate: String,
    mappingJson: String,
    existingIds: Collection<String>,
): FieldErrors =
    errorsOf(
        "id" to idError(source.id, existingIds),
        "dataset" to REQUIRED.takeIf { source.dataset.isBlank() },
        "channel" to "One of ${CHANNEL_TYPES.joinToString()}".takeIf { source.channel.type !in CHANNEL_TYPES },
        "rate" to "A whole number above 0".takeIf { positiveWholeNumber(rate) == null },
        "mapping" to mappingError(mappingJson, source.channel.type),
    )

fun dashboardErrors(dashboard: Dashboard): FieldErrors =
    errorsOf(
        "refresh" to
            "Empty or at least $MIN_REFRESH_SECONDS".takeIf {
                dashboard.refreshSeconds.let { it != null && it < MIN_REFRESH_SECONDS }
            },
        "variables" to "At most one timeRange variable".takeIf { dashboard.variables.count { it.type == "timeRange" } > 1 },
        "widgets" to "Widget ids must be unique".takeIf { dashboard.widgets.distinctBy { it.id }.size != dashboard.widgets.size },
    )

private fun namesError(names: List<String>): String? {
    val invalid = names.firstOrNull { !NAME.matches(it) }
    val duplicate =
        names
            .groupBy { it }
            .entries
            .firstOrNull { it.value.size > 1 }
            ?.key
    return when {
        invalid != null -> "'$invalid': letters, digits and _, starting with a letter or _, at most 63 characters"
        duplicate != null -> "'$duplicate' is declared more than once"
        RESERVED_NAME in names -> "'$RESERVED_NAME' is reserved"
        else -> null
    }
}

private fun mappingError(
    mappingJson: String,
    channel: String,
): String? {
    val rules =
        try {
            platformJson.decodeFromString<List<Rule>>(mappingJson)
        } catch (e: IllegalArgumentException) {
            return "Not a valid mapping: ${e.message}"
        }
    val identityMissing = rules.isEmpty() || rules.any { it.identity.isEmpty() }
    return "A pubsub source needs at least one rule, each with identity".takeIf { channel == PUBSUB_CHANNEL && identityMissing }
}

private fun errorsOf(vararg fields: Pair<String, String?>): FieldErrors =
    fields.mapNotNull { (field, error) -> error?.let { field to it } }.toMap()

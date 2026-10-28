package com.anjo.statisticsui.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.auth.Session
import com.anjo.statisticsui.model.DefinitionSummary
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.ui.common.attempt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class HomeState(
    private val api: PlatformApi,
    private val session: Session,
    private val scope: CoroutineScope,
) {
    var dashboards by mutableStateOf(emptyList<DefinitionSummary>())
        private set
    var creating by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
        private set

    val favourites: List<String> get() = prefs.favourites
    val email get() = session.user?.email
    val canCreate get() = session.canEdit
    val isAdmin get() = session.isAdmin
    val sortedDashboards get() = dashboards.sortedByDescending { it.id in favourites }

    private val prefs get() = session.user?.prefs ?: Prefs()

    fun load() = scope.attempt({ error = it }) { dashboards = api.list("dashboards") }

    fun toggleFavourite(id: String) {
        val changed = if (id in favourites) favourites - id else favourites + id
        scope.attempt({ error = it }) { api.putPrefs(prefs.copy(favourites = changed)) }
    }

    fun signOut() = scope.launch { api.logout() }
}

@Composable
fun rememberHomeState(
    api: PlatformApi = koinInject(),
    session: Session = koinInject(),
): HomeState {
    val scope = rememberCoroutineScope()
    return remember { HomeState(api, session, scope) }
}

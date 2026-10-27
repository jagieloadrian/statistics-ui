# Statistics-UI

Compose Multiplatform frontend (desktop JVM, web) of the universal statistics platform. Backend and the platform
spec live in the `StatisticsService` repo (spec 004).

## Backend URL

The app calls `http://127.0.0.1:8080/api/v2`, set in `composeApp/build.gradle.kts`:

```kotlin
buildkonfig { defaultConfigs { buildConfigField(STRING, "backendurl", "http://127.0.0.1:8080") } }
```

Start the backend first (see its README): PostgreSQL, then
`ADMIN_EMAIL=admin@local ADMIN_PASSWORD='change-me-now' ./gradlew run` in `StatisticsService`.

## Run

Desktop (JVM):

```shell
./gradlew :composeApp:run
```

Tests: `./gradlew :composeApp:jvmTest`.

Web: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` (or `jsBrowserDevelopmentRun`) serves the UI on
port 8081, the origin the backend allows by default (`CORS_HOSTS=localhost:8081`). Both web targets currently fail
to compile with a Kotlin compiler crash ([KT-82395](https://youtrack.jetbrains.com/issue/KT-82395),
`No file found for source null`), so use the desktop app until it is fixed.

## Sign in

Sign in with the bootstrap admin (`ADMIN_EMAIL` / `ADMIN_PASSWORD`, created on the backend's first start while there
are no users). The token is kept between runs (desktop: Java preferences, web: `localStorage`). Sign out from the drawer.

The login screen shows what the backend enables (`GET /auth/config`): the password form, a **Sign in with …**
button for its OpenID Connect provider, or both. Provider sign-in:

- web: the page goes to the provider and comes back to the app's origin, which finishes the sign-in;
- desktop: the system browser opens the provider; the app waits on `http://127.0.0.1:<random port>/callback`
  (**Cancel** stops waiting). Close the browser tab when it says so.

Redirect URIs to register at the provider: the web UI's origin + `/` (dev: `http://localhost:8081/`) and
`http://127.0.0.1/callback` (desktop; the provider must accept any port for loopback, RFC 8252).

Admins add other accounts under **Users** with an initial password:

| Role | Can |
|------|-----|
| viewer | open dashboards shared with them or with everyone |
| editor | also create dashboards and edit their own, share them |
| admin | everything: sources, datasets, users, export/import |

**Account** sets time zone, units (metric/imperial), theme, home dashboard (opened after sign-in) and the password
(not shown for provider-only accounts or when password sign-in is off).

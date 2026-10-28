# Statistics-UI

Compose Multiplatform frontend (desktop JVM, web) of the universal statistics platform. Backend and the platform
spec live in the `StatisticsService` repo (spec 004).

## Backend URL

The app calls `http://127.0.0.1:8080/api/v2`. Another backend is a build-time setting, e.g.
`./gradlew :composeApp:wasmJsBrowserDistribution -PbackendUrl=https://stats.example.com`; an empty value
(`-PbackendUrl=`) makes the web build call the origin it was loaded from (see Deployment).

Start the backend first (see its README): PostgreSQL, then
`ADMIN_EMAIL=admin@local ADMIN_PASSWORD='change-me-now' ./gradlew run` in `StatisticsService`.

## Run

Desktop (JVM):

```shell
./gradlew :composeApp:run
```

Tests: `./gradlew :composeApp:jvmTest` (the common tests run on the JVM). Lint: `./gradlew ktlintCheck`
(ktlint, official style; `./gradlew ktlintFormat` fixes most findings). `./gradlew check` runs both.

Web: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` (or `jsBrowserDevelopmentRun`) serves the UI on
port 8081, the origin the backend allows by default (`CORS_HOSTS=localhost:8081`).

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

## Deployment (homelab)

The web UI ships as `ghcr.io/jagieloadrian/statistics-ui` (`Dockerfile`, nginx-unprivileged with
`.devops/nginx/nginx.conf`) and follows the homelab platform contract. It is stateless, needs no configuration
and no secrets: the image holds only the static files, built with `-PbackendUrl=` so the browser calls the API
on the origin it loaded the UI from.

- **Who calls it:** only the ingress gateway (`home-gateway` in `istio-ingress`, principal
  `cluster.local/ns/istio-ingress/sa/home-gateway-istio`) through the HTTPRoute for `statistics.home.arpa`.
- **Whom it calls:** nobody inside the cluster. The browser calls the backend at `http://statistics.home.arpa/api/`,
  routed by the `statistics-service` HTTPRoute (PathPrefix `/api/`, same host) from the `StatisticsService` repo.
- **Image:** UID/GID 10001, read-only root filesystem (writes only to `/tmp`), plain HTTP on `0.0.0.0:8080`,
  `GET /health/live` and `GET /health/ready`, JSON access log on stdout (`timestamp`, `level`, `message`,
  `logger`, `traceId` from `traceparent`), errors on stderr, graceful stop on SIGQUIT, `.wasm` served as
  `application/wasm`. linux/amd64 and linux/arm64; the Dockerfile has no `RUN`, so buildx needs no emulation.
- **Manifests:** `.devops/k8s` (ServiceAccount, Deployment, Service, HTTPRoute, AuthorizationPolicy, namespace
  `apps`) is read directly by Flux from `main` of this repo (registered in homelab-infra
  `clusters/home/apps/statistics-ui.yaml`).
- **CI:** `.github/workflows/build.yml` pushes `main-<yyyyMMddHHmmss>-<sha7>` on main and `<semver>` on `v*` tags,
  never `latest`; on main it then commits the new tag to `.devops/k8s`, which Flux rolls out.

Local image build:

```shell
./gradlew :composeApp:wasmJsBrowserDistribution -PbackendUrl=
docker buildx build --platform linux/amd64,linux/arm64 -t ghcr.io/jagieloadrian/statistics-ui:<tag> --push .
kubectl kustomize .devops/k8s
```

Register `http://statistics.home.arpa/` as a redirect URI of the OIDC client at the provider.

### Definition of Ready

Verified locally (multi-arch push to a local registry, container run with `--read-only --tmpfs /tmp
--user 10001:10001 --cap-drop ALL`, kustomize + kubeconform with the CRD catalog):

- [x] Manifest list has `linux/amd64` and `linux/arm64` (`docker buildx imagetools inspect`)
- [x] Immutable tag `main-<yyyyMMddHHmmss>-<sha7>` or semver, never `latest` (build.yml)
- [x] Starts as UID 10001 with a read-only root filesystem
- [x] `/health/live` and `/health/ready` answer 200 within a second
- [x] `docker stop` (SIGQUIT) exits 0 in under a second
- [x] Memory request = limit 32Mi, measured 7.6 MiB with 8 workers (24%); CPU request only
- [x] No configuration or secrets needed, none in the image
- [x] Service port `http` with `appProtocol: http`, no TLS in the container
- [x] ServiceAccount + AuthorizationPolicy admitting only the ingress gateway
- [x] JSON logs on stdout
- [x] Stateless, so no PVC, node selector or backup
- [ ] `docker manifest inspect ghcr.io/jagieloadrian/statistics-ui:<tag>` after the first push from CI
- [ ] Start time and memory measured on the RPi5 and the Wyse

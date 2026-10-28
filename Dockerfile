# Web UI, same-origin build: ./gradlew :composeApp:wasmJsBrowserDistribution -PbackendUrl=
# nginx's mime.types already serves .wasm as application/wasm; listens on 8080 as uid 101.
FROM nginxinc/nginx-unprivileged:1.30.6-alpine
COPY composeApp/build/dist/wasmJs/productionExecutable/ /usr/share/nginx/html/

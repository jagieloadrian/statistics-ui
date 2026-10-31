FROM nginxinc/nginx-unprivileged:1.30.6-alpine
ENV NGINX_ENTRYPOINT_QUIET_LOGS=1
COPY .devops/nginx/nginx.conf /etc/nginx/nginx.conf
COPY composeApp/build/dist/wasmJs/productionExecutable/ /usr/share/nginx/html/
USER 10001:10001
EXPOSE 8080
STOPSIGNAL SIGQUIT

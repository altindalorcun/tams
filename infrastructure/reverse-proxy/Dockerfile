FROM nginx:1.27-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY nginx.conf /etc/nginx/conf.d/default.conf

RUN chown -R appuser:appgroup /var/cache/nginx \
    && chown -R appuser:appgroup /var/log/nginx \
    && chown -R appuser:appgroup /etc/nginx/conf.d \
    && touch /var/run/nginx.pid \
    && chown appuser:appgroup /var/run/nginx.pid \
    && mkdir -p /etc/nginx/certs \
    && chown appuser:appgroup /etc/nginx/certs

USER appuser

EXPOSE 8080 8443

CMD ["nginx", "-g", "daemon off;"]

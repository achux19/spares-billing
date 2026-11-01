# ---- build stage ----
FROM node:22-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- run stage ----
FROM nginx:alpine
ENV TZ=Asia/Kolkata
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1

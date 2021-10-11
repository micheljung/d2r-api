FROM timbru31/java-node:11-jdk-14 AS build
ARG NPM_REGISTRY="http://registry.npmjs.org/"
ENV NODE_ENV=production
RUN mkdir /opt/app
WORKDIR /opt/app
COPY . .
RUN chmod +x gradlew && ./gradlew build
WORKDIR /opt/app/api
RUN npm ci \
   && npm cache clean --force \
   && npx rollup -c

FROM node:lts-alpine3.14
ENV PATH /opt/app/api/node_modules/.bin:$PATH
WORKDIR /opt/app/api
COPY --from=build /opt/app/api/bundle.js ./server.js
COPY --from=build /opt/app/api/node_modules/ ./node_modules
CMD ["node", "server.js"]

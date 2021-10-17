FROM node:lts-alpine3.14 AS build
ARG NPM_REGISTRY="http://registry.npmjs.org/"
ENV NODE_ENV=production
RUN mkdir /opt/app
WORKDIR /opt/app
COPY . .
WORKDIR /opt/app/api
RUN npm ci && npx rollup -c

FROM node:lts-alpine3.14
ENV PATH /opt/app/api/node_modules/.bin:$PATH
WORKDIR /opt/app/api
COPY --from=build /opt/app/api/bundle.mjs ./server.mjs
COPY --from=build /opt/app/api/node_modules/ ./node_modules
COPY ./api/d2r/ ./d2r
CMD ["node", "server.mjs"]

FROM node:lts-alpine3.11 AS build
ARG NPM_REGISTRY="http://registry.npmjs.org/"
RUN npm i npm@latest -g
ENV NODE_ENV=production
RUN mkdir /opt/app && chown node:node /opt/app
WORKDIR /opt/app
USER node
COPY ./src ./src
COPY ./index.mjs .
COPY ./package.json .
RUN npm i --silent \
   && npm cache clean --force \
   && npx rollup --format=cjs --file=bundle.js -- index.mjs

FROM node:lts-alpine3.14
ENV PATH /opt/app/node_modules/.bin:$PATH
COPY --from=build /opt/app/bundle.js ./server.js
COPY --from=build /opt/app/node_modules/ /opt/app/node_modules
CMD ["node", "server.js"]

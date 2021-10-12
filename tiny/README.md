# Simplest GraphQl Server

Sometimes these things don't really have to be super complicated. 
Has anyone seen the amount of modules that Apollo brings with it? 
Yeah... Perhaps great for a gateway but, 
overkill for something dead simple.

This repo explores making the tiniest GraphQl server possible. 
Some use cases (maybe):

- Quick Lambda functions
- Intermediary service/api layer between two rest APIs
- Building something _FAST_

## Building the App
```js
npx rollup --format=cjs --file=bundle.js -- index.mjs
```

## Building the Docker Image

```shell
docker build -t someName .
```

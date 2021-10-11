import express from 'express';
import {graphqlHTTP} from 'express-graphql';
import {d2rSchema} from './d2r-schema.mjs';

const PORT = 4000;
const app = express();

app.use(
  '/graphql',
  graphqlHTTP(async (request, response, graphQLParams) => {
    return {
      schema: d2rSchema,
      graphiql: true,
      context: {
        req: request,
      },
    };
  })
);

app.listen(PORT, () => {
  console.log(`🚀 at http://localhost:${PORT}/graphql`);
});

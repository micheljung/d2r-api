import polka from "polka";
import bp from "body-parser";
const { json } = bp;
import send from "@polka/send-type";
import * as gql from "graphql";
import * as taskDefs from "./src/schema";
import resolvers from "./src/resolvers";
const { graphql, buildSchema } = gql;
const { port = 3000 } = process.env;

const serverSchema = buildSchema(taskDefs.schema);

polka()
  .use(json())
  .post("/", async (req, res) => {
    let { query } = req.body;
    const data = await graphql(serverSchema, query, resolvers);
    send(res, 200, data);
  })
  .listen(port, err => {
    if (err) throw err;
    console.log(`🚀 on port: ${port}`);
  });

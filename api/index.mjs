import polka from "polka";
import bp from "body-parser";
import send from "@polka/send-type";
import * as gql from "graphql";
import * as taskDefs from "./build/js/schema.mjs";
import {root} from "./build/js/resolvers/index.mjs";

const {json} = bp;

const {graphql, buildSchema} = gql;
const {port = 3000} = process.env;

const serverSchema = buildSchema(taskDefs.schema);

polka()
  .use(json())
  .post("/", async (req, res) => {
    let {query} = req.body;
    const data = await graphql(serverSchema, query, root);
    send(res, 200, data);
  })
  .listen(port, err => {
    if (err) throw err;
    console.log(`🚀 on port: ${port}`);
  });

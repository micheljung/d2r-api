import csv from 'csv-parser';
import fs from 'fs';
import {GraphQLJSONObject, schemaComposer} from "graphql-compose";
import {GraphQLFloat, GraphQLInt, GraphQLNonNull, GraphQLString} from "graphql";
import flexsearch from 'flexsearch';

const {Document} = flexsearch;

const excelDir = 'd2r/data/data/global/excel/';
const filenames = fs.readdirSync(excelDir);

const INVALID_CHARACTERS = /[^\w\d]/g
const CAPITALS_AT_START = /^([A-Z]+)/g
const documents = {}

schemaComposer.createInputTC({
  name: 'SearchOptionsInput',
  fields: {
    fields: [GraphQLString]
  }
})

schemaComposer.createInputTC({
  name: 'QueryInput',
  fields: {
    query: GraphQLNonNull(GraphQLString),
    options: 'SearchOptionsInput',
    bool: {
      type: GraphQLString,
      default: 'and'
    }
  }
})

function detectColumnProperties(value, current) {
  let isNumber = value.len > 1 && value[0] !== '0' || value.length === 0 ? false : !isNaN(value);

  if (value.trim() === "") {
    current.hasNull = true
    return current
  }

  if (isNumber) {
    if (value.includes('.')) {
      current.hasFloat = true
    } else {
      current.hasInt = true
    }
    return current
  }

  if (value.length > 0) {
    current.hasString = true
  }
  return current
}

function columnToPropertyName(columnName) {
  let newName = columnName.replaceAll(INVALID_CHARACTERS, "")
    .replaceAll(CAPITALS_AT_START, letter => letter[0].toLowerCase() + letter.slice(1, letter.length))

  if (!isNaN(parseInt(columnName[0]))) {
    newName = "_" + newName
  }
  return newName
}

function replaceEmptyStringsWithNull(data) {
  Object.keys(data).forEach(key => {
    if (data[key] === "") {
      data[key] = null;
    }
  });
}

function replacePropertyNames(data) {
  Object.keys(data).forEach(key => {
    let propertyName = columnToPropertyName(key);
    if (propertyName !== key) {
      data[propertyName] = data[key]
      delete data[key]
    }
  });
}

async function parseFile(file, schemaComposer) {
  return new Promise(((resolve) => {
    const fieldName = file.slice(0, file.lastIndexOf('.'))
    const typeName = file.charAt(0).toUpperCase() + file.slice(1, file.lastIndexOf('.'))
    const columnProperties = [];
    let index = 0

    fs.createReadStream(excelDir + "/" + file)
      .pipe(csv({separator: "\t"}))
      .on('headers', (headers) => {
        headers.forEach((header, index) => columnProperties[index] = {
          originalName: header,
          name: columnToPropertyName(header),
          hasString: false,
          hasInt: false,
          hasFloat: false,
          hasNull: false
        })
        documents[fieldName] = new Document({
          cache: 1000,
          store: true,
          index: columnProperties.map(value => value.name)
        })
      })
      .on('data', (data) => {
        Object.values(data).forEach(
          (value, index) => columnProperties[index] = detectColumnProperties(value, columnProperties[index])
        )
        replaceEmptyStringsWithNull(data);
        replacePropertyNames(data);
        documents[fieldName].add(index++, data)
      })
      .on('end', () => {
        appendToSchema(fieldName, typeName, columnProperties, schemaComposer)
        resolve()
      })
  }))
}

// Booleans are too difficult to detect so we don't do it even if we could. Otherwise, we'd end up detecting some but
// not the other. It's easier for API users to decide whether some value needs to be interpreted as boolean.
function determineType(typeName, properties) {
  let type;
  if (properties.hasString) {
    type = GraphQLString
  } else if (properties.hasFloat) {
    type = GraphQLFloat
  } else if (properties.hasInt) {
    type = GraphQLInt
  }

  if (type === undefined) {
    if (properties.name.endsWith("Min") ||
      properties.name.endsWith("Max") ||
      properties.name.endsWith("Lvl") ||
      properties.name.endsWith("able") ||
      properties.name.endsWith("stack") ||
      properties.name.startsWith("min") ||
      properties.name.startsWith("max")
    ) {
      type = GraphQLInt
    } else {
      console.log(`WARN: [${typeName}] Type not determinable, assuming String: ${JSON.stringify(properties)}`)
      type = GraphQLString
    }
  }

  if (!properties.hasNull) {
    type += "!"
  }
  return type
}

function removeDuplicates(result) {
  return [...new Set(result
    .map(it => it.result.map(it2 => it2.doc))
    .flat()
  )]
}

function appendToSchema(fieldName, typeName, columnProperties, schemaComposer) {
  const fields = Object.assign(...columnProperties.map(properties => ({[properties.name]: determineType(typeName, properties)})))
  const type = schemaComposer.createObjectTC({
    name: typeName,
    fields: fields,
  })
  const defaultOptions = {enrich: true};

  schemaComposer.Query.addFields({
    [fieldName]: schemaComposer.createResolver({
      name: "find" + typeName,
      type: [type],
      args: {
        contains: GraphQLString,
        query: [GraphQLJSONObject],
      },
      resolve: async ({source, args, context, info}) => {
        if (args.contains === undefined && args.query === undefined) {
          return Object.values(documents[fieldName].store);
        }
        if (args.query === undefined) {
          const result = documents[fieldName].search(args.contains, Object.assign(defaultOptions));
          return removeDuplicates(result)
        }

        // FIXME this doesn't work yet because of https://github.com/nextapps-de/flexsearch/issues/264
        //  also, how to incorporate {enrich: true}?
        const result = documents[fieldName].search(args.query);
        return removeDuplicates(result)
      }
    })
  })
}

for (const file of filenames) {
  await parseFile(file, schemaComposer)
}

export const d2rSchema = schemaComposer.buildSchema()
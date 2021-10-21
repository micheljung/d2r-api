# Diablo 2: Resurrected API (d2r-api)

An API that provides game data as a GraphQL API.

## How to use

1. Start the API using Docker:

       docker run -d -p "4000:4000" micheljung/d2r-api

1. Access, or post your queries to, [http://localhost:4000/graphql](http://localhost:4000/graphql)

## Examples

### List all Unique items

Query:

```
{
  uniqueitems {
    index
    itemName
  }
}
```

Result:

```json
{
  "data": {
    "uniqueitems": [
      {
        "index": "The Gnasher",
        "itemName": "Hand Axe"
      },
      ...
      {
        "index": "Hellfire Torch",
        "itemName": "charm"
      }
    ]
  }
}
```

### Find all complete rune words

Query:

```
{
  runes(query: [{field:"complete", query: 1}]) {
    runeName
    runesUsed
  }
}
```

Result:

```
{
  "data": {
    "runes": [
      {
        "runeName": "Ancients' Pledge",
        "runesUsed": "RalOrtTal"
      },
      ...
      {
        "runeName": "Zephyr",
        "runesUsed": "OrtEth"
      }
    ]
  }
}
```

Currently, all queried fields are `AND`. `OR` is not yet supported, nor is any combination of both.

## Where the Data comes from

Using the [data-extractor](build-plugins/data-extractor), a developer can extract the game's "excel" data directly
from a game installation into the folder [d2r](d2r). This could be improved by reading the data directly from Blizzard's
CDN at build time, so no game installation is needed.

All data is extracted except `sounds.txt` which is considered big and useless. Other useless data might be removed in
future, too.

Any empty or "category" lines (like "Expansion", "Armors") are removed based on heuristic. Otherwise, the data contains
everything that's present in the game files, including [unused items](https://tcrf.net/Diablo_II/Unused_Objects). It's
up to the caller to detect and remove such unwanted.

To update the extracted data, execute:

    .\gradlew extract

or if your installation is at a non-default location:

     .\gradlew -PdataPath="G:/Diablo II Resurrected/Data" extract

## About the GraphQL Schema

Instead of creating a static GraphQL schema for each entity, it's auto-detected by the following rules:

1. If a field contains any empty value, it's considered nullable
1. If a field contains only numbers, it's considered an `Int`
1. If a field contains only numbers and one of them is a float, it's considered an `Float`
1. If a field contains any non-number content, it's considered a `String`
1. If a field contains no values at all, it's considered `String` and a warning is logged

This leads to a non-perfect schema since e.g. `Boolean` can't be detected reliably (and thus is never used), and the
type of empty columns is most likely incorrect. However, it also removes any manual work, human errors and never needs
to be updated.

If this decision turns out to be problematic, let me know so that a static schema can be added in the future.

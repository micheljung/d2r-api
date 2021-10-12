export const schema = `
  interface Node {
    id: ID
  }
  type Task implements Node {
    id: ID!
    name: String!
    complete: Boolean!
  }
  type Query {
    tasks: [Task]
    task(id: ID): Task
  }
`

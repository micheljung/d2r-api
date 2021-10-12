export default {
  tasks: () => {
    return [ 
      { id:"CB031B56-420C-440B-B09C-857CDC3FDBC4", name:'Go to Market', complete:false },
      { id:"27F08563-47F9-4EB6-9A3C-1ACD07FB68F7", name:'Walk the dog', complete:true },
      { id:"F5A5C82B-F919-4551-A0F0-DEFA11B04FA2", name:'Take a nap', complete:false }
    ]
  },
  task: (args) => {
    return [
      { id:"CB031B56-420C-440B-B09C-857CDC3FDBC4", name:'Go to Market', complete:false },
      { id:"27F08563-47F9-4EB6-9A3C-1ACD07FB68F7", name:'Walk the dog', complete:true },
      { id:"F5A5C82B-F919-4551-A0F0-DEFA11B04FA2", name:'Take a nap', complete:false }
    ].find(o => o.id === args.id)
  }
}

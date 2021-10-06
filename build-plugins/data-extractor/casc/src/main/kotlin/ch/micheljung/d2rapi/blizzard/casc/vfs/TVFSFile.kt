package ch.micheljung.d2rapi.blizzard.casc.vfs

/**
 * TVFS file containing file system path nodes.
 */
class TVFSFile(
  val version: Byte,
  val flags: Int,
  val encodingKeySize: Int,
  val patchKeySize: Int,
  val maximumPathDepth: Int,
  rootNodeList: List<PathNode>
) {
  private val rootNodes: Array<PathNode> = rootNodeList.toTypedArray()

  fun getRootNode(index: Int): PathNode {
    return rootNodes[index]
  }

  fun getRootNodeCount(): Int {
    return rootNodes.size
  }
}
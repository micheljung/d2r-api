package ch.micheljung.d2rapi.blizzard.casc.vfs

/**
 * Prefix nodes generate a path prefix for other nodes.
 */
class PrefixNode(pathFragments: List<ByteArray>, nodes: List<PathNode>) : PathNode(pathFragments) {
  /** Array of child node that this node forms a prefix of.  */
  private val nodes: Array<PathNode> = nodes.toTypedArray()
  fun getNodeCount(): Int {
    return nodes.size
  }

  fun getNode(index: Int): PathNode {
    return nodes[index]
  }

}
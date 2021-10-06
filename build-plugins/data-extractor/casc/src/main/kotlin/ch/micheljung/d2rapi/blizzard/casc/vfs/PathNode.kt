package ch.micheljung.d2rapi.blizzard.casc.vfs

/** Represents a path node. Path nodes can either be prefix nodes or file nodes. */
abstract class PathNode protected constructor(pathFragments: List<ByteArray>) {
  /**
   * Array of path fragments. Each fragment represents part of a path string. Due
   * to the potential for multi byte encoding, one cannot assume that each
   * fragment can be assembled into a valid string.
   */
  private val pathFragments: Array<ByteArray> = pathFragments.toTypedArray()

  fun getPathFragmentCount(): Int {
    return pathFragments.size
  }

  fun getFragment(index: Int): ByteArray {
    return pathFragments[index]
  }

}
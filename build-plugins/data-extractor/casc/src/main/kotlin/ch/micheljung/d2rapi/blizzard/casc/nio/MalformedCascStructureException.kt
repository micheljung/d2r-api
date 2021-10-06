package ch.micheljung.d2rapi.blizzard.casc.nio

import java.io.IOException

class MalformedCascStructureException : IOException {
  constructor(message: String) : super(message)
  constructor(message: String, cause: Throwable) : super(message, cause)
}
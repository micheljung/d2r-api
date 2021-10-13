package ch.micheljung.d2rapi.dto

import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Path


class Csv2Json {
  private val csv = Csv()
  private val objectMapper = ObjectMapper()

  fun convert(path: Path): String = csv.read(path).let { objectMapper.writeValueAsString(it) }
}
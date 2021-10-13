package ch.micheljung.d2rapi.dto

import com.fasterxml.jackson.databind.ObjectMapper


class Json {
  private val objectMapper = ObjectMapper()

  fun asString(any: Any): String = objectMapper.writeValueAsString(any)
}
package ch.micheljung.d2rapi.dto

import java.util.*
import java.util.regex.Pattern

private val INVALID_CHARACTERS = Pattern.compile("[^\\w\\d]")
private val CAPITALS_AT_START = Pattern.compile("^([A-Z]+)")

fun columnToPropertyName(columnName: String): String =
  INVALID_CHARACTERS.matcher(columnName).replaceAll { "" }
    .let { CAPITALS_AT_START.matcher(it).replaceFirst { match -> match.group().lowercase(Locale.getDefault()) } }
    .let { if (it.first().isDigit()) "_$it" else it }

package ch.micheljung.d2rapi.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import java.nio.file.Path


class Csv2Json {
  private val csvMapper = CsvMapper()
  private val objectMapper = ObjectMapper()
  private val csv: CsvSchema = CsvSchema.emptySchema().withHeader().withColumnSeparator('\t')

  fun convert(path: Path): String = csvMapper.reader()
    .forType(MutableMap::class.java)
    .with(csv)
    .readValues<Map<*, *>?>(path.toFile())
    .readAll().let { objectMapper.writeValueAsString(it) }
}
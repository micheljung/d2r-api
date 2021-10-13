package ch.micheljung.d2rapi.dto

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import java.nio.file.Path


class Csv {
  private val csvMapper = CsvMapper()
  private val csv: CsvSchema = CsvSchema.emptySchema().withHeader().withColumnSeparator('\t')

  fun read(path: Path): MutableList<Map<*, *>> = csvMapper.reader()
    .forType(MutableMap::class.java)
    .with(csv)
    .readValues<Map<*, *>?>(path.toFile())
    .readAll()
}
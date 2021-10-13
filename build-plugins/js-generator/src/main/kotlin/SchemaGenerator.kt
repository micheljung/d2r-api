package ch.micheljung.d2rapi.dto

import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName
import org.gradle.api.logging.Logger
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern
import kotlin.reflect.KClass
import kotlin.streams.toList

private val NUMBER_PATTERN: Pattern = Pattern.compile("^-?(?:0|[1-9]\\d*|\\.\\d+|\\d+\\.|\\d+\\.\\d+)$")

class SchemaGenerator(private val logger: Logger) {

  fun generate(sourcePath: Path, targetPath: Path) {
    Files.createDirectories(targetPath)

    val files = Files.list(sourcePath).use { it.toList() }

    logger.info("Building schema...")
    val typeDefinitions = files.map { path ->
      val properties = parseProperties(path)

      val typeName = typeName(path)
      """
      |type $typeName {
      |${
        properties.joinToString("\n") {
          "  ${it.name}: ${
            it.type.toString().replace("kotlin.", "").replace("?", "")
          }${if (it.type.isNullable) "" else "!"}"
        }
      }
      |}
      """.trimMargin()
    }.joinToString("\n")

    val queries = files.map { path ->
      val queryName = path.baseName()
      val typeName = typeName(path)
      "  $queryName(query: String, options: String): [$typeName]"
    }.joinToString("\n")

    Files.writeString(
      targetPath.resolve("schema.mjs"),
      """
        |export const schema = `
        |$typeDefinitions
        |type Query {
        |$queries
        |}
        |`
        """.trimMargin()
    )
  }

  private fun typeName(path: Path) = path.baseName().replaceFirstChar { it.titlecase() }

  private fun parseProperties(it: Path): List<PropertySpec> {
    val columnProperties = Files.lines(it, StandardCharsets.UTF_8).use { stream ->
      stream.findFirst()
        .map { headerLine -> headerLine.split("\t") }
        .orElseThrow()
        .map { columnToPropertyName(it) }
        .map { ColumnProperties(it) }
        .toMutableList()
    }

    Files.lines(it, StandardCharsets.UTF_8).use { stream ->
      stream.skip(1)
        .map {
          it.split("\t").forEachIndexed { index, string ->
            val properties: ColumnProperties = columnProperties[index]
            columnProperties[index] = detectColumnProperties(properties, string)
          }
        }
        .toList()
    }

    return columnProperties.map {
      val type = determineTypeName(it)
      PropertySpec.builder(it.name, type).build()
    }
  }

  private fun determineTypeName(properties: ColumnProperties) =
    when {
      properties.hasString -> String::class
      properties.hasBigDecimal -> Float::class
      properties.hasBigInteger -> Int::class
      properties.hasDouble -> Float::class
      properties.hasFloat -> Float::class
      properties.hasLong -> Int::class
      properties.hasInt -> Int::class
      else -> String::class
    }.asTypeName().copy(nullable = true)

  fun detectColumnProperties(current: ColumnProperties, value: String): ColumnProperties {
    val isNumber = NUMBER_PATTERN.matcher(value).matches()
    if (value.isBlank()) {
      return current.apply { hasNull = true }
    }
    if (!isNumber) {
      return current.apply { hasString = true }
    }
    if (isValidInt(value)) {
      return current.apply { hasInt = true }
    }
    if (isValidLong(value)) {
      return current.apply { hasLong = true }
    }
    if (isValidFloat(value)) {
      return current.apply { hasFloat = true }
    }
    if (isValidDouble(value)) {
      return current.apply { hasDouble = true }
    }
    if (isValidBigInteger(value)) {
      return current.apply { hasBigInteger = true }
    }
    if (isValidBigDecimal(value)) {
      return current.apply { hasBigDecimal = true }
    }

    return current
  }

  private fun isValidInt(value: String) = value.isNotBlank() &&
    value.toIntOrNull().let { it.toString() == value }

  private fun isValidLong(value: String) = value.isNotBlank() &&
    value.toLongOrNull().let { it.toString() == value }

  private fun isValidFloat(value: String) =
    value.isNotBlank() && value.toFloatOrNull()
      .let { it.toString().dropLastWhile { char -> char == '0' } == value.dropLastWhile { char -> char == '0' } }

  private fun isValidDouble(value: String) =
    value.isNotBlank() && value.toDoubleOrNull()
      .let { it.toString().dropLastWhile { char -> char == '0' } == value.dropLastWhile { char -> char == '0' } }

  private fun isValidBigInteger(value: String) = value.isNotBlank() &&
    value.toBigIntegerOrNull().let { it.toString() == value }

  private fun isValidBigDecimal(value: String) = value.isNotBlank() &&
    value.toBigDecimalOrNull().let { it.toString() == value }

  data class TypeDetection(val name: String, val type: PropertyType, val sure: Boolean, val nullable: Boolean)

  enum class PropertyType(val kClass: KClass<*>) {
    UNKNOWN(String::class),
    STRING(String::class),
    INTEGER(Int::class),
    FLOAT(Float::class),
    DOUBLE(Double::class),
    BIG_DECIMAL(BigDecimal::class),
  }

  data class ColumnProperties(
    val name: String,
    var hasNull: Boolean = false,
    var hasBigInteger: Boolean = false,
    var hasBigDecimal: Boolean = false,
    var hasDouble: Boolean = false,
    var hasFloat: Boolean = false,
    var hasInt: Boolean = false,
    var hasLong: Boolean = false,
    var hasString: Boolean = false,
  )
}


package ch.micheljung.d2rapi.dto

import com.squareup.kotlinpoet.*
import org.gradle.api.logging.Logger
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern
import kotlin.reflect.KClass
import kotlin.streams.toList

private val GENERATED = ClassName("javax.annotation.processing", "Generated")
private val NUMBER_PATTERN: Pattern = Pattern.compile("^-?(?:0|[1-9]\\d*|\\.\\d+|\\d+\\.|\\d+\\.\\d+)$")

class DtoGenerator(private val logger: Logger) {

  fun generate(sourcePath: Path, targetPath: Path) {
    Files.createDirectories(targetPath)

    Files.list(sourcePath).use { stream ->
      stream
        // FIXME this file currently causes problems, see https://github.com/wildfly/jandex/issues/156
        .filter { it.fileName.toString() != "skills.txt" }
        .forEach {
          logger.info("Processing $it")
          val className = it.fileName.toString().substringBeforeLast('.').capitalize()
          val properties = parseProperties(it)

          val fileSpec = createClass(className, properties)

          logger.info("Writing ${targetPath.resolve(fileSpec.toJavaFileObject().toUri().toASCIIString())}")
          fileSpec.writeTo(targetPath)
        }
    }
  }

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
      properties.hasBigDecimal -> BigDecimal::class
      properties.hasBigInteger -> BigInteger::class
      properties.hasDouble -> Double::class
      properties.hasFloat -> Float::class
      properties.hasLong -> Long::class
      properties.hasInt -> Int::class
      else -> String::class
    }.asTypeName().copy(nullable = properties.hasNull)

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

  private fun createClass(name: String, headers: List<PropertySpec>): FileSpec {
    val className = ClassName(javaClass.packageName, name)

    val constructorBuilder = FunSpec.constructorBuilder()
    headers.forEach {
      constructorBuilder.addParameter(it.name, it.type)
    }

    val classBuilder = TypeSpec.classBuilder(className)
      .addAnnotation(GENERATED)
      .addModifiers(KModifier.DATA)
      .primaryConstructor(constructorBuilder.build())

    headers.forEach {
      classBuilder.addProperty(PropertySpec.builder(it.name, it.type).initializer(it.name).build())
    }

    return FileSpec.builder(className.packageName, className.simpleName)
      .addType(classBuilder.build())
      .suppressWarningTypes("RedundantVisibilityModifier")
      .build()
  }

  private fun FileSpec.Builder.suppressWarningTypes(vararg types: String): FileSpec.Builder = apply {
    if (types.isEmpty()) {
      return this
    }

    val format = "%S,".repeat(types.count()).trimEnd(',')
    addAnnotation(
      AnnotationSpec.builder(ClassName("", "Suppress"))
        .addMember(format, *types)
        .build()
    )
  }

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
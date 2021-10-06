package ch.micheljung.d2rapi.dto

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.gradle.api.logging.Logger
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

private val APPLICATION_SCOPED = ClassName("javax.enterprise.context", "ApplicationScoped")
private val EXPERIMENTAL_STDLIB_API = ClassName("kotlin", "ExperimentalStdlibApi")
private val LIST = ClassName("kotlin.collections", "List")
private val GENERATED = ClassName("javax.annotation.processing", "Generated")

class ServiceGenerator(private val logger: Logger) {
  fun generate(sourcePath: Path, targetPath: Path, dataFolderPath: Path) {
    Files.createDirectories(targetPath)

    Files.list(sourcePath).use { stream ->
      stream
        // FIXME this file currently causes problems, see https://github.com/wildfly/jandex/issues/156
        .filter { it.fileName.toString() != "skills.txt" }
        .forEach {
          logger.info("Processing $it")
          val baseName = it.fileName.toString().substringBeforeLast('.').capitalize()

          val fileSpec = createClass(baseName, it, dataFolderPath)

          logger.info("Writing ${targetPath.resolve(fileSpec.toJavaFileObject().toUri().toASCIIString())}")
          fileSpec.writeTo(targetPath)
        }
    }
  }

  private fun createClass(baseName: String, resourceFile: Path, dataFolderPath: Path): FileSpec {
    val packageName = javaClass.packageName
    val serviceClassName = ClassName(packageName, "${baseName}Service")
    val dtoClassName = ClassName(packageName, baseName)

    val customKeyMap = Files.lines(resourceFile, StandardCharsets.UTF_8).use { stream ->
      stream.findFirst()
        .map { headerLine -> headerLine.split("\t") }
        .orElseThrow()
        .associateWith { columnToPropertyName(it) }
    }

    val typeSpec = TypeSpec.classBuilder(serviceClassName)
      .addAnnotation(APPLICATION_SCOPED)
      .addAnnotation(EXPERIMENTAL_STDLIB_API)
      .addAnnotation(GENERATED)
      .addProperty(
        PropertySpec
          .builder("items", LIST.parameterizedBy(dtoClassName))
          .initializer(
            """grass<${dtoClassName.simpleName}>{
            |  customKeyMap = mapOf(
            |    ${customKeyMap.entries.joinToString(separator = ",\n    ") { (key, value) -> "\"$key\" to \"$value\"" }}
            |  )
            |}.harvest(
            |  csvReader { delimiter = '\t' }
            |    .readAllWithHeader(javaClass.getResourceAsStream("/${dataPath(resourceFile, dataFolderPath)}")!!)
            |)
            """.trimMargin()
          )
          .build()
      )
      .build()

    return FileSpec.builder(packageName, serviceClassName.simpleName)
      .addImport("com.github.doyaaaaaken.kotlincsv.dsl", "csvReader")
      .addImport("io.blackmo18.kotlin.grass.dsl", "grass")
      .addImport("java.io", "BufferedReader")
      .addImport("java.io", "InputStreamReader")
      .addImport("java.util.stream", "Collectors")
      .addType(typeSpec)
      .suppressWarningTypes("RedundantVisibilityModifier")
      .build()
  }

  private fun dataPath(resourceFile: Path, dataFolderPath: Path) =
    dataFolderPath.relativize(resourceFile).toString().replace('\\', '/')

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
}
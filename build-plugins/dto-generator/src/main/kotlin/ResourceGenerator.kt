package ch.micheljung.d2rapi.dto

import com.squareup.kotlinpoet.*
import org.gradle.api.logging.Logger
import java.nio.file.Files
import java.nio.file.Path

private val REQUEST_SCOPED = ClassName("javax.enterprise.context", "RequestScoped")
private val EXPERIMENTAL_STDLIB_API = ClassName("kotlin", "ExperimentalStdlibApi")
private val GRAPHQL_API = ClassName("org.eclipse.microprofile.graphql", "GraphQLApi")
private val INJECT = ClassName("javax.inject", "Inject")
private val DEFAULT = ClassName("javax.enterprise.inject", "Default")
private val QUERY = ClassName("org.eclipse.microprofile.graphql", "Query")
private val DESCRIPTION = ClassName("org.eclipse.microprofile.graphql", "Description")
private val GENERATED = ClassName("javax.annotation.processing", "Generated")

class ResourceGenerator(private val logger: Logger) {
  fun generate(sourcePath: Path, targetPath: Path) {
    Files.createDirectories(targetPath)

    Files.list(sourcePath).use { stream ->
      stream
        // FIXME this file currently causes problems, see https://github.com/wildfly/jandex/issues/156
        .filter { it.fileName.toString() != "skills.txt" }
        .forEach {
          logger.info("Processing $it")
          val baseName = it.fileName.toString().substringBeforeLast('.').capitalize()

          val fileSpec = createClass(baseName)

          logger.info("Writing ${targetPath.resolve(fileSpec.toJavaFileObject().toUri().toASCIIString())}")
          fileSpec.writeTo(targetPath)
        }
    }
  }

  private fun createClass(baseName: String): FileSpec {
    val packageName = javaClass.packageName
    val resourceClassName = ClassName(packageName, "${baseName}Resource")
    val serviceClassName = ClassName(packageName, "${baseName}Service")

    val typeSpec = TypeSpec.classBuilder(resourceClassName)
      .addAnnotation(REQUEST_SCOPED)
      .addAnnotation(EXPERIMENTAL_STDLIB_API)
      .addAnnotation(GRAPHQL_API)
      .addAnnotation(GENERATED)
      .addProperty(
        PropertySpec
          .builder("service", serviceClassName)
          .mutable()
          .addModifiers(KModifier.LATEINIT)
          .addAnnotation(INJECT)
          .addAnnotation(AnnotationSpec.builder(DEFAULT).useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD).build())
          .build()
      )
      .addFunction(
        FunSpec
          .builder("getAll")
          .addAnnotation(AnnotationSpec.builder(QUERY).addMember("\"${baseName.decapitalize()}\"").build())
          .addAnnotation(AnnotationSpec.builder(DESCRIPTION).addMember("\"Get all ${baseName.capitalize()}\"").build())
          .addStatement("return %L", "service.items")
          .build()
      )
      .build()

    return FileSpec.builder(packageName, resourceClassName.simpleName)
      .addType(typeSpec)
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
}
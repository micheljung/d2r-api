pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
  includeBuild("build-plugins/data-extractor")
  includeBuild("build-plugins/dto-generator")
}
rootProject.name = "d2r-api"

include("api")

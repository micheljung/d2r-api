pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
  includeBuild("build-plugins/data-extractor")
  includeBuild("build-plugins/sprite-converter")
}
rootProject.name = "d2r-api"

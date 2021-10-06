pluginManagement {
  repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
  }
}
rootProject.name = "dto-generator"

includeBuild("../data-extractor")

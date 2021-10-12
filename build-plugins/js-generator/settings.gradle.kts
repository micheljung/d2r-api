pluginManagement {
  repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
    maven(url = "https://nexus.prod.zkb.ch/repository/public/")
  }
}
rootProject.name = "js-generator"

includeBuild("../data-extractor")

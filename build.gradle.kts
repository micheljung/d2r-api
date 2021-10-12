plugins {
  kotlin("jvm") version "1.5.31"
  kotlin("plugin.allopen") version "1.6.0-M1" apply false
  id("ch.micheljung.d2rapi.data-extractor")
}

allprojects {
  apply(plugin = "kotlin")

  group = "ch.micheljung.d2rapi"
  version = "1.0-SNAPSHOT"

  repositories {
    mavenCentral()
    maven(url = "https://nexus.prod.zkb.ch/repository/public/")
  }
}

d2rExtractor {
  source.set(File("C:/Program Files (x86)/Diablo II Resurrected/Data"))
  target.set(layout.projectDirectory.dir("d2r"))
}

dependencies {
  implementation(kotlin("stdlib"))
}

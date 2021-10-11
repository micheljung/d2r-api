import java.nio.file.Path

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
  }
}

d2rExtractor {
  source.set(File("C:/Program Files (x86)/Diablo II Resurrected/Data"))
  target.set(layout.projectDirectory.dir("d2r"))
  excludes.set(listOf(
    // This file is big, useless and contains a column "4737" with no values
    "data/data/global/excel/sounds.txt"
  ))
}

dependencies {
  implementation(kotlin("stdlib"))
}

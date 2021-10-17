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
  val source = project.findProperty("dataPath")?.toString() ?: "C:/Program Files (x86)/Diablo II Resurrected/Data"
  this.source.set(File(source))
  target.set(layout.projectDirectory.dir("api/d2r"))
  excludes.set(
    listOf(
      // This file is big, useless and contains a column "4737" with no values
      "data/data/global/excel/sounds.txt"
    )
  )
}

dependencies {
  implementation(kotlin("stdlib"))
}

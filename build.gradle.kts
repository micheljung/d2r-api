plugins {
  kotlin("jvm") version "1.5.31"
  kotlin("plugin.allopen") version "1.6.0-M1" apply false
  id("io.quarkus") version "2.3.0.Final" apply false
  id("ch.micheljung.d2rapi.data-extractor") apply false
}

allprojects {
  apply(plugin = "kotlin")

  group = "ch.micheljung.d2rapi"
  version = "1.0-SNAPSHOT"

  repositories {
    mavenCentral()
  }
}

dependencies {
  implementation(kotlin("stdlib"))
}

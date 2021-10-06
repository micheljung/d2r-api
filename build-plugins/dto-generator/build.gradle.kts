plugins {
  kotlin("jvm") version "1.5.31"
  id("java-gradle-plugin")
}

group = "ch.micheljung.d2rapi"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation("com.squareup:kotlinpoet:1.10.1")
  implementation("ch.micheljung.d2rapi:data-extractor")

  testImplementation("io.mockk:mockk:1.12.0")

  val kotestVersion = "4.6.3"
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.test {
  useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
  kotlinOptions.javaParameters = true
}

gradlePlugin {
  plugins {
    create("dtoGenerator") {
      id = "ch.micheljung.d2rapi.dto-generator"
      implementationClass = "ch.micheljung.d2rapi.dto.DtoGeneratorPlugin"
    }
  }
}

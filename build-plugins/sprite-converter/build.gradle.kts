plugins {
  kotlin("jvm") version "1.5.31"
  id("java-gradle-plugin")
}

group = "ch.micheljung.d2rapi"
version = "1.0-SNAPSHOT"

allprojects {
  repositories {
    mavenCentral()
  }
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation("com.google.guava:guava:31.0.1-jre")

  testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.1")
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
  kotlinOptions.javaParameters = true
}

tasks.test {
  useJUnitPlatform()
}

gradlePlugin {
  plugins {
    create("dataExtractor") {
      id = "ch.micheljung.d2rapi.sprite-converter"
      implementationClass = "ch.micheljung.d2rapi.extract.SpriteConverterPlugin"
    }
  }
}

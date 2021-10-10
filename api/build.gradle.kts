plugins {
  kotlin("jvm")
  kotlin("plugin.allopen")
  id("io.quarkus")
  id("org.kordamp.gradle.jandex") version "0.11.0"
  id("ch.micheljung.d2rapi.dto-generator")
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
  implementation(kotlin("stdlib-jdk8"))

  implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))

  implementation("io.quarkus:quarkus-kotlin")

  implementation("io.quarkus:quarkus-smallrye-graphql")
  implementation("io.quarkus:quarkus-arc")
  implementation("io.quarkus:quarkus-resteasy")
  implementation("com.github.doyaaaaaken:kotlin-csv-jvm:0.15.2")
  implementation("io.github.blackmo18:kotlin-grass-core-jvm:1.0.0")
  implementation("io.github.blackmo18:kotlin-grass-parser-jvm:0.8.0")

  testImplementation("io.quarkus:quarkus-junit5")
  testImplementation("io.rest-assured:rest-assured")
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

allOpen {
  annotation("javax.ws.rs.Path")
  annotation("javax.enterprise.context.ApplicationScoped")
  annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
  kotlinOptions.javaParameters = true
}

sourceSets["main"].java {
  srcDir("build/generated/sources/d2r")
}

d2rExtractor {
  source.set(File("C:/Program Files (x86)/Diablo II Resurrected/Data"))
  target.set(layout.projectDirectory.dir("src/main/resources/d2r"))
}

d2rDtoGenerator {
  source.set(d2rExtractor.target)
  target.set(layout.buildDirectory.dir("generated/sources/d2r"))
}

tasks.compileKotlin {
  dependsOn(tasks.generate)
}
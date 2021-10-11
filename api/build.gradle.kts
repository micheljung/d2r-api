import com.github.gradle.node.npm.task.NpxTask

plugins {
  id("com.github.node-gradle.node") version "3.1.1"
  id("ch.micheljung.d2rapi.js-generator")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
  kotlinOptions.javaParameters = true
}

d2rJsGenerator {
  source.set(rootProject.d2rExtractor.target)
  target.set(layout.buildDirectory.dir("js"))
}

tasks.compileKotlin {
  dependsOn(tasks.generate)
}

tasks.generate {
  dependsOn(tasks.processResources)
}

tasks.build {
  dependsOn(tasks.generate)
}

tasks.register<NpxTask>("createBundle") {
  dependsOn(tasks.npmInstall)
  command.set("npx")
  args.set(listOf("rollup", "--format=cjs", "--file=bundle.js", "--", "index.mjs"))
  inputs.files("package.json", "package-lock.json", "index.mjs")
  inputs.dir("src")
  inputs.dir(fileTree("node_modules").exclude(".cache"))
  outputs.dir("dist")
}
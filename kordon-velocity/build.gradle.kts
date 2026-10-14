// Velocity 3.4+ / 4.x module. The Velocity API is compileOnly and never ends up in Kordon.jar.
plugins {
    `java-library`
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":kordon-common"))
    compileOnly(libs.velocity.api)
}

// velocity-api ships an annotation processor that would generate a second velocity-plugin.json.
tasks.withType<JavaCompile>().configureEach { options.compilerArgs.add("-proc:none") }

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("velocity-plugin.json") { expand(props) }
}

tasks.shadowJar {
    archiveFileName.set("Kordon-Velocity.jar")
    relocate("com.google.gson", "com.kordonlabs.plugin.lib.gson")
    dependencies { exclude(dependency("com.google.errorprone:.*")) }
    mergeServiceFiles()
    exclude(
        "module-info.class", "META-INF/versions/*/module-info.class", "META-INF/maven/**",
        "META-INF/proguard/**", "META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA"
    )
    manifest {
        attributes(
            "Implementation-Title" to "Kordon-Velocity",
            "Implementation-Version" to project.version
        )
    }
    addMultiReleaseAttribute = false
}

tasks.assemble { dependsOn(tasks.shadowJar) }

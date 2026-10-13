// Lumi / Nukkit platform module.
plugins {
    `java-library`
    alias(libs.plugins.shadow)
}

val lumiJar = providers.systemProperty("lumi.jar")
    .orElse(providers.environmentVariable("LUMI_JAR"))
    .orElse(providers.provider {
        listOf(
            "D:/AEZAMINE/survival/Lumi.jar",
            "../survival/Lumi.jar",
            "../../survival/Lumi.jar",
            "libs/Lumi.jar",
            "../libs/Lumi.jar"
        ).firstOrNull { file(it).exists() } ?: "D:/AEZAMINE/survival/Lumi.jar"
    })

dependencies {
    implementation(project(":kordon-common"))
    compileOnly(files(lumiJar))
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("nukkit.yml", "plugin.yml", "kordon/version.properties")) { expand(props) }
}

tasks.shadowJar {
    archiveFileName.set("Kordon-Lumi.jar")
    relocate("com.google.gson", "com.kordonlabs.plugin.lib.gson")
    dependencies { exclude(dependency("com.google.errorprone:.*")) }
    mergeServiceFiles()
    exclude(
        "module-info.class", "META-INF/versions/*/module-info.class", "META-INF/maven/**",
        "META-INF/proguard/**", "META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA"
    )
    manifest {
        attributes(
            "Implementation-Title" to "Kordon-Lumi",
            "Implementation-Version" to project.version
        )
    }
    addMultiReleaseAttribute = false
}

tasks.assemble { dependsOn(tasks.shadowJar) }

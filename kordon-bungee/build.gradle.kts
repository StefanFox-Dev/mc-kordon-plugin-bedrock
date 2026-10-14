plugins {
    `java-library`
    alias(libs.plugins.shadow)
}

// Non-transitive on purpose: bungeecord-protocol depends on com.mojang:brigadier, which is not on Maven Central.
val bungeeApi = listOf(
    libs.bungeecord.api,
    libs.bungeecord.chat,
    libs.bungeecord.config,
    libs.bungeecord.event,
    libs.bungeecord.protocol,
    libs.bungeecord.serializer,
    libs.bungeecord.dialog,
)

dependencies {
    implementation(project(":kordon-common"))
    for (api in bungeeApi) {
        compileOnly(api) { isTransitive = false }
    }
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("bungee.yml", "kordon/version.properties")) { expand(props) }
}

tasks.shadowJar {
    archiveFileName.set("Kordon-Bungee.jar")
    relocate("com.google.gson", "com.kordonlabs.plugin.lib.gson")
    dependencies { exclude(dependency("com.google.errorprone:.*")) }
    mergeServiceFiles()
    exclude(
        "module-info.class", "META-INF/versions/*/module-info.class", "META-INF/maven/**",
        "META-INF/proguard/**", "META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA"
    )
    manifest {
        attributes(
            "Implementation-Title" to "Kordon-Bungee",
            "Implementation-Version" to project.version
        )
    }
    addMultiReleaseAttribute = false
}

tasks.assemble { dependsOn(tasks.shadowJar) }

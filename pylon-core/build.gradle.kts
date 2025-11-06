import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    kotlin("jvm")
    `java-library`
    id("com.gradleup.shadow") version "9.0.0"
    id("de.eldoria.plugin-yml.paper") version "0.7.1"
    idea
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
}

repositories {
    mavenLocal()
    maven("https://repo.xenondevs.xyz/releases") { name = "InvUI" }
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

dependencies {
    fun paperLibraryApi(dep: Any) {
        paperLibrary(dep)
        compileOnlyApi(dep)
    }

    runtimeOnly(project(":nms"))

    // Kotlin 2.1.10 toolchain
    paperLibraryApi("org.jetbrains.kotlin:kotlin-stdlib:2.1.10")
    paperLibraryApi("org.jetbrains.kotlin:kotlin-reflect:2.1.10")
    paperLibraryApi("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // Updated Paper API for 1.21.10
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")

    // Bukkit / coroutine & InvUI dependencies
    paperLibraryApi("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.22.0")
    paperLibraryApi("com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:2.22.0")
    paperLibraryApi("xyz.xenondevs.invui:invui-core:1.47")
    paperLibrary("xyz.xenondevs.invui:inventory-access-r25:1.47:remapped-mojang")
    paperLibraryApi("xyz.xenondevs.invui:invui-kotlin:1.46")

    api("com.github.Tofaa2.EntityLib:spigot:2.4.11")
    implementation("com.github.retrooper:packetevents-spigot:2.10.0")
    implementation("info.debatty:java-string-similarity:2.0.0")
    implementation("org.bstats:bstats-bukkit:2.2.1")
    paperLibrary("com.github.ben-manes.caffeine:caffeine:3.2.2")

    dokkaPlugin(project(":dokka-plugin"))

    testImplementation(kotlin("test"))
    testImplementation("com.willowtreeapps.assertk:assertk:0.28.1")
    testImplementation("net.kyori:adventure-api:4.20.0")
    testImplementation("net.kyori:adventure-text-minimessage:4.20.0")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        javaParameters = true
        freeCompilerArgs = listOf("-Xjvm-default=all", "-Xwhen-guards")
    }
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/docs/kdoc"))
    }
    dokkaPublications.javadoc {
        outputDirectory.set(layout.buildDirectory.dir("dokka/docs/javadoc"))
    }
    dokkaSourceSets.configureEach {
        externalDocumentationLinks.register("Paper") {
            url("https://jd.papermc.io/paper/1.21.10/")
            packageListUrl("https://jd.papermc.io/paper/1.21.10/element-list")
        }
        externalDocumentationLinks.register("JOML") {
            url("https://javadoc.io/doc/org.joml/joml/latest/")
            packageListUrl("https://javadoc.io/doc/org.joml/joml/latest/element-list")
        }
        externalDocumentationLinks.register("Adventure") {
            url("https://javadoc.io/doc/net.kyori/adventure-api/latest/")
            packageListUrl("https://javadoc.io/doc/net.kyori/adventure-api/latest/element-list")
        }
        externalDocumentationLinks.register("InvUI") {
            url("https://invui.javadoc.xenondevs.xyz/")
            packageListUrl("https://invui.javadoc.xenondevs.xyz/element-list")
        }
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/wickidcow/pylon-core")
            remoteLineSuffix.set("#L")
        }
    }
    dokkaPublications.configureEach { suppressObviousFunctions = true }
}

tasks.dokkaGeneratePublicationJavadoc {
    doLast {
        val searchJs = layout.buildDirectory.file("dokka/docs/javadoc/search.js").get().asFile
        val text = searchJs.readText()
        val codeToFix = "const result = [...modules, ...packages, ...types, ...members, ...tags]"
        if (codeToFix !in text) {
            throw IllegalStateException("Seggan you buffoon, verify Dokka search fix still applies")
        }
        val fixed = """
            const result = [
                ...modules.slice(0, 5),
                ...packages.slice(0, 5),
                ...types.slice(0, 40),
                ...members.slice(0, 40),
                ...tags.slice(0, 5)
            ]
        """.trimIndent()
        searchJs.writeText(text.replace(codeToFix, fixed))
    }
}

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGeneratePublicationHtml)
    archiveClassifier.set("javadoc")
    from(layout.buildDirectory.dir("dokka/docs/kdoc"))
}

tasks.shadowJar {
    mergeServiceFiles()

    exclude("kotlin/**")
    exclude("org/intellij/lang/annotations/**")
    exclude("org/jetbrains/annotations/**")

    relocate("com.github.retrooper.packetevents", "io.github.pylonmc.pylon.core.packetevents")
    relocate("me.tofaa.entitylib", "io.github.pylonmc.pylon.core.entitylib")
    relocate("org.bstats", "io.github.pylonmc.pylon.core.bstats")

    archiveBaseName = "pylon-core"
    archiveClassifier = null
}

paper {
    generateLibrariesJson = true
    name = "PylonCore"
    loader = "io.github.pylonmc.pylon.core.PylonLoader"
    bootstrapper = "io.github.pylonmc.pylon.core.PylonBootstrapper"
    main = "io.github.pylonmc.pylon.core.PylonCore"
    version = project.version.toString()
    authors = listOf("Pylon team")
    apiVersion = "1.21"
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
}

tasks.withType<Jar> { duplicatesStrategy = DuplicatesStrategy.INCLUDE }

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "pylon-core"
            artifact(tasks.jar)
            artifact(tasks.kotlinSourcesJar)
            artifact(javadocJar)

            pom {
                name = artifactId
                description = "The core library for Pylon addons."
                url = "https://github.com/wickidcow/pylon-core"
                licenses {
                    license {
                        name = "GNU Lesser General Public License Version 3"
                        url = "https://www.gnu.org/licenses/lgpl-3.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "PylonMC"
                        name = "PylonMC"
                        organizationUrl = "https://github.com/pylonmc"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/wickidcow/pylon-core.git"
                    developerConnection = "scm:git:ssh://github.com:wickidcow/pylon-core.git"
                    url = "https://github.com/wickidcow/pylon-core"
                }
                withXml {
                    val root = asNode()
                    val dependenciesNode = root.appendNode("dependencies")
                    val configs = listOf(configurations.compileOnlyApi, configurations.api)
                    configs.flatMap { it.get().dependencies }.forEach {
                        val depNode = dependenciesNode.appendNode("dependency")
                        depNode.appendNode("groupId", it.group)
                        depNode.appendNode("artifactId", it.name)
                        depNode.appendNode("version", it.version)
                        depNode.appendNode("scope", "compile")
                    }
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_PASSWORD"))
    sign(publishing.publications["maven"])
}

tasks.withType<Sign> {
    onlyIf {
        System.getenv("SIGNING_KEY") != null && System.getenv("SIGNING_PASSWORD") != null
    }
}

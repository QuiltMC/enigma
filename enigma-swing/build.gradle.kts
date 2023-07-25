plugins {
	application
	id("checkstyle")
	id("java-library")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":enigma"))
    implementation(project(":enigma-server"))

    implementation(libs.jopt)
    implementation(libs.flatlaf)
    implementation(libs.flatlaf.extras) // for SVG icons
    implementation(libs.syntaxpane)
    implementation(libs.swing.dpi)
    implementation(libs.fontchooser)
    testImplementation(testFixtures(project(":enigma")))
}

application {
    mainClass.set("cuchaz.enigma.gui.Main")
}

fun Project.registerTestTask(name: String) {
    tasks.register("${name}TestGui", JavaExec::class.java) {
        group = "test"
        dependsOn(":enigma:${name}TestObf")
        mainClass.set(application.mainClass)
        classpath = sourceSets["main"].runtimeClasspath

        val jar = project(":enigma").file("build/test-obf/${name}.jar")
        val mappings = file("mappings/$name")
        args("-jar", jar, "-mappings", mappings)
        doFirst {
            mappings.mkdirs()
        }
    }
}

project(":enigma").file("src/test/java/cuchaz/enigma/inputs").listFiles()?.forEach {
    if (it.isDirectory) {
        registerTestTask(it.name)
    }
}

registerTestTask("complete")

publishing {
    publications {
        create<MavenPublication>(project.name) {
            groupId = project.group.toString()
            artifactId = project.name.toString()
            version = project.version.toString()
            from(components["java"])
        }
    }
}

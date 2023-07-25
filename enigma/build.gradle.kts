plugins {
    id("checkstyle")
    id("java-library")
    id("java-test-fixtures")
}

val proGuard: Configuration = configurations.create("proGuard")

dependencies {
    implementation(libs.bundles.asm)

    implementation(libs.vineflower)
    implementation(libs.cfr)
    implementation(libs.procyon)

    proGuard(libs.proguard)

    testImplementation(libs.jimfs)
}


// Generate "version.txt" file

val genOutputDir: File = file("$buildDir/generated-resources")

val generateVersionFile: Task by tasks.creating {
    val outputFile = file("$genOutputDir/version.properties")
    inputs.property("version", project.version)
    inputs.property("vineflower-version", libs.vineflower.get().versionConstraint.displayName)
    inputs.property("cfr-version", libs.cfr.get().versionConstraint.displayName)
    inputs.property("procyon-version", libs.procyon.get().versionConstraint.displayName)

    outputs.file(outputFile)
    doLast {
        outputFile.writeText(inputs.properties.entries.joinToString("\n") { "${it.key} = ${it.value}" })
    }
}

sourceSets.main.configure {
    output.dir(mapOf("builtBy" to generateVersionFile), genOutputDir)
}

// Generate obfuscated JARs for tests
// If your test fails for class file version problem with proguard, run gradle with -Dorg.gradle.java.home="<older jdk>" flag
fun Project.registerTestJarTasks(name: String, vararg input: String) {
    val libraryJarsArg = "<java.home>/jmods/java.base.jmod(!**.jar;!module-info.class)"

    tasks.register("${name}TestJar", Jar::class.java) {
        group = "test-setup"
        from(sourceSets["test"].output) {
            include(*input)
        }

        archiveFileName.set("${name}.jar")
        destinationDirectory.set(file("build/test-inputs"))
    }

    val confFile = if (file("src/test/resources/proguard-$name-test.conf").exists()) {
        "src/test/resources/proguard-$name-test.conf"
    } else {
        "src/test/resources/proguard-test.conf"
    }

    tasks.register("${name}TestObf", JavaExec::class.java) {
        group = "test-setup"
        dependsOn("${name}TestJar")
        mainClass.set("proguard.ProGuard")
        classpath(configurations["proGuard"])

        args(
            "@$confFile",
            "-injars", file("build/test-inputs/${name}.jar"),
            "-libraryjars", libraryJarsArg,
            "-outjars", file("build/test-obf/${name}.jar")
            //"-printmapping", file("build/test-obf/${name}.txt")
        )
    }
}

registerTestJarTasks("complete", "cuchaz/enigma/inputs/**/*.class")
tasks.test {
    dependsOn("completeTestObf")
}

file("src/test/java/cuchaz/enigma/inputs").listFiles()?.forEach { f ->
    if (f.isDirectory) {
        registerTestJarTasks(
            f.name,
            "cuchaz/enigma/inputs/${f.name}/**/*.class",
            "cuchaz/enigma/inputs/Keep.class"
        )
        tasks.test.configure {
            dependsOn("${f.name}TestObf")
        }
    }
}

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

plugins {
    application
    id("checkstyle")
    alias(libs.plugins.shadow)
}

dependencies {
    shadow(project(":enigma"))
    implementation(libs.jopt)
}

application {
    mainClass.set("cuchaz.enigma.network.DedicatedEnigmaServer")
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

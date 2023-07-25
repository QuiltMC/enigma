plugins {
	application
	id("checkstyle")
	alias(libs.plugins.shadow)
}


dependencies {
	implementation(project(":enigma"))
	testImplementation(testFixtures(project(":enigma")))
}

application {
	mainClass.set("cuchaz.enigma.command.Main")
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

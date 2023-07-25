plugins {
    id("java")
    `maven-publish`
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    tasks.withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    repositories {
        mavenLocal()
        mavenCentral()

        maven {
            url = uri("https://maven.quiltmc.org/repository/release/")
        }
        maven {
            url = uri("https://maven.quiltmc.org/repository/snapshot/")
        }
        maven {
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            // Vineflower snapshots
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
    }

    // https://github.com/gradle/gradle/issues/16634
    val rootLibs = rootProject.libs

    dependencies {
        implementation(rootLibs.guava)
        implementation(rootLibs.gson)

        implementation(rootLibs.bundles.tinylog)

        testImplementation(rootLibs.junit)
        testRuntimeOnly(rootLibs.junit.engine)
        testImplementation(rootLibs.hamcrest)
    }

    val env = System.getenv()
    val versionSuffix = if (env["GITHUB_ACTIONS"]?.toBoolean() == true) {
        if (env["SNAPSHOTS_URL"] != null) "-SNAPSHOT" else ""
    } else {
        "+local"
    }

    group = "org.quiltmc"
    version = "1.8.7$versionSuffix"

    tasks.test {
        useJUnitPlatform()
    }
}

allprojects {
    publishing {
        repositories {
            mavenLocal()

            val env = System.getenv()
            val mavenUrl = env["MAVEN_URL"]
            val snapshotsUrl = env["SNAPSHOTS_URL"]

            if (mavenUrl != null) {
                maven {
                    url = uri(mavenUrl)
                    credentials {
                        username = env["MAVEN_USERNAME"]
                        password = env["MAVEN_PASSWORD"]
                    }
                }
            } else if (snapshotsUrl != null) {
                maven {
                    url = uri(snapshotsUrl)
                    credentials {
                        username = env["SNAPSHOTS_USERNAME"]
                        password = env["SNAPSHOTS_PASSWORD"]
                    }
                }
            }
        }
    }
}

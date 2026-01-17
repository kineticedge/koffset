import org.gradle.api.JavaVersion.VERSION_25

import org.gradle.jvm.tasks.Jar

val logback_version: String by project
val kafka_version: String by project
val slf4j_version: String by project

val junit_pioneer_version: String by project
val junit_version: String by project

plugins {
    id("java")
}

// this allows for subprojects to use java plugin constructs
// without then also causing the parent to have an empty jar file
// generated.
tasks.withType<Jar> {
    onlyIf { !sourceSets["main"].allSource.isEmpty }
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://packages.confluent.io/maven/")
    }
}

subprojects.forEach {

    it.version = "1.0"

    it.plugins.apply("java")
    it.plugins.apply("application")

    it.java {
        sourceCompatibility = VERSION_25
        targetCompatibility = VERSION_25
    }

    it.dependencies {
        implementation("org.apache.kafka:kafka-clients:$kafka_version")
        implementation("org.slf4j:slf4j-api:$slf4j_version")
        implementation("ch.qos.logback:logback-classic:${logback_version}")
    }

    it.tasks.test {
        useJUnitPlatform()
        environment(System.getenv())
    }

}


subprojects {
    // a quick way to test the application locally without having to build the artifact, build generates
    // a classpath which is then used by this script.
    if (file("${project.projectDir}/run.sh").exists()) {
        val createIntegrationClasspath: (String) -> Unit = { scriptName ->
            val cp = extensions.getByName<JavaPluginExtension>("java").sourceSets["main"].runtimeClasspath.files.joinToString("\n") {
                """export CP="${'$'}{CP}:$it""""
            }
            val file = file(scriptName)
            file.writeText("export CP=\"\"\n$cp\n")
            file.setExecutable(true)
        }
        val postBuildScript by tasks.registering {
            doLast {
                createIntegrationClasspath("./.classpath.sh")
            }
        }
        tasks.named("build").configure {
            finalizedBy(postBuildScript)
        }
    }
}

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    id("java")
    id("application")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

group = "io.kineticedge.ksd.metrics"
version = "1.0"

repositories {
    mavenCentral()
}

val kafka_version: String by project
val slf4j_version: String by project
val netty_version: String by project

val testcontainer_version: String by project
val mockito_version: String by project
val junit_version: String by project
val junit_platform_version: String by project

dependencies {

    implementation("io.netty:netty-transport:${netty_version}")
    implementation("io.netty:netty-codec-http:${netty_version}")
    implementation("io.netty:netty-transport-native-epoll:${netty_version}")

    // need to be runtimeOnly, if you use implementation kafka admin client will fail
    // while netty is not part of Apache Kafka Clients, something is shading netty (grpc)?
    runtimeOnly("io.netty:netty-transport-native-epoll:${netty_version}:linux-x86_64")
    runtimeOnly("io.netty:netty-transport-native-epoll:${netty_version}:linux-aarch_64")

    testImplementation("org.testcontainers:testcontainers:$testcontainer_version")
    testImplementation("org.testcontainers:testcontainers-kafka:$testcontainer_version")
    testImplementation("org.mockito:mockito-core:${mockito_version}")
    testImplementation("org.mockito:mockito-junit-jupiter:${mockito_version}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit_version")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit_version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit_version")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${junit_platform_version}")
}

val dockerBuild by tasks.registering(Exec::class) {
    inputs.files("docker/Dockerfile", "docker/entrypoint.sh")
    workingDir =  file("${project.rootDir}")
   // inputs.files("docker/Dockerfile", "docker/entrypoint.sh")
   // workingDir = project.rootDir // Ensures we run from the project root
    commandLine("/usr/local/bin/docker", "build", "-f", "./koffset-exporter/docker/Dockerfile", "--label", "project=koffset-exporter", "-t", "koffset-exporter:latest", ".")
}

val dockerDevBuild by tasks.registering(Exec::class) {
    workingDir =  file("${project.rootDir}")
    inputs.files("koffset-exporter/docker/Dockerfile.dev", "koffset-exporter/docker/entrypoint.sh")
    commandLine("/usr/local/bin/docker", "build", "-f", "./koffset-exporter/docker/Dockerfile.dev", "--label", "project=koffset-exporter", "-t", "koffset-exporter:latest", "./koffset-exporter")
}

application {
    mainClass.set("io.kineticedge.koffset.Main")
}

val generateVersionProperties by tasks.registering {
    val propertiesFile = layout.buildDirectory.file("generated/version.properties")
    outputs.file(propertiesFile)

    doLast {
        val cmdSha = "git rev-parse --short HEAD"
        val cmdTag = "git describe --tags --exact-match"
        val cmdBranch = "git rev-parse --abbrev-ref HEAD"

        fun run(cmd: String): String = try {
            Runtime.getRuntime().exec(cmd).inputStream.bufferedReader().readText().trim()
        } catch (e: Exception) {
            "unknown"
        }

        val sha = run(cmdSha)
        var ref = run(cmdTag)
        if (ref == "unknown" || ref.isEmpty()) {
            ref = run(cmdBranch)
        }

        val buildTime = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        propertiesFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("""
                build.time=$buildTime
                build.sha=$sha
                build.ref=$ref
            """.trimIndent())
        }
    }
}

tasks.processResources {
    from(generateVersionProperties)
}

//val dockerTagPrev by tasks.registering(Exec::class) {
//    commandLine("/usr/local/bin/docker", "tag", "koffset-exporter:latest", "koffset-exporter:prev")
//    isIgnoreExitValue = true
//}
//
//val dockerRmiPrev by tasks.registering(Exec::class) {
//    commandLine("/usr/local/bin/docker", "rmi", "koffset-exporter:prev")
//    isIgnoreExitValue = true
//}
//
//val dockerBuild by tasks.registering(Exec::class) {
//    inputs.files("Dockerfile")
//    dependsOn(dockerTagPrev)
//    commandLine("/usr/local/bin/docker", "build", "-f", "./docker.local/Dockerfile", "-t", "koffset-exporter:latest", ".")
//}
//dockerBuild.configure {
//    finalizedBy(dockerRmiPrev)
//}
//
//tasks.named("build") {
//    finalizedBy(dockerBuild)
//}

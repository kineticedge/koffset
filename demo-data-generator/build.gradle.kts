plugins {
    id("java")
    //id("application")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

group = "io.kineticedge.koffsets.ddg"
version = "1.0"

repositories {
    mavenCentral()
}

val kafka_version: String by project
val slf4j_version: String by project


dependencies {
}


application {
    mainClass.set("io.kineticedge.koffset.ddg.Main")
}

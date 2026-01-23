rootProject.name = "koffset"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
    }
}

include("koffset-exporter")

// allows for the Dockerfile build to exclude data-demo-generator
if (File(rootDir, "demo-data-generator").exists()) {
    include("demo-data-generator")
}

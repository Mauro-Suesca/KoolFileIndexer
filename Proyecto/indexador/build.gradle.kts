/*
 * Modulo indexador
 */

plugins {
    application
    id("buildlogic.java-application-conventions")
    id("com.gradleup.shadow") version "8.3.8"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation("org.apache.commons:commons-text")
    implementation("org.postgresql:postgresql:42.7.7")
    testImplementation("junit:junit:4.13.2")
}

application {
    // Define the main class for the application.
    mainClass = "koolfileindexer.Indexador"
}

tasks.shadowJar {
    archiveFileName.set("KoolFileIndexer_indexador_${project.version}.jar")
    archiveBaseName.set("KoolFileIndexer_indexador")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    destinationDirectory.set(file("${rootProject.projectDir}/dist"))

    manifest {
        attributes(mapOf("Main-Class" to "koolfileindexer.Indexador"))
    }
}
